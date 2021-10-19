/*
 * MatchBlockRMA6.java Copyright (C) 2021. Daniel H. Huson
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
package megan.rma6;

import jloda.util.Single;
import jloda.util.StringUtils;
import megan.classification.Classification;
import megan.classification.IdParser;
import megan.data.IMatchBlock;
import megan.parsers.sam.SAMMatch;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * matchblock for RMA6
 * Daniel Huson, 6.2015
 */
public class MatchBlockRMA6 implements IMatchBlock {
    private static long countUids = 0;
    private static final Object sync = new Object();

    private long uid;
    private float percentIdentity;
    private String text;
    private final Map<String, Integer> fName2Id = new HashMap<>();

    private SAMMatch samMatch; // major update: we now keep the sam match and only compute text if necessary

    /**
     * constructor
     */
    public MatchBlockRMA6() {
    }

    /**
     * set match data from a SAM match
     *
     * @param samMatch
     */
    public void setFromSAM(SAMMatch samMatch) {
        text = null;
        percentIdentity = 0;

        this.samMatch = samMatch;

        synchronized (sync) {
            uid = countUids++;
        }
    }

    /**
     * erase the block (for reuse)
     */
    public void clear() {
        samMatch = null;
        uid = 0;
        percentIdentity = 0;
        text = null;
        fName2Id.clear();
    }

    /**
     * get the unique identifier for this match (unique within a dataset).
     * In an RMA file, this is always the file position for the match
     *
     * @return uid
     */
    public long getUId() {
        return uid;
    }

    public void setUId(long uid) {
        this.uid = uid;
    }

    /**
     * get the taxon id of the match
     *
     * @return
     */
    public int getTaxonId() {
        return getId(Classification.Taxonomy);
    }

    public void setTaxonId(int taxonId) {
        setId(Classification.Taxonomy, taxonId);
    }

    public int getId(String cName) {
        Integer id = fName2Id.get(cName);
        return id != null ? id : 0;
    }

    /**
     * gets all defined ids
     *
     * @param cNames
     * @return ids
     */
    public int[] getIds(String[] cNames) {
        int[] ids = new int[cNames.length];
        for (int i = 0; i < cNames.length; i++) {
            ids[i] = getId(cNames[i]);
        }
        return ids;
    }

    public void setId(String cName, Integer id) {
        fName2Id.put(cName, id);
    }

    /**
     * get the score of the match
     *
     * @return
     */
    public float getBitScore() {
        if (samMatch == null)
            System.err.println("null");
        return samMatch.getBitScore();
    }

    public void setBitScore(float bitScore) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * get the percent identity
     *
     * @return
     */
    public float getPercentIdentity() {
        if (text == null)
            getText(); // percent identity is calculated while creating text
        return percentIdentity;
    }

    public void setPercentIdentity(float percentIdentity) {
        this.percentIdentity = percentIdentity;
    }

    /**
     * get the refseq id
     *
     * @return
     */
    public String getRefSeqId() {
        return getText() != null ? parseRefSeqId(getText()) : null;
    }

    public void setRefSeqId(String refSeqId) {
    }

    /**
     * gets the E-value
     *
     * @param expected
     * @throws java.io.IOException
     */
    public void setExpected(float expected) {
        throw new RuntimeException("Not implemented");
    }

    public float getExpected() {
        return samMatch.getExpected();
    }

    /**
     * gets the match length
     *
     * @param length
     * @throws java.io.IOException
     */
    public void setLength(int length) {
        throw new RuntimeException("Not implemented");
    }

    public int getLength() {
        return samMatch.getTLength();
    }

    /**
     * get the ignore status
     */
    @Deprecated
    public boolean isIgnore() {
        return false;
    }

    /**
     * set the ignore status
     */
    @Deprecated
    public void setIgnore(boolean ignore) {
    }

    /**
     * get the text
     *
     * @return
     */
    public String getText() {
        if (text == null) {
            final Single<Float> value = new Single<>(0f);
            text = samMatch.getBlastAlignmentText(value);
            percentIdentity = value.get();

        }
        return text;
    }

    @Override
    public String getTextFirstWord() {
		return getText() != null ? StringUtils.getFirstWord(getText()) : null;
    }

    public void setText(String text) {
        throw new RuntimeException("Not implemented");
    }

    public String toString() {
        StringWriter w = new StringWriter();

        w.write("Match uid: " + uid + "--------\n");
        for (String cName : fName2Id.keySet())
            w.write(String.format(" %s: ", cName) + fName2Id.get(cName));
        w.write("\n");
        if (getBitScore() != 0)
            w.write("bitScore: " + getBitScore() + "\n");
        if (percentIdentity != 0)
            w.write("percentIdentity: " + getPercentIdentity() + "\n");
        if (getExpected() != 0)
            w.write("expected: " + getExpected() + "\n");
        if (getLength() != 0)
            w.write("length: " + getLength() + "\n");
        w.write("text: " + text + "\n");
        return w.toString();
    }

    /**
     * parses a Accession id
     *
     * @param aLine
     * @return refseq id
     */
    private static String parseRefSeqId(String aLine) {
        int pos = aLine.indexOf(IdParser.REFSEQ_TAG);
        if (pos != -1) {
            int start = pos + IdParser.REFSEQ_TAG.length();
            int end = start;
            while (end < aLine.length() && (Character.isLetterOrDigit(aLine.charAt(end)) || aLine.charAt(end) == '_'))
                end++;
            if (end > start)
                return aLine.substring(start, end);
        }
        return null;
    }

    @Override
    public int getAlignedQueryStart() {
        return samMatch.getAlignedQueryStart();
    }

    @Override
    public int getAlignedQueryEnd() {
        return samMatch.getAlignedQueryEnd();
    }

    @Override
    public int getRefLength() {
        return samMatch.getRefLength();
    }
}
