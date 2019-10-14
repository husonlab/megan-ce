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
package megan.clusteranalysis.tree;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.NodeView;
import jloda.swing.graphview.PhyloTreeView;

import java.awt.geom.Point2D;
import java.util.LinkedList;

/**
 * run the UPGMA algorithm and compute an embedding
 * Daniel Huson, 3.2011 (enroute Frankfurt - Washington D.C.)
 */
public class UPGMA {
    private static UPGMA instance;

    /**
     * apply the UPGMA algorithm
     *
     * @param taxa
     * @param distances
     * @param treeView
     */
    public static void apply(Taxa taxa, Distances distances, PhyloTreeView treeView) {
        if (instance == null)
            instance = new UPGMA();
        instance.computeUPMATree(taxa, distances, treeView.getPhyloTree());
        embedTree(treeView);
    }

    /**
     * run the UPGMA algorithm
     *
     * @param taxa
     * @param dist
     * @param tree
     */
    private void computeUPMATree(Taxa taxa, Distances dist, PhyloTree tree) {
        tree.clear();

        int ntax = dist.getNtax();

        Node[] subtrees = new Node[ntax + 1];
        int[] sizes = new int[ntax + 1];
        double[] heights = new double[ntax + 1];

        for (int i = 1; i <= ntax; i++) {
            subtrees[i] = tree.newNode();
            tree.setLabel(subtrees[i], taxa.getLabel(i));
            sizes[i] = 1;
        }

        double[][] d = new double[ntax + 1][ntax + 1];// distance matix

        //Initialise d
        //Compute the closest values for each taxa.
        for (int i = 1; i <= ntax; i++) {
            for (int j = i + 1; j <= ntax; j++) {
                double dij = (dist.get(i, j) + dist.get(j, i)) / 2.0;// distance matix h
                d[i][j] = d[j][i] = dij;

            }
        }

        for (int actual = ntax; actual > 2; actual--) {

            int i_min = 0, j_min = 0;
            //Find closest pair.
            double d_min = Double.MAX_VALUE;
            for (int i = 1; i <= actual; i++) {
                for (int j = i + 1; j <= actual; j++) {
                    double dij = d[i][j];
                    if (i_min == 0 || dij < d_min) {
                        i_min = i;
                        j_min = j;
                        d_min = dij;
                    }
                }

            }


            double height = d_min / 2.0;

            Node v = tree.newNode();
            Edge e = tree.newEdge(v, subtrees[i_min]);
            tree.setWeight(e, Math.max(height - heights[i_min], 0.0));
            Edge f = tree.newEdge(v, subtrees[j_min]);
            tree.setWeight(f, Math.max(height - heights[j_min], 0.0));

            subtrees[i_min] = v;
            subtrees[j_min] = null;
            heights[i_min] = height;


            int size_i = sizes[i_min];
            int size_j = sizes[j_min];
            sizes[i_min] = size_i + size_j;

            for (int k = 1; k <= ntax; k++) {
                if ((k == i_min) || k == j_min) continue;
                double dki = (d[k][i_min] * size_i + d[k][j_min] * size_j) / ((double) (size_i + size_j));
                d[k][i_min] = d[i_min][k] = dki;
            }

            //Copy the top row of the matrix and arrays into the empty j_min row/column.
            if (j_min < actual) {
                for (int k = 1; k <= actual; k++) {
                    d[j_min][k] = d[k][j_min] = d[actual][k];
                }
                d[j_min][j_min] = 0.0;
                subtrees[j_min] = subtrees[actual];
                sizes[j_min] = sizes[actual];
                heights[j_min] = heights[actual];
            }
        }

        int sister = 2;
        while (subtrees[sister] == null)
            sister++;

        Node root = tree.newNode();
        tree.setRoot(root);

        double w1, w2;
        double delta = Math.abs(heights[1] - heights[sister]);
        double distance = d[1][sister] - delta;

        if (heights[1] <= heights[sister]) {
            w1 = 0.5 * distance + delta;
            w2 = 0.5 * distance;
        } else {
            w1 = 0.5 * distance;
            w2 = 0.5 * distance + delta;
        }

        Edge e1 = tree.newEdge(root, subtrees[1]);
        tree.setWeight(e1, w1);
        Edge e2 = tree.newEdge(root, subtrees[sister]);
        tree.setWeight(e2, w2);
    }

    /**
     * embed the tree
     *
     * @param treeView
     */
    public static void embedTree(PhyloTreeView treeView) {
        treeView.removeAllInternalPoints();

        Node root = treeView.getPhyloTree().getRoot();
        if (root != null)
            computeEmbeddingRec(treeView, root, null, 0, 0, true);
        treeView.resetViews();
        for (Node v = treeView.getPhyloTree().getFirstNode(); v != null; v = v.getNext()) {
            treeView.setLabel(v, treeView.getPhyloTree().getLabel(v));
            treeView.setLabelLayout(v, NodeView.EAST);
        }
        treeView.trans.setCoordinateRect(treeView.getBBox());
        treeView.fitGraphToWindow();

    }

    /**
     * recursively compute the embedding
     *
     * @param v
     * @param e
     * @param hDistToRoot horizontal distance from node to root
     * @param leafNumber  rank of leaf in vertical ordering
     * @param toScale
     * @return index of last leaf
     */
    private static int computeEmbeddingRec(PhyloTreeView view, Node v, Edge e, double hDistToRoot, int leafNumber, boolean toScale) {
        if (v.getDegree() == 1 && e != null)  // hit a leaf
        {
            view.setLocation(v, toScale ? hDistToRoot : 0, ++leafNumber);
        } else {
            Point2D first = null;
            Point2D last = null;
            double minX = Double.MAX_VALUE;
            for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
                if (f != e) {
                    Node w = f.getOpposite(v);
                    leafNumber = computeEmbeddingRec(view, w, f, hDistToRoot + view.getPhyloTree().getWeight(f), leafNumber, toScale);
                    if (first == null)
                        first = view.getLocation(w);
                    last = view.getLocation(w);
                    if (last.getX() < minX)
                        minX = last.getX();
                }
            }
            if (first != null) // always true
            {
                double x;
                if (toScale)
                    x = hDistToRoot;
                else
                    x = minX - 1;
                double y = 0.5 * (last.getY() + first.getY());
                view.setLocation(v, x, y);

                for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
                    if (f != e) {
                        Node w = f.getOpposite(v);
                        java.util.List<Point2D> list = new LinkedList<>();
                        Point2D p = new Point2D.Double(x, view.getLocation(w).getY());
                        list.add(p);
                        view.setInternalPoints(f, list);
                    }
                }
            }
        }
        return leafNumber;
    }
}
