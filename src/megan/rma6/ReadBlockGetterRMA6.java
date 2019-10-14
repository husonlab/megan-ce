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
import megan.data.IReadBlock;
import megan.data.IReadBlockGetter;
import megan.io.IInputReader;

import java.io.IOException;

/**
 * Read block getter
 * Daniel Huson, 4.2015
 */
public class ReadBlockGetterRMA6 implements IReadBlockGetter {
    private final RMA6File rma6File;
    private final boolean wantReadSequence;
    private final boolean wantMatches;
    private final float minScore;
    private final float maxExpected;
    private final boolean streamOnly;
    private final ReadBlockRMA6 reuseableReadBlock;

    private final long start;
    private final long end;

    private final IInputReader reader;

    /**
     * constructor
     *
     * @param rma6File
     * @param wantReadSequence
     * @param streamOnly
     * @param reuseReadBlockObject
     * @throws IOException
     */
    public ReadBlockGetterRMA6(RMA6File rma6File, boolean wantReadSequence, boolean wantMatches, float minScore, float maxExpected, boolean streamOnly, boolean reuseReadBlockObject) throws IOException {
        this.rma6File = rma6File;
        this.wantReadSequence = wantReadSequence;
        this.wantMatches = wantMatches;
        this.minScore = minScore;
        this.maxExpected = maxExpected;
        this.streamOnly = streamOnly;

        this.start = rma6File.getFooterSectionRMA6().getStartReadsSection();
        this.end = rma6File.getFooterSectionRMA6().getEndReadsSection();

        reader = rma6File.getReader();
        if (streamOnly)
            reader.seek(start);
        if (reuseReadBlockObject)
            reuseableReadBlock = new ReadBlockRMA6(rma6File.getHeaderSectionRMA6().getBlastMode(), rma6File.getHeaderSectionRMA6().isPairedReads(), rma6File.getHeaderSectionRMA6().getMatchClassNames());
        else
            reuseableReadBlock = null;
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
            final ReadBlockRMA6 readBlock = (reuseableReadBlock == null ? new ReadBlockRMA6(rma6File.getHeaderSectionRMA6().getBlastMode(), rma6File.getHeaderSectionRMA6().isPairedReads(), rma6File.getHeaderSectionRMA6().getMatchClassNames()) : reuseableReadBlock);
            readBlock.read(reader, wantReadSequence, wantMatches, minScore, maxExpected);
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
            return rma6File.getReader().getPosition();
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * get total number of reads
     *
     * @return total number of reads
     */
    @Override
    public long getCount() {
        return rma6File.getFooterSectionRMA6().getNumberOfReads();
    }
}
