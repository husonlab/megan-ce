/*
 * MatchBlockFromBlast.java Copyright (C) 2022 Daniel H. Huson
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

import megan.rma2.MatchBlockRMA2;

/**
 * MatchBlock with information on location of text in file
 * Daniel Huson, 10.2010
 */
public class MatchBlockFromBlast extends MatchBlockRMA2 implements IMatchBlock, IMatchBlockWithLocation {
    private Location location;

    /**
     * constructor
     */
    public MatchBlockFromBlast() {
    }

    /**
     * gets the location of the  text
     *
     * @return location of read text or null
     */
    public Location getTextLocation() {
        return location;
    }

    /**
     * sets the location of the  text
     *
     * @param location
     */
    public void setTextLocation(Location location) {
        this.location = location;
    }

    /**
     * erase the block
     */
    public void clear() {
        super.clear();
        location = null;
    }
}
