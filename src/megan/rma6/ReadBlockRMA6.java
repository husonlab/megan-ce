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
package megan.rma6;

import jloda.util.Basic;
import jloda.util.BlastMode;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.io.IInputReader;
import megan.parsers.sam.SAMMatch;
import megan.util.ReadMagnitudeParser;

import java.io.IOException;

/**
 * ReadBlock for RMA6
 * Daniel Huson, 6.2015
 */
public class ReadBlockRMA6 implements IReadBlock {
    private final BlastMode blastMode;
    private final boolean pairedReads;

    private final String[] cNames;
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
        this.blastMode = blastMode;
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
     *
     * @param reader
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

            String querySequence = getReadSequence();
            String queryQuality = null;

            // if the read is not given, find the longest reported sequence, inserting any hard clip that it might have
            if (querySequence == null) {
                int offset = 0;
                int queryHardClip = 0;
                for (int i = 0; i < numberOfMatches; i++) {
                    int end = matchesText.indexOf('\n', offset + 1);
                    if (end == -1)
                        end = matchesText.length();
                    final String aLine = matchesText.substring(offset, end);
                    offset = end + 1;
                    final String[] tokens = Basic.split(aLine, '\t');
                    if (tokens.length > 10) {
                        final String query = tokens[9];
                        if (query != null && (querySequence == null || querySequence.length() < query.length())) {
                            querySequence = query;
                            queryQuality = tokens[10];
                            queryHardClip = parseLeadingHardClip(tokens[5]);
                        }
                    }
                }
                // note: must insert 0's here because SAMMatch looks for initial 0 to identify queries that have had the hard-clipped sequence inserted as 0's
                querySequence = insertLeading0Characters(querySequence, queryHardClip);
                // todo: if we want to use the quality values, then we must uncomment the next line:
                // queryQuality=insertLeading0Characters(queryQuality,queryHardClip);
            }

            // parse and copy the matches that we want to keep
            int offset = 0;
            int matchCount = 0;
            final IMatchBlock[] copies = new MatchBlockRMA6[numberOfMatches]; // need to copy matches we want to keep

            for (int i = 0; i < numberOfMatches; i++) {
                int end = matchesText.indexOf('\n', offset + 1);
                if (end == -1)
                    end = matchesText.length();
                final String aLine = matchesText.substring(offset, end);
                offset = end + 1;
                try {
                    final SAMMatch samMatch = new SAMMatch(blastMode);
                    final String[] tokens = Basic.split(aLine, '\t');
                    if (tokens.length > 10) {
                        if ((tokens[9].equals("*") || tokens[9].length() == 0) && querySequence != null) {
                            tokens[9] = querySequence;
                            if (queryQuality != null)
                                tokens[10] = queryQuality;
                        }
                    }

                    samMatch.parse(tokens, tokens.length);

                    if (samMatch.getRefName() != null) {
                        final MatchBlockRMA6 matchBlock = (MatchBlockRMA6) matchBlocks[i];
                        matchBlock.setFromSAM(samMatch);
                        if (matchBlock.getBitScore() >= minScore && matchBlock.getExpected() <= maxExpected)
                            copies[matchCount++] = matchBlock; // this match is ok, keep it
                    }
                } catch (IOException ex) {
                    System.err.println("RMA6 Parse error: " + ex.getMessage() + ", numberOfMatches=" + numberOfMatches + ", i=" + i + " line=" + aLine);
                }
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


    /**
     * gets the leading hard clip
     *
     * @param cigar
     * @return leading hard clip or 0
     */
    private static int parseLeadingHardClip(String cigar) {
        for (int i = 0; i < cigar.length(); i++) {
            char ch = cigar.charAt(i);
            if (!Character.isDigit(ch)) {
                if (Character.toUpperCase(ch) == 'H' && i > 0)
                    return Integer.parseInt(cigar.substring(0, i));
                else
                    return 0;
            }
        }
        return 0;
    }

    /**
     * inserts the given number of 0 characters
     *
     * @param sequence
     * @param numberOfLeading0Characters
     * @return extended string
     */
    private static String insertLeading0Characters(String sequence, int numberOfLeading0Characters) {
        if (numberOfLeading0Characters == 0)
            return sequence;
        else
            return new String(new char[numberOfLeading0Characters]) + sequence;
    }
}
