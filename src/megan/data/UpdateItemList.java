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
package megan.data;

import java.io.IOException;
import java.util.*;

/**
 * list of necessary updates to RMA file
 * Daniel Huson, 1.2009
 */
public class UpdateItemList extends LinkedList<UpdateItem> {
    private final Map<Long, UpdateItem> readUid2UpdateItem;
    private final int numberOfClassifications;
    private final Map<Integer, UpdateItem>[] first;
    private final Map<Integer, UpdateItem>[] last;
    private final Map<Integer, Integer>[] size;

    /**
     * constructor
     *
     * @param numberOfClassifications
     */
    @SuppressWarnings("unchecked")
    public UpdateItemList(int numberOfClassifications) {
        super();
        readUid2UpdateItem = new HashMap<>(1000000);
        this.numberOfClassifications = numberOfClassifications;
        first = new HashMap[numberOfClassifications];
        last = new HashMap[numberOfClassifications];
        size = new HashMap[numberOfClassifications];
        for (int i = 0; i < numberOfClassifications; i++) {
            first[i] = new HashMap<>(1000000);
            last[i] = new HashMap<>(1000000);
            size[i] = new HashMap<>(1000000);
        }
    }


    /**
     * gets the rescan item for a given read uid
     *
     * @param readUid
     * @return rescan item or null
     */
    public UpdateItem getUpdateItem(final long readUid) {
        return readUid2UpdateItem.get(readUid);
    }

    /**
     * add an item
     * @param readUid
     * @param classIds
     */
    public UpdateItem addItem(final long readUid, int readWeight, final Integer[] classIds) throws IOException {
        if (classIds.length != numberOfClassifications)
            throw new IOException("classIds has wrong length: " + classIds.length + ", should be: " + numberOfClassifications);
        UpdateItem item = new UpdateItem(numberOfClassifications);
        item.setReadUId(readUid);
        add(item);
        readUid2UpdateItem.put(readUid, item);

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
                    size[i].put(id, readWeight);
                } else {
                    lastInClass.setNextInClassifaction(i, readUid);
                    last[i].put(id, item);
                    size[i].put(id, size[i].get(id) + readWeight);
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
    public int getSize(int classificationId, int classId) {
        Integer result = size[classificationId].get(classId);
        if (result == null)
            return 0;
        else
            return result;
    }

    /**
     * gets the mapping of class ids to sizes for a given classification
     *
     * @param classificationId
     * @return class-id to size map
     */
    public Map<Integer, Integer> getClassIdToSizeMap(int classificationId) {
        return size[classificationId];
    }

    /**
     * get the first rescan item for a given classification and class
     *
     * @param classificationId
     * @param classId
     * @return first
     */
    public UpdateItem getFirst(int classificationId, int classId) {
        return first[classificationId].get(classId);
    }

    /**
     * set  the first rescan item for a given classification and class
     *
     * @param classificationId
     * @param classId
     * @param item
     */
    public void setFirst(int classificationId, int classId, UpdateItem item) {
        first[classificationId].put(classId, item);
    }

    /**
     * get the last rescan item for a given classification and class
     *
     * @param classificationId
     * @param classId
     * @return first
     */
    public UpdateItem getLast(int classificationId, int classId) {
        return last[classificationId].get(classId);
    }


    /**
     * set the last rescan item for a given classification and class
     *
     * @param classificationId
     * @param classId
     * @param item
     */
    public void setLast(int classificationId, int classId, UpdateItem item) {
        last[classificationId].put(classId, item);
    }

    /**
     * set the size value for a given class
     *
     * @param classificationId
     * @param classId
     * @param value
     */
    public void setSize(int classificationId, int classId, int value) {
        size[classificationId].put(classId, value);
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
    public void removeClass(int classificationId, int classId) {
        first[classificationId].put(classId, null);
        first[classificationId].remove(classId);
        last[classificationId].put(classId, null);
        last[classificationId].remove(classId);
        size[classificationId].put(classId, null);
        size[classificationId].remove(classId);
    }

    /**
     * append the src class to the target class
     *
     * @param classificationId
     * @param srcClassId
     * @param tarClassId
     */
    public void appendClass(int classificationId, int srcClassId, int tarClassId) {
        int newSize = getSize(classificationId, srcClassId) + getSize(classificationId, tarClassId);

        if (newSize > 0) {
            UpdateItem firstItemSrc = getFirst(classificationId, srcClassId);

            if (firstItemSrc == null) {
                System.err.println("Warning: srcClassId=" + srcClassId + ", tarClassId=" + tarClassId + " firstItemSrc=null");
                return;
            }

            // replace class for all elements in src class:
            long readUid = firstItemSrc.getReadUId();
            while (readUid != 0) {
                UpdateItem item = readUid2UpdateItem.get(readUid);
                item.setClassId(classificationId, tarClassId);
                readUid = item.getNextInClassification(classificationId);
            }

            // rescan first and last items:

            UpdateItem firstItemTar = getFirst(classificationId, tarClassId);
            if (firstItemTar == null)
                setFirst(classificationId, tarClassId, firstItemSrc);


            UpdateItem lastItemTar = getLast(classificationId, tarClassId);
            if (lastItemTar != null)
                lastItemTar.setNextInClassifaction(classificationId, firstItemSrc.getReadUId());

            UpdateItem lastItemSrc = getLast(classificationId, srcClassId);
            setLast(classificationId, tarClassId, lastItemSrc);

            setSize(classificationId, tarClassId, newSize);
            removeClass(classificationId, srcClassId);

            sortChain(classificationId, tarClassId);
        }
    }

    /**
     * after appending a class to an existing class, sorts all rescan items so that they appear in the order in
     * which the reads occur in the file, for a given classId.
     * This is useful for when we extract all reads for a given classId, as then we go through the file sequentially
     *
     * @param classificationId
     * @param classId
     */
    private void sortChain(int classificationId, int classId) {
        // sort all rescan items by readUid:
        SortedSet<UpdateItem> sorted = new TreeSet<>(new UpdateItem());

        UpdateItem updateItem = getFirst(classificationId, classId);
        while (updateItem != null) {
            sorted.add(updateItem);
            long nextReadUid = updateItem.getNextInClassification(classificationId);
            if (nextReadUid != 0)
                updateItem = readUid2UpdateItem.get(nextReadUid);
            else
                updateItem = null;
        }

        // re-build chain:
        UpdateItem first = null;
        UpdateItem prev = null;
        UpdateItem last = null;

        for (UpdateItem current : sorted) {
            if (first == null)
                first = current;
            if (prev != null)
                prev.setNextInClassifaction(classificationId, current.getReadUId());
            prev = current;
            last = current;
        }
        if (first != null)
            setFirst(classificationId, classId, first);
        if (last != null) {
            setLast(classificationId, classId, last);
            last.setNextInClassifaction(classificationId, 0);
        }
    }
}
