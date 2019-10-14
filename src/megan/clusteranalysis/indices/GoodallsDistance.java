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

import java.io.IOException;
import java.util.HashSet;
import java.util.Vector;

/**
 * computes the ecological distances
 * Suparna Mitra, 2011
 */
public class GoodallsDistance {
    public static final String NAME = "Goodall";

    /**
     * apply the named computation
     *
     * @param viewer
     * @param method
     * @param distances
     * @return
     * @throws IOException
     */
    public static int apply(ClassificationViewer viewer, String method, Distances distances) {
        System.err.println("Computing " + method + " distances");
        // setup input data:

        Vector<Double[]> input = new Vector<>();
        double[] total = new double[viewer.getNumberOfDatasets()];

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
        for (Double[] numbers : input) {
            for (int i = 0; i < numbers.length; i++) {
                if (total[i] > 0) {
                    numbers[i] /= total[i];
                }
            }
        }

        System.err.println("Nodes used: " + seen.size());

        distances.setFromUpperTriangle(getGoodallsDistance(input, true));

        return countNodesUsed;
    }

    /*
     * evaluate range
     */

    private static Vector<Double> getRange(Vector<Double[]> numbers) {
        Vector<Double> range = new Vector<>();
        for (Double[] array : numbers) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (Double d : array) {
                if (!Double.isNaN(d)) {
                    if (d < min) min = d;
                    if (d > max) max = d;
                }
            }
            range.add(max - min);
        }
        return range;
    }

    /*
     * transpose vector with Double[] inside
     * species names excluded,so that type String is not to need
     * then normalize the values in each column
     */

    private static Vector<Double[]> transpose(Vector<Double[]> numbers, boolean normalized) {
        int row_l = numbers.get(0).length;
        int col_l = numbers.size();
        Vector<Double[]> res = new Vector<>();
        for (int i = 0; i < row_l; i++) {
            Double[] d = new Double[col_l];
            double sum = 0.0;
            //  System.err.print(i + ". column of raw data ");
            for (int j = 0; j < col_l; j++) {
                d[j] = numbers.get(j)[i];
                sum += d[j];
                //      System.err.print(Math.round(d[j]) + " ");
            }
            //   System.err.println("sum=" + sum);
            /*normalize*/
            if (normalized) {
                for (int c = 0; c < d.length; c++) {
                    d[c] = d[c] * 4000 / sum;
                }
                /*printing*/
                //     System.err.print(i + ". column of normalized ");
                //     for (double single : d) {
                //        System.err.print(Math.round(single) + " ");
                //   }
                //    System.err.println();
            }
            res.add(d);
        }
        return res;
    }

    /*
     * half diagonal table of GowerCoefficient
     * a Vector<Double[]> means one column of the table
     */

    private static Vector<Vector<Double[]>> getGowerCoefficient(Vector<Double[]> numbers, Vector<Double> range) {
        Vector<Vector<Double[]>> res = new Vector<>();
        for (int i = 0; i < numbers.size() - 1; i++) {
            Vector<Double[]> column = new Vector<>();
            for (int j = i + 1; j < numbers.size(); j++) {
                Double[] d1 = numbers.get(i);
                Double[] d2 = numbers.get(j);
                int len = d1.length;

                Double[] s = new Double[len];

                for (int k = 0; k < len; k++) {
                    /*
                     * calculate s value
                     */
                    if (d1[k] == 0.0 && d2[k] == 0.0) s[k] = 0.0;
                    else s[k] = 1.0 - Math.abs(d1[k] - d2[k]) / range.get(k);
                }

                column.add(s);
            }
            res.add(column);
        }
        return res;
    }

    /*
     * evaluate the pair ratio of one vector(one row)
     */

    private static Double[] pairRatio(Double[] row) {
        Double[] res = new Double[row.length];
        for (int i = 0; i < row.length; i++) {
            int count = 0;
            boolean existNaN = false;
            for (Double e : row) {
                if (!Double.isNaN(e)) {
                    if (e >= row[i]) count++;
                } else existNaN = true;
            }
            if (!existNaN) res[i] = (count + 0.0) / row.length;
            else res[i] = Double.NaN;
        }
        return res;
    }

    /*
     * evaluate pair ratios of one table
     * in the result:
     * a array of Double means one row(values for a same spice) in table
     */

    private static Vector<Double[]> getPairRatioMatrix(Vector<Double[]> gower) {
        Vector<Double[]> rowindouble = new Vector<>();
        int len = gower.get(0).length;
        int size = gower.size();
        for (int i = 0; i < len; i++) {
            Double[] row = new Double[size];
            for (int j = 0; j < size; j++) {
                row[j] = gower.get(j)[i];
            }
            rowindouble.add(row);
        }
        Vector<Double[]> res = new Vector<>();
        for (int k = 0; k < len; k++) {
            res.add(pairRatio(rowindouble.get(k)));
        }
        return res;
    }


    private static Vector<Double> getSumOfLogVector(Vector<Double[]> pairRatioMatrix) {
        Vector<Double> res = new Vector<>();
        int len = pairRatioMatrix.get(0).length;

        for (int i = 0; i < len; i++) {
            Double sum = 0.0;
            for (Double[] darray : pairRatioMatrix) {
                if (!Double.isNaN(darray[i])) sum += Math.log10(darray[i]);
            }

            res.add(sum);
        }
        return res;
    }

    /*
     * evaluate site. sym. vector via pairRatio()
     */

    private static Vector<Vector<Double>> getSiteSym(Vector<Double> productVector) {
        Vector<Vector<Double>> res = new Vector<>();
        /*convert into array for application of pairRatio*/
        Double[] toArray = new Double[productVector.size()];
        for (int k = 0; k < productVector.size(); k++) {
            toArray[k] = productVector.get(k);
        }
        Double[] pairratio = pairRatio(toArray);
        /*num is the number of data sets
         * index is the pointer on the array
         * */
        int num = (int) (1 + Math.sqrt(1 + 8 * productVector.size())) / 2;
        int index = 0;
        /*adding element in 2 dimension vector ,so that the
         * result looks like half triangle*/
        for (int i = 0; i < num - 1; i++) {
            Vector<Double> newRow = new Vector<>();
            for (int j = i + 1; j < num; j++) {
                newRow.add(pairratio[index++]);
            }
            res.add(newRow);
        }
        return res;
    }

    /*
     * return inverse of vector
     */

    private static Vector<Vector<Double>> getSiteDist(Vector<Vector<Double>> siteSym) {
        Vector<Vector<Double>> res = new Vector<>();
        for (Vector<Double> row : siteSym) {
            Vector<Double> newRow = new Vector<>();
            for (Double pi : row) {
                Double inverse = 1.0 - pi;
                newRow.add(inverse);
            }
            res.add(newRow);
        }
        return res;
    }

    /**
     * computes Goodall's distance
     *
     * @param numbers
     * @param normalized
     * @return Goodall's
     */
    private static Vector<Vector<Double>> getGoodallsDistance(Vector<Double[]> numbers, boolean normalized) {
        Vector<Double> range = getRange(numbers);
        Vector<Double[]> transpose = transpose(numbers, normalized);
        /*
        System.err.println("Range: " + range);

        System.err.println("tranposed:");
        for (Double[] value : transpose) {
            for (Double v : value)
                System.err.print(v + ", ");
            System.err.println();
        }
        */

        Vector<Vector<Double[]>> gowercoeffi1 = getGowerCoefficient(transpose, range);
        //System.err.println("gowercoeffi done");
        /*column-product vector*/
        Vector<Double[]> gowercoeffi2 = new Vector<>();
        for (Vector<Double[]> vectordarray : gowercoeffi1) {
            gowercoeffi2.addAll(vectordarray);
        }
        Vector<Double[]> pairratio = getPairRatioMatrix(gowercoeffi2);
        // System.err.println("pairratio done");
        //Vector<Double> prod = obj.getProductVector(pairratio);
        Vector<Double> sum = getSumOfLogVector(pairratio);
        // System.err.println("Vector done");
        /*
        System.err.println("Vector:");
        for (Double value : sum) {
            System.err.println(value);
        }
        */

        /*site.dist vector*/
        //Vector<Vector<Double>> sitedist = obj.getSiteDist(obj.getSiteSym(prod));
        return getSiteDist(getSiteSym(sum));
    }


}
