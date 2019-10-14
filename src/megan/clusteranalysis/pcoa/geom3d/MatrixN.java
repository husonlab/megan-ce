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
 * ND matrix
 * Original code by Ken Perlin
 * Created by huson on 9/16/14.
 */
public class MatrixN {
    private final VectorN[] v;

    /**
     * constructor
     *
     * @param n
     */
    public MatrixN(int n) {
        v = new VectorN[n];
        for (int i = 0; i < n; i++)
            v[i] = new VectorN(n);
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
     * get component (i,j)
     *
     * @param i
     * @param j
     * @return
     */
    public double get(int i, int j) {
        return get(i).get(j);
    }

    /**
     * set component (i,j)
     *
     * @param i
     * @param j
     * @param f
     */
    void set(int i, int j, double f) {
        v[i].set(j, f);
    }

    /**
     * get one row
     *
     * @param i
     * @return
     */
    private VectorN get(int i) {
        return v[i];
    }

    private void set(int i, VectorN vec) {
        v[i].set(vec);
    }

    private void set(MatrixN mat) {
        for (int i = 0; i < size(); i++)
            set(i, mat.get(i));
    }

    public String toString() {
        StringBuilder s = new StringBuilder("{");
        for (int i = 0; i < size(); i++)
            s.append(i == 0 ? "" : ",").append(get(i));
        return s + "}";
    }

    /**
     * set to identity matrix
     */
    public void identity() {
        for (int j = 0; j < size(); j++)
            for (int i = 0; i < size(); i++)
                set(i, j, (i == j ? 1 : 0));
    }

    /**
     * pre multiple mat x this
     *
     * @param mat
     */
    void preMultiply(MatrixN mat) {
        final MatrixN tmp = new MatrixN(size());
        for (int j = 0; j < size(); j++)
            for (int i = 0; i < size(); i++) {
                double f = 0.;
                for (int k = 0; k < size(); k++)
                    f += mat.get(i, k) * get(k, j);
                tmp.set(i, j, f);
            }
        set(tmp);
    }

    /**
     * post multiple this x mat
     *
     * @param mat
     */
    public void postMultiply(MatrixN mat) {
        final MatrixN tmp = new MatrixN(size());
        for (int j = 0; j < size(); j++)
            for (int i = 0; i < size(); i++) {
                double f = 0.;
                for (int k = 0; k < size(); k++)
                    f += get(i, k) * mat.get(k, j);
                tmp.set(i, j, f);
            }
        set(tmp);
    }

    /**
     * determines whether this is the identity matrix
     *
     * @return true, if identity
     */
    public boolean isIdentity() {
        for (int i = 0; i < v.length; i++) {
            for (int j = 0; j < v.length; j++) {
                if (i == j) {
                    if (v[i].v[j] != 1)
                        return false;
                } else if (v[i].v[j] != 0)
                    return false;
            }
        }
        return true;
    }
}
