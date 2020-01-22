/*
 * IMatchBlock.java Copyright (C) 2020. Daniel H. Huson
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
 *
 */
package megan.data;

import java.io.IOException;

/**
 * matchblock interface
 * Daniel Huson, 4.2010
 */
public interface IMatchBlock {
    /**
     * get the unique identifier for this match (unique within a dataset).
     * In an RMA file, this is always the file position for the match
     *
     * @return uid
     */
    long getUId();

    void setUId(long uid);

    /**
     * get the taxon id of the match
     *
     * @return
     */
    int getTaxonId();

    void setTaxonId(int taxonId);

    /**
     * get the score of the match
     *
     * @return
     */
    float getBitScore();

    void setBitScore(float bitScore);

    /**
     * get percent identity
     *
     * @return
     */
    float getPercentIdentity();

    void setPercentIdentity(float percentIdentity);

    /**
     * get the refseq id
     *
     * @return
     */
    String getRefSeqId();

    void setRefSeqId(String refSeqId);

    /**
     * gets the E-value
     *
     * @param expected
     * @throws IOException
     */
    void setExpected(float expected);

    float getExpected();

    /**
     * gets the match length
     *
     * @param length
     * @throws IOException
     */
    void setLength(int length);

    int getLength();

    /**
     * get the ignore status
     *
     * @return
     */
    boolean isIgnore();

    void setIgnore(boolean ignore);

    /**
     * get the text
     *
     * @return
     */
    String getText();

    /**
     * get the first word of the text
     *
     * @return
     */
    String getTextFirstWord();

    /**
     * set the text
     *
     * @param text
     */
    void setText(String text);

    int getId(String cName);

    void setId(String cName, Integer id);

    int[] getIds(String[] cNames);

    /**
     * get the start position of the alignment in the query
     *
     * @return query start position
     */
    int getAlignedQueryStart();

    /**
     * get the end position of the alignment in the query
     *
     * @return query end position
     */
    int getAlignedQueryEnd();

    /**
     * get the length of the reference sequence
     *
     * @return reference sequence length
     */
    int getRefLength();
}
