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

import jloda.graph.Node;
import jloda.util.Basic;
import megan.clusteranalysis.tree.Distances;
import megan.core.Document;
import megan.viewer.ViewerBase;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Vector;

/**
 * computes the ecological distances
 * Daniel Huson, 11.2009
 */
public class CalculateEcologicalIndices {
    public static final String GOODALL = "Goodall";
    public static final String GOODALL_NORMALIZED = "Goodall-normalized";

    public static final String CHISSQUARE = "ChiSquare";
    public static final String KULCZYNSKI = "Kulczynski";
    public static final String BRAYCURTIS = "BrayCurtis";
    public static final String HELLINGER = "Hellinger";
    public static final String EUCLIDEAN = "Euclidean";
    public static final String EUCLIDEAN_NORMALIZED = "Euclidean-normalized";

    public static final String[] ALL = new String[]{GOODALL, GOODALL_NORMALIZED, EUCLIDEAN, EUCLIDEAN_NORMALIZED,
            CHISSQUARE, KULCZYNSKI, BRAYCURTIS, HELLINGER};

    /**
     * apply the named computation
     *
     * @param doc
     * @param viewer
     * @param method
     * @param distances
     * @return
     * @throws IOException
     */
    public static int apply(Document doc, ViewerBase viewer, String method, Distances distances, boolean normalize) throws IOException {
        System.err.println("Computing " + method + " distances");
        // setup input data:

        Vector<Double[]> input = new Vector<>();
        double[] total = new double[doc.getNumberOfSamples()];

        int countNodesUsed = 0;
        HashSet<Integer> seen = new HashSet<>();

        for (Node v = viewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
            Integer id = (Integer) v.getInfo();
            if (viewer.getSelected(v)) {
                if (!seen.contains(id)) {
                    seen.add(id);
                    countNodesUsed++;
                    final float[] counts = (v.getOutDegree() == 0 ? viewer.getNodeData(v).getSummarized() : viewer.getNodeData(v).getAssigned());
                    final Double[] numbers = new Double[counts.length];
                    for (int i = 0; i < counts.length; i++) {
                        numbers[i] = (double) counts[i];
                        total[i] += numbers[i];
                    }
                    input.addElement(numbers);
                }
            }
        }
        if (normalize) {
            for (Double[] numbers : input) {
                for (int i = 0; i < numbers.length; i++) {
                    if (total[i] > 0) {
                        numbers[i] /= total[i];
                    }
                }
            }
        }

        System.err.println("Nodes used: " + seen.size());

        Vector<Vector<Double>> upperTriangle;
        // compute matrix:
        if (method.equalsIgnoreCase(GOODALL)) {
            upperTriangle = EcologicalIndices.getGoodallsDistance(input, false);
        } else if (method.equalsIgnoreCase(GOODALL_NORMALIZED)) {
            upperTriangle = EcologicalIndices.getGoodallsDistance(input, true);
        } else if (method.equalsIgnoreCase(CHISSQUARE)) {
            upperTriangle = EcologicalIndices.getChiSquareDistance(input);
        } else if (method.equalsIgnoreCase(KULCZYNSKI)) {
            upperTriangle = EcologicalIndices.getKulczynskiDistance(input);
        } else if (method.equalsIgnoreCase(BRAYCURTIS)) {
            upperTriangle = EcologicalIndices.getBrayCurtisDistance(input);
        } else if (method.equalsIgnoreCase(HELLINGER)) {
            upperTriangle = EcologicalIndices.getHellingerDistance(input);
        } else if (method.equalsIgnoreCase(EUCLIDEAN)) {
            upperTriangle = EcologicalIndices.getEuclidDistance(input, false);
        } else if (method.equalsIgnoreCase(EUCLIDEAN_NORMALIZED)) {
            upperTriangle = EcologicalIndices.getEuclidDistance(input, true);
        } else
            throw new IOException("Unknown distance: " + method);

        /*
        List<String> pids = doc.getDataSetNames();
        printAsNexus(method + " from " + method + " analysis", pids.size(), pids.toArray(new String[pids.size()]), upperTriangle);
         */

        distances.setFromUpperTriangle(upperTriangle);

        return countNodesUsed;
    }

    /**
     * print a distance matrix in nexus format
     *
     * @param ntax
     * @param names
     * @param values
     */
    public static void printAsNexus(String name, int ntax, String[] names, Vector<Vector<Double>> values) {
        Writer w = new StringWriter();

        try {
            w.write("#NEXUS\n[!Computed by MEGAN using " + name + "]\n");
            w.write("begin taxa;\ndimensions ntax=" + ntax + ";\nend;\n");
            w.write("begin distances;\ndimensions ntax=" + ntax + ";\nformat labels no diagonal triangle=upper;\n");

            w.write("matrix\n");
            int count = 0;
            for (Vector<Double> row : values) {
                w.write("'" + names[count++].replaceAll("'", "_") + "'");
                for (Double value : row) {
                    w.write(" " + value.floatValue());

                }
                w.write("\n");
            }
            w.write("'" + names[count].replaceAll("'", "_") + "'\n");
            w.write(";\nend;\n");
        } catch (IOException e) {
            Basic.caught(e);
        }
        System.err.println(w.toString());
    }
}
