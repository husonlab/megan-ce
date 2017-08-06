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

package megan.biom.biom2;

import java.util.HashMap;
import java.util.Map;

public class Biom2Sample {
    private String name;
    private final Map<Integer, Float> classId2Count;
    private final HashMap<String, String> metaDataMap;

    public Biom2Sample() {
        classId2Count = new HashMap<>();
        metaDataMap = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Integer, Float> getClassId2Count() {
        return classId2Count;
    }

    public HashMap<String, String> getMetaDataMap() {
        return metaDataMap;
    }
}
