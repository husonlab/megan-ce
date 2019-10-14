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
package megan.clusteranalysis.nnet;

import jloda.graph.*;
import jloda.phylo.PhyloGraph;
import jloda.phylo.PhyloSplitsGraph;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.PhyloTreeView;
import jloda.swing.util.Geometry;
import megan.clusteranalysis.tree.Taxa;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * The equalOverShorterOfBoth angle algorithm for embedding a circular splits graph
 *
 * @author huson
 * Date: 03-Jan-2004
 */
public class EqualAngle {

    private int ntax;

    /**
     * computes the graph and graph view
     *
     * @param cycle0
     * @param taxa
     * @param splits
     * @throws Exception
     */
    public void createNetwork(int[] cycle0, Taxa taxa, SplitSystem splits, PhyloTreeView view) throws Exception {
        ntax = taxa.getBits().cardinality();

        final PhyloSplitsGraph graph = (PhyloSplitsGraph) view.getGraph();
        graph.clear();

        int[] cycle = normalizeCycle(cycle0);

        for (int i = 1; i <= ntax; i++)
            graph.setTaxon2Cycle(cycle[i], i);


        initGraph(taxa, splits, cycle, graph);

        List<Integer> interiorSplits = getInteriorSplitsOrdered(taxa, splits);

        BitSet usedSplits = new BitSet();

        {
            for (Integer s : interiorSplits) {
                wrapSplit(taxa, splits, s, cycle, graph);
                usedSplits.set(s, true);
            }
        }

        removeTemporaryTrivialEdges(graph);
        assignAnglesToEdges(splits, cycle, graph, new HashSet());

        //We only assign angles to the new edges created (forbiddenSplits contains the list of splits BEFORE convex hull)
        assignAnglesToEdges(splits, cycle, graph, new HashSet());


        // rotateAbout so that edge leaving first taxon ist pointing at 9 o'clock
        boolean optionUseWeights = true;
        if (graph.getNumberOfNodes() > 0 && graph.getNumberOfEdges() > 0) {
            Node v = graph.getTaxon2Node(1);
            Edge e = graph.getFirstAdjacentEdge(v);
            double angle = Math.PI + graph.getAngle(e); // add pi to be consist with Embed
            for (e = graph.getFirstEdge(); e != null; e = graph.getNextEdge(e)) {
                graph.setAngle(e, graph.getAngle(e) - angle);
            }
            assignCoordinatesToNodes(optionUseWeights, view); // need coordinates
        } else
            assignCoordinatesToNodes(optionUseWeights, view);

        view.resetViews();
    }

    /**
     * initializes the graph
     *
     * @param taxa
     * @param splits
     * @param cycle
     * @param graph
     */
    private void initGraph(Taxa taxa, SplitSystem splits, int[] cycle, PhyloSplitsGraph graph) {
        // map from each taxon to it's trivial split in splits
        int[] taxon2split = new int[ntax + 1];

        for (int s = 1; s <= splits.size(); s++) {
            BitSet part = splits.getSplit(s).getA();
            if (part.cardinality() == ntax - 1) {
                part = splits.getSplit(s).getB();
            }
            if (part.cardinality() == 1) // is trivial split
            {
                int t = getMax(part, ntax);
                taxon2split[t] = s;
            }
        }

        Node center = graph.newNode();
        for (int i = 1; i <= ntax; i++) {
            int t = cycle[i];

            Node v = graph.newNode();

            graph.setLabel(v, taxa.getLabel(t));
            graph.addTaxon(v, t);

            Edge e = graph.newEdge(center, v);
            if (taxon2split[t] != 0) {
                int s = taxon2split[t];
                graph.setWeight(e, splits.getSplit(s).getWeight());
                graph.setSplit(e, s);
            } else
                graph.setSplit(e, -1); // mark as temporary split
        }
    }

    /**
     * get the maximum element in the given set
     *
     * @param set
     * @param max
     * @return max element in set
     */
    private int getMax(BitSet set, int max) {
        for (; max >= 0; max--)
            if (set.get(max))
                return max;
        return -1;
    }

    /**
     * returns the list of all non-trivial splits, ordered by by increasing size
     * of the split part containing taxon 1
     *
     * @param taxa
     * @param splits
     * @return non-trivial splits
     */
    private List<Integer> getInteriorSplitsOrdered(Taxa taxa, SplitSystem splits) {
        SortedSet<SplitCardinalityComparator> interiorSplits = new TreeSet<>(new SplitCardinalityComparator());

        for (int s = 1; s <= splits.size(); s++) {
            BitSet part = splits.getSplit(s).getA();
            if (part.cardinality() > 1 && part.cardinality() < ntax - 1) {
                if (!part.get(1))
                    part = splits.getSplit(s).getB();

                interiorSplits.add(new SplitCardinalityComparator(s, part.cardinality()));
            }
        }
        List<Integer> interiorSplitIDs = new LinkedList<>();
        for (SplitCardinalityComparator scc : interiorSplits) {
            interiorSplitIDs.add(scc.getSplitID());
        }
        return interiorSplitIDs;
    }

    /**
     * normalizes cycle so that cycle[1]=1
     *
     * @param cycle
     * @return normalized cycle
     */
    private int[] normalizeCycle(int[] cycle) {
        int[] result = new int[cycle.length];

        int i = 1;
        while (cycle[i] != 1)
            i++;
        int j = 1;
        while (i < cycle.length) {
            result[j] = cycle[i];
            i++;
            j++;
        }
        i = 1;
        while (j < result.length) {
            result[j] = cycle[i];
            i++;
            j++;
        }
        return result;
    }

    /**
     * adds an interior split using the wrapping algorithm
     *
     * @param taxa
     * @param cycle
     * @param splits
     * @param s
     * @param graph
     */
    private void wrapSplit(Taxa taxa, SplitSystem splits, int s, int[] cycle, PhyloSplitsGraph graph) throws Exception {
        BitSet part = (BitSet) (splits.getSplit(s).getA().clone());
        if (part.get(1))
            part = (BitSet) (splits.getSplit(s).getB().clone());

        int xp = 0; // first member of split part not containing taxon 1
        int xq = 0; // last member of split part not containing taxon 1
        for (int i = 1; i <= ntax; i++) {
            int t = cycle[i];
            if (part.get(t)) {
                if (xp == 0)
                    xp = t;
                xq = t;
            }
        }
        Node v = graph.getTaxon2Node(xp);
        Node z = graph.getTaxon2Node(xq);
        Edge targetLeafEdge = graph.getFirstAdjacentEdge(z);

        Edge e = graph.getFirstAdjacentEdge(v);
        v = graph.getOpposite(v, e);
        Node u = null;
        List<Edge> leafEdges = new LinkedList<>();
        leafEdges.add(e);
        Edge nextE;

        NodeSet nodesVisited = new NodeSet(graph);

        do {
            Edge f = e;
            if (nodesVisited.contains(v)) {
                System.err.println(graph);

                throw new Exception("Node already visited: " + v);
            }
            nodesVisited.add(v);

            Edge f0 = f; // f0 is edge by which we enter the node
            f = graph.getNextAdjacentEdgeCyclic(f0, v);
            while (isLeafEdge(f, graph)) {
                leafEdges.add(f);
                if (f == targetLeafEdge) {
                    break;
                }
                if (f == f0)
                    throw new RuntimeException("Node wraparound: f=" + f + " f0=" + f0);

                f = graph.getNextAdjacentEdgeCyclic(f, v);
            }
            if (isLeafEdge(f, graph))
                nextE = null; // at end of chain
            else
                nextE = f; // continue along boundary
            Node w = graph.newNode();
            Edge h = graph.newEdge(w, null, v, f0, Edge.AFTER, Edge.AFTER, null);
            // here we make sure that new edge is inserted after f0

            graph.setSplit(h, s);
            graph.setWeight(h, splits.getSplit(s).getWeight());
            if (u != null) {
                h = graph.newEdge(w, u, null);
                graph.setSplit(h, graph.getSplit(e));
                graph.setWeight(h, graph.getWeight(e));
            }
            for (Edge leafEdge : leafEdges) {
                f = leafEdge;
                h = graph.newEdge(w, graph.getOpposite(v, f));

                graph.setSplit(h, graph.getSplit(f));
                graph.setWeight(h, graph.getWeight(f));
                graph.deleteEdge(f);
            }
            leafEdges.clear();

            if (nextE != null) {
                v = graph.getOpposite(v, nextE);
                e = nextE;
                u = w;
            }
        } while (nextE != null);
    }

    /**
     * does this edge lead to a leaf?
     *
     * @param f
     * @param graph
     * @return is leaf edge
     */
    private boolean isLeafEdge(Edge f, PhyloGraph graph) {
        return graph.getDegree(graph.getSource(f)) == 1 || graph.getDegree(graph.getTarget(f)) == 1;

    }

    /**
     * this removes all temporary trivial edges added to the graph
     *
     * @param graph
     * @throws NotOwnerException
     */
    private void removeTemporaryTrivialEdges(PhyloSplitsGraph graph) {
        EdgeSet tempEdges = new EdgeSet(graph);
        for (Edge e = graph.getFirstEdge(); e != null; e = graph.getNextEdge(e)) {
            if (graph.getSplit(e) == -1) // temporary leaf edge
                tempEdges.add(e);
        }

        for (Edge e : tempEdges) {
            Node v, w;
            if (graph.getDegree(graph.getSource(e)) == 1) {
                v = graph.getSource(e);
                w = graph.getTarget(e);
            } else {
                w = graph.getSource(e);
                v = graph.getTarget(e);
            }
            for (int t : graph.getTaxa(v)) {
                graph.addTaxon(w, t);
            }

            if (graph.getLabel(w) != null && graph.getLabel(w).length() > 0)
                graph.setLabel(w, graph.getLabel(w) + ", " + graph.getLabel(v));
            else
                graph.setLabel(w, graph.getLabel(v));
            graph.clearTaxa(v);
            graph.setLabel(v, null);
            graph.deleteNode(v);
        }
    }

    /**
     * assigns angles to all edges in the graph
     *
     * @param splits
     * @param cycle
     * @param graph
     * @param forbiddenSplits : set of all the splits such as their edges won't have their angles changed
     */
    private void assignAnglesToEdges(SplitSystem splits, int[] cycle, PhyloSplitsGraph graph, Set forbiddenSplits) throws NotOwnerException {

        //We create the list of angles representing the taxas on a circle.
        double[] TaxaAngles = new double[ntax + 1];
        for (int t = 1; t < ntax + 1; t++) {
            TaxaAngles[t] = (Math.PI * 2 * t / (double) ntax);
        }

        double[] split2angle = new double[splits.size() + 1];

        assignAnglesToSplits(TaxaAngles, split2angle, splits, cycle);

        Iterator it = graph.edgeIterator();
        while (it.hasNext()) {
            Edge e = (Edge) it.next();
            if (!forbiddenSplits.contains(graph.getSplit(e))) {
                try {
                    graph.setAngle(e, split2angle[graph.getSplit(e)]);
                } catch (Exception ex) {
                    // silently ignore
                }
            }
        }
    }


    /**
     * assigns angles to the splits in the graph, considering that they are located exactly "in the middle" of two taxa
     * so we fill split2angle using TaxaAngles.
     *
     * @param splits
     * @param cycle
     * @param TaxaAngles  for each taxa, its angle
     * @param split2angle for each split, its angle
     */
    private void assignAnglesToSplits
    (double[] TaxaAngles, double[] split2angle, SplitSystem splits, int[] cycle) {

        for (int s = 1; s <= splits.size(); s++) {
            BitSet part = splits.getSplit(s).getA();
            if (part.get(1))
                part = splits.getSplit(s).getB();
            int xp = 0; // first position of split part not containing taxon 1
            int xq = 0; // last position of split part not containing taxon 1
            for (int i = 1; i <= ntax; i++) {
                int t = cycle[i];
                if (part.get(t)) {
                    if (xp == 0)
                        xp = i;
                    xq = i;
                }
            }

            int xpneighbour = (xp - 2) % ntax + 1;
            int xqneighbour = (xq) % ntax + 1;
            //the split, when represented on the circle of the taxas, is a line which interescts the circle in two
            //places : SplitsByAngle is a sorted list (sorted by the angle of these intersections), where every
            // split thus appears 2 times (once per instersection)
            double TaxaAngleP;
            double TaxaAngleQ;
            TaxaAngleP = Geometry.midAngle(TaxaAngles[xp], TaxaAngles[xpneighbour]);
            TaxaAngleQ = Geometry.midAngle(TaxaAngles[xq], TaxaAngles[xqneighbour]);

            split2angle[s] = Geometry.moduloTwoPI((TaxaAngleQ + TaxaAngleP) / 2);
            if (xqneighbour == 1) {
                split2angle[s] = Geometry.moduloTwoPI(split2angle[s] + Math.PI);
            }
            //System.out.println("split from "+xp+","+xpneighbour+" ("+TaxaAngleP+") to "+xq+","+xqneighbour+" ("+TaxaAngleQ+") -> "+split2angle[s]+" $ "+(Math.PI * (xp + xq)) / (double) ntaxa);s
        }
    }

    /**
     * assigns coordinates to nodes
     *
     * @param useWeights
     * @param view
     */
    private void assignCoordinatesToNodes(boolean useWeights, PhyloTreeView view) throws NotOwnerException {//, CanceledException {
        PhyloTree graph = (PhyloTree) view.getGraph();
        if (graph.getNumberOfNodes() == 0)
            return;
        final Node v = graph.getTaxon2Node(1);
        view.setLocation(v, new Point2D.Float(0, 0));

        BitSet splitsInPath = new BitSet();
        NodeSet nodesVisited = new NodeSet(graph);

        assignCoordinatesToNodesRec(v, splitsInPath, nodesVisited, useWeights, view);
    }


    /**
     * recursively assigns coordinates to all nodes
     *
     * @param v
     * @param splitsInPath
     * @param nodesVisited
     * @param useWeights
     * @param view
     */
    private void assignCoordinatesToNodesRec(Node v, BitSet splitsInPath, NodeSet nodesVisited, boolean useWeights, PhyloTreeView view)
            throws NotOwnerException {//, CanceledException {
        PhyloTree graph = (PhyloTree) view.getGraph();

        if (!nodesVisited.contains(v)) {
            //Deleted so that the user can cancel and it doesn't destroy everything: doc.getProgressListener().checkForCancel();
            nodesVisited.add(v);
            for (Edge e : v.adjacentEdges()) {
                int s = graph.getSplit(e);
                if (!splitsInPath.get(s)) {
                    Node w = graph.getOpposite(v, e);
                    Point2D p = Geometry.translateByAngle(view.getLocation(v),
                            graph.getAngle(e), useWeights ? graph.getWeight(e) : 1);
                    view.setLocation(w, p);
                    splitsInPath.set(s, true);
                    assignCoordinatesToNodesRec(w, splitsInPath, nodesVisited, useWeights, view);
                    splitsInPath.set(s, false);
                }
            }
        }
    }
}
