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
import megan.util.CallBack;
import megan.util.PopupChoice;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.concurrent.Future;

/**
 * draws a correlation plot
 * Daniel Huson, 11.2015
 */
public class CorrelationPlotDrawer extends BarChartDrawer implements IChartDrawer {
    public enum MODE {
        Beans, Circles, Squares, Numbers, Colors;

        public static MODE valueOfIgnoreCase(String label) {
            for (MODE mode : MODE.values()) {
                if (mode.toString().equalsIgnoreCase(label))
                    return mode;
            }
            return null;
        }
    }

    public static final String NAME = "CorrelationPlot";

    protected float[][] dataMatrix = null;

    protected boolean inUpdateCoordinates = true;
    protected String[] classNames = null;
    private final ArrayList<String> previousSamples = new ArrayList<>();
    private final ArrayList<String> previousClasses = new ArrayList<>();
    private Future future; // used in recompute

    private MODE mode;

    /**
     * constructor
     */
    public CorrelationPlotDrawer() {
        setSupportedScalingTypes(ChartViewer.ScalingType.LINEAR);
        mode = MODE.valueOfIgnoreCase(ProgramProperties.get("CorrelationPlotMode", MODE.Beans.toString()));
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

        int numberOfClasses = getChartData().getNumberOfClasses();

        double xStep = (x1 - x0) / (double) numberOfClasses;
        double yStep = (y0 - y1) / (double) numberOfClasses;

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

                int c = getChartData().getNumberOfClasses() - 1;
                for (int j = 0; j < classNames.length; j++) {
                    final String classNameY = classNames[j];
                    final double correlationCoefficient = dataMatrix[i][j];
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
    protected void drawCell(Graphics2D gc, double[] boundingBox, double correlationCoefficent) {
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
                double width = boundingBox[2];
                double height = boundingBox[3];
                int x = (int) Math.round(centerX - width / 2.0); // left
                int y = (int) Math.round(centerY - height / 2.0); // top
                gc.setColor(color);
                gc.fillRect(x, y, (int) Math.round(width), (int) Math.round(height));
                gc.setColor(color.darker());
                gc.drawRect(x, y, (int) Math.round(width), (int) Math.round(height));
                break;
            }
            case Numbers: {
                gc.setFont(getFont(ChartViewer.FontKeys.DrawFont.toString()));
                String label = String.format("%.3f", correlationCoefficent);
                Dimension labelSize = Basic.getStringSize(gc, label, gc.getFont()).getSize();
                int x = (int) Math.round(centerX - labelSize.width / 2.0); // left
                int y = (int) Math.round(centerY); // top
                gc.setColor(color.darker());
                gc.drawString(label, x, y);
            }
        }
    }

    protected void drawScaleBar(Graphics2D gc, final int x, final int width, final int y, final int height) {
        int x0 = x + Math.max(10, width - 25);

        int xLabel = x0 + 15;
        int boxWidth = 10;
        int boxHeight = Math.min(150, height - 15);
        int step = boxHeight / 10;

        int y0 = y + 15;
        for (int i = 0; i <= boxHeight; i++) {
            float p = 1f - (float) i / (float) boxHeight; // is between 1 and 0
            final Color color = getChartColors().getHeatMapTable().getColor((int) Math.round(1000 * p), 1000);
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
     * draw heat map with colors representing series
     *
     * @param gc
     */
    public void drawChartTransposed(Graphics2D gc) {
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
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));

        final boolean doDraw = (size == null);
        Rectangle bbox = null;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        int longest = 0;
        for (String className : getChartData().getClassNames()) {
            longest = Math.max(longest, Basic.getStringSize(gc, className, gc.getFont()).getSize().width);
        }
        int right = Math.max(leftMargin, longest + 5);

        if (doDraw)
            gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));

        int numberOfClasses = getChartData().getNumberOfClasses();
        double yStep = (y0 - y1) / (double) numberOfClasses;
        int c = getChartData().getNumberOfClasses() - 1;
        for (String className : getChartData().getClassNames()) {
            Dimension labelSize = Basic.getStringSize(gc, className, gc.getFont()).getSize();
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

    public boolean canShowLegend() {
        return false;
    }

    @Override
    public String getChartDrawerName() {
        return NAME;
    }

    @Override
    public boolean canTranspose() {
        return false;
    }

    /**
     * do we need to recompute coordinates?
     *
     * @return true, if coordinates need to be recomputed
     */
    private boolean mustUpdateCoordinates() {
        boolean mustUpdate = (dataMatrix == null);

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
     * force update
     */
    @Override
    public void forceUpdate() {
        dataMatrix = null;
        previousClasses.clear();
        previousSamples.clear();
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

    protected void updateCoordinates() {
        System.err.println("Updating...");
        classNames = getChartData().getClassNames().toArray(new String[getChartData().getNumberOfClasses()]);

        dataMatrix = new float[classNames.length][classNames.length];

        for (int i = 0; i < classNames.length; i++) {
            dataMatrix[i][i] = 1;
            for (int j = i + 1; j < classNames.length; j++) {
                dataMatrix[i][j] = dataMatrix[j][i] = computeCorrelationCoefficent(classNames[i], classNames[j]);
            }
        }
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

    protected double computeXAxisLabelHeight(Graphics2D gc) {
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));
        double theHeight = 2 * gc.getFont().getSize();
        if (classLabelAngle != 0) {
            double sin = Math.abs(Math.sin(classLabelAngle));
            for (String label : getChartData().getClassNames()) {
                if (label.length() > 50)
                    label = label.substring(0, 50) + "...";
                Dimension labelSize = Basic.getStringSize(gc, label, gc.getFont()).getSize();
                theHeight = Math.max(theHeight, gc.getFont().getSize() + sin * labelSize.width);
            }
        }
        return theHeight;
    }

    public MODE getMode() {
        return mode;
    }

    public void setMode(MODE mode) {
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
        return new IPopupMenuModifier() {
            @Override
            public void apply(JPopupMenu menu, final CommandManager commandManager) {
                menu.addSeparator();
                CorrelationPlotDrawer.MODE mode = CorrelationPlotDrawer.MODE.valueOfIgnoreCase(ProgramProperties.get("CorrelationPlotMode", CorrelationPlotDrawer.MODE.Beans.toString()));
                final CallBack<MODE> callBack = new CallBack<CorrelationPlotDrawer.MODE>() {
                    public void call(CorrelationPlotDrawer.MODE choice) {
                        setMode(choice);
                        ProgramProperties.put("CorrelationPlotMode", choice.toString());
                        getJPanel().repaint();
                    }
                };
                PopupChoice.addToJMenu(menu, CorrelationPlotDrawer.MODE.values(), mode, callBack);
            }
        };
    }

    @Override
    public void writeData(Writer w) throws IOException {
        w.write("CorrelationPlot");
        for (String className : classNames) {
            w.write("\t" + className);
        }
        w.write("\n");

        for (int a = 0; a < classNames.length; a++) {
            w.write(classNames[a]);
            for (int c = 0; c < classNames.length; c++) {
                final double correlationCoefficient = dataMatrix[c][a];
                w.write(String.format("\t%.4g", correlationCoefficient));
            }
            w.write("\n");
        }
    }
}