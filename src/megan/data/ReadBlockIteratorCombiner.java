/*
 * ReadBlockIteratorCombiner.java Copyright (C) 2023 Daniel H. Huson
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

import java.io.IOException;

/**
 * combines multiple read block iterators into one
 * Created by huson on 5/24/17.
 */
public class ReadBlockIteratorCombiner implements IReadBlockIterator {
    private final IReadBlockIterator[] iterators;
    private int which = 0;
    private long maxProgress = 0;
    private long prevProgress = 0;

    public ReadBlockIteratorCombiner(final IReadBlockIterator[] iterators) {
        this.iterators = iterators;

        for (IReadBlockIterator iterator : iterators) {
            maxProgress += iterator.getMaximumProgress();
        }
    }

    @Override
    public String getStats() {
        return iterators[which].getStats();
    }

    @Override
    public void close() throws IOException {
        for (IReadBlockIterator iterator : iterators)
            iterator.close();
    }

    @Override
    public long getMaximumProgress() {
        return maxProgress;
    }

    @Override
    public long getProgress() {
        return prevProgress + iterators[which].getProgress();
    }

    @Override
    public boolean hasNext() {
        while (which < iterators.length) {
            if (iterators[which].hasNext())
                return true;
            else {
                prevProgress += iterators[which].getMaximumProgress();
                which++;
            }

        }
        return false;
    }

    @Override
    public IReadBlock next() {
        while (which < iterators.length) {
            if (iterators[which].hasNext()) {
                IReadBlock readBlock = iterators[which].next();
                long bits = ((long) which << 54); // we use the top bits of the UID to track which dataset the read comes from
                readBlock.setUId(readBlock.getUId() | bits);
                return readBlock;
            } else {
                prevProgress += iterators[which].getMaximumProgress();
                which++;
            }
        }
        return null;
    }

    @Override
    public void remove() {

    }
}

