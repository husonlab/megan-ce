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

package megan.viewer.gui;

import jloda.swing.util.Geometry;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;

public class SpiralNode8 extends JPanel {

    public void paintComponent(Graphics gc) {
        float[] values = {1.0f, 1.0f, 1.0f, 0.8f, 0, 1.0f, 1.0f, 1.0f, 0.8f, 0, 0, 0, 0, 0, 0, 0.7f, 0.7f, 0.6f, 0.6f, 0.3f, 0.1f, 0.01f, 0f, 0.7f, 0.6f, 0.6f, 0.3f, 0f};

        ArrayList<Float> sorted = new ArrayList<>();
        for (float f : values)
            sorted.add(f);
        sorted.sort((a, b) -> -a.compareTo(b));

        final int nodeHeight = 200;
        final int nodeWidth = 200;

        final int centerX = (getSize().width - nodeHeight) / 2;
        final int centerY = (getSize().height - nodeHeight) / 2;
        final int minX = centerX - nodeWidth / 2;
        final int minY = centerY - nodeHeight / 2;

        gc.setColor(Color.LIGHT_GRAY);
        gc.fillArc(minX - 1, minY - 1, nodeWidth + 2, nodeHeight + 2, 0, 360);


        {
            final int steps = Math.min(36, sorted.size());
            final int add = Math.max(1, sorted.size() / steps);
            final float max = sorted.get(0);
            final double scale = 0.5 * (max > 0 ? 1.0 / max : 1);

            GeneralPath.Float gp = new GeneralPath.Float();
            gp.moveTo(centerX, minY);
            final double radius = 0.5 * nodeHeight;
            final double delta = 2 * Math.PI / steps;

            for (int i = 0; i < steps; i += add) {
                double angle = Math.PI + i * delta;
                double dist = radius * (1 - scale * sorted.get(i));
                Point2D apt = Geometry.rotate(new Point2D.Double(0, dist), angle);
                gp.lineTo(centerX + apt.getX(), centerY + apt.getY());
            }
            gp.closePath();

            gc.setColor(Color.WHITE);
            ((Graphics2D) gc).fill(gp);
        }

        gc.setColor(Color.BLACK);
        gc.drawArc(minX - 1, minY - 1, nodeWidth + 2, nodeHeight + 2, 0, 360);

    }

    public static void main(String[] args) {
        SpiralNode8 panel = new SpiralNode8();
        JFrame application = new JFrame();
        application.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        application.add(panel);
        application.setSize(300, 300);
        application.setVisible(true);
    }
}