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
import megan.chart.data.DefaultChartData;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Objects;

/**
 * draws a line chart
 * Daniel Huson, 5.2012
 */
public class LineChartDrawer extends BarChartDrawer implements IChartDrawer {
    private static final String NAME = "LineChart";

    /**
     * constructor
     */
    public LineChartDrawer() {
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
        if (scalingType == ChartViewer.ScalingType.PERCENT)
            topY = 101;
        else if (scalingType == ChartViewer.ScalingType.LOG) {
            topY = computeMaxYAxisValueLogScale(getMaxValue());
        } else if (scalingType == ChartViewer.ScalingType.SQRT) {
            topY = Math.sqrt(getMaxValue());
        } else
            topY = 1.1 * getMaxValue();

        double yFactor = (y0 - y1) / topY;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        if (x0 >= x1)
            return;

        int numberOfDataSets = getChartData().getNumberOfSeries();
        double xStep = (x1 - x0) / (2 * numberOfDataSets);
        double bigSpace = Math.max(2, Math.min(10, xStep));
        xStep = (x1 - x0 - bigSpace * numberOfDataSets) / numberOfDataSets;

        Point[] previousPoint = new Point[getChartData().getNumberOfClasses()];

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
                    if (value > 0)
                        value = Math.log10(value);
                } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                    value = getChartData().getValueAsDouble(series, className);
                    if (value > 0)
                        value = Math.sqrt(value);
                } else
                    value = getChartData().getValueAsDouble(series, className);

                double xBar = x0 + bigSpace + d * bigSpace + d * xStep;

                double height = value * yFactor;

                Point aPt = new Point((int) Math.round(xBar + xStep / 2.0), (int) Math.round(y0 - height));
                boolean isSelected = getChartData().getChartSelection().isSelected(null, className);
                Color color = getChartColors().getClassColor(class2HigherClassMapper.get(className));

                if (isSelected) {
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    gc.setStroke(HEAVY_STROKE);
                } else
                    gc.setColor(color);

                Point bPt = previousPoint[c];
                if (bPt != null) {
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{series, className});
                    gc.drawLine(bPt.x, bPt.y, aPt.x, aPt.y);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                }
                previousPoint[c] = aPt;

                if (!isSelected) {
                    isSelected = getChartData().getChartSelection().isSelected(series, className);
                    if (isSelected) {
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        gc.setStroke(HEAVY_STROKE);
                    }
                }

                if (!isSelected) {
                    gc.setColor(color.darker());
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{series, className});
                    gc.drawOval(aPt.x - 1, aPt.y - 1, 2, 2);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                } else {
                    gc.drawOval(aPt.x - 2, aPt.y - 2, 4, 4);
                    gc.setStroke(NORMAL_STROKE);
                }

                c++;
                if (showValues || isSelected) {
                    String label = "" + (int) getChartData().getValueAsDouble(series, className);
                    valuesList.add(new DrawableValue(label, aPt.x, aPt.y - 3, isSelected));
                }
            }
            d++;
        }
        if (valuesList.size() > 0) {
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            DrawableValue.drawValues(gc, valuesList, true, false);
            valuesList.clear();
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

        final int y0 = getHeight() - bottomMargin;
        final int y1 = topMargin;

        final String[] series = getChartData().getSeriesNames().toArray(new String[getChartData().getNumberOfSeries()]);

        final double topY;
        final double[] percentFactor;
        if (scalingType == ChartViewer.ScalingType.PERCENT) {
            percentFactor = computePercentFactorPerSampleForTransposedChart((DefaultChartData) getChartData(), series);
            topY = computeMaxClassValueUsingPercentFactorPerSeries((DefaultChartData) getChartData(), series, percentFactor);
        } else if (scalingType == ChartViewer.ScalingType.LOG) {
            topY = computeMaxYAxisValueLogScale(getMaxValue());
            percentFactor = null;
        } else if (scalingType == ChartViewer.ScalingType.SQRT) {
            topY = Math.sqrt(getMaxValue());
            percentFactor = null;
        } else {
            topY = 1.1 * getMaxValue();
            percentFactor = null;
        }

        final double yFactor = (y0 - y1) / topY;

        final int x0 = leftMargin;
        final int x1 = getWidth() - rightMargin;
        if (x0 >= x1)
            return;

        final int numberOfClasses = getChartData().getNumberOfClasses();
        double xStep = (x1 - x0) / (2 * numberOfClasses);
        final double bigSpace = Math.max(2, Math.min(10, xStep));
        xStep = (x1 - x0 - bigSpace * numberOfClasses) / numberOfClasses;

        Point[] previousPoint = new Point[getChartData().getNumberOfSeries()];
        // main drawing loop:
        int c = 0;
        for (String className : getChartData().getClassNames()) {
            if (showXAxis) {
                final double xLabel = x0 + bigSpace + c * bigSpace + (c + 0.5) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 10);
                final Dimension labelSize = BasicSwing.getStringSize(gc, className, gc.getFont()).getSize();
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
            for (int i = 0; i < series.length; i++) {
                final String seriesName = series[i];

                double value = getChartData().getValueAsDouble(seriesName, className);
                switch (scalingType) { // modify if not linear scale:
                    case PERCENT: {
                        value *= Objects.requireNonNull(percentFactor)[i];
                        break;
                    }
                    case LOG: {
                        if (value == 1)
                            value = Math.log10(2) / 2;
                        else if (value > 0)
                            value = Math.log10(value);
                        break;
                    }
                    case SQRT: {
                        if (value > 0)
                            value = Math.sqrt(value);
                        break;
                    }
                }

                final double xBar = x0 + bigSpace + c * bigSpace + c * xStep;

                final double height = value * yFactor;

                final Point aPt = new Point((int) Math.round(xBar + xStep / 2.0), (int) Math.round(y0 - height));
                boolean isSelected = getChartData().getChartSelection().isSelected(seriesName, null);
                final Color color = getChartColors().getSampleColor(seriesName);

                if (isSelected) {
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    gc.setStroke(HEAVY_STROKE);
                } else
                    gc.setColor(color);

                Point bPt = previousPoint[d];
                if (bPt != null) {
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{seriesName, className});
                    gc.drawLine(bPt.x, bPt.y, aPt.x, aPt.y);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                }
                previousPoint[d] = aPt;

                if (!isSelected) {
                    isSelected = getChartData().getChartSelection().isSelected(seriesName, className);
                    if (isSelected) {
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        gc.setStroke(HEAVY_STROKE);
                    }
                }

                if (!isSelected) {
                    gc.setColor(color.darker());
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{seriesName, className});
                    gc.drawOval(aPt.x - 1, aPt.y - 1, 2, 2);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                } else {
                    gc.drawOval(aPt.x - 2, aPt.y - 2, 4, 4);
                    gc.setStroke(NORMAL_STROKE);
                }

                d++;
                if (showValues || isSelected) {
                    String label = "" + (int) getChartData().getValueAsDouble(seriesName, className);
                    valuesList.add(new DrawableValue(label, aPt.x, aPt.y - 3, isSelected));
                }
            }
            c++;
        }
        if (valuesList.size() > 0) {
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            DrawableValue.drawValues(gc, valuesList, true, false);
            valuesList.clear();
        }
    }

    public boolean canShowValues() {
        return true;
    }

    @Override
    public String getChartDrawerName() {
        return NAME;
    }
}
