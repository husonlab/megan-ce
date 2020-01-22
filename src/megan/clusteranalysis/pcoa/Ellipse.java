/*
 * Ellipse.java Copyright (C) 2020. Daniel H. Huson
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
 *
 */

package megan.clusteranalysis.pcoa;

import jloda.swing.graphview.Transform;
import jloda.swing.util.Geometry;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * ellipse
 * Created by huson on 9/14/16.
 */
public class Ellipse {
    private double centerX;
    private double centerY;
    private double lengthA;
    private double lengthB;
    private double angleInRadians;
    private Color color;

    /**
     * default constructor
     */
    public Ellipse() {
    }

    /**
     * constructor
     *
     * @param centerX
     * @param centerY
     * @param lengthA
     * @param lengthB
     * @param angleInRadians
     */
    public Ellipse(double centerX, double centerY, double lengthA, double lengthB, double angleInRadians) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.lengthA = lengthA;
        this.lengthB = lengthB;
        this.angleInRadians = angleInRadians;
    }

    /**
     * paint the ellipse
     *
     * @param g
     */
    public void paint(Graphics g) {
        final Graphics2D g2d = (Graphics2D) g;
        final AffineTransform old = g2d.getTransform();
        if (color != null)
            g2d.setColor(color);
        g2d.rotate(angleInRadians, centerX, centerY);
        g2d.drawOval((int) Math.round(centerX - lengthA), (int) Math.round(centerY - lengthB),
                (int) Math.round(2 * lengthA), (int) Math.round(2 * lengthB));
        g2d.setTransform(old);
    }

    /**
     * paint the ellipse
     *
     * @param g
     */
    public void paint(Graphics g, Transform trans) {
        final Graphics2D g2d = (Graphics2D) g;
        if (color != null)
            g2d.setColor(color);
        final Point center = trans.w2d(centerX, centerY);

        final double lenX = Geometry.length(Geometry.diff(trans.w2d(lengthA, 0), trans.w2d(0, 0)));
        final double lenY = Geometry.length(Geometry.diff(trans.w2d(0, lengthB), trans.w2d(0, 0)));

        final AffineTransform old = g2d.getTransform();
        g2d.rotate(angleInRadians, center.getX(), center.getY());
        g2d.drawOval((int) Math.round(center.getX() - lenX), (int) Math.round(center.getY() - lenY),
                (int) Math.round(2 * lenX), (int) Math.round(2 * lenY));
        g2d.setTransform(old);
    }

    public double getCenterX() {
        return centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX = centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public void setCenterY(double centerY) {
        this.centerY = centerY;
    }

    public double getLengthA() {
        return lengthA;
    }

    public void setLengthA(double lengthA) {
        this.lengthA = lengthA;
    }

    public double getLengthB() {
        return lengthB;
    }

    public void setLengthB(double lengthB) {
        this.lengthB = lengthB;
    }

    public double getAngleInRadians() {
        return angleInRadians;
    }

    public void setAngleInRadians(double angleInRadians) {
        this.angleInRadians = angleInRadians;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
