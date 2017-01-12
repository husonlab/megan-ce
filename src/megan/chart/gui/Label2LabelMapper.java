/*
 *  Copyright (C) 2017 Daniel H. Huson
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
package megan.chart.gui;

import java.util.HashMap;
import java.util.Map;

/**
 * default label 2 label mapper
 * Daniel Huson, 7.2012
 */
public class Label2LabelMapper {
    private final Map<String, String> label2label = new HashMap<>();

    /**
     * get mapped label for label
     *
     * @param label
     * @return replacement label
     */
    public String get(String label) {
        String result = label2label.get(label);
        if (result != null)
            return result;
        else
            return label;
    }

    /**
     * set a label 2 label map
     *
     * @param label
     * @param newLabel
     */
    public void put(String label, String newLabel) {
        label2label.put(label, newLabel);
    }

    /**
     * get size
     *
     * @return size
     */
    public int size() {
        return label2label.size();
    }

    /**
     * erase
     */
    public void clear() {
        label2label.clear();
    }
}
