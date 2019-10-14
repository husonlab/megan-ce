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
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.Triplet;
import megan.chart.IChartDrawer;
import megan.chart.data.DefaultChartData;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.Objects;

/**
 * draws a stacked line chart
 * Daniel Huson, 5.2012
 */
public class StackedLineChartDrawer extends BarChartDrawer implements IChartDrawer {
    private static final String NAME = "StackedLineChart";

    /**
     * constructor
     */
    public StackedLineChartDrawer() {
        transposedHeightsAdditive = true;
        setSupportedScalingTypes(ChartViewer.ScalingType.LINEAR, ChartViewer.ScalingType.PERCENT);
    }

    /**
     * draw bars with colors representing classes
     *
     * @param gc
     */
    public void drawChart(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);

        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));

        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        double topY;
        switch (scalingType) {
            case PERCENT:
                topY = 101;
                break;
            case LOG:
                topY = computeMaxYAxisValueLogScale(getMaxValue());
                break;
            case SQRT:
                topY = Math.sqrt(getMaxValue());
                break;
            default:
                topY = 1.1 * getMaxValue();
        }

        double yFactor = (y0 - y1) / topY;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        if (x0 >= x1)
            return;

        int numberOfSeries = getChartData().getNumberOfSeries();
        double xStep = (x1 - x0) / (2 * numberOfSeries);
        double bigSpace = Math.max(2, Math.min(10, xStep));
        xStep = (x1 - x0 - bigSpace * numberOfSeries) / numberOfSeries;

        Point[] previousPoint = new Point[getChartData().getNumberOfClasses()];

        java.util.List<Triplet<String, String, int[]>> list = new LinkedList<>();
        // main drawing loop:

        int d = 0;
        for (String series : getChartData().getSeriesNames()) {
            if (showXAxis) {
                double xLabel = x0 + bigSpace + d * bigSpace + (d + 0.5) * xStep;
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
                gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.BLACK));
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{series, null});
                drawString(gc, label, apt.getX(), apt.getY(), classLabelAngle);
                if (sgc != null)
                    sgc.clearCurrentItem();
            }

            double currentHeight = y0;
            double currentValueForLog = 0;
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
                    if (value >= 1) {
                        value = Math.log10(value + currentValueForLog);
                        if (currentValueForLog >= 1)
                            value -= Math.log10(currentValueForLog);
                    }
                    currentValueForLog += getChartData().getValueAsDouble(series, className);
                } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                    value = getChartData().getValueAsDouble(series, className);
                    if (value >= 1) {
                        value = Math.sqrt(value + currentValueForLog);
                        if (currentValueForLog >= 1)
                            value -= Math.sqrt(currentValueForLog);
                    }
                    currentValueForLog += getChartData().getValueAsDouble(series, className);
                } else
                    value = getChartData().getValueAsDouble(series, className);

                double xBar = x0 + bigSpace + d * bigSpace + d * xStep;

                double height = value * yFactor;

                Point aPt = new Point((int) Math.round(xBar + xStep / 2.0), (int) Math.round(currentHeight - height));
                currentHeight -= height;

                Point bPt = previousPoint[c];

                if (bPt == null && numberOfSeries == 1)
                    bPt = new Point(aPt.x - 2, aPt.y);

                if (bPt != null) {
                    Triplet<String, String, int[]> triplet = new Triplet<>(series, className, new int[]{bPt.x, bPt.y, aPt.x, aPt.y});
                    list.add(triplet);
                }
                previousPoint[c] = aPt;
                c++;
            }
            d++;
        }

        // need to draw in reverse order to get correct ordering of polygons
        list = Basic.reverseList(list);
        for (Triplet<String, String, int[]> pair : list) {
            String series = pair.get1();
            String className = pair.get2();
            int[] coords = pair.get3();
            Color color = getChartColors().getClassColor(class2HigherClassMapper.get(className));
            gc.setColor(color);
            int[] xs = new int[]{coords[0], coords[2], coords[2], coords[0]};
            int[] ys = new int[]{coords[1], coords[3], y0, y0};
            gc.fillPolygon(xs, ys, 4);

            gc.setColor(color.darker());
            if (sgc != null)
                sgc.setCurrentItem(new String[]{series, className});
            gc.drawLine(coords[0], coords[1], coords[2], coords[3]);
            if (sgc != null)
                sgc.clearCurrentItem();
        }

        Triplet<String, String, int[]> current = null;
        for (Triplet<String, String, int[]> next : list) {
            if (current != null) {
                String className = current.get2();
                String series = current.get1();
                int[] coords = current.get3();
                if (getChartData().getChartSelection().isSelected(null, className)) {
                    gc.setStroke(HEAVY_STROKE);
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    gc.drawLine(coords[0], coords[1], coords[2], coords[3]);
                    coords = next.get3();
                    gc.drawLine(coords[0], coords[1], coords[2], coords[3]);
                    gc.setStroke(NORMAL_STROKE);
                } else if (getChartData().getChartSelection().isSelected(series, null)) {
                    gc.setStroke(HEAVY_STROKE);
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    gc.drawOval(coords[2] - 1, coords[3] - 1, 2, 2);
                    gc.setStroke(NORMAL_STROKE);
                }
            }
            current = next;
        }
        if (current != null) {
            String className = current.get2();
            String series = current.get1();
            int[] coords = current.get3();
            if (getChartData().getChartSelection().isSelected(null, className)) {
                gc.setStroke(HEAVY_STROKE);
                gc.setColor(ProgramProperties.SELECTION_COLOR);
                gc.drawLine(coords[0], coords[1], coords[2], coords[3]);
                gc.setStroke(NORMAL_STROKE);
            } else if (getChartData().getChartSelection().isSelected(series, null)) {
                gc.setStroke(HEAVY_STROKE);
                gc.setColor(ProgramProperties.SELECTION_COLOR);
                gc.drawOval(coords[2] - 1, coords[3] - 1, 2, 2);
                gc.setStroke(NORMAL_STROKE);
            }
        }
    }

    /**
     * draw bars in which colors are by dataset
     *
     * @param gc
     */
    public void drawChartTransposed(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);

        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));

        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        final String[] series = getChartData().getSeriesNames().toArray(new String[getChartData().getNumberOfSeries()]);

        final double topY;
        final double[] percentFactor;
        switch (scalingType) {
            case PERCENT: {
                final String[] seriesIncludingDisabled = getChartData().getSeriesNamesIncludingDisabled();
                percentFactor = computePercentFactorPerSampleForTransposedChart((DefaultChartData) getChartData(), seriesIncludingDisabled);
                topY = computeMaxClassValueUsingPercentFactorPerSeries((DefaultChartData) getChartData(), seriesIncludingDisabled, percentFactor);
                break;
            }
            case LOG: {
                topY = computeMaxYAxisValueLogScale(getMaxValue());
                percentFactor = null;
                break;
            }
            case SQRT: {
                topY = Math.sqrt(getMaxValue());
                percentFactor = null;
                break;
            }
            default:
            case LINEAR: {
                topY = 1.1 * getMaxValue();
                percentFactor = null;
            }
        }

        final double yFactor = (y0 - y1) / topY;

        final int x0 = leftMargin;
        final int x1 = getWidth() - rightMargin;
        if (x0 >= x1)
            return;

        final int numberOfClasses = getChartData().getNumberOfClasses();
        double xStep = (x1 - x0) / (2 * Math.max(1, numberOfClasses));
        final double bigSpace = Math.max(2, Math.min(10, xStep));
        xStep = (x1 - x0 - bigSpace * numberOfClasses) / numberOfClasses;
        Point[] previousPoint = new Point[getChartData().getNumberOfSeries()];

        java.util.List<Triplet<String, String, int[]>> list = new LinkedList<>();
        // main drawing loop:
        int c = 0;
        for (String className : getChartData().getClassNames()) {
            if (showXAxis) {
                double xLabel = x0 + bigSpace + c * bigSpace + (c + 0.5) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 10);
                Dimension labelSize = BasicSwing.getStringSize(gc, className, gc.getFont()).getSize();
                if (classLabelAngle == 0) {
                    apt.setLocation(apt.getX() - labelSize.getWidth() / 2, apt.getY());
                } else if (classLabelAngle > Math.PI / 2) {
                    apt = Geometry.translateByAngle(apt, classLabelAngle, -labelSize.width);
                }
                if (getChartData().getChartSelection().isSelected(null, className)) {
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    fillAndDrawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, classLabelAngle, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                }
                gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.BLACK));
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{null, className});
                drawString(gc, className, apt.getX(), apt.getY(), classLabelAngle);
                if (sgc != null)
                    sgc.clearCurrentItem();
            }

            double currentHeight = y0;
            double currentValueForLog = 0;

            int d = 0;
            for (int i = 0; i < series.length; i++) {
                final String seriesName = series[i];

                double value = getChartData().getValueAsDouble(seriesName, className);
                switch (scalingType) { // modify if not linear scale:
                    case PERCENT: {
                        value *= Objects.requireNonNull(percentFactor)[i];
                        break;
                    }
                    case LOG: {
                        if (value >= 1) {
                            if (currentValueForLog <= 1) {
                                value = Math.log10(value);
                            } else {
                                value = Math.log10(value + currentValueForLog) - Math.log10(currentValueForLog);
                            }
                            currentValueForLog += getChartData().getValueAsDouble(seriesName, className);
                        } else // no change in height
                            value = 0;
                        break;
                    }
                    case SQRT: {
                        if (value >= 1) {
                            if (currentValueForLog <= 1) {
                                value = Math.sqrt(value);
                            } else {
                                value = Math.sqrt(value + currentValueForLog) - Math.sqrt(currentValueForLog);
                            }
                            currentValueForLog += getChartData().getValueAsDouble(seriesName, className);
                        } else // no change in height
                            value = 0;
                        break;
                    }
                }

                final double xBar = x0 + bigSpace + c * bigSpace + c * xStep;
                final double height = value * yFactor;

                final Point aPt = new Point((int) Math.round(xBar + xStep / 2.0), (int) Math.round(currentHeight - height));

                currentHeight -= height;

                Point bPt = previousPoint[d];

                if (bPt == null && numberOfClasses == 1)
                    bPt = new Point(aPt.x - 2, aPt.y);

                if (bPt != null) {
                    Triplet<String, String, int[]> triplet = new Triplet<>(seriesName, className, new int[]{bPt.x, bPt.y, aPt.x, aPt.y});
                    list.add(triplet);
                }
                previousPoint[d] = aPt;
                d++;
            }
            c++;
        }

        // need to draw in reverse order to get correct ordering of polygons
        list = Basic.reverseList(list);
        for (Triplet<String, String, int[]> triplet : list) {
            String seriesName = triplet.get1();
            String className = triplet.get2();
            int[] coords = triplet.get3();
            Color color = getChartColors().getSampleColor(seriesName);
            gc.setColor(color);
            int[] xs = new int[]{coords[0], coords[2], coords[2], coords[0]};
            int[] ys = new int[]{coords[1], coords[3], y0, y0};
            gc.fillPolygon(xs, ys, 4);
            gc.setColor(color.darker());
            if (sgc != null)
                sgc.setCurrentItem(new String[]{seriesName, className});
            gc.drawLine(coords[0], coords[1], coords[2], coords[3]);
            if (sgc != null)
                sgc.clearCurrentItem();
        }

        Triplet<String, String, int[]> current = null;
        for (Triplet<String, String, int[]> next : list) {
            if (current != null) {
                String seriesName = current.get1();
                String className = current.get2();
                int[] coords = current.get3();
                if (getChartData().getChartSelection().isSelected(seriesName, null)) {
                    gc.setStroke(HEAVY_STROKE);
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    gc.drawLine(coords[0], coords[1], coords[2], coords[3]);
                    coords = next.get3();
                    gc.drawLine(coords[0], coords[1], coords[2], coords[3]);
                    gc.setStroke(NORMAL_STROKE);
                } else if (getChartData().getChartSelection().isSelected(null, className)) {
                    gc.setStroke(HEAVY_STROKE);
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    gc.drawOval(coords[2] - 1, coords[3] - 1, 2, 2);
                    gc.setStroke(NORMAL_STROKE);
                }
            }
            current = next;
        }
        if (current != null) {
            String seriesName = current.get1();
            String className = current.get2();
            int[] coords = current.get3();
            if (getChartData().getChartSelection().isSelected(seriesName, null)) {
                gc.setStroke(HEAVY_STROKE);
                gc.setColor(ProgramProperties.SELECTION_COLOR);
                gc.drawLine(coords[0], coords[1], coords[2], coords[3]);
                gc.setStroke(NORMAL_STROKE);
            } else if (getChartData().getChartSelection().isSelected(null, className)) {
                gc.setStroke(HEAVY_STROKE);
                gc.setColor(ProgramProperties.SELECTION_COLOR);
                gc.drawOval(coords[2] - 1, coords[3] - 1, 2, 2);
                gc.setStroke(NORMAL_STROKE);
            }
        }
    }

    /**
     * gets the max value used in the plot
     *
     * @return max value
     */
    @Override
    protected double getMaxValue() {
        if (!isTranspose())
            return getChartData().getMaxTotalSeries();
        else {
            double maxValue = 0;
            for (String className : getChartData().getClassNames()) {
                double sum = 0;
                for (String series : getChartData().getSeriesNames()) {
                    sum += getChartData().getValueAsDouble(series, className);
                }
                maxValue = Math.max(sum, maxValue);
            }
            return maxValue;
        }
    }

    public boolean canShowValues() {
        return false;
    }

    @Override
    public String getChartDrawerName() {
        return NAME;
    }
}
