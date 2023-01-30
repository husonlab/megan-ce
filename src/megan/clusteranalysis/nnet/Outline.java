/*
 * Outline.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package megan.clusteranalysis.nnet;

import javafx.geometry.Point2D;
import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloSplitsGraph;
import jloda.swing.graphview.PhyloTreeView;
import jloda.util.BitSetUtils;
import megan.clusteranalysis.tree.Taxa;

import java.util.*;
import java.util.function.Function;

/**
 * the outline algorithm for computing a phylogenetic outline
 * Daniel Huson, 1.2021
 */
public class Outline {

    private int ntax;

    /**
     * computes the graph and graph view
     *
	 */
    public void createNetwork(int[] cycle0, Taxa taxa, SplitSystem splits, PhyloTreeView view) {
        final boolean useWeights = true;

        ntax = taxa.getBits().cardinality();

        final PhyloSplitsGraph graph = (PhyloSplitsGraph) view.getGraph();
        graph.clear();

        final int[] cycle = normalizeCycle(cycle0);

        splits.addAllTrivial(taxa);

        for (int i = 1; i <= ntax; i++)
            graph.setTaxon2Cycle(cycle[i], i);

        final double[] split2angle = assignAnglesToSplits(ntax, splits, cycle,  360);

        final ArrayList<Event> events;
        {
            final ArrayList<Event> outbound = new ArrayList<>();
            final ArrayList<Event> inbound = new ArrayList<>();

            for (int s = 1; s <= splits.size(); s++) {
                final Split split = splits.getSplit(s);
                if (true) {
                    outbound.add(new Event(Event.Type.outbound, s, cycle, split));
                    inbound.add(new Event(Event.Type.inbound, s, cycle, split));
                }
            }
            events = Event.radixSort(ntax, outbound, inbound);
        }

        final BitSet currentSplits = new BitSet();
        Point2D location = new Point2D(0, 0);
        final Node start = graph.newNode();
        view.setLocation(start,new java.awt.geom.Point2D.Double(0,0));

        final NodeArray<Point2D> node2point=new NodeArray<>(graph);
        node2point.put(start, new Point2D(location.getX(), location.getY()));

        final Map<BitSet,Node> splits2node=new HashMap<>();

        splits2node.put(new BitSet(), start);

        Event previousEvent = null;

        // System.err.println("Algorithm:");
        // System.err.println("Start: " + start.getId());

        final BitSet taxaFound = new BitSet();

        Node previousNode = start;
        for (Event event : events) {
            // System.err.println(event);

            if (event.isStart()) {
                currentSplits.set(event.getS(), true);
                location = GeometryUtilsFX.translateByAngle(location, split2angle[event.getS()], useWeights ? event.getWeight() : 1);
            } else {
                currentSplits.set(event.getS(), false);
                location = GeometryUtilsFX.translateByAngle(location, split2angle[event.getS()] + 180, useWeights ? event.getWeight() : 1);
            }

            final boolean mustCreateNode = (splits2node.get(currentSplits) == null);
            final Node v;
            if (mustCreateNode) {
                v = graph.newNode();
                splits2node.put(BitSetUtils.copy(currentSplits), v);
                node2point.put(v, new Point2D(location.getX(), location.getY()));
                view.setLocation(v, new java.awt.geom.Point2D.Double(location.getX(), location.getY()));
            } else {
                v = splits2node.get(currentSplits);
                location = node2point.get(v);
            }
            // System.err.println("Node: " + v.getId());

            if (!v.isAdjacent(previousNode)) {
                final Edge e = graph.newEdge(previousNode, v);
                graph.setSplit(e, event.getS());
                graph.setWeight(e, useWeights ? event.getWeight() : 1);
                graph.setAngle(e, split2angle[event.getS()]);

                if (!mustCreateNode) // just closed loop
                {
                    // loops.add(createLoop(v, e));
                }
            }

            if (previousEvent != null) {
                if (event.getS() == previousEvent.getS()) {
                    for (int t : BitSetUtils.members(splits.getSplit(event.getS()).getPartNotContaining(cycle[1]))) {
                        graph.addTaxon(previousNode, t);
                        taxaFound.set(t);
                        graph.setLabel(previousNode,taxa.getLabel(t));
                    }
                }
            }

            previousNode = v;
            previousEvent = event;
        }

        for (int t = 1; t <= ntax; t++) {
            if (!taxaFound.get(t)) {
                graph.addTaxon(start, t);
                graph.setLabel(start, taxa.getLabel(t));
            }

        }

        if (false) {
            for (Node v : graph.nodes()) {
                // if (graph.getLabel(v) != null)
                System.err.println("Node " + v.getId() + " " + graph.getLabel(v) + " point: " + node2point.get(v));
            }
            for (Edge e : graph.edges()) {
                System.err.println("Edge " + e.getSource().getId() + " - " + e.getTarget().getId() + " split: " + graph.getSplit(e));
            }
        }


        view.resetViews();
    }

    /**
     * normalizes cycle so that cycle[1]=1
     *
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
     * assigns angles to all edges in the graph
     *
	 */
    public static double[] assignAnglesToSplits(int ntaxa, SplitSystem splits, int[] cycle, double totalAngle) {
        //We create the list of angles representing the positions on a circle.
        double[] angles = new double[ntaxa + 1];
        for (int t = 1; t <= ntaxa; t++) {
            angles[t] = (totalAngle * (t - 1) / (double) ntaxa) + 270 - 0.5 * totalAngle;
        }
        double[] split2angle = new double[splits.size() + 1];
        assignAnglesToSplits(ntaxa, angles, split2angle, splits, cycle);
        return split2angle;
    }


    /**
     * assigns angles to the splits in the graph, considering that they are located exactly "in the middle" of two taxa
     * so we fill split2angle using TaxaAngles.
     *
     * @param angles      for each taxa, its angle
     * @param split2angle for each split, its angle
     */
    private static void assignAnglesToSplits(int ntaxa, double[] angles, double[] split2angle, SplitSystem splits, int[] cycle) {
        for (int s = 1; s <= splits.size(); s++) {
            final Split split= splits.getSplit(s);
            int xp = 0; // first position of split part not containing taxon cycle[1]
            int xq = 0; // last position of split part not containing taxon cycle[1]
            final BitSet part = split.getPartNotContaining(cycle[1]);
            for (int i = 2; i <= ntaxa; i++) {
                int t = cycle[i];
                if (part.get(t)) {
                    if (xp == 0)
                        xp = i;
                    xq = i;
                }
            }

            split2angle[s] = GeometryUtilsFX.modulo360(0.5 * (angles[xp] + angles[xq]));

            //System.out.println("split from "+xp+","+xpneighbour+" ("+TaxaAngleP+") to "+xq+","+xqneighbour+" ("+TaxaAngleQ+") -> "+split2angle[s]+" $ "+(180 * (xp + xq)) / (double) ntaxa);s
        }
    }
    static class Event {
        enum Type {outbound, inbound}

        private final double weight;
        private int iPos;
        private int jPos;
        private final int s;
        private final Type type;

        public Event(Type type, int s, int[] cycle, Split split) {
            this.type = type;
            this.s = s;
            this.weight = split.getWeight();
            int firstInCycle = cycle[1];

            iPos = Integer.MAX_VALUE;
            jPos = Integer.MIN_VALUE;
            final BitSet farSide = split.getPartNotContaining(firstInCycle);
            for (int i = 0; i < cycle.length; i++) {
                final int t = cycle[i];
                if (t > 0 && farSide.get(t)) {
                    iPos = Math.min(iPos, i);
                    jPos = Math.max(jPos, i);
                }
            }
        }

        public int getS() {
            return s;
        }

        private int getIPos() {
            return iPos;
        }

        private int getJPos() {
            return jPos;
        }

        public double getWeight() {
            return weight;
        }

        public boolean isStart() {
            return type == Type.outbound;
        }

        public boolean isEnd() {
            return type == Type.inbound;
        }

        public String toString() {
            return type.name() + " S" + s + " (" + iPos + "-" + jPos + ")"; //: " + Basic.toString(split.getPartNotContaining(firstInCycle), ",");
        }

        public static ArrayList<Event> radixSort(int ntax, ArrayList<Event> outbound, ArrayList<Event> inbound) {
            countingSort(outbound, ntax, a -> ntax - a.getJPos());
            countingSort(outbound, ntax, Event::getIPos);
            countingSort(inbound, ntax, a -> ntax - a.getIPos());
            countingSort(inbound, ntax, Event::getJPos);

            return merge(outbound, inbound);
        }

        private static void countingSort(ArrayList<Event> events, int maxKey, Function<Event, Integer> key) {
            if (events.size() > 1) {
                final int[] key2pos = new int[maxKey + 1];
                // count keys
                for (Event event : events) {
                    final int value = key.apply(event);
                    key2pos[value]++;
                }

                // set positions
                {
                    int pos = 0;
                    for (int i = 0; i < key2pos.length; i++) {
                        final int add = key2pos[i];
                        key2pos[i] = pos;
                        pos += add;
                    }
                }

                final Event[] other = new Event[events.size()];

                // insert at positions:
                for (Event event : events) {
                    final int k = key.apply(event);
                    final int pos = key2pos[k]++;
                    other[pos] = event;
                }

                // copy to result:
                events.clear();
                Collections.addAll(events, other);
            }
        }

        private static ArrayList<Event> merge(ArrayList<Event> outbound, ArrayList<Event> inbound) {
            final ArrayList<Event> events = new ArrayList<>(outbound.size() + inbound.size());

            int ob = 0;
            int ib = 0;
            while (ob < outbound.size() && ib < inbound.size()) {
                if (outbound.get(ob).getIPos() < inbound.get(ib).getJPos() + 1) {
                    events.add(outbound.get(ob++));
                } else {
                    events.add(inbound.get(ib++));
                }
            }
            while (ob < outbound.size())
                events.add(outbound.get(ob++));
            while (ib < inbound.size())
                events.add(inbound.get(ib++));

            return events;
        }
    }
}
