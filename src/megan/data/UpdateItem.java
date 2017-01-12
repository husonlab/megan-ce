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

import java.util.Comparator;

/**
 * records necessary updates to data
 * Daniel Huson, 1.2009
 */
public class UpdateItem implements Comparator<UpdateItem> {
    private long readUId; // position of read
    private long[] nextInClass; // next item in each classification
    private int[] classId; // class id

    /**
     * only for use as comparable item
     */
    public UpdateItem() {

    }

    /**
     * constructor
     *
     * @param numberClassifications
     */
    public UpdateItem(int numberClassifications) {
        nextInClass = new long[numberClassifications];
        classId = new int[numberClassifications];
    }

    /**
     * Get classIds
     *
     * @return
     */
    public int[] getClassIds() {
        return classId;
    }

    public long getNextInClassification(int classificationId) {
        return nextInClass[classificationId];
    }

    public void setNextInClassifaction(int classificationId, long pos) {
        nextInClass[classificationId] = pos;
    }

    public long getReadUId() {
        return readUId;
    }

    public void setReadUId(long readUId) {
        this.readUId = readUId;
    }

    public int getClassId(int i) {
        return classId[i];
    }

    public void setClassId(int i, int classId) {
        this.classId[i] = classId;
    }

    public int size() {
        return nextInClass.length;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("readUid= ").append(readUId).append(", classIds=");
        for (int id : classId) buf.append(" ").append(id);
        buf.append(" nextInClass=");
        for (long nextInClas : nextInClass) buf.append(" ").append(nextInClas);
        return buf.toString();
    }

    public int compare(UpdateItem updateItem1, UpdateItem updateItem2) {
        if (updateItem1.readUId < updateItem2.readUId)
            return -1;
        else if (updateItem1.readUId > updateItem2.readUId)
            return 1;
        else
            return 0;
    }
}
