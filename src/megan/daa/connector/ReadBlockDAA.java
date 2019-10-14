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
import megan.daa.io.DAAMatchRecord;
import megan.daa.io.DAAQueryRecord;
import megan.daa.io.Translator;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.util.ReadMagnitudeParser;

/**
 * ReadBlock for DAA
 * Daniel Huson, 8.2015
 */
public class ReadBlockDAA implements IReadBlock {
    private boolean wantReadSequences; // to mimic other implementations of read sequences, we only support get read sequence, only if originally requested
    private DAAQueryRecord queryRecord;
    private int numberOfMatches;
    private IMatchBlock[] matchBlocks;

    private int readWeight = 1;

    /**
     * Constructor
     */
    public ReadBlockDAA() {
    }

    /**
     * get the unique identifier for this read (unique within a dataset).
     * In an RMA file, this is always the file position for the read
     *
     * @return uid
     */
    public long getUId() {
        return queryRecord.getLocation();
    }

    public void setUId(long uid) {
        queryRecord.setLocation(uid);
    }

    /**
     * get the name of the read (first word in header)
     *
     * @return name
     */
    public String getReadName() {
        return Basic.toString(queryRecord.getQueryName());
    }

    /**
     * get the fastA header for the read
     *
     * @return fastA header
     */
    public String getReadHeader() {
        return Basic.toString(queryRecord.getQueryName());
    }

    public void setReadHeader(String readHeader) {
        System.err.println("Not implemented");
    }

    /**
     * get the sequence of the read
     *
     * @return sequence
     */
    public String getReadSequence() {
        return (wantReadSequences ? Basic.toString(Translator.translate(queryRecord.getSourceSequence(), queryRecord.getDaaParser().getSourceAlphabet())) : null);
    }

    public void setReadSequence(String readSequence) {
        System.err.println("Not implemented");
    }

    /**
     * gets the uid of the mated read
     *
     * @return uid of mate or 0
     */
    public long getMateUId() {
        return 0;
    }

    public void setMateUId(long mateUid) {
        System.err.println("Not implemented");
    }

    /**
     * get the mate type
     * Possible values: FIRST_MATE, SECOND_MATE
     *
     * @return mate type
     */
    public byte getMateType() {
        return 0;
    }

    public void setMateType(byte mateType) {
        System.err.println("Not implemented");
        ;
    }

    /**
     * set the read length
     *
     * @param readLength
     */
    public void setReadLength(int readLength) {
        System.err.println("Not implemented");
    }

    public int getReadLength() {
        return queryRecord.getSourceSequence().length;
    }

    /**
     * get the readComplexity
     *
     * @param readComplexity
     */
    public void setComplexity(float readComplexity) {
    }

    public float getComplexity() {
        return 0;
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
     * set the read block and its match blocks from records
     *
     * @param queryRecord
     * @param matchRecords
     * @param wantMatches
     */
    public void setFromQueryAndMatchRecords(DAAQueryRecord queryRecord, DAAMatchRecord[] matchRecords, boolean wantReadSequences, boolean wantMatches, float minScore, float maxExpected) {
        this.wantReadSequences = wantReadSequences;
        this.queryRecord = queryRecord;
        numberOfMatches = matchRecords.length;

        if (wantMatches) {
            matchBlocks = new IMatchBlock[matchRecords.length];
            int numberGoodMatches = 0;
            for (DAAMatchRecord matchRecord : matchRecords) {
                MatchBlockDAA matchBlockDAA = new MatchBlockDAA(queryRecord.getDaaParser(), matchRecord);
                if (matchBlockDAA.getBitScore() >= minScore && matchBlockDAA.getExpected() <= maxExpected) {
                    matchBlocks[numberGoodMatches++] = matchBlockDAA;
                }
            }
            if (numberGoodMatches < numberOfMatches) {
                final IMatchBlock[] tmp = new MatchBlockDAA[numberGoodMatches];
                System.arraycopy(matchBlocks, 0, tmp, 0, numberGoodMatches);
                matchBlocks = tmp;
            }
        } else {
            matchBlocks = new IMatchBlock[0];
            numberOfMatches = 0;
        }

        setReadWeight(ReadMagnitudeParser.parseMagnitude(getReadHeader()));
    }
}
