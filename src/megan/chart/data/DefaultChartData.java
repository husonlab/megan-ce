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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * maintains data used by charts
 * Daniel Huson, 5.2012
 */
public class DefaultChartData implements IChartData {
    private final ChartSelection chartSelection;
    private String dataSetName;

    private String seriesLabel;
    private String classesLabel;
    private String countsLabel;

    private final ArrayList<String> seriesNames;

    private final Map<String, Float> series2TotalSize; // maps to total size

    private final Collection<String> classNames;

    private final Map<String, Map<String, Number>> series2Class2Values;

    private final Map<String, Double> series2size; // maps to size associated with selected data

    private final Map<String, Double> classes2size;

    private final Map<String, Pair<Number, Number>> series2Range;
    private Pair<Number, Number> range;

    private final Map<String, String> samplesTooltips;
    private final Map<String, String> classesTooltips;

    private boolean useTotalSize = false;

    private PhyloTree tree;

    /**
     * constructor
     */
    public DefaultChartData() {
        chartSelection = new ChartSelection();
        seriesNames = new ArrayList<>();
        series2TotalSize = new HashMap<>();
        classNames = new ArrayList<>();
        series2Class2Values = new HashMap<>();
        series2size = new HashMap<>();
        classes2size = new HashMap<>();
        series2Range = new HashMap<>();
        samplesTooltips = new HashMap<>();
        classesTooltips = new HashMap<>();
    }

    /**
     * set dataset name
     *
     * @param name
     */
    public void setDataSetName(String name) {
        this.dataSetName = name;
    }

    /**
     * get dataset name
     *
     * @return series label
     */
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

    /**
     * get the number of datasets
     *
     * @return
     */
    public int getNumberOfSeries() {
        return seriesNames.size();
    }

    /**
     * get all series names including those currently disabled
     *
     * @return names
     */
    public String[] getSeriesNamesIncludingDisabled() {
        return (new ArrayList<>(series2size.keySet())).toArray(new String[series2size.size()]);
    }

    /**
     * get all series names
     *
     * @return names
     */
    public Collection<String> getSeriesNames() {
        return seriesNames;
    }

    /**
     * set the collection of series names in the order that they should appear
     *
     * @param allSeries
     */
    public void setAllSeries(Collection<String> allSeries) {
        this.seriesNames.clear();
        this.seriesNames.addAll(allSeries);
    }

    /**
     * set the total size for each series. This is needed when normalizing by the total number of reads in samples
     *
     * @param sizes
     */
    public void setAllSeriesTotalSizes(float... sizes) {
        for (int i = 0; i < sizes.length; i++)
            this.series2TotalSize.put(seriesNames.get(i), sizes[i]);
    }

    /**
     * get the number of classes
     *
     * @return number of classes
     */
    public int getNumberOfClasses() {
        return classNames.size();
    }

    /**
     * get  the collection of class names in the order that they should appear
     *
     * @return
     */
    public Collection<String> getClassNames() {
        return classNames;
    }

    /**
     * set the   collection of class names in the order that they should appear
     *
     * @param classNames
     */
    public void setClassNames(Collection<String> classNames) {
        this.classNames.clear();
        this.classNames.addAll(classNames);
    }

    /**
     * gets the value for the given series and className
     *
     * @param series
     * @param className
     * @return number or null
     */
    public Number getValue(String series, String className) {
        Map<String, Number> class2Values = series2Class2Values.get(series);
        if (class2Values == null)
            return 0;
        else {
            Number value = class2Values.get(className);
            if (value == null)
                return 0;
            else
                return value.doubleValue();
        }
    }

    /**
     * gets the value for the given series and className
     *
     * @param series
     * @param className
     * @return number or null
     */
    public double getValueAsDouble(String series, String className) {
        Map<String, Number> class2Values = series2Class2Values.get(series);
        if (class2Values == null)
            return 0;
        Number value = class2Values.get(className);
        return value == null ? 0 : value.doubleValue();
    }

    /**
     * set the data for a specific data set
     *
     * @param series
     * @param classes2values
     */
    public void setDataForSeries(String series, Map<String, Number> classes2values) {
        series2Class2Values.put(series, classes2values);
        Number min = null;
        Number max = null;
        double total = 0;
        if (classes2values != null) {
            for (Number value : classes2values.values()) {
                if (min == null) {
                    min = value;
                    max = value;
                } else {
                    if (value.doubleValue() < min.doubleValue())
                        min = value;
                    if (value.doubleValue() > max.doubleValue())
                        max = value;
                }
                total += value.doubleValue();
            }
            series2Range.put(series, new Pair<>(min, max));
            if (range == null)
                range = new Pair<>(min, max);
            else if (min != null) {
                if (min.doubleValue() < range.get1().doubleValue())
                    range.set1(min);
                if (max.doubleValue() > range.get2().doubleValue())
                    range.set2(max);
            }
            series2size.put(series, total);
            samplesTooltips.put(series, String.format("%s: %.0f", series, series2size.get(series)));
            for (Map.Entry<String, Number> entry : classes2values.entrySet()) {
                String className = entry.getKey();
                Number value = entry.getValue();
                Number previous = classes2size.get(className);
                classes2size.put(className, previous == null ? value.doubleValue() : previous.doubleValue() + value.doubleValue());
            }
            for (String className : classes2size.keySet()) {
                classesTooltips.put(className, String.format("%s: %.0f", className, classes2size.get(className)));
            }
        }
    }

    public Map<String, Number> getDataForSeries(String series) {
        return series2Class2Values.get(series);
    }

    /**
     * get the range (min,max) of all values
     *
     * @return range
     */
    public Pair<Number, Number> getRange() {
        return range;
    }

    /**
     * gets the range of values for the given dataset
     *
     * @param series
     * @return
     */
    public Pair<Number, Number> getRange(String series) {
        return series2Range.get(series);
    }

    /**
     * erase
     */
    public void clear() {
        chartSelection.clearSelectionClasses();
        chartSelection.clearSelectionSeries();
        series2Class2Values.clear();
        series2Range.clear();
        series2size.clear();
        classes2size.clear();
        samplesTooltips.clear();
        classesTooltips.clear();
        range = null;
    }

    /**
     * put a data point
     *
     * @param series
     * @param className
     * @param value
     */
    public void putValue(String series, String className, Number value) {
        if (value == null)
            value = 0;
        Map<String, Number> class2value = series2Class2Values.computeIfAbsent(series, k -> new HashMap<>());
        class2value.put(className, value);
        Pair<Number, Number> range = getRange(series);
        if (range == null) {
            range = new Pair<>(value, value);
            series2Range.put(series, range);
        } else {
            if (value.doubleValue() < range.get1().doubleValue())
                range.set1(value);
            if (value.doubleValue() > range.get2().doubleValue())
                range.set2(value);
        }
        Pair<Number, Number> wholeRange = getRange();
        if (wholeRange == null) {
            this.range = new Pair<>(value, value);
        } else {
            if (value.doubleValue() < wholeRange.get1().doubleValue())
                wholeRange.set1(value);
            if (value.doubleValue() > wholeRange.get2().doubleValue())
                wholeRange.set2(value);
        }
        Double previous = series2size.get(series);
        series2size.put(series, previous == null ? value.doubleValue() : previous + value.doubleValue());
        samplesTooltips.put(series, String.format("%s: %.0f", series, series2size.get(series)));
        previous = classes2size.get(className);
        classes2size.put(className, previous == null ? value.doubleValue() : previous + value.doubleValue());
        classesTooltips.put(className, String.format("%s: %.0f", className, classes2size.get(className)));
    }

    public double getTotalForSeries(String series) {
        if (isUseTotalSize())
            return series2TotalSize.get(series);
        else
            return series2size.get(series);
    }

    public double getTotalForSeriesIncludingDisabledAttributes(String series) {
        if (isUseTotalSize())
            return series2TotalSize.get(series);
        else {
            double total = 0;
            for (String className : classes2size.keySet()) {
                Number value = getValue(series, className);
                if (value != null)
                    total += value.doubleValue();
            }
            return total;
        }
    }

    public double getTotalForClass(String className) {
        Double value = classes2size.get(className);
        return value == null ? 0 : value;
    }

    public double getTotalForClassIncludingDisabledSeries(String className) {
        double total = 0;
        for (String series : series2size.keySet()) {
            total += getValue(series, className).doubleValue();
        }
        return total;
    }

    public double getMaxTotalSeries() {
        double max = 0;
        for (String series : seriesNames) {
            Number value = series2size.get(series);
            if (value != null)
                max = Math.max(max, value.doubleValue());
        }
        return max;
    }

    public double getMaxTotalClass() {
        double max = 0;
        for (String className : classNames) {
            Number value = classes2size.get(className);
            if (value != null)
                max = Math.max(max, value.doubleValue());
        }
        return max;
    }

    public void read(Reader r) throws IOException {
        System.err.println("Read data: not implemented");
    }

    public void write(Writer w0) throws IOException {
        BufferedWriter w = new BufferedWriter(w0);

        w.write("#Series:");
        for (String series : getSeriesNames()) {
            w.write("\t");
            w.write(series);
        }
        w.write("\n");
        for (String className : getClassNames()) {
            w.write(className);
            for (String series : getSeriesNames()) {
                w.write("\t" + getValue(series, className).toString());
            }
            w.write("\n");
        }
        w.flush();
    }

    public void setEnabledClassNames(Collection<String> classNames) {
        this.classNames.clear();
        this.classNames.addAll(classNames);

        range = null;

        for (String series : seriesNames) {
            double total = 0;
            for (String className : classNames) {
                Number value = getValue(series, className);
                if (value != null) {
                    total += value.doubleValue();
                    if (range == null) {
                        range = new Pair<>(value, value);
                    } else {
                        if (value.doubleValue() < range.get1().doubleValue())
                            range.set1(value);
                        if (value.doubleValue() > range.get2().doubleValue())
                            range.set2(value);
                    }
                }
                series2size.put(series, total);
            }
        }
    }

    public void setEnabledSeries(Collection<String> seriesNames) {
        this.seriesNames.clear();
        this.seriesNames.addAll(seriesNames);

        range = null;

        for (String className : classNames) {
            double total = 0;
            for (String series : seriesNames) {
                Number value = getValue(series, className);
                if (value != null) {
                    total += value.doubleValue();

                    if (range == null) {
                        range = new Pair<>(value, value);
                    } else {
                        if (value.doubleValue() < range.get1().doubleValue())
                            range.set1(value);
                        if (value.doubleValue() > range.get2().doubleValue())
                            range.set2(value);
                    }
                }
            }
            classes2size.put(className, total);
        }
    }

    public ChartSelection getChartSelection() {
        return chartSelection;
    }

    public Map<String, String> getSamplesTooltips() {
        return samplesTooltips;
    }

    public Map<String, String> getClassesTooltips() {
        return classesTooltips;
    }

    public PhyloTree getTree() {
        return tree;
    }

    public void setTree(PhyloTree tree) {
        this.tree = tree;
    }

    public String[] getClassNamesIncludingDisabled() {
        return classes2size.keySet().toArray(new String[0]);
    }

    public boolean isUseTotalSize() {
        return useTotalSize && hasTotalSize();
    }

    public void setUseTotalSize(boolean useTotalSize) {
        this.useTotalSize = useTotalSize;
    }

    public boolean hasTotalSize() {
        return series2TotalSize.size() == seriesNames.size();
    }
}
