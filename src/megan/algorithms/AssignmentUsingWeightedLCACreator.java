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

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import jloda.util.ProgressPercentage;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.data.ClassificationFullTree;
import megan.classification.data.IntIntMap;
import megan.classification.data.Name2IdMap;
import megan.core.Document;
import megan.daa.connector.DAAConnector;
import megan.daa.connector.MatchBlockDAA;
import megan.daa.connector.ReadBlockDAA;
import megan.data.IConnector;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.viewer.TaxonomicLevels;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sets up the weighted-LCA algorithm
 * Daniel Huson, 2.2016
 */
public class AssignmentUsingWeightedLCACreator implements IAssignmentAlgorithmCreator {
    private static final Object syncTaxId2SpeciesId = new Object();
    private static IntIntMap taxId2SpeciesId;

    private int[] refId2weight;
    private Map<String, Integer> ref2weight; // map reference sequence to number of reads associated with it
    private final Object syncRef = new Object();

    private final String cName;
    private final boolean cNameIsTaxonomy;

    private final Name2IdMap name2IdMap;

    private final boolean useIdentityFilter;
    private final float percentToCover;

    /**
     * constructor
     */
    public AssignmentUsingWeightedLCACreator(final Document doc, final String cName, final float percentToCover) throws IOException, CanceledException {
        this.cName = cName;
        this.useIdentityFilter = doc.isUseIdentityFilter();
        ClassificationFullTree fullTree = ClassificationManager.get(cName, true).getFullTree();
        name2IdMap = ClassificationManager.get(cName, true).getName2IdMap();
        cNameIsTaxonomy = (cName.equals(Classification.Taxonomy));

        this.percentToCover = (percentToCover >= 99.9999 ? 100 : percentToCover);

        System.err.println(String.format("Using 'Weighted LCA' assignment (%.1f %%) on %s", this.percentToCover, cName));

        if (taxId2SpeciesId == null) {
            synchronized (syncTaxId2SpeciesId) {
                if (taxId2SpeciesId == null) {
                    taxId2SpeciesId = new IntIntMap(fullTree.getNumberOfNodes(), 0.999f);
                    ProgressListener progress = doc.getProgressListener();
                    progress.setSubtask("Computing taxon-to-species map");
                    progress.setMaximum(fullTree.getNumberOfNodes());
                    progress.setProgress(0);
                    computeTax2SpeciesMapRec(fullTree.getRoot(), 0, taxId2SpeciesId, progress);
                    if (progress instanceof ProgressPercentage)
                        ((ProgressPercentage) progress).reportTaskCompleted();
                }
            }
        }
        computeWeights(doc);
    }

    /**
     * recursively compute the taxon-id to species-id map
     *
     * @param v
     * @param taxId2SpeciesId
     * @return taxa below species
     */
    private void computeTax2SpeciesMapRec(final Node v, int speciesId, final IntIntMap taxId2SpeciesId, final ProgressListener progress) throws CanceledException {
        final int taxId = (Integer) v.getInfo();

        if (speciesId == 0) {
            if (name2IdMap.getRank(taxId) == TaxonomicLevels.getSpeciesId()) {
                speciesId = taxId;
                taxId2SpeciesId.put(taxId, speciesId);
            }
        } else
            taxId2SpeciesId.put(taxId, speciesId);

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e))
            computeTax2SpeciesMapRec(e.getTarget(), speciesId, taxId2SpeciesId, progress);
        progress.incrementProgress();
    }

    /**
     * compute all the reference weights
     *
     * @param doc
     */
    private void computeWeights(final Document doc) throws IOException, CanceledException {
        final IConnector connector = doc.getConnector();
        if (connector instanceof DAAConnector) {
            DAAConnector daaConnector = (DAAConnector) connector;
            refId2weight = new int[(int) daaConnector.getDAAHeader().getDbSeqsUsed()];
        } else
            ref2weight = new HashMap<>(10000000);

        final int numberOfThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        final ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

        final long[] totalMatches = new long[numberOfThreads];
        final long[] totalWeight = new long[numberOfThreads];

        final ArrayBlockingQueue<IReadBlock> queue = new ArrayBlockingQueue<>(1000);
        final IReadBlock sentinel = new ReadBlockDAA();

        final ProgressListener progress = doc.getProgressListener();
        progress.setSubtask("Computing weights");

        // we don't use multi-threaded code here because most effort is reading the data, not analysing it.
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNumber = i;
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        final BitSet activeMatches = new BitSet(); // pre filter matches for taxon identification

                        while (true) {
                            final IReadBlock readBlock = queue.take();
                            if (readBlock == sentinel)
                                break;

                            if (progress.isUserCancelled())
                                break;

                            ActiveMatches.compute(doc.getMinScore(), doc.getTopPercent(), doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, cName, activeMatches);

                            int chosenId = 0; // species id, if there is one, otherwise taxon id
                            for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
                                final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                                int id = (cNameIsTaxonomy ? matchBlock.getTaxonId() : matchBlock.getId(cName));
                                if (id > 0) {
                                    id = taxId2SpeciesId.get(id); // todo: there is a  problem here: what if the match is to a higher rank and that is incompatible with the majority species?
                                    if (id > 0) {
                                        if (chosenId == 0)
                                            chosenId = id;
                                        else if (chosenId != id) {
                                            chosenId = -1; // means mismatch
                                        }
                                    }
                                }
                                totalMatches[threadNumber]++;
                            }
                            if (chosenId > 0) {
                                for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
                                    final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                                    int id = (cNameIsTaxonomy ? matchBlock.getTaxonId() : matchBlock.getId(cName));
                                    if (id == chosenId) {
                                        if (ref2weight != null) {
                                            final String ref = matchBlock.getTextFirstWord();
                                            synchronized (syncRef) {
                                                final Integer count = Basic.replaceNull(ref2weight.get(ref), 0);
                                                ref2weight.put(ref, count + 1);
                                            }
                                        } else {
                                            final int refId = ((MatchBlockDAA) matchBlock).getSubjectId();
                                            synchronized (syncRef) {
                                                refId2weight[refId] += 1;
                                            }
                                        }
                                        totalWeight[threadNumber]++;
                                    }
                                }
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
        try (final IReadBlockIterator it = connector.getAllReadsIterator(doc.getMinScore(), doc.getMaxExpected(), false, true)) {
            progress.setMaximum(it.getMaximumProgress());
            progress.setProgress(0);

            while (it.hasNext()) {
                queue.put(it.next());
                progress.setProgress(it.getProgress());
            }
            for (int i = 0; i < numberOfThreads; i++) { // add one senitel for each thread
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

        if (progress instanceof ProgressPercentage)
            ((ProgressPercentage) progress).reportTaskCompleted();
        System.err.println(String.format("Total matches:    %,12d ", Basic.getSum(totalMatches)));
        System.err.println(String.format("Total references: %,12d ", (ref2weight != null ? ref2weight.size() : refId2weight.length)));
        System.err.println(String.format("Total weights:    %,12d ", Basic.getSum(totalWeight)));
        System.err.println();

        if (false) {
            SortedSet<String> refs = new TreeSet<>();
            refs.addAll(ref2weight.keySet());
            for (String ref : refs) {
                System.err.println(String.format("%s ->%5d", ref, ref2weight.get(ref)));
            }
        }
    }

    /**
     * creates a new assignment algorithm
     * use this repeatedly to create multiple assignment algorithms that can be run in parallel
     *
     * @return assignment algorithm
     */

    public IAssignmentAlgorithm createAssignmentAlgorithm() {
        return new AssignmentUsingWeightedLCA(cName, refId2weight, ref2weight, taxId2SpeciesId, percentToCover, useIdentityFilter);
    }
}

