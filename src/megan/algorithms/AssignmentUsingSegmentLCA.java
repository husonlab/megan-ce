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
 * performs taxonId assignment using a segment-oriented algorithm
 * Daniel Huson, 4.2017
 *
 * @deprecated
 */
public class AssignmentUsingSegmentLCA implements IAssignmentAlgorithm {
    private final double topPercentFactor;
    private final double coverFactor;
    private final ClassificationFullTree fullTree;

    // all these are used during computation:
    private final HashMap<Integer, Integer> tax2covered = new HashMap<>();
    private final Map<Node, Integer> node2covered = new HashMap<>();
    private final Set<Node> allNodes = new HashSet<>();

    private final Set<Integer> taxa = new HashSet<>();
    private final ArrayList<String> addresses = new ArrayList<>(taxa.size());
    private final BitSet currentMatches = new BitSet(); // set of matches currently active

    private StartStopEvent[] events = new StartStopEvent[10000]; // not final because may get resized...
    private final Comparator<StartStopEvent> comparator;

    /**
     * constructor
     */
    public AssignmentUsingSegmentLCA(Document doc) {
        this.coverFactor = doc.getLcaCoveragePercent() / 100.0;
        this.topPercentFactor = (100.0 - doc.getTopPercent()) / 100.0;
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

        tax2covered.clear();
        computeTaxaToCoveredMap(activeMatches, readBlock, tax2covered);

        if (tax2covered.size() == 0)
            return IdMapper.UNASSIGNED_ID;

        if (tax2covered.size() == 1)
            return tax2covered.keySet().iterator().next();

        allNodes.clear();
        final Node root = computeInducedTree(tax2covered.keySet(), allNodes);

        node2covered.clear();
        computeNodes2CoveredRec(root, allNodes, tax2covered, node2covered);

        return computeBestTaxon(root, node2covered);
    }

    /**
     * computes the taxon to segments map. On each segment, we apply the top-percent filter
     *
     * @param activeMatches
     * @param readBlock
     * @param taxa2covered
     */
    private void computeTaxaToCoveredMap(BitSet activeMatches, IReadBlock readBlock, HashMap<Integer, Integer> taxa2covered) {
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

        final boolean debug = false;

        currentMatches.clear();

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
                        // determine the top-percent threshold on the current segment:
                        double topPercentThreshold = 0;
                        for (int m = currentMatches.nextSetBit(0); m != -1; m = currentMatches.nextSetBit(m + 1)) {
                            final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
                            final int taxonId = matchBlock.getTaxonId(); // store the best score for each taxon
                            if (taxonId > 0 && !TaxonomyData.isTaxonDisabled(taxonId)) {
                                topPercentThreshold = Math.max(topPercentThreshold, matchBlock.getBitScore() / Math.abs(matchBlock.getAlignedQueryEnd() - matchBlock.getAlignedQueryStart()));
                            }
                        }
                        topPercentThreshold = topPercentFactor * topPercentThreshold;

                        taxa.clear();
                        for (int m = currentMatches.nextSetBit(0); m != -1; m = currentMatches.nextSetBit(m + 1)) {
                            final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
                            final int taxonId = matchBlock.getTaxonId(); // store the best score for each taxon
                            if (taxonId > 0 && !TaxonomyData.isTaxonDisabled(taxonId) && (matchBlock.getBitScore() / Math.abs(matchBlock.getAlignedQueryEnd() - matchBlock.getAlignedQueryStart())) >= topPercentThreshold) {
                                taxa.add(taxonId);
                            }
                        }
                        final int lca;
                        {
                            addresses.clear();
                            for (Integer taxId : taxa) {
                                addresses.add(fullTree.getAddress(taxId));
                            }
                            lca = fullTree.getAddress2Id(LCAAddressing.getCommonPrefix(addresses, false));
                        }
                        taxa2covered.merge(lca, segmentLength, Integer::sum);
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
    }

    /**
     * computes the set of all nodes that lie between the given taxa and their LCA
     *
     * @param taxa
     * @param allNodes
     * @return root node
     */
    private Node computeInducedTree(final Set<Integer> taxa, final Set<Node> allNodes) {
        // compute the root node:
        final Node root;
        if (taxa.contains(1))
            root = fullTree.getRoot();
        else {
            addresses.clear();
            for (Integer taxId : taxa) {
                addresses.add(fullTree.getAddress(taxId));
            }
            final int rootId = fullTree.getAddress2Id(LCAAddressing.getCommonPrefix(addresses, false));
            root = fullTree.getANode(rootId);
        }

        allNodes.add(root);

        // add all nodes between that taxa and the root:
        for (Integer taxId : taxa) {
            Node v = fullTree.getANode(taxId);
            while (!allNodes.contains(v)) {
                allNodes.add(v);
                if (v.getInDegree() > 0)
                    v = v.getFirstInEdge().getSource();
                else
                    throw new RuntimeException("No root");
            }
        }
        return root;
    }

    /**
     * computes the node to covered mapping for all nodes in induced tree
     *
     * @param v
     * @param allNodes
     * @param tax2covered
     * @param node2covered
     */
    private int computeNodes2CoveredRec(final Node v, Set<Node> allNodes, HashMap<Integer, Integer> tax2covered, Map<Node, Integer> node2covered) {
        Integer count = tax2covered.get(v.getInfo());
        if (count == null)
            count = 0;

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            final Node w = e.getTarget();
            if (allNodes.contains(w)) {
                count += computeNodes2CoveredRec(w, allNodes, tax2covered, node2covered);
            }
        }
        node2covered.put(v, count);
        //System.err.println(TaxonomyData.getName2IdMap().get((Integer)v.getInfo())+": covered="+count);

        return count;
    }


    /**
     * computes the lowest taxon above the given percentage of the total weight
     *
     * @param v
     * @param node2covered
     * @return best taxon
     */
    private int computeBestTaxon(Node v, Map<Node, Integer> node2covered) {
        final double toCover = coverFactor * node2covered.get(v);

        while (true) {
            Node goodChild = null;
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                final Node w = e.getTarget();
                if (node2covered.containsKey(w)) {
                    if (node2covered.get(w) >= toCover) {
                        goodChild = w;
                        break;
                    }
                }
            }
            if (goodChild == null)
                return (Integer) v.getInfo();
            else
                v = goodChild;
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
