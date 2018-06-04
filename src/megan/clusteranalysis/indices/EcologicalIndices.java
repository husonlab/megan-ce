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

import java.text.DecimalFormat;
import java.util.Vector;

/**
 * Compute ecological indices
 * Suparna Mitra, 26.1.2011
 */
public class EcologicalIndices {
    static boolean inTest = false;

    /*
     * evaluate range
     */

    private static Vector<Double> getRange(Vector<Double[]> numbers) {
        Vector<Double> range = new Vector<>();
        for (Double[] array : numbers) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (int i = inTest ? 1 : 0; i < array.length; i++) {
                double d = array[i];
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

    public static Vector<Double[]> transpose(Vector<Double[]> numbers, boolean normalized) {
        int row_l = numbers.get(0).length;
        int col_l = numbers.size();
        Vector<Double[]> res = new Vector<>();
        for (int i = inTest ? 1 : 0; i < row_l; i++) {
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

    private static Vector<Vector<Double>> evaluateGowerfromGowerCoeffi(Vector<Vector<Double[]>> gower) {
        /*
         * calculate S value in form of half diagonal table
         */
        Vector<Vector<Double>> res = new Vector<>();
        for (Vector<Double[]> column : gower) {
            /*in column level*/
            Vector<Double> newcol = new Vector<>();
            for (Double[] darray : column) {
                /*in single entity level*/
                int count = 0;
                Double sum = 0.0;
                for (Double d : darray) {
                    if (!Double.isNaN(d)) {
                        count++;
                        sum += d;
                    }
                }
                Double s = sum / count;
                newcol.add(s);
            }
            res.add(newcol);
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

    /*
     * calculate product of each column
     */

    private static Vector<Double> getProductVector(Vector<Double[]> pairRatioMatrix) {
        Vector<Double> res = new Vector<>();
        int len = pairRatioMatrix.get(0).length;

        for (int i = 0; i < len; i++) {
            Double product = 1.0;
            for (Double[] darray : pairRatioMatrix) {
                if (!Double.isNaN(darray[i])) product *= darray[i];
            }
            res.add(product);
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

    /*
     * calculate Euclid difference
     */

    private static Vector<Vector<Double>> getEuclidDiff(Vector<Double[]> numbers) {
        Vector<Vector<Double>> res = new Vector<>();
        for (int i = 0; i < numbers.size() - 1; i++) {
            Vector<Double> row = new Vector<>();
            for (int j = i + 1; j < numbers.size(); j++) {
                Double[] d1 = numbers.get(i);
                Double[] d2 = numbers.get(j);
                int len = d1.length;
                Double sum = 0.0;
                for (int k = 0; k < len; k++) {
                    sum += Math.pow((d1[k] - d2[k]), 2);
                }
                sum = Math.sqrt(sum);
                row.add(sum);
            }
            res.add(row);
        }
        Double max = 0.0;
        for (Vector<Double> row : res) {
            for (Double singlevalue : row) {
                if (singlevalue > max) max = singlevalue;
            }
        }
        if (max > 0.0) {
            for (Vector<Double> row : res) {
                for (int i = 0; i < row.size(); i++) {
                    Double d = row.get(i) / max;
                    row.set(i, d);
                }
            }
        }
        return res;
    }

    /*
     * calculate Hellinger
     */

    private static Vector<Vector<Double>> getDistHellinger(Vector<Double[]> numbers) {
        Vector<Vector<Double>> res = new Vector<>();
        for (int i = 0; i < numbers.size() - 1; i++) {
            Vector<Double> row = new Vector<>();
            for (int j = i + 1; j < numbers.size(); j++) {
                Double[] d1 = numbers.get(i);
                Double[] d2 = numbers.get(j);
                int len = d1.length;
                Double sum1 = 0.0;
                Double sum2 = 0.0;
                for (int k = 0; k < len; k++) {
                    sum1 += d1[k];
                    sum2 += d2[k];
                }
                Double sum3 = 0.0;
                for (int l = 0; l < len; l++) {
                    sum3 += Math.pow(Math.sqrt(d1[l] / sum1) - Math.sqrt(d2[l] / sum2), 2);
                }
                row.add(Math.sqrt(sum3));
            }
            res.add(row);
        }
        return res;
    }

    /*
     * calculate Bray Curtis
     */

    private static Vector<Vector<Double>> getDistBrayCurtis(Vector<Double[]> numbers) {
        final Vector<Vector<Double>> res = new Vector<>();
        for (int i = 0; i < numbers.size() - 1; i++) {
            final Vector<Double> row = new Vector<>();
            for (int j = i + 1; j < numbers.size(); j++) {
                Double[] d1 = numbers.get(i);
                Double[] d2 = numbers.get(j);
                int len = d1.length;
                double sum1 = 0.0;
                double sum2 = 0.0;
                for (int k = 0; k < len; k++) {
                    sum1 += Math.min(d1[k], d2[k]);
                    sum2 += d1[k] + d2[k];
                }
                if (sum2 != 0)
                    row.add(1 - 2 * sum1 / sum2);
                else
                    row.add(0.0);
            }
            res.add(row);
        }
        return res;
    }

    /*
     * calculate Kulczynski
     */

    private static Vector<Vector<Double>> getDistKulczynski(Vector<Double[]> numbers) {
        final Vector<Vector<Double>> res = new Vector<>();
        for (int i = 0; i < numbers.size() - 1; i++) {
            final Vector<Double> row = new Vector<>();
            for (int j = i + 1; j < numbers.size(); j++) {
                Double[] d1 = numbers.get(i);
                Double[] d2 = numbers.get(j);
                int len = d1.length;
                double sum1 = 0.0;
                double sum2 = 0.0;
                double sum3 = 0.0;
                for (int k = 0; k < len; k++) {
                    sum1 += Math.min(d1[k], d2[k]);
                    sum2 += d1[k];
                    sum3 += d2[k];
                }
                row.add(1 - 0.5 * (sum1 / sum2 + sum1 / sum3));
            }
            res.add(row);
        }
        return res;
    }

    /*
     * calculate Chi.Square
     */

    private static Vector<Vector<Double>> getDistChiSquare(Vector<Double[]> numbers) {
        final Vector<Vector<Double>> res = new Vector<>();
        for (int i = 0; i < numbers.size() - 1; i++) {
            final Vector<Double> row = new Vector<>();
            for (int j = i + 1; j < numbers.size(); j++) {
                final Double[] d1 = numbers.get(i);
                final Double[] d2 = numbers.get(j);
                int len = d1.length;
                double sum1 = 0.0;
                double sum2 = 0.0;
                for (int k = 0; k < len; k++) {
                    sum1 += d1[k];
                    sum2 += d2[k];
                }
                double sum3 = 0.0;
                for (int k = 0; k < len; k++) {
                    if (sum1 > 0 && sum2 > 0 && d1[k] + d2[k] > 0)
                        sum3 += ((sum1 + sum2) / (d1[k] + d2[k])) * Math.pow((d1[k] / sum1) - (d2[k] / sum2), 2);
                }
                row.add(sum3 > 0 ? Math.sqrt(sum3) : 0);

            }
            res.add(row);
        }
        return res;
    }

    /*
     * round double values
     */

    static public double roundTwoDecimals(double d) {
        final DecimalFormat twoDForm = new DecimalFormat("#.###");
        return Double.valueOf(twoDForm.format(d));
    }

    /**
     * computes Goodall's distance
     *
     * @param numbers
     * @param normalized
     * @return Goodall's
     */
    static public Vector<Vector<Double>> getGoodallsDistance(Vector<Double[]> numbers, boolean normalized) {
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
            for (Double[] darray : vectordarray) {
                gowercoeffi2.add(darray);
            }
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

    /**
     * compute chi-square
     *
     * @param numbers
     * @return chi-square distance
     */
    static public Vector<Vector<Double>> getChiSquareDistance(Vector<Double[]> numbers) {
        Vector<Double[]> transpose = transpose(numbers, false);
        return getDistChiSquare(transpose);
    }

    /**
     * Hellinger distance
     *
     * @param numbers
     * @return Hellinger
     */
    static public Vector<Vector<Double>> getHellingerDistance(Vector<Double[]> numbers) {
        Vector<Double[]> transpose = transpose(numbers, false);
        return getDistHellinger(transpose);
    }

    /**
     * Kulczynski distance
     *
     * @param numbers
     * @return Kulczynski
     */
    static public Vector<Vector<Double>> getKulczynskiDistance(Vector<Double[]> numbers) {
        Vector<Double[]> transpose = transpose(numbers, false);
        return getDistKulczynski(transpose);
    }

    static public Vector<Vector<Double>> getBrayCurtisDistance(Vector<Double[]> numbers) {
        Vector<Double[]> transpose = transpose(numbers, false);
        return getDistBrayCurtis(transpose);
    }

    /**
     * Euclidean distance
     *
     * @param numbers
     * @param normalize
     * @return Euclidean
     */
    static public Vector<Vector<Double>> getEuclidDistance(Vector<Double[]> numbers, boolean normalize) {
        Vector<Double[]> transpose = transpose(numbers, normalize);
        return getEuclidDiff(transpose);
    }


}
