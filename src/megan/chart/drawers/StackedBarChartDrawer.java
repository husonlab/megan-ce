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
import java.awt.geom.Rectangle2D;

/**
 * draws a stacked bar chart
 * Daniel Huson, 5.2012
 */
public class StackedBarChartDrawer extends BarChartDrawer implements IChartDrawer {
    private static final String NAME = "StackedBarChart";

    /**
     * constructor
     */
    public StackedBarChartDrawer() {
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
            default:
                topY = 1.1 * getMaxValue();
        }

        double yFactor = (y0 - y1) / topY;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        if (x0 >= x1)
            return;

        int numberOfDataSets = getChartData().getNumberOfSeries();
        double xStep = (x1 - x0) / (2 * numberOfDataSets);
        double bigSpace = Math.max(2, Math.min(10, xStep));
        xStep = (x1 - x0 - (isGapBetweenBars() ? bigSpace * numberOfDataSets : 0)) / numberOfDataSets;

        // main drawing loop:
        int d = 0;
        for (String series : getChartData().getSeriesNames()) {
            {
                String label = seriesLabelGetter.getLabel(series);
                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                double xLabel = x0 + (isGapBetweenBars() ? (d + 1) * bigSpace : 0) + (d + 0.5) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 12);
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
            for (String className : getChartData().getClassNames()) {
                double value;
                if (scalingType == ChartViewer.ScalingType.PERCENT) {
                    double total = getChartData().getTotalForSeriesIncludingDisabledAttributes(series);
                    if (total == 0)
                        value = 0;
                    else
                        value = 100 * getChartData().getValueAsDouble(series, className) / total;
                } else
                    value = getChartData().getValueAsDouble(series, className);

                double xBar = x0 + (isGapBetweenBars() ? (d + 1) * bigSpace : 0) + d * xStep;
                double height = value * yFactor;

                Rectangle2D rect = new Rectangle((int) Math.round(xBar),
                        (int) Math.round(currentHeight - height), (int) Math.round(xStep), (int) Math.round(height));
                currentHeight -= height;

                Color color = getChartColors().getClassColor(class2HigherClassMapper.get(className));

                gc.setColor(color);
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{series, className});
                gc.fill(rect);
                if (sgc != null)
                    sgc.clearCurrentItem();
                if (getChartData().getChartSelection().isSelected(series, className)) {
                    gc.setStroke(HEAVY_STROKE);
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    gc.draw(rect);
                    gc.setStroke(NORMAL_STROKE);
                } else {
                    gc.setColor(color.darker());
                    gc.draw(rect);
                }
            }
            d++;
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

        double topY;
        final double[] percentFactor;
        if (scalingType == ChartViewer.ScalingType.PERCENT) {
            final String[] seriesIncludingDisabled = getChartData().getSeriesNamesIncludingDisabled();
            percentFactor = computePercentFactorPerSampleForTransposedChart((DefaultChartData) getChartData(), seriesIncludingDisabled);
            topY = computeMaxClassValueUsingPercentFactorPerSeries((DefaultChartData) getChartData(), seriesIncludingDisabled, percentFactor);
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
        if (numberOfClasses == 0)
            return;
        double xStep = (x1 - x0) / (2 * numberOfClasses);
        final double bigSpace = Math.max(2, Math.min(10, xStep));
        xStep = (x1 - x0 - (isGapBetweenBars() ? bigSpace * numberOfClasses : 0)) / numberOfClasses;

        // main drawing loop:
        int c = 0;
        for (String className : getChartData().getClassNames()) {
            double currentHeight = y0;
            {
                final Dimension labelSize = BasicSwing.getStringSize(gc, className, gc.getFont()).getSize();
                final double xLabel = x0 + (isGapBetweenBars() ? (c + 1) * bigSpace : 0) + (c + 0.5) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 12);
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

            for (int i = 0; i < series.length; i++) {
                String seriesName = series[i];

                double value = getChartData().getValueAsDouble(seriesName, className);
                if (scalingType == ChartViewer.ScalingType.PERCENT && percentFactor != null) {
                    value *= percentFactor[i];
                }

                final double xBar = x0 + (isGapBetweenBars() ? (c + 1) * bigSpace : 0) + c * xStep;
                final double height = value * yFactor;

                final Rectangle2D rect = new Rectangle((int) Math.round(xBar),
                        (int) Math.round(currentHeight - height), (int) Math.round(xStep), (int) Math.round(height));

                currentHeight -= height;
                final Color color = getChartColors().getSampleColor(seriesName);
                gc.setColor(color);
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{seriesName, className});
                gc.fill(rect);
                if (sgc != null)
                    sgc.clearCurrentItem();
                if (getChartData().getChartSelection().isSelected(seriesName, className)) {
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    gc.setStroke(HEAVY_STROKE);
                    gc.draw(rect);
                    gc.setStroke(NORMAL_STROKE);
                } else {
                    gc.setColor(color.darker());
                    gc.draw(rect);
                }
            }
            c++;
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
