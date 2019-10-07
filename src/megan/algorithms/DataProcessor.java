/*
 *  Copyright (C) 2019 Daniel H. Huson
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

import jloda.swing.window.NotificationsInSwing;
import jloda.util.*;
import jloda.util.interval.Interval;
import jloda.util.interval.IntervalTree;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.core.ContaminantManager;
import megan.core.Document;
import megan.core.ReadAssignmentCalculator;
import megan.core.SyncArchiveAndDataTable;
import megan.data.*;
import megan.io.InputOutputReaderWriter;
import megan.rma6.RMA6File;
import megan.rma6.ReadBlockRMA6;

import java.io.IOException;
import java.util.*;

/**
 * Analyzes all reads in a sample
 * Daniel Huson, 1.2009, 3.2016
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
        try {
            progress.setTasks("Binning reads", "Initializing...");

            System.err.println("Initializing binning...");
            if (doc.isUseIdentityFilter()) {
                System.err.println("Using rank-specific min percent-identity values for taxonomic assignment of 16S reads");
            }

            final ContaminantManager contaminantManager;
            if (doc.isUseContaminantFilter() && doc.getDataTable().hasContaminants()) {
                contaminantManager = new ContaminantManager();
                contaminantManager.parseTaxonIdsString(doc.getDataTable().getContaminants());
                System.err.println(String.format("Using contaminants profile: %,d input, %,d total", contaminantManager.inputSize(), contaminantManager.size()));
            } else
                contaminantManager = null;

            final int numberOfClassifications = doc.getActiveViewers().size();
            final String[] cNames = doc.getActiveViewers().toArray(new String[numberOfClassifications]);
            final boolean[] useLCAForClassification = new boolean[numberOfClassifications];
            final int taxonomyIndex = Basic.getIndex(Classification.Taxonomy, cNames);
            for (int c = 0; c < numberOfClassifications; c++)
                if (c == taxonomyIndex) {
                    useLCAForClassification[c] = true;
                } else {
                    ClassificationManager.ensureTreeIsLoaded(cNames[c]);
                    useLCAForClassification[c] = ProgramProperties.get(cNames[c] + "UseLCA", cNames[c].equals(Classification.Taxonomy));
                }

            final UpdateItemList updateList = new UpdateItemList(numberOfClassifications);

            final boolean doMatePairs = doc.isPairedReads() && doc.getMeganFile().isRMA6File();

            if (doc.isPairedReads() && !doc.getMeganFile().isRMA6File())
                System.err.println("WARNING: Not an RMA6 file, will ignore paired read information");
            if (doMatePairs)
                System.err.println("Using paired reads in taxonomic assignment...");

            // step 0: set up classification algorithms

            final double minPercentReadToCover = doc.getMinPercentReadToCover();
            int numberOfReadsFailedCoveredThreshold = 0;
            final IntervalTree<Object> intervals;
            if (minPercentReadToCover > 0 && doc.isLongReads() || doc.getReadAssignmentMode() == Document.ReadAssignmentMode.alignedBases)
                intervals = new IntervalTree<>();
            else
                intervals = null;

            if (minPercentReadToCover > 0)
                System.err.println(String.format("Minimum percentage of read to be covered: %.1f%%", minPercentReadToCover));

            final boolean usingLongReadAlgorithm = (doc.getLcaAlgorithm() == Document.LCAAlgorithm.longReads);

            final IAssignmentAlgorithmCreator[] assignmentAlgorithmCreators = new IAssignmentAlgorithmCreator[numberOfClassifications];
            for (int c = 0; c < numberOfClassifications; c++) {
                if (c == taxonomyIndex) {
                    switch (doc.getLcaAlgorithm()) {
                        default:
                        case naive:
                            assignmentAlgorithmCreators[c] = new AssignmentUsingLCAForTaxonomyCreator(cNames[c], doc.isUseIdentityFilter(), doc.getLcaCoveragePercent());
                            break;
                        case weighted:
                            // we are assuming that taxonomy classification is The taxonomy classification
                            assignmentAlgorithmCreators[c] = new AssignmentUsingWeightedLCACreator(doc, doc.isUseIdentityFilter(), doc.getLcaCoveragePercent());
                            break;
                        case longReads:
                            assignmentAlgorithmCreators[c] = new AssignmentUsingIntervalUnionLCACreator(doc);
                            break;
                    }
                } else if (useLCAForClassification[c])
                    assignmentAlgorithmCreators[c] = new AssignmentUsingLCACreator(cNames[c]);
                else if (usingLongReadAlgorithm)
                    assignmentAlgorithmCreators[c] = new AssignmentUsingMultiGeneBestHitCreator(cNames[c], doc.getMeganFile().getFileName());
                else
                    assignmentAlgorithmCreators[c] = new AssignmentUsingBestHitCreator(cNames[c], doc.getMeganFile().getFileName());
            }

            final ReferenceCoverFilter referenceCoverFilter;
            if (doc.getMinPercentReferenceToCover() > 0) {
                referenceCoverFilter = new ReferenceCoverFilter(doc.getMinPercentReferenceToCover());
                referenceCoverFilter.compute(doc.getProgressListener(), doc.getConnector(), doc.getMinScore(), doc.getTopPercent(), doc.getMaxExpected(), doc.getMinPercentIdentity());
            } else
                referenceCoverFilter = null;

            // step 1:  stream through reads and assign classes

            long numberOfReadsFound = 0;
            double totalWeight = 0;
            long numberOfMatches = 0;
            long numberOfReadsWithLowComplexity = 0;
            long numberOfReadsWithHits = 0;
            long numberAssignedViaMatePair = 0;

            final int[] countUnassigned = new int[numberOfClassifications];
            final int[] countAssigned = new int[numberOfClassifications];

            final IAssignmentAlgorithm[] assignmentAlgorithm = new IAssignmentAlgorithm[numberOfClassifications];
            for (int c = 0; c < numberOfClassifications; c++)
                assignmentAlgorithm[c] = assignmentAlgorithmCreators[c].createAssignmentAlgorithm();

            final Set<Integer>[] knownIds = new HashSet[numberOfClassifications];
            for (int c = 0; c < numberOfClassifications; c++) {
                knownIds[c] = new HashSet<>();
                knownIds[c].addAll(ClassificationManager.get(cNames[c], true).getName2IdMap().getIds());
            }

            final IConnector connector = doc.getConnector();
            final InputOutputReaderWriter mateReader = doMatePairs ? new InputOutputReaderWriter(doc.getMeganFile().getFileName(), "r") : null;

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

            System.err.println("Binning reads...");
            progress.setTasks("Binning reads", "Analyzing alignments");

            try (final IReadBlockIterator it = connector.getAllReadsIterator(0, 10, false, true)) {
                progress.setMaximum(it.getMaximumProgress());
                progress.setProgress(0);

                final ReadBlockRMA6 mateReadBlock;
                if (doMatePairs) {
                    try (RMA6File RMA6File = new RMA6File(doc.getMeganFile().getFileName(), "r")) {
                        final String[] matchClassificationNames = RMA6File.getHeaderSectionRMA6().getMatchClassNames();
                        mateReadBlock = new ReadBlockRMA6(doc.getBlastMode(), true, matchClassificationNames);
                    }
                } else
                    mateReadBlock = null;

                while (it.hasNext()) {
                    progress.setProgress(it.getProgress());

                    // clean up previous values
                    for (int c = 0; c < numberOfClassifications; c++) {
                        classIds[c] = 0;
                        if (usingLongReadAlgorithm) {
                            moreClassIds[c].clear();
                            multiGeneWeights[c] = 0;
                        }
                    }

                    final IReadBlock readBlock = it.next();

                    if (readBlock.getNumberOfAvailableMatchBlocks() > 0)
                        numberOfReadsWithHits += readBlock.getReadWeight();

                    readBlock.setReadWeight(readAssignmentCalculator.compute(readBlock, intervals));

                    numberOfReadsFound++;
                    totalWeight += readBlock.getReadWeight();
                    numberOfMatches += readBlock.getNumberOfMatches();

                    final boolean hasLowComplexity = readBlock.getComplexity() > 0 && readBlock.getComplexity() + 0.01 < doc.getMinComplexity();

                    if (hasLowComplexity)
                        numberOfReadsWithLowComplexity += readBlock.getReadWeight();

                    int taxId = 0;
                    if (taxonomyIndex >= 0) {
                        final BitSet activeMatchesForTaxa = new BitSet(); // pre filter matches for taxon identification
                        ActiveMatches.compute(doc.getMinScore(), topPercentForActiveMatchFiltering, doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, Classification.Taxonomy, activeMatchesForTaxa);

                        if (referenceCoverFilter != null)
                            referenceCoverFilter.applyFilter(readBlock, activeMatchesForTaxa);

                        if (minPercentReadToCover == 0 || ensureCovered(minPercentReadToCover, readBlock, activeMatchesForTaxa, intervals)) {
                            if (doMatePairs && readBlock.getMateUId() > 0) {
                                mateReader.seek(readBlock.getMateUId());
                                mateReadBlock.read(mateReader, false, true, doc.getMinScore(), doc.getMaxExpected());
                                taxId = assignmentAlgorithm[taxonomyIndex].computeId(activeMatchesForTaxa, readBlock);
                                final BitSet activeMatchesForMateTaxa = new BitSet(); // pre filter matches for mate-based taxon identification
                                ActiveMatches.compute(doc.getMinScore(), topPercentForActiveMatchFiltering, doc.getMaxExpected(), doc.getMinPercentIdentity(), mateReadBlock, Classification.Taxonomy, activeMatchesForMateTaxa);
                                if (referenceCoverFilter != null)
                                    referenceCoverFilter.applyFilter(readBlock, activeMatchesForMateTaxa);

                                int mateTaxId = assignmentAlgorithm[taxonomyIndex].computeId(activeMatchesForMateTaxa, mateReadBlock);
                                if (mateTaxId > 0) {
                                    if (taxId <= 0) {
                                        taxId = mateTaxId;
                                        numberAssignedViaMatePair++;
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
                            numberOfReadsFailedCoveredThreshold++;
                        if (contaminantManager != null) {
                            if ((usingLongReadAlgorithm && contaminantManager.isContaminantLongRead(taxId)) || (!usingLongReadAlgorithm && contaminantManager.isContaminantShortRead(readBlock, activeMatchesForTaxa)))
                                taxId = IdMapper.CONTAMINANTS_ID;
                        }
                    }

                    for (int c = 0; c < numberOfClassifications; c++) {
                        int id;
                        if (taxId == IdMapper.CONTAMINANTS_ID) {
                            id = IdMapper.CONTAMINANTS_ID;
                        } else if (hasLowComplexity) {
                            id = IdMapper.LOW_COMPLEXITY_ID;
                        } else if (c == taxonomyIndex) {
                            id = taxId;
                        } else {
                            final BitSet activeMatchesForFunction = new BitSet(); // pre filter matches for taxon identification
                            ActiveMatches.compute(doc.getMinScore(), topPercentForActiveMatchFiltering, doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, cNames[c], activeMatchesForFunction);
                            if (referenceCoverFilter != null)
                                referenceCoverFilter.applyFilter(readBlock, activeMatchesForFunction);

                            id = assignmentAlgorithm[c].computeId(activeMatchesForFunction, readBlock);

                            if (id > 0 && usingLongReadAlgorithm && assignmentAlgorithm[c] instanceof IMultiAssignmentAlgorithm) {
                                int numberOfSegments = ((IMultiAssignmentAlgorithm) assignmentAlgorithm[c]).getAdditionalClassIds(c, numberOfClassifications, moreClassIds[c]);
                                multiGeneWeights[c] = (numberOfSegments > 0 ? (float) readBlock.getReadWeight() / (float) numberOfSegments : 0);
                            }
                        }

                        if (id <= 0 && readBlock.getNumberOfAvailableMatchBlocks() == 0)
                            id = IdMapper.NOHITS_ID;
                        else if (!knownIds[c].contains(id) && (!usingLongReadAlgorithm || !nonEmptyIntersection(knownIds[c], c, moreClassIds[c])))
                            id = IdMapper.UNASSIGNED_ID;

                        classIds[c] = id;
                        if (id == IdMapper.UNASSIGNED_ID)
                            countUnassigned[c]++;
                        else if (id > 0)
                            countAssigned[c]++;
                    }
                    updateList.addItem(readBlock.getUId(), readBlock.getReadWeight(), classIds);

                    if (usingLongReadAlgorithm) {
                        for (int c = 0; c < numberOfClassifications; c++) {
                            for (int[] classId : moreClassIds[c]) {
                                updateList.addItem(readBlock.getUId(), multiGeneWeights[c], classId);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                Basic.caught(ex);
            } finally {
                if (mateReader != null)
                    mateReader.close();
            }

            if (progress.isUserCancelled())
                throw new CanceledException();

            if (progress instanceof ProgressPercentage) {
                ((ProgressPercentage) progress).reportTaskCompleted();
            }

            System.err.println(String.format("Total reads:  %,15d", numberOfReadsFound));
            if (totalWeight > numberOfReadsFound)
                System.err.println(String.format("Total weight: %,15d", (long) totalWeight));

            if (numberOfReadsWithLowComplexity > 0)
                System.err.println(String.format("Low complexity:%,15d", numberOfReadsWithLowComplexity));
            if (numberOfReadsFailedCoveredThreshold > 0)
                System.err.println(String.format("Low covered:   %,15d", numberOfReadsFailedCoveredThreshold));

            System.err.println(String.format("With hits:     %,15d ", numberOfReadsWithHits));
            System.err.println(String.format("Alignments:    %,15d", numberOfMatches));

            for (int c = 0; c < numberOfClassifications; c++) {
                System.err.println(String.format("%-19s%,11d", "Assig. " + cNames[c] + ":", countAssigned[c]));
            }

            // if used mate pairs, report here:
            if (numberAssignedViaMatePair > 0) {
                System.err.println(String.format("Tax. ass. by mate:%,12d", numberAssignedViaMatePair));
            }

            progress.setCancelable(false); // can't cancel beyond here because file could be left in undefined state

            doc.setNumberReads(numberOfReadsFound);

            // If min support percentage is set, set the min support:
            if (doc.getMinSupportPercent() > 0) {
                doc.setMinSupport((int) Math.max(1, (doc.getMinSupportPercent() / 100.0) * (totalWeight)));
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

            connector.updateClassifications(cNames, updateList, progress);

            connector.setNumberOfReads((int) doc.getNumberOfReads());

            // 4. sync
            progress.setTasks("Binning reads", "Syncing");
            SyncArchiveAndDataTable.syncRecomputedArchive2Summary(doc.getReadAssignmentMode(), doc.getTitle(), "LCA", doc.getBlastMode(), doc.getParameterString(), connector, doc.getDataTable(), (int) doc.getAdditionalReads());

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

    /**
     * is one of the class ids known?
     *
     * @param knownIds
     * @param classId
     * @param moreClassIds
     * @return
     */
    private static boolean nonEmptyIntersection(Set<Integer> knownIds, int classId, ArrayList<int[]> moreClassIds) {
        for (int[] array : moreClassIds) {
            if (knownIds.contains(array[classId]))
                return true;
        }
        return false;
    }

    /**
     * check that enough of read is covered by alignments
     *
     * @param minCoveredPercent percent of read that must be covered
     * @param readBlock
     * @param activeMatches
     * @param intervals         this will be non-null in long read mode, in which case we check the total cover, otherwise, we check the amount covered by any one match
     * @return true, if sufficient coverage
     */
    private static boolean ensureCovered(double minCoveredPercent, IReadBlock readBlock, BitSet activeMatches, IntervalTree<Object> intervals) {
        int lengthToCover = (int) (0.01 * minCoveredPercent * readBlock.getReadLength());
        if (lengthToCover == 0)
            return true;

        if (intervals != null)
            intervals.clear();

        for (int m = activeMatches.nextSetBit(0); m != -1; m = activeMatches.nextSetBit(m + 1)) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
            if (Math.abs(matchBlock.getAlignedQueryEnd() - matchBlock.getAlignedQueryStart()) >= lengthToCover)
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
}
