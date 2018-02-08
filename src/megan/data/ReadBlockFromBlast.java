/*
 *  Copyright (C) 2018 Daniel H. Huson
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

import jloda.util.Basic;
import megan.rma2.ReadBlockRMA2;

/**
 * readblock with information on location of text in file
 * Daniel Huson, 10.2010
 */
public class ReadBlockFromBlast extends ReadBlockRMA2 implements IReadBlock, IReadBlockWithLocation {
    private Location location;

    /**
     * constructor
     */
    public ReadBlockFromBlast() {
    }

    /**
     * set read sequence. Overloaded to also compute complexity and read length
     *
     * @param readSequence
     */
    public void setReadSequence(String readSequence) {
        super.setReadSequence(readSequence);
        if (readSequence != null) {
            setReadLength(Basic.getNumberOfNonSpaceCharacters(readSequence));
        }
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
     * get the i-th match block
     *
     * @param i
     * @return match block
     */
    public IMatchBlockWithLocation getMatchBlock(int i) {
        return (IMatchBlockWithLocation) super.getMatchBlock(i);
    }

    /**
     * erase the block
     */
    public void clear() {
        super.clear();
        location = null;
    }
}
