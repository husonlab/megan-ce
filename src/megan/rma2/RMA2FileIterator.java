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

package megan.rma2;

import jloda.util.Basic;
import jloda.util.IFileIterator;
import megan.data.IReadBlock;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

/**
 * iterates over all the matches in an RMA file, line by line
 */
public class RMA2FileIterator implements IFileIterator {
    private final ReadBlockIteratorAllRMA2 iterator;
    private String[] lines = new String[100000];
    private int pos = 0;
    private int top = 0;

    /**
     * constructor
     *
     * @param rma2File
     * @throws IOException
     */
    public RMA2FileIterator(String rma2File) throws IOException {
        iterator = new ReadBlockIteratorAllRMA2(true, true, true, 0, 1000, new File(rma2File));
    }

    /**
     * gets the current line number
     *
     * @return line number
     */
    @Override
    public long getLineNumber() {
        return 0;
    }

    /**
     * close associated file or database
     */
    @Override
    public void close() throws IOException {
        iterator.close();

    }

    /**
     * gets the maximum progress value
     *
     * @return maximum progress value
     */
    @Override
    public long getMaximumProgress() {
        return iterator.getMaximumProgress();
    }

    /**
     * gets the current progress value
     *
     * @return current progress value
     */
    @Override
    public long getProgress() {
        return iterator.getProgress();
    }

    /**
     * does next element exist?
     *
     * @return
     */
    public boolean hasNext() {
        return iterator.hasNext() || pos < top;
    }

    public String next() {
        if (pos == top) {
            pos = top = 0;
            IReadBlock block = iterator.next();
            StringBuilder buf = new StringBuilder();
            buf.append("Query= ").append(block.getReadName()).append("\n\n");
            for (int i = 0; i < block.getNumberOfAvailableMatchBlocks(); i++) {
                buf.append("\n").append(block.getMatchBlock(i).getText());
            }
            BufferedReader r = new BufferedReader(new StringReader(buf.toString()));
            String aLine;
            try {
                while ((aLine = r.readLine()) != null) {
                    if (top == lines.length) {
                        String[] tmp = new String[2 * lines.length];
                        System.arraycopy(lines, 0, tmp, 0, lines.length);
                        lines = tmp;
                    }
                    lines[top++] = aLine;
                }
            } catch (IOException e) {
                Basic.caught(e);
            }
        }
        return lines[pos++];
    }

    public void remove() {
    }

    public static void main(String[] args) throws IOException {
        String file = "/Users/huson/data/megan/ecoli/ecoli-testrun-2000-nr.rma";

        try (final RMA2FileIterator iterator = new RMA2FileIterator(file)) {
            while (iterator.hasNext()) {
                System.err.println(iterator.next());
            }
        }
    }
}
