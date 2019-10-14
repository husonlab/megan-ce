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
package megan.rma3;

import jloda.util.ListOfLongs;
import megan.core.ClassificationType;
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
public class ClassificationBlockRMA3 implements IClassificationBlock {

    public final static String FORMAT = "NumClasses:Integer [ClassId:Integer Count:Integer [Location:Long]*]*";
    // number of classes, the for each class: class-id, count and then all locations in matches section

    private final Map<Integer, Integer> map = new HashMap<>();
    private ClassificationType classificationType;

    public ClassificationBlockRMA3(ClassificationType classificationType) {
        this.classificationType = classificationType;
    }

    @Override
    public int getSum(Integer key) {
        Integer sum = map.get(key);
        return sum != null ? sum : 0;
    }

    @Override
    public void setWeightedSum(Integer key, float num) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public float getWeightedSum(Integer key) {
        return map.get(key);
    }

    @Override
    public void setSum(Integer key, int num) {
        map.put(key, num);

    }

    @Override
    public String getName() {
        return classificationType.toString();
    }

    @Override
    public void setName(String name) {
        classificationType = ClassificationType.valueOf(name);
    }

    @Override
    public Set<Integer> getKeySet() {
        return map.keySet();
    }

    /**
     * write to file
     *
     * @param writer
     * @param classId2locations
     * @throws IOException
     */
    public void write(IOutputWriter writer, Map<Integer, ListOfLongs> classId2locations) throws IOException {
        writer.writeInt(map.size());
        for (Object key : map.keySet()) {
            writer.writeInt((Integer) key); // class id
            final Integer sum = map.get(key);
            writer.writeInt(sum); // count
            if (classId2locations != null) {
                final ListOfLongs list = classId2locations.get(key);
                if (list.size() != sum)
                    throw new IOException("Wrong number of locations: " + list.size() + ", should be: " + sum);
                for (int i = 0; i < list.size(); i++)
                    writer.writeLong(list.get(i));
            }
        }
    }

    /**
     * reads the named classification block
     *
     * @param classificationsFooter
     * @param reader
     * @return size
     * @throws java.io.IOException
     */
    public int read(ClassificationsFooterRMA3 classificationsFooter, IInputReader reader) throws IOException {
        map.clear();

        long start = classificationsFooter.getStart(classificationType);
        if (start != 0) {
            reader.seek(start);
            final int numberOfClasses = reader.readInt();
            for (int i = 0; i < numberOfClasses; i++) {
                int classId = reader.readInt();
                int sum = reader.readInt();
                for (int z = 0; z < 8; z++)
                    reader.skipBytes(sum); // skip all locations, 8 bytes each
                map.put(classId, sum);
            }
        }
        return map.size();
    }

    /**
     * reads the named classification block
     *
     * @param classificationsFooter
     * @param reader
     * @return size
     * @throws java.io.IOException
     */
    public int read(ClassificationsFooterRMA3 classificationsFooter, InputReader reader, int classId) throws IOException {
        map.clear();

        long start = classificationsFooter.getStart(classificationType);
        if (start != 0) {
            final int numberOfClasses = reader.readInt();
            for (int i = 0; i < numberOfClasses; i++) {
                int currentId = reader.readInt();
                int sum = reader.readInt();
                reader.skipBytes(8 * sum); // skip all locations
                if (currentId == classId) {
                    map.put(currentId, sum);
                    break;
                }
            }
        }
        return map.size();
    }

    /**
     * read all locations for a given class and adds the to list
     *
     * @param classificationsFooter
     * @param reader
     * @param classId
     * @param list
     * @return list of locations
     * @throws IOException
     */
    public int readLocations(ClassificationsFooterRMA3 classificationsFooter, IInputReader reader, int classId, ListOfLongs list) throws IOException {
        long start = classificationsFooter.getStart(classificationType);
        if (start != 0) {
            reader.seek(start);
            final int numberOfClasses = reader.readInt();
            for (int i = 0; i < numberOfClasses; i++) {
                int currentId = reader.readInt();
                int sum = reader.readInt();
                if (currentId == classId) {
                    for (int z = 0; z < sum; z++) {
                        list.add(reader.readLong());
                    }
                } else
                    reader.skipBytes(8 * sum); // skip all locations
            }
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
        } catch (IOException ignored) {
        }
        return w.toString();
    }
}
