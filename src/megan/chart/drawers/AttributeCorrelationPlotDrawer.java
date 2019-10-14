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
import jloda.util.Basic;
import jloda.util.Correlation;
import jloda.util.ProgramProperties;
import megan.chart.IChartDrawer;
import megan.chart.cluster.ClusteringTree;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * draws a correlation plot
 * Daniel Huson, 11.2015
 */
public class AttributeCorrelationPlotDrawer extends CorrelationPlotDrawer implements IChartDrawer {
    private static final String NAME = "AttributeCorrelationPlot";

    private String[] attributeNames;

    private final Set<String> previousAttributes = new HashSet<>();

    private final ClusteringTree attributesClusteringTree;
    private final ClusteringTree classesClusteringTree;

    private boolean previousClusterAttributes = false;

    /**
     * constructor
     */
    public AttributeCorrelationPlotDrawer() {
        super();
        attributesClusteringTree = new ClusteringTree(ClusteringTree.TYPE.ATTRIBUTES, ClusteringTree.SIDE.RIGHT);
        classesClusteringTree = new ClusteringTree(ClusteringTree.TYPE.CLASSES, ClusteringTree.SIDE.TOP);
        previousTranspose = isTranspose();
    }

    /**
     * draw correlation plot chart
     *
     * @param gc
     */
    public void drawChart(Graphics2D gc) {
        final SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
            drawYAxis(gc, null); // need this for selection
        }

        if (!getChartTitle().startsWith("Correlation plot: "))
            setChartTitle("Correlation plot: " + getChartTitle());

        final int numberOfClasses = (classNames != null ? classNames.length : 0);
        final int numberOfAttributes = (attributeNames != null ? attributeNames.length : 0);

        if (viewer.getClassesList().isDoClustering())
            y1 += topTreeSpace; // do this before other clustering

        if (sgc == null) {
            drawScaleBar(gc, x1, scaleWidth, y1, y0 - y1);
        }

        if (viewer.getAttributesList().isDoClustering()) {
            x1 -= rightTreeSpace;
            int height = (int) Math.round((y0 - y1) / (numberOfAttributes + 1.0) * numberOfAttributes);
            int yStart = y0 + ((y1 - y0) - height) / 2;
            final Rectangle rect = new Rectangle(x1, yStart, rightTreeSpace, height);
            attributesClusteringTree.paint(gc, rect);
        }

        if (viewer.getClassesList().isDoClustering()) {
            int width = (int) ((x1 - x0) / (numberOfClasses + 1.0) * numberOfClasses);
            int xStart = x0 + ((x1 - x0) - width) / 2;
            final Rectangle rect = new Rectangle(xStart, y1 - topTreeSpace, width, topTreeSpace);
            classesClusteringTree.paint(gc, rect);
        }

        // main drawing loop:
        if (numberOfClasses > 0 && numberOfAttributes > 0) {
            double xStep = (x1 - x0) / (double) numberOfClasses;
            double yStep = (y0 - y1) / (double) numberOfAttributes;

            int d = 0;
            for (String classNameX : classNames) {
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

                int c = numberOfAttributes - 1;
                for (String attributeNameY : attributeNames) {
                    final Float correlationCoefficient = dataMatrix.get(classNameX, attributeNameY);
                    if (correlationCoefficient != null) {
                        final double[] boundingBox = new double[]{x0 + d * xStep, y0 - (c + 1) * yStep, xStep, yStep};

                        // gc.drawRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                        drawCell(gc, boundingBox, correlationCoefficient);

                        if (sgc != null && !sgc.isShiftDown()) {
                            sgc.setCurrentItem(new String[]{null, classNameX, attributeNameY});
                            gc.fillRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                            sgc.clearCurrentItem();
                            sgc.setCurrentItem(new String[]{null, null, attributeNameY});
                            gc.fillRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                            sgc.clearCurrentItem();
                        }
                        boolean isSelected = false;
                        if (getChartData().getChartSelection().isSelectedClass(classNameX)) {
                            if (getChartData().getChartSelection().isSelectedAttribute(attributeNameY) || getChartData().getChartSelection().getSelectedAttributes().size() == 0)
                                isSelected = true;
                        } else if (getChartData().getChartSelection().getSelectedClasses().size() == 0 && getChartData().getChartSelection().isSelectedAttribute(attributeNameY))
                            isSelected = true;

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
        final SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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

        final int numberOfClasses = (classNames != null ? classNames.length : 0);

        final int numberOfAttributes = (attributeNames != null ? attributeNames.length : 0);

        if (viewer.getAttributesList().isDoClustering())
            y1 += topTreeSpace; // do this before other clustering

        if (sgc == null) {
            drawScaleBar(gc, x1, scaleWidth, y1, y0 - y1);
        }

        if (viewer.getClassesList().isDoClustering()) {
            x1 -= rightTreeSpace;
            int height = (int) Math.round((y0 - y1) / (numberOfClasses + 1.0) * numberOfClasses);
            int yStart = y0 + ((y1 - y0) - height) / 2;
            final Rectangle rect = new Rectangle(x1, yStart, rightTreeSpace, height);
            classesClusteringTree.paint(gc, rect);
        }

        if (viewer.getAttributesList().isDoClustering()) {
            int width = (int) ((x1 - x0) / (numberOfAttributes + 1.0) * numberOfAttributes);
            int xStart = x0 + ((x1 - x0) - width) / 2;
            final Rectangle rect = new Rectangle(xStart, y1 - topTreeSpace, width, topTreeSpace);
            attributesClusteringTree.paint(gc, rect);
        }

        double xStep = (x1 - x0) / (double) numberOfAttributes;
        double yStep = (y0 - y1) / (double) numberOfClasses;

        // main drawing loop:
        if (numberOfClasses > 0 && numberOfAttributes > 0) {
            int d = 0;
            for (String attributeNameX : attributeNames) {
                final double xLabel = x0 + (d + 0.5) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 10);
                final Dimension labelSize = BasicSwing.getStringSize(gc, attributeNameX, gc.getFont()).getSize();
                if (classLabelAngle == 0) {
                    apt.setLocation(apt.getX() - labelSize.getWidth() / 2, apt.getY());
                } else if (classLabelAngle > Math.PI / 2) {
                    apt = Geometry.translateByAngle(apt, classLabelAngle, -labelSize.width);
                }
                if (getChartData().getChartSelection().isSelectedAttribute(attributeNameX)) {
                    fillAndDrawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, classLabelAngle, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                }
                gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.DARK_GRAY));
                drawString(gc, attributeNameX, apt.getX(), apt.getY(), classLabelAngle);
                if (sgc != null) {
                    sgc.setCurrentItem(new String[]{null, attributeNameX});
                    drawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, classLabelAngle);
                    sgc.clearCurrentItem();
                }

                int c = numberOfClasses - 1;
                for (String classNameY : classNames) {
                    final Float correlationCoefficient = dataMatrix.get(classNameY, attributeNameX);
                    if (correlationCoefficient != null) {
                        final double[] boundingBox = new double[]{x0 + d * xStep, y0 - (c + 1) * yStep, xStep, yStep};

                        // gc.drawRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                        drawCell(gc, boundingBox, correlationCoefficient);

                        if (sgc != null && !sgc.isShiftDown()) {
                            sgc.setCurrentItem(new String[]{null, classNameY, attributeNameX});
                            gc.fillRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                            sgc.clearCurrentItem();
                            sgc.setCurrentItem(new String[]{null, classNameY, attributeNameX});
                            gc.fillRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                            sgc.clearCurrentItem();
                        }
                        boolean isSelected = false;
                        if (getChartData().getChartSelection().isSelectedClass(classNameY)) {
                            if (getChartData().getChartSelection().isSelectedAttribute(attributeNameX) || getChartData().getChartSelection().getSelectedAttributes().size() == 0)
                                isSelected = true;
                        } else if (getChartData().getChartSelection().getSelectedClasses().size() == 0 && getChartData().getChartSelection().isSelectedAttribute(attributeNameX))
                            isSelected = true;

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
        final int numberOfAttributes = (attributeNames == null ? 0 : attributeNames.length);
        if (numberOfAttributes > 0) {

            final SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
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
            for (String attributeName : attributeNames) {
                longest = Math.max(longest, BasicSwing.getStringSize(gc, attributeName, gc.getFont()).getSize().width);
            }
            int right = Math.max(leftMargin, longest + 5);

            if (doDraw)
                gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));

            double yStep = (y0 - y1) / (double) numberOfAttributes;
            int d = numberOfAttributes - 1;
            for (String attributeName : attributeNames) {
                Dimension labelSize = BasicSwing.getStringSize(gc, attributeName, gc.getFont()).getSize();
                int x = right - labelSize.width - 4;
                int y = (int) Math.round(y0 - (d + 0.5) * yStep);
                if (doDraw) {
                    if (getChartData().getChartSelection().isSelectedAttribute(attributeName)) {
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        fillAndDrawRect(gc, x, y, labelSize.width, labelSize.height, 0, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                    }
                    gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.DARK_GRAY));
                    gc.drawString(attributeName, x, y);
                } else {
                    Rectangle rect = new Rectangle(x, y, labelSize.width, labelSize.height);
                    if (bbox == null)
                        bbox = rect;
                    else
                        bbox.add(rect);
                }
                if (sgc != null) {
                    sgc.setCurrentItem(new String[]{null, null, attributeName});
                    drawRect(gc, x, y, labelSize.width, labelSize.height, 0);
                    sgc.clearCurrentItem();
                }
                d--;
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
    protected void drawYAxisTransposed(Graphics2D gc, Dimension size) {
        final int numberOfClasses = (classNames == null ? 0 : classNames.length);
        if (numberOfClasses > 0) {
            final SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
            gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));

            final boolean doDraw = (size == null);
            Rectangle bbox = null;

            int x0 = leftMargin;
            int x1 = getWidth() - rightMargin;
            int y0 = getHeight() - bottomMargin;
            int y1 = topMargin;

            if (viewer.getAttributesList().isDoClustering())
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
                    if (getChartData().getChartSelection().isSelectedClass(className)) {
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
            previousClusterAttributes = false;
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
            final Set<String> currentAttributes = new HashSet<>(getViewer().getAttributesList().getEnabledLabels());
            if (!previousAttributes.equals(currentAttributes)) {
                mustUpdate = true;
                previousAttributes.clear();
                previousAttributes.addAll(currentAttributes);
            }
        }

        {
            if (!previousClusterClasses && viewer.getClassesList().isDoClustering())
                mustUpdate = true;
        }

        {
            if (!previousClusterAttributes && viewer.getAttributesList().isDoClustering())
                mustUpdate = true;
        }

        if (!mustUpdate) {
            final Set<String> currentAttributes = new HashSet<>(getViewer().getAttributesList().getAllLabels());
            if (!currentAttributes.equals(viewer.getDir().getDocument().getSampleAttributeTable().getNumericalAttributes())) {
                viewer.getAttributesList().sync(viewer.getDir().getDocument().getSampleAttributeTable().getNumericalAttributes(), null, false);
                mustUpdate = true;
            }
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
            future = executorService.submit(() -> {
                try {
                    inUpdateCoordinates = true;
                    updateCoordinates();
                    SwingUtilities.invokeAndWait(() -> {
                        try {
                            if (!previousClusterClasses && viewer.getClassesList().isDoClustering())
                                updateClassesJList();
                            previousClusterClasses = viewer.getClassesList().isDoClustering();
                            if (!previousClusterAttributes && viewer.getAttributesList().isDoClustering())
                                updateAttributesJList();
                            previousClusterAttributes = viewer.getAttributesList().isDoClustering();
                            viewer.repaint();
                        } catch (Exception e) {
                            Basic.caught(e);
                        }
                    });
                } catch (Exception e) {
                    //Basic.caught(e);
                } finally {
                    future = null;
                    inUpdateCoordinates = false;
                }
            });
        } else {
            if (!previousClusterClasses && viewer.getClassesList().isDoClustering())
                updateClassesJList();
            previousClusterClasses = viewer.getClassesList().isDoClustering();
            if (!previousClusterAttributes && viewer.getAttributesList().isDoClustering())
                updateAttributesJList();
            previousClusterAttributes = viewer.getAttributesList().isDoClustering();
        }
    }

    /**
     * force update
     */
    @Override
    public void forceUpdate() {
        dataMatrix.clear();
    }

    protected void updateCoordinates() {
        System.err.println("Updating...");

        dataMatrix.clear();
        previousClasses.clear();
        previousSamples.clear();
        attributesClusteringTree.clear();
        classesClusteringTree.clear();

        if (classesClusteringTree.getChartSelection() == null)
            classesClusteringTree.setChartSelection(viewer.getChartSelection());

        if (attributesClusteringTree.getChartSelection() == null)
            attributesClusteringTree.setChartSelection(viewer.getChartSelection());

        final String[] currentClasses;
        {
            final Collection<String> list = getViewer().getClassesList().getEnabledLabels();
            currentClasses = list.toArray(new String[0]);
        }

        final String[] currentAttributes;
        {
            final Collection<String> list = getViewer().getAttributesList().getEnabledLabels();
            currentAttributes = list.toArray(new String[0]);
        }

        for (String className : currentClasses) {
            for (String attributeName : currentAttributes) {
                try {
                    dataMatrix.put(className, attributeName, computeCorrelationCoefficent(className, attributeName));
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }
        }
        if (viewer.getClassesList().isDoClustering()) {
            classesClusteringTree.setRootSide(isTranspose() ? ClusteringTree.SIDE.RIGHT : ClusteringTree.SIDE.TOP);
            classesClusteringTree.updateClustering(currentClasses, dataMatrix.copy());
            final Collection<String> list = classesClusteringTree.getLabelOrder();
            classNames = list.toArray(new String[0]);
        } else
            classNames = currentClasses;

        if (viewer.getAttributesList().isDoClustering()) {
            attributesClusteringTree.setRootSide(isTranspose() ? ClusteringTree.SIDE.TOP : ClusteringTree.SIDE.RIGHT);
            attributesClusteringTree.updateClustering(currentAttributes, dataMatrix.computeTransposedTable());
            final Collection<String> list = attributesClusteringTree.getLabelOrder();
            attributeNames = list.toArray(new String[0]);
        } else
            attributeNames = currentAttributes;

        chartData.setClassesLabel("");
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

    private void updateAttributesJList() {
        final Collection<String> selected = new ArrayList<>(viewer.getChartSelection().getSelectedAttributes());
        final Collection<String> ordered = new ArrayList<>(Arrays.asList(attributeNames));
        final Collection<String> others = viewer.getAttributesList().getAllLabels();
        others.removeAll(ordered);
        ordered.addAll(others);
        viewer.getAttributesList().sync(ordered, null, true);
        viewer.getAttributesList().setDisabledLabels(others);
        viewer.getChartSelection().setSelectedAttribute(selected, true);
    }

    /**
     * return Pearson's correlation coefficient
     *
     * @param classNameX
     * @param attributeNameY
     * @return Pearson's correlation coefficient
     */
    private float computeCorrelationCoefficent(String classNameX, String attributeNameY) {
        ArrayList<Double> xValues = new ArrayList<>(getChartData().getSeriesNames().size());
        ArrayList<Double> yValues = new ArrayList<>(getChartData().getSeriesNames().size());

        for (String sample : getChartData().getSeriesNames()) {
            final double x = getChartData().getValueAsDouble(sample, classNameX);
            final double y;

            final Object obj = viewer.getDir().getDocument().getSampleAttributeTable().get(sample, attributeNameY);
            if (obj == null)
                y = 0;
            else if (obj instanceof Number)
                y = ((Number) obj).doubleValue();
            else if (Basic.isDouble(obj.toString())) {
                y = Basic.parseDouble(obj.toString());
            } else
                throw new IllegalArgumentException("Attribute '" + attributeNameY + "': has non-numerical value: " + obj);

            xValues.add(x);
            yValues.add(y);
        }
        return (float) Correlation.computePersonsCorrelationCoefficent(xValues.size(), xValues, yValues);
    }

    @Override
    public String getChartDrawerName() {
        return NAME;
    }

    public IPopupMenuModifier getPopupMenuModifier() {
        return (menu, commandManager) -> {
            AttributeCorrelationPlotDrawer.super.getPopupMenuModifier().apply(menu, commandManager);

            menu.addSeparator();
            {
                final AbstractAction action = (new AbstractAction("Show Correlation Values...") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        final String classification = viewer.getParentViewer().getClassName();

                        final StringBuilder buf = new StringBuilder();
                        buf.append("show window=Message;");
                        for (String attribute : getChartData().getChartSelection().getSelectedSeries()) {
                            buf.append(";correlate class='").append(Basic.toString(getChartData().getChartSelection().getSelectedClasses(), "' '")).append("'")
                                    .append(" classification='").append(classification).append("' attribute='").append(attribute).append("';");
                        }
                        commandManager.getDir().execute(buf.toString(), commandManager);
                    }
                });
                action.setEnabled(getChartData().getChartSelection().getSelectedClasses().size() >= 1 &&
                        getChartData().getChartSelection().getSelectedSeries().size() >= 1);

                menu.add(action);
            }
            menu.addSeparator();

            {
                final AbstractAction action = (new AbstractAction("Flip Selected Subtree") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (classesClusteringTree.hasSelectedSubTree()) {
                            //System.err.println("Rotate Classes");
                            classesClusteringTree.rotateSelectedSubTree();
                            final Collection<String> list = classesClusteringTree.getLabelOrder();
                            classNames = list.toArray(new String[list.size()]);
                            updateClassesJList();
                            getJPanel().repaint();
                        } else if (attributesClusteringTree.hasSelectedSubTree()) {
                            // System.err.println("Old order: "+Basic.toString(seriesClusteringTree.getLabelOrder(),","));
                            //System.err.println("Rotate Series");
                            attributesClusteringTree.rotateSelectedSubTree();
                            //System.err.println("New order: "+Basic.toString(seriesClusteringTree.getLabelOrder(),","));
                            final Collection<String> list = attributesClusteringTree.getLabelOrder();
                            attributeNames = list.toArray(new String[list.size()]);
                            updateAttributesJList();
                            getJPanel().repaint();
                        }
                    }
                });
                action.setEnabled(classesClusteringTree.hasSelectedSubTree() != attributesClusteringTree.hasSelectedSubTree());
                menu.add(action);
            }
        };
    }

    @Override
    public void writeData(Writer w) throws IOException {
        w.write("AttributeCorrelationPlot");
        for (String className : classNames) {
            w.write("\t" + className);
        }
        w.write("\n");

        for (String attributeName : attributeNames) {
            w.write(attributeName);
            for (String className : classNames) {
                final double correlationCoefficient = dataMatrix.get(className, attributeName);
                w.write(String.format("\t%.4g", correlationCoefficient));
            }
            w.write("\n");
        }
    }

    @Override
    public boolean canCluster(ClusteringTree.TYPE type) {
        return (type == null || type == ClusteringTree.TYPE.ATTRIBUTES || type == ClusteringTree.TYPE.CLASSES);
    }

    public boolean canAttributes() {
        return true;
    }
}