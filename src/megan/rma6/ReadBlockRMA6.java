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
package megan.rma6;

import jloda.util.Basic;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.io.IInputReader;
import megan.parsers.blast.BlastMode;
import megan.parsers.sam.SAMMatch;
import megan.util.ReadMagnitudeParser;

import java.io.IOException;

/**
 * ReadBlock for RMA6
 * Daniel Huson, 6.2015
 */
public class ReadBlockRMA6 implements IReadBlock {
    private final SAMMatch tmpSAMMatch; // sam match is used for converting from SAM to MatchBlock
    private final boolean pairedReads;

    private String[] cNames;
    private long uid;
    private String readHeader;
    private String readSequence;
    private int readLength;
    private float readComplexity;
    private int readWeight;
    private long mateUid;
    private byte mateType;
    private int numberOfMatches;
    private IMatchBlock[] matchBlocks;

    /**
     * Constructor
     *
     * @param blastMode
     */
    public ReadBlockRMA6(BlastMode blastMode, boolean pairedReads, String[] cNames) {
        tmpSAMMatch = new SAMMatch(blastMode);
        this.pairedReads = pairedReads;
        this.cNames = cNames;

    }

    /**
     * get the unique identifier for this read (unique within a dataset).
     * In an RMA file, this is always the file position for the read
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
     * get the name of the read (first word in header)
     *
     * @return name
     */
    public String getReadName() {
        return readHeader != null ? Basic.swallowLeadingGreaterSign(Basic.getFirstWord(readHeader)) : null;
    }

    /**
     * get the fastA header for the read
     *
     * @return fastA header
     */
    public String getReadHeader() {
        return readHeader;
    }

    public void setReadHeader(String readHeader) {
        this.readHeader = readHeader;
    }

    /**
     * get the sequence of the read
     *
     * @return sequence
     */
    public String getReadSequence() {
        return readSequence;
    }

    public void setReadSequence(String readSequence) {
        this.readSequence = readSequence;
    }

    /**
     * gets the uid of the mated read
     *
     * @return uid of mate or 0
     */
    public long getMateUId() {
        return mateUid;
    }

    public void setMateUId(long mateUid) {
        this.mateUid = mateUid;
    }

    /**
     * get the mate type
     * Possible values: FIRST_MATE, SECOND_MATE
     *
     * @return mate type
     */
    public byte getMateType() {
        return mateType;
    }

    public void setMateType(byte mateType) {
        this.mateType = mateType;
    }

    /**
     * set the read length
     *
     * @param readLength
     */
    public void setReadLength(int readLength) {
        this.readLength = readLength;
    }

    public int getReadLength() {
        return readLength;
    }

    /**
     * get the complexity
     *
     * @param readComplexity
     */
    public void setComplexity(float readComplexity) {
        this.readComplexity = readComplexity;
    }

    public float getComplexity() {
        return readComplexity;
    }

    /**
     * set the weight of a read
     *
     * @param readWeight
     */

    public void setReadWeight(int readWeight) {
        this.readWeight = readWeight;
    }

    /**
     * get the weight of a read
     *
     * @return weight
     */

    public int getReadWeight() {
        return readWeight;
    }

    /**
     * get  the original number of matches
     *
     * @return number of matches
     */
    public int getNumberOfMatches() {
        return numberOfMatches;
    }

    public void setNumberOfMatches(int numberOfMatches) {
        this.numberOfMatches = numberOfMatches;
    }

    /**
     * gets the current number of matches available
     *
     * @return
     */
    public int getNumberOfAvailableMatchBlocks() {
        return matchBlocks != null ? matchBlocks.length : 0;
    }

    /**
     * get the matches. May be less than the original number of matches (when filtering matches)
     *
     * @return matches
     */
    public IMatchBlock[] getMatchBlocks() {
        return matchBlocks;
    }

    public void setMatchBlocks(IMatchBlock[] matchBlocks) {
        this.matchBlocks = matchBlocks;
    }

    /**
     * get the i-th match block
     *
     * @param i
     * @return match block
     */
    public IMatchBlock getMatchBlock(int i) {
        return matchBlocks[i];
    }

    /**
     * reads a read block
     *  @param reader
     * @param wantReadSequence
     * @param wantMatches
     */
    public void read(IInputReader reader, boolean wantReadSequence, boolean wantMatches, float minScore, float maxExpected) throws IOException {
        setUId(reader.getPosition());
        if (pairedReads)
            mateUid = reader.readLong();

        String readText = reader.readString();
        if (readText.length() > 0) {
            int pos = readText.indexOf('\n');
            if (pos == -1) // only one line...
            {
                setReadHeader(readText);
                setReadWeight(ReadMagnitudeParser.parseMagnitude(getReadHeader()));
                setReadSequence(null);
                setReadLength(0);
            } else if (pos > 0) { // looks like more than one line
                setReadHeader(readText.substring(0, pos));
                setReadWeight(ReadMagnitudeParser.parseMagnitude(getReadHeader()));
                if (pos + 1 < readText.length()) {
                    String sequence = Basic.removeAllWhiteSpaces(readText.substring(pos + 1));
                    setReadSequence(wantReadSequence ? sequence : null);
                    setReadLength(sequence.length());
                } else {
                    setReadSequence(null);
                    setReadLength(0);
                }
            } else {
                setReadHeader(null);
                setReadSequence(null);
                setReadLength(0);
            }
        }
        numberOfMatches = reader.readInt();
        if (wantMatches) {
            // construct match blocks:
            matchBlocks = new MatchBlockRMA6[numberOfMatches];
            for (int i = 0; i < numberOfMatches; i++)
                matchBlocks[i] = new MatchBlockRMA6();
            // for each match, read taxon-id and classification ids:
            for (int i = 0; i < numberOfMatches; i++) {
                for (String cName : cNames) {
                    matchBlocks[i].setId(cName, reader.readInt()); // read 4*fName.length bytes
                }
            }

            // read the text for all matches:
            final String matchesText = reader.readString(); // assume each line is in SAM format and ends on \n
            int offset = 0;
            int matchCount = 0;
            final IMatchBlock[] copies = new MatchBlockRMA6[numberOfMatches]; // need to copy matches we want to keep

            for (int i = 0; i < numberOfMatches; i++) {
                int end = matchesText.indexOf('\n', offset + 1);
                if (end == -1)
                    end = matchesText.length();
                final String aLine = matchesText.substring(offset, end);
                tmpSAMMatch.parse(aLine);
                ((MatchBlockRMA6) matchBlocks[matchCount]).setFromSAM(tmpSAMMatch);
                offset = end + 1;
                if (matchBlocks[matchCount].getBitScore() >= minScore && matchBlocks[matchCount].getExpected() <= maxExpected)
                    copies[matchCount++] = matchBlocks[i]; // this match is ok, keep it
            }
            if (matchCount < matchBlocks.length) { // some matches didn't meet the minScore or maxExpected criteria, resize
                matchBlocks = new MatchBlockRMA6[matchCount];
                System.arraycopy(copies, 0, matchBlocks, 0, matchCount);
            }
        } else {
            reader.skipBytes(cNames.length * numberOfMatches * 4); // skip taxon and cName ids
            reader.skipBytes(Math.abs(reader.readInt())); // skip text
        }
    }
}
