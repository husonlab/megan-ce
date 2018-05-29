/*
 *  Copyright (C) 2018 Daniel H. Huson
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
package megan.clusteranalysis.indices;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.clusteranalysis.tree.Distances;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import java.io.IOException;

/**
 * unweighted UnweightedTaxonomicUniFrac distance
 * todo: need to cleanup use of induced ranked taxonomy
 * Daniel Huson, 9.2012, 11.2017
 */
public class UniFrac {
    public static final String UnweightedTaxonomicUniFrac = "UnweightedTaxonomicUniFrac";
    public static final String WeightedTaxonomicUniFrac = "WeightedTaxonomicUniFrac";

    /**
     * apply the chosen UniFrac method method to the taxonomy
     *
     * @param viewer
     * @param method
     * @param threshold
     * @param distances
     * @return number of nodes used to compute value
     */
    public static int apply(final MainViewer viewer, String method, final int threshold, final Distances distances) throws CanceledException {
        System.err.println("Computing " + method + " distances");

        final int nodesUsed;
        if (method.equalsIgnoreCase(UnweightedTaxonomicUniFrac))
            nodesUsed = applyUnweightedUniformUniFrac(viewer, threshold, distances);
        else
            nodesUsed = applyWeightedUniformUniFrac(viewer, distances);

        System.err.println("Nodes used: " + nodesUsed + " (all selected nodes and their ancestors)");
        return nodesUsed;
    }

    /**
     * apply the unweighted taxonomic unifrac method
     *
     * @param viewer
     * @param threshold
     * @param distances for each pair of samples i and j, the proportion of nodes in which either sample i or j has a none-zero count, but not both
     * @return number of nodes used to compute value
     */
    public static int applyUnweightedUniformUniFrac(final MainViewer viewer, final int threshold, final Distances distances) throws CanceledException {
        final int nTax = distances.getNtax();

        int countNodes = 0;

        int[][] diff = new int[nTax][nTax];

        final NodeSet allAbove = getSelectedOrAbove(viewer.getTree(), viewer.getSelectedNodes());

        final ProgressListener progress = viewer.getDocument().getProgressListener();
        progress.setSubtask("Unweighted uniform UniFrac");
        progress.setProgress(0);
        progress.setMaximum(allAbove.size());

        for (Node v : allAbove) {
            final int taxonId = (Integer) v.getInfo();
            if (taxonId > 0 && v.getOutDegree() != 1 && TaxonomicLevels.isMajorRank(TaxonomyData.getTaxonomicRank(taxonId)))  // only use proper nodes
            {
                countNodes++;
                final float[] counts = (v.getOutDegree() == 0 ? viewer.getNodeData(v).getSummarized() : viewer.getNodeData(v).getAssigned());
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
                distances.set(s + 1, t + 1, (countNodes > 0 ? (double) diff[s][t] / (double) countNodes : 0));
            }
        }

        return countNodes;
    }


    /**
     * apply the named computation to the taxonomy
     *
     * @param viewer
     * @param distances for each pair of samples i and j, the sum of absolute differences on each node, divided by the total counts for both samples
     * @return number of nodes used to compute value
     * @throws IOException
     */
    public static int applyWeightedUniformUniFrac(final MainViewer viewer, final Distances distances) throws CanceledException {
        final int nTax = distances.getNtax();

        int countNodes = 0;
        double[][] diff = new double[nTax][nTax];
        double[][] sum = new double[nTax][nTax];

        final NodeSet allAbove = getSelectedOrAbove(viewer.getTree(), viewer.getSelectedNodes());


        final ProgressListener progress = viewer.getDocument().getProgressListener();
        progress.setSubtask("Unweighted uniform UniFrac");
        progress.setProgress(0);
        progress.setMaximum(allAbove.size());

        for (Node v : allAbove) {
            final int taxonId = (Integer) v.getInfo();
            if (taxonId > 0 && v.getOutDegree() != 1 && TaxonomicLevels.isMajorRank(TaxonomyData.getTaxonomicRank(taxonId)))  // only use proper nodes
            {
                countNodes++;
                final float[] counts = (v.getOutDegree() == 0 ? viewer.getNodeData(v).getSummarized() : viewer.getNodeData(v).getAssigned());
                for (int s = 0; s < nTax; s++) {
                    for (int t = s + 1; t < nTax; t++) {
                        diff[s][t] += Math.abs(counts[s] - counts[t]);
                        sum[s][t] += counts[s] + counts[t];
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

        return countNodes;
    }

    /**
     * get all nodes that are selected or an ancestor of a selected node
     *
     * @param tree
     * @param selectedNodes
     * @return selected and above
     */
    private static NodeSet getSelectedOrAbove(PhyloTree tree, NodeSet selectedNodes) {
        final NodeSet set = new NodeSet(tree);
        set.addAll(selectedNodes);
        getSelectedOrAboveRec(tree.getRoot(), set);
        return set;
    }

    /**
     * recursively does the work
     *
     * @param v
     * @param set
     * @return true, if anything below is selected
     */
    private static boolean getSelectedOrAboveRec(Node v, NodeSet set) {
        if (v.getOutDegree() == 0) {
            return set.contains(v);
        } else {
            boolean selectedBelow = false;
            for (Edge e : v.outEdges()) {
                final Node w = e.getTarget();
                if (getSelectedOrAboveRec(w, set))
                    selectedBelow = true;
            }
            if (selectedBelow)
                set.add(v);
            return selectedBelow;
        }
    }
}
