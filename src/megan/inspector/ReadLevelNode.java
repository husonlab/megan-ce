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
package megan.inspector;

/**
 * node to represent a read
 * Daniel Huson, 2.2006
 */
public class ReadLevelNode extends NodeBase {
    protected long uId;

    /**
     * constructor for archive data
     *
     * @param name
     * @param numberOfMatches
     * @param uId
     */
    public ReadLevelNode(String name, int numberOfMatches, long uId) {
        super(name);

        this.rank = numberOfMatches;
        this.uId = uId;
    }

    public boolean isLeaf() {
        return uId == 0; // never leaf, because at least data node is contained below
    }

    public String toString() {
        return getName() + (rank > 0 ? " [" + (int) rank + "]" : "");
    }

    public long getUId() {
        return uId;
    }
}
