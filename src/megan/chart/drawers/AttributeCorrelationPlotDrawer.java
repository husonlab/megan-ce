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
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;

/**
 * draws a correlation plot
 * Daniel Huson, 11.2015
 */
public class AttributeCorrelationPlotDrawer extends CorrelationPlotDrawer implements IChartDrawer {

    public static final String NAME = "AttributeCorrelationPlot";

    private String[] attributeNames;


    /**
     * constructor
     */
    public AttributeCorrelationPlotDrawer() {
        super();
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
        double xStep = (x1 - x0) / (double) numberOfClasses;
        double yStep = (y0 - y1) / (double) numberOfAttributes;


        // main drawing loop:
        if (classNames != null) {
            int d = 0;
            for (int i = 0; i < classNames.length; i++) {
                final String classNameX = classNames[i];
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
                for (int j = 0; j < numberOfAttributes; j++) {
                    final String attributeNameY = attributeNames[j];
                    final double correlationCoefficient = correlationDataMatrix[i][j];
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
                    boolean isSelected = getChartData().getChartSelection().isSelectedClass(classNameX) || getChartData().getChartSelection().isSelectedSeries(attributeNameY);
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
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));

        final boolean doDraw = (size == null);
        Rectangle bbox = null;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;


        if (attributeNames == null)
            return;
        final int numberOfAttributes = attributeNames.length;

        int longest = 0;
        for (String attributeName : attributeNames) {
            longest = Math.max(longest, Basic.getStringSize(gc, attributeName, gc.getFont()).getSize().width);
        }
        int right = Math.max(leftMargin, longest + 5);

        if (doDraw)
            gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));

        double yStep = (y0 - y1) / (double) numberOfAttributes;
        int c = numberOfAttributes - 1;
        for (String attributeName : attributeNames) {
            Dimension labelSize = Basic.getStringSize(gc, attributeName, gc.getFont()).getSize();
            int x = right - labelSize.width - 4;
            int y = (int) Math.round(y0 - (c + 0.5) * yStep);
            if (doDraw) {
                if (getChartData().getChartSelection().isSelectedSeries(attributeName)) {
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
                sgc.setCurrentItem(new String[]{attributeName, null});
                drawRect(gc, x, y, labelSize.width, labelSize.height, 0);
                sgc.clearCurrentItem();
            }
            c--;
        }
        if (size != null && bbox != null) {
            size.setSize(bbox.width + 3, bbox.height);
        }
    }

    protected void updateCoordinates() {
        System.err.println("Updating...");
        classNames = getChartData().getClassNames().toArray(new String[getChartData().getNumberOfClasses()]);

        final Collection<String> list = getViewer().getDir().getDocument().getSampleAttributeTable().getNumericalAttributes();
        attributeNames = list.toArray(new String[list.size()]);

        correlationDataMatrix = new float[classNames.length][attributeNames.length];

        for (int i = 0; i < classNames.length; i++) {
            for (int j = 0; j < attributeNames.length; j++) {
                try {
                    correlationDataMatrix[i][j] = computeCorrelationCoefficent(classNames[i], attributeNames[j]);
                } catch (Exception ex) {
                    Basic.caught(ex);
                }
            }
        }
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
        };
    }
}