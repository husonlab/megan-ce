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
package megan.inspector;

import megan.data.IReadBlock;

/**
 * node to represent the head line for a read
 * Daniel Huson, 2.2006
 */
public class ReadHeadLineNode extends NodeBase {
    private final String readHeader;
    private final String readSequence;
    private final long uid;

    /**
     * constructor
     *
     * @param readBlock
     */
    public ReadHeadLineNode(IReadBlock readBlock) {
        super(readBlock.getReadName());
        this.readHeader = readBlock.getReadHeader();
        this.readSequence = readBlock.getReadSequence();
        this.uid = readBlock.getUId();
        this.rank = readBlock.getNumberOfMatches();
    }

    public boolean isLeaf() {
        return readHeader == null; // never leaf, because at least data node is contained below
    }

    public String toString() {
        if (getReadLength() <= 0) {
            if (rank <= 0)
                return getName();
            else
                return String.format("%s [matches=%,d]", getName(), (int) rank);
        } else // readLength>0
        {
            if (rank <= 0)
                return String.format("%s [length=%,d]", getName(), getReadLength());
            else
                return String.format("%s [length=%,d, matches=%,d]", getName(), getReadLength(), (int) rank);
        }
    }

    public long getUId() {
        return uid;
    }

    private int getReadLength() {
        return readSequence != null ? readSequence.length() : 0;
    }

    public String getReadHeader() {
        return readHeader;
    }

    public String getReadSequence() {
        return readSequence;
    }
}
