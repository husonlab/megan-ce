/*
 * IPlot2DData.java Copyright (C) 2022 Daniel H. Huson
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

/**
 * chart plot2D data interface
 * Daniel Huson, 5.2012
 */
public interface IPlot2DData extends IData {

    void setSeriesNames(Collection<String> series);

    void addSeriesName(String name);

    Collection<Pair<Number, Number>> getDataForSeries(String series);

    void setDataForSeries(String series, Collection<Pair<Number, Number>> dataXY);

    Pair<Number, Number> getRangeX();

    Pair<Number, Number> getRangeX(String series);

    Pair<Number, Number> getRangeY();

    Pair<Number, Number> getRangeY(String series);

    void addValue(String series, Number x, Number y);
}
