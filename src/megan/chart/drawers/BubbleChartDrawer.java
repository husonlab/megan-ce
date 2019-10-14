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
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * draws a bubble chart
 * Daniel Huson, 6.2012
 */
public class BubbleChartDrawer extends BarChartDrawer implements IChartDrawer {
    private static final String NAME = "BubbleChart";

    int maxRadius = 30;

    /**
     * constructor
     */
    public BubbleChartDrawer() {
    }

    /**
     * draw bubbles with colors representing classes
     *
     * @param gc
     */
    public void drawChart(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);

        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));


        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;

        Rectangle scaleBox = new Rectangle(x1 - 2 * maxRadius, y1 + 40, 2 * maxRadius, 2 * maxRadius + 12);
        x1 -= (scaleBox.width + maxRadius);

        if (x0 >= x1)
            return;
        if (sgc == null)
            drawScaleLegend(gc, scaleBox);

        int numberOfSeries = getChartData().getNumberOfSeries();
        int numberOfClasses = getChartData().getNumberOfClasses();

        double xStep = (x1 - x0) / numberOfSeries;
        double yStep = (y0 - y1) / (0.5 + numberOfClasses);

        double maxValue = getChartData().getRange().get2().doubleValue();
        if (scalingType == ChartViewer.ScalingType.LOG && maxValue > 0)
            maxValue = Math.log(maxValue);
        else if (scalingType == ChartViewer.ScalingType.SQRT && maxValue > 0)
            maxValue = Math.sqrt(maxValue);
        else if (scalingType == ChartViewer.ScalingType.PERCENT)
            maxValue = 100;

        double factor = (maxValue > 0 ? getMaxRadius() / maxValue : 1);

        // main drawing loop:
        int d = 0;
        for (String series : getChartData().getSeriesNames()) {
            if (isShowXAxis()) {
                double xLabel = x0 + (d + 0.5) * xStep;
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
                    if (value > 1)
                        value = Math.log(value);
                } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                    value = getChartData().getValueAsDouble(series, className);
                    if (value > 0)
                        value = Math.sqrt(value);
                } else
                    value = getChartData().getValueAsDouble(series, className);
                value *= factor;

                int[] oval = new int[]{(int) ((x0 + (d + 0.5) * xStep) - value), (int) ((y0 - (c + 1) * yStep) - value), (int) (2 * value), (int) (2 * value)};
                Color color = getChartColors().getClassColor(class2HigherClassMapper.get(className), 150);
                gc.setColor(color);
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{series, className});

                gc.fillOval(oval[0], oval[1], oval[2], oval[3]);
                if (sgc != null)
                    sgc.clearCurrentItem();

                boolean isSelected = getChartData().getChartSelection().isSelected(series, className);
                if (isSelected) {
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    if (oval[2] <= 1) {
                        oval[0] -= 1;
                        oval[1] -= 1;
                        oval[2] += 2;
                        oval[3] += 2;
                    }
                    gc.setStroke(HEAVY_STROKE);
                    gc.drawOval(oval[0], oval[1], oval[2], oval[3]);
                    gc.setStroke(NORMAL_STROKE);
                } else {
                    gc.setColor(color.darker());
                    gc.drawOval(oval[0], oval[1], oval[2], oval[3]);
                }
                c++;
                if (showValues || isSelected) {
                    String label = "" + (int) getChartData().getValueAsDouble(series, className);
                    valuesList.add(new DrawableValue(label, oval[0] + oval[2] + 2, oval[1] + oval[3] / 2, isSelected));
                }
            }
            d++;
        }
        if (valuesList.size() > 0) {
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            DrawableValue.drawValues(gc, valuesList, false, true);
            valuesList.clear();
        }
    }


    /**
     * draw bubbles in which colors are by dataset
     *
     * @param gc
     */
    public void drawChartTransposed(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));

        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        if (x0 >= x1)
            return;

        int numberOfClasses = getChartData().getNumberOfClasses();
        int numberOfSeries = getChartData().getNumberOfSeries();

        double xStep = (x1 - x0) / numberOfClasses;
        double yStep = (y0 - y1) / (0.5 + numberOfSeries);

        double maxValue = getChartData().getRange().get2().doubleValue();
        if (scalingType == ChartViewer.ScalingType.LOG && maxValue > 0)
            maxValue = Math.log(maxValue);
        else if (scalingType == ChartViewer.ScalingType.SQRT && maxValue > 0)
            maxValue = Math.sqrt(maxValue);
        else if (scalingType == ChartViewer.ScalingType.PERCENT)
            maxValue = 100;
        double factor = (maxValue > 0 ? getMaxRadius() / maxValue : 1);

        // main drawing loop:
        int c = 0;
        for (String className : getChartData().getClassNames()) {
            if (isShowXAxis()) {

                double xLabel = x0 + (c + 0.5) * xStep;
                Dimension labelSize = BasicSwing.getStringSize(gc, className, gc.getFont()).getSize();
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 10);
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
            for (String series : getChartData().getSeriesNames()) {
                double value;
                if (scalingType == ChartViewer.ScalingType.PERCENT) {
                    double total = getChartData().getTotalForClassIncludingDisabledSeries(className);
                    if (total == 0)
                        value = 0;
                    else
                        value = 100 * getChartData().getValueAsDouble(series, className) / total;
                } else if (scalingType == ChartViewer.ScalingType.LOG) {
                    value = getChartData().getValueAsDouble(series, className);
                    if (value > 1)
                        value = Math.log(value);
                } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                    value = getChartData().getValueAsDouble(series, className);
                    if (value > 1)
                        value = Math.sqrt(value);
                } else
                    value = getChartData().getValueAsDouble(series, className);

                value *= factor;

                int[] oval = new int[]{(int) ((x0 + (c + 0.5) * xStep) - value), (int) ((y0 - (d + 1) * yStep) - value), (int) (2 * value), (int) (2 * value)};
                Color color = getChartColors().getSampleColorWithAlpha(series, 150);
                gc.setColor(color);
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{series, className});

                gc.fillOval(oval[0], oval[1], oval[2], oval[3]);
                if (sgc != null)
                    sgc.clearCurrentItem();
                boolean isSelected = getChartData().getChartSelection().isSelected(series, className);
                if (isSelected) {
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    if (oval[2] <= 1) {
                        oval[0] -= 1;
                        oval[1] -= 1;
                        oval[2] += 2;
                        oval[3] += 2;
                    }
                    gc.setStroke(HEAVY_STROKE);
                    gc.drawOval(oval[0], oval[1], oval[2], oval[3]);
                    gc.setStroke(NORMAL_STROKE);
                } else {
                    gc.setColor(color.darker());
                    gc.drawOval(oval[0], oval[1], oval[2], oval[3]);
                }
                d++;
                if (showValues || isSelected) {
                    String label = "" + (int) getChartData().getValueAsDouble(series, className);
                    valuesList.add(new DrawableValue(label, oval[0] + oval[2] + 2, oval[1] + oval[3] / 2, isSelected));
                }
            }
            c++;
        }
        if (valuesList.size() > 0) {
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            DrawableValue.drawValues(gc, valuesList, false, true);
            valuesList.clear();
        }
    }

    private int getMaxRadius() {
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
    }

    /**
     * draw the y-axis
     *
     * @param gc
     */
    protected void drawYAxis(Graphics2D gc, Dimension size) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));

        boolean doDraw = (size == null);
        Rectangle bbox = null;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        if (!isTranspose()) {
            int longest = 0;
            for (String className : getChartData().getClassNames()) {
                longest = Math.max(longest, BasicSwing.getStringSize(gc, className, gc.getFont()).getSize().width);
            }
            int colorRectTotalWidth = gc.getFont().getSize() + 4;
            int right = Math.max(leftMargin, longest + maxRadius / 2 + 2);

            int numberOfClasses = getChartData().getNumberOfClasses();
            double yStep = (y0 - y1) / (0.5 + numberOfClasses);
            int c = 0;
            for (String className : getChartData().getClassNames()) {
                Dimension labelSize = BasicSwing.getStringSize(gc, className, gc.getFont()).getSize();
                int x = right - maxRadius / 2 - 2 - labelSize.width - colorRectTotalWidth;
                int y = (int) Math.round(y0 - (c + 1) * yStep);
                int height = gc.getFont().getSize();
                if (doDraw) {
                    gc.setColor(getChartColors().getClassColor(class2HigherClassMapper.get(className), 150));
                    gc.fillRect(right - maxRadius / 2 - colorRectTotalWidth, y - height + 4, height - 5, height - 5);
                    if (getChartData().getChartSelection().isSelected(null, className)) {
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        fillAndDrawRect(gc, x, y, labelSize.width, labelSize.height, 0, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                    }
                    gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.DARK_GRAY));
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{null, className});
                    gc.drawString(className, x, y);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                } else {
                    int width = labelSize.width + colorRectTotalWidth;
                    Rectangle rect = new Rectangle(x, y, width + 2, labelSize.height);
                    if (bbox == null)
                        bbox = rect;
                    else
                        bbox.add(rect);
                }
                c++;
            }
        } else // tranposed
        {
            if (x0 >= x1)
                return;
            int longest = 0;
            for (String series : getChartData().getSeriesNames()) {
                String label = seriesLabelGetter.getLabel(series);
                longest = Math.max(longest, BasicSwing.getStringSize(gc, label, gc.getFont()).getSize().width);

            }
            int colorRectTotalWidth = gc.getFont().getSize() + 4;
            int right = Math.max(leftMargin, longest + maxRadius / 2 + 2);

            int numberOfDataSets = getChartData().getNumberOfSeries();
            double yStep = (y0 - y1) / (0.5 + numberOfDataSets);
            int d = 0;
            for (String series : getChartData().getSeriesNames()) {
                String label = seriesLabelGetter.getLabel(series);
                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                int x = right - maxRadius / 2 - 2 - labelSize.width - colorRectTotalWidth;
                int y = (int) Math.round(y0 - (d + 1) * yStep);
                int height = gc.getFont().getSize();
                if (doDraw) {
                    gc.setColor(getChartColors().getSampleColor(series));
                    gc.fillRect(right - maxRadius / 2 - colorRectTotalWidth, y - height + 4, height - 5, height - 5);
                    if (getChartData().getChartSelection().isSelected(series, null)) {
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        fillAndDrawRect(gc, x, y, labelSize.width, labelSize.height, 0, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                    }
                    gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.DARK_GRAY));
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{series, null});
                    gc.drawString(label, x, y);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                } else {
                    int width = labelSize.width + colorRectTotalWidth;
                    Rectangle rect = new Rectangle(x, y, width + 2, labelSize.height);
                    if (bbox == null)
                        bbox = rect;
                    else
                        bbox.add(rect);
                }
                d++;
            }
        }
        if (size != null && bbox != null) {
            size.setSize(bbox.width + maxRadius / 2, bbox.height);
        }
    }

    private void drawScaleLegend(Graphics gc, Rectangle box) {
        final int boxMidX = (int) Math.round(box.getX() + box.getWidth() / 2);
        final int boxBottomY = (int) Math.round(box.getY() + box.getHeight());
        final int roundedMaxValue = replaceAllButFirstDigitByZero((int) getMaxValue());

        int[][] diameterAndNumber = new int[3][2];

        switch (scalingType) {
            case PERCENT: {
                diameterAndNumber[0][0] = 2 * maxRadius;
                diameterAndNumber[0][1] = 100;
                diameterAndNumber[1][0] = maxRadius;
                diameterAndNumber[1][1] = 50;
                diameterAndNumber[2][0] = maxRadius / 5;
                diameterAndNumber[2][1] = 10;
                break;
            }
            case ZSCORE:
                System.err.println("ZSCORE: not legal scaling for BubbleChart");
            case LINEAR: {
                diameterAndNumber[0][0] = 2 * maxRadius;
                diameterAndNumber[0][1] = roundedMaxValue;
                diameterAndNumber[1][0] = maxRadius;
                diameterAndNumber[1][1] = roundedMaxValue / 2;
                diameterAndNumber[2][0] = maxRadius / 5;
                diameterAndNumber[2][1] = roundedMaxValue / 10;
                break;
            }
            case LOG: {
                final int maxLog = (int) Math.log(getMaxValue());
                final int logRoundedMaxValue = (int) Math.log(roundedMaxValue);

                diameterAndNumber[0][0] = 2 * Math.round(logRoundedMaxValue * maxRadius / maxLog);
                diameterAndNumber[0][1] = roundedMaxValue;
                int number = replaceFirstDigitByOne(roundedMaxValue);
                for (int i = 1; i <= 2; i++) {
                    while (Math.abs((2 * Math.log(number) * maxRadius / maxLog) - diameterAndNumber[i - 1][0]) < 12 && number >= 1) {
                        number /= 10;
                    }
                    if (number > 0) {
                        diameterAndNumber[i][0] = 2 * (int) Math.round(Math.log(number) * maxRadius / maxLog);
                        diameterAndNumber[i][1] = number;
                    }
                }
                break;
            }
            case SQRT: {
                final int maxSqrt = (int) Math.sqrt(getMaxValue());
                final int sqrtRoundedMaxValue = (int) Math.sqrt(roundedMaxValue);

                diameterAndNumber[0][0] = 2 * Math.round(sqrtRoundedMaxValue * maxRadius / maxSqrt);
                diameterAndNumber[0][1] = roundedMaxValue;
                int number = replaceFirstDigitByOne(roundedMaxValue);
                for (int i = 1; i <= 2; i++) {
                    while (Math.abs((2 * Math.sqrt(number) * maxRadius / maxSqrt) - diameterAndNumber[i - 1][0]) < 12 && number >= 1) {
                        number /= 10;
                    }
                    if (number > 0) {
                        diameterAndNumber[i][0] = 2 * (int) Math.round(Math.sqrt(number) * maxRadius / maxSqrt);
                        diameterAndNumber[i][1] = number;
                    }
                }
                break;
            }
        }

        Font font = getFont(ChartViewer.FontKeys.LegendFont.toString());
        gc.setFont(new Font(font.getFamily(), font.getStyle(), 9));
        gc.setColor(Color.darkGray);
        ((Graphics2D) gc).setStroke(new BasicStroke(1));

        // draw the circles:
        int prevTop = -1;
        for (int[] pair : diameterAndNumber) {
            final int diameter = pair[0];
            final int top = boxBottomY - diameter - 14;
            // if(prevTop==-1 || top-prevTop>10)
            {
                final String label = String.format("%,d", pair[1]) + (scalingType == ChartViewer.ScalingType.PERCENT ? "%" : "");
                drawStringCentered(gc, label, boxMidX, top + 2, true);
                gc.drawOval(boxMidX - diameter / 2, boxBottomY - diameter, diameter, diameter);
                prevTop = top;
            }

        }
    }

    private int replaceAllButFirstDigitByZero(int value) {
        String str = "" + value;
        return Integer.parseInt(str.charAt(0) + str.substring(1).replaceAll(".", "0")); // use, replace all digits by 0
    }

    private int replaceFirstDigitByOne(int value) {
        String str = "" + value;
        return Integer.parseInt("1" + str.substring(1));
    }

    public boolean canShowValues() {
        return true;
    }

    @Override
    public ChartViewer.ScalingType getScalingTypePreference() {
        return ChartViewer.ScalingType.SQRT;
    }

    @Override
    public String getChartDrawerName() {
        return NAME;
    }
}
