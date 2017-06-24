/*
 *  Copyright (C) 2015 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.algorithms;

import jloda.util.*;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.core.Document;
import megan.core.ReadAssignmentCalculator;
import megan.core.SyncArchiveAndDataTable;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.data.UpdateItemList;
import megan.fx.NotificationsInSwing;
import megan.io.InputOutputReaderWriter;
import megan.rma6.RMA6File;
import megan.rma6.ReadBlockRMA6;
import megan.util.interval.Interval;
import megan.util.interval.IntervalTree;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Analyzes all reads in a sample
 * Daniel Huson, 1.2009, 3.2016, 5.2017
 */
public class DataProcessorParallel {
    private final Document doc;
    private final int numberOfClassifications;
    private final Counts counts;
    private final Object syncUpdateList = new Object();

    private final IReadBlock sentinel = new ReadBlockRMA6(null, false, null);

    /**
     * constructor
     *
     * @param doc
     */
    public DataProcessorParallel(final Document doc) {
        this.doc = doc;
        this.numberOfClassifications = doc.getActiveViewers().size();
        this.counts = new Counts(numberOfClassifications);
    }

    /**
     * process a dataset
     *
     * @throws CanceledException
     */
    public int apply() throws CanceledException {
        final ProgressListener progress = doc.getProgressListener();
        try {
            progress.setTasks("Binning reads", "Initialization");

            System.err.println("Binning reads... using parallel algorithm");
            if (doc.isUseIdentityFilter()) {
                System.err.println("Using min percent-identity values for taxonomic assignment of 16S reads");
            }

            final boolean doMatePairs = doc.isPairedReads() && doc.getMeganFile().isRMA6File();
            if (doc.isPairedReads() && !doc.getMeganFile().isRMA6File())
                System.err.println("WARNING: Not an RMA6 file, will ignore paired read information");
            if (doMatePairs)
                System.err.println("Using paired reads in taxonomic assignment...");
            final boolean usingLongReadAlgorithm = (doc.getLcaAlgorithm() == Document.LCAAlgorithm.longReads);

            final String[] cNames = doc.getActiveViewers().toArray(new String[numberOfClassifications]);
            final boolean[] useLCAForClassification = new boolean[numberOfClassifications];
            final int taxonomyIndex = Basic.getIndex(Classification.Taxonomy, cNames);
            for (int c = 0; c < numberOfClassifications; c++) {
                if (c == taxonomyIndex) {
                    useLCAForClassification[c] = true;
                } else {
                    ClassificationManager.ensureTreeIsLoaded(cNames[c]);
                    useLCAForClassification[c] = ProgramProperties.get(cNames[c] + "UseLCA", cNames[c].equals(Classification.Taxonomy));
                }
            }

            // setup assignment algorithms:
            final IAssignmentAlgorithmCreator[] assignmentAlgorithmCreators = new IAssignmentAlgorithmCreator[numberOfClassifications];
            for (int c = 0; c < numberOfClassifications; c++) {
                if (c == taxonomyIndex) {
                    switch (doc.getLcaAlgorithm()) {
                        default:
                        case naive:
                            assignmentAlgorithmCreators[c] = new AssignmentUsingLCAForTaxonomyCreator(cNames[c], doc.isUseIdentityFilter());
                            break;
                        case weighted:
                            // we are assuming that taxonomy classification is The taxonomy classification
                            assignmentAlgorithmCreators[c] = new AssignmentUsingWeightedLCACreator(doc, doc.isUseIdentityFilter(), doc.getWeightedLCAPercent());
                            break;
                        case longReads:
                            assignmentAlgorithmCreators[c] = new AssignmentUsingSegmentBasedLCACreator(doc, doc.getTopPercent());
                            break;
                    }
                } else if (useLCAForClassification[c])
                    assignmentAlgorithmCreators[c] = new AssignmentUsingLCACreator(cNames[c]);
                else if (usingLongReadAlgorithm)
                    assignmentAlgorithmCreators[c] = new AssignmentUsingMultiGeneBestHitCreator(cNames[c], doc.getMeganFile().getFileName());
                else
                    assignmentAlgorithmCreators[c] = new AssignmentUsingBestHitCreator(cNames[c], doc.getMeganFile().getFileName());
            }

            final UpdateItemList updateList = new UpdateItemList(numberOfClassifications);

            final ArrayList<Exception> exceptions = new ArrayList<>();

            final int numberOfThreads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
            final ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
            final CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

            final ArrayBlockingQueue<IReadBlock> queue = new ArrayBlockingQueue<>(Math.max(1000, 10 * numberOfThreads));

            // launch all slave threads
            for (int t = 0; t < numberOfThreads; t++) {
                service.submit(new Runnable() {
                    public void run() {
                        try {
                            applyInThread(cNames, assignmentAlgorithmCreators, taxonomyIndex, queue, updateList, progress);
                        } catch (Exception ex) {
                            exceptions.add(ex);
                        } finally {
                            countDownLatch.countDown();
                        }
                    }
                });
            }

            // fill the queue:
            try (final IReadBlockIterator it = doc.getConnector().getAllReadsIterator(0, 10, false, true)) {
                progress.setTasks("Binning reads", "Analyzing alignments");
                progress.setMaximum(it.getMaximumProgress());
                progress.setProgress(0);
                while (it.hasNext()) {
                    queue.put(it.next());
                    try {
                        progress.setProgress(it.getProgress());
                    } catch (CanceledException ex) {
                        break;
                    }
                }
            }
            // add one sentinel per thread:
            for (int i = 0; i < numberOfThreads; i++) {
                queue.put(sentinel);
            }

            // wait for all slave threads to finish:
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                Basic.caught(e);
            }
            service.shutdownNow();

            if (exceptions.size() > 0) {
                throw exceptions.get(0);
            }

            if (progress.isUserCancelled())
                throw new CanceledException();

            if (progress instanceof ProgressPercentage) {
                ((ProgressPercentage) progress).reportTaskCompleted();
            }

            // report summary:

            System.err.println(String.format("Total reads:  %,15d", counts.numberOfReadsFound.get()));
            if (counts.totalWeightX1000.get() > counts.numberOfReadsFound.get())
                System.err.println(String.format("Total weight: %,15d", (long) counts.getTotalWeight()));

            if (counts.numberOfReadsWithLowComplexity.get() > 0)
                System.err.println(String.format("Low complexity:%,15d", counts.numberOfReadsWithLowComplexity.get()));
            if (counts.numberOfReadsFailedCoveredThreshold.get() > 0)
                System.err.println(String.format("Low covered:   %,15d", counts.numberOfReadsFailedCoveredThreshold.get()));

            System.err.println(String.format("With hits:     %,15d ", counts.numberOfReadsWithHits.get()));
            System.err.println(String.format("Alignments:    %,15d", counts.numberOfMatches.get()));

            for (int c = 0; c < numberOfClassifications; c++) {
                System.err.println(String.format("%-19s%,11d", "Assig. " + cNames[c] + ":", counts.countAssigned[c].get()));
            }

            // if used mate pairs, report here:
            if (counts.numberAssignedViaMatePair.get() > 0) {
                System.err.println(String.format("Tax. ass. by mate:%,12d", counts.numberAssignedViaMatePair.get()));
            }

            progress.setCancelable(false); // can't cancel beyond here because file could be left in undefined state

            doc.setNumberReads(counts.numberOfReadsFound.get());

            // If min support percentage is set, set the min support:
            if (doc.getMinSupportPercent() > 0) {
                doc.setMinSupport((int) Math.max(1, Math.round((doc.getMinSupportPercent() / 100.0) * (counts.numberOfReadsWithHits.get() + counts.numberAssignedViaMatePair.get()))));
                System.err.println("MinSupport set to: " + doc.getMinSupport());
            }

            // 2. apply min support and disabled taxa filter

            for (int c = 0; c < numberOfClassifications; c++) {
                final String cName = cNames[c];
                // todo: need to remove assignments to disabled ids when not using the LCA algorithm
                if (useLCAForClassification[c] && (doc.getMinSupport() > 0 || ClassificationManager.get(cName, false).getIdMapper().getDisabledIds().size() > 0)) {
                    progress.setTasks("Binning reads", "Applying min-support & disabled filter to " + cName + "...");
                    final MinSupportFilter minSupportFilter = new MinSupportFilter(cName, updateList.getClassIdToWeightMap(c), doc.getMinSupport(), progress);
                    final Map<Integer, Integer> changes = minSupportFilter.apply();

                    for (Integer srcId : changes.keySet()) {
                        updateList.appendClass(c, srcId, changes.get(srcId));
                    }
                    System.err.println(String.format("Min-supp. changes:%,12d", changes.size()));
                }
            }

            // 3. save classifications

            progress.setTasks("Binning reads", "Writing classification tables");

            doc.getConnector().updateClassifications(cNames, updateList, progress);
            doc.getConnector().setNumberOfReads((int) doc.getNumberOfReads());

            // 4. sync
            progress.setTasks("Binning reads", "Syncing");
            SyncArchiveAndDataTable.syncRecomputedArchive2Summary(doc.getReadAssignmentMode(), doc.getTitle(), "LCA", doc.getBlastMode(), doc.getParameterString(), doc.getConnector(), doc.getDataTable(), (int) doc.getAdditionalReads());

            if (progress instanceof ProgressPercentage)
                ((ProgressPercentage) progress).reportTaskCompleted();

            // MeganProperties.addRecentFile(new File(doc.getMeganFile().getFileName()));
            doc.setDirty(false);

            // report classification sizes:
            for (String cName : cNames) {
                System.err.println(String.format("Class. %-13s%,10d", cName + ":", doc.getConnector().getClassificationSize(cName)));
            }
            return (int) doc.getDataTable().getTotalReads();
        } catch (CanceledException ex) {
            throw ex;
        } catch (Exception ex) {
            Basic.caught(ex);
            NotificationsInSwing.showInternalError("Data Processor failed: " + ex.getMessage());
            return 0;
        }
    }

    /**
     * performs taxonomic and functional binning
     *
     * @param queue
     * @param updateList
     * @param progress
     * @throws Exception
     */
    private void applyInThread(String[] cNames, IAssignmentAlgorithmCreator[] assignmentAlgorithmCreators, final int taxonomyIndex, final ArrayBlockingQueue<IReadBlock> queue, final UpdateItemList updateList, final ProgressListener progress) throws Exception {
        final int numberOfClassifications = doc.getActiveViewers().size();
        final double minCoveredPercent = doc.getMinPercentReadToCover();
        final boolean usingLongReadAlgorithm = (doc.getLcaAlgorithm() == Document.LCAAlgorithm.longReads);
        final boolean doMatePairs = doc.isPairedReads() && doc.getMeganFile().isRMA6File();

        // step 0: set up classification algorithms

        final IntervalTree<Object> intervals;
        if (minCoveredPercent > 0 && doc.isLongReads() || doc.getReadAssignmentMode() == Document.ReadAssignmentMode.alignedBases)
            intervals = new IntervalTree<>();
        else
            intervals = null;

        if (minCoveredPercent > 0)
            System.err.println(String.format("Minimum percentage of read to be covered: %.1f%%", minCoveredPercent));


        // step 1:  stream through reads and assign classes

        final IAssignmentAlgorithm[] assignmentAlgorithm = new IAssignmentAlgorithm[numberOfClassifications];
        for (int c = 0; c < numberOfClassifications; c++)
            assignmentAlgorithm[c] = assignmentAlgorithmCreators[c].createAssignmentAlgorithm();

        final Set<Integer>[] knownIds = new HashSet[numberOfClassifications];
        for (int c = 0; c < numberOfClassifications; c++) {
            knownIds[c] = new HashSet<>();
            knownIds[c].addAll(ClassificationManager.get(cNames[c], true).getName2IdMap().getIds());
        }

        final InputOutputReaderWriter mateReader = doMatePairs ? new InputOutputReaderWriter(doc.getMeganFile().getFileName(), "r") : null;

        try {
            final float topPercentForActiveMatchFiltering;
            if (usingLongReadAlgorithm) {
                topPercentForActiveMatchFiltering = 0;
            } else
                topPercentForActiveMatchFiltering = doc.getTopPercent();

            final int[] classIds = new int[numberOfClassifications];
            final ArrayList<int[]>[] moreClassIds;
            final float[] multiGeneWeights;

            if (usingLongReadAlgorithm) {
                moreClassIds = new ArrayList[numberOfClassifications];
                for (int c = 0; c < numberOfClassifications; c++)
                    moreClassIds[c] = new ArrayList<>();
                multiGeneWeights = new float[numberOfClassifications];
            } else {
                moreClassIds = null;
                multiGeneWeights = null;
            }

            final ReadAssignmentCalculator readAssignmentCalculator = new ReadAssignmentCalculator(doc.getReadAssignmentMode());

            final ReadBlockRMA6 mateReadBlock;
            if (doMatePairs) {
                try (RMA6File RMA6File = new RMA6File(doc.getMeganFile().getFileName(), "r")) {
                    String[] matchClassificationNames = RMA6File.getHeaderSectionRMA6().getMatchClassNames();
                    mateReadBlock = new ReadBlockRMA6(doc.getBlastMode(), true, matchClassificationNames);
                }
            } else
                mateReadBlock = null;

            // main loop
            while (true) {
                progress.checkForCancel();

                // System.err.println("Queue size: "+queue.size());
                final IReadBlock readBlock = queue.take();
                if (readBlock == sentinel)
                    return;

                // clean up previous values
                for (int c = 0; c < numberOfClassifications; c++) {
                    classIds[c] = 0;
                    if (usingLongReadAlgorithm) {
                        moreClassIds[c].clear();
                        multiGeneWeights[c] = 0;
                    }
                }

                final boolean hasLowComplexity = readBlock.getComplexity() > 0 && readBlock.getComplexity() + 0.01 < doc.getMinComplexity();

                if (readBlock.getNumberOfAvailableMatchBlocks() > 0)
                    counts.numberOfReadsWithHits.addAndGet(readBlock.getReadWeight());

                readBlock.setReadWeight(readAssignmentCalculator.compute(readBlock, intervals));

                counts.numberOfReadsFound.incrementAndGet();
                counts.addToTotalWeight(readBlock.getReadWeight());
                counts.numberOfMatches.addAndGet(readBlock.getNumberOfMatches());

                if (hasLowComplexity)
                    counts.numberOfReadsWithLowComplexity.addAndGet(readBlock.getReadWeight());

                int taxId = 0;
                if (taxonomyIndex >= 0) {
                    final BitSet activeMatchesForTaxa = new BitSet(); // pre filter matches for taxon identification
                    ActiveMatches.compute(doc.getMinScore(), topPercentForActiveMatchFiltering, doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, Classification.Taxonomy, activeMatchesForTaxa);
                    if (minCoveredPercent == 0 || ensureCovered(minCoveredPercent, readBlock, activeMatchesForTaxa, intervals)) {
                        if (doMatePairs && readBlock.getMateUId() > 0) {
                            mateReader.seek(readBlock.getMateUId());
                            mateReadBlock.read(mateReader, false, true, doc.getMinScore(), doc.getMaxExpected());
                            taxId = assignmentAlgorithm[taxonomyIndex].computeId(activeMatchesForTaxa, readBlock);
                            final BitSet activeMatchesForMateTaxa = new BitSet(); // pre filter matches for mate-based taxon identification
                            ActiveMatches.compute(doc.getMinScore(), topPercentForActiveMatchFiltering, doc.getMaxExpected(), doc.getMinPercentIdentity(), mateReadBlock, Classification.Taxonomy, activeMatchesForMateTaxa);
                            int mateTaxId = assignmentAlgorithm[taxonomyIndex].computeId(activeMatchesForMateTaxa, mateReadBlock);
                            if (mateTaxId > 0) {
                                if (taxId <= 0) {
                                    taxId = mateTaxId;
                                    counts.numberAssignedViaMatePair.incrementAndGet();
                                } else {
                                    int bothId = assignmentAlgorithm[taxonomyIndex].getLCA(taxId, mateTaxId);
                                    if (bothId == taxId)
                                        taxId = mateTaxId;
                                        // else if(bothId==taxId) taxId=taxId; // i.e, no change
                                    else if (bothId != mateTaxId)
                                        taxId = bothId;
                                }
                            }
                        } else {
                            taxId = assignmentAlgorithm[taxonomyIndex].computeId(activeMatchesForTaxa, readBlock);
                        }
                    } else
                        counts.numberOfReadsFailedCoveredThreshold.incrementAndGet();
                }

                for (int c = 0; c < numberOfClassifications; c++) {
                    int id;
                    if (hasLowComplexity) {
                        id = IdMapper.LOW_COMPLEXITY_ID;
                    } else if (c == taxonomyIndex) {
                        id = taxId;
                    } else {
                        final BitSet activeMatchesForFunction = new BitSet(); // pre filter matches for taxon identification
                        ActiveMatches.compute(doc.getMinScore(), topPercentForActiveMatchFiltering, doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, cNames[c], activeMatchesForFunction);
                        id = assignmentAlgorithm[c].computeId(activeMatchesForFunction, readBlock);
                        if (id > 0 && usingLongReadAlgorithm && assignmentAlgorithm[c] instanceof IMultiAssignmentAlgorithm) {
                            int numberOfSegments = ((IMultiAssignmentAlgorithm) assignmentAlgorithm[c]).getOtherClassIds(c, numberOfClassifications, moreClassIds[c]);
                            multiGeneWeights[c] = (numberOfSegments > 0 ? (float) readBlock.getReadWeight() / (float) numberOfSegments : 0);
                        }
                    }

                    if (id <= 0 && readBlock.getNumberOfAvailableMatchBlocks() == 0)
                        id = IdMapper.NOHITS_ID;
                    else if (!knownIds[c].contains(id))
                        id = IdMapper.UNASSIGNED_ID;

                    classIds[c] = id;
                    if (id == IdMapper.UNASSIGNED_ID)
                        counts.countUnassigned[c].incrementAndGet();
                    else if (id > 0)
                        counts.countAssigned[c].incrementAndGet();
                }
                synchronized (syncUpdateList) {
                    updateList.addItem(readBlock.getUId(), readBlock.getReadWeight(), classIds);

                    if (usingLongReadAlgorithm) {
                        for (int c = 0; c < numberOfClassifications; c++) {
                            for (int[] aClassIds : moreClassIds[c]) {
                                updateList.addItem(readBlock.getUId(), multiGeneWeights[c], aClassIds);
                            }
                        }
                    }
                }
            }
        } finally {
            if (mateReader != null)
                mateReader.close();
        }
    }

    /**
     * check that enough of read is covered by alignments
     *
     * @param minCoveredPercent percent of read that must be covered
     * @param readBlock
     * @param activeMatches
     * @param intervals this will be non-null in long read mode, in which case we check the total cover, otherwise, we check the amount covered by any one match
     * @return true, if sufficient coverage
     */
    private boolean ensureCovered(double minCoveredPercent, IReadBlock readBlock, BitSet activeMatches, IntervalTree<Object> intervals) {
        int lengthToCover = (int) (0.01 * minCoveredPercent * readBlock.getReadLength());
        if (lengthToCover == 0)
            return true;

        if (intervals != null)
            intervals.clear();

        for (int m = activeMatches.nextSetBit(0); m != -1; m = activeMatches.nextSetBit(m + 1)) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
            if (Math.abs(matchBlock.getAlignedQueryStart() - matchBlock.getAlignedQueryStart()) >= lengthToCover)
                return true;
            if (intervals != null) {
                Interval<Object> interval = new Interval<>(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), null);
                intervals.add(interval);
                if (intervals.getCovered() >= lengthToCover)
                    return true;
            }
        }
        return false;
    }

    class Counts {
        final AtomicLong numberOfReadsFound = new AtomicLong();
        private AtomicLong totalWeightX1000 = new AtomicLong();
        final AtomicLong numberOfMatches = new AtomicLong();
        final AtomicLong numberOfReadsWithLowComplexity = new AtomicLong();
        final AtomicLong numberOfReadsWithHits = new AtomicLong();
        final AtomicLong numberAssignedViaMatePair = new AtomicLong();
        final AtomicLong numberOfReadsFailedCoveredThreshold = new AtomicLong();
        final AtomicInteger[] countUnassigned;
        final AtomicInteger[] countAssigned;

        Counts(int numberOfClassifications) {
            countUnassigned = new AtomicInteger[numberOfClassifications];
            for (int i = 0; i < numberOfClassifications; i++)
                countUnassigned[i] = new AtomicInteger();
            countAssigned = new AtomicInteger[numberOfClassifications];
            for (int i = 0; i < numberOfClassifications; i++)
                countAssigned[i] = new AtomicInteger();
        }

        public void setTotalWeight(double weight) {
            totalWeightX1000.set(Math.round(1000 * weight));
        }

        public long getTotalWeight() {
            return totalWeightX1000.get() / 1000;
        }

        public void addToTotalWeight(double weight) {
            totalWeightX1000.addAndGet(Math.round(1000 * weight));
        }
    }
}
