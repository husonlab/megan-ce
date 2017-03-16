/*
 *  Copyright (C) 2015 Daniel H. Huson
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

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;

public class CircSpiral extends JPanel {

    public void paintComponent(Graphics gc) {
        float[] values = {1.0f, .7f, 0.3f, 0.01f, 0.001f};
        //float[] values={1.0f,.7f,0.7f,0.6f,0.3f};

        final int nodeHeight = 200;
        final int nodeWidth = 200;

        final int centerX = (getSize().width - nodeHeight) / 2;
        final int centerY = (getSize().height - nodeHeight) / 2;
        final int minX = centerX - nodeWidth / 2;
        final int maxX = centerX + nodeWidth / 2;
        final int minY = centerY - nodeHeight / 2;
        final int maxY = centerY + nodeHeight / 2;


        gc.setColor(Color.DARK_GRAY);
        gc.fillArc(minX - 3, minY - 3, nodeWidth + 6, nodeHeight + 6, 0, 360);

        final float factor = 0.7f;

        {

            float[] x = new float[5];
            float[] y = new float[5];

            for (int i = 0; i < 5; i++) {
                switch (i) {
                    case 0:
                        x[i] = centerX;
                        y[i] = minY + 0.5f * factor * values[i] * nodeHeight;
                        break;
                    case 1:
                        x[i] = maxX - 0.5f * factor * values[i] * nodeWidth;
                        y[i] = centerY;
                        break;
                    case 2:
                        x[i] = centerX;
                        y[i] = maxY - 0.5f * factor * values[i] * nodeHeight;
                        break;
                    case 3:
                        x[i] = minX + 0.5f * factor * values[i] * nodeWidth;
                        y[i] = centerY;
                        break;
                    case 4:
                        x[i] = centerX;
                        y[i] = minY + factor * values[i] * nodeHeight;
                        break;
                }
            }
            GeneralPath.Float gp = new GeneralPath.Float();
            gp.moveTo(x[0], y[0]);

            if (false) {
                gp.lineTo(x[1], y[1]);

                gp.lineTo(x[2], y[2]);
                gp.lineTo(x[3], y[3]);
                gp.lineTo(x[4], y[4]);
            } else {
                gp.quadTo(x[1], y[0], x[1], y[1]);

                gp.quadTo(x[1], y[2], x[2], y[2]);
                gp.quadTo(x[3], y[2], x[3], y[3]);
                gp.quadTo(x[3], y[4], x[4], y[4]);
                gp.lineTo(x[0], y[0]);
            }

            gp.closePath();

            gc.setColor(Color.WHITE);
            ((Graphics2D) gc).fill(gp);

            /*
            gc.setColor(Color.BLUE);
            ((Graphics2D) gc).draw(gp);
            */
        }


        if (false) {
            gc.setColor(Color.RED);

            for (int i = 0; i < 4; i++) {
                ((Graphics2D) gc).setStroke(new BasicStroke(4 - i));
                final int ovalHeight;
                final int ovalWidth;
                final int startAngle;
                switch (i) {
                    default:
                    case 0: {
                        ovalHeight = (int) Math.round((1 - factor * values[i]) * nodeHeight);
                        gc.drawLine(centerX, centerY - nodeHeight / 2, centerX, centerY - nodeHeight / 2 + (nodeHeight - ovalHeight) / 2);
                        ovalWidth = (int) Math.round((1 - factor * values[i + 1]) * nodeWidth);
                        startAngle = 90;
                        break;
                    }
                    case 1: {
                        ovalHeight = (int) Math.round((1 - factor * values[i + 1]) * nodeHeight);
                        ovalWidth = (int) Math.round((1 - factor * values[i]) * nodeWidth);
                        startAngle = 180;
                        break;
                    }
                    case 2: {
                        ovalHeight = (int) Math.round((1 - factor * values[i]) * nodeHeight);
                        ovalWidth = (int) Math.round((1 - factor * values[i + 1]) * nodeWidth);
                        startAngle = 270;
                        break;
                    }
                    case 3: {
                        ovalHeight = (int) Math.round((1 - factor * values[i + 1]) * nodeHeight);
                        ovalWidth = (int) Math.round((1 - factor * values[i]) * nodeWidth);
                        startAngle = 0;
                        break;
                    }
                }
                gc.drawArc(centerX - ovalWidth / 2, centerY - ovalHeight / 2, ovalWidth, ovalHeight, 180 - startAngle, -90);
            }
        }

        gc.setColor(Color.BLACK);
        gc.drawArc(minX - 3, minY - 3, nodeWidth + 6, nodeHeight + 6, 0, 360);

    }

    public static void main(String[] args) {
        CircSpiral panel = new CircSpiral();
        JFrame application = new JFrame();
        application.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        application.add(panel);
        application.setSize(300, 300);
        application.setVisible(true);
    }
}