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
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import megan.chart.IChartDrawer;
import megan.chart.data.IPlot2DData;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * draws a 2D plot
 * Daniel Huson, 5.2012
 */
public class Plot2DDrawer extends ChartDrawerBase implements IChartDrawer {
    public static final String NAME = "Plot2D";


    private final Double maxDisplayedXValue = null;
    private Double maxDisplayedYValue = null;

    public enum GridStyle {ABOVE, BELOW, NONE}

    private GridStyle gridStyle = GridStyle.ABOVE;

    private final Set<String> seriesWithoutLines = new HashSet<>();
    private final Set<String> seriesWithoutDots = new HashSet<>();
    private final Set<String> seriesWithJitter = new HashSet<>();

    /**
     * constructor
     */
    public Plot2DDrawer() {
        super();
        setBackground(Color.WHITE);
        leftMargin = 100;
        rightMargin = 20;
    }

    /**
     * paints the chart
     *
     * @param gc0
     */
    public void paint(Graphics gc0) {
        super.paint(gc0);
        final Graphics2D gc = (Graphics2D) gc0;

        bottomMargin = 50;

        if (isShowXAxis()) {
            double xAxisLabelHeight = computeXAxisLabelHeight(gc);
            bottomMargin += xAxisLabelHeight;

            if (classLabelAngle > 0 && classLabelAngle < Math.PI / 2)
                rightMargin = Math.max(75, (int) (0.8 * xAxisLabelHeight));
        } else
            bottomMargin += 20;

        drawTitle(gc);

        if (getChartData().getRangeX() == null || getChartData().getRangeY() == null)
            return; // nothing to draw

        if (isLargeEnough()) {
            if (gridStyle == GridStyle.BELOW) {
                //   drawGrid(gc, gridStyle);
            }

            computeScrollBackReferenceRect();
            drawChart(gc);
            if (isShowXAxis())
                drawXAxis(gc);
            if (isShowYAxis())
                drawYAxis(gc);
            if (gridStyle == GridStyle.ABOVE) {
                //   drawGrid(gc, gridStyle);
            }
        }
    }

    /**
     * draw the title of the chart
     *
     * @param gc
     */
    private void drawTitle(Graphics2D gc) {
        if (chartTitle != null) {
            Dimension labelSize = BasicSwing.getStringSize(gc, chartTitle, gc.getFont()).getSize();
            int x = (getWidth() - labelSize.width) / 2;
            int y = labelSize.height + 5;
            gc.setFont(getFont(ChartViewer.FontKeys.TitleFont.toString()));
            gc.setColor(getFontColor(ChartViewer.FontKeys.TitleFont.toString(), Color.BLACK));
            gc.drawString(chartTitle, x, y);
            //gc.drawLine(x,y+4,x+labelSize.width,y+4);
        }
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
    private void drawXAxis(Graphics2D gc) {
        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        // draw x-axis
        gc.setColor(Color.BLACK);
        gc.drawLine(x0, y0, x1 + 10, y0);
        drawArrowHead(gc, new Point(x0, y0), new Point(x1 + 10, y0));
        drawXAxisTicks(gc);

        if (getChartData().getSeriesLabel() != null) {
            gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));
            gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.BLACK));
            int x = 5;
            int y = getHeight() - bottomMargin + 25;
            if (isTranspose())
                gc.drawString(getChartData().getClassesLabel(), x, y);
            else
                gc.drawString(getChartData().getSeriesLabel(), x, y);
        }

    }

    /**
     * draw the ticks along the X axis
     *
     * @param gc
     */
    private void drawXAxisTicks(Graphics2D gc) {
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        int y0 = getHeight() - bottomMargin;

        double botX = 0;
        double topX = transpose ? getChartData().getRangeY().get2().doubleValue() : getChartData().getRangeX().get2().doubleValue();

        double xFactor;
        if (topX > botX)
            xFactor = (x1 - x0) / (topX - botX);
        else
            xFactor = 1;

        int tickStepX = 0;

        int minSpace = 50;
        for (int i = 1; tickStepX == 0; i *= 10) {
            if (i * xFactor >= minSpace)
                tickStepX = i;
            else if (2.5 * i * xFactor >= minSpace)
                tickStepX = (int) (2.5 * i);
            else if (5 * i * xFactor >= minSpace)
                tickStepX = 5 * i;
        }

        int startX = 0;
        while (startX + tickStepX < botX) {
            startX += tickStepX;
        }
        double offsetX = botX - startX;

        gc.setColor(Color.BLACK);
        for (int value = startX; (value - 1) < topX; value += tickStepX) {
            if (value >= botX) {
                if (maxDisplayedXValue != null && value > maxDisplayedXValue)
                    break;
                String label = "" + value;
                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                double xPos = x0 + value * xFactor - offsetX;
                //  if (xPos - x0 > tickStepX)
                Point2D apt = new Point2D.Double(xPos, (y0 + labelSize.getHeight() + 2));
                if (classLabelAngle == 0) {
                    apt.setLocation(apt.getX() - labelSize.getWidth() / 2, apt.getY());
                } else if (classLabelAngle > Math.PI / 2) {
                    apt = Geometry.translateByAngle(apt, classLabelAngle, -labelSize.width);
                }
                drawString(gc, label, apt.getX(), apt.getY(), classLabelAngle);
                gc.drawLine((int) xPos, y0, (int) xPos, y0 - 2);
            }
        }
    }

    private double computeXAxisLabelHeight(Graphics2D gc) {
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));
        double theHeight = 2 * gc.getFont().getSize();
        if (classLabelAngle != 0) {
            double sin = Math.abs(Math.sin(classLabelAngle));
            Dimension labelSize = BasicSwing.getStringSize(gc, "" + maxDisplayedXValue, gc.getFont()).getSize();
            theHeight = gc.getFont().getSize() + sin * labelSize.width;
        }
        return theHeight;
    }

    /**
     * draw the y-axis
     *
     * @param gc
     */
    private void drawYAxis(Graphics2D gc) {

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        gc.setColor(Color.BLACK);

        // draw y-axis
        gc.drawLine(x0, y0, x0, y1 - 10);
        drawArrowHead(gc, new Point(x0, y0), new Point(x0, y1 - 10));
        drawYAxisTicks(gc);

        if (getChartData().getCountsLabel() != null) {
            String label = getChartData().getCountsLabel();
            if (scalingType == ChartViewer.ScalingType.PERCENT)
                label += " (%)";

            Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
            int x = 15;
            int y = (y0 + y1) / 2 - labelSize.width / 2;
            gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));
            gc.setColor(getFontColor(ChartViewer.FontKeys.YAxisFont.toString(), Color.BLACK));
            drawString(gc, label, x, y, Math.PI / 2);
            //gc.drawString(getyAxisLabel(),x,y);
        }
    }

    /**
     * draw the ticks along the Y axis
     *
     * @param gc
     */
    private void drawYAxisTicks(Graphics2D gc) {
        gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));

        if (scalingType == ChartViewer.ScalingType.LOG) {
            drawYAxisTicksLog(gc);
            return;
        } else if (scalingType == ChartViewer.ScalingType.SQRT) {
            drawYAxisTicksSqrt(gc);
            return;
        }
        int x0 = leftMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        double botY;
        double topY;
        if (scalingType == ChartViewer.ScalingType.PERCENT) {
            botY = 0;
            topY = 100;
        } else {
            botY = 0;
            topY = getChartData().getRangeY().get2().doubleValue();
        }

        double yFactor;
        if (topY > botY)
            yFactor = (y0 - y1) / (topY - botY);
        else
            yFactor = 1;

        int tickStepY = 0;

        int minSpace = 50;
        for (int i = 1; tickStepY == 0; i *= 10) {
            if (i * yFactor >= minSpace)
                tickStepY = i;
            else if (2.5 * i * yFactor >= minSpace)
                tickStepY = (int) (2.5 * i);
            else if (5 * i * yFactor >= minSpace)
                tickStepY = 5 * i;
        }

        int startY = 0;
        while (startY + tickStepY < botY) {
            startY += tickStepY;
        }
        double offsetY = botY - startY;

        gc.setColor(Color.BLACK);
        for (int value = startY; (value - 1) < topY; value += tickStepY) {
            if (value >= botY) {
                if (maxDisplayedYValue != null && value > maxDisplayedYValue)
                    break;
                String label = "" + value;
                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                double y = y0 - value * yFactor + offsetY;
                if (y < y1)
                    break;
                float yPos = (float) (y + labelSize.getHeight() / 2.0);
                gc.drawString(label, leftMargin - (int) (labelSize.getWidth() + 3), yPos);
                gc.drawLine(x0, (int) y, x0 + 2, (int) y);

            }
        }
    }

    /**
     * draw the ticks along the Y axis
     *
     * @param gc
     */
    private void drawYAxisTicksSqrt(Graphics2D gc) {
        gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        double maxValue = getChartData().getRangeY().get2().doubleValue();
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
                String label = "" + (long) value;
                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                previousY = yPos;
                int x = leftMargin - (int) (labelSize.getWidth() + 3);
                int y = (int) (yPos + labelSize.getHeight() / 2.0);
                gc.setColor(Color.BLACK);
                gc.drawString(label, x, y);
                if (gridStyle == GridStyle.BELOW) {
                    gc.setColor(Color.LIGHT_GRAY);
                    gc.drawLine(x0, (int) Math.round(yPos), x1, (int) Math.round(yPos));
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

        String axisLabel = getChartData().getCountsLabel() + " (sqrt scale)";
        Dimension labelSize = BasicSwing.getStringSize(gc, axisLabel, gc.getFont()).getSize();
        int x = 10;
        int y = (y0 + y1) / 2 - labelSize.width;
        gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));
        drawString(gc, axisLabel, x, y, Math.PI / 2);
    }

    /**
     * draw the ticks along the Y axis
     *
     * @param gc
     */
    private void drawYAxisTicksLog(Graphics2D gc) {
        gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));

        int x0 = leftMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        double maxValue = getChartData().getRangeY().get2().doubleValue();
        double botY = 0;
        double topY = computeMaxYAxisValueLogScale(maxValue);

        double yFactor;
        if (topY > botY)
            yFactor = (y0 - y1) / (topY - botY);
        else
            yFactor = 1;

        double value = 0;
        double previousY = -100000;
        int mantisse = 0;
        int exponent = 0;

        while (value <= maxValue) {
            if (maxDisplayedYValue != null && value > maxDisplayedYValue)
                break;
            double yPos = y0 - (value > 0 ? Math.log10(value) : 0) * yFactor;
            if ((mantisse <= 1 || mantisse == 5) && Math.abs(yPos - previousY) >= 20) {
                String label = "" + (long) value;
                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                previousY = yPos;
                int x = leftMargin - (int) (labelSize.getWidth() + 3);
                int y = (int) (yPos + labelSize.getHeight() / 2.0);
                gc.drawString(label, x, y);
                gc.drawLine(x0, (int) Math.round(yPos), x0 + 2, (int) Math.round(yPos));
            }
            if (mantisse < 9)
                mantisse++;
            else {
                mantisse = 1;
                exponent++;
            }
            value = mantisse * Math.pow(10, exponent);
        }
    }

    /**
     * draw bars in which colors are by dataset
     *
     * @param gc
     */
    public void drawChart(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        double botX = 0;
        double topX = transpose ? getChartData().getRangeY().get2().doubleValue() : getChartData().getRangeX().get2().doubleValue();

        double factorX;
        if (topX > botX)
            factorX = (x1 - x0) / (topX - botX);
        else
            factorX = 1;

        int tickStepX = 0;

        int minSpace = 50;
        for (int i = 1; tickStepX == 0; i *= 10) {
            if (i * factorX >= minSpace)
                tickStepX = i;
            else if (2.5 * i * factorX >= minSpace)
                tickStepX = (int) (2.5 * i);
            else if (5 * i * factorX >= minSpace)
                tickStepX = 5 * i;
        }

        int startX = 0;
        while (startX + tickStepX < botX) {
            startX += tickStepX;
        }
        double offsetX = botX - startX;

        double botY;
        double topY;
        if (scalingType == ChartViewer.ScalingType.PERCENT) {
            botY = 0;
            topY = 100;
        } else if (scalingType == ChartViewer.ScalingType.LOG) {
            botY = 0;
            topY = computeMaxYAxisValueLogScale(getChartData().getRangeY().get2().doubleValue());
        } else if (scalingType == ChartViewer.ScalingType.SQRT) {
            botY = 0;
            topY = Math.sqrt(getChartData().getRangeY().get2().doubleValue());
        } else {
            botY = 0;
            topY = getChartData().getRangeY().get2().doubleValue();
        }

        double factorY;
        if (topY > botY)
            factorY = (y0 - y1) / (topY - botY);
        else
            factorY = 1;

        int tickStepY = 0;

        for (int i = 1; tickStepY == 0; i *= 10) {
            if (i * factorY >= minSpace)
                tickStepY = i;
            else if (2.5 * i * factorY >= minSpace)
                tickStepY = (int) (2.5 * i);
            else if (5 * i * factorY >= minSpace)
                tickStepY = 5 * i;
        }

        int startY = 0;
        while (startY + tickStepY < botY) {
            startY += tickStepY;
        }
        double offsetY = botY - startY;
        Random random = new Random(666);
        double maxX = getChartData().getRangeX().get2().doubleValue();
        double maxY = getChartData().getRangeY().get2().doubleValue();

        for (String series : getChartData().getSeriesNames()) {
            Point previous = null;
            boolean showLines = isShowLines(series);
            boolean showDots = isShowDots(series);
            boolean useJitter = isUseJitter(series);
            Color color = getChartColors().getSampleColor(series);
            Color darker = color.darker();

            boolean isSelected = getChartData().getChartSelection().isSelected(series, null);

            if (sgc != null)
                sgc.setCurrentItem(new String[]{series, null});

            java.util.Collection<Pair<Number, Number>> data = getChartData().getDataForSeries(series);
            for (Pair<Number, Number> pair : data) {
                double xValue = pair.get1().doubleValue();
                double yValue;

                if (getScalingType() == ChartViewer.ScalingType.PERCENT) {
                    yValue = (100.0 / maxY) * pair.get2().doubleValue();
                } else if (getScalingType() == ChartViewer.ScalingType.LOG) {
                    yValue = pair.get2().doubleValue();
                    if (yValue > 0)
                        yValue = Math.log10(yValue);
                } else if (getScalingType() == ChartViewer.ScalingType.SQRT) {
                    yValue = pair.get2().doubleValue();
                    if (yValue > 0)
                        yValue = Math.sqrt(yValue);
                } else {
                    yValue = pair.get2().doubleValue();
                }

                int x = (int) Math.round(xValue * factorX + x0 - offsetX);
                int y = (int) Math.round(y0 - yValue * factorY - offsetY);

                if (useJitter) {
                    x += (random.nextInt(8) - 4);
                    y += (random.nextInt(8) - 4);
                }

                if (showLines) {
                    if (previous != null) {
                        if (isSelected) {
                            gc.setColor(ProgramProperties.SELECTION_COLOR);
                            gc.setStroke(HEAVY_STROKE);
                            gc.drawLine(previous.x, previous.y, x, y);
                            gc.setStroke(NORMAL_STROKE);
                        } else {
                            gc.setColor(color);
                            gc.drawLine(previous.x, previous.y, x, y);
                        }
                    }
                    previous = new Point(x, y);
                }
                if (showDots) {
                    if (isSelected) {
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        gc.setStroke(HEAVY_STROKE);
                        gc.fillOval(x - 2, y - 2, 4, 4);
                        gc.setStroke(NORMAL_STROKE);
                    } else {
                        gc.setColor(darker);
                        gc.fillOval(x - 2, y - 2, 4, 4);
                    }
                }
                if (showValues || isSelected) {
                    String label = pair.get1() + "," + pair.get2();
                    valuesList.add(new DrawableValue(label, x + 4, y, isSelected));
                }
            }
            if (sgc != null)
                sgc.clearCurrentItem();
        }

        if (valuesList.size() > 0) {
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            DrawableValue.drawValues(gc, valuesList, false, false);
            valuesList.clear();
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

    public GridStyle getGridStyle() {
        return gridStyle;
    }

    public void setGridStyle(GridStyle gridStyle) {
        this.gridStyle = gridStyle;
    }

    public void setShowLines(String series, boolean join) {
        if (join)
            seriesWithoutLines.remove(series);
        else
            seriesWithoutLines.add(series);
    }

    private boolean isShowLines(String series) {
        return !seriesWithoutLines.contains(series);
    }

    public void setShowDots(String series, boolean join) {
        if (join)
            seriesWithoutDots.remove(series);
        else
            seriesWithoutDots.add(series);
    }

    private boolean isShowDots(String series) {
        return !seriesWithoutDots.contains(series);
    }

    public boolean isUseJitter(String series) {
        return seriesWithJitter.contains(series);
    }

    public void setUseJitter(String series, boolean useJitter) {
        if (useJitter)
            seriesWithJitter.add(series);
        else
            seriesWithJitter.remove(series);
    }

    public IPlot2DData getChartData() {
        return (IPlot2DData) super.getChartData();
    }

    public boolean canTranspose() {
        return false;
    }

    public boolean canShowValues() {
        return true;
    }

    @Override
    public String getChartDrawerName() {
        return NAME;
    }
}
