/*
 * Distances.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.clusteranalysis.tree;

import java.util.Vector;

/**
 * a simple distance matrix
 * Daniel Huson, 5.2010
 */
public class Distances {
    private final double[][] matrix;

    public Distances(int ntax) {
        matrix = new double[ntax][ntax];
    }

    /**
     * get the value
     *
     * @param i between 1 and ntax
     * @param j between 1 and ntax
     * @return value
     */
    public double get(int i, int j) {
        return matrix[i - 1][j - 1];
    }

    /**
     * set the value
     *
     * @param i     between 1 and ntax
     * @param j     between 1 and ntax
	 */
    public void set(int i, int j, double value) {
        matrix[i - 1][j - 1] = matrix[j - 1][i - 1] = value;
    }

    /**
     * increment the count
     *
	 */
    public void increment(int i, int j) {
        matrix[i - 1][j - 1]++;
        matrix[j - 1][i - 1]++;
    }

    /**
     * get the number of taxa
     *
     * @return number of taxa
     */
    public int getNtax() {
        return matrix.length;
    }

    /**
     * sets the distances from an upper triangle representation of distances
     *
	 */
    public void setFromUpperTriangle(Vector<Vector<Double>> upperTriangle) {
        for (int i = 0; i < upperTriangle.size(); i++) {
            matrix[i][i] = 0;
            Vector<Double> row = upperTriangle.get(i);
            for (int count = 0; count < row.size(); count++)
                matrix[i][i + count + 1] = matrix[i + count + 1][i] = row.get(count);
        }
    }

    /**
     * get the matrix
     *
     * @return matrix
     */
    public double[][] getMatrix() {
        return matrix;
    }

    /**
     * replace all NaN by zero
     */
    public boolean replaceNaNByZero() {
        boolean changed = false;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                if (Double.isNaN(matrix[i][j])) {
                    matrix[i][j] = 0;
                    changed = true;
                }
            }
        }
        return changed;
    }
}
