/*
 * ReadBlockMS.java Copyright (C) 2024 Daniel H. Huson
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

package megan.ms.client.connector;

import jloda.util.StringUtils;
import megan.daa.io.ByteInputStream;
import megan.daa.io.ByteOutputStream;
import megan.daa.io.InputReaderLittleEndian;
import megan.daa.io.OutputWriterLittleEndian;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;

import java.io.IOException;
import java.util.ArrayList;


public class ReadBlockMS implements IReadBlock {
    private long uid;
    private String readHeader;
    private String readSequence;
    private int readWeight;
    private long mateUid;
    private byte mateType;
    private int readLength;
    private float complexity;
    private int numberOfMatches;

    private IMatchBlock[] matchBlocks = new IMatchBlock[0];


    public ReadBlockMS() {

    }

    @Override
    public long getUId() {
        return uid;
    }

    @Override
    public void setUId(long uid) {
        this.uid = uid;
    }

    @Override
    public String getReadName() {
		return StringUtils.getFirstWord(StringUtils.swallowLeadingGreaterOrAtSign(getReadHeader()));
    }


    @Override
    public String getReadHeader() {
        return readHeader;
    }

    @Override
    public void setReadHeader(String readHeader) {
        this.readHeader = readHeader;
    }

    @Override
    public String getReadSequence() {
        return readSequence;
    }

    @Override
    public void setReadSequence(String readSequence) {
        this.readSequence = readSequence;
    }

    @Override
    public int getReadWeight() {
        return readWeight;
    }

    @Override
    public void setReadWeight(int readWeight) {
        this.readWeight = readWeight;
    }

    @Override
    public long getMateUId() {
        return mateUid;
    }

    @Override
    public void setMateUId(long mateUid) {
        this.mateUid = mateUid;
    }

    @Override
    public byte getMateType() {
        return mateType;
    }

    @Override
    public void setMateType(byte mateType) {
        this.mateType = mateType;
    }

    @Override
    public int getReadLength() {
        return readLength;
    }

    @Override
    public void setReadLength(int readLength) {
        this.readLength = readLength;
    }

    @Override
    public float getComplexity() {
        return complexity;
    }

    @Override
    public void setComplexity(float complexity) {
        this.complexity = complexity;
    }

    @Override
    public int getNumberOfMatches() {
        return numberOfMatches;
    }

    @Override
    public void setNumberOfMatches(int numberOfMatches) {
        this.numberOfMatches = numberOfMatches;
    }

    @Override
    public int getNumberOfAvailableMatchBlocks() {
        return matchBlocks.length;
    }

    @Override
    public IMatchBlock getMatchBlock(int i) {
        return matchBlocks[i];
    }

    @Override
    public IMatchBlock[] getMatchBlocks() {
        return matchBlocks;
    }

    @Override
    public void setMatchBlocks(IMatchBlock[] matchBlocks) {
        this.matchBlocks = matchBlocks;
    }

    public static String writeToString(IReadBlock readBlock, boolean includeUid, boolean includeHeader, boolean includeSequence, boolean includeMatches) {
        final ArrayList<String> list = new ArrayList<>();

        if (includeUid)
            list.add(String.valueOf(readBlock.getUId()));
        if (includeHeader)
            list.add(">" + readBlock.getReadHeader());
        if (includeSequence)
            list.add(readBlock.getReadSequence());
        if (includeMatches) {
            list.add("");
            for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
                list.add(readBlock.getMatchBlock(i).getText());
            }
        }
		return StringUtils.toString(list, "\n");
    }

    /**
     * write a match block to bytes
     */
    public static byte[] writeToBytes(String[] cNames, IReadBlock readBlock, boolean includeSequences,boolean includeMatches) throws IOException {
        try (var stream = new ByteOutputStream(); var outs = new OutputWriterLittleEndian(stream)) {
            outs.writeLong(readBlock.getUId());
            outs.writeNullTerminatedString(readBlock.getReadHeader().getBytes());

            if(includeSequences) {
                var sequence=readBlock.getReadSequence();
                if(sequence!=null)
                    outs.writeNullTerminatedString(sequence.getBytes());
                else
                    outs.write((byte) 0);
            }
            else
                outs.write((byte) 0);
            outs.writeInt(readBlock.getReadWeight());
            outs.writeLong(readBlock.getMateUId());
            outs.writeInt(readBlock.getReadLength());
            outs.writeFloat(readBlock.getComplexity());
            outs.writeInt(readBlock.getNumberOfMatches());

            if (includeMatches) {
                outs.writeInt(readBlock.getNumberOfMatches());
                for (int m = 0; m < readBlock.getNumberOfMatches(); m++) {
                    MatchBlockMS.writeToBytes(cNames, readBlock.getMatchBlock(m), outs);
                }
            } else
                outs.writeInt(0);
            return stream.getExactLengthCopy();
        }
    }

    public static ReadBlockMS readFromBytes(String[] cNames, byte[] bytes) throws IOException {
        final ReadBlockMS readBlock = new ReadBlockMS();
        try (InputReaderLittleEndian ins = new InputReaderLittleEndian(new ByteInputStream(bytes, 0, bytes.length))) {
            readBlock.uid = ins.readLong();
            readBlock.readHeader = ins.readNullTerminatedBytes();
            readBlock.readSequence = ins.readNullTerminatedBytes();
            readBlock.readWeight = ins.readInt();
            readBlock.mateUid = ins.readLong();
            readBlock.readLength = ins.readInt();
            readBlock.complexity = ins.readFloat();
            readBlock.numberOfMatches = ins.readInt();

            final int availableMatches = ins.readInt();
            final MatchBlockMS[] matchBlocks = new MatchBlockMS[availableMatches];
            for (int m = 0; m < availableMatches; m++) {
                matchBlocks[m] = MatchBlockMS.getFromBytes(cNames, ins);
            }
            readBlock.matchBlocks = matchBlocks;
        }
        return readBlock;
    }
}

