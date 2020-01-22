/*
 * ReadBlockIteratorMaxCount.java Copyright (C) 2020. Daniel H. Huson
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

import java.io.IOException;

/**
 * Iterator over a set of read blocks, upto a given max count
 * Daniel Huson, 6.2016
 */
public class ReadBlockIteratorMaxCount implements IReadBlockIterator {
    private final IReadBlockIterator it;
    private final int maxCount;
    private int count;

    public ReadBlockIteratorMaxCount(IReadBlockIterator it, int maxCount) {
        this.it = it;
        this.maxCount = maxCount;
        count = 0;
    }

    @Override
    public String getStats() {
        return it.getStats();
    }

    @Override
    public void close() throws IOException {
        it.close();
    }

    @Override
    public long getMaximumProgress() {
        return maxCount;
    }

    @Override
    public long getProgress() {
        return count;
    }

    @Override
    public boolean hasNext() {
        return count < maxCount && it.hasNext();
    }

    @Override
    public IReadBlock next() {
        count++;
        return it.next();
    }
}
