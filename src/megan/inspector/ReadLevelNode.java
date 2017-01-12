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
package megan.inspector;

/**
 * node to represent a read
 * Daniel Huson, 2.2006
 */
public class ReadLevelNode extends NodeBase {
    protected long uId;
    protected int readLength;

    /**
     * constructor for archive data
     *  @param name
     * @param numberOfMatches
     * @param uId
     * @param readLength
     */
    public ReadLevelNode(String name, int numberOfMatches, long uId, int readLength) {
        super(name);

        this.rank = numberOfMatches;
        this.uId = uId;
        this.readLength = readLength;
    }

    public boolean isLeaf() {
        return uId == 0; // never leaf, because at least data node is contained below
    }

    public String toString() {
        if (readLength <= 0) {
            if (rank <= 0)
                return getName();
            else
                return getName() + " [matches=" + (int) rank + "]";
        } else // readLength>0
        {
            if (rank <= 0)
                return getName() + " [length=" + readLength + "]";
            else
                return getName() + " [length=" + readLength + ", matches=" + (int) rank + "]";

        }
    }

    public long getUId() {
        return uId;
    }
}
