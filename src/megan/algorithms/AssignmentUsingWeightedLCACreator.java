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

import jloda.fx.util.ProgramExecutorService;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import jloda.util.ProgressPercentage;
import megan.classification.Classification;
import megan.core.Document;
import megan.daa.connector.DAAConnector;
import megan.daa.connector.MatchBlockDAA;
import megan.daa.connector.ReadBlockDAA;
import megan.data.IConnector;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

/**
 * Sets up the weighted-LCA algorithm
 * Daniel Huson, 2.2016
 */
public class AssignmentUsingWeightedLCACreator implements IAssignmentAlgorithmCreator {
    private int[] refId2weight;
    private Map<String, Integer> ref2weight; // map reference sequence to number of reads associated with it
    private final Object syncRef = new Object();

    private final boolean useIdentityFilter;
    private final float percentToCover;

    private final String cName = Classification.Taxonomy;

    private final Taxon2SpeciesMapping taxon2SpeciesMapping;

    /**
     * constructor
     */
    public AssignmentUsingWeightedLCACreator(final Document doc, final boolean usingIdentityFilter, final float percentToCover) throws IOException, CanceledException {
        this.useIdentityFilter = usingIdentityFilter;

        this.taxon2SpeciesMapping = Taxon2SpeciesMapping.getInstance(doc.getProgressListener());

        this.percentToCover = (percentToCover >= 99.9999 ? 100 : percentToCover);

        System.err.println(String.format("Using 'Weighted LCA' assignment (%.1f %%) on %s", this.percentToCover, cName));

        computeWeights(doc);
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

        final int numberOfThreads =ProgramExecutorService.getNumberOfCoresToUse();
        final ExecutorService executorService = ProgramExecutorService.createServiceForParallelAlgorithm(numberOfThreads);
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

        final long[] totalMatches = new long[numberOfThreads];
        final long[] totalWeight = new long[numberOfThreads];

        final ArrayBlockingQueue<IReadBlock> queue = new ArrayBlockingQueue<>(1000);
        final IReadBlock sentinel = new ReadBlockDAA();

        final ProgressListener progress = doc.getProgressListener();
        progress.setSubtask("Computing weights");

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNumber = i;
            executorService.submit(() -> {
                try {
                    final BitSet activeMatches = new BitSet(); // pre filter matches for taxon identification

                    while (true) {
                        final IReadBlock readBlock = queue.take();
                        if (readBlock == sentinel)
                            break;

                        if (progress.isUserCancelled())
                            break;

                        ActiveMatches.compute(doc.getMinScore(), doc.getTopPercent(), doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, cName, activeMatches);
                        totalMatches[threadNumber] += activeMatches.cardinality();

                        int speciesId = 0; // assigns weights at the species level
                        for (int i1 = activeMatches.nextSetBit(0); i1 != -1; i1 = activeMatches.nextSetBit(i1 + 1)) {
                            final IMatchBlock matchBlock = readBlock.getMatchBlock(i1);
                            int id = matchBlock.getTaxonId();
                            if (id > 0) {
                                id = taxon2SpeciesMapping.getSpecies(id); // todo: there is a potential problem here: what if the match is to a higher rank and that is incompatible with the majority species?
                                if (id > 0) {
                                    if (speciesId == 0)
                                        speciesId = id;
                                    else if (speciesId != id) {
                                        speciesId = -1; // means mismatch
                                        break;
                                    }
                                }
                            }
                        }

                        if (speciesId > 0) {
                            for (int i1 = activeMatches.nextSetBit(0); i1 != -1; i1 = activeMatches.nextSetBit(i1 + 1)) {
                                final IMatchBlock matchBlock = readBlock.getMatchBlock(i1);
                                int id = matchBlock.getTaxonId();
                                if (id > 0) {
                                    id = taxon2SpeciesMapping.getSpecies(id);
                                    if (id == speciesId) {
                                        if (ref2weight != null) {
                                            final String ref = matchBlock.getTextFirstWord();
                                            synchronized (syncRef) {
                                                final Integer count = Basic.replaceNull(ref2weight.get(ref), 0);
                                                ref2weight.put(ref, count + Math.max(1, readBlock.getReadWeight()));
                                            }
                                        } else {
                                            final int refId = ((MatchBlockDAA) matchBlock).getSubjectId();
                                            synchronized (syncRef) {
                                                refId2weight[refId] += Math.max(1, readBlock.getReadWeight());
                                            }
                                        }
                                        totalWeight[threadNumber] += Math.max(1, readBlock.getReadWeight());
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    Basic.caught(ex);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        /*
         * feed the queue:
         */
        try (final IReadBlockIterator it = connector.getAllReadsIterator(doc.getMinScore(), doc.getMaxExpected(), false, true)) {
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

        if (progress instanceof ProgressPercentage)
            ((ProgressPercentage) progress).reportTaskCompleted();
        System.err.println(String.format("Total matches:    %,12d ", Basic.getSum(totalMatches)));
        System.err.println(String.format("Total references: %,12d ", (ref2weight != null ? ref2weight.size() : refId2weight.length)));
        System.err.println(String.format("Total weights:    %,12d ", Basic.getSum(totalWeight)));
        System.err.println();

    }

    /**
     * creates a new assignment algorithm
     * use this repeatedly to create multiple assignment algorithms that can be run in parallel
     *
     * @return assignment algorithm
     */

    public AssignmentUsingWeightedLCA createAssignmentAlgorithm() {
        return new AssignmentUsingWeightedLCA(cName, refId2weight, ref2weight, taxon2SpeciesMapping, percentToCover, useIdentityFilter);
    }
}

