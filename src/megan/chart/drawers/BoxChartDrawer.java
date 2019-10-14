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
import megan.chart.data.WhiskerData;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;
import megan.core.Document;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * draws a box chart
 * Daniel Huson, 7.2016
 */
public class BoxChartDrawer extends BarChartDrawer implements IChartDrawer {
    private static final String NAME = "BoxChart";

    /**
     * constructor
     */
    public BoxChartDrawer() {
        super();
    }

    protected double computeXAxisLabelHeight(Graphics2D gc) {
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));
        double theHeight = 2 * gc.getFont().getSize();
        if (classLabelAngle != 0) {
            double sin = Math.abs(Math.sin(classLabelAngle));
            for (String className : getChartData().getClassNames()) {
                Dimension labelSize = BasicSwing.getStringSize(gc, className, gc.getFont()).getSize();
                theHeight = Math.max(theHeight, gc.getFont().getSize() + sin * labelSize.width);
            }
        }
        return theHeight + 12;
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
        return theHeight + 12;
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
        else {
            String prefix = null;
            final Document doc = getViewer().getDir().getDocument();
            final boolean hasGroups = doc.getSampleAttributeTable().hasGroups();

            for (String sample : doc.getSampleAttributeTable().getSampleOrder()) {
                String groupId = hasGroups ? doc.getSampleAttributeTable().getGroupId(sample) : "all";
                if (groupId != null) {
                    int pos = groupId.indexOf('=');
                    if (pos > 0) {
                        if (prefix == null)
                            prefix = groupId.substring(0, pos);
                        else if (!prefix.equals(groupId.substring(0, pos))) {
                            prefix = null;
                            break;
                        }
                    }
                }
            }
            gc.drawString(prefix != null ? prefix : "Grouped", x, y);
        }
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

        final Document doc = getViewer().getDir().getDocument();
        final Map<String, Integer> group2index = new HashMap<>();
        final ArrayList<Pair<String, ArrayList<String>>> groupSamplePairs = new ArrayList<>();
        final boolean hasGroups = doc.getSampleAttributeTable().hasGroups();
        final Random random = new Random(666);

        for (String series : doc.getSampleAttributeTable().getSampleOrder()) {
            series = cleanSeriesName(series);
            if (chartData.getSeriesNames().contains(series)) {
                String groupId = hasGroups ? doc.getSampleAttributeTable().getGroupId(series) : "all";
                if (groupId != null) {
                    Integer index = group2index.get(groupId);
                    if (index == null) {
                        index = groupSamplePairs.size();
                        groupSamplePairs.add(new Pair<>(groupId, new ArrayList<>()));
                        group2index.put(groupId, index);
                    }
                    final ArrayList<String> list = groupSamplePairs.get(index).getSecond();
                    list.add(series);
                }
            }
        }
        final WhiskerData whiskerData = new WhiskerData();
        final WhiskerData whiskerDataTransformed = new WhiskerData();

        int numberOfGroups = groupSamplePairs.size(); // because all samples shown in one column
        int numberOfClasses = getChartData().getNumberOfClasses();
        if (numberOfGroups == 0 || numberOfClasses == 0)
            return;

        double xStep = (x1 - x0) / ((numberOfClasses + (isGapBetweenBars() ? 1 : 0)) * numberOfGroups);
        final double bigSpace = Math.max(2, Math.min(10, xStep));
        xStep = (x1 - x0 - (isGapBetweenBars() ? bigSpace * numberOfGroups : 0)) / (numberOfClasses * numberOfGroups);

        final BasicStroke dotted = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{1, 2}, 0);

        // main drawing loop:
        int d = 0;
        for (Pair<String, ArrayList<String>> pair : groupSamplePairs) {
            final String groupName = pair.getFirst();
            if (isShowXAxis()) {
                final double xLabel = leftMargin + (isGapBetweenBars() ? (d + 1) * bigSpace : 0) + ((d + 0.5) * numberOfClasses) * xStep;
                Point2D apt = new Point2D.Double(xLabel, getHeight() - 2);
                Dimension labelSize = BasicSwing.getStringSize(gc, groupName, gc.getFont()).getSize();
                apt.setLocation(apt.getX() - labelSize.getWidth() / 2, apt.getY());
                gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.BLACK));
                drawString(gc, groupName, apt.getX(), apt.getY(), 0);
            }

            // for each class, compute the whisker data and then draw
            int c = 0;
            for (String className : getChartData().getClassNames()) {
                int xPos = (int) Math.round(x0 + (isGapBetweenBars() ? (d + 1) * bigSpace : 0) + (d * numberOfClasses + c) * xStep);
                final boolean isSelected = getChartData().getChartSelection().isSelected(null, className);

                if (isShowXAxis()) {
                    Point2D apt = new Point2D.Double(xPos, getHeight() - bottomMargin + 10);
                    Dimension labelSize = BasicSwing.getStringSize(gc, className, gc.getFont()).getSize();
                    if (classLabelAngle == 0) {
                        apt.setLocation(apt.getX() - labelSize.getWidth() / 2, apt.getY());
                    } else if (classLabelAngle > Math.PI / 2) {
                        apt = Geometry.translateByAngle(apt, classLabelAngle, -labelSize.width);
                    }
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{null, className});
                    if (isSelected) {
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        fillAndDrawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, classLabelAngle, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                    }
                    gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.BLACK));
                    drawString(gc, className, apt.getX(), apt.getY(), classLabelAngle);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                }

                whiskerData.clear();
                whiskerDataTransformed.clear();

                for (String series : pair.getSecond()) {
                    double value = getChartData().getValueAsDouble(series, className);
                    whiskerData.add(value, series);
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
                    whiskerDataTransformed.add(value, series);
                }

                // draw whiskers:
                // coordinates for d-th dataset and c-th class:
                final Color color = getChartColors().getClassColor(class2HigherClassMapper.get(className));
                final Color darkColor = color.darker();

                for (final Pair<Double, String> p : whiskerDataTransformed) {
                    boolean isSelected2 = isSelected;

                    final double value = p.getFirst();
                    final String series = p.getSecond();
                    if (sgc != null) {
                        sgc.setCurrentItem(new String[]{series, className});
                    } else if (!isSelected2)
                        isSelected2 = getChartData().getChartSelection().isSelected(series, null);

                    final int x = xPos + random.nextInt(6) - 3;
                    int height = (int) Math.round(y0 - Math.max(1, value * yFactor));

                    if (isSelected2) {
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        gc.fillOval(x - 5, height - 5, 10, 10);
                    }

                    gc.setColor(color);
                    gc.fillOval(x - 1, height - 1, 2, 2);
                    gc.setColor(darkColor);
                    gc.drawOval(x - 1, height - 1, 2, 2);

                    if (sgc != null) {
                        sgc.clearCurrentItem();
                    }
                }

                gc.setColor(isSelected ? ProgramProperties.SELECTION_COLOR : darkColor);

                final int minHeight = (int) Math.round(y0 - Math.max(1, whiskerDataTransformed.getMin() * yFactor));
                final int quarterHeight = (int) Math.round(y0 - Math.max(1, whiskerDataTransformed.getFirstQuarter() * yFactor));
                final int medianHeight = (int) Math.round(y0 - Math.max(1, whiskerDataTransformed.getMedian() * yFactor));
                final int threeQuaterHeigth = (int) Math.round(y0 - Math.max(1, whiskerDataTransformed.getThirdQuarter() * yFactor));
                final int maxHeight = (int) Math.round(y0 - Math.max(1, whiskerDataTransformed.getMax() * yFactor));

                gc.drawLine(xPos - 4, minHeight, xPos + 4, minHeight);
                gc.drawLine(xPos - 4, maxHeight, xPos + 4, maxHeight);

                gc.drawLine(xPos - 7, quarterHeight, xPos + 7, quarterHeight);
                gc.setStroke(HEAVY_STROKE);
                gc.drawLine(xPos - 6, medianHeight, xPos + 6, medianHeight);
                gc.setStroke(NORMAL_STROKE);
                gc.drawLine(xPos - 7, threeQuaterHeigth, xPos + 7, threeQuaterHeigth);
                gc.drawLine(xPos - 7, quarterHeight, xPos - 7, threeQuaterHeigth);
                gc.drawLine(xPos + 7, quarterHeight, xPos + 7, threeQuaterHeigth);

                gc.setStroke(dotted);
                gc.drawLine(xPos, minHeight, xPos, quarterHeight);
                gc.drawLine(xPos, maxHeight, xPos, threeQuaterHeigth);
                gc.setStroke(NORMAL_STROKE);

                if (sgc != null)
                    sgc.clearCurrentItem();

                if (showValues || isSelected) {
                    String label = "" + (int) whiskerData.getMedian();
                    valuesList.add(new DrawableValue(label, xPos - 4, medianHeight - 1, isSelected));
                    if (minHeight > medianHeight) {
                        label = "" + (int) whiskerData.getMin();
                        valuesList.add(new DrawableValue(label, xPos - 4, minHeight + getFont().getSize() + 1, isSelected));
                    }
                    if (medianHeight - getFont().getSize() > maxHeight) {
                        label = "" + (int) whiskerData.getMax();
                        valuesList.add(new DrawableValue(label, xPos - 4, maxHeight - 1, isSelected));
                    }
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
    }

    /**
     * draw chart
     *
     * @param gc
     */
    public void drawChartTransposed(Graphics2D gc) {
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

        final Document doc = getViewer().getDir().getDocument();
        final Map<String, Integer> group2index = new HashMap<>();
        final ArrayList<Pair<String, ArrayList<String>>> groupSamplePairs = new ArrayList<>();
        final boolean hasGroups = doc.getSampleAttributeTable().hasGroups();
        final Random random = new Random(666);

        for (String series : doc.getSampleAttributeTable().getSampleOrder()) {
            series = cleanSeriesName(series);
            if (chartData.getSeriesNames().contains(series)) {
                String groupId = hasGroups ? doc.getSampleAttributeTable().getGroupId(series) : "all";
                if (groupId != null) {
                    Integer index = group2index.get(groupId);
                    if (index == null) {
                        index = groupSamplePairs.size();
                        groupSamplePairs.add(new Pair<>(groupId, new ArrayList<>()));
                        group2index.put(groupId, index);
                    }
                    final ArrayList<String> list = groupSamplePairs.get(index).getSecond();
                    list.add(series);
                }
            }
        }
        final WhiskerData whiskerData = new WhiskerData();
        final WhiskerData whiskerDataTransformed = new WhiskerData();

        int numberOfGroups = groupSamplePairs.size(); // because all samples shown in one column
        int numberOfClasses = getChartData().getNumberOfClasses();
        if (numberOfGroups == 0 || numberOfClasses == 0)
            return;
        double xStep = (x1 - x0) / ((numberOfGroups + (isGapBetweenBars() ? 1 : 0)) * numberOfClasses);
        final double bigSpace = Math.max(2, Math.min(10, xStep));
        xStep = (x1 - x0 - (isGapBetweenBars() ? bigSpace * numberOfClasses : 0)) / (numberOfClasses * numberOfGroups);

        final BasicStroke dotted = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{1, 2}, 0);

        // main drawing loop:
        int d = 0;
        for (Pair<String, ArrayList<String>> pair : groupSamplePairs) {
            final String groupName = pair.getFirst();

            // for each class, compute the whisker data and then draw
            int c = 0;
            for (String className : getChartData().getClassNames()) {
                whiskerData.clear();
                whiskerDataTransformed.clear();

                final int xPos = (int) Math.round(x0 + (isGapBetweenBars() ? (c + 1) * bigSpace : 0) + (c * numberOfGroups + d) * xStep);
                final boolean isSelected = getChartData().getChartSelection().isSelected(null, className);

                if (isShowXAxis()) {
                    if (group2index.size() > 1) {
                        Point2D bpt = new Point2D.Double(xPos, getHeight() - bottomMargin + 10);
                        final Dimension labelSize = BasicSwing.getStringSize(gc, groupName, gc.getFont()).getSize();
                        if (classLabelAngle == 0) {
                            bpt.setLocation(bpt.getX() - labelSize.getWidth() / 2, bpt.getY() + getFont().getSize() + 1);
                        } else {
                            bpt.setLocation(bpt.getX() - getFont().getSize() - 1, bpt.getY());
                            if (classLabelAngle > Math.PI / 2) {
                                bpt = Geometry.translateByAngle(bpt, classLabelAngle, -labelSize.width);
                            }
                        }
                        gc.setColor(Color.LIGHT_GRAY);
                        drawString(gc, groupName, bpt.getX(), bpt.getY(), classLabelAngle);
                    }

                    Point2D apt = new Point2D.Double(xPos, getHeight() - bottomMargin + 10);
                    final Dimension labelSize = BasicSwing.getStringSize(gc, className, gc.getFont()).getSize();
                    if (classLabelAngle == 0) {
                        apt.setLocation(apt.getX() - labelSize.getWidth() / 2, apt.getY());
                    } else if (classLabelAngle > Math.PI / 2) {
                        apt = Geometry.translateByAngle(apt, classLabelAngle, -labelSize.width);
                    }
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{null, className});
                    if (isSelected) {
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        fillAndDrawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, classLabelAngle, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                    }
                    gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.BLACK));
                    drawString(gc, className, apt.getX(), apt.getY(), classLabelAngle);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                }

                for (String series : pair.getSecond()) {
                    double value = getChartData().getValueAsDouble(series, className);
                    whiskerData.add(value, series);
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
                    whiskerDataTransformed.add(value, series);
                }

                // draw whiskers:
                // coordinates for d-th dataset and c-th class:
                final Color color = getChartColors().getClassColor(class2HigherClassMapper.get(className));
                final Color darkColor = color.darker();

                for (final Pair<Double, String> p : whiskerDataTransformed) {
                    final double value = p.getFirst();
                    final String series = p.getSecond();
                    boolean isSelected2 = isSelected;
                    if (sgc != null) {
                        sgc.setCurrentItem(new String[]{series, className});
                    } else if (!isSelected2)
                        isSelected2 = getChartData().getChartSelection().isSelected(series, null);

                    final int x = xPos + random.nextInt(6) - 3;
                    int height = (int) Math.round(y0 - Math.max(1, value * yFactor));

                    if (isSelected2) {
                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                        gc.fillOval(x - 5, height - 5, 10, 10);
                    }

                    gc.setColor(color);
                    gc.fillOval(x - 1, height - 1, 2, 2);
                    gc.setColor(darkColor);
                    gc.drawOval(x - 1, height - 1, 2, 2);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                }

                gc.setColor(isSelected ? ProgramProperties.SELECTION_COLOR : darkColor);

                final int minHeight = (int) Math.round(y0 - Math.max(1, whiskerDataTransformed.getMin() * yFactor));
                final int quarterHeight = (int) Math.round(y0 - Math.max(1, whiskerDataTransformed.getFirstQuarter() * yFactor));
                final int medianHeight = (int) Math.round(y0 - Math.max(1, whiskerDataTransformed.getMedian() * yFactor));
                final int threeQuaterHeigth = (int) Math.round(y0 - Math.max(1, whiskerDataTransformed.getThirdQuarter() * yFactor));
                final int maxHeight = (int) Math.round(y0 - Math.max(1, whiskerDataTransformed.getMax() * yFactor));

                gc.drawLine(xPos - 4, minHeight, xPos + 4, minHeight);
                gc.drawLine(xPos - 4, maxHeight, xPos + 4, maxHeight);

                gc.drawLine(xPos - 7, quarterHeight, xPos + 7, quarterHeight);
                gc.setStroke(HEAVY_STROKE);
                gc.drawLine(xPos - 6, medianHeight, xPos + 6, medianHeight);
                gc.setStroke(NORMAL_STROKE);
                gc.drawLine(xPos - 7, threeQuaterHeigth, xPos + 7, threeQuaterHeigth);
                gc.drawLine(xPos - 7, quarterHeight, xPos - 7, threeQuaterHeigth);
                gc.drawLine(xPos + 7, quarterHeight, xPos + 7, threeQuaterHeigth);

                gc.setStroke(dotted);
                gc.drawLine(xPos, minHeight, xPos, quarterHeight);
                gc.drawLine(xPos, maxHeight, xPos, threeQuaterHeigth);
                gc.setStroke(NORMAL_STROKE);

                if (sgc != null)
                    sgc.clearCurrentItem();

                if (showValues || isSelected) {
                    String label = "" + (int) whiskerData.getMedian();
                    valuesList.add(new DrawableValue(label, xPos - 4, medianHeight - 1, isSelected));
                    if (minHeight > medianHeight) {
                        label = "" + (int) whiskerData.getMin();
                        valuesList.add(new DrawableValue(label, xPos - 4, minHeight + getFont().getSize() + 1, isSelected));
                    }
                    if (medianHeight - getFont().getSize() > maxHeight) {
                        label = "" + (int) whiskerData.getMax();
                        valuesList.add(new DrawableValue(label, xPos - 4, maxHeight - 1, isSelected));
                    }
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
    }

    private String cleanSeriesName(String series) {
        if (series.endsWith(".rma") || series.endsWith(".rma2") || series.endsWith(".rma6") || series.endsWith(".daa"))
            return series.substring(0, series.lastIndexOf("."));
        else
            return series;
    }


    @Override
    public String getChartDrawerName() {
        return NAME;
    }

    public void updateView() {
        System.err.println("UpdateView");
    }
}
