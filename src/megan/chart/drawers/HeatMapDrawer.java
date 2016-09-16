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
package megan.chart.drawers;

import jloda.gui.ColorTable;
import jloda.gui.IPopupMenuModifier;
import jloda.gui.commands.CommandManager;
import jloda.util.Basic;
import jloda.util.Geometry;
import jloda.util.ProgramProperties;
import jloda.util.Statistics;
import megan.chart.IChartDrawer;
import megan.chart.cluster.ClusteringTree;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;
import megan.util.Table;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * draws a heatmap chart
 * Daniel Huson, 6.2012
 */
public class HeatMapDrawer extends BarChartDrawer implements IChartDrawer {
    public static final String NAME = "HeatMap";

    private ColorTable colorTable;

    protected float[][] dataMatrix = null;

    protected Table<String, String, Double> zScores = null;
    private double zScoreCutoff = 3;

    protected boolean inUpdateCoordinates = true;
    private final ArrayList<String> previousSamples = new ArrayList<>();
    private final ArrayList<String> previousClasses = new ArrayList<>();
    private Future future; // used in recompute

    private final ClusteringTree seriesClusteringTree;
    private final ClusteringTree classesClusteringTree;

    private final int treeSpace = 100;

    private boolean previousTranspose;

    /**
     * constructor
     */
    public HeatMapDrawer() {
        getSupportedScalingTypes().add(ChartViewer.ScalingType.ZSCORE);
        seriesClusteringTree = new ClusteringTree(ClusteringTree.TYPE.SERIES, ClusteringTree.SIDE.TOP);
        classesClusteringTree = new ClusteringTree(ClusteringTree.TYPE.CLASSES, ClusteringTree.SIDE.RIGHT);
        previousTranspose = isTranspose();
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
        int scaleWidth = 30;
        int x1 = getWidth() - rightMargin - scaleWidth;
        if (x0 >= x1)
            return;

        if (inUpdateCoordinates) {
            gc.setFont(getFont("Default"));
            gc.setColor(Color.LIGHT_GRAY);
            gc.drawString("Computing z-scores...", x0, y1 + 20);
            viewer.getScrollPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            return;
        } else {
            viewer.getScrollPane().setCursor(Cursor.getDefaultCursor());
        }


        int numberOfSeries = getChartData().getNumberOfSeries();
        int numberOfClasses = getChartData().getNumberOfClasses();

        final Collection<String> seriesOrder;
        final Collection<String> classesOrder;
        if (scalingType == ChartViewer.ScalingType.ZSCORE && isDoClustering()) {
            x1 -= treeSpace;
            if (sgc == null)
                drawScaleBar(gc, x1, scaleWidth, y1, y0 - y1);
            y1 += treeSpace;

            {
                int width = (int) ((x1 - x0) / (numberOfSeries + 1.0) * numberOfSeries);
                int xStart = x0 + ((x1 - x0) - width) / 2;
                final Rectangle rect = new Rectangle(xStart, y1 - treeSpace, width, treeSpace);
                seriesClusteringTree.paint(gc, rect);
                seriesOrder = seriesClusteringTree.getLabelOrder();
            }
            {
                int height = (int) Math.round((y0 - y1) / (numberOfClasses + 1.0) * numberOfClasses);
                int yStart = y0 + ((y1 - y0) - height) / 2;
                final Rectangle rect = new Rectangle(x1, yStart, treeSpace, height);
                classesClusteringTree.paint(gc, rect);
                classesOrder = classesClusteringTree.getLabelOrder();
            }
        } else {
            seriesOrder = getChartData().getSeriesNames();
            classesOrder = getChartData().getClassNames();
            if (sgc == null)
                drawScaleBar(gc, x1, scaleWidth, y1, y0 - y1);
        }

        double xStep = (x1 - x0) / (double) numberOfSeries;
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

            for (String series : seriesOrder) {
                double xLabel = x0 + (d + 0.5) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 10);
                String label = seriesLabelGetter.getLabel(series);
                Dimension labelSize = Basic.getStringSize(gc, label, gc.getFont()).getSize();
                if (classLabelAngle == 0) {
                    apt.setLocation(apt.getX() - labelSize.getWidth() / 2, apt.getY());
                } else if (classLabelAngle > Math.PI / 2) {
                    apt = Geometry.translateByAngle(apt, classLabelAngle, -labelSize.width);
                }
                if (getChartData().getChartSelection().isSelectedSeries(series)) {
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
                for (String className : classesOrder) {
                    Color color;
                    if (scalingType == ChartViewer.ScalingType.PERCENT) {
                        double total = getChartData().getTotalForSeriesIncludingDisabledAttributes(series);
                        double value;
                        if (total == 0)
                            value = 0;
                        else
                            value = 100 * getChartData().getValueAsDouble(series, className) / total;
                        color = colorTable.getColor((int) (1000 * value), (int) (1000 * maxValue));
                    } else if (scalingType == ChartViewer.ScalingType.LOG) {
                        double value = getChartData().getValueAsDouble(series, className);
                        color = colorTable.getColorLogScale((int) value, inverseMaxValueLog);
                    } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                        double value = Math.sqrt(getChartData().getValueAsDouble(series, className));
                        color = colorTable.getColor((int) value, (int) maxValue);
                    } else if (scalingType == ChartViewer.ScalingType.ZSCORE) {
                        double value = Math.max(-zScoreCutoff, Math.min(zScoreCutoff, zScores.get(series, className)));
                        color = colorTable.getColor((int) (value + zScoreCutoff), (int) (2 * zScoreCutoff));
                    } else {
                        double value = getChartData().getValueAsDouble(series, className);
                        color = colorTable.getColor((int) value, (int) maxValue);
                    }
                    gc.setColor(color);

                    int[] rect = new int[]{(int) Math.round(x0 + d * xStep), (int) Math.round(y0 - (c + 1) * yStep),
                            (int) Math.round(xStep), (int) Math.round(yStep)};
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{series, className});
                    if (isGapBetweenBars() && rect[2] > 2 && rect[3] > 2) {
                        gc.fillRect(rect[0] + 1, rect[1] + 1, rect[2] - 2, rect[3] - 2);
                    } else
                        gc.fillRect(rect[0], rect[1], rect[2] + 1, rect[3] + 1);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                    boolean isSelected = getChartData().getChartSelection().isSelectedSeries(series)
                            || getChartData().getChartSelection().isSelectedClass(className);
                    if (isSelected) {
                        gc.setStroke(HEAVY_STROKE);
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        gc.drawRect(rect[0], rect[1], rect[2], rect[3]);
                        gc.setStroke(NORMAL_STROKE);
                    }
                    if (showValues || isSelected) {
                        String aLabel;
                        if (scalingType == ChartViewer.ScalingType.ZSCORE)
                            aLabel = String.format("%.2f", zScores.get(series, className));
                        else
                            aLabel = "" + (int) getChartData().getValueAsDouble(series, className);
                        valuesList.add(new DrawableValue(aLabel, rect[0] + rect[2] / 2, rect[1] + rect[3] / 2, isSelected));
                    }
                    c++;
                }
                d++;
            }
        }

        if (valuesList.size() > 0) {
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            DrawableValue.drawValues(gc, valuesList, true, true);
            valuesList.clear();
        }
        gc.setColor(Color.WHITE);
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
        int scaleWidth = 30;
        int x1 = getWidth() - rightMargin - scaleWidth;
        if (x0 >= x1)
            return;

        if (inUpdateCoordinates) {
            gc.setFont(getFont("Default"));
            gc.setColor(Color.LIGHT_GRAY);
            gc.drawString("Computing z-scores...", x0, y1 + 20);
            viewer.getScrollPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            return;
        } else
            viewer.getScrollPane().setCursor(Cursor.getDefaultCursor());

        int numberOfSeries = getChartData().getNumberOfSeries();
        int numberOfClasses = getChartData().getNumberOfClasses();

        final Collection<String> seriesOrder;
        final Collection<String> classesOrder;

        if (scalingType == ChartViewer.ScalingType.ZSCORE && isDoClustering()) {
            x1 -= treeSpace;
            if (sgc == null)
                drawScaleBar(gc, x1, scaleWidth, y1, y0 - y1);
            y1 += treeSpace;

            {
                int width = (int) ((x1 - x0) / (numberOfClasses + 1.0) * numberOfClasses);
                int xStart = x0 + ((x1 - x0) - width) / 2;
                final Rectangle rect = new Rectangle(xStart, y1 - treeSpace, width, treeSpace);
                classesClusteringTree.paint(gc, rect);
                classesOrder = classesClusteringTree.getLabelOrder();
            }
            {
                int height = (int) Math.round((y0 - y1) / (numberOfSeries + 1.0) * numberOfSeries);
                int yStart = y0 + ((y1 - y0) - height) / 2;
                final Rectangle rect = new Rectangle(x1, yStart, treeSpace, height);
                seriesClusteringTree.paint(gc, rect);
                seriesOrder = seriesClusteringTree.getLabelOrder();
            }
        } else {
            seriesOrder = getChartData().getSeriesNames();
            classesOrder = getChartData().getClassNames();
            if (sgc == null)
                drawScaleBar(gc, x1, scaleWidth, y1, y0 - y1);
        }

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
            for (String className : classesOrder) {
                double xLabel = x0 + (d + 0.5) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 10);
                Dimension labelSize = Basic.getStringSize(gc, className, gc.getFont()).getSize();
                if (classLabelAngle == 0) {
                    apt.setLocation(apt.getX() - labelSize.getWidth() / 2, apt.getY());
                } else if (classLabelAngle > Math.PI / 2) {
                    apt = Geometry.translateByAngle(apt, classLabelAngle, -labelSize.width);
                }
                if (getChartData().getChartSelection().isSelectedClass(className)) {
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
                for (String series : seriesOrder) {
                    Color color;
                    if (scalingType == ChartViewer.ScalingType.PERCENT) {
                        double total = getChartData().getTotalForClassIncludingDisabledSeries(className);
                        double value;
                        if (total == 0)
                            value = 0;
                        else
                            value = 100 * getChartData().getValueAsDouble(series, className) / total;
                        color = colorTable.getColor((int) value, (int) maxValue);
                    } else if (scalingType == ChartViewer.ScalingType.LOG) {
                        double value = getChartData().getValueAsDouble(series, className);
                        color = colorTable.getColorLogScale((int) value, inverseMaxValueLog);
                    } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                        double value = Math.sqrt(getChartData().getValueAsDouble(series, className));
                        color = colorTable.getColor((int) value, (int) maxValue);
                    } else if (scalingType == ChartViewer.ScalingType.ZSCORE) {
                        double value = Math.max(-zScoreCutoff, Math.min(zScoreCutoff, zScores.get(series, className)));
                        color = colorTable.getColor((int) (value + zScoreCutoff), (int) (2 * zScoreCutoff));
                    } else {
                        double value = getChartData().getValueAsDouble(series, className);
                        color = colorTable.getColor((int) value, (int) maxValue);
                    }
                    gc.setColor(color);

                    int[] rect = new int[]{(int) Math.round(x0 + d * xStep), (int) Math.round(y0 - (c + 1) * yStep),
                            (int) Math.round(xStep), (int) Math.round(yStep)};
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{series, className});
                    if (isGapBetweenBars() && rect[2] > 2 && rect[3] > 2) {
                        gc.fillRect(rect[0] + 1, rect[1] + 1, rect[2] - 2, rect[3] - 2);
                    } else
                        gc.fillRect(rect[0], rect[1], rect[2] + 1, rect[3] + 1);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                    boolean isSelected = getChartData().getChartSelection().isSelectedSeries(series)
                            || getChartData().getChartSelection().isSelectedClass(className);
                    if (isSelected) {
                        gc.setStroke(HEAVY_STROKE);
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        gc.drawRect(rect[0], rect[1], rect[2], rect[3]);
                        gc.setStroke(NORMAL_STROKE);
                    }
                    if (showValues || isSelected) {
                        String aLabel;
                        if (scalingType == ChartViewer.ScalingType.ZSCORE)
                            aLabel = String.format("%.2f", zScores.get(series, className));
                        else
                            aLabel = "" + (int) getChartData().getValueAsDouble(series, className);
                        valuesList.add(new DrawableValue(aLabel, rect[0] - rect[2] / 2, rect[1] + rect[3] / 2, isSelected));
                    }
                    c++;
                }
                d++;
            }
        }

        if (valuesList.size() > 0) {
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            DrawableValue.drawValues(gc, valuesList, true, true);
            valuesList.clear();
        }
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

        final Collection<String> seriesOrder;
        final Collection<String> classesOrder;

        if (scalingType == ChartViewer.ScalingType.ZSCORE && isDoClustering()) {
            y1 += treeSpace;
            seriesOrder = seriesClusteringTree.getLabelOrder();
            classesOrder = classesClusteringTree.getLabelOrder();
        } else {
            seriesOrder = getChartData().getSeriesNames();
            classesOrder = getChartData().getClassNames();
        }
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
            for (String series : seriesOrder) {
                String label = seriesLabelGetter.getLabel(series);
                longest = Math.max(longest, Basic.getStringSize(gc, label, gc.getFont()).getSize().width);
            }
            int right = Math.max(leftMargin, longest + 5);

            if (doDraw)
                gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));

            int numberOfDataSets = getChartData().getNumberOfSeries();
            double yStep = (y0 - y1) / (numberOfDataSets);
            int d = 0;
            for (String series : seriesOrder) {
                String label = seriesLabelGetter.getLabel(series);
                Dimension labelSize = Basic.getStringSize(gc, label, gc.getFont()).getSize();
                int x = right - labelSize.width - 4;
                int y = (int) Math.round(y0 - (d + 0.5) * yStep);
                if (doDraw) {
                    if (getChartData().getChartSelection().isSelectedSeries(series)) {
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
            for (String className : classesOrder) {
                longest = Math.max(longest, Basic.getStringSize(gc, className, gc.getFont()).getSize().width);
            }
            int right = Math.max(leftMargin, longest + 5);

            if (doDraw)
                gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));
            int numberOfClasses = getChartData().getNumberOfClasses();
            double yStep = (y0 - y1) / (double) numberOfClasses;
            int c = 0;
            for (String className : classesOrder) {
                Dimension labelSize = Basic.getStringSize(gc, className, gc.getFont()).getSize();
                int x = right - labelSize.width - 4;
                int y = (int) Math.round(y0 - (c + 0.5) * yStep);
                if (doDraw) {
                    if (getChartData().getChartSelection().isSelectedClass(className)) {
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

    /**
     * draw scale bar
     *
     * @param gc
     * @param x
     * @param width
     * @param y
     * @param height
     */
    protected void drawScaleBar(Graphics2D gc, final int x, final int width, final int y, final int height) {
        final int x0 = x + Math.max(10, width - 25);

        int xLabel = x0 + 15;
        int boxWidth = 10;
        int boxHeight = Math.min(getScalingType() == ChartViewer.ScalingType.ZSCORE && isDoClustering() ? treeSpace - 12 : 150, height - 15);

        int y0 = y;


        for (int i = 0; i <= boxHeight; i++) {
            float p = 1f - (float) i / (float) boxHeight; // is between 1 and 0
            final Color color = getChartColors().getHeatMapTable().getColor(Math.round(1000 * p), 1000);
            gc.setColor(color);
            gc.drawLine(x0, y0 + i, x0 + boxWidth, y0 + i);
        }
        gc.setColor(Color.BLACK);

        gc.drawRect(x0, y0, boxWidth, boxHeight);

        final double max;
        final double min;
        final double step;
        final double yStep;
        final String format;

        switch (getScalingType()) {
            case ZSCORE:
                gc.drawString("z-score", x0, y - 5);
                max = zScoreCutoff;
                min = -zScoreCutoff;
                step = 1;
                yStep = boxHeight / (2.0 * zScoreCutoff);
                format = "%+1.1f";
                break;
            case PERCENT:
                gc.drawString("%", x0, y - 5);
                max = 100;
                min = 0;
                step = 20;
                yStep = boxHeight / 5;
                format = "%.0f";
                break;
            case LINEAR: {
                double maxValue = 0;
                for (String series : getChartData().getSeriesNames()) {
                    maxValue = Math.max(maxValue, getChartData().getRange(series).getSecond().intValue());
                }
                gc.drawString("Count", x0, y - 5);
                int tens = 1;
                int factor = 1;
                while (factor * tens < maxValue) {
                    if (factor < 9)
                        factor++;
                    else {
                        tens *= 10;
                        factor = 1;
                    }
                }
                max = factor * tens;
                min = 0;
                if (factor >= 4) {
                    step = tens;
                    yStep = boxHeight / factor;
                } else {
                    step = tens / 2;
                    yStep = boxHeight / (2 * factor);
                }
                format = "%,.0f";
                break;
            }
            case LOG: {
                double maxValue = 0;
                for (String series : getChartData().getSeriesNames()) {
                    maxValue = Math.max(maxValue, getChartData().getRange(series).getSecond().intValue());
                }
                gc.drawString("Count", x0, y - 5);
                int tens = 1;
                int factor = 1;
                while (factor * tens < maxValue) {
                    if (factor < 9)
                        factor++;
                    else {
                        tens *= 10;
                        factor = 1;
                    }
                }
                max = factor * tens;
                min = 1;
                format = "%,.0f";

                double q = boxHeight / Math.log10(factor * tens);

                gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));
                boolean first = true;
                for (double p = max; p >= min; p /= 10) {
                    double yy = y0 + boxHeight - q * Math.log10(p);
                    gc.drawString(String.format(format, p), xLabel, Math.round(yy + gc.getFont().getSize() / 2));
                    if (first) {
                        p /= factor;
                        first = false;
                    }
                }
                return;
            }
            case SQRT: {
                double maxValue = 0;
                for (String series : getChartData().getSeriesNames()) {
                    maxValue = Math.max(maxValue, getChartData().getRange(series).getSecond().intValue());
                }
                gc.drawString("Count", x0, y - 5);
                int tens = 1;
                int factor = 1;
                while (factor * tens < maxValue) {
                    if (factor < 9)
                        factor++;
                    else {
                        tens *= 10;
                        factor = 1;
                    }
                }
                max = factor * tens;
                min = 0;
                if (factor >= 4) {
                    step = tens;
                } else {
                    step = tens / 2;
                }
                format = "%,.0f";

                double q = boxHeight / Math.sqrt(factor * tens);

                gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));
                for (double p = min; p <= max; p += step) {
                    double yy = y0 + boxHeight - q * Math.sqrt(p);
                    gc.drawString(String.format(format, p), xLabel, Math.round(yy + gc.getFont().getSize() / 2));
                }
                return;
            }
            default: {
                double maxValue = 0;
                for (String series : getChartData().getSeriesNames()) {
                    maxValue = Math.max(maxValue, getChartData().getRange(series).getSecond().intValue());
                }
                gc.drawString("Count", x0, y - 5);
                int tens = 1;
                int factor = 1;
                while (factor * tens < maxValue) {
                    if (factor < 9)
                        factor++;
                    else {
                        tens *= 10;
                        factor = 1;
                    }
                }
                max = factor * tens;
                min = 0;
                step = max;
                yStep = boxHeight;
                format = "%,.0f";
                break;
            }
        }
        gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));
        for (double p = max; p >= min; p -= step) {
            gc.drawString(String.format(format, p), xLabel, y0 + gc.getFont().getSize() / 2);
            y0 += yStep;
        }
    }

    /**
     * do we need to recompute coordinates?
     *
     * @return true, if coordinates need to be recomputed
     */
    private boolean mustUpdateCoordinates() {
        boolean mustUpdate = (zScores == null);

        if (previousTranspose != isTranspose()) {
            mustUpdate = true;
            previousTranspose = isTranspose();
        }

        if (!mustUpdate && scalingType == ChartViewer.ScalingType.ZSCORE && getChartData().getNumberOfClasses() > 0 &&
                getChartData().getNumberOfSeries() > 0 && zScores.size() == 0) {
            mustUpdate = true;
        }

        final ArrayList<String> currentClasses = new ArrayList<>();
        currentClasses.addAll(getChartData().getClassNames());
        if (!previousClasses.equals(currentClasses)) {
            mustUpdate = true;
            previousClasses.clear();
            previousClasses.addAll(currentClasses);
        }
        final ArrayList<String> currentSamples = new ArrayList<>();
        currentSamples.addAll(getChartData().getSeriesNames());
        if (!previousSamples.equals(currentSamples)) {
            mustUpdate = true;
            previousSamples.clear();
            previousSamples.addAll(currentSamples);
        }
        return mustUpdate;
    }

    /**
     * updates the view
     */
    public void updateView() {
        if (mustUpdateCoordinates()) {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
            inUpdateCoordinates = true;
            future = executorService.submit(new Runnable() {
                public void run() {
                    try {
                        updateCoordinates();
                        if (SwingUtilities.isEventDispatchThread()) {
                            inUpdateCoordinates = false;
                            viewer.repaint();
                            future = null;
                        } else {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                public void run() {
                                    inUpdateCoordinates = false;
                                    viewer.repaint();
                                    future = null;
                                }
                            });
                        }
                    } catch (Exception e) {
                        inUpdateCoordinates = false;
                    }
                }
            });
        }
    }

    /**
     * force update
     */
    @Override
    public void forceUpdate() {
        System.err.println("Force update");
        zScores = null;
        previousClasses.clear();
        previousSamples.clear();
        seriesClusteringTree.clear();
        classesClusteringTree.clear();
    }

    /**
     * computes z-scores on scale of -zScoreCutoff to zScoreCutoff
     */
    protected void updateCoordinates() {
        System.err.println("Updating...");
        zScores = new Table<>();

        if (isTranspose()) {
            classesClusteringTree.setRootSide(ClusteringTree.SIDE.TOP);
            seriesClusteringTree.setRootSide(ClusteringTree.SIDE.RIGHT);
        } else {
            classesClusteringTree.setRootSide(ClusteringTree.SIDE.RIGHT);
            seriesClusteringTree.setRootSide(ClusteringTree.SIDE.TOP);

        }

        if (seriesClusteringTree.getChartSelection() == null)
            seriesClusteringTree.setChartSelection(viewer.getChartSelection());
        if (classesClusteringTree.getChartSelection() == null)
            classesClusteringTree.setChartSelection(viewer.getChartSelection());

        for (String className : getChartData().getClassNames()) {

            final Map<String, Double> series2value = new HashMap<>();
            for (String series : getChartData().getSeriesNames()) {
                final double total = getChartData().getTotalForSeries(series);
                series2value.put(series, (total > 0 ? (getChartData().getValueAsDouble(series, className) / total) : 0));
            }
            final Statistics statistics = new Statistics(series2value.values());
            for (String series : getChartData().getSeriesNames()) {
                final double value = series2value.get(series);
                zScores.put(series, className, statistics.getZScore(value));
            }
        }

        seriesClusteringTree.updateClustering(zScores);
        classesClusteringTree.updateClustering(zScores);
        // todo: check whether we always need to call this
        updateSeriesJList();
        updateClassesJList();
        //System.err.println("Order: " + Basic.toString(seriesClustering.getLabelOrder(), ","));
        //System.err.println("Order: " + Basic.toString(classesClustering.getLabelOrder(), ","));
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

    public ChartViewer.ScalingType getScalingTypePreference() {
        return ChartViewer.ScalingType.ZSCORE;
    }

    public IPopupMenuModifier getPopupMenuModifier() {
        return new IPopupMenuModifier() {
            @Override
            public void apply(JPopupMenu menu, final CommandManager commandManager) {
                menu.addSeparator();

                final AbstractAction action = (new AbstractAction("Flip Selected Subtree") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (classesClusteringTree.hasSelectedSubTree()) {
                            //System.err.println("Rotate Classes");
                            classesClusteringTree.rotateSelectedSubTree();
                            updateClassesJList();
                            getJPanel().repaint();
                        } else if (seriesClusteringTree.hasSelectedSubTree()) {
                            // System.err.println("Old order: "+Basic.toString(seriesClusteringTree.getLabelOrder(),","));
                            //System.err.println("Rotate Series");
                            seriesClusteringTree.rotateSelectedSubTree();
                            //System.err.println("New order: "+Basic.toString(seriesClusteringTree.getLabelOrder(),","));
                            updateSeriesJList();
                            getJPanel().repaint();
                        }
                    }
                });
                action.setEnabled(classesClusteringTree.hasSelectedSubTree() != seriesClusteringTree.hasSelectedSubTree());
                menu.add(action);
            }
        };
    }

    private void updateSeriesJList() {
        final Collection<String> selected = new ArrayList<>();
        selected.addAll(viewer.getChartSelection().getSelectedSeries());
        final Collection<String> ordered = seriesClusteringTree.getLabelOrder();
        final Collection<String> others = viewer.getSeriesList().getAllLabels();
        others.removeAll(ordered);
        ordered.addAll(others);
        viewer.getSeriesList().sync(ordered, viewer.getSeriesList().getLabel2ToolTips(), true);
        viewer.getChartSelection().setSelectedSeries(selected, true);
        viewer.getSeriesList().setDisabledLabels(others);
    }

    private void updateClassesJList() {
        final Collection<String> selected = new ArrayList<>();
        selected.addAll(viewer.getChartSelection().getSelectedClasses());
        final Collection<String> ordered = classesClusteringTree.getLabelOrder();
        final Collection<String> others = viewer.getClassesList().getAllLabels();
        others.removeAll(ordered);
        ordered.addAll(others);
        viewer.getClassesList().sync(ordered, viewer.getClassesList().getLabel2ToolTips(), true);
        viewer.getChartSelection().setSelectedClass(selected, true);
        viewer.getClassesList().setDisabledLabels(others);

    }

    @Override
    public boolean canCluster(ClusteringTree.TYPE type) {
        return scalingType == ChartViewer.ScalingType.ZSCORE && (type == null || type == ClusteringTree.TYPE.SERIES || type == ClusteringTree.TYPE.CLASSES);
    }

    public boolean isDoClustering() {
        return scalingType == ChartViewer.ScalingType.ZSCORE && super.isDoClustering();
    }
}
