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
import jloda.util.Pair;
import megan.daa.io.*;
import megan.data.IReadBlock;
import megan.data.IReadBlockGetter;
import megan.io.FileInputStreamAdapter;
import megan.io.FileRandomAccessReadOnlyAdapter;

import java.io.IOException;

/**
 * Read block getter
 * Daniel Huson, 8.2015
 */
public class ReadBlockGetterDAA implements IReadBlockGetter {
    private final DAAParser daaParser;

    private final boolean wantReadSequences;
    private final boolean wantMatches;
    private final float minScore;
    private final float maxExpected;

    private final boolean streamOnly;
    private final ReadBlockDAA reuseableReadBlock;

    private final long start;
    private final long end;

    private final InputReaderLittleEndian reader;
    private final InputReaderLittleEndian refReader;

    private final ByteInputBuffer inputBuffer = new ByteInputBuffer();
    private final DAAMatchRecord[] daaMatchRecords = new DAAMatchRecord[50000]; // when parsing long reads the number can be quite big

    private final boolean longReads;

    /**
     * constructor
     *
     * @param daaHeader
     * @param wantMatches
     * @param streamOnly
     * @param reuseReadBlockObject
     * @throws IOException
     */
    public ReadBlockGetterDAA(DAAHeader daaHeader, boolean wantReadSequences, boolean wantMatches, float minScore, float maxExpected, boolean streamOnly, boolean reuseReadBlockObject, boolean longReads) throws IOException {
        this.daaParser = new DAAParser(daaHeader);
        if (daaHeader.getNumberOfReferences() == 0)
            daaHeader.loadReferences(!streamOnly || !wantMatches);
        if (daaHeader.getNumberOfRefAnnotations() == 0)
            daaHeader.loadRefAnnotations();

        this.wantReadSequences = wantReadSequences;
        this.wantMatches = wantMatches;
        this.minScore = minScore;
        this.maxExpected = maxExpected;
        this.streamOnly = streamOnly;

        this.start = daaHeader.computeBlockStart(daaHeader.getAlignmentsBlockIndex());
        this.end = start + daaHeader.getBlockSize(daaHeader.getAlignmentsBlockIndex());

        reader = new InputReaderLittleEndian(streamOnly ? new FileInputStreamAdapter(daaHeader.getFileName()) : new FileRandomAccessReadOnlyAdapter(daaHeader.getFileName()));
        refReader = new InputReaderLittleEndian(new FileRandomAccessReadOnlyAdapter(daaHeader.getFileName()));

        // todo: 'stream only' doesn't work when need to grab reference headers
        //reader = new InputReaderLittleEndian(new FileRandomAccessReadOnlyAdapter(daaHeader.getFileName()));

        if (streamOnly)
            reader.seek(start);

        if (reuseReadBlockObject)
            reuseableReadBlock = new ReadBlockDAA();
        else
            reuseableReadBlock = null;

        this.longReads = longReads;
    }

    /**
     * gets the read block associated with the given uid
     *
     * @param uid or -1, if in streaming mode
     * @return read block or null
     * @throws IOException
     */
    @Override
    public IReadBlock getReadBlock(long uid) throws IOException {
        if (uid == -1) {
            if (!streamOnly)
                throw new IOException("getReadBlock(uid=" + uid + ") failed: not streamOnly");
        } else {
            if (streamOnly) {
                throw new IOException("getReadBlock(uid=" + uid + ") failed: streamOnly");
            }
            reader.seek(uid);
        }

        if (reader.getPosition() < end) {
            if (uid >= 0) {
            }
            final ReadBlockDAA readBlock = (reuseableReadBlock == null ? new ReadBlockDAA() : reuseableReadBlock);

            final Pair<DAAQueryRecord, DAAMatchRecord[]> pair = daaParser.readQueryAndMatches(reader, refReader, wantMatches, daaMatchRecords.length, inputBuffer, daaMatchRecords, longReads);
            readBlock.setFromQueryAndMatchRecords(pair.get1(), pair.get2(), wantReadSequences, wantMatches, minScore, maxExpected);
            return readBlock;
        }
        return null;
    }

    /**
     * closes the accessor
     *
     * @throws IOException
     */
    @Override
    public void close() {
        try {
            reader.close();
            refReader.close();
        } catch (IOException e) {
            Basic.caught(e);
        }
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public long getPosition() {
        try {
            return reader.getPosition();
        } catch (IOException e) {
            return -1;
        }
    }

    public DAAHeader getDAAHeader() {
        return daaParser.getHeader();
    }

    /**
     * get total number of reads
     *
     * @return total number of reads
     */
    @Override
    public long getCount() {
        return daaParser.getHeader().getQueryRecords();
    }
}
