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
package megan.io;

import jloda.util.FileLineIterator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * map-based int getter
 * Daniel 4.2015
 */
public class IntFileGetterHashMap implements IIntGetter {
    private final Map<Long, Integer> map;
    private long limit;

    public IntFileGetterHashMap(File file) throws IOException {
        map = new HashMap<>();

        final FileLineIterator it = new FileLineIterator(file.getPath());
        while (it.hasNext()) {
            String aLine = it.next().trim();
            if (!aLine.startsWith("#")) {
                int pos = aLine.indexOf('\t');
                if (pos > 0) {
                    long key = Long.parseLong(aLine.substring(0, pos));
                    if (key + 1 >= limit)
                        limit = key + 1;
                    map.put(key, Integer.parseInt(aLine.substring(pos + 1)));
                }
            }
        }
        it.close();
    }

    /**
     * gets value for given index
     *
     * @param index
     * @return value or 0
     */
    @Override
    public int get(long index) {
        Integer value = map.get(index);
        return Objects.requireNonNullElse(value, 0);

    }

    /**
     * length of array
     *
     * @return array length
     * @throws java.io.IOException
     */
    @Override
    public long limit() {
        return limit;
    }

    /**
     * close the array
     */
    @Override
    public void close() {

    }
}
