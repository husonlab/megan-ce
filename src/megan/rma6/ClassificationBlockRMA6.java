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
package megan.rma6;

import jloda.util.ListOfLongs;
import megan.data.IClassificationBlock;
import megan.io.IInputReader;
import megan.io.IOutputWriter;
import megan.io.InputReader;
import megan.io.OutputWriterHumanReadable;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * implements a classification block
 * Created by huson on 5/16/14.
 */
public class ClassificationBlockRMA6 implements IClassificationBlock {
    private final Map<Integer, Integer> map2Weight = new HashMap<>();
    private String classificationName;

    public ClassificationBlockRMA6(String classificationName) {
        this.classificationName = classificationName;
    }

    public int getSum(Integer key) {
        Integer sum = map2Weight.get(key);
        return sum != null ? sum : 0;
    }

    public int getWeightedSum(Integer key) {
        return map2Weight.get(key);
    }

    public void setSum(Integer key, int num) {
        map2Weight.put(key, num);
    }

    public String getName() {
        return classificationName;
    }

    public void setName(String name) {
        classificationName = name;
    }

    public Set<Integer> getKeySet() {
        return map2Weight.keySet();
    }

    /**
     * write to file
     *
     * @param writer
     * @param classId2locations
     * @throws IOException
     */
    public void write(IOutputWriter writer, Map<Integer, ListOfLongs> classId2locations) throws IOException {
        writer.writeInt(map2Weight.size());
        for (Object key : map2Weight.keySet()) {
            writer.writeInt((Integer) key); // class id
            final Integer sum = map2Weight.get(key);
            writer.writeInt(sum); //weight
            if (classId2locations != null) {
                final ListOfLongs list = classId2locations.get(key);
                writer.writeInt(list.size());
                for (int i = 0; i < list.size(); i++)
                    writer.writeLong(list.get(i));
            } else
                writer.writeInt(0);

        }
    }

    /**
     * reads the named classification block
     *
     * @param reader
     * @return size
     * @throws IOException
     */
    public int read(long position, IInputReader reader) throws IOException {
        map2Weight.clear();

        reader.seek(position);
        final int numberOfClasses = reader.readInt();
        for (int i = 0; i < numberOfClasses; i++) {
            final int classId = reader.readInt();
            final int weight = reader.readInt();
            final int count = reader.readInt();
            reader.skipBytes(count * 8); // skip all locations, 8 bytes each
            map2Weight.put(classId, weight);
        }
        return map2Weight.size();
    }

    /**
     * reads the named classification block
     *
     * @param reader
     * @return size
     * @throws IOException
     */
    public int read(long position, InputReader reader, int classId) throws IOException {
        reader.seek(position);
        map2Weight.clear();

        final int numberOfClasses = reader.readInt();
        for (int i = 0; i < numberOfClasses; i++) {
            final int currentId = reader.readInt();
            final int weight = reader.readInt();
            final int count = reader.readInt();
            reader.skipBytes(count * 8); // skip all locations, 8 bytes each
            if (currentId == classId) {
                map2Weight.put(currentId, weight);
                break;
            }
        }
        return map2Weight.size();
    }

    /**
     * read all locations for a given class and adds them to list
     *
     * @param reader
     * @param classId
     * @param list
     * @return list of locations
     * @throws IOException
     */
    public int readLocations(long position, IInputReader reader, int classId, ListOfLongs list) throws IOException {
        reader.seek(position);
        final int numberOfClasses = reader.readInt();
        for (int i = 0; i < numberOfClasses; i++) {
            final int currentId = reader.readInt();
            reader.readInt(); // weight
            final int count = reader.readInt();
            if (currentId == classId) {
                for (int z = 0; z < count; z++) {
                    list.add(reader.readLong());
                }
            } else
                reader.skipBytes(count * 8); // skip all locations, 8 bytes each

        }
        return list.size();
    }

    /**
     * human readable representation
     *
     * @return string
     */
    public String toString() {
        final IOutputWriter w = new OutputWriterHumanReadable(new StringWriter());
        try {
            // w.writeString(classificationType.toString()+":\n");
            write(w, null);
        } catch (IOException e) {
        }
        return w.toString();
    }
}
