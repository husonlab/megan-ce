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

import java.util.Comparator;

/**
 * records necessary updates to data
 * Daniel Huson, 1.2009
 */
public class UpdateItem {
    private long readUId; // position of read
    private UpdateItem[] nextInClass; // next item in each classification
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
        nextInClass = new UpdateItem[numberClassifications];
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

    public UpdateItem getNextInClassification(int classificationId) {
        return nextInClass[classificationId];
    }

    public void setNextInClassifaction(int classificationId, UpdateItem item) {
        nextInClass[classificationId] = item;
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
        for (UpdateItem nextInClas : nextInClass) buf.append(" ").append(nextInClas.getReadUId());
        return buf.toString();
    }

    public static Comparator<UpdateItem> getComparator() {
        return (a, b) -> {
            if (a.readUId < b.readUId)
                return -1;
            else if (a.readUId > b.readUId)
                return 1;
            else {
                for (int i = 0; i < a.classId.length; i++) {
                    if (a.classId[i] < b.classId[i])
                        return -1;
                    else if (a.classId[i] > b.classId[i])
                        return 1;
                }
            }
            return 0;
        };
    }
}
