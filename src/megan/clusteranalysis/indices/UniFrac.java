/*
 * UniFrac.java Copyright (C) 2021. Daniel H. Huson
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
 *
 */
package megan.clusteranalysis.indices;

import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.util.CanceledException;
import jloda.util.progress.ProgressListener;
import jloda.util.StringUtils;
import megan.clusteranalysis.tree.Distances;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import java.io.IOException;

/**
 * unweighted and weighted distance
 * Daniel Huson, 9.2012, 11.2017, 6.2018
 */
public class UniFrac {
    public final static String UnweightedUniformUniFrac = "UnweightedUniformUniFrac";
    public final static String WeightedUniformUniFrac = "WeightedUniformUniFrac";


    /**
     * apply the unweighted taxonomic unifrac method
     *
     * @param viewer
     * @param threshold
     * @param distances for each pair of samples i and j, the proportion of ranked nodes in which either sample i or j has a none-zero count, but not both
     * @return number of nodes used to compute value
     */
    public static int applyUnweightedUniformUniFrac(final MainViewer viewer, final int threshold, final Distances distances) throws CanceledException {
		System.err.println("Computing " + StringUtils.fromCamelCase(UnweightedUniformUniFrac) + " distances");

        final int nTax = distances.getNtax();

        int countNodesUsed = 0;

        final PhyloTree tree = viewer.getTree();

        final NodeSet inducedNodes = new NodeSet(tree);
        inducedNodes.addAll(viewer.getSelectedNodes());

        final NodeArray<float[]> summarized = new NodeArray<>(tree);

        computeSummarizedCountsOnInducedTreeRec(tree.getRoot(), inducedNodes, viewer, summarized, nTax);

        removeRootNodeAndNodesOnPathLeadingToIt(tree.getRoot(), inducedNodes);

        int[][] diff = new int[nTax][nTax];

        final ProgressListener progress = viewer.getDocument().getProgressListener();
        progress.setTasks("Computing", "Unweighted uniform UniFrac");
        progress.setProgress(0);
        progress.setMaximum(inducedNodes.size());

        for (Node v : inducedNodes) {
            final int taxonId = (Integer) v.getInfo();
            if (taxonId > 0 && TaxonomicLevels.isMajorRank(TaxonomyData.getTaxonomicRank(taxonId)))  // only use proper nodes
            {
                countNodesUsed++;
                final float[] counts = summarized.get(v);

                for (int s = 0; s < nTax; s++) {
                    for (int t = s + 1; t < nTax; t++) {
                        if ((counts[s] >= threshold) != (counts[t] >= threshold))
                            diff[s][t]++;
                    }
                }
            }
            progress.incrementProgress();
        }
        for (int s = 0; s < nTax; s++) {
            for (int t = s + 1; t < nTax; t++) {
                distances.set(s + 1, t + 1, (countNodesUsed > 0 ? (double) diff[s][t] / (double) countNodesUsed : 0));
            }
        }

        System.err.println("Nodes used: " + countNodesUsed);
        return countNodesUsed;
    }


    /**
     * apply the named computation to the taxonomy
     *
     * @param viewer
     * @param distances for each pair of samples i and j, the sum of absolute differences of summarized counts (not assigned counts!) on each node, normalized
     *                  such that two identical profiles get distance 0 and two disjoint profiles get distance 1
     * @return number of nodes used to compute value
     * @throws IOException
     */
    public static int applyWeightedUniformUniFrac(final ClassificationViewer viewer, final Distances distances) throws CanceledException {
		System.err.println("Computing " + StringUtils.fromCamelCase(WeightedUniformUniFrac) + " distances");

        final int nTax = distances.getNtax();

        int countNodesUsed = 0;

        final PhyloTree tree = viewer.getTree();

        final NodeSet inducedNodes = new NodeSet(tree);
        inducedNodes.addAll(viewer.getSelectedNodes());

        final NodeArray<float[]> summarized = new NodeArray<>(tree);

        computeSummarizedCountsOnInducedTreeRec(tree.getRoot(), inducedNodes, viewer, summarized, nTax);

        final ProgressListener progress = viewer.getDocument().getProgressListener();
        progress.setTasks("Computing", "Weighted uniform UniFrac");
        progress.setProgress(0);
        progress.setMaximum(2 * inducedNodes.size());

        final Node root = removeRootNodeAndNodesOnPathLeadingToIt(tree.getRoot(), inducedNodes);

        // setup total number of reads in each sample summarized below the root
        final double[] total = new double[nTax];
        {
            for (Edge e : root.outEdges()) {
                final Node w = e.getTarget();
                if (inducedNodes.contains(w)) {
                    for (int s = 0; s < nTax; s++)
                        total[s] += summarized.get(w)[s];
                }
            }
        }

        final double[][] diff = new double[nTax][nTax]; // difference between two samples
        final double[][] sum = new double[nTax][nTax]; // largest possible difference between two samples

        for (Node v : inducedNodes) {
            final int taxonId = (Integer) v.getInfo();
            if (taxonId > 0 && TaxonomicLevels.isMajorRank(TaxonomyData.getTaxonomicRank(taxonId)))  // only use proper nodes
            {
                countNodesUsed++;
                final float[] count = summarized.get(v); // total number of reads that "descend" from node v
                for (int s = 0; s < nTax; s++) {
                    final double p = (total[s] > 0 ? count[s] / total[s] : 0);
                    for (int t = s + 1; t < nTax; t++) {
                        final double q = (total[t] > 0 ? count[t] / total[t] : 0);

                        diff[s][t] += Math.abs(p - q); // normalized differences between datasets
                        sum[s][t] += (p + q);
                    }
                }
            }
            progress.incrementProgress();
        }

        for (int s = 0; s < nTax; s++) {
            for (int t = s + 1; t < nTax; t++) {
                distances.set(s + 1, t + 1, (sum[s][t] > 0 ? diff[s][t] / sum[s][t] : 0));
            }
        }
        return countNodesUsed;
    }

    /**
     * recursively compute the summarized counts for all nodes in tree induced by user selection
     *
     * @param v
     * @param selected
     * @param summarized
     * @return true, if selected nodes on or below v
     */
    private static boolean computeSummarizedCountsOnInducedTreeRec(Node v, NodeSet selected, ClassificationViewer viewer, NodeArray<float[]> summarized, int ntax) {
        float[] currentSummarized = null;

        for (Edge e : v.outEdges()) {
            if (computeSummarizedCountsOnInducedTreeRec(e.getTarget(), selected, viewer, summarized, ntax)) {
                if (currentSummarized == null) {
                    currentSummarized = new float[ntax];
                }
                final float[] childSummarized = summarized.get(e.getTarget());
                for (int s = 0; s < ntax; s++) {
                    currentSummarized[s] += childSummarized[s];
                }
            }
        }

        final NodeData nodeData = viewer.getNodeData(v);

        if (currentSummarized != null) { // has selected below
            for (int s = 0; s < ntax; s++) // add counts for current node
                currentSummarized[s] += nodeData.getAssigned()[s];
            summarized.put(v, currentSummarized);
            selected.add(v);
            return true;
        } else if (selected.contains(v)) { // nothing selected below, but this node is selected, so it is a selection leaf
            summarized.put(v, nodeData.getSummarized());
            return true;
        } else
            return false;
    }

    /**
     * remove the root node and root path
     *
     * @param v
     * @param induced
     * @return the root
     */
    private static Node removeRootNodeAndNodesOnPathLeadingToIt(Node v, NodeSet induced) {

        while (true) {
            induced.remove(v);
            Node selectedChild = null;
            for (Edge e : v.outEdges()) {
                if (induced.contains(e.getTarget())) {
                    if (selectedChild == null)
                        selectedChild = e.getTarget();
                    else
                        return v;  // more than one selected child, done
                }
            }
            if (selectedChild == null) // no selected children below, then degenerate case...
                return v;
            else
                v = selectedChild;
        }
    }
}
