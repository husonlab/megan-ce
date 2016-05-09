/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import jloda.util.Basic;
import jloda.util.FileIterator;
import megan.io.Compressor;
import megan.util.SAMFileFilter;

import java.io.IOException;

/**
 * iterates over a file of alignments and returns them in SAM format
 * Daniel Huson, 4.2015
 */
public class SAM2SAMIterator implements ISAMIterator {
    private final FileIterator samIterator;
    private final int maxMatchesPerRead;

    private byte[] firstOfNext = new byte[0];
    private int lengthOfFirstOfNext;
    private byte[] result = new byte[100000];
    private int length = 0;
    private int matchesInResult = 0;

    /**
     * constructor
     *
     * @param fileName
     * @param maxMatchesPerRead
     * @throws IOException
     */
    public SAM2SAMIterator(String fileName, int maxMatchesPerRead) throws IOException {
        this.maxMatchesPerRead = maxMatchesPerRead;

        if (!SAMFileFilter.getInstance().accept(fileName))
            throw new IOException("File not in SAM format: " + fileName);
        samIterator = new FileIterator(fileName);
        // skip header lines:
        while (samIterator.hasNext() && samIterator.peekNextByte() == '@')
            samIterator.next();
    }

    /**
     * returns the next number of matches found
     *
     * @return number of next matches found
     */
    @Override
    public int next() {
        moveToNext();
        return matchesInResult;
    }

    /**
     * is there next?
     *
     * @return next
     */
    @Override
    public boolean hasNext() {
        return lengthOfFirstOfNext > 0 || samIterator.hasNext();
    }

    /**
     * get the matches found as single text
     *
     * @return text
     */
    @Override
    public byte[] getMatchesText() {
        return result;
    }

    /**
     * gets the length of the matches string
     *
     * @return length
     */
    @Override
    public int getMatchesTextLength() {
        return length;
    }

    @Override
    public long getMaximumProgress() {
        return samIterator.getMaximumProgress();
    }

    @Override
    public long getProgress() {
        return samIterator.getProgress();
    }

    @Override
    public void close() throws IOException {
        samIterator.close();
    }

    @Override
    public byte[] getQueryText() {
        return null;
    }

    private void moveToNext() {
        matchesInResult = 0;

        if (lengthOfFirstOfNext > 0) {
            if (result.length < lengthOfFirstOfNext)
                result = new byte[2 * lengthOfFirstOfNext];
            System.arraycopy(firstOfNext, 0, result, 0, lengthOfFirstOfNext);
            length = lengthOfFirstOfNext;
            matchesInResult++;
            lengthOfFirstOfNext = 0;
        } else
            length = 0;

        while (samIterator.hasNext()) {
            byte[] line = samIterator.next();
            int lineLength = samIterator.getLineLength();
            if (length == 0 || sameQuery(result, line)) {
                if (matchesInResult < maxMatchesPerRead) {
                    matchesInResult++;
                    if (result.length < length + lineLength + 1) {
                        byte[] tmp = new byte[2 * (length + lineLength)];
                        System.arraycopy(result, 0, tmp, 0, length);
                        result[length++] = '\n';
                        result = tmp;
                    }
                    System.arraycopy(line, 0, result, length, lineLength);
                    length += lineLength;
                }
            } else {
                if (firstOfNext.length < lineLength + 1) {
                    firstOfNext = new byte[lineLength + 1];
                }
                System.arraycopy(line, 0, firstOfNext, 0, lineLength);
                lengthOfFirstOfNext = lineLength;
                //firstOfNext[lengthOfFirstOfNext++]='\n';
                break;
            }
        }
    }

    /**
     * are the two strings identical up until the first tab
     *
     * @param a
     * @param b
     * @return true, if identical up to first tab
     */
    private boolean sameQuery(byte[] a, byte[] b) {
        int top = Math.min(a.length, b.length);
        for (int i = 0; i < top; i++) {
            if (a[i] != b[i])
                return false;
            if (a[i] == '\t')
                return true;
        }
        return a.length == b.length;
    }

    public static void main(String[] args) throws IOException {
        String file = "/Users/huson/data/diamond/debugging/YhgE/yhge.sam";

        Compressor compressor = new Compressor();

        int count = 0;
        ISAMIterator it = new SAM2SAMIterator(file, 20);
        while (it.hasNext()) {
            System.err.println("Got:    " + it.next());
            System.err.println("length: " + it.getMatchesTextLength());
            System.err.println("comp. : " + compressor.deflateString2ByteArray(Basic.toString(it.getMatchesText(), 0, it.getMatchesTextLength())).length);
            System.err.println(Basic.toString(it.getMatchesText(), 0, it.getMatchesTextLength()));
            if (count++ > 20)
                break;
        }
        it.close();
    }
}
