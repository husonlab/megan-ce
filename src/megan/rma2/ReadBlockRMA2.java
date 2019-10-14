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
import jloda.util.Pair;
import megan.data.*;
import megan.io.IInputReader;
import megan.io.IOutputWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

/**
 * a simple read block
 * Daniel Huson, 9.2010
 */
public class ReadBlockRMA2 implements IReadBlock {
    private long uid;
    private String readHeader;
    private String readSequence;
    private int readWeight;
    private long mateReadUId;
    private byte mateType;
    private int readLength;
    private float complexity;
    private int numberOfMatches;

    private IMatchBlock[] matchBlocks = new MatchBlockRMA2[0];

    /**
     * erase the block  (for reuse)
     */
    public void clear() {
        uid = 0;
        readHeader = null;
        readSequence = null;
        readWeight = 1;
        mateReadUId = 0;
        mateType = 0;
        readLength = 0;
        complexity = 0;
        numberOfMatches = 0;
        matchBlocks = new MatchBlockRMA2[0];
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
        if (readHeader != null) {
            String name = Basic.getFirstWord(readHeader);
            if (name.startsWith(">"))
                return name.substring(1).trim();
            else
                return name;
        }
        return null;
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

    /**
     * set the read sequence
     *
     * @param readSequence
     */
    public void setReadSequence(String readSequence) {
        this.readSequence = readSequence;
    }

    /**
     * gets the uid of the mated read
     *
     * @return uid of mate or 0
     */
    public long getMateUId() {
        return mateReadUId;
    }

    public void setMateUId(long mateReadUId) {
        this.mateReadUId = mateReadUId;
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

    public void setMateType(byte type) {
        this.mateType = type;
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

    public int getReadWeight() {
        return readWeight;
    }

    public void setReadWeight(int readWeight) {
        this.readWeight = readWeight;
    }

    /**
     * get the complexity
     *
     * @param complexity
     */
    public void setComplexity(float complexity) {
        this.complexity = complexity;
    }

    public float getComplexity() {
        return complexity;
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
        if (matchBlocks != null)
            return matchBlocks.length;
        else
            return 0;
    }

    /**
     * get the matches. May be less than the original number of matches (when filtering matches)
     *
     * @return
     */
    public IMatchBlock[] getMatchBlocks() {
        return matchBlocks;
    }

    public void setMatchBlocks(IMatchBlock[] matchBlocks) {
        this.matchBlocks = matchBlocks;
        if (numberOfMatches == 0)
            setNumberOfMatches(matchBlocks.length);
    }

    /**
     * get the i-th match block
     *
     * @param i
     * @return match block
     */
    public IMatchBlock getMatchBlock(int i) {
        if (matchBlocks != null)
            return matchBlocks[i];
        else
            return null;
    }

    /**
     * read a read block from an RMA file
     *
     * @param rma2Formatter
     * @param uid             seek to this position, unless -1
     * @param wantReadText
     * @param wantMatchData
     * @param wantMatchText
     * @param minScore
     * @param textReader
     * @param dataIndexReader
     * @return readblock
     * @throws java.io.IOException
     */
    public static ReadBlockRMA2 read(RMA2Formatter rma2Formatter, long uid, boolean wantReadText, boolean wantMatchData,
                                     boolean wantMatchText, float minScore, float maxExpected, TextStorageReader textReader, IInputReader dataIndexReader) throws IOException {
        ReadBlockRMA2 readBlock = rma2Formatter.isWantLocationData() ? new ReadBlockFromBlast() : new ReadBlockRMA2();

        if (uid == -1)
            uid = dataIndexReader.getPosition();
        else
            dataIndexReader.seek(uid);
        readBlock.setUId(uid);

        ReadBlockRMA2Formatter readBlockFormatter = rma2Formatter.getReadBlockRMA2Formatter();
        readBlockFormatter.read(dataIndexReader);
        readBlock.setReadWeight(readBlockFormatter.hasReadWeight() ? readBlockFormatter.getReadWeight() : 1);
        readBlock.setMateUId(readBlockFormatter.getMateUId());
        readBlock.setMateType(readBlockFormatter.getMateType());
        readBlock.setReadLength(readBlockFormatter.getReadLength());
        readBlock.setComplexity(readBlockFormatter.getComplexity());
        readBlock.setNumberOfMatches(readBlockFormatter.getNumberOfMatches());

        // System.err.println("Number of matches: "+readBlock.getTotalMatches());
        Location location = new Location(dataIndexReader.readChar(), dataIndexReader.readLong(), dataIndexReader.readInt());

        if (wantReadText) {
            Pair<String, String> headerSequence = textReader.getHeaderAndSequence(location);
            if (rma2Formatter.isWantLocationData()) {
                ((ReadBlockFromBlast) readBlock).setTextLocation(location);
            }
            readBlock.setReadHeader(headerSequence.getFirst());
            readBlock.setReadSequence(headerSequence.getSecond());
        }

        if (wantMatchData || wantMatchText) {
            List<IMatchBlock> matchBlocks = new LinkedList<>();
            int skippedMatches = 0;
            for (int i = 0; i < readBlock.getNumberOfMatches(); i++) {
                // System.err.println("Reading match " + i + " : " + dataIndexReader.getPosition());

                MatchBlockRMA2 matchBlock = MatchBlockRMA2.read(rma2Formatter, -1, wantMatchData, wantMatchText, minScore, maxExpected, textReader, dataIndexReader);
                if (matchBlock == null) // bitscore too low
                {
                    skippedMatches = (readBlock.getNumberOfMatches() - (i + 1));
                    break;
                }
                matchBlocks.add(matchBlock);
            }
            readBlock.setMatchBlocks(matchBlocks.toArray(new IMatchBlock[0]));
            if (skippedMatches > 0) {
                // need to skip the rest of the bits:
                dataIndexReader.skipBytes(skippedMatches * MatchBlockRMA2.getBytesInIndexFile(rma2Formatter.getMatchBlockRMA2Formatter()));
            }
        } else // skip all matches
            dataIndexReader.skipBytes(readBlock.getNumberOfMatches() * MatchBlockRMA2.getBytesInIndexFile(rma2Formatter.getMatchBlockRMA2Formatter()));
        return readBlock;
    }

    /**
     * write a given read block to a RMA file
     *
     * @param rma2Formatter
     * @param readBlock
     * @param dumpWriter
     * @param indexWriter
     * @throws java.io.IOException
     */
    public static void write(RMA2Formatter rma2Formatter, IReadBlockWithLocation readBlock, IOutputWriter dumpWriter, IOutputWriter indexWriter) throws IOException {
        readBlock.setUId(indexWriter.getPosition()); // uid is position in index file

        Location location = readBlock.getTextLocation();

        if (dumpWriter != null) // need to write text to dump file and set location
        {
            long dataDumpPos = dumpWriter.getPosition();
            if (readBlock.getReadSequence() != null)
                dumpWriter.writeString(readBlock.getReadHeader() + "\n" + readBlock.getReadSequence());
            else
                dumpWriter.writeString(readBlock.getReadHeader() + "\n");
            if (location == null) {
                location = new Location();
                readBlock.setTextLocation(location);
            }
            location.setFileId(0);
            location.setPosition(dataDumpPos);
            location.setSize((int) (dumpWriter.getPosition() - dataDumpPos));
        }

        // write read to dataindex:
        // System.err.println("Writing read " + readBlock.getReadName() + ": " + indexWriter.getPosition());
        // don't write uid!
        ReadBlockRMA2Formatter readBlockFormatter = rma2Formatter.getReadBlockRMA2Formatter();
        readBlockFormatter.setReadWeight(readBlock.getReadWeight());
        readBlockFormatter.setMateUId(readBlock.getMateUId());
        readBlockFormatter.setMateType(readBlock.getMateType());
        readBlockFormatter.setReadLength(readBlock.getReadLength());
        readBlockFormatter.setComplexity(readBlock.getComplexity());
        readBlockFormatter.setNumberOfMatches(readBlock.getNumberOfMatches());
        readBlockFormatter.write(indexWriter);

        if (location != null) {
            indexWriter.writeChar((char) location.getFileId());
            indexWriter.writeLong(location.getPosition());
            indexWriter.writeInt(location.getSize());
        } else {
            indexWriter.writeChar((char) -1);
            indexWriter.writeLong(-1);
            indexWriter.writeInt(-1);
        }

        for (int i = 0; i < readBlock.getNumberOfMatches(); i++) {
            IMatchBlockWithLocation matchBlock = readBlock.getMatchBlock(i);
            // System.err.println("Writing match " + i + ": " + dataIndexWriter.getPosition());
            MatchBlockRMA2.write(rma2Formatter, matchBlock, dumpWriter, indexWriter);
        }

        //System.err.println(readBlock.toString());
    }

    public String toString() {
        StringWriter w = new StringWriter();
        w.write("Read uid: " + uid + "----------------------\n");
        if (readHeader != null)
            w.write("readHeader: " + readHeader + "\n");
        if (readSequence != null)
            w.write("readSequence: " + readSequence + "\n");
        if (readWeight != 1)
            w.write("readWeight: " + readWeight + "\n");
        if (mateReadUId != 0)
            w.write("mateReadUId: " + mateReadUId + "\n");
        if (readLength != 0)
            w.write("readLength: " + readLength + "\n");
        if (complexity != 0)
            w.write("complexity: " + complexity + "\n");
        w.write("numberOfMatches: " + numberOfMatches + "\n");

        for (IMatchBlock matchBlock : matchBlocks) w.write(matchBlock.toString());
        return w.toString();
    }


    /**
     * add the matchblocks to the readblock
     *
     * @param matchBlocks
     */
    public void addMatchBlocks(SortedSet<IMatchBlock> matchBlocks) {
        setMatchBlocks(matchBlocks.toArray(new IMatchBlock[0]));
    }
}
