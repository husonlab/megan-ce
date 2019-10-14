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
package megan.clusteranalysis.pcoa.geom3d;

/**
 * Nd vector
 * Original code by Ken Perlin
 * Created by huson on 9/16/14.
 */
public class VectorN {
    final double[] v;

    /**
     * constructor
     *
     * @param n
     */
    public VectorN(int n) {
        v = new double[n];
    }

    /**
     * constructor
     *
     * @param vector
     */
    public VectorN(VectorN vector) {
        v = new double[vector.v.length];
        System.arraycopy(vector.v, 0, v, 0, v.length);
    }

    /**
     * get dimension
     *
     * @return size
     */
    private int size() {
        return v.length;
    }

    /**
     * get component
     *
     * @param j
     * @return j-th component
     */
    public double get(int j) {
        return v[j];
    }

    /**
     * set j-th component
     *
     * @param j
     * @param f
     */
    public void set(int j, double f) {
        v[j] = f;
    }

    /**
     * set
     *
     * @param vector
     */
    public void set(VectorN vector) {
        for (int j = 0; j < size(); j++)
            set(j, vector.get(j));
    }

    /**
     * get string
     *
     * @return string
     */
    public String toString() {
        StringBuilder s = new StringBuilder("{");
        for (int j = 0; j < size(); j++)
            s.append(j == 0 ? "" : ",").append(get(j));
        return s + "}";
    }

    /**
     * multiple by given matrix
     *
     * @param mat
     */
    public void transform(MatrixN mat) {
        final VectorN tmp = new VectorN(size());
        for (int i = 0; i < size(); i++) {
            double f = 0d;
            for (int j = 0; j < size(); j++)
                f += mat.get(i, j) * get(j);
            tmp.set(i, f);
        }
        set(tmp);
    }

    /**
     * compute euclidean distance to given vector
     *
     * @param vector
     * @return distance
     */
    public double distance(VectorN vector) {
        double d = 0d;
        for (int i = 0; i < size(); i++) {
            double x = vector.get(0) - get(0);
            double y = vector.get(1) - get(1);
            d += x * x + y * y;
        }
        return Math.sqrt(d);
    }
}

