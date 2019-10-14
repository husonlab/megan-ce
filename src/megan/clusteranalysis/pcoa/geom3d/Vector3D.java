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
 * 3D vector
 * Original code by Ken Perlin
 * Created by huson on 9/16/14.
 */
public class Vector3D extends VectorN {

    /**
     * constructor
     */
    public Vector3D() {
        super(4);
    }

    /**
     * constructor
     *
     * @param x
     * @param y
     * @param z
     */
    public Vector3D(double x, double y, double z) {
        super(4);
        set(x, y, z);
    }

    /**
     * constructor
     *
     * @param x
     * @param y
     * @param z
     * @param w
     */
    public Vector3D(double x, double y, double z, double w) {
        super(4);
        set(x, y, z, w);
    }

    /**
     * constructor
     *
     * @param vector
     */
    public Vector3D(Vector3D vector) {
        super(vector);
    }

    /**
     * set values
     *
     * @param x
     * @param y
     * @param z
     * @param w
     */
    private void set(double x, double y, double z, double w) {
        set(0, x);
        set(1, y);
        set(2, z);
        set(3, w);
    }

    /**
     * set values
     *
     * @param x
     * @param y
     * @param z
     */
    private void set(double x, double y, double z) {
        set(x, y, z, 1);
    }
}
