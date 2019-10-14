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
 * 3D matrix
 * Original code by Ken Perlin
 * Created by huson on 9/16/14.
 */
public class Matrix3D extends MatrixN {
    /**
     * constructor
     */
    public Matrix3D() {
        super(4);
        identity();
    }

    /**
     * rotate transformation about the X axis
     *
     * @param theta
     */
    public void rotateX(double theta) {

        Matrix3D tmp = new Matrix3D();
        double c = Math.cos(theta);
        double s = Math.sin(theta);

        tmp.set(1, 1, c);
        tmp.set(1, 2, -s);
        tmp.set(2, 1, s);
        tmp.set(2, 2, c);

        preMultiply(tmp);
    }

    /**
     * rotate transformation about the X axis
     *
     * @param theta
     */
    public void rotateY(double theta) {

        Matrix3D tmp = new Matrix3D();
        double c = Math.cos(theta);
        double s = Math.sin(theta);

        tmp.set(2, 2, c);
        tmp.set(2, 0, -s);
        tmp.set(0, 2, s);
        tmp.set(0, 0, c);

        preMultiply(tmp);
    }

    /**
     * rotate transformation about the Z axis
     *
     * @param theta
     */
    public void rotateZ(double theta) {

        Matrix3D tmp = new Matrix3D();
        double c = Math.cos(theta);
        double s = Math.sin(theta);

        tmp.set(0, 0, c);
        tmp.set(0, 1, -s);
        tmp.set(1, 0, s);
        tmp.set(1, 1, c);

        preMultiply(tmp);
    }

    /**
     * translate
     *
     * @param a
     * @param b
     * @param c
     */
    private void translate(double a, double b, double c) {

        Matrix3D tmp = new Matrix3D();

        tmp.set(0, 3, a);
        tmp.set(1, 3, b);
        tmp.set(2, 3, c);

        preMultiply(tmp);
    }

    public void translate(Vector3D v) {
        translate(v.get(0), v.get(1), v.get(2));
    }

    /**
     * scale uniformly
     *
     * @param s
     */
    void scale(double s) {

        Matrix3D tmp = new Matrix3D();

        tmp.set(0, 0, s);
        tmp.set(1, 1, s);
        tmp.set(2, 2, s);

        preMultiply(tmp);
    }

    /**
     * scale non-uniformly
     *
     * @param r
     * @param s
     * @param t
     */
    private void scale(double r, double s, double t) {

        Matrix3D tmp = new Matrix3D();

        tmp.set(0, 0, r);
        tmp.set(1, 1, s);
        tmp.set(2, 2, t);

        preMultiply(tmp);
    }

    /**
     * scale non-uniformly
     *
     * @param v
     */
    public void scale(Vector3D v) {
        scale(v.get(0), v.get(1), v.get(2));
    }
}

