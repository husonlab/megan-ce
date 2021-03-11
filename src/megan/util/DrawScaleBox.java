/*
 *  DrawScaleBox.java Copyright (C) 2021. Daniel H. Huson GPL
 *   
 *   (Some files contain contributions from other authors, who are then mentioned separately.)
 *   
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *   
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *   
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package megan.util;

import megan.core.Document;
import megan.viewer.gui.ColorGradient;
import megan.viewer.gui.NodeDrawer;

import java.awt.*;
import java.util.function.Function;

/**
 * draws the scale box
 * Daniel Huson, 2.2021
 */
public class DrawScaleBox {

    private static final int width = 50;
    private static final int height = 20;

    /**
     * draw the scale box
     */
    public static void draw(Graphics g, int x, int y, Document doc, NodeDrawer nodeDrawer) {
        draw(g, x, y, doc,
                nodeDrawer.getStyle(), nodeDrawer.getScalingType(), (int) Math.round(nodeDrawer.getMaxTotalCount()), nodeDrawer.getMaxNodeHeight());
    }

    public static void draw(Graphics g, int x, int y, Document doc, NodeDrawer.Style style, ScalingType scalingType, int maxCount, int maxNodeSize) {
        var colorGradient = (doc != null ? new ColorGradient(doc.getChartColorManager().getHeatMapTable(), maxCount) : null);

        if (maxCount > 1) {
            try {
                ((Graphics2D) g).setStroke(new BasicStroke(1));
                g.setColor(Color.GRAY);
                g.setFont(Font.decode("Arial-12"));

                g.drawString("Scale:", x, y + 15);

                final int x1 = x + 40;

                if (style == NodeDrawer.Style.HeatMap) {
                    drawHeatMapScale(g, x1, y, colorGradient, scalingType, maxCount);
                    drawBox(g, x1, y, maxCount);
                } // end heatmap
                else if (style == NodeDrawer.Style.BarChart) {
                    drawBarChartScaleBox(g, x1, y, scalingType, maxCount);
                    drawBox(g, x1, y, maxCount);
                } // end bar chart
                else // circle or pie chart or cox comb
                {
                    drawCircleScaleBox(g, x1, y, scalingType, maxCount, maxNodeSize);

                }
            } catch (Exception ignored) {
                // shouldn't throw an exception, but put catch here just not to break anything...
            }
        }
    }

    private static void drawHeatMapScale(Graphics g, int x, int y, ColorGradient colorGradient, ScalingType scalingType, int maxCount) {
        for (int i = 0; i < width; i++) {
            final int value = Math.round((i * maxCount) / (float) width);
            final Color color;
            switch (scalingType) {
                default:
                case LINEAR:
                    color = colorGradient.getColor(value);
                    break;
                case SQRT:
                    color = colorGradient.getColorSqrtScale(value);
                    break;
                case LOG:
                    color = colorGradient.getColorLogScale(value);
                    break;
            }
            g.setColor(color);
            g.drawLine(x + i+1, y, x + i+1, y + height);
        }
    }

    private static void drawBarChartScaleBox(Graphics g, int x, int y, ScalingType scalingType, int maxCount) {
        final Function<Float, Integer> map;
        final int nValues;
        switch (scalingType) {
            case LOG:
                map = count -> (count == 0 || maxCount == 0 ? 0 : (int) Math.round((height * Math.log(count)) / Math.log(maxCount)));
                nValues = 18;
                break;
            case SQRT:
                map = count -> (count == 0 || maxCount == 0 ? 0 : (int) Math.round((height * Math.sqrt(count)) / Math.sqrt(maxCount)));
                nValues = 12;
                break;
            default:
            case LINEAR:
                map = count -> (count == 0 || maxCount == 0 ? 0 : Math.round(count * height / (float) maxCount));
                nValues = 2;
                break;
        }

        g.setColor(Color.LIGHT_GRAY);
        //g.fillRect(x, y, width, height);
        final int[][] points = new int[2][nValues + 1];
        for (int i = 0; i < nValues; i++) {
            points[0][i] = x + (i * width) / (nValues - 1);
            points[1][i] = y + height - map.apply((i * maxCount) / (float) (nValues - 1));
        }
        points[0][nValues] = x + width;
        points[1][nValues] = y + height;

        Polygon polygon = new Polygon(points[0], points[1], points[0].length);
        g.fillPolygon(polygon);
        g.setColor(Color.GRAY);
        g.drawPolygon(polygon);
    }


    private static void drawBox (Graphics g, int x, int y,int maxCount) {
            g.setColor(Color.GRAY);

            g.drawRect(x, y, width, height);
            g.drawLine(x + width / 4, y, x + width / 4, y + height);
            g.drawLine(x + width / 2, y, x + width / 2, y + height);
            g.drawLine(x + 3 * width / 4, y, x + 3 * width / 4, y + height);

            g.setFont(Font.decode("Arial-9"));
            g.drawString("0", x - 3, y + height + 10);
            g.drawString("25", x - 6 + width / 4, y + height + 10);

            g.drawString("50", x - 5 + width / 2, y + height + 10);
            g.drawString("75", x - 5 + 3 * width / 4, y + height + 10);
            g.drawString("100%", x - 5 + width, y + height + 10);


            g.setFont(Font.decode("Arial-12"));
            g.drawString(String.format("%,d", maxCount), x + width + 2, y + 9);
        }

    private static void drawCircleScaleBox(Graphics g, int x, int y, ScalingType scalingType, int maxCount, int maxNodeSize) {
        final Function<Float, Integer> map;
        final int[] percent;
        switch (scalingType) {
            case LOG:
                map = count -> (count == 0 || maxCount == 0 ? 0 : (int) Math.round((maxNodeSize * Math.log(count)) / Math.log(maxCount)));
                percent = new int[]{0, 1, 5, 25, 100};
                break;
            case SQRT:
                map = count -> (count == 0 || maxCount == 0 ? 0 : (int) Math.round((maxNodeSize * Math.sqrt(count)) / Math.sqrt(maxCount)));
                percent = new int[]{0, 25, 50, 75, 100};
                break;
            default:
            case LINEAR:
                map = count -> (count == 0 || maxCount == 0 ? 0 : Math.round(count * maxNodeSize / (float) maxCount));
                percent=new int[]{0,25,50,75,100};
                break;
        }

        for (int i = 1; i <= 4; i++) {
            final int radius = map.apply((percent[i] / 100.0f) * maxCount);
            g.drawOval(Math.round(x + maxNodeSize + 2 - radius), Math.round(y + 2 * maxNodeSize - 2 * radius), 2 * radius, 2 * radius);

            g.setFont(Font.decode("Arial-12"));
            g.drawString(String.format("%,d", maxCount), x + 2 * maxNodeSize + 2, y + 9);

            g.setFont(Font.decode("Arial-8"));
            g.drawString(String.format("%d%%", percent[3]), x + 2 * maxNodeSize + 4, y + 20);
            g.drawString(String.format("%d%%", percent[2]), x + 2 * maxNodeSize + (percent[2] >= 10 ? 4 : 7), y + 29);
            g.drawString(String.format("%d%%", percent[1]), x + 2 * maxNodeSize+(percent[1]>=10?4:7), y + 38);
        }
    }
}
