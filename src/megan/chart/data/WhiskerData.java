/*
 *  Copyright (C) 2015 Daniel H. Huson
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


import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * whisker plot data
 * Daniel Huson, 7.2016
 */
public class WhiskerData {
    private final SortedSet<Double> values = new TreeSet<>();
    private Double[] array;

    public void clear() {
        values.clear();
        array = null;
    }

    public void add(Double a) {
        values.add(a);
        array = null;
    }

    public double getMin() {
        return values.size() > 0 ? values.first() : 0;
    }

    public double getMax() {
        return values.size() > 0 ? values.last() : 0;
    }

    public double getFirstQuarter() {
        if (values.size() == 0)
            return 0;
        ensureArray();
        return array[array.length / 4];
    }

    public double getThirdQuarter() {
        if (values.size() == 0)
            return 0;
        ensureArray();
        return array[(3 * array.length) / 4];
    }

    public double getMedian() {
        if (values.size() == 0)
            return 0;
        ensureArray();
        return array[array.length / 2];
    }

    public Iterator<Double> iterator() {
        return values.iterator();
    }

    private void ensureArray() {
        if (array == null) {
            synchronized (values) {
                if (array == null) {
                    array = values.toArray(new Double[values.size()]);
                }
            }
        }
    }
}
