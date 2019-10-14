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
import jloda.util.interval.IntervalChain;
import megan.daa.connector.ReadBlockDAA;
import megan.data.IConnector;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.util.BlastParsingUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Computes all references that are covered to the desired level and then can be used to filter matches
 * Daniel Huson, 3.2018
 */
public class ReferenceCoverFilter {
    private boolean isActive = false;
    private float proportionToCover = 0;
    private final Set<String> referencesToUse = new HashSet<>();

    private final int mask = 1023; // 2^10 - 1
    private final Object[] sync = new Object[mask + 1];

    /**
     * Constructor
     *
     * @param percentToCover
     */
    public ReferenceCoverFilter(float percentToCover) {
        Arrays.fill(sync, new Object());
        setPercentToCover(percentToCover);
    }

    /**
     * apply the filter
     *
     * @param connector
     * @param minScore
     * @param topPercent
     * @param maxExpected
     * @param minPercentIdentity
     */
    public void compute(ProgressListener progress, final IConnector connector, final float minScore, final float topPercent, final float maxExpected, final float minPercentIdentity) throws CanceledException, IOException {
        isActive = false;
        referencesToUse.clear();

        if (getPercentToCover() > 0) {
            final Map<String, Integer> ref2length = new HashMap<>();
            final Map<String, IntervalChain> ref2intervals = new HashMap<>();

            progress.setSubtask("Determining reference coverage");
            System.err.println(String.format("Running reference coverage filter with threshold=%.1f%%", getPercentToCover()));

            final int numberOfThreads = Math.min( ProgramExecutorService.getNumberOfCoresToUse(), connector.getNumberOfReads());
            if (numberOfThreads == 0)
                return; // no reads

            final ExecutorService service = ProgramExecutorService.createServiceForParallelAlgorithm(numberOfThreads);
            try {
                final CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);
                final IReadBlock sentinel = new ReadBlockDAA();

                final LinkedBlockingQueue<IReadBlock> queue = new LinkedBlockingQueue<>(1000);

                for (int t = 0; t < numberOfThreads; t++) {
                    service.submit(() -> {
                        try {
                            while (true) {
                                final IReadBlock readBlock = queue.take();
                                if (readBlock == sentinel)
                                    break;
                                final BitSet activeMatches = new BitSet(); // pre filter matches for taxon identification
                                ActiveMatches.compute(minScore, topPercent, maxExpected, minPercentIdentity, readBlock, null, activeMatches);
                                for (int m = activeMatches.nextSetBit(0); m != -1; m = activeMatches.nextSetBit(m + 1)) {
                                    final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
                                    final String refId = matchBlock.getTextFirstWord();
                                    synchronized (sync[refId.hashCode() & mask]) {
                                        if (ref2length.get(refId) == null)
                                            ref2length.put(refId, matchBlock.getRefLength());
                                        IntervalChain intervals = ref2intervals.get(refId);
                                        if (intervals == null) {
                                            intervals = new IntervalChain();
                                            ref2intervals.put(refId, intervals);
                                        }
                                        final String matchText = matchBlock.getText();
                                        final int start = BlastParsingUtils.getStartSubject(matchText);
                                        final int end = BlastParsingUtils.getEndSubject(matchText);
                                        intervals.add(start, end);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Basic.caught(e);
                        } finally {
                            countDownLatch.countDown();
                        }
                    });
                }

                // feed the queue
                try (final IReadBlockIterator it = connector.getAllReadsIterator(0, 10, false, true)) {
                    progress.setMaximum(it.getMaximumProgress());
                    progress.setProgress(0);

                    while (it.hasNext()) {
                        try {
                            queue.put(it.next());
                        } catch (InterruptedException e) {
                            Basic.caught(e);
                            break;
                        }
                        progress.setProgress(it.getProgress());
                    }
                    for (int i = 0; i < numberOfThreads; i++) {
                        try {
                            queue.put(sentinel);
                        } catch (InterruptedException e) {
                            Basic.caught(e);
                            break;
                        }
                    }
                }

                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } finally {
                service.shutdownNow();
            }

            for (String ref : ref2length.keySet()) {
                Integer length = ref2length.get(ref);
                if (length != null) {
                    IntervalChain intervalChain = ref2intervals.get(ref);
                    if (intervalChain != null && intervalChain.getLength() >= proportionToCover * length)
                        referencesToUse.add(ref);
                }
            }
            if (progress instanceof ProgressPercentage)
                ((ProgressPercentage) progress).reportTaskCompleted();

            System.err.println("Reference cover filter: using " + referencesToUse.size() + " of " + ref2intervals.size() + " references");
            if (referencesToUse.size() == ref2intervals.size()) {
                isActive = false;
                referencesToUse.clear(); // nothing filtered, might as well clear
            } else
                isActive = true;
        }
    }

    private Set<String> getReferencesToUse() {
        return referencesToUse;
    }

    public boolean useReference(String refId) {
        return !isActive || referencesToUse.contains(refId);
    }

    private float getPercentToCover() {
        return 100 * proportionToCover;
    }

    private void setPercentToCover(float percent) {
        this.proportionToCover = percent / 100.0f;
    }

    public boolean isActive() {
        return isActive;
    }

    /**
     * apply the filter to the set of active matches
     *
     * @param readBlock
     * @param activeMatches
     */
    public void applyFilter(IReadBlock readBlock, BitSet activeMatches) {
        if (isActive) {
            for (int m = activeMatches.nextSetBit(0); m != -1; m = activeMatches.nextSetBit(m + 1)) {
                String refId = readBlock.getMatchBlock(m).getTextFirstWord();
                if (!getReferencesToUse().contains(refId))
                    activeMatches.set(m, false);
            }
        }
    }
}
