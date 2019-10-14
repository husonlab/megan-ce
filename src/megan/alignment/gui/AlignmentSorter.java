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
package megan.alignment.gui;

import jloda.graph.*;
import jloda.util.Basic;
import jloda.util.Pair;

import java.util.*;

/**
 * sort rows in alignment
 * Daniel Huson, 9.2011
 */
public class AlignmentSorter {
    /**
     * sort by original order
     *
     * @param alignment
     */
    public static void sortByOriginalOrder(final Alignment alignment) {
        alignment.resetOrder();
    }

    /**
     * sort rows alphabetically by name
     *
     * @param alignment
     */
    public static void sortByName(final Alignment alignment, final boolean descending) {
        Integer[] array = new Integer[alignment.getNumberOfSequences()];
        for (int i = 0; i < alignment.getNumberOfSequences(); i++)
            array[i] = alignment.getOrder(i);
        alignment.resetOrder(); // need this so that getName etc gets the correct name

        Arrays.sort(array, (a, b) -> {
            int value = alignment.getName(a).compareTo(alignment.getName(b));
            return descending ? -value : value;
        });
        alignment.setOrder(Arrays.asList(array));
    }

    /**
     * sort rows by start
     *
     * @param alignment
     */
    public static void sortByStart(final Alignment alignment, final boolean descending) {
        Integer[] array = new Integer[alignment.getNumberOfSequences()];
        for (int i = 0; i < alignment.getNumberOfSequences(); i++)
            array[i] = alignment.getOrder(i);
        alignment.resetOrder();

        Arrays.sort(array, (a, b) -> {
            int sA = alignment.getLane(a).getFirstNonGapPosition();
            int sB = alignment.getLane(b).getFirstNonGapPosition();
            return descending ? sB - sA : sA - sB;
        });
        alignment.setOrder(Arrays.asList(array));
    }

    /**
     * sort rows by similarity
     *
     * @param alignment
     */
    public static void sortBySimilarity(final Alignment alignment, final boolean descending) {
        alignment.resetOrder();

        final Pair<Integer, Integer>[] list = new Pair[alignment.getNumberOfSequences()];

        for (int i = 0; i < alignment.getNumberOfSequences(); i++) {
            list[i] = new Pair<>(alignment.getLane(i).getFirstNonGapPosition(), i);
        }
        Arrays.sort(list, new Pair<>()); // sort by start position

        float[][] similarity = new float[alignment.getNumberOfSequences()][alignment.getNumberOfSequences()];

        for (int il = 0; il < list.length; il++) {
            final int i = list[il].getSecond();
            final int iLast = alignment.getLane(i).getLastNonGapPosition();

            for (int jl = il + 1; jl < list.length; jl++) {
                if (list[jl].getFirst() > iLast)
                    break; // start of read j ist after end of read i
                final int j = list[jl].getSecond();
                similarity[i][j] = similarity[j][i] = computeSimilarity(alignment.getLane(i), alignment.getLane(j));
            }
        }

        Graph graph = new Graph();
        Node[] row2node = new Node[alignment.getNumberOfSequences()];

        SortedSet<Edge> edges = new TreeSet<>((e1, e2) -> {
            Float a1 = (Float) e1.getInfo();
            Float a2 = (Float) e2.getInfo();
            if (a1 > a2)
                return -1;
            else if (a1 < a2)
                return 1;
            else if (e1.getId() < e2.getId())
                return -1;
            else if (e1.getId() > e2.getId())
                return 1;
            else
                return 0;
        });
        for (int i = 0; i < alignment.getNumberOfSequences(); i++) {
            row2node[i] = graph.newNode();
            row2node[i].setInfo(i);
        }
        for (int i = 0; i < alignment.getNumberOfSequences(); i++) {
            for (int j = i + 1; j < alignment.getNumberOfSequences(); j++) {
                if (similarity[i][j] > 0) {
                    edges.add(graph.newEdge(row2node[i], row2node[j], similarity[i][j]));
                }
            }
        }

        NodeArray<Node> otherEndOfChain = new NodeArray<>(graph);
        for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
            otherEndOfChain.put(v, v);
        }

        EdgeSet selectedEdges = new EdgeSet(graph);
        for (Edge e : edges) {
            Node v = e.getSource();
            Node w = e.getTarget();

            if (otherEndOfChain.get(v) != w && getNumberOfAdjacentSelected(v, selectedEdges) < 2 && getNumberOfAdjacentSelected(w, selectedEdges) < 2) {
                selectedEdges.add(e);
                Node ov = otherEndOfChain.get(v);
                Node ow = otherEndOfChain.get(w);
                otherEndOfChain.put(ov, ow);
                otherEndOfChain.put(ow, ov);
            }
        }

        List<List<Node>> chains = new LinkedList<>();

        NodeSet used = new NodeSet(graph);
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            if (!used.contains(v) && getNumberOfAdjacentSelected(v, selectedEdges) == 1) {
                List<Node> chain = new LinkedList<>();
                extractChainRec(v, null, used, chain, selectedEdges);
                chains.add(chain);
            }
        }
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            if (!used.contains(v)) {
                List<Node> chain = new LinkedList<>();
                chain.add(v);
                chains.add(chain);
            }
        }

        sortChains(alignment, chains, null);

        LinkedList<Integer> order = new LinkedList<>();
        for (List<Node> chain : chains) {
            for (Node v : chain) {
                order.add((Integer) v.getInfo());
            }
        }
        alignment.setOrder(order);
    }

    /**
     * extract chain of nodes
     *
     * @param v
     * @param e
     * @param used
     * @param order
     */
    private static void extractChainRec(Node v, Edge e, NodeSet used, List<Node> order, EdgeSet selectedEdges) {
        if (!used.contains(v)) {
            used.add(v);
            order.add(v);
            for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
                if (f != e && selectedEdges.contains(f))
                    extractChainRec(f.getOpposite(v), f, used, order, selectedEdges);
            }
        } else
            throw new RuntimeException("Illegal cycle at: " + v);
    }

    /**
     * get number of adjacent nodes that are selected
     *
     * @param v
     * @param selectedEdges
     * @return selected adjacent
     */
    private static int getNumberOfAdjacentSelected(Node v, EdgeSet selectedEdges) {
        int count = 0;
        for (Edge e = v.getFirstAdjacentEdge(); e != null; e = v.getNextAdjacentEdge(e)) {
            if (selectedEdges.contains(e)) {
                count++;

            }
        }
        return count;
    }

    /**
     * computes the similarity of two sequences
     *
     * @param a
     * @param b
     * @return distance
     */
    private static float computeSimilarity(Lane a, Lane b) {
        int same = 0;
        int diff = 0;
        int firstCoordinate = Math.max(a.getFirstNonGapPosition(), b.getFirstNonGapPosition());
        int lastCoordinate = Math.min(a.getLastNonGapPosition(), b.getLastNonGapPosition());

        for (int i = firstCoordinate; i <= lastCoordinate; i++) {
            char cha = a.charAt(i);
            char chb = b.charAt(i);

            if (Character.isLetter(cha) && Character.isLetter(chb)) {
                if (Character.toLowerCase(cha) == Character.toLowerCase((chb)))
                    same++;
                else
                    diff++;
            }
        }
        return Math.max(same > 0 ? 1 : 0, same - 3 * diff);
    }


    /**
     * sort chains from left to right
     *
     * @param chains
     * @param array
     */
    private static void sortChains(final Alignment alignment, List<List<Node>> chains, final Integer[] array) {
        // first reverse any chains so that left most sequence occurs first
        for (List<Node> chain : chains) {
            Node first = chain.get(0);
            Node last = chain.get(chain.size() - 1);
            int a = (Integer) first.getInfo();
            int b = (Integer) last.getInfo();
            if (alignment.getLane(a).getFirstNonGapPosition() > alignment.getLane(b).getFirstNonGapPosition()) {
                // need to reverse the chain
                List<Node> tmp = Basic.reverseList(chain);
                chain.clear();
                chain.addAll(tmp);
            }
        }

        SortedSet<List<Node>> sorted = new TreeSet<>((listA, listB) -> {
            Node nodeA = listA.get(0);
            Node nodeB = listB.get(0);
            Lane laneA = alignment.getLane((Integer) nodeA.getInfo());
            Lane laneB = alignment.getLane((Integer) nodeB.getInfo());

            if (laneA.getFirstNonGapPosition() < laneB.getFirstNonGapPosition())
                return -1;
            else if (laneA.getFirstNonGapPosition() > laneB.getFirstNonGapPosition())
                return 1;
            else if (nodeA.getId() < nodeB.getId())
                return -1;
            else if (nodeA.getId() > nodeB.getId())
                return 1;
            else
                return 0;
        });

        sorted.addAll(chains);
        chains.clear();
        chains.addAll(sorted);
    }

    /**
     * move the selected interval of sequences up one
     *
     * @param firstRow
     * @param lastRow
     * @return true, if moved
     */
    public static boolean moveUp(Alignment alignment, int firstRow, int lastRow) {
        lastRow = Math.min(lastRow, alignment.getNumberOfSequences());
        if (firstRow <= 0 || firstRow > lastRow)
            return false;
        else {
            Integer[] array = new Integer[alignment.getNumberOfSequences()];
            for (int i = 0; i < alignment.getNumberOfSequences(); i++)
                array[i] = alignment.getOrder(i);
            int replaced = array[firstRow - 1];
            System.arraycopy(array, firstRow, array, firstRow - 1, lastRow + 1 - firstRow);
            array[lastRow] = replaced;
            alignment.setOrder(Arrays.asList(array));
            return true;
        }
    }

    /**
     * move the selected interval of sequences down one
     *
     * @param firstRow
     * @param lastRow
     * @return true, if moved
     */
    public static boolean moveDown(Alignment alignment, int firstRow, int lastRow) {
        firstRow = Math.max(0, firstRow);
        if (lastRow >= alignment.getNumberOfSequences() - 1)
            return false;
        else {
            Integer[] array = new Integer[alignment.getNumberOfSequences()];
            for (int i = 0; i < alignment.getNumberOfSequences(); i++)
                array[i] = alignment.getOrder(i);
            int replaced = array[lastRow + 1];
            System.arraycopy(array, firstRow, array, firstRow + 1, lastRow + 1 - firstRow);
            array[firstRow] = replaced;
            alignment.setOrder(Arrays.asList(array));
            return true;
        }
    }
}
