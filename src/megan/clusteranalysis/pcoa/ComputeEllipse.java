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

package megan.clusteranalysis.pcoa;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import jloda.swing.util.Geometry;
import jloda.util.APoint2D;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class ComputeEllipse {
    /**
     * Direct ellipse fit, proposed in article
     * A. W. Fitzgibbon, M. Pilu, R. B. Fisher "Direct Least Squares Fitting of Ellipses" IEEE Trans. PAMI, Vol. 21, pages 476-480 (1999)
     * <p>
     * This is a java reimplementation of the mathlab function provided in
     * http://de.mathworks.com/matlabcentral/fileexchange/22684-ellipse-fit--direct-method-/content/EllipseDirectFit.m
     * <p>
     * This code is based on a numerically stable version
     * of this fit published by R. Halir and J. Flusser
     * <p>
     * Input:  points) is the array of 2D coordinates of n points
     * <p>
     * Output: A = [a b c d e f]' is the vector of algebraic
     * parameters of the fitting ellipse:
     * ax^2 + bxy + cy^2 +dx + ey + f = 0
     * the vector A is normed, so that ||A||=1
     * <p>
     * This is a fast non-iterative ellipse fit.
     * <p>
     * It returns ellipses only, even if points are
     * better approximated by a hyperbola.
     * It is somewhat biased toward smaller ellipses.
     *
     * @param points input 2D points
     * @return algebraic parameters of the fitting ellipse
     */
    private static double[] apply(final double[][] points) {
        final int nPoints = points.length;
        final double[] centroid = getMean(points);
        final double xCenter = centroid[0];
        final double yCenter = centroid[1];
        final double[][] d1 = new double[nPoints][3];
        for (int i = 0; i < nPoints; i++) {
            final double xixC = points[i][0] - xCenter;
            final double yiyC = points[i][1] - yCenter;
            d1[i][0] = xixC * xixC;
            d1[i][1] = xixC * yiyC;
            d1[i][2] = yiyC * yiyC;
        }
        final Matrix D1 = new Matrix(d1);
        final double[][] d2 = new double[nPoints][3];
        for (int i = 0; i < nPoints; i++) {
            d2[i][0] = points[i][0] - xCenter;
            d2[i][1] = points[i][1] - yCenter;
            d2[i][2] = 1;
        }

        final Matrix D2 = new Matrix(d2);
        final Matrix S1 = D1.transpose().times(D1);
        final Matrix S2 = D1.transpose().times(D2);
        final Matrix S3 = D2.transpose().times(D2);
        final Matrix T = (S3.inverse().times(-1)).times(S2.transpose());
        final Matrix M = S1.plus(S2.times(T));

        final double[][] m = M.getArray();
        final double[][] n = {{m[2][0] / 2, m[2][1] / 2, m[2][2] / 2}, {-m[1][0], -m[1][1], -m[1][2]},
                {m[0][0] / 2, m[0][1] / 2, m[0][2] / 2}};

        final Matrix N = new Matrix(n);
        final EigenvalueDecomposition E = N.eig();
        final Matrix eVec = E.getV();

        final Matrix R1 = eVec.getMatrix(0, 0, 0, 2);
        final Matrix R2 = eVec.getMatrix(1, 1, 0, 2);
        final Matrix R3 = eVec.getMatrix(2, 2, 0, 2);

        final Matrix cond = (R1.times(4)).arrayTimes(R3).minus(R2.arrayTimes(R2));

        int firstPositiveIndex = 0;
        for (int i = 0; i < 3; i++) {
            if (cond.get(0, i) > 0) {
                firstPositiveIndex = i;
                break;
            }
        }
        final Matrix A1 = eVec.getMatrix(0, 2, firstPositiveIndex, firstPositiveIndex);

        final Matrix A = new Matrix(6, 1);
        A.setMatrix(0, 2, 0, 0, A1);
        A.setMatrix(3, 5, 0, 0, T.times(A1));

        final double[] a = A.getColumnPackedCopy();
        final double a4 = a[3] - 2 * a[0] * xCenter - a[1] * yCenter;
        final double a5 = a[4] - 2 * a[2] * yCenter - a[1] * xCenter;
        final double a6 = a[5] + a[0] * xCenter * xCenter + a[2] * yCenter * yCenter + a[1] * xCenter * yCenter - a[3] * xCenter - a[4] * yCenter;
        A.set(3, 0, a4);
        A.set(4, 0, a5);
        A.set(5, 0, a6);

        final Matrix Anorm = A.times(1 / A.normF());
        return Anorm.getColumnPackedCopy();
    }

    /**
     * compute the mean coordinate of a set of points
     *
     * @param points
     * @return mean
     */
    private static double[] getMean(final double[][] points) {
        final int dim = points[0].length;
        final double[] result = new double[dim];

        for (double[] point : points) {
            for (int i = 0; i < dim; i++) {
                result[i] += point[i];
            }
        }

        for (int i = 0; i < dim; i++) {
            result[i] /= points.length;
        }
        return result;
    }

    /**
     * converts variable description of ellipse to dimensions
     * Based on http://mathworld.wolfram.com/Ellipse.html
     *
     * @param variables
     * @return dimensions centerX,centerY,lengthAxisA,lengthAxisB,angle
     */
    private static double[] convertVariablesToDimension(final double[] variables) {
        final double a = variables[0];
        final double b = variables[1] / 2;
        final double c = variables[2];
        final double d = variables[3] / 2;
        final double f = variables[4] / 2;
        final double g = variables[5];

        final double centerX = (c * d - b * f) / (b * b - a * c);
        final double centerY = (a * f - b * d) / (b * b - a * c);

        final double numerator = 2 * (a * f * f + c * d * d + g * b * b - 2 * b * d * f - a * c * g);
        final double lengthAxisA = Math.sqrt((numerator) / ((b * b - a * c) * (Math.sqrt((a - c) * (a - c) + 4 * b * b) - (a + c))));
        final double lengthAxisB = Math.sqrt((numerator) / ((b * b - a * c) * (-Math.sqrt((a - c) * (a - c) + 4 * b * b) - (a + c))));

        double phi = 0;
        if (b == 0) {
            if (a <= c)
                phi = 0;
            else if (a > c)
                phi = Math.PI / 2;
        } else {
            if (a < c)
                phi = Math.atan(2 * b / (a - c)) / 2;
            else if (a > c)
                phi = Math.atan(2 * b / (a - c)) / 2 + Math.PI / 2;
        }
        return new double[]{centerX, centerY, lengthAxisA, lengthAxisB, phi};
    }

    /**
     * compute an ellipse
     *
     * @param points
     * @return ellipse
     */
    public static Ellipse computeEllipse(ArrayList<Point2D> points) {
        final double[][] array = new double[points.size()][2];
        int i = 0;
        for (Point2D aPoint : points) {
            array[i][0] = aPoint.getX();
            array[i++][1] = aPoint.getY();
        }

        final double[] dimensions = convertVariablesToDimension(apply(array));
        return new Ellipse(dimensions[0], dimensions[1], dimensions[2], dimensions[3], dimensions[4]);
    }

    /**
     * compute an ellipse
     *
     * @param points
     * @return ellipse
     */
    public static javafx.scene.shape.Ellipse computeEllipseFX(ArrayList<APoint2D> points) {
        final double[][] array = new double[points.size()][2];
        int i = 0;
        for (APoint2D aPoint : points) {
            array[i][0] = aPoint.getX();
            array[i++][1] = aPoint.getY();
        }

        final double[] dimensions = convertVariablesToDimension(apply(array));

        javafx.scene.shape.Ellipse ellipse = new javafx.scene.shape.Ellipse(dimensions[0], dimensions[1], dimensions[2], dimensions[3]);
        ellipse.setRotate(Geometry.rad2deg(dimensions[4]));
        return ellipse;
    }
}