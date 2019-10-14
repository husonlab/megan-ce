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

import jloda.graph.*;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import jloda.util.ProgressPercentage;

import java.util.ArrayList;
import java.util.List;

/**
 * Extract a set of paths through the overlap graph
 * Daniel Huson, 5.2015
 */
public class PathExtractor {
    private final Graph overlapGraph;

    private Node[][] paths;
    private Node[] singletons;
    private final List<Integer>[] readId2ContainedReads;

    /**
     * constructor
     *
     * @param overlapGraph
     */
    public PathExtractor(Graph overlapGraph, List<Integer>[] readId2ContainedReads) {
        this.overlapGraph = overlapGraph;
        this.readId2ContainedReads = readId2ContainedReads;
    }

    /**
     * determines the paths through the graph
     * The algorithm determines the longest path between any start read and any end read, where the length is given by the total
     * number of pairwise overlapped bases in the path
     */
    public int apply(ProgressListener progress) throws CanceledException {
        // make a working copy of the graph. Necessary because we remove stuff from the graph
        final Graph overlapGraphWorkingCopy = new Graph();
        final NodeArray<Node> new2oldNode = new NodeArray<>(overlapGraphWorkingCopy);
        final EdgeArray<Edge> new2oldEdge = new EdgeArray<>(overlapGraphWorkingCopy);
        {
            progress.setSubtask("Copying graph");
            progress.setMaximum(overlapGraph.getNumberOfNodes() + overlapGraph.getNumberOfEdges());
            progress.setProgress(0);

            NodeArray<Node> old2newNode = new NodeArray<>(this.overlapGraph);
            for (Node v = this.overlapGraph.getFirstNode(); v != null; v = this.overlapGraph.getNextNode(v)) {
                final Node w = overlapGraphWorkingCopy.newNode(v.getInfo());
                w.setData(v.getData());
                new2oldNode.put(w, v);
                old2newNode.put(v, w);
                progress.incrementProgress();
            }
            for (Edge e = this.overlapGraph.getFirstEdge(); e != null; e = this.overlapGraph.getNextEdge(e)) {
                final Edge f = overlapGraphWorkingCopy.newEdge(old2newNode.get(e.getSource()), old2newNode.get(e.getTarget()), e.getInfo());
                new2oldEdge.put(f, e);
                progress.incrementProgress();
            }
        }
        if (progress instanceof ProgressPercentage)
            ((ProgressPercentage) progress).reportTaskCompleted();

        // extract contigs from graph, deleting their nodes
        progress.setSubtask("Extracting paths");
        progress.setMaximum(overlapGraphWorkingCopy.getNumberOfNodes());
        progress.setProgress(0);

        final List<Node> toDelete = new ArrayList<>(overlapGraphWorkingCopy.getNumberOfNodes());
        final EdgeArray<Integer> edgeWeights = new EdgeArray<>(overlapGraphWorkingCopy);

        for (Node v = overlapGraphWorkingCopy.getFirstNode(); v != null; v = v.getNext()) {
            if (v.getInDegree() == 0) {
                visitNodesRec(v, edgeWeights);
            }
            progress.incrementProgress();
        }
        if (progress instanceof ProgressPercentage)
            ((ProgressPercentage) progress).reportTaskCompleted();

        final List<Node[]> pathsList = new ArrayList<>();

        // generate all paths in this loop:
        progress.setSubtask("Extracting paths");
        final int initialNumberOfEdges = overlapGraphWorkingCopy.getNumberOfEdges();
        progress.setMaximum(initialNumberOfEdges);
        progress.setProgress(0);

        while (overlapGraphWorkingCopy.getNumberOfEdges() > 0) {
            Edge bestEdge = overlapGraphWorkingCopy.getFirstEdge();
            for (Edge e = overlapGraphWorkingCopy.getFirstEdge(); e != null; e = overlapGraphWorkingCopy.getNextEdge(e)) {
                if (edgeWeights.getValue(e) > edgeWeights.getValue(bestEdge))
                    bestEdge = e;
            }

            Node v = bestEdge.getSource();

            final List<Node> path = new ArrayList<>(); // new path

            path.add(new2oldNode.get(bestEdge.getSource()));

            int weight = edgeWeights.getValue(bestEdge);

            while (v.getOutDegree() > 0) {
                // find predecessor node:
                Node w = null;
                for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                    int eWeight = edgeWeights.getValue(f);
                    if (eWeight == weight) {
                        w = f.getTarget();
                        weight -= (Integer) f.getInfo(); // subtracting the overlap length of f
                        break;
                    }
                }
                if (w == null)
                    throw new RuntimeException("w==null");
                path.add(new2oldNode.get(w));
                toDelete.add(v);
                v = w;
            }
            toDelete.add(v);

            // remove all nodes used in contig
            for (Node z : toDelete) {
                overlapGraphWorkingCopy.deleteNode(z);
            }
            toDelete.clear();

            // clear edge weights:
            for (Edge z = overlapGraphWorkingCopy.getFirstEdge(); z != null; z = z.getNext()) {
                edgeWeights.put(z, null);
            }

            // set weights to reflect longest path
            for (Node z = overlapGraphWorkingCopy.getFirstNode(); z != null; z = z.getNext()) {
                if (z.getInDegree() == 0) {
                    visitNodesRec(z, edgeWeights);
                }
            }

            pathsList.add(path.toArray(new Node[0]));
            progress.setProgress(initialNumberOfEdges - overlapGraphWorkingCopy.getNumberOfEdges());
        }
        if (progress instanceof ProgressPercentage)
            ((ProgressPercentage) progress).reportTaskCompleted();

        // singleton reads:
        final List<Node> singletonList = new ArrayList<>();
        for (Node v = overlapGraphWorkingCopy.getFirstNode(); v != null; v = overlapGraphWorkingCopy.getNextNode(v)) {
            int readId = (Integer) v.getInfo();
            if (readId2ContainedReads != null && readId < readId2ContainedReads.length && readId2ContainedReads[readId] != null && readId2ContainedReads[readId].size() > 0)
                pathsList.add(new Node[]{v});
            else
                singletonList.add(new2oldNode.get(v));
        }
        paths = pathsList.toArray(new Node[pathsList.size()][]);
        singletons = singletonList.toArray(new Node[0]);

        return paths.length;
    }

    /**
     * recursively visit all nodes and set edge weights
     * The weight of an edge e is the maximum sum of overlaps on any outgoing path from e
     *
     * @param v
     * @return path length
     */
    private int visitNodesRec(Node v, EdgeArray<Integer> edgeWeights) {
        int maxValue = 0;
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            if (edgeWeights.getValue(e) == null) {
                edgeWeights.put(e, visitNodesRec(e.getTarget(), edgeWeights) + (Integer) e.getInfo());
                // note that (Integer)e.getInfo() is the overlap length of e
            }
            maxValue = Math.max(maxValue, edgeWeights.getValue(e));
        }
        return maxValue;
    }

    /**
     * get all selected paths through graph
     *
     * @return paths
     */
    public Node[][] getPaths() {
        return paths;
    }

    /**
     * get all singleton nodes
     *
     * @return singletons
     */
    public Node[] getSingletons() {
        return singletons;
    }
}
