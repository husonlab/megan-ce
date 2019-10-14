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
package megan.daa.connector;

import jloda.util.Basic;
import megan.classification.IdParser;
import megan.daa.io.*;
import megan.data.IMatchBlock;
import megan.parsers.sam.SAMMatch;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * matchblock for DAA
 * Daniel Huson, 6.2015
 */
public class MatchBlockDAA implements IMatchBlock {
    private static long countUids = 0;
    private static final Object sync = new Object();

    private final DAAParser daaParser;
    private DAAMatchRecord matchRecord;

    private long uid;
    private final Map<String, Integer> fName2Id = new HashMap<>();
    private int taxonId;

    /**
     * constructor
     */
    public MatchBlockDAA(DAAParser daaParser, DAAMatchRecord matchRecord) {
        this.daaParser = daaParser;
        this.matchRecord = matchRecord;

        final DAAHeader header = daaParser.getHeader();

        for (int f = 0; f < header.getNumberOfRefAnnotations(); f++) {
            fName2Id.put(header.getRefAnnotationName(f), header.getRefAnnotation(f, matchRecord.getSubjectId()));
        }
        taxonId = header.getRefAnnotation(header.getRefAnnotationIndexForTaxonomy(), matchRecord.getSubjectId());

        synchronized (sync) {
            uid = countUids++;
        }
    }

    /**
     * erase the block (for reuse)
     */
    public void clear() {
        uid = 0;
        matchRecord = null;
        fName2Id.clear();
        taxonId = 0;
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
        return taxonId;
    }

    public void setTaxonId(int taxonId) {
        this.taxonId = taxonId;
    }

    public int getId(String cName) {
        final Integer id = fName2Id.get(cName);
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
        return Math.round(daaParser.getHeader().computeAlignmentBitScore(matchRecord.getScore()));
        // we round because otherwise there is a small difference between RMA and DAA files
    }

    public void setBitScore(float bitScore) {
        System.err.println("Not implemented");
    }

    /**
     * get the percent identity
     *
     * @return
     */
    public float getPercentIdentity() {
        return Utilities.computePercentIdentity(matchRecord);
    }

    public void setPercentIdentity(float percentIdentity) {
        System.err.println("Not implemented");
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
        System.err.println("Not implemented");
    }

    /**
     * gets the E-value
     *
     * @param expected
     * @throws java.io.IOException
     */
    public void setExpected(float expected) {
        System.err.println("Not implemented");
    }

    public float getExpected() {
        return daaParser.getHeader().computeAlignmentExpected(matchRecord.getQuery().length, matchRecord.getScore());
    }

    /**
     * gets the match length
     *
     * @param length
     * @throws java.io.IOException
     */
    public void setLength(int length) {
        System.err.println("Not implemented");
    }

    public int getLength() {
        return matchRecord.getLen();
    }

    /**
     * get the ignore status
     *
     * @return
     * @deprecated
     */
    public boolean isIgnore() {
        return false;
    }

    /**
     * set the ignore status
     *
     * @param ignore
     * @deprecated
     */
    public void setIgnore(boolean ignore) {
        System.err.println("Not implemented");
    }

    /**
     * get the text
     *
     * @return
     */
    public String getText() {
        try { // todo: do this directly and more efficently
            ByteOutputBuffer buffer = new ByteOutputBuffer();
            SAMUtilities.createSAM(daaParser, matchRecord, buffer, daaParser.getAlignmentAlphabet());
            SAMMatch match = new SAMMatch(daaParser.getBlastMode());
            match.parse(buffer.getBytes(), buffer.size());
            return match.getBlastAlignmentText();
        } catch (Exception e) {
            Basic.caught(e);
            return "";
        }
    }

    /**
     * this is experimental code that is used to verify that DAA with frame-shifts is handled ok
     *
     * @param matchRecord
     * @param queryAlphabet
     * @return two alignment tracks
     */
    private String[] computeAlignmentBlastX(DAAMatchRecord matchRecord, byte[] queryAlphabet) {
        final byte[] totalQuerySequence = matchRecord.getQueryRecord().getSourceSequence();
        final int totalQueryLength = matchRecord.getQueryRecord().getQueryLength();

        final byte[] querySeq = matchRecord.getFrame() > 0 ? totalQuerySequence : Translator.getReverseComplement(totalQuerySequence);
        final int start = matchRecord.getFrame() > 0 ? matchRecord.getQueryBegin() : totalQueryLength - matchRecord.getQueryBegin() - 1;

        final StringBuilder[] bufs = {new StringBuilder(), new StringBuilder()};

        int q = start;
        for (CombinedOperation editOp : matchRecord.getTranscript().gather()) {
            switch (editOp.getEditOperation()) {
                case op_match: // handling match
                {
                    for (int i = 0; i < editOp.getCount(); i++) {
                        char aa = (char) daaParser.getAlignmentAlphabet()[Translator.getAminoAcid(querySeq, q)];
                        bufs[0].append(aa);
                        bufs[1].append(aa);
                        q += 3;
                    }
                    break;
                }
                case op_insertion: // handling insertion
                {
                    for (int i = 0; i < editOp.getCount(); i++) {
                        char aa = (char) daaParser.getAlignmentAlphabet()[Translator.getAminoAcid(querySeq, q)];
                        bufs[0].append(aa);
                        bufs[1].append('-');
                        q += 3;
                    }
                    break;
                }
                case op_deletion: // handling deletion
                {
                    char c = (char) queryAlphabet[editOp.getLetter()];
                    bufs[0].append('-');
                    bufs[1].append(c);
                    break;
                }
                case op_substitution: // handling substitution
                {
                    char c = (char) queryAlphabet[editOp.getLetter()];
                    if (c == '/') {
                        bufs[0].append("/");
                        bufs[1].append("-");
                        q -= 1;
                    } else if (c == '\\') {
                        bufs[0].append("\\");
                        bufs[1].append("-");
                        q += 1;
                    } else {
                        char aa = (char) daaParser.getAlignmentAlphabet()[Translator.getAminoAcid(querySeq, q)];
                        bufs[0].append(aa);
                        bufs[1].append(c);
                        q += 3;
                    }
                    break;
                }
            }
        }

        return new String[]{bufs[0].toString(), bufs[1].toString()};
    }


    @Override
    public String getTextFirstWord() {
        return Basic.toString(matchRecord.getSubjectName());
    }

    public void setText(String text) {
        System.err.println("Not implemented");
    }

    public String toString() {
        StringWriter w = new StringWriter();

        w.write("Match uid: " + uid + "--------\n");
        for (String cName : fName2Id.keySet())
            w.write(String.format("%4s: ", cName) + fName2Id.get(cName));
        w.write("\n");
        if (getBitScore() != 0)
            w.write("bitScore: " + getBitScore() + "\n");
        if (getPercentIdentity() != 0)
            w.write("percentIdentity: " + getPercentIdentity() + "\n");
        if (getExpected() != 0)
            w.write("expected: " + getExpected() + "\n");
        if (getLength() != 0)
            w.write("length: " + getLength() + "\n");
        if (getText() != null)
            w.write("text: " + getText() + "\n");
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

    public int getSubjectId() {
        return matchRecord.getSubjectId();
    }

    @Override
    public int getAlignedQueryStart() {
        return matchRecord.getQueryBegin() + 1;
    }

    @Override
    public int getAlignedQueryEnd() {
        return matchRecord.getQueryEnd() + 1;
    }

    @Override
    public int getRefLength() {
        return matchRecord.getTotalSubjectLen();
    }


    /**
     * compute the BLAST frame
     *
     * @param frame (in range 0-5)
     * @return BLAST frame (in range -2 to 2)
     */
    private static int computeBlastFrame(int frame) {
        return frame <= 2 ? frame + 1 : 2 - frame;
    }
}
