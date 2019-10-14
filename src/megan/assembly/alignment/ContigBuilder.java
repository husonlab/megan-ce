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
package megan.assembly.alignment;

import jloda.graph.Node;
import jloda.util.*;
import megan.alignment.gui.Alignment;
import megan.alignment.gui.Lane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * builds contigs from paths and data
 * Daniel Huson, 5.2015
 */
public class ContigBuilder {
    private final ArrayList<Pair<String, String>> result;

    private final Node[][] paths;
    private final Node[] singles;
    private final List<Integer>[] readId2ContainedReads;

    private int countContigs;
    private int countSingletons;

    /**
     * constructor
     *
     * @param paths
     * @param singles
     */
    public ContigBuilder(Node[][] paths, Node[] singles, List<Integer>[] readId2ContainedReads) {
        this.paths = paths;
        this.singles = singles;
        this.readId2ContainedReads = readId2ContainedReads;
        result = new ArrayList<>();
    }

    /**
     * apply the algorith
     *
     * @param minReads
     * @param minCoverage
     * @param minLength
     */
    public void apply(int alignmentNumber, Alignment alignment, int minReads, double minCoverage, int minLength, boolean sortAlignmentByContigs, ProgressListener progress) throws CanceledException {
        progress.setSubtask("Building contigs");
        progress.setMaximum(paths.length);
        progress.setProgress(0);

        countContigs = 0;
        countSingletons = singles.length;

        for (Node[] contig : paths) {
            if (contig.length > 0) {
                countContigs++;
                final String contigName = (alignmentNumber == 0 ? String.format("Contig-%06d", countContigs) : String.format("Contig-%06d.%d", alignmentNumber, countContigs));

                final StringBuilder sequenceBuffer = new StringBuilder();
                int minCoordinate = Integer.MAX_VALUE;
                int maxCoordinate = Integer.MIN_VALUE;
                int totalBases = 0;
                int totalReads = 0;
                for (int i = 0; i < contig.length; i++) {
                    totalReads++;
                    int readId = (Integer) contig[i].getInfo();
                    if (readId2ContainedReads[readId] != null) {
                        totalReads += readId2ContainedReads[readId].size();
                        // System.err.println("Contained: " + readId2ContainedReads[readId].size());
                    }
                    final Lane iLane = alignment.getLane(readId);
                    minCoordinate = Math.min(minCoordinate, iLane.getFirstNonGapPosition());
                    maxCoordinate = Math.max(maxCoordinate, iLane.getLastNonGapPosition());

                    totalBases += iLane.getLastNonGapPosition() - iLane.getFirstNonGapPosition() + 1;
                    if (i + 1 < contig.length) {
                        int nextReadId = (Integer) contig[i + 1].getInfo();
                        int length = alignment.getLane(nextReadId).getFirstNonGapPosition() - iLane.getFirstNonGapPosition();
                        sequenceBuffer.append(iLane.getBlock(), 0, length);
                    } else {
                        sequenceBuffer.append(iLane.getBlock());
                    }
                }
                if (totalReads < minReads) {
                    continue;
                }

                // remove all gaps from contig. These are induced by other reads in other contigs, so not need to keep them
                // also,  this won't change the frame when processing BlastText alignments
                final String contigSequence = sequenceBuffer.toString().replaceAll("-", ""); // remove all gaps...
                if (contigSequence.length() < minLength) {
                    continue;
                }

                float coverage = (float) totalBases / Math.max(1.0f, contigSequence.length());
                if (coverage < minCoverage) {
                    continue;
                }

                final String referenceName = Basic.replaceSpaces(alignment.getReferenceName(), '_');

                final Pair<String, String> aContig = new Pair<>();
                aContig.setFirst(String.format(">%s\tlength=%d\treads=%d\tcoverage=%.1f\tref=%s\tcoords=%d..%d\n", contigName, contigSequence.length(), totalReads, coverage, Basic.swallowLeadingGreaterSign(referenceName), (minCoordinate + 1), (maxCoordinate + 1)));
                aContig.setSecond(contigSequence);

                System.err.print(aContig.getFirst());
                result.add(aContig);

            } else
                countSingletons++;
            progress.incrementProgress();
        }

        // sort contigs in alignment:
        if (sortAlignmentByContigs)
            sortAlignmentByContigs(alignment);

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
        return countContigs;
    }

    public int getCountSingletons() {
        return countSingletons;
    }

    /**
     * sorts the alignment by contigs
     *
     * @param alignment
     */
    private void sortAlignmentByContigs(final Alignment alignment) {
        Arrays.sort(paths, (a, b) -> {
            Integer posA = alignment.getLane((Integer) a[0].getInfo()).getFirstNonGapPosition();
            Integer posB = alignment.getLane((Integer) b[0].getInfo()).getFirstNonGapPosition();
            return posA.compareTo(posB);
        });

        Arrays.sort(paths, (a, b) -> -Integer.compare(a.length, b.length));

        // sort reads by contigs:
        List<Integer> order = new ArrayList<>(alignment.getNumberOfSequences());
        for (Node[] contig : paths) {
            for (Node v : contig) {
                {
                    Integer id = (Integer) v.getInfo();
                    order.add(id);
                }

                final List<Integer> contained = readId2ContainedReads[(Integer) v.getInfo()];
                if (contained != null) {
                    order.addAll(contained);
                }
            }
        }

        Basic.randomize(singles, 666);

        for (Node v : singles) {
            Integer id = (Integer) v.getInfo();
            order.add(id);
        }

        alignment.setOrder(order);
    }

}
