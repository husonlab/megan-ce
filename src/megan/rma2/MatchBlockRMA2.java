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
package megan.rma2;

import jloda.util.Basic;
import megan.data.*;
import megan.io.ByteByteInt;
import megan.io.IInputReader;
import megan.io.IOutputWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * matchblock for RMA2
 * Daniel Huson, 9.2010, 3.2016
 */
public class MatchBlockRMA2 implements IMatchBlock {
    private static final String TAXONOMY = "Taxonomy";
    private static final String SEED = "SEED";
    private static final String KEGG = "KEGG";
    private static final String COG = "EGGNOG";

    private final Map<String, Integer> cName2id = new HashMap<>();

    private long uid;
    private float bitScore;
    private float percentIdentity;
    private String refSeqId;
    private float expected;
    private int length;
    private boolean ignore;
    private String text;


    /**
     * erase the block (for reuse)
     */
    public void clear() {
        uid = 0;
        bitScore = 0;
        percentIdentity = 0;
        refSeqId = null;
        expected = 0;
        length = 0;
        ignore = false;
        text = null;
        cName2id.clear();
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
     * gets the id for the named classification
     *
     * @param name
     * @return id
     */
    public int getId(String name) {
        Integer id = cName2id.get(name);
        return id == null ? 0 : id;
    }

    /**
     * set the id for the named classification
     *
     * @param name
     * @param id
     */
    public void setId(String name, Integer id) {
        cName2id.put(name, id);
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

    /**
     * get the taxon id of the match
     *
     * @return
     */
    public int getTaxonId() {
        return getId(TAXONOMY);
    }

    public void setTaxonId(int taxonId) {
        cName2id.put(TAXONOMY, taxonId);
    }


    /**
     * get the score of the match
     *
     * @return
     */
    public float getBitScore() {
        return bitScore;
    }

    public void setBitScore(float bitScore) {
        this.bitScore = bitScore;
    }

    /**
     * get the percent identity
     *
     * @return
     */
    public float getPercentIdentity() {
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
        return refSeqId;
    }

    public void setRefSeqId(String refSeqId) {
        this.refSeqId = refSeqId;
    }

    /**
     * gets the E-value
     *
     * @param expected
     * @throws java.io.IOException
     */
    public void setExpected(float expected) {
        this.expected = expected;
    }

    public float getExpected() {
        return expected;
    }

    /**
     * gets the match length
     *
     * @param length
     * @throws java.io.IOException
     */
    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    /**
     * get the ignore status
     *
     * @return
     */
    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    /**
     * get the text
     *
     * @return
     */
    public String getText() {
        return text;
    }

    @Override
    public String getTextFirstWord() {
        return text != null ? Basic.getFirstWord(text) : null;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * read a match block
     *
     * @param rma2Formatter
     * @param uid
     * @param wantMatchData
     * @param wantMatchText
     * @param textReader
     * @param dataIndexReader
     * @return matchBlock
     * @throws java.io.IOException
     */
    public static MatchBlockRMA2 read(RMA2Formatter rma2Formatter, long uid, boolean wantMatchData, boolean wantMatchText,
                                      float minScore, float maxExpected, TextStorageReader textReader, IInputReader dataIndexReader) throws IOException {
        MatchBlockRMA2 matchBlock = rma2Formatter.isWantLocationData() ? new MatchBlockFromBlast() : new MatchBlockRMA2();

        if (uid == -1)
            uid = dataIndexReader.getPosition();
        else
            dataIndexReader.seek(uid);

        // total items to read: float float int bytebyteint char long int  = 4+4+4+6+2+8+4=32

        if (!wantMatchData && (minScore > 0 || maxExpected < 10000))
            wantMatchData = true; // need score or expected, and also get everything else

        if (wantMatchData) {
            matchBlock.setUId(uid);

            MatchBlockRMA2Formatter matchBlockFormatter = rma2Formatter.getMatchBlockRMA2Formatter();
            matchBlockFormatter.read(dataIndexReader);

            if (matchBlockFormatter.hasBitScore())
                matchBlock.setBitScore(matchBlockFormatter.getBitScore());
            if (matchBlockFormatter.hasExpected())
                matchBlock.setExpected(matchBlockFormatter.getExpected());
            if (matchBlockFormatter.hasPercentIdentity())
                matchBlock.setPercentIdentity(matchBlockFormatter.getPercentIdentity());
            if (matchBlockFormatter.hasTaxonId())
                matchBlock.setTaxonId(matchBlockFormatter.getTaxonId());
            if (matchBlockFormatter.hasSeedId())
                matchBlock.setId(SEED, matchBlockFormatter.getSeedId());
            if (matchBlockFormatter.hasCogId())
                matchBlock.setId(COG, matchBlockFormatter.getCogId());
            if (matchBlockFormatter.hasKeggId())
                matchBlock.setId(KEGG, matchBlockFormatter.getKeggId());
            if (matchBlockFormatter.hasRefSeqId())
                matchBlock.setRefSeqId(matchBlockFormatter.getRefSeqId().toString());

            if (matchBlock.getBitScore() < minScore || matchBlock.getExpected() > maxExpected) {
                dataIndexReader.skipBytes(14); // skip the unwanted entries    char+long+int
                return null;
            }
        } else {
            dataIndexReader.skipBytes(rma2Formatter.getMatchBlockRMA2Formatter().getNumberOfBytes()); // skip the unwanted fixed entries
        }

        if (wantMatchText) {
            final Location location = new Location(dataIndexReader.readChar(), dataIndexReader.readLong(), dataIndexReader.readInt());
            matchBlock.setText(textReader.getText(location));

            if (rma2Formatter.isWantLocationData()) {
                ((MatchBlockFromBlast) matchBlock).setTextLocation(location);
            }

        } else
            dataIndexReader.skipBytes(14);

        return matchBlock;
    }

    /**
     * get the size of a match block in the index file
     *
     * @param matchBlockRMA2Formatter
     * @return number of bytes in match block
     */
    public static int getBytesInIndexFile(MatchBlockRMA2Formatter matchBlockRMA2Formatter) {
        return matchBlockRMA2Formatter.getNumberOfBytes() + 14; // 14 for location information
    }

    /**
     * writes a match to the RMA file
     *
     * @param rma2Formatter
     * @param matchBlock
     * @param dumpWriter
     * @param indexWriter   @throws java.io.IOException
     */
    public static void write(RMA2Formatter rma2Formatter, IMatchBlockWithLocation matchBlock, IOutputWriter dumpWriter, IOutputWriter indexWriter) throws IOException {
        matchBlock.setUId(indexWriter.getPosition());

        Location location = matchBlock.getTextLocation();

        if (dumpWriter != null) {
            if (location == null) {
                location = new Location();
                matchBlock.setTextLocation(location);
            }
            location.setFileId(0);
            location.setPosition(dumpWriter.getPosition());
            dumpWriter.writeString(matchBlock.getText());
            location.setSize((int) (dumpWriter.getPosition() - location.getPosition()));
        }

        // write match to dataindex:
        MatchBlockRMA2Formatter matchBlockFormatter = rma2Formatter.getMatchBlockRMA2Formatter();

        if (matchBlockFormatter.hasBitScore())
            matchBlockFormatter.setBitScore(matchBlock.getBitScore());
        if (matchBlockFormatter.hasExpected())
            matchBlockFormatter.setExpected(matchBlock.getExpected());
        if (matchBlockFormatter.hasPercentIdentity())
            matchBlockFormatter.setPercentIdentity(matchBlock.getPercentIdentity());
        if (matchBlockFormatter.hasTaxonId())
            matchBlockFormatter.setTaxonId(matchBlock.getTaxonId());
        if (matchBlockFormatter.hasSeedId())
            matchBlockFormatter.setSeedId(matchBlock.getId(SEED));
        if (matchBlockFormatter.hasCogId())
            matchBlockFormatter.setCogId(matchBlock.getId(COG));
        if (matchBlockFormatter.hasKeggId())
            matchBlockFormatter.setKeggId(matchBlock.getId(KEGG));
        if (matchBlockFormatter.hasRefSeqId())
            matchBlockFormatter.setRefSeqId(new ByteByteInt(matchBlock.getRefSeqId()));
        matchBlockFormatter.write(indexWriter);

        if (location != null) {
            indexWriter.writeChar((char) location.getFileId());
            indexWriter.writeLong(location.getPosition());
            indexWriter.writeInt(location.getSize());
        } else {
            indexWriter.writeChar((char) -1);
            indexWriter.writeLong(-1);
            indexWriter.writeInt(-1);
        }
    }

    public String toString() {
        StringWriter w = new StringWriter();

        w.write("Match uid: " + uid + "--------\n");
        if (getTaxonId() != 0)
            w.write("taxonId: " + getTaxonId() + "\n");
        if (getId(SEED) != 0)
            w.write("seedId: " + getId(SEED) + "\n");
        if (getId(COG) != 0)
            w.write("cogId:  " + getId(COG) + "\n");
        if (getId(KEGG) != 0)
            w.write("keggId: " + getId(KEGG) + "\n");
        if (bitScore != 0)
            w.write("bitScore: " + bitScore + "\n");
        if (percentIdentity != 0)
            w.write("percentIdentity: " + percentIdentity + "\n");
        if (refSeqId != null && refSeqId.length() > 0)
            w.write("refSeqId: " + refSeqId + "\n");
        if (expected != 0)
            w.write("expected: " + expected + "\n");
        if (length != 0)
            w.write("length: " + length + "\n");
        if (ignore)
            w.write("ignore: " + "\n");
        w.write("text: " + text + "\n");
        return w.toString();
    }

    /**
     * get the start position of the alignment in the query
     *
     * @return query start position
     */
    @Override
    public int getAlignedQueryStart() {
        return 0;
    }

    /**
     * get the end position of the alignment in the query
     *
     * @return
     */
    @Override
    public int getAlignedQueryEnd() {
        return 0;
    }

    @Override
    public int getRefLength() {
        return 0;
    }
}
