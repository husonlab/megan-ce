/*
 * DAA2SAMIterator.java Copyright (C) 2022 Daniel H. Huson
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

package megan.daa.io;

import jloda.util.Pair;
import megan.parsers.blast.ISAMIterator;

import java.io.IOException;

/**
 * iterates over all SAM records in a DAA file
 */
public class DAA2SAMIterator implements ISAMIterator {
    private final DAA2QuerySAMIterator daa2QuerySAMIterator;

    private Pair<byte[], byte[]> next;

    private boolean parseLongReads;

    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     */
    public DAA2SAMIterator(String fileName, int maxMatchesPerRead, boolean parseLongReads) throws IOException {
        this.daa2QuerySAMIterator = new DAA2QuerySAMIterator(fileName, maxMatchesPerRead, parseLongReads);
    }

    /**
     * gets the next matches
     *
     * @return number of matches
     */
    @Override
    public int next() {
        next = daa2QuerySAMIterator.next();
        return countNewLines(next.getSecond());
    }

    /**
     * is there more data?
     *
     * @return true, if more data available
     */
    @Override
    public boolean hasNext() {
        return daa2QuerySAMIterator.hasNext();
    }

    /**
     * gets the matches text
     *
     * @return matches text
     */
    @Override
    public byte[] getMatchesText() {
        return next.getSecond();
    }

    @Override
    public byte[] getQueryText() {
        return next.getFirst();
    }

    /**
     * length of matches text
     *
     * @return length of text
     */
    @Override
    public int getMatchesTextLength() {
        return next.getSecond().length;
    }

    @Override
    public long getMaximumProgress() {
        return daa2QuerySAMIterator.getMaximumProgress();
    }

    @Override
    public long getProgress() {
        return daa2QuerySAMIterator.getProgress();
    }

    @Override
    public void close() throws IOException {
        daa2QuerySAMIterator.close();
    }

    private int countNewLines(byte[] bytes) {
        int count = 0;
        for (byte aByte : bytes) {
            if (aByte == '\n')
                count++;
        }
        return count;
    }


    @Override
    public void setParseLongReads(boolean longReads) {
        parseLongReads = longReads;
    }

    @Override
    public boolean isParseLongReads() {
        return parseLongReads;
    }
}
