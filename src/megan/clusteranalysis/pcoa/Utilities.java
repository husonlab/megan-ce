/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.clusteranalysis.pcoa;

import Jama.Matrix;
import jloda.util.Basic;

/**
 * Math utilities for computing PCoA and biplot.
 * Daniel Huson, 7.2014
 */
public class Utilities {

    /**
     * compute centered inner product matrix
     *
     * @param matrix
     * @return new matrix
     */
    public static Matrix computeDoubleCenteringOfSquaredMatrix(Matrix matrix) {
        int size = matrix.getColumnDimension();
        Matrix result = new Matrix(matrix.getColumnDimension(), matrix.getRowDimension());
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double v1 = 0;
                for (int k = 0; k < size; k++) {
                    v1 += matrix.get(k, j) * matrix.get(k, j) / size;
                }
                double v2 = 0;
                for (int k = 0; k < size; k++) {
                    v2 += matrix.get(i, k) * matrix.get(i, k) / size;
                }
                double v3 = 0;
                for (int k = 0; k < size; k++) {
                    for (int l = 0; l < size; l++) {
                        v3 += matrix.get(k, l) * matrix.get(k, l) / (size * size);
                    }
                }
                double v4 = matrix.get(i, j);
                result.set(i, j, 0.5 * (v1 + v2 - v3 - (v4 * v4)));
            }
        }
        return result;
    }

    /**
     * center and scale a given matrix.
     * Center means: subtract column-mean from each column
     * Scale means: divide each column by square root of (sum-of-squares of column / (number of columns -1))
     * See http://docs.tibco.com/pub/enterprise-runtime-for-R/1.1.0-november-2012/TERR_1.1.0_LanguageRef/base/scale.html
     *
     * @param matrix
     * @return new matrix
     */
    public static double[][] centerAndScale(double[][] matrix) {
        final double[][] result = matrix.clone();
        final int nRows = result.length;
        final int nCols = result[0].length;

        if (nRows < 2 || nCols == 0) // matrix to
            return result;

        // center:
        for (int col = 0; col < nCols; col++) {
            double mean = 0;
            for (double[] aRow : result) {
                mean += aRow[col];
            }
            mean /= nRows;
            for (double[] aRow : result) {
                aRow[col] -= mean;
            }
        }
        // scale:
        for (int col = 0; col < nCols; col++) {
            double sumOfSquares = 0;
            for (double[] aRow : result) {
                sumOfSquares += aRow[col] * aRow[col];
            }
            double value = Math.sqrt(sumOfSquares / (nRows - 1));
            if (value != 0) {
                for (double[] aRow : result) {
                    aRow[col] /= value;
                }
            }
        }

        return result;
    }

    /**
     * sort indices by values
     *
     * @param m
     * @return sorted indices
     * todo: replace by proper sorting
     */
    public static int[] sortValues(Matrix m) {
        double[] v = new double[m.getColumnDimension()];
        int[] index = new int[v.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = m.get(i, i);
            index[i] = i;
        }

        for (int i = 0; i < v.length; i++) {
            for (int j = i + 1; j < v.length; j++) {
                if (Math.abs(v[i]) < Math.abs(v[j])) {
                    double tmpValue = v[j];
                    v[j] = v[i];
                    v[i] = tmpValue;
                    int tmpIndex = index[j];
                    index[j] = index[i];
                    index[i] = tmpIndex;
                }
            }
        }
        return index;
    }

    /**
     * compute the covariance between the columns of two matrices
     *
     * @param x
     * @param y
     * @param biasCorrected (if true, multiples values by (nRows/(nRows-1))
     * @return convariance
     */
    public static double[][] computeCovariance(final double[][] x, final double[][] y, final boolean biasCorrected) {
        final int rowsX = x.length;
        final int colsX = x[0].length;
        final int rowsY = y.length;
        final int colsY = y[0].length;
        final double[][] cov = new double[colsX][colsY];

        System.err.println("rowsCov: " + colsX + " colsCov: " + colsY);

        final double[] meanColX = new double[colsX];
        for (int colX = 0; colX < colsX; colX++) {
            double mean = 0;
            for (double[] row : x) {
                mean += row[colX];
            }
            meanColX[colX] = mean / rowsX;
        }

        final double[] meanColY = new double[colsY];
        for (int colY = 0; colY < colsY; colY++) {
            double mean = 0;
            for (double[] row : y) {
                mean += row[colY];
            }
            meanColY[colY] = mean / rowsY;
        }

        for (int colX = 0; colX < colsX; colX++) {
            for (int colY = 0; colY < colsY; colY++) {
                double result = 0;
                for (int row = 0; row < rowsX; row++) {
                    final double xDev = x[row][colX] - meanColX[colX];
                    final double yDev = y[row][colY] - meanColY[colY];
                    result += (xDev * yDev - result) / (row + 1);
                }
                cov[colX][colY] = biasCorrected ? result * ((double) rowsX / (double) (rowsX - 1)) : result;
            }
        }
        return cov;
    }

    /**
     * matrix multiplication
     *
     * @param x
     * @param y
     * @return x*y
     */
    public static double[][] multiply(double[][] x, double[][] y) {
        final int rowsX = x.length;
        final int colsX = x[0].length;
        final int rowsY = y.length;
        final int colsY = y[0].length;

        if (colsX != rowsY)
            throw new RuntimeException("multiply(x,y): incompatible dimensions");

        final double[][] z = new double[rowsX][colsY];

        for (int a = 0; a < rowsX; a++) {
            for (int b = 0; b < colsY; b++) {
                double value = 0;
                for (int c = 0; c < colsX; c++) {
                    value += x[a][c] * y[c][b];
                }
                z[a][b] = value;
            }
        }
        return z;
    }

    /**
     * creates a  matrix with a and b on the diagonal and all other entries are zero
     *
     * @param values
     * @return matrix
     */
    public static double[][] diag(double[] values) {
        final int dim = values.length;
        final double[][] matrix = new double[dim][dim];
        for (int i = 0; i < dim; i++) {
            matrix[i][i] = values[i];
        }
        return matrix;
    }

    public static void main(String[] args) {
        double[][] matrix = {{1, 3, 2}, {2, 6, 4}, {3, 9, 8}};

        System.err.println(Basic.toString(matrix));

        matrix = multiply(matrix, matrix);
        System.err.println(Basic.toString(matrix));
    }

    public static void scalarMultiply(final double value, double[] vector) {
        for (int i = 0; i < vector.length; i++)
            vector[i] *= value;
    }

    public static void sqrt(double[] vector) {
        for (int i = 0; i < vector.length; i++)
            vector[i] = Math.sqrt(vector[i]);
    }

    public static void invertValues(double[] vector) {
        for (int i = 0; i < vector.length; i++)
            vector[i] = 1.0 / vector[i];
    }

    /**
     * copy only first nCols of matrix
     *
     * @param matrix
     * @param nCols
     * @return copy with first nCols
     */
    public static double[][] truncateRows(double[][] matrix, int nCols) {
        final int nRows = matrix.length;
        final double[][] result = new double[nRows][nCols];
        for (int row = 0; row < nRows; row++) {
            System.arraycopy(matrix[row], 0, result[row], 0, nCols);
        }
        return result;
    }

    /**
     * gets squared length of a vector
     *
     * @param vector
     * @return squared length
     */
    public static double getSquaredLength(double[] vector) {
        double result = 0;
        for (double value : vector)
            result += value * value;
        return result;
    }
}
