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
package megan.data;

import jloda.util.Basic;
import jloda.util.Single;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * find all reads
 * Daniel Huson, 8.2015
 */
public class FindAllReadsIterator implements IReadBlockIterator {

    private final Pattern pattern;
    private final FindSelection findSelector;
    private final Single<Boolean> canceled;

    private final IReadBlockIterator allReadsIterator;

    private IReadBlock nextRead = null;
    private int countReads = 0;

    /**
     * constructor
     *
     * @param allReadsIterator
     * @param canceled
     */
    public FindAllReadsIterator(String regularExpression, FindSelection findSelector, IReadBlockIterator allReadsIterator, Single<Boolean> canceled) throws IOException {
        this.findSelector = findSelector;
        this.canceled = canceled;
        pattern = Pattern.compile(regularExpression);
        this.allReadsIterator = allReadsIterator;

        nextRead = fetchNext();
    }

    @Override
    public String getStats() {
        return "Reads: " + countReads;
    }

    @Override
    public void close() {
        try {
            allReadsIterator.close();
        } catch (IOException e) {
            Basic.caught(e);
        }
    }

    @Override
    public long getMaximumProgress() {
        return allReadsIterator.getMaximumProgress();
    }

    @Override
    public long getProgress() {
        return allReadsIterator.getProgress();
    }

    @Override
    public boolean hasNext() {
        return !canceled.get() && nextRead != null;
    }

    @Override
    public IReadBlock next() {
        final IReadBlock result = nextRead;
        if (result != null) {
            countReads++;
            nextRead = fetchNext();
        }
        return result;
    }

    @Override
    public void remove() {

    }

    /**
     * fetches the next read that matches the search pattern
     *
     * @return next match
     */
    private IReadBlock fetchNext() {
        while (!canceled.get() && allReadsIterator.hasNext()) {
            IReadBlock readBlock = allReadsIterator.next();
            if (FindSelection.doesMatch(findSelector, readBlock, pattern))
                return readBlock;
        }
        return null;
    }
}
