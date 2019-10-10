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

import java.io.Serializable;

/**
 * interface to ReadBlock
 * Daniel Huson, 4.2010
 */
public interface IReadBlock extends Serializable {
    /**
     * get the unique identifier for this read (unique within a dataset).
     * In an RMA file, this is always the file position for the read
     *
     * @return uid
     */
    long getUId();

    void setUId(long uid);

    /**
     * get the name of the read (first word in header)
     *
     * @return name
     */
    String getReadName();

    /**
     * get the fastA header for the read
     *
     * @return fastA header
     */
    String getReadHeader();

    void setReadHeader(String readHeader);

    /**
     * get the sequence of the read
     *
     * @return sequence
     */
    String getReadSequence();

    void setReadSequence(String readSequence);

    /**
     * gets the uid of the mated read
     *
     * @return uid of mate or 0
     */
    long getMateUId();

    void setMateUId(long mateReadUId);

    /**
     * get the mate type
     * Possible values: FIRST_MATE, SECOND_MATE
     *
     * @return mate type
     * @deprecated
     */
    @Deprecated
    byte getMateType();

    /**
     * get mate type
     *
     * @param type
     * @deprecated
     */
    @Deprecated
    void setMateType(byte type);

    /**
     * set the read length
     *
     * @param readLength
     */
    void setReadLength(int readLength);

    int getReadLength();

    /**
     * get the complexity
     *
     * @param complexity
     */
    void setComplexity(float complexity);

    float getComplexity();

    /**
     * set the weight of a read
     *
     * @param weight
     */
    void setReadWeight(int weight);

    /**
     * get the weight of a read
     *
     * @return weight
     */
    int getReadWeight();

    /**
     * get  the original number of matches
     *
     * @return number of matches
     */
    int getNumberOfMatches();

    void setNumberOfMatches(int numberOfMatches);

    /**
     * gets the current number of matches available
     *
     * @return
     */
    int getNumberOfAvailableMatchBlocks();

    /**
     * get the matches. May be less than the original number of matches (when filtering matches)
     *
     * @return
     */
    IMatchBlock[] getMatchBlocks();

    void setMatchBlocks(IMatchBlock[] matchBlocks);

    /**
     * get the i-th match block
     *
     * @param i
     * @return match block
     */
    IMatchBlock getMatchBlock(int i);
}
