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

import jloda.swing.util.Geometry;
import jloda.util.ProgramProperties;
import megan.chart.IChartDrawer;
import megan.chart.IMultiChartDrawable;
import megan.chart.data.DefaultChartData;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;

/**
 * draws a pie chart
 * Daniel Huson, 6.2012
 */
public class PieChartDrawer extends BarChartDrawer implements IChartDrawer, IMultiChartDrawable {
    private static final String NAME = "PieChart";

    private Graphics graphics;
    private int width;
    private int height;

    /**
     * constructor
     */
    public PieChartDrawer() {
        setSupportedScalingTypes(ChartViewer.ScalingType.LINEAR, ChartViewer.ScalingType.PERCENT);
    }

    /**
     * draw a pie in which slices are classes
     *
     * @param gc
     */
    public void drawChart(Graphics2D gc) {
        int x0 = 2;
        int x1 = getWidth() - 2;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;
        if (x0 >= x1)
            return;

        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);

        Rectangle deviceBBox = new Rectangle(x0, y1, x1 - x0, y0 - y1);
        int diameter = Math.min(deviceBBox.width, deviceBBox.height) - 70;
        deviceBBox.x = deviceBBox.x + (deviceBBox.width - diameter) / 2;
        deviceBBox.y = deviceBBox.y + (deviceBBox.height - diameter) / 2;

        if (getChartData().getSeriesNames().size() == 0)
            return; // nothing to draw.

        String series = getChartData().getSeriesNames().iterator().next();

        double factor = 360.0 / getChartData().getTotalForSeriesIncludingDisabledAttributes(series);

        double totalValue = 0;
        Arc2D arc = new Arc2D.Double();
        arc.setArcType(Arc2D.PIE);
        arc.setFrame(deviceBBox.x + 1, deviceBBox.y + 1, diameter, diameter);
        Point center = new Point((int) arc.getFrame().getCenterX(), (int) arc.getFrame().getCenterY());

        gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));

        for (String className : getChartData().getClassNames()) {
            double value = getChartData().getValue(series, className).doubleValue();
            if (value > 0) {
                arc.setAngleStart(totalValue * factor);
                arc.setAngleExtent(value * factor);
                totalValue += value;
                gc.setColor(getChartColors().getClassColor(class2HigherClassMapper.get(className)));
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{series, className});
                gc.fill(arc);
                if (sgc != null)
                    sgc.clearCurrentItem();
                gc.setColor(Color.black);
                gc.draw(arc);
            }
            boolean isSelected = getChartData().getChartSelection().isSelected(null, className);
            if (isShowValues() || isSelected) {
                double textAngle = Geometry.deg2rad(360 - (arc.getAngleStart() + arc.getAngleExtent() / 2));
                Point2D apt = Geometry.translateByAngle(center, textAngle, diameter / 2 + 5);
                if (isSelected)
                    gc.setColor(ProgramProperties.SELECTION_COLOR_ADDITIONAL_TEXT);
                else
                    gc.setColor(getFontColor(ChartViewer.FontKeys.ValuesFont.toString(), Color.DARK_GRAY));
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{series, className});
                drawString(gc, "" + (int) value, apt.getX(), apt.getY(), textAngle);
                if (sgc != null)
                    sgc.clearCurrentItem();
            }
        }
        // now draw all the selected stuff
        if (getChartData().getChartSelection().getSelectedClasses().size() > 0) {
            gc.setStroke(HEAVY_STROKE);
            gc.setColor(ProgramProperties.SELECTION_COLOR);
            totalValue = 0;
            for (String className : getChartData().getClassNames()) {
                double value = getChartData().getValue(series, className).doubleValue();
                arc.setAngleStart(totalValue * factor);
                arc.setAngleExtent(value * factor);
                totalValue += value;
                if (getChartData().getChartSelection().isSelected(null, className)) {
                    gc.draw(arc);
                }
            }
            gc.setStroke(NORMAL_STROKE);
        }
    }

    /**
     * draw a pie in which slices are series
     *
     * @param gc
     */
    public void drawChartTransposed(Graphics2D gc) {
        int x0 = 2;
        int x1 = getWidth() - 2;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;
        if (x0 >= x1)
            return;

        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);

        Rectangle deviceBBox = new Rectangle(x0, y1, x1 - x0, y0 - y1);
        int diameter = Math.min(deviceBBox.width, deviceBBox.height) - 70;
        deviceBBox.x = deviceBBox.x + (deviceBBox.width - diameter) / 2;
        deviceBBox.y = deviceBBox.y + (deviceBBox.height - diameter) / 2;

        if (getChartData().getMaxTotalSeries() <= 0) {
            return; // nothing to draw.
        }
        String className = getChartData().getClassNames().iterator().next();

        double factor = 360.0 / getChartData().getTotalForClassIncludingDisabledSeries(className);

        double totalValue = 0;
        Arc2D arc = new Arc2D.Double();
        arc.setArcType(Arc2D.PIE);
        arc.setFrame(deviceBBox.x + 1, deviceBBox.y + 1, diameter, diameter);
        Point center = new Point((int) arc.getFrame().getCenterX(), (int) arc.getFrame().getCenterY());

        gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));

        for (String series : getChartData().getSeriesNames()) {
            double value = getChartData().getValue(series, className).doubleValue();
            if (value > 0) {
                arc.setAngleStart(totalValue * factor);
                arc.setAngleExtent(value * factor);
                totalValue += value;

                gc.setColor(getChartColors().getSampleColor(series));
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{series, className});
                gc.fill(arc);
                if (sgc != null)
                    sgc.clearCurrentItem();
                gc.setColor(Color.black);
                gc.draw(arc);
                boolean isSelected = getChartData().getChartSelection().isSelected(series, null);
                if (isShowValues() || isSelected) {
                    double textAngle = Geometry.deg2rad(360 - (arc.getAngleStart() + arc.getAngleExtent() / 2));
                    Point2D apt = Geometry.translateByAngle(center, textAngle, diameter / 2 + 5);
                    if (isSelected)
                        gc.setColor(ProgramProperties.SELECTION_COLOR_ADDITIONAL_TEXT);
                    else
                        gc.setColor(getFontColor(ChartViewer.FontKeys.ValuesFont.toString(), Color.DARK_GRAY));
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{series, className});
                    drawString(gc, "" + (int) value, apt.getX(), apt.getY(), textAngle);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                }
            }
        }

        if (chartData.getChartSelection().getSelectedSeries().size() > 0) {
            totalValue = 0;
            gc.setStroke(HEAVY_STROKE);
            gc.setColor(ProgramProperties.SELECTION_COLOR);
            for (String series : getChartData().getSeriesNames()) {
                double value = getChartData().getValue(series, className).doubleValue();
                arc.setAngleStart(totalValue * factor);
                arc.setAngleExtent(value * factor);
                totalValue += value;
                if (getChartData().getChartSelection().isSelected(series, null)) {
                    gc.draw(arc);
                }
            }
            gc.setStroke(NORMAL_STROKE);
        }
    }

    @Override
    public boolean isShowXAxis() {
        return false;
    }

    @Override
    public boolean isShowYAxis() {
        return false;
    }

    public boolean canShowValues() {
        return true;
    }

    /**
     * create a new instance of the given type of drawer, sharing internal data structures
     *
     * @return
     */
    public PieChartDrawer createInstance() {
        final PieChartDrawer drawer = new PieChartDrawer();
        drawer.setViewer(viewer);
        drawer.setChartData(new DefaultChartData());
        drawer.setClass2HigherClassMapper(class2HigherClassMapper);
        drawer.setSeriesLabelGetter(seriesLabelGetter);
        drawer.setExecutorService(executorService);
        return drawer;

    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;

    }

    @Override
    public void setGraphics(Graphics graphics) {
        this.graphics = graphics;
    }

    @Override
    public Graphics getGraphics() {
        return graphics;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public JToolBar getBottomToolBar() {
        return null;
    }

    @Override
    public boolean getShowXAxisPreference() {
        return false;
    }

    @Override
    public boolean getShowYAxisPreference() {
        return false;
    }

    /**
     * copy all user parameters from the given base drawer
     *
     * @param baseDrawer
     */
    @Override
    public void setValues(IMultiChartDrawable baseDrawer) {
    }

    @Override
    public String getChartDrawerName() {
        return NAME;
    }
}
