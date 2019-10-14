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
package megan.chart.data;

import jloda.phylo.PhyloTree;
import jloda.util.Pair;
import megan.chart.gui.ChartSelection;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

/**
 * chart plot2D data interface
 * Daniel Huson, 6.2012
 */
public class DefaultPlot2DData implements IPlot2DData {
    private final ChartSelection chartSelection;
    private String dataSetName;

    private String seriesLabel;
    private String classesLabel;
    private String countsLabel;

    private final Collection<String> seriesNames;
    private final Set<String> seriesNamesAsSet;
    private final Map<String, LinkedList<Pair<Number, Number>>> series2DataXY;
    private final Map<String, Pair<Number, Number>> series2RangeX;
    private final Map<String, Pair<Number, Number>> series2RangeY;
    private final Pair<Number, Number> rangeX = new Pair<>(0, 0);
    private final Pair<Number, Number> rangeY = new Pair<>(0, 0);

    private final Map<String, String> seriesToolTips;
    private final Map<String, String> classesToolTips;


    public DefaultPlot2DData() {
        chartSelection = new ChartSelection();
        seriesNames = new LinkedList<>();
        seriesNamesAsSet = new HashSet<>();
        series2DataXY = new HashMap<>();
        series2RangeX = new HashMap<>();
        series2RangeY = new HashMap<>();
        seriesToolTips = new HashMap<>();
        classesToolTips = new HashMap<>();
    }

    public void clear() {
        chartSelection.clearSelectionClasses();
        chartSelection.clearSelectionSeries();
        seriesNames.clear();
        seriesNamesAsSet.clear();
        series2DataXY.clear();
        series2RangeX.clear();
        series2RangeY.clear();
        seriesToolTips.clear();
        classesToolTips.clear();
        rangeX.set(0, 0);
        rangeY.set(0, 0);
    }

    public void setDataSetName(String dataSetName) {
        this.dataSetName = dataSetName;
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public String getSeriesLabel() {
        return seriesLabel;
    }

    public void setSeriesLabel(String seriesLabel) {
        this.seriesLabel = seriesLabel;
    }

    public String getClassesLabel() {
        return classesLabel;
    }

    public void setClassesLabel(String classesLabel) {
        this.classesLabel = classesLabel;
    }

    public String getCountsLabel() {
        return countsLabel;
    }

    public void setCountsLabel(String countsLabel) {
        this.countsLabel = countsLabel;
    }

    public int getNumberOfSeries() {
        return series2DataXY.keySet().size();
    }

    public Collection<String> getSeriesNames() {
        return seriesNames;
    }

    public void setSeriesNames(Collection<String> seriesNames) {
        this.seriesNames.clear();
        this.seriesNamesAsSet.clear();
        for (String name : seriesNames) {
            if (!seriesNamesAsSet.contains(name)) {
                this.seriesNames.add(name);
                this.seriesNamesAsSet.add(name);
            }
        }
    }

    public void addSeriesName(String name) {
        if (!seriesNamesAsSet.contains(name)) {
            this.seriesNames.add(name);
            this.seriesNamesAsSet.add(name);
        }
    }

    public Collection<Pair<Number, Number>> getDataForSeries(String series) {
        return series2DataXY.get(series);
    }

    public void setDataForSeries(String series, Collection<Pair<Number, Number>> dataXY) {
        LinkedList<Pair<Number, Number>> list = new LinkedList<>(dataXY);
        series2DataXY.put(series, list);
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Pair<Number, Number> pair : list) {
            double x = pair.get1().doubleValue();
            double y = pair.get2().doubleValue();
            minX = Math.min(x, minX);
            maxX = Math.max(x, maxX);
            minY = Math.min(y, minY);
            maxY = Math.max(y, maxY);
        }
        series2RangeX.put(series, new Pair<>(minX, maxX));
        series2RangeY.put(series, new Pair<>(minY, maxY));

        if (rangeX.get1().doubleValue() == 0 && rangeX.get2().doubleValue() == 0)
            rangeX.set(minX, maxX);
        else
            rangeX.set(Math.min(rangeX.get1().doubleValue(), minX), Math.max(rangeX.get2().doubleValue(), maxX));
        if (rangeY.get1().doubleValue() == 0 && rangeY.get2().doubleValue() == 0)
            rangeY.set(minY, maxY);
        else
            rangeY.set(Math.min(rangeY.get1().doubleValue(), minY), Math.max(rangeY.get2().doubleValue(), maxY));

        addSeriesName(series);

        seriesToolTips.put(series, String.format("%s   X-range: %.1f - %.1f   Y-range: %.1f - %.1f", series, minX, maxX, minY, maxY));

    }

    public Pair<Number, Number> getRangeX() {
        return rangeX;
    }

    public Pair<Number, Number> getRangeX(String series) {
        return series2RangeX.get(series);
    }

    public Pair<Number, Number> getRangeY() {
        return rangeY;
    }

    public Pair<Number, Number> getRangeY(String series) {
        return series2RangeY.get(series);
    }

    public void addValue(String series, Number x, Number y) {
        LinkedList<Pair<Number, Number>> list = series2DataXY.computeIfAbsent(series, k -> new LinkedList<>());
        list.add(new Pair<>(x, y));

        Pair<Number, Number> rangeXd = series2RangeX.get(series);
        if (rangeXd == null)
            series2RangeX.put(series, new Pair<>(x, x));
        else
            rangeXd.set(Math.min(rangeXd.get1().doubleValue(), x.doubleValue()), Math.max(rangeXd.get2().doubleValue(), x.doubleValue()));

        Pair<Number, Number> rangeYd = series2RangeY.get(series);
        if (rangeYd == null)
            series2RangeY.put(series, new Pair<>(y, y));
        else
            rangeYd.set(Math.min(rangeYd.get1().doubleValue(), y.doubleValue()), Math.max(rangeYd.get2().doubleValue(), y.doubleValue()));

        if (rangeX.get1().doubleValue() == 0 && rangeX.get2().doubleValue() == 0)
            rangeX.set(x, x);
        else
            rangeX.set(Math.min(rangeX.get1().doubleValue(), x.doubleValue()), Math.max(rangeX.get2().doubleValue(), x.doubleValue()));
        if (rangeY.get1().doubleValue() == 0 && rangeY.get2().doubleValue() == 0)
            rangeY.set(y, y);

        else
            rangeY.set(Math.min(rangeY.get1().doubleValue(), y.doubleValue()), Math.max(rangeY.get2().doubleValue(), y.doubleValue()));
    }

    public void read(Reader r) throws IOException {
        System.err.println("Read data: not implemented");
    }

    public void write(Writer w0) throws IOException {
        BufferedWriter w = new BufferedWriter(w0);

        for (String series : getSeriesNames()) {
            Collection<Pair<Number, Number>> list = getDataForSeries(series);
            w.write("#Series: " + series + "\n");
            for (Pair<Number, Number> pair : list) {
                w.write(pair.get1() + "\t" + pair.get2() + "\n");
            }
            w.flush();
        }
    }

    public void setEnabledSeries(Collection<String> seriesNames) {
        this.seriesNames.clear();
        this.seriesNames.addAll(seriesNames);

        // need to update the total range of values:
        for (String series : seriesNames) {
            Pair<Number, Number> rangeXd = series2RangeX.get(series);
            Pair<Number, Number> rangeYd = series2RangeY.get(series);
            if (rangeX.get1().doubleValue() == 0 && rangeX.get2().doubleValue() == 0)
                rangeX.set(rangeXd.get1(), rangeXd.get2());
            else
                rangeX.set(Math.min(rangeX.get1().doubleValue(), rangeXd.get1().doubleValue()), Math.max(rangeX.get2().doubleValue(), rangeXd.get2().doubleValue()));
            if (rangeY.get1().doubleValue() == 0 && rangeY.get2().doubleValue() == 0)
                rangeY.set(rangeYd.get1(), rangeYd.get2());
            else
                rangeY.set(Math.min(rangeY.get1().doubleValue(), rangeYd.get1().doubleValue()), Math.max(rangeY.get2().doubleValue(), rangeYd.get2().doubleValue()));
        }
    }

    public ChartSelection getChartSelection() {
        return chartSelection;
    }

    public Map<String, String> getSamplesTooltips() {
        return seriesToolTips;
    }

    public Map<String, String> getClassesTooltips() {
        return classesToolTips;
    }

    public PhyloTree getTree() {
        return null;
    }

    public void setTree(PhyloTree tree) {
    }
}
