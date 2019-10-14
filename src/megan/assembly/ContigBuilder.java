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
package megan.assembly;

import jloda.fx.util.ProgramExecutorService;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

/**
 * builds contigs from paths and data
 * Daniel Huson, 5.2015
 */
public class ContigBuilder {
    private final ArrayList<Pair<String, String>> result;
    private final Node[][] paths;
    private final List<Integer>[] readId2ContainedReads;

    /**
     * constructor
     *
     * @param paths
     */
    public ContigBuilder(Node[][] paths, List<Integer>[] readId2ContainedReads) {
        this.paths = paths;
        this.readId2ContainedReads = readId2ContainedReads;
        result = new ArrayList<>();
    }

    /**
     * apply the algorith
     *
     * @param minReads
     * @param minAvCoverage
     * @param minLength
     */
    public void apply(final ReadData[] reads, final int minReads, final double minAvCoverage, final int minLength, final ProgressListener progress) throws CanceledException {
        progress.setSubtask("Building contigs");
        progress.setMaximum(paths.length);
        progress.setProgress(0);

        if (paths.length == 0) {
            if (progress instanceof ProgressPercentage)
                ((ProgressPercentage) progress).reportTaskCompleted();
            return;
        }

        final int numberOfThreads =ProgramExecutorService.getNumberOfCoresToUse();
        final ExecutorService service = ProgramExecutorService.createServiceForParallelAlgorithm(numberOfThreads);
        final CountDownLatch countDownLatch = new CountDownLatch(paths.length);

        for (final Node[] path : paths) {
            service.submit(() -> {
                try {
                    int contigSize = path.length;
                    if (contigSize > 0) {
                        final StringBuilder sequenceBuffer = new StringBuilder();
                        int totalBases = 0;
                        int totalReads = 0;

                        // process the first read:
                        {
                            Node currentNode = path[0];
                            ReadData currentRead;

                            int currentReadId = (Integer) currentNode.getInfo();
                            currentRead = reads[currentReadId];
                            totalReads++;
                            int readId = (Integer) currentNode.getInfo();
                            if (readId2ContainedReads[readId] != null) {
                                totalReads += readId2ContainedReads[readId].size();
                            }
                            sequenceBuffer.append(currentRead.getSegment());
                            totalBases += currentRead.getSegment().length();
                        }

                        // process all other reads:
                        for (int i = 1; i < path.length; i++) {
                            Node prevNode = path[i - 1];
                            Node currentNode = path[i];
                            int nextReadId = (Integer) currentNode.getInfo();
                            totalReads++;
                            if (readId2ContainedReads[nextReadId] != null) {
                                totalReads += readId2ContainedReads[nextReadId].size();
                            }

                            final ReadData nextRead = reads[nextReadId];
                            Edge e = prevNode.getCommonEdge(currentNode);
                            int overlap = (Integer) e.getInfo();
                            sequenceBuffer.append(nextRead.getSegment().substring(overlap));
                            totalBases += nextRead.getSegment().length();
                        }

                        if (totalReads < minReads) {
                            return;
                        }

                        // remove all gaps from contig. These are induced by other reads in other contigs, so not need to keep them
                        // also,  this won't change the frame when processing BlastText alignments
                        final String contigSequence = sequenceBuffer.toString().replaceAll("-", ""); // remove all gaps...
                        if (contigSequence.length() < minLength) {
                            return;
                        }

                        float coverage = (float) totalBases / Math.max(1.0f, contigSequence.length());
                        if (coverage < minAvCoverage) {
                            return;
                        }

                        synchronized (result) {
                            final Pair<String, String> aContig = new Pair<>();
                            final String contigName = String.format("Contig-%06d", result.size() + 1);
                            aContig.setFirst(String.format(">%s length=%d reads=%d avCoverage=%.1f", contigName, contigSequence.length(), totalReads, coverage));
                            aContig.setSecond(contigSequence);
                            result.add(aContig);
                        }
                    }
                } finally {
                    countDownLatch.countDown();
                    try {
                        progress.incrementProgress();
                    } catch (CanceledException e) {
                        service.shutdownNow();
                        while (countDownLatch.getCount() > 0)
                            countDownLatch.countDown();
                    }
                }
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Basic.caught(e);
        }
        service.shutdownNow();

        if (progress instanceof ProgressPercentage)
            ((ProgressPercentage) progress).reportTaskCompleted();
    }

    /**
     * get the computed contigs
     *
     * @return contigs
     */
    public ArrayList<Pair<String, String>> getContigs() {
        return result;
    }

    public int getCountContigs() {
        return result.size();
    }
}
