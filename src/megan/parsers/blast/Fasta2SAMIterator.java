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
package megan.parsers.blast;

import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.FastaFileFilter;
import jloda.util.Basic;

import java.io.IOException;


/**
 * parses a FastA file into SAM format. This is used to partition a database such as NR by taxonomic or other assignment
 * Daniel Huson, 4.2015
 */
public class Fasta2SAMIterator extends SAMIteratorBase implements ISAMIterator {
    private byte[] matchesText = new byte[0];

    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     */
    public Fasta2SAMIterator(String fileName, int maxNumberOfMatchesPerRead) throws IOException {
        super(fileName, maxNumberOfMatchesPerRead);
        if (!FastaFileFilter.accept(fileName, true)) {
            NotificationsInSwing.showWarning("Might not be FastA format: " + fileName);
        }
    }

    /**
     * is there more data?
     *
     * @return true, if more data available
     */
    @Override
    public boolean hasNext() {
        return hasNextLine();
    }

    /**
     * gets the next matches
     *
     * @return number of matches
     */
    public int next() {
        if (!hasNextLine())
            return -1;

        String line = nextLine();

        if (line == null || !line.startsWith(">"))
            return -1;

        final String queryName = Basic.getReadName(line);

        matchesText = makeSAM(queryName, Basic.replaceSpaces(line, ' ')).getBytes();

        while (hasNextLine()) {
            line = nextLine();
            if (line.startsWith(">")) {
                pushBackLine(line);
                break;
            }
        }
        return 1;
    }

    /**
     * gets the matches text
     *
     * @return matches text
     */
    @Override
    public byte[] getMatchesText() {
        return matchesText;
    }

    /**
     * length of matches text
     *
     * @return length of text
     */
    @Override
    public int getMatchesTextLength() {
        return matchesText.length;
    }

    /**
     * make a SAM line
     */
    private String makeSAM(String queryName, String referenceLine) {

        return queryName + "\t0\t" + referenceLine + "\t0\t255\t*\t*\t0\t0\t*\t*\tAS:i:100\t\n";
    }
}
