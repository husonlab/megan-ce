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
import jloda.util.Basic;
import megan.clusteranalysis.tree.Distances;
import megan.core.Document;
import megan.viewer.ViewerBase;

/**
 * Jensen shannon divergence
 * See: http://enterotype.embl.de/enterotypes.html
 * Daniel Huson, 9.2014
 */
public class JensenShannonDivergence {
    public static final String NAME = "SqrtJensenShannonDivergence";

    /**
     * apply the named computation to the taxonomy
     *
     * @param viewer
     * @param distances
     * @return number of nodes used to compute value
     * @throws java.io.IOException
     */
    public static int apply(final ViewerBase viewer, final Distances distances) {
        System.err.println("Computing " + Basic.fromCamelCase(NAME) + " distances");

        final double[][] profiles = computeProfiles(viewer.getDocument(), viewer);

        System.err.println("Samples: " + profiles.length + " classes: " + profiles[0].length);

        for (int x = 0; x < profiles.length; x++) {
            distances.set(x + 1, x + 1, 0);
            for (int y = x + 1; y < profiles.length; y++) {
                distances.set(x + 1, y + 1, Math.sqrt(computeJSD(profiles[x], profiles[y])));
                distances.set(y + 1, x + 1, distances.get(x + 1, y + 1));
            }
        }

        return profiles[0].length;
    }

    /**
     * compute the Jensen-Shannon divergence
     *
     * @param px
     * @param py
     * @return
     */
    private static double computeJSD(double[] px, double[] py) {
        double[] m = computeMean(px, py);

        return 0.5 * (computeKLD(px, m) + computeKLD(py, m));
    }

    /**
     * compute the  Kullback-Leibler divergence
     *
     * @param px
     * @param py
     */
    private static double computeKLD(double[] px, double[] py) {
        double result = 0;
        for (int i = 0; i < px.length; i++) {
            double xi = Math.max(px[i], 0.0000000001);
            double yi = Math.max(py[i], 0.0000000001);
            result += xi * Math.log(xi / yi);
        }
        return result;
    }

    /**
     * return mean of two profiles
     *
     * @param px
     * @param py
     * @return mean
     */
    private static double[] computeMean(double[] px, double[] py) {
        double[] m = new double[px.length];
        for (int i = 0; i < px.length; i++)
            m[i] = 0.5 * (px[i] + py[i]);
        return m;
    }

    /**
     * compute profiles for  analysis
     *
     * @param doc
     * @return profiles. First index is sample, second is class
     */
    private static double[][] computeProfiles(Document doc, ViewerBase graphView) {
        final int totalSamples = doc.getNumberOfSamples();
        int totalClasses = 0;

        for (Node v = graphView.getGraph().getFirstNode(); v != null; v = v.getNext()) {
            if (graphView.getSelected(v)) {
                totalClasses++;
            }
        }

        double[][] profiles = new double[totalSamples][totalClasses];

        int classCount = 0;

        for (Node v : graphView.getSelectedNodes()) {
            float[] counts = (v.getOutDegree() == 0 ? graphView.getNodeData(v).getSummarized() : graphView.getNodeData(v).getAssigned());
            for (int sampleCount = 0; sampleCount < totalSamples; sampleCount++) {
                profiles[sampleCount][classCount] = counts[sampleCount];
            }
            classCount++;
        }

        for (double[] profile : profiles) {
            double sum = 0;
            for (double value : profile)
                sum += value;
            //System.err.println("Sum: "+sum);
            if (sum > 0) {
                for (int i = 0; i < profile.length; i++) {
                    profile[i] /= sum;
                }
            }
        }

        return profiles;
    }
}
