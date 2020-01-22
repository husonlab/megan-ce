/*
 * AllReadsIterator.java Copyright (C) 2020. Daniel H. Huson
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
package megan.data;

import jloda.util.Basic;

import java.io.IOException;

/**
 * Iterates over all reads in a file
 * Daniel Huson, 8.2015
 */
public class AllReadsIterator implements IReadBlockIterator {
    private final IReadBlockGetter readBlockGetter;
    private long countReads = 0;
    private final long totalCount;

    /**
     * constructor
     *
     * @param readBlockGetter
     */
    public AllReadsIterator(IReadBlockGetter readBlockGetter) throws IOException {
        this.readBlockGetter = readBlockGetter;
        totalCount = readBlockGetter.getCount();
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
        return totalCount;
    }

    @Override
    public long getProgress() {
        return countReads;
    }

    @Override
    public boolean hasNext() {
        return countReads < totalCount;
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
