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
package megan.chart.drawers;

import jloda.swing.util.BasicSwing;
import jloda.swing.util.Geometry;
import jloda.util.ProgramProperties;
import megan.chart.IChartDrawer;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * draws a bricks chart
 * Daniel Huson, 3.2013
 */
public class BricksChartDrawer extends BubbleChartDrawer implements IChartDrawer {
    private static final String NAME = "BricksChart";

    /**
     * constructor
     */
    public BricksChartDrawer() {
    }

    /**
     * draw bricks with colors representing classes
     *
     * @param gc
     */
    public void drawChart(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));

        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        if (x0 >= x1)
            return;

        int numberOfDataSets = getChartData().getNumberOfSeries();
        int numberOfClasses = getChartData().getNumberOfClasses();

        double xStep = (x1 - x0) / numberOfDataSets;
        double yStep = (y0 - y1) / (0.5 + numberOfClasses);

        double maxValue = getChartData().getRange().get2().doubleValue();
        if (scalingType == ChartViewer.ScalingType.LOG && maxValue > 0)
            maxValue = Math.log(maxValue);
        else if (scalingType == ChartViewer.ScalingType.SQRT && maxValue > 0)
            maxValue = Math.sqrt(maxValue);
        else if (scalingType == ChartViewer.ScalingType.PERCENT)
            maxValue = 100;

        int gridWidth = 5;
        double drawWidth = (double) maxRadius / (double) gridWidth;
        int totalBoxes = gridWidth * gridWidth;
        int e = maxValue > 0 ? (int) Math.ceil(Math.log10(maxValue)) : 0;
        int x = (int) Math.ceil(maxValue / Math.pow(10, e));
        int boxValue = (int) ((x * Math.pow(10, e)) / totalBoxes);

        // main drawing loop:
        int d = 0;
        for (String series : getChartData().getSeriesNames()) {
            if (isShowXAxis()) {
                double xLabel = x0 + (d + 0.5) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 10);
                String label = seriesLabelGetter.getLabel(series);
                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                if (classLabelAngle == 0) {
                    apt.setLocation(apt.getX() - labelSize.getWidth() / 2, apt.getY());
                } else if (classLabelAngle > Math.PI / 2) {
                    apt = Geometry.translateByAngle(apt, classLabelAngle, -labelSize.width);
                }
                if (getChartData().getChartSelection().isSelected(series, null)) {
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    fillAndDrawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, classLabelAngle, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                }
                gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.DARK_GRAY));
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{series, null});
                drawString(gc, label, apt.getX(), apt.getY(), classLabelAngle);
                if (sgc != null)
                    sgc.clearCurrentItem();
            }

            int c = 0;
            for (String className : getChartData().getClassNames()) {
                double value;
                if (scalingType == ChartViewer.ScalingType.PERCENT) {
                    double total = getChartData().getTotalForSeriesIncludingDisabledAttributes(series);
                    if (total == 0)
                        value = 0;
                    else
                        value = 100 * getChartData().getValueAsDouble(series, className) / total;
                } else if (scalingType == ChartViewer.ScalingType.LOG) {
                    value = getChartData().getValueAsDouble(series, className);
                    if (value > 1)
                        value = Math.log(value);
                } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                    value = getChartData().getValueAsDouble(series, className);
                    if (value > 0)
                        value = Math.sqrt(value);
                } else
                    value = getChartData().getValueAsDouble(series, className);

                Point bottomLeft = new Point((int) ((x0 + (d + 0.5) * xStep) - maxRadius / 2), (int) ((y0 - (c + 1) * yStep)));
                Color color = getChartColors().getClassColor(class2HigherClassMapper.get(className), 150);
                gc.setColor(color);

                int numberOfBoxes = (value <= 0 ? 0 : (int) Math.ceil(totalBoxes / maxValue * value));
                int currentWidth = Math.min(totalBoxes, (int) Math.ceil(Math.sqrt(numberOfBoxes + 1)));
                Rectangle2D rect = new Rectangle2D.Double();
                {
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{series, className});
                    int row = 0;
                    int col = 0;
                    for (int i = 1; i <= numberOfBoxes; i++) {
                        rect.setRect(bottomLeft.x + col * drawWidth, bottomLeft.y - row * drawWidth, drawWidth, drawWidth);
                        if (i == numberOfBoxes)  // scale the last box to show how full it is:
                        {
                            double coveredValue = (numberOfBoxes - 1) * boxValue;
                            double diff = value - coveredValue;
                            double factor = diff / boxValue;
                            double height = rect.getHeight() * factor;
                            double y = rect.getY() + (rect.getHeight() - height);
                            rect.setRect(rect.getX(), y, rect.getWidth(), height);
                        }

                        gc.fill(rect);
                        if ((i % currentWidth) == 0) {
                            col = 0;
                            row++;
                        } else
                            col++;
                    }
                    if (sgc != null)
                        sgc.clearCurrentItem();
                }
                gc.setColor(color.darker());
                {
                    int row = 0;
                    int col = 0;
                    for (int i = 1; i <= numberOfBoxes; i++) {
                        rect.setRect(bottomLeft.x + col * drawWidth, bottomLeft.y - row * drawWidth, drawWidth, drawWidth);
                        gc.draw(rect);
                        if ((i % currentWidth) == 0) {
                            col = 0;
                            row++;
                        } else
                            col++;
                    }
                }

                boolean isSelected = getChartData().getChartSelection().isSelected(series, className);

                if (isSelected) {
                    gc.setStroke(HEAVY_STROKE);
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    int row = 0;
                    int col = 0;
                    for (int i = 1; i <= numberOfBoxes; i++) {
                        rect.setRect(bottomLeft.x + col * drawWidth, bottomLeft.y - row * drawWidth, drawWidth, drawWidth);
                        gc.draw(rect);
                        if ((i % currentWidth) == 0) {
                            col = 0;
                            row++;
                        } else
                            col++;
                    }
                    gc.setStroke(NORMAL_STROKE);
                }
                c++;
                if (showValues || isSelected) {
                    String label = "" + (int) getChartData().getValueAsDouble(series, className);
                    valuesList.add(new DrawableValue(label, bottomLeft.x + maxRadius + 2, bottomLeft.y - maxRadius / 2, isSelected));
                }
            }
            d++;
        }
        if (valuesList.size() > 0) {
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            DrawableValue.drawValues(gc, valuesList, false, true);
            valuesList.clear();
        }
        drawScale(gc, drawWidth, boxValue);
    }


    /**
     * draw bricks in which colors are by sample
     *
     * @param gc
     */
    public void drawChartTransposed(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));

        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        if (x0 >= x1)
            return;

        int numberOfClasses = getChartData().getNumberOfClasses();
        int numberOfDataSets = getChartData().getNumberOfSeries();

        double xStep = (x1 - x0) / numberOfClasses;
        double yStep = (y0 - y1) / (0.5 + numberOfDataSets);

        double maxValue = getChartData().getRange().get2().doubleValue();
        if (scalingType == ChartViewer.ScalingType.LOG && maxValue > 0)
            maxValue = Math.log(maxValue);
        else if (scalingType == ChartViewer.ScalingType.SQRT && maxValue > 0)
            maxValue = Math.sqrt(maxValue);
        else if (scalingType == ChartViewer.ScalingType.PERCENT)
            maxValue = 100;

        int gridWidth = 5;
        double drawWidth = (double) maxRadius / (double) gridWidth;
        int totalBoxes = gridWidth * gridWidth;
        int e = maxValue > 0 ? (int) Math.ceil(Math.log10(maxValue)) : 0;
        int x = (int) Math.ceil(maxValue / Math.pow(10, e));
        int boxValue = (int) ((x * Math.pow(10, e)) / totalBoxes);

        // main drawing loop:
        int c = 0;
        for (String className : getChartData().getClassNames()) {
            if (isShowXAxis()) {

                double xLabel = x0 + (c + 0.5) * xStep;
                Dimension labelSize = BasicSwing.getStringSize(gc, className, gc.getFont()).getSize();
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 10);
                if (classLabelAngle == 0) {
                    apt.setLocation(apt.getX() - labelSize.getWidth() / 2, apt.getY());
                } else if (classLabelAngle > Math.PI / 2) {
                    apt = Geometry.translateByAngle(apt, classLabelAngle, -labelSize.width);
                }
                if (getChartData().getChartSelection().isSelected(null, className)) {
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    fillAndDrawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, classLabelAngle, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                }
                gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.DARK_GRAY));
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{null, className});
                drawString(gc, className, apt.getX(), apt.getY(), classLabelAngle);
                if (sgc != null)
                    sgc.clearCurrentItem();
            }
            int d = 0;
            for (String series : getChartData().getSeriesNames()) {
                double value;
                if (scalingType == ChartViewer.ScalingType.PERCENT) {
                    double total = getChartData().getTotalForClassIncludingDisabledSeries(className);
                    if (total == 0)
                        value = 0;
                    else
                        value = 100 * getChartData().getValueAsDouble(series, className) / total;
                } else if (scalingType == ChartViewer.ScalingType.LOG) {
                    value = getChartData().getValueAsDouble(series, className);
                    if (value > 1)
                        value = Math.log(value);
                } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                    value = getChartData().getValueAsDouble(series, className);
                    if (value > 1)
                        value = Math.sqrt(value);
                } else
                    value = getChartData().getValueAsDouble(series, className);

                Point bottomLeft = new Point((int) ((x0 + (c + 0.5) * xStep) - maxRadius / 2), (int) ((y0 - (d + 1) * yStep)));

                Color color = getChartColors().getSampleColorWithAlpha(series, 150);
                gc.setColor(color);

                int numberOfBoxes = (value <= 0 ? 0 : (int) Math.ceil(totalBoxes / maxValue * value));
                int currentWidth = Math.min(totalBoxes, (int) Math.ceil(Math.sqrt(numberOfBoxes + 1)));
                Rectangle2D rect = new Rectangle2D.Double();

                {
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{series, className});

                    int row = 0;
                    int col = 0;
                    for (int i = 1; i <= numberOfBoxes; i++) {
                        rect.setRect(bottomLeft.x + col * drawWidth, bottomLeft.y - row * drawWidth, drawWidth, drawWidth);
                        if (i == numberOfBoxes)  // scale the last box to show how full it is:
                        {
                            double coveredValue = (numberOfBoxes - 1) * boxValue;
                            double diff = value - coveredValue;
                            double factor = diff / boxValue;
                            double height = rect.getHeight() * factor;
                            double y = rect.getY() + (rect.getHeight() - height);
                            rect.setRect(rect.getX(), y, rect.getWidth(), height);
                        }

                        gc.fill(rect);
                        if ((i % currentWidth) == 0) {
                            col = 0;
                            row++;
                        } else
                            col++;
                    }
                    if (sgc != null)
                        sgc.clearCurrentItem();
                }
                gc.setColor(color.darker());
                {
                    int row = 0;
                    int col = 0;
                    for (int i = 1; i <= numberOfBoxes; i++) {
                        rect.setRect(bottomLeft.x + col * drawWidth, bottomLeft.y - row * drawWidth, drawWidth, drawWidth);
                        gc.draw(rect);
                        if ((i % currentWidth) == 0) {
                            col = 0;
                            row++;
                        } else
                            col++;
                    }
                }

                boolean isSelected = getChartData().getChartSelection().isSelected(series, className);

                if (isSelected) {
                    gc.setStroke(HEAVY_STROKE);
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    int row = 0;
                    int col = 0;
                    for (int i = 1; i <= numberOfBoxes; i++) {
                        rect.setRect(bottomLeft.x + col * drawWidth, bottomLeft.y - row * drawWidth, drawWidth, drawWidth);
                        gc.draw(rect);
                        if ((i % currentWidth) == 0) {
                            col = 0;
                            row++;
                        } else
                            col++;
                    }
                    gc.setStroke(NORMAL_STROKE);
                }
                d++;
                if (showValues || isSelected) {
                    String label = "" + (int) getChartData().getValueAsDouble(series, className);
                    valuesList.add(new DrawableValue(label, bottomLeft.x + maxRadius + 2, bottomLeft.y - maxRadius / 2, isSelected));
                }
            }
            c++;
        }
        if (valuesList.size() > 0) {
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            DrawableValue.drawValues(gc, valuesList, false, true);
            valuesList.clear();
        }
        drawScale(gc, drawWidth, boxValue);
    }

    /**
     * draw scale
     *
     * @param gc
     * @param drawWidth
     * @param boxValue
     */
    private void drawScale(Graphics2D gc, double drawWidth, int boxValue) {
        int x = 20;
        int y = topMargin - 30;
        Rectangle rect = new Rectangle(x, y, (int) drawWidth, (int) drawWidth);
        gc.setColor(Color.LIGHT_GRAY);
        gc.fill(rect);
        gc.setColor(Color.DARK_GRAY);
        gc.draw(rect);
        gc.setFont(getFont(ChartViewer.FontKeys.LegendFont.toString()));
        gc.drawString(String.format(" = %,d", boxValue), (int) (x + rect.getWidth()), (int) (y + rect.getHeight()));
    }

    @Override
    public String getChartDrawerName() {
        return NAME;
    }
}
