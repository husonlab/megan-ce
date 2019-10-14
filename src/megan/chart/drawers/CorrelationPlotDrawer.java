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

import jloda.swing.commands.CommandManager;
import jloda.swing.util.BasicSwing;
import jloda.swing.util.Geometry;
import jloda.swing.window.IPopupMenuModifier;
import jloda.util.Correlation;
import jloda.util.ProgramProperties;
import jloda.util.Table;
import megan.chart.IChartDrawer;
import megan.chart.cluster.ClusteringTree;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;
import megan.util.CallBack;
import megan.util.PopupChoice;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * draws a correlation plot
 * Daniel Huson, 11.2015
 */
public class CorrelationPlotDrawer extends BarChartDrawer implements IChartDrawer {
    public enum MODE {
        Beans, Circles, Squares, Numbers, Colors;

        static MODE valueOfIgnoreCase(String label) {
            for (MODE mode : MODE.values()) {
                if (mode.toString().equalsIgnoreCase(label))
                    return mode;
            }
            return null;
        }
    }

    private static final String NAME = "CorrelationPlot";

    final Table<String, String, Float> dataMatrix = new Table<>();

    String[] classNames = null;
    private String[] seriesNames = null;

    final ArrayList<String> previousSamples = new ArrayList<>();
    final ArrayList<String> previousClasses = new ArrayList<>();

    private final ClusteringTree topClusteringTree;
    private final ClusteringTree rightClusteringTree;

    private boolean previousClusterSeries = false;
    boolean previousClusterClasses = false;
    boolean previousTranspose;
    Future future; // used in recompute

    final int topTreeSpace = ProgramProperties.get("topTreeHeight", 100);
    final int rightTreeSpace = ProgramProperties.get("rightTreeWidth", 100);

    private MODE mode;

    boolean inUpdateCoordinates = true;

    /**
     * constructor
     */
    public CorrelationPlotDrawer() {
        setSupportedScalingTypes(ChartViewer.ScalingType.LINEAR);
        mode = MODE.valueOfIgnoreCase(ProgramProperties.get("CorrelationPlotMode", MODE.Beans.toString()));
        rightClusteringTree = new ClusteringTree(ClusteringTree.TYPE.CLASSES, ClusteringTree.SIDE.RIGHT);
        topClusteringTree = new ClusteringTree(ClusteringTree.TYPE.CLASSES, ClusteringTree.SIDE.TOP);
        executorService = Executors.newSingleThreadExecutor(); // ensure we only use one thread at a time on update
    }

    /**
     * draw correlation plot chart
     *
     * @param gc
     */
    public void drawChart(Graphics2D gc) {
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics) gc : null);
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));

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
            gc.drawString("Computing correlation plot...", x0, y1 + 20);
            viewer.getScrollPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            return;
        } else
            viewer.getScrollPane().setCursor(Cursor.getDefaultCursor());

        if (sgc != null) {
            drawYAxis(gc, null);
        }

        if (!getChartTitle().startsWith("Correlation plot: "))
            setChartTitle("Correlation plot: " + getChartTitle());

        final int numberOfClasses = (classNames == null ? 0 : classNames.length);

        if (viewer.getClassesList().isDoClustering())
            y1 += topTreeSpace; // do this before other clustering

        if (sgc == null) {
            drawScaleBar(gc, x1, scaleWidth, y1, y0 - y1);
        }

        if (viewer.getClassesList().isDoClustering()) {
            x1 -= rightTreeSpace;
            int width = (int) ((x1 - x0) / (numberOfClasses + 1.0) * numberOfClasses);
            int xStart = x0 + ((x1 - x0) - width) / 2;
            final Rectangle rect = new Rectangle(xStart, y1 - topTreeSpace, width, topTreeSpace);
            topClusteringTree.paint(gc, rect);
        }

        if (viewer.getClassesList().isDoClustering()) {
            int height = (int) Math.round((y0 - y1) / (numberOfClasses + 1.0) * numberOfClasses);
            int yStart = y0 + ((y1 - y0) - height) / 2;
            final Rectangle rect = new Rectangle(x1, yStart, rightTreeSpace, height);
            rightClusteringTree.paint(gc, rect);
        }

        if (numberOfClasses > 0) {
            double xStep = (x1 - x0) / (double) numberOfClasses;
            double yStep = (y0 - y1) / (double) numberOfClasses;

            // main drawing loop:
            int d = 0;
            for (final String classNameX : classNames) {
                final double xLabel = x0 + (d + 0.5) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 10);
                final Dimension labelSize = BasicSwing.getStringSize(gc, classNameX, gc.getFont()).getSize();
                if (classLabelAngle == 0) {
                    apt.setLocation(apt.getX() - labelSize.getWidth() / 2, apt.getY());
                } else if (classLabelAngle > Math.PI / 2) {
                    apt = Geometry.translateByAngle(apt, classLabelAngle, -labelSize.width);
                }
                if (getChartData().getChartSelection().isSelected(null, classNameX)) {
                    fillAndDrawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, classLabelAngle, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                }
                gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.DARK_GRAY));
                drawString(gc, classNameX, apt.getX(), apt.getY(), classLabelAngle);
                if (sgc != null) {
                    sgc.setCurrentItem(new String[]{null, classNameX});
                    drawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, classLabelAngle);
                    sgc.clearCurrentItem();
                }

                int c = numberOfClasses - 1;
                for (final String classNameY : classNames) {
                    final Float correlationCoefficient = dataMatrix.get(classNameX, classNameY);
                    if (correlationCoefficient != null) {
                        final double[] boundingBox = new double[]{x0 + d * xStep, y0 - (c + 1) * yStep, xStep, yStep};

                        // gc.drawRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                        drawCell(gc, boundingBox, correlationCoefficient);

                        if (sgc != null && !sgc.isShiftDown()) {
                            sgc.setCurrentItem(new String[]{null, classNameX});
                            gc.fillRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                            sgc.clearCurrentItem();
                            sgc.setCurrentItem(new String[]{null, classNameY});
                            gc.fillRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                            sgc.clearCurrentItem();
                        }
                        boolean isSelected = !classNameX.equals(classNameY) && getChartData().getChartSelection().isSelected(null, classNameX) && getChartData().getChartSelection().isSelected(null, classNameY);
                        if (isSelected) {
                            gc.setStroke(HEAVY_STROKE);
                            gc.setColor(ProgramProperties.SELECTION_COLOR);
                            gc.drawRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                            gc.setStroke(NORMAL_STROKE);
                        }
                        if (showValues || isSelected) {
                            String aLabel = String.format("%.3f", correlationCoefficient);
                            valuesList.add(new DrawableValue(aLabel, (int) Math.round(boundingBox[0] + boundingBox[2] / 2), (int) Math.round(boundingBox[1] + boundingBox[3] / 2) - gc.getFont().getSize() / 2, isSelected));
                        }
                    }
                    c--;
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
     * draw correlation plot chart
     *
     * @param gc
     */
    public void drawChartTransposed(Graphics2D gc) {
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics) gc : null);
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));

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
            gc.drawString("Computing correlation plot...", x0, y1 + 20);
            viewer.getScrollPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            return;
        } else
            viewer.getScrollPane().setCursor(Cursor.getDefaultCursor());

        if (sgc != null) {
            drawYAxis(gc, null);
        }

        if (sgc == null) {
            drawScaleBar(gc, x1, scaleWidth, y1, y0 - y1);
        }

        if (!getChartTitle().startsWith("Correlation plot: "))
            setChartTitle("Correlation plot: " + getChartTitle());

        final int numberOfSeries = (seriesNames == null ? 0 : seriesNames.length);

        if (viewer.getSeriesList().isDoClustering())
            y1 += topTreeSpace; // do this before other clustering

        if (sgc == null) {
            drawScaleBar(gc, x1, scaleWidth, y1, y0 - y1);
        }

        if (viewer.getSeriesList().isDoClustering()) {
            x1 -= rightTreeSpace;
            int width = (int) ((x1 - x0) / (numberOfSeries + 1.0) * numberOfSeries);
            int xStart = x0 + ((x1 - x0) - width) / 2;
            final Rectangle rect = new Rectangle(xStart, y1 - topTreeSpace, width, topTreeSpace);
            topClusteringTree.paint(gc, rect);
        }

        if (viewer.getSeriesList().isDoClustering()) {
            int height = (int) Math.round((y0 - y1) / (numberOfSeries + 1.0) * numberOfSeries);
            int yStart = y0 + ((y1 - y0) - height) / 2;
            final Rectangle rect = new Rectangle(x1, yStart, rightTreeSpace, height);
            rightClusteringTree.paint(gc, rect);
        }

        if (numberOfSeries > 0) {
            double xStep = (x1 - x0) / (double) numberOfSeries;
            double yStep = (y0 - y1) / (double) numberOfSeries;

            // main drawing loop:
            int d = 0;
            for (final String seriesNameX : seriesNames) {
                final double xLabel = x0 + (d + 0.5) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 10);
                final Dimension labelSize = BasicSwing.getStringSize(gc, seriesNameX, gc.getFont()).getSize();
                if (classLabelAngle == 0) {
                    apt.setLocation(apt.getX() - labelSize.getWidth() / 2, apt.getY());
                } else if (classLabelAngle > Math.PI / 2) {
                    apt = Geometry.translateByAngle(apt, classLabelAngle, -labelSize.width);
                }
                if (getChartData().getChartSelection().isSelected(seriesNameX, null)) {
                    fillAndDrawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, classLabelAngle, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                }
                gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.DARK_GRAY));
                drawString(gc, seriesNameX, apt.getX(), apt.getY(), classLabelAngle);
                if (sgc != null) {
                    sgc.setCurrentItem(new String[]{seriesNameX, null});
                    drawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, classLabelAngle);
                    sgc.clearCurrentItem();
                }

                int c = numberOfSeries - 1;
                for (final String seriesNameY : seriesNames) {
                    final Float correlationCoefficient = dataMatrix.get(seriesNameX, seriesNameY);
                    if (correlationCoefficient != null) {
                        final double[] boundingBox = new double[]{x0 + d * xStep, y0 - (c + 1) * yStep, xStep, yStep};

                        // gc.drawRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                        drawCell(gc, boundingBox, correlationCoefficient);

                        if (sgc != null && !sgc.isShiftDown()) {
                            sgc.setCurrentItem(new String[]{seriesNameY, null});
                            gc.fillRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                            sgc.clearCurrentItem();
                            sgc.setCurrentItem(new String[]{seriesNameY, null});
                            gc.fillRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                            sgc.clearCurrentItem();
                        }
                        boolean isSelected = !seriesNameX.equals(seriesNameY) && getChartData().getChartSelection().isSelected(seriesNameX, null) && getChartData().getChartSelection().isSelected(seriesNameY, null);
                        if (isSelected) {
                            gc.setStroke(HEAVY_STROKE);
                            gc.setColor(ProgramProperties.SELECTION_COLOR);
                            gc.drawRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                            gc.setStroke(NORMAL_STROKE);
                        }
                        if (showValues || isSelected) {
                            String aLabel = String.format("%.3f", correlationCoefficient);
                            valuesList.add(new DrawableValue(aLabel, (int) Math.round(boundingBox[0] + boundingBox[2] / 2), (int) Math.round(boundingBox[1] + boundingBox[3] / 2) - gc.getFont().getSize() / 2, isSelected));
                        }
                    }
                    c--;
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
     * draw cell
     *
     * @param gc
     * @param boundingBox
     * @param correlationCoefficent
     */
    void drawCell(Graphics2D gc, double[] boundingBox, double correlationCoefficent) {
        double centerX = boundingBox[0] + boundingBox[2] / 2; // center x
        double centerY = boundingBox[1] + boundingBox[3] / 2; // center y

        //Color color = ColorUtilities.interpolateRGB(lowColor, highColor, (float) ((correlationCoefficent + 1.0) / 2.0));

        Color color = getChartColors().getHeatMapTable().getColor((int) Math.round(500.0 * (correlationCoefficent + 1.0)), 1000);

        switch (getMode()) {
            case Beans: {
                double width = 2 + Math.min(boundingBox[2], boundingBox[3]) * (correlationCoefficent + 1.0) / 2.0;
                double height = 2 + Math.min(boundingBox[2], boundingBox[3]) * (1.0 - correlationCoefficent) / 2.0;
                int x = (int) Math.round(centerX - width / 2.0); // left
                int y = (int) Math.round(centerY - height / 2.0); // top
                if (correlationCoefficent >= 1) { // diagonal up
                    gc.setColor(color.darker());
                    gc.rotate(Geometry.deg2rad(-45), centerX, centerY);
                    gc.drawLine(x, y, x + (int) Math.round(width), y);
                    gc.rotate(Geometry.deg2rad(45), centerX, centerY);
                } else if (correlationCoefficent <= -1) { // diagonal down
                    gc.setColor(color.darker());
                    gc.rotate(Geometry.deg2rad(45), centerX, centerY);
                    gc.drawLine(x, y, x + (int) Math.round(width), y);
                    gc.rotate(Geometry.deg2rad(-45), centerX, centerY);
                } else { // ellipse
                    gc.setColor(color);
                    gc.rotate(Geometry.deg2rad(-45), centerX, centerY);
                    gc.fillOval(x, y, (int) Math.round(width), (int) Math.round(height));
                    gc.setColor(color.darker());
                    gc.drawOval(x, y, (int) Math.round(width), (int) Math.round(height));
                    gc.rotate(Geometry.deg2rad(45), centerX, centerY);
                }
                break;
            }
            case Circles: {
                double width = Math.min(boundingBox[2], boundingBox[3]);
                double height = Math.min(boundingBox[2], boundingBox[3]);
                double radius = Math.abs(correlationCoefficent) * Math.min(width, height);
                int x = (int) Math.round(centerX - radius / 2.0); // left
                int y = (int) Math.round(centerY - radius / 2.0); // top
                gc.setColor(color);
                gc.fillOval(x, y, (int) Math.round(radius), (int) Math.round(radius));
                gc.setColor(color.darker());
                gc.drawOval(x, y, (int) Math.round(radius), (int) Math.round(radius));
                break;
            }
            case Squares: {
                double width = Math.min(boundingBox[2], boundingBox[3]) * Math.abs(correlationCoefficent);
                double height = Math.min(boundingBox[2], boundingBox[3]) * Math.abs(correlationCoefficent);
                int x = (int) Math.round(centerX - width / 2.0); // left
                int y = (int) Math.round(centerY - height / 2.0); // top
                gc.setColor(color);
                gc.fillRect(x, y, (int) Math.round(width), (int) Math.round(height));
                gc.setColor(color.darker());
                gc.drawRect(x, y, (int) Math.round(width), (int) Math.round(height));
                break;
            }
            case Colors: {
                final double width = boundingBox[2];
                final double height = boundingBox[3];
                final double x = centerX - width / 2.0; // left
                final double y = centerY - height / 2.0; // top
                gc.setColor(color);
                if (isGapBetweenBars() && width > 3 && height > 3) {
                    final Rectangle2D rect = new Rectangle2D.Double(x + 1, y + 1, width - 2, height - 2);
                    gc.fill(rect);
                } else {
                    final Rectangle2D rect = new Rectangle2D.Double(x, y, width + 1, height + 1);
                    gc.fill(rect);
                    if (isShowVerticalGridLines()) {
                        gc.setColor(color.darker());
                        gc.draw(rect);
                    }
                }
                break;
            }
            case Numbers: {
                gc.setFont(getFont(ChartViewer.FontKeys.DrawFont.toString()));
                String label = String.format("%.3f", correlationCoefficent);
                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                int x = (int) Math.round(centerX - labelSize.width / 2.0); // left
                int y = (int) Math.round(centerY); // top
                gc.setColor(color.darker());
                gc.drawString(label, x, y);
            }
        }
    }

    void drawScaleBar(Graphics2D gc, final int x, final int width, final int y, final int height) {
        int x0 = x + Math.max(10, width - 25);

        int xLabel = x0 + 15;
        int boxWidth = 10;
        int boxHeight = Math.min(150, height - 15);
        int step = boxHeight / 10;

        int y0 = y + 15;
        for (int i = 0; i <= boxHeight; i++) {
            float p = 1f - (float) i / (float) boxHeight; // is between 1 and 0
            final Color color = getChartColors().getHeatMapTable().getColor(Math.round(1000 * p), 1000);
            gc.setColor(color);
            gc.drawLine(x0, y0 + i, x0 + boxWidth, y0 + i);
        }
        gc.setColor(Color.BLACK);
        gc.drawRect(x0, y0, boxWidth, boxHeight);

        gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));
        for (float p = 1f; p >= -1f; p -= 0.2f) { // is between 1 and -1
            gc.drawString(String.format("%+1.1f", p), xLabel, y0 + gc.getFont().getSize() / 2);
            y0 += step;
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
        gc.drawString(getChartData().getClassesLabel(), x, y);
    }

    /**
     * draw the y-axis
     *
     * @param gc
     */
    protected void drawYAxis(Graphics2D gc, Dimension size) {
        if (inUpdateCoordinates)
            return;

        if (isTranspose()) {
            drawYAxisTransposed(gc, size);
            return;
        }
        final int numberOfClasses = (classNames == null ? 0 : classNames.length);
        if (numberOfClasses > 0) {
            SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
            gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));

            final boolean doDraw = (size == null);
            Rectangle bbox = null;

            int x0 = leftMargin;
            int x1 = getWidth() - rightMargin;
            int y0 = getHeight() - bottomMargin;
            int y1 = topMargin;

            if (viewer.getClassesList().isDoClustering())
                y1 += topTreeSpace;

            int longest = 0;
            for (String className : classNames) {
                longest = Math.max(longest, BasicSwing.getStringSize(gc, className, gc.getFont()).getSize().width);
            }
            int right = Math.max(leftMargin, longest + 5);

            if (doDraw)
                gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));

            double yStep = (y0 - y1) / (double) numberOfClasses;
            int c = numberOfClasses - 1;
            for (String className : classNames) {
                Dimension labelSize = BasicSwing.getStringSize(gc, className, gc.getFont()).getSize();
                int x = right - labelSize.width - 4;
                int y = (int) Math.round(y0 - (c + 0.5) * yStep);
                if (doDraw) {
                    if (getChartData().getChartSelection().isSelected(null, className)) {
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        fillAndDrawRect(gc, x, y, labelSize.width, labelSize.height, 0, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                    }
                    gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.DARK_GRAY));
                    gc.drawString(className, x, y);
                } else {
                    Rectangle rect = new Rectangle(x, y, labelSize.width, labelSize.height);
                    if (bbox == null)
                        bbox = rect;
                    else
                        bbox.add(rect);
                }
                if (sgc != null) {
                    sgc.setCurrentItem(new String[]{null, className});
                    drawRect(gc, x, y, labelSize.width, labelSize.height, 0);
                    sgc.clearCurrentItem();
                }
                c--;
            }
            if (size != null && bbox != null) {
                size.setSize(bbox.width + 3, bbox.height);
            }
        }
    }

    /**
     * draw the y-axis
     *
     * @param gc
     */
    void drawYAxisTransposed(Graphics2D gc, Dimension size) {
        final int numberOfSeries = (seriesNames == null ? 0 : seriesNames.length);
        if (numberOfSeries > 0) {
            SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics) gc : null);
            gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));

            final boolean doDraw = (size == null);
            Rectangle bbox = null;

            int x0 = leftMargin;
            int x1 = getWidth() - rightMargin;
            int y0 = getHeight() - bottomMargin;
            int y1 = topMargin;

            if (viewer.getSeriesList().isDoClustering())
                y1 += topTreeSpace;

            int longest = 0;
            for (String seriesName : seriesNames) {
                longest = Math.max(longest, BasicSwing.getStringSize(gc, seriesName, gc.getFont()).getSize().width);
            }
            int right = Math.max(leftMargin, longest + 5);

            if (doDraw)
                gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));

            double yStep = (y0 - y1) / (double) numberOfSeries;
            int c = numberOfSeries - 1;
            for (String seriesName : seriesNames) {
                Dimension labelSize = BasicSwing.getStringSize(gc, seriesName, gc.getFont()).getSize();
                int x = right - labelSize.width - 4;
                int y = (int) Math.round(y0 - (c + 0.5) * yStep);
                if (doDraw) {
                    if (getChartData().getChartSelection().isSelected(seriesName, null)) {
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        fillAndDrawRect(gc, x, y, labelSize.width, labelSize.height, 0, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                    }
                    gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.DARK_GRAY));
                    gc.drawString(seriesName, x, y);
                } else {
                    Rectangle rect = new Rectangle(x, y, labelSize.width, labelSize.height);
                    if (bbox == null)
                        bbox = rect;
                    else
                        bbox.add(rect);
                }
                if (sgc != null) {
                    sgc.setCurrentItem(new String[]{seriesName, null});
                    drawRect(gc, x, y, labelSize.width, labelSize.height, 0);
                    sgc.clearCurrentItem();
                }
                c--;
            }
            if (size != null && bbox != null) {
                size.setSize(bbox.width + 3, bbox.height);
            }
        }
    }

    public boolean canShowLegend() {
        return false;
    }

    @Override
    public String getChartDrawerName() {
        return NAME;
    }

    @Override
    public boolean canTranspose() {
        return true;
    }

    /**
     * do we need to recompute coordinates?
     *
     * @return true, if coordinates need to be recomputed
     */
    private boolean mustUpdateCoordinates() {
        boolean mustUpdate = (dataMatrix.size() == 0);


        if (previousTranspose != isTranspose()) {
            mustUpdate = true;
        }

        if (scalingType != ChartViewer.ScalingType.LINEAR)
            return mustUpdate;

        if (previousTranspose != isTranspose()) {
            previousTranspose = isTranspose();
            previousClusterSeries = false;
            previousClusterClasses = false;
        }

        {
            final ArrayList<String> currentClasses = new ArrayList<>(getChartData().getClassNames());
            if (!previousClasses.equals(currentClasses)) {
                mustUpdate = true;
                previousClasses.clear();
                previousClasses.addAll(currentClasses);
            }
        }
        {
            final ArrayList<String> currentSamples = new ArrayList<>(getChartData().getSeriesNames());
            if (!previousSamples.equals(currentSamples)) {
                mustUpdate = true;
                previousSamples.clear();
                previousSamples.addAll(currentSamples);
            }
        }
        {
            if (!previousClusterClasses && viewer.getClassesList().isDoClustering() && !isTranspose())
                mustUpdate = true;
        }

        {
            if (!previousClusterSeries && viewer.getSeriesList().isDoClustering() && isTranspose())
                mustUpdate = true;
        }

        return mustUpdate;
    }

    /**
     * force update
     */
    @Override
    public void forceUpdate() {
        dataMatrix.clear();
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
            future = executorService.submit(() -> {
                try {
                    updateCoordinates();
                    SwingUtilities.invokeAndWait(() -> {
                        if (!previousClusterClasses && viewer.getClassesList().isDoClustering())
                            updateClassesJList();
                        previousClusterClasses = viewer.getClassesList().isDoClustering();
                        if (!previousClusterSeries && viewer.getSeriesList().isDoClustering())
                            updateSeriesJList();
                        previousClusterSeries = viewer.getSeriesList().isDoClustering();
                        viewer.repaint();
                    });
                } catch (Exception e) {
                    // Basic.caught(e);
                } finally {
                    future = null;
                    inUpdateCoordinates = false;
                }
            });
        }
    }

    void updateCoordinates() {
        System.err.println("Updating...");

        dataMatrix.clear();
        previousClasses.clear();
        previousSamples.clear();
        topClusteringTree.clear();
        rightClusteringTree.clear();

        if (topClusteringTree.getChartSelection() == null)
            topClusteringTree.setChartSelection(viewer.getChartSelection());

        if (rightClusteringTree.getChartSelection() == null)
            rightClusteringTree.setChartSelection(viewer.getChartSelection());

        final String[] currentClasses;
        {
            final Collection<String> list = getViewer().getClassesList().getEnabledLabels();
            currentClasses = list.toArray(new String[0]);
        }

        final String[] currentSeries;
        {
            final Collection<String> list = getViewer().getSeriesList().getEnabledLabels();
            currentSeries = list.toArray(new String[0]);
        }
        if (!isTranspose()) {
            for (int i = 0; i < currentClasses.length; i++) {
                dataMatrix.put(currentClasses[i], currentClasses[i], 1f);
                for (int j = i + 1; j < currentClasses.length; j++) {
                    final float value = computeCorrelationCoefficent(currentClasses[i], currentClasses[j]);
                    dataMatrix.put(currentClasses[i], currentClasses[j], value);
                    dataMatrix.put(currentClasses[j], currentClasses[i], value);
                }
            }
            if (viewer.getClassesList().isDoClustering()) {
                topClusteringTree.setType(ClusteringTree.TYPE.CLASSES);
                topClusteringTree.updateClustering(currentClasses, dataMatrix);
                rightClusteringTree.setType(ClusteringTree.TYPE.CLASSES);
                rightClusteringTree.updateClustering(currentClasses, dataMatrix);
                final Collection<String> list = topClusteringTree.getLabelOrder();
                classNames = list.toArray(new String[0]);
            } else
                classNames = currentClasses;
        } else {
            for (int i = 0; i < currentSeries.length; i++) {
                dataMatrix.put(currentSeries[i], currentSeries[i], 1f);
                for (int j = i + 1; j < currentSeries.length; j++) {
                    final float value = computeCorrelationCoefficentTransposed(currentSeries[i], currentSeries[j]);
                    dataMatrix.put(currentSeries[i], currentSeries[j], value);
                    dataMatrix.put(currentSeries[j], currentSeries[i], value);
                }
                if (viewer.getSeriesList().isDoClustering()) {
                    topClusteringTree.setType(ClusteringTree.TYPE.SERIES);
                    topClusteringTree.updateClustering(currentSeries, dataMatrix);
                    rightClusteringTree.setType(ClusteringTree.TYPE.SERIES);
                    rightClusteringTree.updateClustering(currentSeries, dataMatrix);
                    final Collection<String> list = topClusteringTree.getLabelOrder();
                    seriesNames = list.toArray(new String[0]);
                } else
                    seriesNames = currentSeries;
            }
        }
        chartData.setClassesLabel("");
    }

    /**
     * return Pearson's correlation coefficient
     *
     * @param classNameX
     * @param classNameY
     * @return Pearson's correlation coefficient
     */
    private float computeCorrelationCoefficent(String classNameX, String classNameY) {
        final ArrayList<Double> xValues = new ArrayList<>(getChartData().getSeriesNames().size());
        final ArrayList<Double> yValues = new ArrayList<>(getChartData().getSeriesNames().size());

        for (String sample : getChartData().getSeriesNames()) {
            xValues.add(getChartData().getValueAsDouble(sample, classNameX));
            yValues.add(getChartData().getValueAsDouble(sample, classNameY));
        }
        return (float) Correlation.computePersonsCorrelationCoefficent(xValues.size(), xValues, yValues);
    }

    /**
     * return Pearson's correlation coefficient
     *
     * @param seriesNameX
     * @param seriesNameY
     * @return Pearson's correlation coefficient
     */
    private float computeCorrelationCoefficentTransposed(String seriesNameX, String seriesNameY) {
        final ArrayList<Double> xValues = new ArrayList<>(getChartData().getClassNames().size());
        final ArrayList<Double> yValues = new ArrayList<>(getChartData().getClassNames().size());

        for (String className : getChartData().getClassNames()) {
            xValues.add(getChartData().getValueAsDouble(seriesNameX, className));
            yValues.add(getChartData().getValueAsDouble(seriesNameY, className));
        }
        return (float) Correlation.computePersonsCorrelationCoefficent(xValues.size(), xValues, yValues);
    }

    private void updateClassesJList() {
        final Collection<String> selected = new ArrayList<>(viewer.getChartSelection().getSelectedClasses());
        final Collection<String> ordered = new ArrayList<>(Arrays.asList(classNames));
        final Collection<String> others = viewer.getClassesList().getAllLabels();
        others.removeAll(ordered);
        ordered.addAll(others);
        viewer.getClassesList().sync(ordered, viewer.getClassesList().getLabel2ToolTips(), true);
        viewer.getClassesList().setDisabledLabels(others);
        viewer.getChartSelection().setSelectedClass(selected, true);
    }

    private void updateSeriesJList() {
        final Collection<String> selected = new ArrayList<>(viewer.getChartSelection().getSelectedSeries());
        final Collection<String> ordered = new ArrayList<>(Arrays.asList(seriesNames));
        final Collection<String> others = viewer.getSeriesList().getAllLabels();
        others.removeAll(ordered);
        ordered.addAll(others);
        viewer.getSeriesList().sync(ordered, null, true);
        viewer.getSeriesList().setDisabledLabels(others);
        viewer.getChartSelection().setSelectedSeries(selected, true);
    }

    protected double computeXAxisLabelHeight(Graphics2D gc) {
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));
        double theHeight = 2 * gc.getFont().getSize();
        if (classLabelAngle != 0) {
            double sin = Math.abs(Math.sin(classLabelAngle));
            for (String label : getChartData().getClassNames()) {
                if (label.length() > 50)
                    label = label.substring(0, 50) + "...";
                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                theHeight = Math.max(theHeight, gc.getFont().getSize() + sin * labelSize.width);
            }
        }
        return theHeight;
    }

    private MODE getMode() {
        return mode;
    }

    private void setMode(MODE mode) {
        this.mode = mode;
    }

    public void setMode(String mode) {
        this.mode = MODE.valueOfIgnoreCase(mode);
    }

    @Override
    public boolean canColorByRank() {
        return false;
    }

    @Override
    public boolean usesHeatMapColors() {
        return true;
    }

    public IPopupMenuModifier getPopupMenuModifier() {
        return (menu, commandManager) -> {
            menu.addSeparator();
            MODE mode = MODE.valueOfIgnoreCase(ProgramProperties.get("CorrelationPlotMode", MODE.Beans.toString()));
            final CallBack<MODE> callBack = new CallBack<>() {
                public void call(MODE choice) {
                    setMode(choice);
                    ProgramProperties.put("CorrelationPlotMode", choice.toString());
                    getJPanel().repaint();
                }
            };
            PopupChoice.addToJMenu(menu, MODE.values(), mode, callBack);
            menu.addSeparator();

            final AbstractAction action = (new AbstractAction("Flip Selected Subtree") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (rightClusteringTree.hasSelectedSubTree()) {
                        //System.err.println("Rotate Classes");
                        rightClusteringTree.rotateSelectedSubTree();
                        final Collection<String> list = rightClusteringTree.getLabelOrder();
                        if (!isTranspose()) {
                            classNames = list.toArray(new String[0]);
                            updateClassesJList();
                        } else {
                            seriesNames = list.toArray(new String[0]);
                            updateSeriesJList();
                        }
                        getJPanel().repaint();
                    }
                }
            });
            action.setEnabled(rightClusteringTree.hasSelectedSubTree());
            menu.add(action);

        };
    }

    @Override
    public void writeData(Writer w) throws IOException {
        w.write("CorrelationPlot");
        if (!isTranspose()) {
            for (String className : classNames) {
                w.write("\t" + className);
            }
            w.write("\n");

            for (String name1 : classNames) {
                w.write(name1);
                for (String name2 : classNames) {
                    w.write(String.format("\t%.4g", dataMatrix.get(name1, name2)));
                }
                w.write("\n");
            }
        } else {
            for (String name : seriesNames) {
                w.write("\t" + name);
            }
            w.write("\n");

            for (String name1 : seriesNames) {
                w.write(name1);
                for (String name2 : seriesNames) {
                    w.write(String.format("\t%.4g", dataMatrix.get(name1, name2)));
                }
                w.write("\n");
            }
        }
    }

    @Override
    public boolean canCluster(ClusteringTree.TYPE type) {
        return ((type == null) || ((type == ClusteringTree.TYPE.CLASSES) && !isTranspose()) || ((type == ClusteringTree.TYPE.SERIES) && isTranspose()));
    }
}