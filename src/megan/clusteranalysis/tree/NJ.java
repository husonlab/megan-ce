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
import jloda.graph.EdgeDoubleArray;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.PhyloTreeView;
import jloda.swing.util.Geometry;
import jloda.util.Basic;

import java.util.HashMap;
import java.util.Random;

/**
 * run the NJ algorithm and compute an embedding
 * Daniel Huson, 9.2012
 */
public class NJ {
    private static NJ instance;

    public static void apply(Taxa taxa, Distances distances, PhyloTreeView treeView) {
        if (instance == null)
            instance = new NJ();
        PhyloTree tree = (PhyloTree) treeView.getGraph();
        instance.computeNJ(taxa, distances, tree);
        instance.computeEmbedding(treeView, tree);
    }

    /**
     * run the UPGMA algorithm
     *
     * @param taxa
     * @param dist
     * @param tree
     */
    private void computeNJ(Taxa taxa, Distances dist, PhyloTree tree) {
        tree.clear();
        try {

            HashMap<String, Node> TaxaHashMap = new HashMap<>();
            int nbNtax = dist.getNtax();
            StringBuffer[] tax = new StringBuffer[nbNtax + 1];
            //Taxalabels are saved as a StringBuffer array

            for (int i = 1; i <= nbNtax; i++) {
                tax[i] = new StringBuffer();
                tax[i].append(taxa.getLabel(i));
                Node v = tree.newNode(); // create newNode for each Taxon
                tree.setLabel(v, tax[i].toString());
                TaxaHashMap.put(tax[i].toString(), v);
            }

            double[][] h = new double[nbNtax + 1][nbNtax + 1];// distance matix
            double[] b = new double[nbNtax + 1];// the b variable in Neighbor Joining
            int i_min = 0, j_min = 0; // needed for manipulation of h and b
            double temp, dist_e = 0.0, dist_f = 0.0;//new edge weights
            StringBuffer tax_old_i; //labels of taxa that are being merged
            StringBuffer tax_old_j;
            Node v;
            Edge e, f; //from tax_old to new=merged edge

            for (int i = 0; i <= nbNtax; i++) {
                h[0][i] = 1.0; // with 1.0 marked columns indicate columns/rows
                h[i][0] = 1.0;// that haven't been deleted after merging
            }
            for (int i = 1; i <= nbNtax; i++) {
                for (int j = 1; j <= nbNtax; j++) { //fill up the
                    if (i < j)
                        h[i][j] = dist.get(i, j);// distance matix h
                    else
                        h[i][j] = dist.get(j, i);
                }
            }

            // calculate b:
            for (int i = 1; i <= nbNtax; i++) {
                for (int j = 1; j <= nbNtax; j++) {
                    b[i] += h[i][j];
                }
            }
            // recall: int i_min=0, j_min=0;

            // actual for (finding all nearest Neighbors)
            for (int actual = nbNtax; actual > 2; actual--) {
                // find: min D (h, b, b)
                double d_min = Double.MAX_VALUE;
                for (int i = 1; i < nbNtax; i++) {
                    if (h[0][i] == 0.0) continue;
                    for (int j = i + 1; j <= nbNtax; j++) {
                        if (h[0][j] == 0.0)
                            continue;
                        if (h[i][j] - ((b[i] + b[j]) / (actual - 2)) < d_min) {
                            d_min = h[i][j] - ((b[i] + b[j]) / (actual - 2));
                            i_min = i;
                            j_min = j;
                        }
                    }
                }
                dist_e = 0.5 * (h[i_min][j_min] + b[i_min] / (actual - 2)
                        - b[j_min] / (actual - 2));
                dist_f = 0.5 * (h[i_min][j_min] + b[j_min] / (actual - 2)
                        - b[i_min] / (actual - 2));

                h[j_min][0] = 0.0;// marking
                h[0][j_min] = 0.0;

                // tax taxa rescan:
                tax_old_i = new StringBuffer(tax[i_min].toString());
                tax_old_j = new StringBuffer(tax[j_min].toString());
                tax[i_min].insert(0, "(");
                tax[i_min].append(",");
                tax[i_min].append(tax[j_min]);
                tax[i_min].append(")");
                tax[j_min].delete(0, tax[j_min].length());

                // b rescan:

                b[i_min] = 0.0;
                b[j_min] = 0.0;

                // fusion of h
                // double h_min = h[i_min][j_min];

                for (int i = 1; i <= nbNtax; i++) {
                    if (h[0][i] == 0.0)
                        continue;
                    //temp=(h[i][i_min] + h[i][j_min] - h_min)/2; This is incorrect
                    temp = (h[i][i_min] + h[i][j_min] - dist_e - dist_f) / 2; // correct NJ


                    if (i != i_min) {
                        b[i] = b[i] - h[i][i_min] - h[i][j_min] + temp;
                    }
                    b[i_min] += temp;
                    h[i][i_min] = temp;
                    b[j_min] = 0.0;
                }

                for (int i = 0; i <= nbNtax; i++) {
                    h[i_min][i] = h[i][i_min];
                    h[i][j_min] = 0.0;
                    h[j_min][i] = 0.0;
                }

                // generate new Node for merged Taxa:
                v = tree.newNode();
                TaxaHashMap.put(tax[i_min].toString(), v);

                // generate Edges from two Taxa that are merged to one:
                e = tree.newEdge(TaxaHashMap.get(tax_old_i.toString()), v);
                tree.setWeight(e, Math.max(dist_e, 0.0));
                f = tree.newEdge(TaxaHashMap.get(tax_old_j.toString()), v);
                tree.setWeight(f, Math.max(dist_f, 0.0));
            }

            // evaluating last two nodes:
            for (int i = 1; i <= nbNtax; i++) {
                if (h[0][i] == 1.0) {
                    i_min = i;
                    i++;

                    for (; i <= nbNtax; i++) {
                        if (h[0][i] == 1.0) {
                            j_min = i;
                        }
                    }
                }
            }
            tax_old_i = new StringBuffer(tax[i_min].toString());
            tax_old_j = new StringBuffer(tax[j_min].toString());

            tax[i_min].insert(0, "(");
            tax[i_min].append(",");
            tax[i_min].append(tax[j_min]);
            tax[i_min].append(")");
            tax[j_min].delete(0, tax[j_min].length()); //not neces. but sets content to NULL

            // generate new Node for merged Taxa:
            // generate Edges from two Taxa that are merged to one:
            e = tree.newEdge(TaxaHashMap.get
                    (tax_old_i.toString()), TaxaHashMap.get(tax_old_j.toString()));
            tree.setWeight(e, Math.max(h[i_min][j_min], 0.0));
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        //System.err.println(tree.toString());
    }

    /**
     * compute an embedding of the graph
     *
     * @return true, if embedding was computed
     */
    private boolean computeEmbedding(PhyloTreeView treeView, PhyloTree tree) {
        treeView.removeAllInternalPoints();

        if (tree.getNumberOfNodes() == 0)
            return true;
        treeView.removeAllInternalPoints();

        // don't use setRoot to remember root
        Node root = tree.getFirstNode();
        NodeSet leaves = new NodeSet(tree);

        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            if (tree.getDegree(v) == 1)
                leaves.add(v);
            if (tree.getDegree(v) > tree.getDegree(root))
                root = v;
        }

        // recursively visit all nodes in the tree and determine the
        // angle 0-2PI of each edge. nodes are placed around the unit
        // circle at position
        // n=1,2,3,... and then an edge along which we visited nodes
        // k,k+1,...j-1,j is directed towards positions k,k+1,...,j

        EdgeDoubleArray angle = new EdgeDoubleArray(tree); // angle of edge
        Random rand = new Random();
        rand.setSeed(1);
        int seen = setAnglesRec(tree, 0, root, null, leaves, angle, rand);

        if (seen != leaves.size())
            System.err.println("Warning: Number of nodes seen: " + seen +
                    " != Number of leaves: " + leaves.size());

        // recursively compute node coordinates from edge angles:
        setCoordsRec(treeView, tree, root, null, angle);

        treeView.trans.setCoordinateRect(treeView.getBBox());
        treeView.resetViews();
        treeView.fitGraphToWindow();

        return true;
    }

    /**
     * Recursively determines the angle of every tree edge.
     *
     * @param num    int
     * @param root   Node
     * @param entry  Edge
     * @param leaves NodeSet
     * @param angle  EdgeDoubleArray
     * @param rand   Random
     * @return b int
     */

    private int setAnglesRec(PhyloTree tree, int num, Node root, Edge entry, NodeSet leaves,
                             EdgeDoubleArray angle, Random rand) {
        if (leaves.contains(root))
            return num + 1;
        else {
            // edges.permute(); // look at children in random order
            int a = num; // is number of nodes seen so far
            int b = 0;     // number of nodes after visiting subtree
            for (Edge e : root.adjacentEdges()) {
                if (e != entry) {
                    b = setAnglesRec(tree, a, tree.getOpposite(root, e), e, leaves, angle, rand);

                    // point towards the segment of the unit circle a...b:
                    angle.set(e, Math.PI * (a + 1 + b) / leaves.size());
                    a = b;
                }
            }
            if (b == 0)
                System.err.println("Warning: setAnglesRec: recursion failed");
            return b;
        }
    }

    /**
     * recursively compute node coordinates from edge angles:
     *
     * @param root  Node
     * @param entry Edge
     * @param angle EdgeDouble
     */
    private void setCoordsRec(PhyloTreeView treeView, PhyloTree tree, Node root, Edge entry, EdgeDoubleArray angle) {
        for (Edge e : root.adjacentEdges()) {
            if (e != entry) {
                Node v = tree.getOpposite(root, e);

                // translate in the computed direction by the given amount
                treeView.setLocation(v,
                        Geometry.translateByAngle(treeView.getLocation(root), angle.get(e),
                                tree.getWeight(e)));
                setCoordsRec(treeView, tree, v, e, angle);
            }
        }
    }
}
