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

package megan.daa.io;

/**
 * DAA query record
 * Daniel Huson, 8.2015
 */
public class DAAQueryRecord {
    private DAAParser daaParser;
    private byte[] queryName;
    private byte[] sourceSequence;
    private final byte[][] context = new byte[6][];
    private int queryLength;

    // additional stuff:
    private long location; // location in file

    /**
     * constructor
     */
    public DAAQueryRecord() {
    }

    /**
     * constructor
     *
     * @param daaParser
     */
    public DAAQueryRecord(DAAParser daaParser) {
        this.setDaaParser(daaParser);
    }

    /**
     * parses a buffer
     *
     * @param buffer
     */
    public void parseBuffer(ByteInputBuffer buffer) {
        queryLength = buffer.readIntLittleEndian();
        queryName = buffer.readBytesNullTerminated();
        int flags = buffer.readCharBigEndian();

        boolean hasN = ((flags & 1) == 1);

        switch (daaParser.getHeader().getAlignMode()) {
            case blastp: { // todo: untested
                byte[] packed = PackedSequence.readPackedSequence(buffer, queryLength, 5);
                sourceSequence = context[0] = PackedSequence.getUnpackedSequence(packed, queryLength, 5);
                break;
            }
            case blastx: {
                byte[] packed = PackedSequence.readPackedSequence(buffer, queryLength, hasN ? 3 : 2);
                sourceSequence = PackedSequence.getUnpackedSequence(packed, queryLength, hasN ? 3 : 2);
                byte[][] sixFrameTranslation = Translator.getSixFrameTranslations(sourceSequence);
                System.arraycopy(sixFrameTranslation, 0, context, 0, sixFrameTranslation.length);
                break;
            }
            case blastn: { // todo: untested
                byte[] packed = PackedSequence.readPackedSequence(buffer, queryLength, hasN ? 3 : 2);
                sourceSequence = PackedSequence.getUnpackedSequence(packed, queryLength, hasN ? 3 : 2);
                context[0] = sourceSequence;
                context[1] = Translator.getReverseComplement(sourceSequence);
                break;
            }
            default:
        }
    }

    public DAAParser getDaaParser() {
        return daaParser;
    }

    private void setDaaParser(DAAParser daaParser) {
        this.daaParser = daaParser;
    }

    public byte[] getQueryName() {
        return queryName;
    }

    public byte[] getSourceSequence() {
        return sourceSequence;
    }

    public byte[][] getContext() {
        return context;
    }

    public byte[] getQueryFastA(byte[] alphabet) {
        ByteOutputBuffer buffer = new ByteOutputBuffer();
        buffer.write((byte) '>');
        buffer.write(queryName);
        buffer.write((byte) '\n');
        buffer.write(Translator.translate(sourceSequence, alphabet, 0, sourceSequence.length));
        buffer.write((byte) '\n');
        return buffer.copyBytes();
    }

    public long getLocation() {
        return location;
    }

    public void setLocation(long location) {
        this.location = location;
    }

    public int getQueryLength() {
        return queryLength;
    }
}
