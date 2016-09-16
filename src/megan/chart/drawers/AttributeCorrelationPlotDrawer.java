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

import jloda.gui.IPopupMenuModifier;
import jloda.gui.commands.CommandManager;
import jloda.util.Basic;
import jloda.util.Correlation;
import jloda.util.Geometry;
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
import java.util.concurrent.Future;

/**
 * draws a correlation plot
 * Daniel Huson, 11.2015
 */
public class AttributeCorrelationPlotDrawer extends CorrelationPlotDrawer implements IChartDrawer {

    public static final String NAME = "AttributeCorrelationPlot";

    private String[] attributeNames;

    protected boolean inUpdateCoordinates = true;
    private final ArrayList<String> previousSamples = new ArrayList<>();
    private final ArrayList<String> previousAttributes = new ArrayList<>();
    private final ArrayList<String> previousClasses = new ArrayList<>();
    private Future future; // used in recompute

    private final ClusteringTree attributesClusteringTree;
    private final ClusteringTree classesClusteringTree;

    private final int treeSpace = 100;

    /**
     * constructor
     */
    public AttributeCorrelationPlotDrawer() {
        super();
        attributesClusteringTree = new ClusteringTree(ClusteringTree.TYPE.ATTRIBUTES, ClusteringTree.SIDE.RIGHT);
        classesClusteringTree = new ClusteringTree(ClusteringTree.TYPE.CLASSES, ClusteringTree.SIDE.TOP);
    }

    /**
     * draw correlation plot chart
     *
     * @param gc
     */
    public void drawChart(Graphics2D gc) {
        updateView();

        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
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

        final int numberOfClasses = getChartData().getNumberOfClasses();

        final int numberOfAttributes = (attributeNames != null ? attributeNames.length : 0);

        final Collection<String> classesOrder;
        final Map<String, Integer> class2pos = new HashMap<>();
        for (int i = 0; i < classNames.length; i++)
            class2pos.put(classNames[i], i);

        final Collection<String> attributesOrder;
        final Map<String, Integer> attribute2pos = new HashMap<>();
        for (int i = 0; i < attributeNames.length; i++)
            attribute2pos.put(attributeNames[i], i);

        if (isDoClustering()) {
            x1 -= treeSpace;

            y1 += treeSpace;

            {
                int width = (int) ((x1 - x0) / (numberOfClasses + 1.0) * numberOfClasses);
                int xStart = x0 + ((x1 - x0) - width) / 2;
                final Rectangle rect = new Rectangle(xStart, y1 - treeSpace, width, treeSpace);
                classesClusteringTree.paint(gc, rect);
                classesOrder = classesClusteringTree.getLabelOrder();
            }
            {
                int height = (int) Math.round((y0 - y1) / (numberOfAttributes + 1.0) * numberOfAttributes);
                int yStart = y0 + ((y1 - y0) - height) / 2;
                final Rectangle rect = new Rectangle(x1, yStart, treeSpace, height);
                attributesClusteringTree.paint(gc, rect);
                attributesOrder = attributesClusteringTree.getLabelOrder();
            }
        } else {
            classesOrder = getChartData().getClassNames();
            attributesOrder = new ArrayList<>(numberOfAttributes);
            Collections.addAll(attributesOrder, attributeNames);
        }

        double xStep = (x1 - x0) / (double) numberOfClasses;
        double yStep = (y0 - y1) / (double) numberOfAttributes;

        // main drawing loop:
        if (classNames != null) {
            int d = 0;
            for (String classNameX : classesOrder) {
                final double xLabel = x0 + (d + 0.5) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 10);
                final Dimension labelSize = Basic.getStringSize(gc, classNameX, gc.getFont()).getSize();
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
                for (String attributeNameY : attributesOrder) {
                    final double correlationCoefficient = dataMatrix[class2pos.get(classNameX)][attribute2pos.get(attributeNameY)];
                    final double[] boundingBox = new double[]{x0 + d * xStep, y0 - (c + 1) * yStep, xStep, yStep};

                    // gc.drawRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                    drawCell(gc, boundingBox, correlationCoefficient);

                    if (sgc != null && !sgc.isShiftDown()) {
                        sgc.setCurrentItem(new String[]{null, classNameX});
                        gc.fillRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                        sgc.clearCurrentItem();
                        sgc.setCurrentItem(new String[]{attributeNameY, null});
                        gc.fillRect((int) Math.round(boundingBox[0]), (int) Math.round(boundingBox[1]), (int) Math.round(boundingBox[2]), (int) Math.round(boundingBox[3]));
                        sgc.clearCurrentItem();
                    }
                    boolean isSelected = getChartData().getChartSelection().isSelectedClass(classNameX) || getChartData().getChartSelection().isSelectedAttribute(attributeNameY);
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
        final SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));

        final boolean doDraw = (size == null);
        Rectangle bbox = null;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        if (isDoClustering())
            y1 += treeSpace;

        if (attributeNames == null)
            return;

        final Collection<String> attributesOrder;
        final int numberOfAttributes;
        if (isDoClustering()) {
            attributesOrder = attributesClusteringTree.getLabelOrder();
            numberOfAttributes = attributesOrder.size();
        } else {
            numberOfAttributes = attributeNames.length;
            attributesOrder = new ArrayList<>(numberOfAttributes);
            Collections.addAll(attributesOrder, attributeNames);
        }

        int longest = 0;
        for (String attributeName : attributeNames) {
            longest = Math.max(longest, Basic.getStringSize(gc, attributeName, gc.getFont()).getSize().width);
        }
        int right = Math.max(leftMargin, longest + 5);

        if (doDraw)
            gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));

        double yStep = (y0 - y1) / (double) numberOfAttributes;
        int c = numberOfAttributes - 1;
        for (String attributeName : attributesOrder) {
            Dimension labelSize = Basic.getStringSize(gc, attributeName, gc.getFont()).getSize();
            int x = right - labelSize.width - 4;
            int y = (int) Math.round(y0 - (c + 0.5) * yStep);
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
            c--;
        }
        if (size != null && bbox != null) {
            size.setSize(bbox.width + 3, bbox.height);
        }
    }

    /**
     * do we need to recompute coordinates?
     *
     * @return true, if coordinates need to be recomputed
     */
    private boolean mustUpdateCoordinates() {
        boolean mustUpdate = (dataMatrix == null);


        if (!mustUpdate && scalingType == ChartViewer.ScalingType.LINEAR && getChartData().getNumberOfClasses() > 0 &&
                getChartData().getNumberOfSeries() > 0 && dataMatrix.length == 0) {
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

        final ArrayList<String> currentAttributes = new ArrayList<>();
        currentAttributes.addAll(getViewer().getNumericalAttributes());
        if (!previousAttributes.equals(currentAttributes)) {
            mustUpdate = true;
            previousAttributes.clear();
            previousAttributes.addAll(currentAttributes);
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

        dataMatrix = null;
        previousClasses.clear();
        previousSamples.clear();
        attributesClusteringTree.clear();
        classesClusteringTree.clear();
    }

    protected void updateCoordinates() {
        System.err.println("Updating...");

        classesClusteringTree.setRootSide(ClusteringTree.SIDE.TOP);
        if (classesClusteringTree.getChartSelection() == null)
            classesClusteringTree.setChartSelection(viewer.getChartSelection());

        attributesClusteringTree.setRootSide(ClusteringTree.SIDE.RIGHT);
        if (attributesClusteringTree.getChartSelection() == null)
            attributesClusteringTree.setChartSelection(viewer.getChartSelection());

        classNames = getChartData().getClassNames().toArray(new String[getChartData().getNumberOfClasses()]);

        final Collection<String> list = getViewer().getNumericalAttributes();
        attributeNames = list.toArray(new String[list.size()]);

        dataMatrix = new float[classNames.length][attributeNames.length];

        for (int i = 0; i < classNames.length; i++) {
            for (int j = 0; j < attributeNames.length; j++) {
                try {
                    dataMatrix[i][j] = computeCorrelationCoefficent(classNames[i], attributeNames[j]);
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }
        }
        classesClusteringTree.updateClustering(classNames, dataMatrix);

        attributesClusteringTree.updateClustering(attributeNames, Basic.transposeMatrix(dataMatrix));

        // todo: check whether we always need to call this
        updateClassesJList();
        updateAttributesJList();
    }

    private void updateClassesJList() {
        final Collection<String> selected = new ArrayList<>();
        selected.addAll(viewer.getChartSelection().getSelectedClasses());
        final Collection<String> ordered = classesClusteringTree.getLabelOrder();
        final Collection<String> others = viewer.getClassesList().getAllLabels();
        others.removeAll(ordered);
        ordered.addAll(others);
        viewer.getChartSelection().setSelectedClass(selected, true);
        viewer.getClassesList().sync(ordered, viewer.getClassesList().getLabel2ToolTips(), true);
        viewer.getClassesList().setDisabledLabels(others);
    }

    private void updateAttributesJList() {
        final Collection<String> selected = new ArrayList<>();
        selected.addAll(viewer.getChartSelection().getSelectedAttributes());
        final Collection<String> ordered = attributesClusteringTree.getLabelOrder();
        final Collection<String> others = getViewer().getNumericalAttributes();
        others.removeAll(ordered);
        ordered.addAll(others);
        viewer.getChartSelection().setSelectedAttribute(selected, true);
        //viewer.getClassesList().sync(ordered, viewer.getClassesList().getLabel2ToolTips(), true);
        //viewer.getClassesList().setDisabledLabels(others);
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

            Object obj = viewer.getDir().getDocument().getSampleAttributeTable().get(sample, attributeNameY);
            if (obj instanceof Number)
                y = ((Number) obj).doubleValue();
            else
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
        return new IPopupMenuModifier() {
            @Override
            public void apply(JPopupMenu menu, final CommandManager commandManager) {
                AttributeCorrelationPlotDrawer.super.getPopupMenuModifier().apply(menu, commandManager);

                menu.addSeparator();
                {
                    AbstractAction action = (new AbstractAction("Show Correlation Values...") {
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
                                updateClassesJList();
                                getJPanel().repaint();
                            } else if (attributesClusteringTree.hasSelectedSubTree()) {
                                // System.err.println("Old order: "+Basic.toString(seriesClusteringTree.getLabelOrder(),","));
                                //System.err.println("Rotate Series");
                                attributesClusteringTree.rotateSelectedSubTree();
                                //System.err.println("New order: "+Basic.toString(seriesClusteringTree.getLabelOrder(),","));
                                //updateSeriesJList();
                                getJPanel().repaint();
                            }
                        }
                    });
                    action.setEnabled(classesClusteringTree.hasSelectedSubTree() != attributesClusteringTree.hasSelectedSubTree());
                    menu.add(action);
                }
            }
        };
    }

    @Override
    public void writeData(Writer w) throws IOException {
        final int numberOfAttributes = (attributeNames != null ? attributeNames.length : 0);
        w.write("AttributeCorrelationPlot");
        for (String className : classNames) {
            w.write("\t" + className);
        }
        w.write("\n");

        for (int a = 0; a < numberOfAttributes; a++) {
            w.write(attributeNames[a]);
            for (int c = 0; c < classNames.length; c++) {
                final double correlationCoefficient = dataMatrix[c][a];
                w.write(String.format("\t%.4g", correlationCoefficient));
            }
            w.write("\n");
        }
    }

    @Override
    public boolean canCluster(ClusteringTree.TYPE type) {
        return (type == null || type == ClusteringTree.TYPE.ATTRIBUTES || type == ClusteringTree.TYPE.CLASSES);
    }

    public boolean isDoClustering() {
        return super.isDoClustering();
    }
}