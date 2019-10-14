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
import megan.chart.data.IChartData;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

/**
 * draws a bar chart
 * Daniel Huson, 5.2012
 */
public class BarChartDrawer extends ChartDrawerBase implements IChartDrawer {
    public static final String NAME = "BarChart";
    private Double maxDisplayedYValue = null;
    final Rectangle lastDown = null;

    private boolean showVerticalGridLines = true;
    private boolean gapBetweenBars = true;

    /**
     * constructor
     */
    public BarChartDrawer() {
        setBackground(Color.WHITE);
    }

    /**
     * paints the chart
     *
     * @param gc0
     */
    public void paint(Graphics gc0) {
        super.paint(gc0);
        leftMargin = 90;
        rightMargin = 75;
        bottomMargin = 200;
        topMargin = 20;

        final Graphics2D gc = (Graphics2D) gc0;

        if (getChartData().getRange() == null || getChartData().getNumberOfClasses() == 0 || getChartData().getNumberOfSeries() == 0) {
            drawTitle(gc, null);
            gc.setColor(Color.LIGHT_GRAY);
            String label = "No data to show: please select nodes in main viewer and then press 'sync'";
            gc.setFont(getFont("Default"));
            Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
            gc.drawString(label, (getWidth() - labelSize.width) / 2, 50);
            return;
        }

        Dimension dim = new Dimension();
        drawTitle(gc, dim);
        topMargin = dim.height;
        if (isShowYAxis()) {
            drawYAxis(gc, dim);
            leftMargin = dim.width;
        }

        bottomMargin = 0;
        if (isShowXAxis()) {
            int xAxisLabelHeight;
            if (isTranspose())
                xAxisLabelHeight = (int) Math.round(computeXAxisLabelHeightTransposed(gc) + 15);
            else
                xAxisLabelHeight = (int) Math.round(computeXAxisLabelHeight(gc) + 15);
            xAxisLabelHeight = Math.min((int) (0.7 * getHeight()), xAxisLabelHeight);

            bottomMargin += xAxisLabelHeight;

            if (classLabelAngle > 0 && classLabelAngle < Math.PI / 2)
                rightMargin = Math.max(75, (int) (0.8 * xAxisLabelHeight));
        } else
            bottomMargin += 20;

        drawTitle(gc, null);


        if (isLargeEnough()) {
            if (isShowXAxis())
                drawXAxis(gc);

            if (isShowYAxis())
                drawYAxis(gc, null);

            if (isTranspose()) {
                computeScrollBackReferenceRect();
                drawChartTransposed(gc);
            } else // color by dataset
            {
                computeScrollBackReferenceRect();
                drawChart(gc);
            }
        }
    }

    /**
     * draw the title of the chart.
     * If size!=null, sets the size only, does not draw
     *
     * @param gc
     */
    private void drawTitle(Graphics2D gc, Dimension size) {
        gc.setFont(getFont(ChartViewer.FontKeys.TitleFont.toString()));
        if (chartTitle != null) {
            Dimension labelSize = BasicSwing.getStringSize(gc, chartTitle, gc.getFont()).getSize();
            if (size != null) {
                size.setSize(labelSize.getWidth(), labelSize.getHeight() + 20);
                return;
            }

            int x = (getWidth() - labelSize.width) / 2;
            int y = labelSize.height;
            gc.setColor(getFontColor(ChartViewer.FontKeys.TitleFont.toString(), Color.BLACK));
            gc.drawString(chartTitle, x, y);
            //gc.drawLine(x,y+4,x+labelSize.width,y+4);
        } else if (size != null)
            size.setSize(0, 20);
    }

    /**
     * is canvas large enough to draw chart?
     *
     * @return true, if can draw chart
     */
    private boolean isLargeEnough() {
        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;
        return x0 < x1 && y0 > y1;
    }

    /**
     * draw the x axis
     *
     * @param gc
     */
    protected void drawXAxis(Graphics2D gc) {
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));
        gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.BLACK));
        int x = 5;
        int y = getHeight() - bottomMargin + 25;
        if (isTranspose())
            gc.drawString(getChartData().getClassesLabel(), x, y);
        else
            gc.drawString(getChartData().getSeriesLabel(), x, y);
    }

    /**
     * draw the y-axis
     *
     * @param gc
     */
    protected void drawYAxis(Graphics2D gc, Dimension size) {
        gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));

        double topY;
        if (scalingType == ChartViewer.ScalingType.PERCENT) {
            if (isTranspose()) {
                final String[] seriesIncludingDisabled = getChartData().getSeriesNamesIncludingDisabled();
                final double[] percentFactor;
                percentFactor = computePercentFactorPerSampleForTransposedChart((DefaultChartData) getChartData(), seriesIncludingDisabled);
                topY = computeMaxClassValueUsingPercentFactorPerSeries((DefaultChartData) getChartData(), seriesIncludingDisabled, percentFactor);
                if (transposedHeightsAdditive && seriesIncludingDisabled.length > 0) {
                    topY /= seriesIncludingDisabled.length;
                }
            } else
                topY = 101;
        } else if (scalingType == ChartViewer.ScalingType.LOG) {
            drawYAxisLog(gc, size);
            return;
        } else if (scalingType == ChartViewer.ScalingType.SQRT) {
            drawYAxisSqrt(gc, size);
            return;
        } else
            topY = 1.1 * getMaxValue();

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        boolean doDraw = (size == null);

        Rectangle bbox = null;

        double yFactor = (y0 - y1) / topY;
        int tickStep = 0;

        int minSpace = 50;
        for (int i = 1; tickStep == 0; i *= 10) {
            if (i * yFactor >= minSpace)
                tickStep = i;
            else if (2.5 * i * yFactor >= minSpace)
                tickStep = (int) (2.5 * i);
            else if (5 * i * yFactor >= minSpace)
                tickStep = 5 * i;
        }

        for (int value = 0; (value - 1) < topY; value += tickStep) {
            if (maxDisplayedYValue != null && value > maxDisplayedYValue)
                break;
            String label = String.format("%,d", value);
            Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
            double yPos = y0 - value * yFactor;
            int x = leftMargin - (int) (labelSize.getWidth() + 3);
            int y = (int) (yPos + labelSize.getHeight() / 2.0);
            if (doDraw) {
                gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));
                gc.drawString(label, x, y);
                if (value == 0 || isShowVerticalGridLines()) {
                    gc.setColor(Color.LIGHT_GRAY);
                    gc.drawLine(x0, (int) Math.round(yPos), x1, (int) Math.round(yPos));
                }
            } else {
                Rectangle rect = new Rectangle(x, y, labelSize.width, labelSize.height);
                if (bbox == null)
                    bbox = rect;
                else
                    bbox.add(rect);
            }
        }

        String axisLabel = getChartData().getCountsLabel() + (scalingType == ChartViewer.ScalingType.PERCENT ? " (%)" : "");
        Dimension labelSize = BasicSwing.getStringSize(gc, axisLabel, gc.getFont()).getSize();
        int x = 10;
        int y = (y0 + y1) / 2 - labelSize.width;
        if (doDraw) {
            gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));
            drawString(gc, axisLabel, x, y, Math.PI / 2);
        } else {
            Rectangle rect = new Rectangle(x, y, labelSize.height, labelSize.width);   // yes, other way around (because label rotated)
            if (bbox == null)
                bbox = rect;
            else
                bbox.add(rect);
        }

        // draw y-axis
        if (doDraw) {
            gc.setColor(Color.BLACK);
            gc.drawLine(x0, y0, x0, y1);
            drawArrowHead(gc, new Point(x0, y0), new Point(x0, y1));
        }

        if (size != null)
            size.setSize(bbox.width + 5, bbox.height);
    }

    /**
     * draw the y-axis  on a log scale
     *
     * @param gc
     */
    protected void drawYAxisLog(Graphics2D gc, Dimension size) {
        gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        boolean doDraw = (size == null);

        Rectangle bbox = null;

        double maxValue = getMaxValue();
        double yFactor = (y0 - y1) / computeMaxYAxisValueLogScale(maxValue);

        double value = 0;
        double previousY = -100000;
        int mantisse = 0;
        int exponent = 0;

        while (value <= maxValue) {
            if (maxDisplayedYValue != null && value > maxDisplayedYValue)
                break;
            double yPos = y0 - (value > 0 ? Math.log10(value) : 0) * yFactor;
            if ((mantisse <= 1 || mantisse == 5) && Math.abs(yPos - previousY) >= 20) {
                String label = String.format("%,d", (long) value);

                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                previousY = yPos;
                int x = leftMargin - (int) (labelSize.getWidth() + 3);
                int y = (int) (yPos + labelSize.getHeight() / 2.0);
                if (doDraw) {
                    gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));
                    gc.drawString(label, x, y);
                    if (value == 0 || isShowVerticalGridLines()) {
                        gc.setColor(Color.LIGHT_GRAY);
                        gc.drawLine(x0, (int) Math.round(yPos), x1, (int) Math.round(yPos));
                    }
                } else {
                    Rectangle rect = new Rectangle(x, y, labelSize.width, labelSize.height);
                    if (bbox == null)
                        bbox = rect;
                    else
                        bbox.add(rect);
                }
            }
            if (mantisse < 9)
                mantisse++;
            else {
                mantisse = 1;
                exponent++;
            }
            value = mantisse * Math.pow(10, exponent);
        }

        String axisLabel = getChartData().getCountsLabel();
        Dimension labelSize = BasicSwing.getStringSize(gc, axisLabel, gc.getFont()).getSize();
        int x = 10;
        int y = (y0 + y1) / 2 - labelSize.width;
        if (doDraw) {
            gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));
            drawString(gc, axisLabel, x, y, Math.PI / 2);
        } else {
            Rectangle rect = new Rectangle(x, y, labelSize.height, labelSize.width);   // yes, other way around (because label rotated)
            if (bbox == null)
                bbox = rect;
            else
                bbox.add(rect);
        }

        // draw y-axis
        if (doDraw) {
            gc.setColor(Color.BLACK);
            gc.drawLine(x0, y0, x0, y1);
            drawArrowHead(gc, new Point(x0, y0), new Point(x0, y1));
        }

        if (size != null)
            size.setSize(bbox.width + 5, bbox.height);
    }

    /**
     * draw the y-axis  on a sqrt scale
     *
     * @param gc
     */
    private void drawYAxisSqrt(Graphics2D gc, Dimension size) {
        gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        boolean doDraw = (size == null);

        Rectangle bbox = null;

        double maxValue = getMaxValue();
        double yFactor = (y0 - y1) / Math.sqrt(maxValue);

        double value = 0;
        double previousY = -100000;
        int mantisse = 0;
        int exponent = 0;

        while (value <= maxValue) {
            if (maxDisplayedYValue != null && value > maxDisplayedYValue)
                break;
            double yPos = y0 - (value > 0 ? Math.sqrt(value) : 0) * yFactor;
            if ((mantisse <= 1 || mantisse == 5) && Math.abs(yPos - previousY) >= 20) {
                String label = String.format("%,d", (long) value);
                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                previousY = yPos;
                int x = leftMargin - (int) (labelSize.getWidth() + 3);
                int y = (int) (yPos + labelSize.getHeight() / 2.0);
                if (doDraw) {
                    gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));
                    gc.drawString(label, x, y);
                    if (showVerticalGridLines) {
                        gc.setColor(Color.LIGHT_GRAY);
                        gc.drawLine(x0, (int) Math.round(yPos), x1, (int) Math.round(yPos));
                    }
                } else {
                    Rectangle rect = new Rectangle(x, y, labelSize.width, labelSize.height);
                    if (bbox == null)
                        bbox = rect;
                    else
                        bbox.add(rect);
                }
            }
            if (mantisse < 9)
                mantisse++;
            else {
                mantisse = 1;
                exponent++;
            }
            value = mantisse * Math.pow(10, exponent);
        }

        String axisLabel = getChartData().getCountsLabel();
        Dimension labelSize = BasicSwing.getStringSize(gc, axisLabel, gc.getFont()).getSize();
        int x = 10;
        int y = (y0 + y1) / 2 - labelSize.width;
        if (doDraw) {
            gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));
            drawString(gc, axisLabel, x, y, Math.PI / 2);
        } else {
            Rectangle rect = new Rectangle(x, y, labelSize.height, labelSize.width);   // yes, other way around (because label rotated)
            if (bbox == null)
                bbox = rect;
            else
                bbox.add(rect);
        }

        // draw y-axis
        if (doDraw) {
            gc.setColor(Color.BLACK);
            gc.drawLine(x0, y0, x0, y1);
            drawArrowHead(gc, new Point(x0, y0), new Point(x0, y1));
        }

        if (size != null)
            size.setSize(bbox.width + 5, bbox.height);
    }

    protected double computeXAxisLabelHeight(Graphics2D gc) {
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));
        double theHeight = 2 * gc.getFont().getSize();
        if (classLabelAngle != 0) {
            double sin = Math.abs(Math.sin(classLabelAngle));
            for (String series : getChartData().getSeriesNames()) {
                String label = seriesLabelGetter.getLabel(series);
                if (label.length() > 50)
                    label = label.substring(0, 50) + "...";
                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                theHeight = Math.max(theHeight, gc.getFont().getSize() + sin * labelSize.width);
            }
        }
        return theHeight;
    }

    protected double computeXAxisLabelHeightTransposed(Graphics2D gc) {
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));
        double theHeight = 2 * gc.getFont().getSize();
        if (classLabelAngle != 0) {
            double sin = Math.abs(Math.sin(classLabelAngle));
            for (String className : getChartData().getClassNames()) {
                Dimension labelSize = BasicSwing.getStringSize(gc, className, gc.getFont()).getSize();
                theHeight = Math.max(theHeight, gc.getFont().getSize() + sin * labelSize.width);
            }
        }
        return theHeight;
    }

    /**
     * draw chart
     *
     * @param gc
     */
    public void drawChart(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        // if(sgc!=null) lastDown=(Rectangle)sgc.getSelectionRectangle().clone();

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
        int numberOfClasses = getChartData().getNumberOfClasses();
        if (numberOfDataSets == 0 || numberOfClasses == 0)
            return;
        double xStep = (x1 - x0) / ((numberOfClasses + (isGapBetweenBars() ? 1 : 0)) * numberOfDataSets);
        final double bigSpace = Math.max(2, Math.min(10, xStep));
        xStep = (x1 - x0 - (isGapBetweenBars() ? bigSpace * numberOfDataSets : 0)) / (numberOfClasses * numberOfDataSets);

        // main drawing loop:
        int d = 0;
        for (String series : getChartData().getSeriesNames()) {
            if (isShowXAxis()) {
                final double xLabel = leftMargin + (isGapBetweenBars() ? (d + 1) * bigSpace : 0) + ((d + 0.5) * numberOfClasses) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - bottomMargin + 10);
                String label = seriesLabelGetter.getLabel(series);
                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                if (classLabelAngle == 0) {
                    apt.setLocation(apt.getX() - labelSize.getWidth() / 2, apt.getY());
                } else if (classLabelAngle > Math.PI / 2) {
                    apt = Geometry.translateByAngle(apt, classLabelAngle, -labelSize.width);
                }
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{series, null});
                if (getChartData().getChartSelection().isSelected(series, null)) {
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    fillAndDrawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, classLabelAngle, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                }
                gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.BLACK));
                drawString(gc, label, apt.getX(), apt.getY(), classLabelAngle);
                if (sgc != null)
                    sgc.clearCurrentItem();
            }

            int c = 0;
            for (String className : getChartData().getClassNames()) {
                double value = getChartData().getValueAsDouble(series, className);
                switch (scalingType) { // modify if not linear scale:
                    case PERCENT: {
                        double total = getChartData().getTotalForSeriesIncludingDisabledAttributes(series);

                        if (total == 0)
                            value = 0;
                        else
                            value *= (100 / total);
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

                // coordinates for d-th dataset and c-th class:
                double xBar = x0 + (isGapBetweenBars() ? (d + 1) * bigSpace : 0) + (d * numberOfClasses + c) * xStep;

                double height = Math.max(1, value * yFactor);

                Rectangle2D rect = new Rectangle((int) Math.round(xBar),
                        (int) Math.round(y0 - height), Math.max(1, (int) Math.round(xStep)), (int) Math.round(height));

                boolean isSelected = getChartData().getChartSelection().isSelected(series, className);
                Color color = getChartColors().getClassColor(class2HigherClassMapper.get(className));
                gc.setColor(color);
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{series, className});
                gc.fill(rect);
                if (sgc != null)
                    sgc.clearCurrentItem();

                if (isSelected) {
                    gc.setStroke(HEAVY_STROKE);
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    gc.draw(rect);
                    gc.setStroke(NORMAL_STROKE);
                } else {
                    gc.setColor(color.darker());
                    gc.draw(rect);
                }

                if (showValues || isSelected) {
                    String label = "" + (int) getChartData().getValueAsDouble(series, className);
                    valuesList.add(new DrawableValue(label, (int) (rect.getX() + rect.getWidth() / 2), (int) (rect.getY() - 2), isSelected));
                }
                c++;
            }
            d++;
        }
        if (valuesList.size() > 0) {
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            DrawableValue.drawValues(gc, valuesList, true, false);
            valuesList.clear();
        }

        if (sgc == null && lastDown != null) {
            gc.setColor(Color.green);
            gc.draw(lastDown);
        }
    }


    /**
     * draw bars in which colors are by dataset
     *
     * @param gc
     */
    public void drawChartTransposed(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        // if(sgc!=null) lastDown=(Rectangle)sgc.getSelectionRectangle().clone();
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));

        final int y0 = getHeight() - bottomMargin;
        final int y1 = topMargin;

        final String[] series = getChartData().getSeriesNames().toArray(new String[getChartData().getNumberOfSeries()]);

        final double topY;
        final double[] percentFactor;
        if (scalingType == ChartViewer.ScalingType.PERCENT) {
            final String[] seriesIncludingDisabled = getChartData().getSeriesNamesIncludingDisabled();
            percentFactor = computePercentFactorPerSampleForTransposedChart((DefaultChartData) getChartData(), seriesIncludingDisabled);
            topY = computeMaxClassValueUsingPercentFactorPerSeries((DefaultChartData) getChartData(), seriesIncludingDisabled, percentFactor);
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

        final int numberOfDataSets = getChartData().getNumberOfSeries();
        final int numberOfClasses = getChartData().getNumberOfClasses();
        double xStep = (x1 - x0) / (numberOfClasses * (numberOfDataSets + (isGapBetweenBars() ? 1 : 0))); // step size if big spaces were as big as bars
        final double bigSpace = Math.max(2.0, Math.min(10.0, xStep));
        xStep = (x1 - x0 - (isGapBetweenBars() ? bigSpace * (numberOfClasses) : 0)) / (double) (numberOfClasses * numberOfDataSets);

        // main drawing loop:
        int c = 0;
        for (String className : getChartData().getClassNames()) {
            if (isShowXAxis()) {
                final double xLabel = leftMargin + (isGapBetweenBars() ? (c + 1) * bigSpace : 0) + ((c + 0.5) * numberOfDataSets) * xStep;
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
                gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.BLACK));
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

                final double xBar = leftMargin + (isGapBetweenBars() ? (c + 1) * bigSpace : 0) + (c * numberOfDataSets + d) * xStep;
                final double height = Math.max(0, value * yFactor);
                final Rectangle2D rect = new Rectangle((int) Math.round(xBar),
                        (int) Math.round(y0 - height), Math.max(1, (int) Math.round(xStep)), (int) Math.round(height));

                final Color color = getChartColors().getSampleColor(seriesName);

                gc.setColor(color);
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{seriesName, className});
                gc.fill(rect);
                if (sgc != null)
                    sgc.clearCurrentItem();
                final boolean isSelected = getChartData().getChartSelection().isSelected(seriesName, className);

                if (isSelected) {
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    gc.setStroke(HEAVY_STROKE);
                    gc.draw(rect);
                    gc.setStroke(NORMAL_STROKE);
                } else {
                    gc.setColor(color.darker());
                    gc.draw(rect);
                }

                if (showValues || isSelected) {
                    String label = "" + (int) getChartData().getValueAsDouble(seriesName, className);
                    valuesList.add(new DrawableValue(label, (int) (rect.getX() + rect.getWidth() / 2), (int) (rect.getY() - 2), isSelected));
                }
                d++;
            }
            c++;
        }
        if (valuesList.size() > 0) {
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            DrawableValue.drawValues(gc, valuesList, true, false);
            valuesList.clear();
        }

        if (sgc == null && lastDown != null) {
            gc.setColor(Color.green);
            gc.draw(lastDown);
        }
    }

    public Double getMaxDisplayedYValue() {
        return maxDisplayedYValue;
    }

    /**
     * use this to set the max displayed y value
     *
     * @param maxDisplayedYValue
     */
    public void setMaxDisplayedYValue(Double maxDisplayedYValue) {
        this.maxDisplayedYValue = maxDisplayedYValue;
    }

    /**
     * gets the max value used in the plot. Stacked bar chart overrides this
     *
     * @return max value
     */
    protected double getMaxValue() {
        try {
            return getChartData().getRange().get2().doubleValue();
        } catch (NullPointerException ex) {
            return 100;
        }
    }

    public IChartData getChartData() {
        return (IChartData) chartData;
    }

    public boolean canShowValues() {
        return true;
    }

    @Override
    public String getChartDrawerName() {
        return NAME;
    }

    /**
     * gets factor used to compute percentage for a series in a transposed chart
     *
     * @param chartData
     * @param series
     * @return factors
     */
    public double[] computePercentFactorPerSampleForTransposedChart(DefaultChartData chartData, String[] series) {
        final double[] percentFactorPerSample = new double[series.length];

        for (int i = 0; i < series.length; i++) {
            double value = chartData.getTotalForSeriesIncludingDisabledAttributes(series[i]);
            percentFactorPerSample[i] = (value == 0 ? 0 : 100 / value);
        }
        return percentFactorPerSample;
    }

    /**
     * gets the max value for a given class in a transposed chart when using percentages
     *
     * @param chartData
     * @param series
     * @param percentFactorPerSeries
     * @return max sum seen for any of the classes
     */
    public double computeMaxClassValueUsingPercentFactorPerSeries(DefaultChartData chartData, String[] series, double[] percentFactorPerSeries) {
        double maxValue = 0;
        for (String className : chartData.getClassNamesIncludingDisabled()) {
            double total = 0;
            for (int i = 0; i < series.length; i++) {
                String seriesName = series[i];
                if (transposedHeightsAdditive) // stacked charts
                    total += percentFactorPerSeries[i] * chartData.getValueAsDouble(seriesName, className);
                else // bar chart, line chart
                    total = Math.max(total, percentFactorPerSeries[i] * chartData.getValueAsDouble(seriesName, className));
            }
            if (total > maxValue) {
                maxValue = total;
            }
        }
        return 1.1 * maxValue;
    }

    public boolean isShowVerticalGridLines() {
        return showVerticalGridLines;
    }

    public void setShowVerticalGridLines(boolean showVerticalGridLines) {
        this.showVerticalGridLines = showVerticalGridLines;
    }

    public boolean isGapBetweenBars() {
        return gapBetweenBars;
    }

    public void setGapBetweenBars(boolean gapBetweenBars) {
        this.gapBetweenBars = gapBetweenBars;
    }
}
