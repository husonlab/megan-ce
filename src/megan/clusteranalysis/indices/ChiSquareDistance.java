/*
 *  Copyright (C) 2015 Daniel H. Huson
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


/**
 * compute the ChiSquare metric between any two samples
 * Daniel Huson, 6.2018
 */
public class ChiSquareDistance {
    public static final String NAME = "ChiSquare";

    /**
     * compute the ChiSquare metric between any two samples
     *
     * @param viewer
     * @param distances
     * @return number of nodes used to compute value
     */
    public static int apply(final ClassificationViewer viewer, final Distances distances) {
        System.err.println("Computing " + NAME + " distances");

        final int nTax = distances.getNtax();

        final double[][] matrix = distances.getMatrix();
        for (int s = 0; s < nTax; s++) {
            for (int t = s + 1; t < nTax; t++) {
                matrix[s][t] = 0;
            }
        }

        final double[] total = new double[nTax];
        for (Node v : viewer.getSelectedNodes()) {
            final float[] count = (v.getOutDegree() == 0 ? viewer.getNodeData(v).getSummarized() : viewer.getNodeData(v).getAssigned());
            for (int s = 0; s < nTax; s++)
                total[s] += count[s];
        }

        for (Node v : viewer.getSelectedNodes()) {
            final float[] count = (v.getOutDegree() == 0 ? viewer.getNodeData(v).getSummarized() : viewer.getNodeData(v).getAssigned());
            for (int s = 0; s < nTax; s++) {
                final double p = (total[s] > 0 ? count[s] / total[s] : 0);
                for (int t = s + 1; t < nTax; t++) {
                    final double q = (total[t] > 0 ? count[t] / total[t] : 0);
                    if (p + q > 0) {
                        matrix[s][t] += Math.pow(p - q, 2) / (p + q);
                    }
                }
            }
        }
        for (int s = 0; s < nTax; s++) {
            for (int t = s + 1; t < nTax; t++) {
                matrix[s][t] = matrix[t][s] = 2 * matrix[s][t];
            }
        }
        return viewer.getSelectedNodes().size();
    }
}
