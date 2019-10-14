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

import jloda.graph.*;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.ProgressListener;
import megan.alignment.gui.Alignment;
import megan.alignment.gui.Lane;

import java.util.*;

/**
 * builds the overlap graph
 * Daniel Huson, 5.2015
 */
public class OverlapGraphBuilder {
    private final Graph overlapGraph = new Graph();
    private final NodeArray<String> node2readName = new NodeArray<>(overlapGraph);
    private List<Integer>[] readId2ContainedReads;
    private final int minOverlap;

    /**
     * constructor
     *
     * @param minOverlap
     */
    public OverlapGraphBuilder(int minOverlap) {
        this.minOverlap = minOverlap;
    }

    /**
     * build the overlap graph
     *
     * @param alignment
     * @param progress
     * @return number of nodes
     * @throws CanceledException
     */
    public int apply(final Alignment alignment, ProgressListener progress) throws CanceledException {
        // alignment.resetOrder();

        if (progress != null) {
            progress.setSubtask("Building overlap graph");
            progress.setMaximum(alignment.getNumberOfSequences());
            progress.setProgress(0);
        }

        final Pair<Integer, Integer>[] list = new Pair[alignment.getNumberOfSequences()];
        final int[] numberOfLetters = new int[alignment.getNumberOfSequences()];

        for (int i = 0; i < alignment.getNumberOfSequences(); i++) {
            list[i] = new Pair<>(alignment.getLane(i).getFirstNonGapPosition(), i);
            numberOfLetters[i] = countLetters(alignment.getLane(i));
        }
        Arrays.sort(list, new Pair<>()); // sort by start position


        //  overlap graph. Each node is a read, each edge is a suffix-prefix overlap
        readId2ContainedReads = new List[alignment.getNumberOfSequences()];
        EdgeArray<Integer> edgeWeights = new EdgeArray<>(overlapGraph);

        {
            final Set<Integer> toDelete = new HashSet<>();
            // compute mapping to nodes:
            final Node[] i2node = new Node[alignment.getNumberOfSequences()];
            for (int i = 0; i < alignment.getNumberOfSequences(); i++) {
                i2node[i] = overlapGraph.newNode(i);
                node2readName.put(i2node[i], Basic.getFirstWord(alignment.getLane(i).getName()));
            }
            // compute edges and mark contained reads for removal
            for (int il = 0; il < list.length; il++) {
                final int i = list[il].getSecond();
                if (!toDelete.contains(i)) {
                    final Lane iLane = alignment.getLane(i);
                    final int iStart = iLane.getFirstNonGapPosition();
                    final int iEnd = iLane.getLastNonGapPosition();
                    for (int jl = il + 1; jl < list.length; jl++) {
                        final int j = list[jl].get2();
                        final Lane jLane = alignment.getLane(j);
                        final int jStart = jLane.getFirstNonGapPosition();
                        if (jStart > iEnd)
                            break; //

                        if (!toDelete.contains(j)) {
                            final int jEnd = jLane.getLastNonGapPosition();

                            if ((iStart < jStart || (iStart == jStart && i < j))) {
                                int numberOfLettersInOverlap = computeNumberOfLettersInPerfectOverlap(iLane, jLane);
                                if (iEnd >= jEnd && numberOfLettersInOverlap == numberOfLetters[j]) { // contained
                                    toDelete.add(j);
                                    List<Integer> contained = readId2ContainedReads[i];
                                    if (contained == null) {
                                        contained = readId2ContainedReads[i] = new ArrayList<>();
                                    }
                                    contained.add(j);
                                } else if (numberOfLettersInOverlap >= minOverlap) {
                                    overlapGraph.newEdge(i2node[i], i2node[j], numberOfLettersInOverlap);
                                }
                            }
                        }
                    }
                }
                if (progress != null)
                    progress.incrementProgress();
            }
            // remove all reads that are properly contained in some other read
            for (int i : toDelete) {
                overlapGraph.deleteNode(i2node[i]);
            }
        }

        if (progress != null)
            System.err.println("Overlap graph has " + overlapGraph.getNumberOfNodes() + " nodes and " + overlapGraph.getNumberOfEdges() + " edges");

        // assign weights to edges, weight is max path length that follows given edge
        for (Node v = overlapGraph.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getInDegree() == 0) {
                visitNodesRec(v, edgeWeights);
            }
        }
        return overlapGraph.getNumberOfNodes();
    }


    /**
     * gets the produced overlap graph
     *
     * @return overlap graph
     */
    public Graph getOverlapGraph() {
        return overlapGraph;
    }

    /**
     * gets the read-id to contained reads mapping
     *
     * @return mapping of read ids to contained reads
     */
    public List<Integer>[] getReadId2ContainedReads() {
        return readId2ContainedReads;
    }

    /**
     * count number of letters in sequence
     *
     * @param lane
     * @return number of letters
     */
    private int countLetters(Lane lane) {
        int count = 0;
        for (int i = lane.getFirstNonGapPosition(); i <= lane.getLastNonGapPosition(); i++) {
            if (Character.isLetter(lane.charAt(i)))
                count++;
        }
        return count;
    }

    /**
     * recursively visit all nodes and set edge weights
     *
     * @param v
     * @return path length
     */
    private int visitNodesRec(Node v, EdgeArray<Integer> edgeWeight) {
        int maxValue = 0;
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            if (edgeWeight.getValue(e) == null) {
                edgeWeight.put(e, visitNodesRec(e.getTarget(), edgeWeight) + 1);
            }
            maxValue = Math.max(maxValue, edgeWeight.getValue(e));
        }
        return maxValue;
    }

    /**
     * count the number of letters in the overlap between two lanes.
     *
     * @param iLane
     * @param jLane
     * @return number of letters in percent overlap, or 0, if mismatch encountered
     */
    private int computeNumberOfLettersInPerfectOverlap(Lane iLane, Lane jLane) {
        final int firstCoordinate = Math.max(iLane.getFirstNonGapPosition(), jLane.getFirstNonGapPosition());
        final int lastCoordinate = Math.min(iLane.getLastNonGapPosition(), jLane.getLastNonGapPosition());

        int count = 0;
        for (int i = firstCoordinate; i < lastCoordinate; i++) {
            char iChar = Character.toLowerCase(iLane.charAt(i));
            char jChar = Character.toLowerCase(jLane.charAt(i));

            if (iChar != jChar && iChar != 'n' && jChar != 'n')
                return 0;
            else if (Character.isLetter(iChar))
                count++;
        }
        return count;
    }

    /**
     * gets the the name of the read associated with a node
     *
     * @return read name
     */
    public NodeArray<String> getNode2ReadNameMap() {
        return node2readName;
    }
}
