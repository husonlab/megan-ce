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
package megan.data;

import java.io.IOException;
import java.util.*;

/**
 * list of necessary updates to RMA file
 * Daniel Huson, 1.2009
 */
public class UpdateItemList extends LinkedList<UpdateItem> {
    private final int numberOfClassifications;
    private final Map<Integer, UpdateItem>[] first;
    private final Map<Integer, UpdateItem>[] last;
    private final Map<Integer, Float>[] weights;

    /**
     * constructor
     *
     * @param numberOfClassifications
     */
    @SuppressWarnings("unchecked")
    public UpdateItemList(int numberOfClassifications) {
        super();
        this.numberOfClassifications = numberOfClassifications;
        first = new HashMap[numberOfClassifications];
        last = new HashMap[numberOfClassifications];
        weights = new HashMap[numberOfClassifications];
        for (int i = 0; i < numberOfClassifications; i++) {
            first[i] = new HashMap<>(1000000);
            last[i] = new HashMap<>(1000000);
            weights[i] = new HashMap<>(1000000);
        }
    }

    /**
     * add an item
     *
     * @param readUid
     * @param classIds
     */
    public UpdateItem addItem(final long readUid, float readWeight, final int[] classIds) throws IOException {
        if (classIds.length != numberOfClassifications)
            throw new IOException("classIds has wrong length: " + classIds.length + ", should be: " + numberOfClassifications);
        final UpdateItem item = new UpdateItem(numberOfClassifications);
        item.setReadUId(readUid);
        add(item);

        if (readWeight == 0) {
            // throw new RuntimeException("Internal error: ReadWeight=0");
            readWeight = 1;
        }

        for (int i = 0; i < numberOfClassifications; i++) {
            final int id = classIds[i];
            if (id != 0) {
                item.setClassId(i, id);
                UpdateItem lastInClass = last[i].get(id);

                if (lastInClass == null) {
                    first[i].put(id, item);
                    last[i].put(id, item);
                    weights[i].put(id, readWeight);
                } else {
                    lastInClass.setNextInClassifaction(i, item);
                    last[i].put(id, item);
                    weights[i].put(id, weights[i].get(id) + readWeight);
                }
            }
        }
        return item;
    }

    /**
     * get the weighted size of a class for a given classification
     *
     * @param classificationId
     * @param classId
     * @return size of class
     */
    public float getWeight(int classificationId, int classId) {
        Float result = weights[classificationId].get(classId);
        return Objects.requireNonNullElse(result, 0f);
    }

    private void setWeight(int classificationId, int classId, float weight) {
        this.weights[classificationId].put(classId, weight);
    }

    /**
     * gets the mapping of class ids to sizes for a given classification
     *
     * @param classificationId
     * @return class-id to size map
     */
    public Map<Integer, Float> getClassIdToWeightMap(int classificationId) {
        return weights[classificationId];
    }

    /**
     * get the first UpdateItem for a given classification and class
     *
     * @param classificationId
     * @param classId
     * @return first
     */
    public UpdateItem getFirst(int classificationId, int classId) {
        return first[classificationId].get(classId);
    }

    /**
     * set  the first UpdateItem for a given classification and class
     *
     * @param classificationId
     * @param classId
     * @param item
     */
    private void setFirst(int classificationId, int classId, UpdateItem item) {
        first[classificationId].put(classId, item);
    }

    /**
     * get the last UpdateItem for a given classification and class
     *
     * @param classificationId
     * @param classId
     * @return first
     */
    private UpdateItem getLast(int classificationId, int classId) {
        return last[classificationId].get(classId);
    }


    /**
     * set the last UpdateItem for a given classification and class
     *
     * @param classificationId
     * @param classId
     * @param item
     */
    private void setLast(int classificationId, int classId, UpdateItem item) {
        last[classificationId].put(classId, item);
    }

    /**
     * gets the set of class ids defined for a given classification
     *
     * @param classificationId
     * @return
     */
    public Set<Integer> getClassIds(int classificationId) {
        return first[classificationId].keySet();
    }

    /**
     * remove the given class from the given classification
     *
     * @param classificationId
     * @param classId
     */
    private void removeClass(int classificationId, int classId) {
        first[classificationId].put(classId, null);
        first[classificationId].remove(classId);
        last[classificationId].put(classId, null);
        last[classificationId].remove(classId);
        weights[classificationId].put(classId, null);
        weights[classificationId].remove(classId);
    }

    /**
     * append the src class to the target class
     *
     * @param classificationId
     * @param srcClassId
     * @param tarClassId
     */
    public void appendClass(int classificationId, int srcClassId, int tarClassId) {
        float newSize = getWeight(classificationId, srcClassId) + getWeight(classificationId, tarClassId);

        if (newSize > 0) {
            UpdateItem firstItemSrc = getFirst(classificationId, srcClassId);

            if (firstItemSrc == null) {
                System.err.println("Warning: srcClassId=" + srcClassId + ", tarClassId=" + tarClassId + " firstItemSrc=null");
                return;
            }

            // replace class for all elements in src class:
            UpdateItem item = firstItemSrc;
            while (item != null) {
                item.setClassId(classificationId, tarClassId);
                item = item.getNextInClassification(classificationId);
            }

            // rescan first and last items:

            UpdateItem firstItemTar = getFirst(classificationId, tarClassId);
            if (firstItemTar == null)
                setFirst(classificationId, tarClassId, firstItemSrc);

            UpdateItem lastItemTar = getLast(classificationId, tarClassId);
            if (lastItemTar != null)
                lastItemTar.setNextInClassifaction(classificationId, firstItemSrc);

            UpdateItem lastItemSrc = getLast(classificationId, srcClassId);
            setLast(classificationId, tarClassId, lastItemSrc);

            setWeight(classificationId, tarClassId, newSize);
            removeClass(classificationId, srcClassId);

            sortChain(classificationId, tarClassId);
        }
    }

    /**
     * after appending a class to an existing class, sorts all UpdateItems so that they appear in the order in
     * which the reads occur in the file, for a given classId.
     * This is useful for when we extract all reads for a given classId, as then we go through the file sequentially
     *
     * @param classificationId
     * @param classId
     */
    private void sortChain(int classificationId, int classId) {
        // sort all UpdateItems by readUid:
        final ArrayList<UpdateItem> sorted = new ArrayList<>(100000);

        UpdateItem item = getFirst(classificationId, classId);
        while (item != null) {
            sorted.add(item);
            item = item.getNextInClassification(classificationId);
        }
        sorted.sort(UpdateItem.getComparator());

        // re-build chain:
        UpdateItem first = null;
        UpdateItem prev = null;
        UpdateItem last = null;

        for (UpdateItem current : sorted) {
            if (first == null)
                first = current;
            if (prev != null)
                prev.setNextInClassifaction(classificationId, current);
            prev = current;
            last = current;
        }
        if (first != null)
            setFirst(classificationId, classId, first);
        if (last != null) {
            setLast(classificationId, classId, last);
            last.setNextInClassifaction(classificationId, null);
        }
    }
}
