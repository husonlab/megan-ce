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
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import megan.chart.IChartDrawer;
import megan.chart.IMultiChartDrawable;
import megan.chart.data.IChartData;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.*;

/**
 * draws a multi-chart
 * Daniel Huson, 6.2012, 4.2105
 */
public class MultiChartDrawer extends BarChartDrawer implements IChartDrawer {
    private final Dimension panelSize = new Dimension();
    private final Map<String, IMultiChartDrawable> label2drawer = new HashMap<>();
    private final IMultiChartDrawable baseDrawer;
    private boolean labelsAreSeries = false;

    /**
     * constructor
     *
     * @param baseDrawer
     */
    public MultiChartDrawer(IMultiChartDrawable baseDrawer) {
        super();
        this.baseDrawer = baseDrawer;
        setViewer(baseDrawer.getViewer());
        setChartData(baseDrawer.getChartData());
        setClass2HigherClassMapper(baseDrawer.getClass2HigherClassMapper());
        setSeriesLabelGetter(baseDrawer.getSeriesLabelGetter());
        setScalingType(getScalingTypePreference());
        setShowXAxis(getShowXAxisPreference());
        setShowYAxis(getShowYAxisPreference());
    }

    /**
     * draw chart in which colors are by series
     *
     * @param gc
     */
    public void drawChart(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        //if(sgc!=null) lastDown=(Rectangle)sgc.getSelectionRectangle().clone();

        int numberOfPanels = getChartData().getNumberOfSeries();

        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;
        int rows = Math.max(1, (int) Math.sqrt(numberOfPanels));
        int cols = Math.max(1, (int) Math.ceil((double) numberOfPanels / rows));
        panelSize.setSize(getWidth() / cols, (y0 - y1) / rows - 14);

        Font labelFont = getFont("Default");
        AffineTransform transform = gc.getTransform();

        int panel = 0;
        for (String series : getChartData().getSeriesNames()) {
            final int h = (panel % cols) * panelSize.width;
            final int v = (panel / cols) * (panelSize.height + 14) + y1;

            final IMultiChartDrawable chartDrawer = label2drawer.get(series);
            if (chartDrawer == null)
                continue;

            chartDrawer.setMargins(0, 0, 0, 0);
            chartDrawer.setTranspose(true);
            chartDrawer.setScalingType(getScalingType());
            chartDrawer.setShowValues(isShowValues());
            chartDrawer.getChartData().setDataSetName(getChartData().getDataSetName());
            chartDrawer.getChartData().setSeriesLabel(series);
            chartDrawer.getChartData().getChartSelection().clearSelectionClasses();
            chartDrawer.getChartData().getChartSelection().setSelectedClass(getChartData().getChartSelection().getSelectedClasses(), true);
            chartDrawer.getChartData().getChartSelection().clearSelectionSeries();
            chartDrawer.getChartData().getChartSelection().setSelectedSeries(getChartData().getChartSelection().getSelectedSeries(), true);
            chartDrawer.getChartData().getChartSelection().setSelectedBasedOnSeries(getChartData().getChartSelection().isSelectedBasedOnSeries());
            chartDrawer.setValues(baseDrawer);

            chartDrawer.setWidth(panelSize.width);
            chartDrawer.setHeight(panelSize.height);

            chartDrawer.updateView();

            AffineTransform newTransform = (AffineTransform) transform.clone();
            newTransform.translate(h, v);
            gc.setTransform(newTransform);

            Rectangle saveClip = gc.getClipBounds();
            Rectangle newClip = new Rectangle(0, 0, panelSize.width, panelSize.height + 14);
            if (saveClip != null) { // can be null when using selection graphics
                newClip = newClip.intersection(saveClip);
                gc.setClip(newClip);
            }
            if (sgc != null) {
                sgc.setCurrentItem(new String[]{series, null});
                gc.fillRect(0, 0, panelSize.width, panelSize.height + 14);
                sgc.clearCurrentItem();
            }

            chartDrawer.drawChart(gc);
            if (numberOfPanels > 1) {
                gc.setColor(Color.DARK_GRAY);
                gc.setFont(labelFont);
                String label = seriesLabelGetter.getLabel(series);
                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                while (labelSize.width + 2 > panelSize.width && label.length() >= 5) {
                    label = label.substring(0, label.length() - 4) + "...";
                    labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                }
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{series, null});
                gc.drawString(label, (panelSize.width - labelSize.width) / 2, panelSize.height + 12);
                if (sgc != null)
                    sgc.clearCurrentItem();
            }
            if (saveClip != null)
                gc.setClip(saveClip);
            if (numberOfPanels > 1) {
                gc.setColor(Color.LIGHT_GRAY);
                gc.drawRect(0, 0, panelSize.width, panelSize.height + 14);
            }
            if (getChartData().getChartSelection().isSelected(series, null)) {
                gc.setStroke(HEAVY_STROKE);
                gc.setColor(ProgramProperties.SELECTION_COLOR);
                gc.drawRect(0, 0, panelSize.width, panelSize.height + 14);
                gc.setStroke(NORMAL_STROKE);
            }

            panel++;
        }
        gc.setTransform(transform);

        if (sgc == null && lastDown != null) {
            gc.setColor(Color.green);
            gc.draw(lastDown);
        }
    }

    /**
     * draw chart with colors representing classes
     *
     * @param gc
     */
    public void drawChartTransposed(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        int numberOfPanels = getChartData().getNumberOfClasses();

        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;
        int rows = Math.max(1, (int) Math.sqrt(numberOfPanels));
        int cols = Math.max(1, (int) Math.ceil((double) numberOfPanels / rows));
        panelSize.setSize(getWidth() / cols, (y0 - y1) / rows - 14);

        Font labelFont = getFont("Default");
        AffineTransform transform = gc.getTransform();

        int panel = 0;

        for (String className : getChartData().getClassNames()) {
            final int h = (panel % cols) * panelSize.width;
            final int v = (panel / cols) * (panelSize.height + 14) + y1;

            final IMultiChartDrawable chartDrawer = label2drawer.get(className);
            if (chartDrawer == null)
                continue;

            chartDrawer.setMargins(0, 0, 0, 0);
            chartDrawer.setTranspose(false);
            chartDrawer.setScalingType(getScalingType());
            chartDrawer.setShowValues(isShowValues());
            chartDrawer.getChartData().setSeriesLabel(chartData.getSeriesLabel());
            chartDrawer.getChartData().setClassesLabel(chartData.getClassesLabel());
            chartDrawer.getChartData().setCountsLabel(chartData.getCountsLabel());
            chartDrawer.getChartData().setDataSetName(getChartData().getDataSetName());
            chartDrawer.getChartData().getChartSelection().clearSelectionClasses();
            chartDrawer.getChartData().getChartSelection().setSelectedClass(getChartData().getChartSelection().getSelectedClasses(), true);
            chartDrawer.getChartData().getChartSelection().clearSelectionSeries();
            chartDrawer.getChartData().getChartSelection().setSelectedSeries(getChartData().getChartSelection().getSelectedSeries(), true);
            chartDrawer.getChartData().getChartSelection().setSelectedBasedOnSeries(getChartData().getChartSelection().isSelectedBasedOnSeries());
            chartDrawer.setWidth(panelSize.width);
            chartDrawer.setHeight(panelSize.height);
            chartDrawer.setValues(baseDrawer);

            chartDrawer.updateView();

            AffineTransform newTransform = (AffineTransform) transform.clone();
            newTransform.translate(h, v);
            gc.setTransform(newTransform);

            Rectangle saveClip = gc.getClipBounds();
            Rectangle newClip = new Rectangle(0, 0, panelSize.width, panelSize.height + 14);
            if (saveClip != null) {
                newClip = newClip.intersection(saveClip);
                gc.setClip(newClip);
            }
            chartDrawer.drawChartTransposed(gc);
            if (numberOfPanels > 1) {
                gc.setColor(Color.DARK_GRAY);
                gc.setFont(labelFont);
                String label = className;
                Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                while (labelSize.width + 2 > panelSize.width && label.length() >= 5) {
                    label = className.substring(0, label.length() - 4) + "...";
                    labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                }
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{null, className});
                gc.drawString(label, (panelSize.width - labelSize.width) / 2, panelSize.height + 12);
                if (sgc != null)
                    sgc.clearCurrentItem();
            }
            if (saveClip != null)
                gc.setClip(saveClip);
            if (numberOfPanels > 1) {
                gc.setColor(Color.LIGHT_GRAY);
                gc.drawRect(0, 0, panelSize.width, panelSize.height + 14);
            }
            if (getChartData().getChartSelection().isSelected(null, className)) {
                gc.setStroke(HEAVY_STROKE);
                gc.setColor(ProgramProperties.SELECTION_COLOR);
                gc.drawRect(0, 0, panelSize.width, panelSize.height + 14);
                gc.setStroke(NORMAL_STROKE);
            }
            if (sgc != null) {
                sgc.setCurrentItem(new String[]{null, className});
                gc.fillRect(0, 0, panelSize.width, panelSize.height + 14);
                sgc.clearCurrentItem();
            }

            panel++;
        }
        gc.setTransform(transform);
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
    }

    public void updateView() {
        if (!isTranspose()) {
            if (!labelsAreSeries) {
                label2drawer.clear();
                labelsAreSeries = true;
            }

            Set<String> toDelete = new HashSet<>();
            for (String label : label2drawer.keySet()) {
                if (!getChartData().getSeriesNames().contains(label)) {
                    toDelete.add(label);
                    label2drawer.get(label).close();
                }
            }
            label2drawer.keySet().removeAll(toDelete);
            for (String label : getChartData().getSeriesNames()) {
                IMultiChartDrawable chartDrawer = label2drawer.get(label);
                if (chartDrawer == null) {
                    chartDrawer = baseDrawer.createInstance();
                    label2drawer.put(label, chartDrawer);
                }
                chartDrawer.setWidth(panelSize.width);
                chartDrawer.setHeight(panelSize.height);
                chartDrawer.setGraphics(getGraphics());
                chartDrawer.setValues(baseDrawer);

                chartDrawer.setChartTitle(getChartTitle());
                ((IChartData) chartDrawer.getChartData()).setDataForSeries(label, getChartData().getDataForSeries(label));
                ((IChartData) chartDrawer.getChartData()).setEnabledClassNames(getChartData().getClassNames());
                chartDrawer.getChartData().setEnabledSeries(Collections.singletonList(label));
                for (String target : fonts.keySet()) {
                    Pair<Font, Color> pair = fonts.get(target);
                    chartDrawer.setFont(target, pair.get1(), pair.get2());
                }
            }
        } else {
            if (labelsAreSeries) {
                label2drawer.clear();
                labelsAreSeries = false;
            }
            Set<String> toDelete = new HashSet<>();
            for (String label : label2drawer.keySet()) {
                if (!getChartData().getClassNames().contains(label)) {
                    toDelete.add(label);
                    label2drawer.get(label).close();
                }
            }
            label2drawer.keySet().removeAll(toDelete);
            for (String label : getChartData().getClassNames()) {
                IMultiChartDrawable chartDrawer = label2drawer.get(label);
                if (chartDrawer == null) {
                    chartDrawer = baseDrawer.createInstance();
                    label2drawer.put(label, chartDrawer);
                }
                chartDrawer.setWidth(panelSize.width);
                chartDrawer.setHeight(panelSize.height);
                chartDrawer.setGraphics(getGraphics());
                chartDrawer.setValues(baseDrawer);

                chartDrawer.setChartTitle(getChartTitle());
                for (String series : getChartData().getSeriesNames()) {
                    ((IChartData) chartDrawer.getChartData()).putValue(series, label, getChartData().getValue(series, label));
                }
                chartDrawer.getChartData().setEnabledSeries(getChartData().getSeriesNames());
                ((IChartData) chartDrawer.getChartData()).setEnabledClassNames(Collections.singletonList(label));
                for (String target : fonts.keySet()) {
                    Pair<Font, Color> pair = fonts.get(target);
                    chartDrawer.setFont(target, pair.get1(), pair.get2());
                }
            }
        }
    }

    public void forceUpdate() {
        for (IMultiChartDrawable drawer : label2drawer.values()) {
            drawer.forceUpdate();
        }
    }

    public boolean canShowLegend() {
        return baseDrawer.canShowLegend();
    }

    public boolean canTranspose() {
        return baseDrawer.canTranspose();
    }

    public boolean canShowXAxis() {
        return false;
    }

    public boolean canShowYAxis() {
        return false;
    }

    public boolean canShowValues() {
        return baseDrawer.canShowValues();
    }

    public void close() {
        for (String label : label2drawer.keySet())
            label2drawer.get(label).close();
    }

    public boolean isXYLocked() {
        return baseDrawer.isXYLocked();
    }

    public IMultiChartDrawable getBaseDrawer() {
        return baseDrawer;
    }

    @Override
    public JToolBar getBottomToolBar() {
        return baseDrawer.getBottomToolBar();
    }

    @Override
    public ChartViewer.ScalingType getScalingTypePreference() {
        return baseDrawer != null ? baseDrawer.getScalingTypePreference() : ChartViewer.ScalingType.LINEAR;
    }

    @Override
    public boolean getShowXAxisPreference() {
        return baseDrawer != null && baseDrawer.getShowXAxisPreference();
    }

    @Override
    public boolean getShowYAxisPreference() {
        return baseDrawer != null && baseDrawer.getShowYAxisPreference();
    }

    @Override
    public String getChartDrawerName() {
        return baseDrawer.getChartDrawerName();
    }

    @Override
    public boolean isSupportedScalingType(ChartViewer.ScalingType scalingType) {
        return baseDrawer.isSupportedScalingType(scalingType);
    }

    @Override
    public void setScalingType(ChartViewer.ScalingType scalingType) {
        if (scalingType != getScalingType()) {
            super.setScalingType(scalingType);
            forceUpdate();
        }
    }
}

