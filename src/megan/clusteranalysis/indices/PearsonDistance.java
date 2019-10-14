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
package megan.clusteranalysis.indices;

import jloda.graph.Node;
import megan.clusteranalysis.tree.Distances;
import megan.viewer.ClassificationViewer;

import java.util.HashSet;
import java.util.LinkedList;

/**
 * Pearson's correlation distance
 * Daniel Huson, 9.2012
 */
public class PearsonDistance {
    public static final String NAME = "Pearsons-Correlation";

    /**
     * apply the PearsonDistance computation to the given classification
     *
     * @param viewer
     * @param distances
     * @return number of nodes used to compute value
     * @throws java.io.IOException
     */
    public static int apply(final ClassificationViewer viewer, final Distances distances) {
        System.err.println("Computing " + NAME + " distances");

        double[][] vectors = computeVectors(viewer, distances.getNtax());
        int rank = distances.getNtax();
        computeCorrelationMatrix(rank, vectors, distances);
        convertCorrelationsToDistances(distances);

        return vectors.length;
    }

    /**
     * compute vectors for  analysis
     *
     * @return vectors. First index is class, second is sample
     */
    private static double[][] computeVectors(ClassificationViewer graphView, int numberOfSamples) {
        double[] total = new double[numberOfSamples];

        HashSet<Integer> seen = new HashSet<>();
        LinkedList<double[]> rows = new LinkedList<>();
        for (Node v = graphView.getGraph().getFirstNode(); v != null; v = v.getNext()) {
            if (graphView.getSelected(v)) {
                if (!seen.contains(v.getInfo())) {
                    seen.add((Integer) v.getInfo());
                    double[] row = new double[numberOfSamples];
                    final float[] values = (v.getOutDegree() == 0 ? graphView.getNodeData(v).getSummarized() : graphView.getNodeData(v).getAssigned());
                    for (int i = 0; i < values.length; i++) {
                        row[i] = values[i];
                        total[i] += row[i];
                    }
                    rows.add(row);
                }
            }
        }
        for (double[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (total[i] > 0)
                    row[i] /= total[i];
            }
        }
        return rows.toArray(new double[rows.size()][]);
    }

    /**
     * computes the pearson distances
     *
     * @param rank
     * @param vectors
     * @param distances
     */
    private static void computeCorrelationMatrix(int rank, double[][] vectors, Distances distances) {
        // compute mean for each row
        double[] mean = new double[rank];
        for (double[] row : vectors) {
            for (int col = 0; col < rank; col++) {
                mean[col] += row[col];
            }
        }
        for (int col = 0; col < rank; col++) {
            mean[col] /= vectors.length;
        }
        double[] stddev = new double[rank];
        for (double[] row : vectors) {
            for (int col = 0; col < rank; col++) {
                stddev[col] += (row[col] - mean[col]) * (row[col] - mean[col]);
            }
        }
        for (int col = 0; col < rank; col++) {
            stddev[col] = Math.sqrt(stddev[col] / vectors.length);
        }

        for (int di = 0; di < rank; di++) {
            distances.set(di + 1, di + 1, 0);
            for (int dj = di + 1; dj < rank; dj++) {
                double cor = 0;
                for (double[] row : vectors) {
                    cor += (row[di] - mean[di]) * (row[dj] - mean[dj]) / (stddev[di] * stddev[dj]);
                }
                cor /= vectors.length;
                distances.set(di + 1, dj + 1, cor);
            }
        }
    }

    /**
     * convert correlations into distances by subtracting from 1 and dividing by 2
     *
     * @param distances
     */
    private static void convertCorrelationsToDistances(Distances distances) {
        for (int i = 1; i <= distances.getNtax(); i++) {
            for (int j = i + 1; j <= distances.getNtax(); j++) {
                distances.set(i, j, (1.0 - distances.get(i, j) * distances.get(i, j)));
            }
        }
    }
}
