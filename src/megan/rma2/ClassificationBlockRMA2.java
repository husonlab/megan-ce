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
package megan.rma2;

import jloda.util.Pair;
import megan.data.IClassificationBlock;
import megan.io.IInputReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * classification representation of RMA2
 * Daniel Huson, 9.2010
 */
public class ClassificationBlockRMA2 implements IClassificationBlock {
    private final String classificationName;

    private final Map<Integer, Pair<Integer, Long>> id2SumAndPos = new HashMap<>();
    private final Map<Integer, Integer> id2WeightedSum = new HashMap<>();

    /**
     * constructor
     *
     * @param classificationName
     */
    public ClassificationBlockRMA2(String classificationName) {
        this.classificationName = classificationName;
    }


    /**
     * get the number associated with a key
     *
     * @param key
     * @return number
     */
    public int getSum(Integer key) {
        Pair<Integer, Long> pair = id2SumAndPos.get(key);
        if (pair == null)
            return 0;
        else
            return pair.getFirst();
    }

    /**
     * get the number associated with a key
     *
     * @param key
     * @return number
     */
    public float getWeightedSum(Integer key) {
        Integer value = id2WeightedSum.get(key);
        return Objects.requireNonNullElse(value, 0);
    }

    /**
     * set the weighted sum
     *
     * @param key
     * @param sum
     */
    public void setWeightedSum(Integer key, float sum) {
        //throw new RuntimeException("Not implemented");
    }

    /**
     * set the number associated with a key -> just set not written to disk
     *
     * @param key
     * @param sum
     */
    public void setSum(Integer key, int sum) {
        Pair<Integer, Long> pair = id2SumAndPos.get(key);
        if (pair == null) {
            pair = new Pair<>();
            id2SumAndPos.put(key, pair);
        }
        pair.setFirst(sum);
    }

    public long getPos(Integer key) {
        Pair<Integer, Long> pair = id2SumAndPos.get(key);
        if (pair == null)
            return 0;
        else
            return pair.getSecond();
    }

    public void setPos(Integer key, long pos) {
        Pair<Integer, Long> pair = id2SumAndPos.get(key);
        if (pair == null) {
            pair = new Pair<>();
            id2SumAndPos.put(key, pair);
        }
        pair.setSecond(pos);
    }

    private void setSumAndPos(Integer key, int sum, long pos) {
        id2SumAndPos.put(key, new Pair<>(sum, pos));

    }

    /**
     * get the name of this classification
     *
     * @return name
     */
    public String getName() {
        return classificationName;
    }

    /**
     * set the name of this classification
     *
     * @param name
     */
    public void setName(String name) {
    }


    public Set<Integer> getKeySet() {
        return id2SumAndPos.keySet();
    }

    /**
     * read in the classification block from a file
     *
     * @return classes read
     * @throws java.io.IOException
     */
    public int load(IInputReader r) throws IOException {
        id2SumAndPos.clear();
        try (r) {
            int numberOfClasses = 0;
            while (r.getPosition() < r.length()) {
                int classId = r.readInt();
                int count = r.readInt();
                if (count < 0) {
                    setWeightedSum(classId, -count);
                    count = r.readInt();
                } else
                    setWeightedSum(classId, count);
                long pos = r.readLong();
                setSumAndPos(classId, count, pos);
                numberOfClasses++;
            }
            // System.err.println("Loaded:\n"+toString());

            return numberOfClasses;
        }
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Classification ").append(getName()).append(":\n");
        for (Integer key : id2SumAndPos.keySet()) {
            buf.append(key).append(" -> ").append(id2SumAndPos.get(key)).append("\n");
        }
        return buf.toString();
    }

    /**
     * gets the count and pos for the given class in the given classification
     *
     * @param rma2File
     * @param classification
     * @param classId
     * @return (count, pos) or null
     * @throws java.io.IOException
     */
    public static Pair<Integer, Long> getCountAndPos(RMA2File rma2File, String classification, int classId) throws IOException {

        try (IInputReader r = rma2File.getClassificationIndexReader(classification)) {
            int size = rma2File.getClassificationSize(classification);
            for (int i = 0; i < size; i++) {
                int id = r.readInt();
                int count = r.readInt();
                if (count < 0) {
                    count = r.readInt();
                }
                long pos = r.readLong();
                if (id == classId)
                    return new Pair<>(count, pos);
            }
            return null;
        }
    }

    /**
     * gets the count and pos map for the classification
     *
     * @param rma2File
     * @param classification
     * @return (count, pos) or null
     * @throws java.io.IOException
     */
    public static Map<Integer, Pair<Integer, Long>> getCountAndPos(RMA2File rma2File, String classification) throws IOException {

        Map<Integer, Pair<Integer, Long>> map = new HashMap<>();
        try (IInputReader r = rma2File.getClassificationIndexReader(classification)) {
            int size = rma2File.getClassificationSize(classification);
            for (int i = 0; i < size; i++) {
                int id = r.readInt();
                int count = r.readInt();
                if (count < 0) {
                    count = r.readInt();
                }
                long pos = r.readLong();
                map.put(id, new Pair<>(count, pos));
            }
        }
        return map;
    }
}
