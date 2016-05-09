/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import jloda.gui.ColorTable;
import jloda.util.Basic;
import jloda.util.Geometry;
import jloda.util.ProgramProperties;
import megan.chart.IChartDrawer;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * draws a heatmap chart
 * Daniel Huson, 6.2012
 */
public class HeatMapDrawer extends BarChartDrawer implements IChartDrawer {
    public static final String NAME = "HeatMap";

    private int maxRadius = 30;
    private boolean useRed = false;

    private ColorTable colorTable;

    /**
     * constructor
     */
    public HeatMapDrawer() {
    }

    /**
     * draw heat map with colors representing classes
     *
     * @param gc
     */
    public void drawChart(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));

        colorTable = getChartColors().getHeatMapTable();

        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        if (x0 >= x1)
            return;

        int numberOfDataSets = getChartData().getNumberOfSeries();
        int numberOfClasses = getChartData().getNumberOfClasses();

        double xStep = (x1 - x0) / (double) numberOfDataSets;
        double yStep = (y0 - y1) / (double) (numberOfClasses);

        double maxValue = getChartData().getRange().get2().doubleValue();
        double inverseMaxValueLog = 0;
        if (scalingType == ChartViewer.ScalingType.LOG && maxValue > 0) {
            maxValue = Math.log(maxValue);
            if (maxValue != 0)
                inverseMaxValueLog = 1 / maxValue;
        } else if (scalingType == ChartViewer.ScalingType.SQRT && maxValue > 0) {
            maxValue = Math.sqrt(maxValue);
        } else if (scalingType == ChartViewer.ScalingType.PERCENT) {
            maxValue = 100;
        }

        // main drawing loop:
        {
            int d = 0;
            for (String series : getChartData().getSeriesNames()) {
                double xLabel = x0 + (d + 0.5) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 10);
                String label = seriesLabelGetter.getLabel(series);
                Dimension labelSize = Basic.getStringSize(gc, label, gc.getFont()).getSize();
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
                drawString(gc, label, apt.getX(), apt.getY(), classLabelAngle);
                if (sgc != null) {
                    sgc.setCurrentItem(new String[]{label, null});
                    drawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, classLabelAngle);
                    sgc.clearCurrentItem();
                }
                int c = 0;
                for (String className : getChartData().getClassNames()) {
                    Color color;
                    if (scalingType == ChartViewer.ScalingType.PERCENT) {
                        double total = getChartData().getTotalForSeriesIncludingDisabledAttributes(series);
                        double value;
                        if (total == 0)
                            value = 0;
                        else
                            value = 100 * getChartData().getValueAsDouble(series, className) / total;
                        if (useRed)
                            color = Black2RedGradient.getColor((int) value, (int) maxValue);
                        else
                            color = colorTable.getColor((int) value, (int) maxValue);
                    } else if (scalingType == ChartViewer.ScalingType.LOG) {
                        double value = getChartData().getValueAsDouble(series, className);
                        if (useRed)
                            color = Black2RedGradient.getColorLogScale((int) value, maxValue, inverseMaxValueLog);
                        else
                            color = colorTable.getColorLogScale((int) value, inverseMaxValueLog);
                    } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                        double value = Math.sqrt(getChartData().getValueAsDouble(series, className));
                        if (useRed)
                            color = Black2RedGradient.getColor((int) value, (int) maxValue);
                        else
                            color = colorTable.getColor((int) value, (int) maxValue);
                    } else {
                        double value = getChartData().getValueAsDouble(series, className);
                        if (useRed)
                            color = Black2RedGradient.getColor((int) value, (int) maxValue);
                        else
                            color = colorTable.getColor((int) value, (int) maxValue);
                    }
                    gc.setColor(color);

                    int[] rect = new int[]{(int) Math.round(x0 + d * xStep), (int) Math.round(y0 - (c + 1) * yStep),
                            (int) Math.round(xStep), (int) Math.round(yStep)};
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{series, className});
                    gc.fillRect(rect[0], rect[1], rect[2] + 1, rect[3] + 1);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                    boolean isSelected = getChartData().getChartSelection().isSelected(series, className);
                    if (isSelected) {
                        gc.setStroke(HEAVY_STROKE);
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        gc.drawRect(rect[0], rect[1], rect[2], rect[3]);
                        gc.setStroke(NORMAL_STROKE);
                    }
                    if (showValues || isSelected) {
                        String aLabel = "" + (int) getChartData().getValueAsDouble(series, className);
                        valuesList.add(new DrawableValue(aLabel, rect[0] + rect[2] / 2, rect[1] + rect[3] / 2, isSelected));
                    }
                    c++;
                }
                d++;
            }
        }
        gc.setColor(Color.WHITE);

        if (isGapBetweenBars()) {

            gc.drawLine(x0, y0, x1, y0);
            for (int c = 1; c < numberOfClasses; c++) {
                int y = (int) Math.round(y0 - c * yStep);
                gc.drawLine(x0, y, x1, y);
            }
            gc.drawLine(x0, y1, x1, y1);

            gc.drawLine(x0, y0, x0, y1);
            for (int d = 1; d < numberOfDataSets; d++) {
                int x = (int) Math.round(x0 + d * xStep);
                gc.drawLine(x, y0, x, y1);
            }
            gc.drawLine(x1, y0, x1, y1);
        }

        if (valuesList.size() > 0) {
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            DrawableValue.drawValues(gc, valuesList, true, true);
            valuesList.clear();
        }
    }

    /**
     * draw heat map with colors representing series
     *
     * @param gc
     */
    public void drawChartTransposed(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));

        colorTable = getChartColors().getHeatMapTable();

        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        if (x0 >= x1)
            return;

        int numberOfSeries = getChartData().getNumberOfSeries();
        int numberOfClasses = getChartData().getNumberOfClasses();

        double xStep = (x1 - x0) / (double) numberOfClasses;
        double yStep = (y0 - y1) / (double) numberOfSeries;

        double maxValue = getChartData().getRange().get2().doubleValue();
        double inverseMaxValueLog = 0;
        if (scalingType == ChartViewer.ScalingType.LOG && maxValue > 0) {
            maxValue = Math.log(maxValue);
            if (maxValue != 0)
                inverseMaxValueLog = 1 / maxValue;
        } else if (scalingType == ChartViewer.ScalingType.SQRT && maxValue > 0) {
            maxValue = Math.sqrt(maxValue);
        } else if (scalingType == ChartViewer.ScalingType.PERCENT)
            maxValue = 100;

        // main drawing loop:
        {
            int d = 0;
            for (String className : getChartData().getClassNames()) {
                double xLabel = x0 + (d + 0.5) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 10);
                Dimension labelSize = Basic.getStringSize(gc, className, gc.getFont()).getSize();
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

                drawString(gc, className, apt.getX(), apt.getY(), classLabelAngle);
                if (sgc != null) {
                    sgc.setCurrentItem(new String[]{null, className});
                    drawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, classLabelAngle);
                    sgc.clearCurrentItem();
                }

                int c = 0;
                for (String series : getChartData().getSeriesNames()) {
                    Color color;
                    if (scalingType == ChartViewer.ScalingType.PERCENT) {
                        double total = getChartData().getTotalForClassIncludingDisabledSeries(className);
                        double value;
                        if (total == 0)
                            value = 0;
                        else
                            value = 100 * getChartData().getValueAsDouble(series, className) / total;
                        if (useRed)
                            color = Black2RedGradient.getColor((int) value, (int) maxValue);
                        else
                            color = colorTable.getColor((int) value, (int) maxValue);
                    } else if (scalingType == ChartViewer.ScalingType.LOG) {
                        double value = getChartData().getValueAsDouble(series, className);
                        if (useRed)
                            color = Black2RedGradient.getColorLogScale((int) value, maxValue, inverseMaxValueLog);
                        else
                            color = colorTable.getColorLogScale((int) value, inverseMaxValueLog);
                    } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                        double value = Math.sqrt(getChartData().getValueAsDouble(series, className));
                        if (useRed)
                            color = Black2RedGradient.getColor((int) value, (int) maxValue);
                        else
                            color = colorTable.getColor((int) value, (int) maxValue);
                    } else {
                        double value = getChartData().getValueAsDouble(series, className);
                        if (useRed)
                            color = Black2RedGradient.getColor((int) value, (int) maxValue);
                        else
                            color = colorTable.getColor((int) value, (int) maxValue);
                    }
                    gc.setColor(color);

                    int[] rect = new int[]{(int) Math.round(x0 + d * xStep), (int) Math.round(y0 - (c + 1) * yStep),
                            (int) Math.round(xStep), (int) Math.round(yStep)};
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{series, className});
                    gc.fillRect(rect[0], rect[1], rect[2] + 1, rect[3] + 1);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                    boolean isSelected = getChartData().getChartSelection().isSelected(series, className);
                    if (isSelected) {
                        gc.setStroke(HEAVY_STROKE);
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        gc.drawRect(rect[0], rect[1], rect[2], rect[3]);
                        gc.setStroke(NORMAL_STROKE);
                    }
                    if (showValues || isSelected) {
                        String label = "" + (int) getChartData().getValueAsDouble(series, className);
                        valuesList.add(new DrawableValue(label, rect[0] - rect[2] / 2, rect[1] + rect[3] / 2, isSelected));
                    }
                    c++;
                }
                d++;
            }
        }
        gc.setColor(Color.WHITE);

        gc.drawLine(x0, y0, x1, y0);
        for (int c = 1; c < numberOfSeries; c++) {
            int y = (int) Math.round(y0 - c * yStep);
            gc.drawLine(x0, y, x1, y);
        }
        gc.drawLine(x0, y1, x1, y1);

        gc.drawLine(x0, y0, x0, y1);
        for (int d = 1; d < numberOfClasses; d++) {
            int x = (int) Math.round(x0 + d * xStep);
            gc.drawLine(x, y0, x, y1);
        }
        gc.drawLine(x1, y0, x1, y1);

        if (valuesList.size() > 0) {
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            DrawableValue.drawValues(gc, valuesList, true, true);
            valuesList.clear();
        }
    }

    public int getMaxRadius() {
        return maxRadius;
    }

    public void setMaxRadius(int maxRadius) {
        this.maxRadius = maxRadius;
    }

    /**
     * draw the x axis
     *
     * @param gc
     */
    protected void drawXAxis(Graphics2D gc) {
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));
        gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.BLACK));

        gc.setColor(Color.BLACK);
        int x = 5;
        int y = getHeight() - bottomMargin + 25;
        if (!isTranspose())
            gc.drawString(getChartData().getSeriesLabel(), x, y);
        else
            gc.drawString(getChartData().getClassesLabel(), x, y);
    }

    /**
     * draw the y-axis
     *
     * @param gc
     */
    protected void drawYAxis(Graphics2D gc, Dimension size) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);

        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));

        boolean doDraw = (size == null);
        Rectangle bbox = null;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        /*
        if (getyAxisLabel() != null) {
            gc.setColor(Color.BLACK);
            Dimension labelSize = Basic.getStringSize(gc, getyAxisLabel(), getFont()).getSize();
            int x = 10;
            int y = (y0 + y1) / 2 - labelSize.width;
            drawString(gc, getyAxisLabel(), x, y, Math.PI / 2);
            //gc.drawString(getyAxisLabel(),x,y);
        }
        */

        if (isTranspose()) {
            if (x0 >= x1)
                return;

            int longest = 0;
            for (String series : getChartData().getSeriesNames()) {
                String label = seriesLabelGetter.getLabel(series);
                longest = Math.max(longest, Basic.getStringSize(gc, label, gc.getFont()).getSize().width);
            }
            int right = Math.max(leftMargin, longest + 5);

            if (doDraw)
                gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));

            int numberOfDataSets = getChartData().getNumberOfSeries();
            double yStep = (y0 - y1) / (numberOfDataSets);
            int d = 0;
            for (String series : getChartData().getSeriesNames()) {
                String label = seriesLabelGetter.getLabel(series);
                Dimension labelSize = Basic.getStringSize(gc, label, gc.getFont()).getSize();
                int x = right - labelSize.width - 4;
                int y = (int) Math.round(y0 - (d + 0.5) * yStep);
                if (doDraw) {
                    if (getChartData().getChartSelection().isSelected(series, null)) {
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        fillAndDrawRect(gc, x, y, labelSize.width, labelSize.height, 0, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                    }
                    gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.DARK_GRAY));
                    gc.drawString(label, x, y);
                } else {
                    Rectangle rect = new Rectangle(x, y, labelSize.width, labelSize.height);
                    if (bbox == null)
                        bbox = rect;
                    else
                        bbox.add(rect);
                }
                if (sgc != null) {
                    sgc.setCurrentItem(new String[]{series, null});
                    drawRect(gc, x, y, labelSize.width, labelSize.height, 0);
                    sgc.clearCurrentItem();
                }
                d++;
            }
        } else {
            int longest = 0;
            for (String className : getChartData().getClassNames()) {
                longest = Math.max(longest, Basic.getStringSize(gc, className, gc.getFont()).getSize().width);
            }
            int right = Math.max(leftMargin, longest + 5);

            if (doDraw)
                gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));
            int numberOfClasses = getChartData().getNumberOfClasses();
            double yStep = (y0 - y1) / (double) numberOfClasses;
            int c = 0;
            for (String className : getChartData().getClassNames()) {
                Dimension labelSize = Basic.getStringSize(gc, className, gc.getFont()).getSize();
                int x = right - labelSize.width - 4;
                int y = (int) Math.round(y0 - (c + 0.5) * yStep);
                if (doDraw) {
                    if (getChartData().getChartSelection().isSelected(null, className)) {
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        fillAndDrawRect(gc, x, y, labelSize.width, labelSize.height, 0, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                    }
                    gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.DARK_GRAY));
                    gc.drawString(className, x, y);
                } else {
                    Rectangle rect = new Rectangle(x, y, labelSize.width, labelSize.height);
                    if (bbox == null)
                        bbox = rect;
                    else
                        bbox.add(rect);
                }
                c++;
                if (sgc != null) {
                    sgc.setCurrentItem(new String[]{null, className});
                    drawRect(gc, x, y, labelSize.width, labelSize.height, 0);
                    sgc.clearCurrentItem();
                }

            }
        }
        if (size != null && bbox != null) {
            size.setSize(bbox.width + 3, bbox.height);
        }
    }

    protected void drawYAxisGrid(Graphics2D gc) {
    }

    public boolean canShowLegend() {
        return false;
    }

    @Override
    public String getChartDrawerName() {
        return NAME;
    }

    @Override
    public boolean usesHeatMapColors() {
        return true;
    }
}
