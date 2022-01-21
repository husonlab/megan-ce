/*
 * ReadBlockIterator.java Copyright (C) 2022 Daniel H. Huson
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
package megan.data;

import jloda.util.Basic;

import java.io.IOException;
import java.util.Iterator;

/**
 * iterator over reads in named classes
 * <p/>
 * Daniel Huson, 4.2015
 */
public class ReadBlockIterator implements IReadBlockIterator {
    private final IReadBlockGetter readBlockGetter;
    private final Iterator<Long> iterator;
    private final long numberOfReads;

    private int countReads = 0;

    /**
     * constructor
     *
     * @param readBlockGetter
     */
    public ReadBlockIterator(Iterator<Long> readIdsIterator, long numberOfReads, IReadBlockGetter readBlockGetter) {
        this.iterator = readIdsIterator;
        this.numberOfReads = numberOfReads;
        this.readBlockGetter = readBlockGetter;
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
        return numberOfReads;
    }

    @Override
    public long getProgress() {
        return countReads;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public IReadBlock next() {
        countReads++;
        try {
            return readBlockGetter.getReadBlock(iterator.next());
        } catch (IOException e) {
            Basic.caught(e);
            return null;
        }
    }

    @Override
    public void remove() {
    }
}
