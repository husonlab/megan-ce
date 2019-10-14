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

import jloda.util.Basic;
import jloda.util.FileLineIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * map based int putter
 * Daniel Huson, 4.2015
 */
public class IntMapPutter implements IIntGetter, IIntPutter {
    private final Map<Long, Integer> map;
    private long limit;
    private final File file;
    private boolean mustWriteOnClose = false;

    /**
     * constructor
     *
     * @param file
     * @throws IOException
     */
    public IntMapPutter(File file, boolean loadFileIfExists) throws IOException {
        this.file = file;
        map = new HashMap<>();

        if (loadFileIfExists && file.exists()) {
            final FileLineIterator it = new FileLineIterator(file.getPath());
            while (it.hasNext()) {
                String aLine = it.next().trim();
                if (!aLine.startsWith("#")) {
                    int pos = aLine.indexOf('\t');
                    if (pos > 0) {
                        long index = Long.parseLong(aLine.substring(0, pos));
                        if (index + 1 >= limit)
                            limit = index + 1;
                        map.put(index, Integer.parseInt(aLine.substring(pos + 1)));
                    }
                }
            }
            it.close();
        }
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
     * puts value for given index
     *
     * @param index
     * @param value
     */
    @Override
    public void put(long index, int value) {
        if (index + 1 >= limit)
            limit = index + 1;
        map.put(index, value);
        if (!mustWriteOnClose)
            mustWriteOnClose = true;
    }

    /**
     * length of array
     * todo: limit can be incorrect if getMap() was used to change values
     *
     * @return array length
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
        if (mustWriteOnClose) {
            System.err.println("Writing file: " + file);
            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                for (Long key : map.keySet()) {
                    w.write(key + "\t" + map.get(key) + "\n");
                }
            } catch (IOException ex) {
                Basic.caught(ex);
            }
        }
    }

}
