/*
 *  Copyright (C) 2016 Daniel H. Huson
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
import megan.core.SyncArchiveAndDataTable;
import megan.daa.connector.ReadBlockDAA;
import megan.data.IConnector;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.data.UpdateItemList;
import megan.fx.NotificationsInSwing;
import megan.io.InputOutputReaderWriter;
import megan.rma6.RMA6File;
import megan.rma6.ReadBlockRMA6;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Analyzes all reads in a sample
 * Daniel Huson, 1.2009, 3.2106
 */
public class DataProcessor {
    /**
     * process a dataset
     *
     * @param doc
     * @throws jloda.util.CanceledException
     */
    public static int apply(final Document doc) throws CanceledException {
        final ProgressListener progress = doc.getProgressListener();

        progress.setTasks("Analyzing reads & alignments", "Initialization");
        try {
            System.err.println("Analyzing...");
            if (doc.isUseIdentityFilter()) {
                System.err.println("Using min percent-identity values for taxonomic assignment of 16S reads");
            }

            final int numberOfClassifications = doc.getActiveViewers().size();
            final String[] cNames = doc.getActiveViewers().toArray(new String[numberOfClassifications]);
            final int taxonomyIndex = Basic.getIndex(Classification.Taxonomy, cNames);
            for (int i = 0; i < cNames.length; i++) {
                if (i != taxonomyIndex)
                    ClassificationManager.ensureTreeIsLoaded(cNames[i]);
            }

            final UpdateItemList updateList = new UpdateItemList(cNames.length);

            final boolean doMatePairs = doc.isPairedReads() && doc.getMeganFile().isRMA6File();

            if (doc.isPairedReads() && !doMatePairs)
                System.err.println("WARNING: Not an RMA6 file, will ignore paired read information");
            if (doMatePairs)
                System.err.println("Using paired reads in taxonomic assignment...");

            // step 0: set up classification algorithms

            final IAssignmentAlgorithmCreator[] assignmentAlgorithmCreators = new IAssignmentAlgorithmCreator[numberOfClassifications];
            for (int i = 0; i < numberOfClassifications; i++) {
                if (i == taxonomyIndex) {
                    if (doc.isWeightedLCA()) {
                        assignmentAlgorithmCreators[i] = new AssignmentUsingWeightedLCACreator(doc, cNames[taxonomyIndex], doc.getWeightedLcaPercent());
                    } else
                        assignmentAlgorithmCreators[i] = new AssignmentUsingLCAForTaxonomyCreator(cNames[i], doc.isUseIdentityFilter());
                } else if (ProgramProperties.get(cNames[i] + "UseLCA", false))
                    assignmentAlgorithmCreators[i] = new AssignmentUsingLCACreator(cNames[i]);
                else
                    assignmentAlgorithmCreators[i] = new AssignmentUsingBestHitCreator(cNames[i]);
            }

            // setup multi-threading:
            final int numberOfThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
            final ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
            final CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

            final Integer[] classIds = new Integer[numberOfClassifications];

            // step 1:  stream through reads and assign classes

            progress.setSubtask("Processing alignments");

            final long[] numberOfReadsFound = new long[numberOfThreads];
            final long[] numberOfMatches = new long[numberOfThreads];
            final long[] numberOfReadsWithLowComplexity = new long[numberOfThreads];
            final long[] numberOfReadsWithHits = new long[numberOfThreads];
            final long[] numberAssignedViaMatePair = new long[numberOfThreads];

            final IConnector connector = doc.getConnector();
            final InputOutputReaderWriter mateReader = doMatePairs ? new InputOutputReaderWriter(doc.getMeganFile().getFileName(), "r") : null;

            final int[][] countUnassigned = new int[numberOfClassifications][numberOfThreads];
            final int[][] countAssigned = new int[numberOfClassifications][numberOfThreads];

            final ArrayBlockingQueue<IReadBlock> queue = new ArrayBlockingQueue<>(1000);
            final IReadBlock sentinel = new ReadBlockDAA();

            for (int i = 0; i < numberOfThreads; i++) {
                final int threadNumber = i;
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final IAssignmentAlgorithm[] assignmentAlgorithm = new IAssignmentAlgorithm[numberOfClassifications];
                            for (int i = 0; i < numberOfClassifications; i++)
                                assignmentAlgorithm[i] = assignmentAlgorithmCreators[i].createAssignmentAlgorithm();

                            final Set<Integer>[] knownIds = new HashSet[numberOfClassifications];
                            for (int i = 0; i < cNames.length; i++) {
                                knownIds[i] = new HashSet<>();
                                knownIds[i].addAll(ClassificationManager.get(cNames[i], true).getName2IdMap().getIds());
                            }
                            final ReadBlockRMA6 mateReadBlock;
                            if (doMatePairs) {
                                try (RMA6File RMA6File = new RMA6File(doc.getMeganFile().getFileName(), "r")) {
                                    String[] matchClassificationNames = RMA6File.getHeaderSectionRMA6().getMatchClassNames();
                                    mateReadBlock = new ReadBlockRMA6(doc.getBlastMode(), doMatePairs, matchClassificationNames);
                                }
                            } else
                                mateReadBlock = null;

                            final BitSet activeMatches = new BitSet(); // pre filter matches for taxon identification
                            final BitSet activeMatchesForMateTaxa = new BitSet(); // pre filter matches for mate-based taxon identification

                            while (true) {
                                final IReadBlock readBlock = queue.take();
                                if (readBlock == sentinel)
                                    break;

                                if (progress.isUserCancelled())
                                    break;

                                if (readBlock.getReadWeight() == 0)
                                    readBlock.setReadWeight(1);

                                numberOfReadsFound[threadNumber] += readBlock.getReadWeight();
                                numberOfMatches[threadNumber] += readBlock.getNumberOfMatches();

                                final boolean hasLowComplexity = readBlock.getComplexity() > 0 && readBlock.getComplexity() + 0.01 < doc.getMinComplexity();

                                if (hasLowComplexity)
                                    numberOfReadsWithLowComplexity[threadNumber] += readBlock.getReadWeight();

                                ActiveMatches.compute(doc.getMinScore(), doc.getTopPercent(), doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, Classification.Taxonomy, activeMatches);

                                int taxId;
                                if (doMatePairs && readBlock.getMateUId() > 0) {
                                    synchronized (mateReader) {
                                        mateReader.seek(readBlock.getMateUId());
                                        mateReadBlock.read(mateReader, false, true, doc.getMinScore(), doc.getMaxExpected());
                                    }
                                    ActiveMatches.compute(doc.getMinScore(), doc.getTopPercent(), doc.getMaxExpected(), doc.getMinPercentIdentity(), mateReadBlock, Classification.Taxonomy, activeMatchesForMateTaxa);
                                    ActiveMatches.restrictActiveMatchesToSameIds(readBlock, activeMatches, mateReadBlock, Classification.Taxonomy, activeMatchesForMateTaxa);
                                    taxId = assignmentAlgorithm[taxonomyIndex].computeId(activeMatches, readBlock);
                                    if (taxId <= 0) {
                                        taxId = assignmentAlgorithm[taxonomyIndex].computeId(activeMatchesForMateTaxa, mateReadBlock);
                                        if (taxId > 0)
                                            numberAssignedViaMatePair[threadNumber]++;
                                    }
                                } else
                                    taxId = assignmentAlgorithm[taxonomyIndex].computeId(activeMatches, readBlock);

                                if (activeMatches.cardinality() > 0)
                                    numberOfReadsWithHits[threadNumber] += readBlock.getReadWeight();

                                for (int i = 0; i < numberOfClassifications; i++) {
                                    int id;
                                    if (hasLowComplexity) {
                                        id = IdMapper.LOW_COMPLEXITY_ID;
                                    } else if (i == taxonomyIndex) {
                                        id = taxId;
                                    } else {
                                        ActiveMatches.compute(doc.getMinScore(), doc.getTopPercent(), doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, cNames[i], activeMatches);
                                        id = assignmentAlgorithm[i].computeId(activeMatches, readBlock);
                                    }
                                    if (!knownIds[i].contains(id))
                                        id = IdMapper.UNASSIGNED_ID;

                                    classIds[i] = id;
                                    if (id == IdMapper.UNASSIGNED_ID)
                                        countUnassigned[i][threadNumber]++;
                                    else if (id > 0)
                                        countAssigned[i][threadNumber]++;
                                }
                                synchronized (updateList) {
                                    updateList.addItem(readBlock.getUId(), readBlock.getReadWeight(), classIds);
                                }
                            }
                        } catch (Exception ex) {
                            Basic.caught(ex);
                        } finally {
                            countDownLatch.countDown();
                        }
                    }
                });
            }

            /**
             * feed the queue:
             */
            try (final IReadBlockIterator it = connector.getAllReadsIterator(0, 10, false, true)) {
                progress.setMaximum(it.getMaximumProgress());
                progress.setProgress(0);

                while (it.hasNext()) {
                    queue.put(it.next());
                    progress.setProgress(it.getProgress());
                }
                for (int i = 0; i < numberOfThreads; i++) { // add one sentinel for each thread
                    queue.put(sentinel);
                }
            } catch (Exception e) {
                Basic.caught(e);
            }

            // await worker threads:
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                Basic.caught(e);
            } finally {
                executorService.shutdownNow();
            }

            if (progress.isUserCancelled())
                throw new CanceledException();

            if (progress instanceof ProgressPercentage) {
                ((ProgressPercentage) progress).reportTaskCompleted();
            }

            System.err.println(String.format("Total reads:   %,15d", Basic.getSum(numberOfReadsFound)));
            if (Basic.getSum(numberOfReadsWithLowComplexity) > 0)
                System.err.println(String.format("Low complexity:%,15d", Basic.getSum(numberOfReadsWithLowComplexity)));
            System.err.println(String.format("With hits:     %,15d ", Basic.getSum(numberOfReadsWithHits)));
            System.err.println(String.format("Alignments:    %,15d", Basic.getSum(numberOfMatches)));

            for (int i = 0; i < countAssigned.length; i++) {
                System.err.println(String.format("%-19s%,11d", "Assig. " + cNames[i] + ":", Basic.getSum(countAssigned[i])));
            }

            // if used mate pairs, report here:
            if (Basic.getSum(numberAssignedViaMatePair) > 0) {
                System.err.println(String.format("Tax. ass. by mate:%,12d", Basic.getSum(numberAssignedViaMatePair)));
            }

            progress.setCancelable(false); // can't cancel beyond here because file could be left in undefined state

            doc.setNumberReads(Basic.getSum(numberOfReadsFound));

            // If min support percentage is set, set the min support:
            if (doc.getMinSupportPercent() > 0) {
                doc.setMinSupport((int) Math.max(1, (doc.getMinSupportPercent() / 100.0) * (Basic.getSum(numberOfReadsWithHits) + Basic.getSum(numberAssignedViaMatePair))));
                System.err.println("MinSupport set to: " + doc.getMinSupport());
            }

            // 2. apply min support and disabled taxa filter

            for (int i = 0; i < numberOfClassifications; i++) {
                final String cName = cNames[i];
                // todo: need to remove assignments to disabled ids when not using the LCA algorithm
                if (ProgramProperties.get(cName + "UseLCA", cName.equals(Classification.Taxonomy)) && (doc.getMinSupport() > 0 || ClassificationManager.get(cName, false).getIdMapper().getDisabledIds().size() > 0)) {
                    //System.err.println("Applying min-support filter to " + cName + "...");
                    progress.setSubtask("Applying min-support & disabled filter to " + cName + "...");
                    final MinSupportFilter minSupportFilter = new MinSupportFilter(cName, updateList.getClassIdToSizeMap(i), doc.getMinSupport(), progress);
                    final Map<Integer, Integer> changes = minSupportFilter.apply();

                    for (Integer srcId : changes.keySet()) {
                        updateList.appendClass(i, srcId, changes.get(srcId));
                    }
                    System.err.println(String.format("Min-supp. changes:%,12d", changes.size()));
                }
            }

            // 3. save classifications

            doc.getProgressListener().setSubtask("Writing classification tables");

            connector.updateClassifications(cNames, updateList, progress);
            connector.setNumberOfReads((int) doc.getNumberOfReads());

            // 4. sync
            progress.setSubtask("Syncing");
            SyncArchiveAndDataTable.syncRecomputedArchive2Summary(doc.getTitle(), "LCA", doc.getBlastMode(), doc.getParameterString(), connector, doc.getDataTable(), (int) doc.getAdditionalReads());

            if (progress instanceof ProgressPercentage)
                ((ProgressPercentage) progress).reportTaskCompleted();

            // MeganProperties.addRecentFile(new File(doc.getMeganFile().getFileName()));
            doc.setDirty(false);

            // report classification sizes:
            for (String cName : cNames) {
                System.err.println(String.format("Class. %-13s%,10d", cName + ":", connector.getClassificationSize(cName)));
            }

            return (int) doc.getDataTable().getTotalReads();
        } catch (IOException ex) {
            Basic.caught(ex);
            NotificationsInSwing.showInternalError("Data Processor failed: " + ex.getMessage());
        }
        return 0;
    }
}
