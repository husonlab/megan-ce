/*
 * IMatchBlock.java Copyright (C) 2022 Daniel H. Huson
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
	 */
    int getTaxonId();

    void setTaxonId(int taxonId);

    /**
     * get the score of the match
     *
	 */
    float getBitScore();

    void setBitScore(float bitScore);

    /**
     * get percent identity
     *
	 */
    float getPercentIdentity();

    void setPercentIdentity(float percentIdentity);

    /**
     * get the refseq id
     *
	 */
    String getRefSeqId();

    void setRefSeqId(String refSeqId);

    /**
     * gets the E-value
     *
	 */
    void setExpected(float expected);

    float getExpected();

    /**
     * gets the match length
     *
	 */
    void setLength(int length);

    int getLength();

    /**
     * get the ignore status
     *
	 */
    boolean isIgnore();

    void setIgnore(boolean ignore);

    /**
     * get the text
     *
	 */
    String getText();

    /**
     * get the first word of the text
     *
	 */
    String getTextFirstWord();

    /**
     * set the text
     *
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
