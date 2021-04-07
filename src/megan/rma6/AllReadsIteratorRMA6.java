/*
 * AllReadsIteratorRMA6.java Copyright (C) 2021. Daniel H. Huson
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
 *
 */
package megan.rma6;

import jloda.util.Basic;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;

import java.io.IOException;

/**
 * Iterates over all reads in an RMA6 file
 * Created by huson on 5/16/14.
 */
public class AllReadsIteratorRMA6 implements IReadBlockIterator {

    private final ReadBlockGetterRMA6 readBlockGetter;
    private int countReads = 0;

    /**
     * constructor
     *
     * @param wantMatches
     * @param file
     */
    public AllReadsIteratorRMA6(boolean wantReadSequence, boolean wantMatches, RMA6File file, float minScore, float maxExpected) throws IOException {
        readBlockGetter = new ReadBlockGetterRMA6(file, wantReadSequence, wantMatches, minScore, maxExpected, true, false);
    }

    @Override
    public String getStats() {
        return "Reads: " + countReads;
    }

    @Override
    public void close() {
        readBlockGetter.close();
    }

    @Override
    public long getMaximumProgress() {
        return readBlockGetter.getEnd() - readBlockGetter.getStart();
    }

    @Override
    public long getProgress() {
        return readBlockGetter.getPosition() - readBlockGetter.getStart();
    }

    @Override
    public boolean hasNext() {
        return readBlockGetter.getPosition() < readBlockGetter.getEnd();
    }

    @Override
    public IReadBlock next() {
        if (!hasNext())
            return null;
        try {
            final IReadBlock readBlock = readBlockGetter.getReadBlock(-1);
            countReads++;
            return readBlock;
        } catch (IOException e) {
            Basic.caught(e);
            return null;
        }
    }

    @Override
    public void remove() {

    }
}
