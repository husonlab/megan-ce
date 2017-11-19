/*
 *  Copyright (C) 2017 Daniel H. Huson
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

import jloda.graph.Node;
import megan.clusteranalysis.tree.Distances;
import megan.viewer.ClassificationViewer;
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
     * apply the named computation to the taxonomy
     *
     * @param viewer
     * @param method
     * @param threshold
     * @param distances
     * @return number of nodes used to compute value
     * @throws IOException
     */
    public static int apply(final ClassificationViewer viewer, String method, final int threshold, final Distances distances) throws IOException {
        System.err.println("Computing " + method + " distances");

        if (method.equalsIgnoreCase(UnweightedTaxonomicUniFrac))
            return applyUnweighted(viewer, threshold, distances);
        else
            return applyWeighted(viewer, distances);
    }

    /**
     * apply the unweighted taxonomic unifrac method
     *
     * @param viewer
     * @param threshold
     * @param distances
     * @return number of nodes used to compute value
     * @throws IOException
     */
    public static int applyUnweighted(final ClassificationViewer viewer, final int threshold, final Distances distances) throws IOException {
        final int nTax = distances.getNtax();

        int countNodes = 0;

        int[][] sum = new int[nTax][nTax];

        for (Node v = viewer.getTree().getFirstNode(); v != null; v = v.getNext()) {
            final int taxonId = (Integer) v.getInfo();
            if (taxonId > 0 && v.getOutDegree() != 1 && TaxonomicLevels.isMajorRank(TaxonomyData.getTaxonomicRank(taxonId)))  // only use proper nodes
            {
                countNodes++;
                final float[] counts = (v.getOutDegree() == 0 ? viewer.getNodeData(v).getSummarized() : viewer.getNodeData(v).getAssigned());
                for (int s = 0; s < nTax; s++) {
                    for (int t = s + 1; t < nTax; t++) {
                        if ((counts[s] >= threshold) != (counts[t] >= threshold))
                            sum[s][t]++;
                    }
                }
            }
        }
        for (int s = 0; s < nTax; s++) {
            for (int t = s + 1; t < nTax; t++) {
                distances.set(s + 1, t + 1, (countNodes > 0 ? (double) sum[s][t] / (double) countNodes : 0));
            }
        }

        return countNodes;
    }

    /**
     * apply the named computation to the taxonomy
     *
     * @param viewer
     * @param distances
     * @return number of nodes used to compute value
     * @throws IOException
     */
    public static int applyWeighted(final ClassificationViewer viewer, final Distances distances) throws IOException {
        final int nTax = distances.getNtax();

        int countNodes = 0;
        double[][] diff = new double[nTax][nTax];
        double[][] sum = new double[nTax][nTax];

        for (Node v = viewer.getTree().getFirstNode(); v != null; v = v.getNext()) {
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
        }
        for (int s = 0; s < nTax; s++) {
            for (int t = s + 1; t < nTax; t++) {
                distances.set(s + 1, t + 1, (sum[s][t] > 0 ? diff[s][t] / sum[s][t] : 0));
            }
        }

        return countNodes;
    }
}
