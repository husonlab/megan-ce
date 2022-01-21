/*
 * MatchBlockMS.java Copyright (C) 2022 Daniel H. Huson
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * matchblock for megan server
 * Daniel Huson, 8.2020
 */
public class MatchBlockMS implements IMatchBlock {
    private long uid;
    private int taxonId;
    private float bitScore;
    private float percentIdentity;
    private String refSeqId;
    private float expected;
    private int length;
    private boolean ignore;
    private String text;
    private final Map<String, Integer> class2id = new HashMap<>();

    private int alignedQueryStart;
    private int alignedQueryEnd;
    private int refLength;

    public MatchBlockMS() {
    }

    public long getUId() {
        return uid;
    }

    public void setUId(long uid) {
        this.uid = uid;
    }


    @Override
    public int getTaxonId() {
        return taxonId;
    }

    @Override
    public void setTaxonId(int taxonId) {
        this.taxonId = taxonId;
    }

    @Override
    public float getBitScore() {
        return bitScore;
    }


    @Override
    public void setBitScore(float bitScore) {
        this.bitScore = bitScore;
    }


    @Override
    public float getPercentIdentity() {
        return percentIdentity;
    }


    @Override
    public void setPercentIdentity(float percentIdentity) {
        this.percentIdentity = percentIdentity;
    }


    @Override
    public String getRefSeqId() {
        return refSeqId;
    }


    @Override
    public void setRefSeqId(String refSeqId) {
        this.refSeqId = refSeqId;
    }


    @Override
    public float getExpected() {
        return expected;
    }


    @Override
    public void setExpected(float expected) {
        this.expected = expected;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public boolean isIgnore() {
        return ignore;
    }

    @Override
    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getTextFirstWord() {
		return StringUtils.getFirstWord(getText());
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public int getId(String cName) {
        final Integer id = class2id.get(cName);
        return id == null ? 0 : id;
    }

    @Override
    public void setId(String cName, Integer id) {
        class2id.put(cName, id);
    }

    @Override
    public int[] getIds(String[] cNames) {
        return new int[0];
    }


    @Override
    public int getAlignedQueryStart() {
        return alignedQueryStart;
    }


    @Override
    public int getAlignedQueryEnd() {
        return alignedQueryEnd;
    }


    @Override
    public int getRefLength() {
        return refLength;
    }

    public static byte[] writeToBytes(String[] cNames, IMatchBlock matchBlock) throws IOException {
        try (ByteOutputStream stream = new ByteOutputStream();
             OutputWriterLittleEndian outs = new OutputWriterLittleEndian(stream)) {
            writeToBytes(cNames, matchBlock, outs);
            return stream.getExactLengthCopy();
        }
    }

    public static void writeToBytes(String[] cNames, IMatchBlock matchBlock, OutputWriterLittleEndian outs) throws IOException {
        outs.writeLong(matchBlock.getUId());
        outs.writeInt(matchBlock.getTaxonId());
        outs.writeFloat(matchBlock.getBitScore());
        outs.writeFloat(matchBlock.getPercentIdentity());
        outs.writeFloat(matchBlock.getExpected());
        outs.writeInt(matchBlock.getLength());
        outs.writeNullTerminatedString(matchBlock.getText().getBytes());
        final int[] ids = matchBlock.getIds(cNames);

        outs.writeInt(ids.length);
        for (int id : ids) {
            outs.writeInt(id);
        }
        outs.writeInt(matchBlock.getAlignedQueryStart());
        outs.writeInt(matchBlock.getAlignedQueryEnd());
        outs.writeInt(matchBlock.getRefLength());
    }

    public static MatchBlockMS getFromBytes(String[] cNames, byte[] bytes) throws IOException {
        try (InputReaderLittleEndian ins = new InputReaderLittleEndian(new ByteInputStream(bytes, 0, bytes.length))) {
            return getFromBytes(cNames, ins);
        }
    }

    public static MatchBlockMS getFromBytes(String[] cNames, InputReaderLittleEndian ins) throws IOException {
        final MatchBlockMS matchBlock = new MatchBlockMS();
        matchBlock.uid = ins.readLong();
        matchBlock.taxonId = ins.readInt();
        matchBlock.bitScore = ins.readFloat();
        matchBlock.percentIdentity = ins.readFloat();
        matchBlock.expected = ins.readFloat();
        matchBlock.length = ins.readInt();
        matchBlock.text = ins.readNullTerminatedBytes();
        int count = ins.readInt();
        for (int i = 0; i < count; i++) {
            matchBlock.setId(cNames[i], ins.readInt());
        }
        matchBlock.alignedQueryStart = ins.readInt();
        matchBlock.alignedQueryEnd = ins.readInt();
        matchBlock.refLength = ins.readInt();
        return matchBlock;
    }
}
