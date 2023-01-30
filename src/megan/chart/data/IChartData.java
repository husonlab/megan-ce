/*
 * IChartData.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.chart.data;

import jloda.util.Pair;

import java.util.Collection;
import java.util.Map;

/**
 * chart data interface
 * Daniel Huson, 5.2012
 */
public interface IChartData extends IData {
    void setAllSeries(Collection<String> allSeries);

    void setAllSeriesTotalSizes(float... sizes);

    String[] getSeriesNamesIncludingDisabled();

    boolean hasTotalSize();

    boolean isUseTotalSize();

    void setUseTotalSize(boolean value);

    int getNumberOfClasses();

    void setEnabledClassNames(Collection<String> classNames);

    Collection<String> getClassNames();

    String[] getClassNamesIncludingDisabled();

    void setClassNames(Collection<String> classNames);

    Number getValue(String series, String className);

    double getValueAsDouble(String series, String className);

    double getTotalForSeries(String series);

    double getTotalForClass(String className);

    void setDataForSeries(String series, Map<String, Number> classes2values);

    Pair<Number, Number> getRange();

    Pair<Number, Number> getRange(String series);

    void putValue(String series, String className, Number value);

    double getMaxTotalSeries();

    double getMaxTotalClass();

    Map<String, Number> getDataForSeries(String series);

    double getTotalForSeriesIncludingDisabledAttributes(String series);

    double getTotalForClassIncludingDisabledSeries(String className);
}
