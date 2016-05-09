/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.daa.connector;

import megan.data.IClassificationBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * implements a classification block for DAA
 * Created by huson on 5/16/14.
 */
public class ClassificationBlockDAA implements IClassificationBlock {
    private final Map<Integer, Integer> classId2Weight;
    private String classificationName;

    public ClassificationBlockDAA(String classificationName) {
        this.classificationName = classificationName;
        classId2Weight = new HashMap<>();
    }

    public int getSum(Integer key) {
        Integer value = classId2Weight.get(key);
        return value == null ? 0 : value;
    }

    public int getWeightedSum(Integer key) {
        return classId2Weight.get(key);
    }

    public void setSum(Integer key, int num) {
        classId2Weight.put(key, num);
    }

    public String getName() {
        return classificationName;
    }

    public void setName(String name) {
        classificationName = name;
    }

    public Set<Integer> getKeySet() {
        return classId2Weight.keySet();
    }
}
