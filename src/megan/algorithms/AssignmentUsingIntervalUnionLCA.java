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

import jloda.graph.Edge;
import jloda.graph.Node;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.classification.data.ClassificationFullTree;
import megan.core.Document;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.viewer.TaxonomyData;

import java.util.*;

/**
 * performs taxonId assignment using a union-based algorithm
 * Created by huson on 4/12/17.
 */
public class AssignmentUsingIntervalUnionLCA implements IAssignmentAlgorithm {
    private final float weightedPercentFactor;
    private final float topPercent;
    private final ClassificationFullTree fullTree;

    // all these are used during computation:
    private final HashSet<Node> allNodes = new HashSet<>();
    private final HashMap<Integer, IntervalList> taxa2intervals = new HashMap<>();
    private final Map<Node, Integer> node2covered = new HashMap<>();

    private StartStopEvent[] events = new StartStopEvent[10000]; // not final because may get resized...
    private final Comparator<StartStopEvent> comparator;

    /**
     * constructor
     */
    public AssignmentUsingIntervalUnionLCA(Document doc) {
        this.weightedPercentFactor = Math.min(1f, doc.getLcaCoveragePercent() / 100.0f);
        this.topPercent = doc.getTopPercent();
        this.fullTree = ClassificationManager.get(Classification.Taxonomy, true).getFullTree();

        comparator = createComparator();
    }

    /**
     * compute taxonId id
     *
     * @param activeMatches
     * @param readBlock
     * @return taxonId id
     */
    public int computeId(BitSet activeMatches, IReadBlock readBlock) {
        if (readBlock.getNumberOfMatches() == 0)
            return IdMapper.NOHITS_ID;
        if (activeMatches.cardinality() == 0)
            return IdMapper.UNASSIGNED_ID;

        taxa2intervals.clear();
        computeTaxaToSegmentsMap(activeMatches, readBlock, taxa2intervals);

        if (taxa2intervals.size() == 0)
            return IdMapper.UNASSIGNED_ID;

        if (taxa2intervals.size() == 1)
            return taxa2intervals.keySet().iterator().next();

        allNodes.clear();
        final Node root = computeInducedTree(taxa2intervals, allNodes);

        node2covered.clear();
        computeCoveredBasesRec(root, allNodes, taxa2intervals, node2covered);

        final double threshold = weightedPercentFactor * node2covered.get(root);
        return getLCA(root, allNodes, node2covered, threshold);
    }

    /**
     * computes the taxon to segments map. On each segment, we apply the top-percent filter
     *
     * @param activeMatches
     * @param readBlock
     * @param taxa2intervals
     */
    private void computeTaxaToSegmentsMap(BitSet activeMatches, IReadBlock readBlock, HashMap<Integer, IntervalList> taxa2intervals) {
        // determine all start and stop events:
        int numberOfEvents = 0;
        for (int m = activeMatches.nextSetBit(0); m != -1; m = activeMatches.nextSetBit(m + 1)) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
            int taxonId = matchBlock.getTaxonId();
            if (taxonId > 0 && !TaxonomyData.isTaxonDisabled(taxonId)) {
                if (numberOfEvents + 1 >= events.length) { // need enough to add two new events
                    StartStopEvent[] tmp = new StartStopEvent[2 * events.length];
                    System.arraycopy(events, 0, tmp, 0, numberOfEvents);
                    events = tmp;
                }
                if (events[numberOfEvents] == null)
                    events[numberOfEvents] = new StartStopEvent();
                events[numberOfEvents++].set(true, Math.min(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd()), m);
                if (events[numberOfEvents] == null)
                    events[numberOfEvents] = new StartStopEvent();
                events[numberOfEvents++].set(false, Math.max(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd()), m);
            }
        }
        Arrays.sort(events, 0, numberOfEvents, comparator);

        final BitSet currentMatches = new BitSet(); // set of matches currently active
        final Map<Integer, Float> taxon2BestScore = new HashMap<>();

        StartStopEvent previousEvent = null;
        for (int c = 0; c < numberOfEvents; c++) {
            final StartStopEvent currentEvent = events[c];
            if (previousEvent == null) {
                if (!currentEvent.isStart())
                    throw new RuntimeException("Taxon end before begin: " + currentEvent);
                currentMatches.set(currentEvent.getMatchId());
            } else {
                if (currentEvent.getPos() > previousEvent.getPos()) {
                    final int segmentLength = (currentEvent.getPos() - previousEvent.getPos() + 1); // length of segment

                    if (segmentLength > 0) {
                        taxon2BestScore.clear();
                        for (int m = currentMatches.nextSetBit(0); m != -1; m = currentMatches.nextSetBit(m + 1)) {
                            final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
                            final int taxonId = matchBlock.getTaxonId(); // store the best score for each taxon

                            if (taxonId > 0 && !TaxonomyData.isTaxonDisabled(taxonId)) {
                                Float bestScore = taxon2BestScore.get(taxonId);
                                if (bestScore == null)
                                    taxon2BestScore.put(taxonId, matchBlock.getBitScore());
                                else
                                    taxon2BestScore.put(taxonId, Math.max(bestScore, matchBlock.getBitScore()));
                            }
                        }
                        // determine the top-percent threshold on the current segment:
                        float topPercentThreshold = 0;
                        for (Float value : taxon2BestScore.values()) {
                            topPercentThreshold = Math.max(topPercentThreshold, value);
                        }
                        topPercentThreshold = (100.0f - topPercent) / 100.0f * topPercentThreshold;

                        // add the segments for all taxa whose best match exceeds the threshold:
                        for (Integer taxonId : taxon2BestScore.keySet()) {
                            if (taxon2BestScore.get(taxonId) >= topPercentThreshold) {
                                IntervalList intervals = taxa2intervals.get(taxonId);
                                if (intervals == null) {
                                    intervals = new IntervalList();
                                    taxa2intervals.put(taxonId, intervals);
                                }
                                intervals.add(previousEvent.getPos(), currentEvent.getPos());
                            }
                        }
                    }
                }
                // update the set of current matches:
                if (currentEvent.isStart()) {
                    currentMatches.set(currentEvent.getMatchId());
                } else { // is end event
                    currentMatches.clear(currentEvent.getMatchId());
                }
            }
            previousEvent = currentEvent;
        }
        for (IntervalList list : taxa2intervals.values()) {
            list.setIsSorted(true); // initially, lists are sorted by construction
        }
    }

    /**
     * computes the set of all nodes that lie between the given taxa and their LCA
     *
     * @param taxa2intervals
     * @param allNodes
     * @return root node
     */
    private Node computeInducedTree(HashMap<Integer, IntervalList> taxa2intervals, Set<Node> allNodes) {
        // compute the local root node:
        final Node rootOfAllNodes;
        {
            final ArrayList<String> addresses = new ArrayList<>(taxa2intervals.size());
            for (Integer taxId : taxa2intervals.keySet()) {
                addresses.add(fullTree.getAddress(taxId));
            }
            final int rootId = fullTree.getAddress2Id(LCAAddressing.getCommonPrefix(addresses, false));
            rootOfAllNodes = fullTree.getANode(rootId);
        }

        allNodes.add(rootOfAllNodes);

        // add all nodes between that taxa and the root:
        for (Integer taxId : taxa2intervals.keySet()) {
            Node v = fullTree.getANode(taxId);
            if (v != null) {
                while (!allNodes.contains(v)) {
                    allNodes.add(v);
                    if (v.getInDegree() > 0)
                        v = v.getFirstInEdge().getSource();
                    else
                        break; // must be v==fullTree.getRoot()
                }
            }
        }
        return rootOfAllNodes;
    }

    /**
     * computes the number of bases that each taxon is covered by. Side effect is to change all taxa2intervals intervals.
     *
     * @param v
     * @param allNodes
     * @param taxa2intervals
     */
    private IntervalList computeCoveredBasesRec(final Node v, final HashSet<Node> allNodes, final HashMap<Integer, IntervalList> taxa2intervals, final Map<Node, Integer> node2covered) {
        final int taxId = (Integer) v.getInfo();

        final IntervalList intervals;
        if (taxa2intervals.get(taxId) != null)
            intervals = taxa2intervals.get(taxId);
        else {
            intervals = new IntervalList();
            taxa2intervals.put(taxId, intervals);
        }

        // get intervals of children:
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            final Node w = e.getTarget();
            if (allNodes.contains(w)) {
                final IntervalList intervalsW = computeCoveredBasesRec(w, allNodes, taxa2intervals, node2covered);
                intervals.addAll(intervalsW.getAll()); // this will trigger recomputation of amount covered
            }
        }
        node2covered.put(v, intervals.getCovered());
        return intervals;
    }

    /**
     * computes the node that is above all nodes whose coverage meets the threshold
     *
     * @param v
     * @param allNodes
     * @param node2covered
     * @param threshold
     * @return LCA of all nodes that meet the threshold
     */
    private int getLCA(Node v, HashSet<Node> allNodes, Map<Node, Integer> node2covered, double threshold) {
        while (true) {
            Node bestChild = null;

            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                final Node w = e.getTarget();
                if (allNodes.contains(w)) {
                    if (node2covered.get(w) >= threshold) {
                        if (bestChild == null)
                            bestChild = w;
                        else { // has at least two best children, return v
                            return (Integer) v.getInfo();
                        }
                    }
                }
            }
            if (bestChild != null)
                v = bestChild; // has exactly one child that beats threshold, move down to it
            else
                return (Integer) v.getInfo(); //  no best child, return v
        }
    }

    @Override
    public int getLCA(int id1, int id2) {
        if (id1 == 0)
            return id2;
        else if (id2 == 0)
            return id1;
        else
            return fullTree.getAddress2Id(LCAAddressing.getCommonPrefix(new String[]{fullTree.getAddress(id1), fullTree.getAddress(id2)}, 2, false));
    }

    private Comparator<StartStopEvent> createComparator() {
        return (a, b) -> {
            if (a.getPos() < b.getPos())
                return -1;
            else if (a.getPos() > b.getPos())
                return 1;
            else if (a.isStart() && b.isEnd())
                return -1;
            else if (a.isEnd() && b.isStart())
                return 1;
            else
                return 0;
        };
    }

    private static class StartStopEvent {
        private boolean start;
        private int pos;
        private int matchId;

        void set(boolean start, int pos, int matchId) {
            this.start = start;
            this.pos = pos;
            this.matchId = matchId;
        }

        boolean isStart() {
            return start;
        }

        boolean isEnd() {
            return !start;
        }

        int getPos() {
            return pos;
        }

        int getMatchId() {
            return matchId;
        }
    }
}
