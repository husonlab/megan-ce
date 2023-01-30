/*
 * ISAMIterator.java Copyright (C) 2023 Daniel H. Huson
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
package megan.parsers.blast;

import java.io.IOException;

/**
 * iterator for SAM format
 * Daniel Huson 4.2015
 */
public interface ISAMIterator {
    /**
     * gets the next matches
     *
     * @return number of matches
     */
    int next();

    /**
     * is there more data?
     *
     * @return true, if more data available
     */
    boolean hasNext();

    /**
     * gets the matches text
     *
     * @return matches text
     */
    byte[] getMatchesText();

    byte[] getQueryText();

    /**
     * length of matches text
     *
     * @return length of text
     */
    int getMatchesTextLength();

    long getMaximumProgress();

    long getProgress();

    void close() throws IOException;

    /**
     * are we parsing long reads?
     *
	 */
    void setParseLongReads(boolean longReads);

    boolean isParseLongReads();
}
