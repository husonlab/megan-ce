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

import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import jloda.util.ProgressPercentage;

import java.util.*;

/**
 * assembles a set of reads that align to a specific class in some classification
 * <p>
 * Daniel Huson, 5.2015
 */
public class OverlapGraphBuilder {
    private final Graph overlapGraph = new Graph();
    private final NodeArray<String> node2readName = new NodeArray<>(overlapGraph);
    private List<Integer>[] readId2ContainedReads;
    private ReadData[] readDatas;
    private int minOverlap;
    private final boolean verbose;

    /**
     * constructor
     */
    public OverlapGraphBuilder(int minOverlap, boolean verbose) {
        this.minOverlap = minOverlap;
        this.verbose = verbose;
    }

    /**
     * apply
     *
     * @param readData
     * @param progress
     * @throws CanceledException
     */
    public void apply(final List<ReadData> readData, final ProgressListener progress) throws CanceledException {
        readDatas = readData.toArray(new ReadData[0]);
        // collect all matches for each reference:

        progress.setSubtask("Sorting reads and matches by reference");
        progress.setMaximum(readDatas.length);
        progress.setProgress(0);

        readId2ContainedReads = new List[readDatas.length];

        long countPairs = 0;
        Map<String, SortedSet<MatchData>> ref2matches = new HashMap<>();

        for (int r = 0; r < readDatas.length; r++) {
            final ReadData read = readDatas[r];
            if (read.getMatches() != null) {
                for (int m = 0; m < read.getMatches().length; m++) {
                    final MatchData match = read.getMatches()[m];
                    SortedSet<MatchData> set = ref2matches.get(match.getRefName());
                    if (set == null) {
                        set = new TreeSet<>(new MatchData());
                        ref2matches.put(match.getRefName(), set);
                    }
                    set.add(match);
                    countPairs++;
                }
            }
            progress.setProgress(r);
        }
        if (progress instanceof ProgressPercentage)
            ((ProgressPercentage) progress).reportTaskCompleted();
        if (verbose)
            System.err.println(String.format("Overlaps:   %,10d", countPairs));

        buildOverlapGraph(readDatas, ref2matches, minOverlap);
    }


    /**
     * build the overlap graph
     *
     * @param reads
     * @param ref2matches
     */
    private void buildOverlapGraph(ReadData[] reads, Map<String, SortedSet<MatchData>> ref2matches, int minOverlap) {
        final Node[] nodes = new Node[reads.length];

        final BitSet containedReadIds = new BitSet();

        for (String refName : ref2matches.keySet()) {
            final MatchData[] matches = ref2matches.get(refName).toArray(new MatchData[0]);

            for (int i = 0; i < matches.length; i++) {
                final MatchData iMatch = matches[i];

                if (!containedReadIds.get(iMatch.getRead().getId())) {
                    Node v = nodes[iMatch.getRead().getId()];
                    if (v == null) {
                        v = nodes[iMatch.getRead().getId()] = overlapGraph.newNode(iMatch.getRead().getId());
                        node2readName.setValue(v, iMatch.getRead().getName());
                    }

                    for (int j = i + 1; j < matches.length; j++) {
                        final MatchData jMatch = matches[j];
                        if (3 * (iMatch.getLastPosInRef() - jMatch.getFirstPosInRef()) <= minOverlap)
                            break; // no chance of an overlap

                        int overlapLength = computePerfectOverlapLength(iMatch, jMatch);
                        if (overlapLength > 0 && jMatch.getLastPosInRef() <= iMatch.getLastPosInRef()) { // contained
                            containedReadIds.set(jMatch.getRead().getId());
                            List<Integer> contained = readId2ContainedReads[i];
                            if (contained == null) {
                                contained = readId2ContainedReads[i] = new ArrayList<>();
                            }
                            contained.add(j);
                        } else if (overlapLength >= minOverlap) {
                            Node w = nodes[jMatch.getRead().getId()];
                            if (w == null) {
                                w = nodes[jMatch.getRead().getId()] = overlapGraph.newNode(jMatch.getRead().getId());
                                node2readName.setValue(w, jMatch.getRead().getName());
                            }

                            final Edge e = overlapGraph.getCommonEdge(v, w);
                            if (e == null) {
                                overlapGraph.newEdge(v, w, overlapLength);
                            } else if ((Integer) e.getInfo() < overlapLength) {
                                e.setInfo(overlapLength);
                            }
                        }
                    }
                }
            }
        }
        if (verbose) {
            System.err.println(String.format("Graph nodes:%,10d", overlapGraph.getNumberOfNodes()));
            System.err.println(String.format("Graph edges:%,10d", overlapGraph.getNumberOfEdges()));
            System.err.println(String.format("Cont. reads:%,10d", containedReadIds.cardinality()));
        }
    }

    /**
     * computess the number of matching letters, else returns 0
     *
     * @param iMatch
     * @param jMatch
     * @return number of matching letters or 0
     */
    private int computePerfectOverlapLength(MatchData iMatch, MatchData jMatch) {
        try {
            int first = Math.max(iMatch.getFirstPosInRef(), jMatch.getFirstPosInRef());
            int last = Math.min(iMatch.getLastPosInRef(), jMatch.getLastPosInRef());

            int count = 0;
            for (int refPos = first; refPos <= last; refPos++) {
                for (int k = 0; k < 3; k++) {
                    int iPos = 3 * (refPos - iMatch.getFirstPosInRef()) + k;
                    int jPos = 3 * (refPos - jMatch.getFirstPosInRef()) + k;
                    char iChar = Character.toLowerCase(iMatch.getRead().getSegment().charAt(iPos));
                    char jChar = Character.toLowerCase(jMatch.getRead().getSegment().charAt(jPos));

                    if (iChar != jChar && iChar != 'n' && jChar != 'n')
                        return 0;
                    else if (Character.isLetter(iMatch.getRead().getSegment().charAt(iPos)))
                        count++;
                }
            }
            return count;
        } catch (Exception ex) {
            return 0;
        }
    }


    /**
     * get the overlap graph
     *
     * @return overlap graph
     */
    public Graph getOverlapGraph() {
        return overlapGraph;
    }

    /**
     * get the readDatas associated with the overlap graph
     *
     * @return readDatas
     */
    public ReadData[] getReadId2ReadData() {
        return readDatas;
    }

    /**
     * gets the the name of the read associated with a node
     *
     * @return read name
     */
    public NodeArray<String> getNode2ReadNameMap() {
        return node2readName;
    }


    public int getMinOverlap() {
        return minOverlap;
    }

    public void setMinOverlap(int minOverlap) {
        this.minOverlap = minOverlap;
    }

    public List<Integer>[] getReadId2ContainedReads() {
        return readId2ContainedReads;
    }
}
