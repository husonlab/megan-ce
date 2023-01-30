/*
 * Location.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Stores the location of a text
 * Daniel Huson, 10.2010
 */
public class Location implements Serializable {
    @Serial
    private static final long serialVersionUID = -6952597828705416365L; // DHH: changed id because LocationManager removed
    private int fileId;
    private long position;
    private int size;

    /**
     * default constructor
     */
    public Location() {
    }

    /**
     * constructor
     *
	 */
    public Location(int fileId, long position, int size) {
        this.fileId = fileId;
        this.position = position;
        this.size = size;
    }

    public int getFileId() {
        return fileId;
    }

    public long getPosition() {
        return position;
    }

    public int getSize() {
        return size;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String toString() {
        return "[fileId=" + fileId + ", pos=" + position + ", size=" + size + "]";
    }

    public Object clone() {
        return new Location(fileId, position, size);
    }
}
