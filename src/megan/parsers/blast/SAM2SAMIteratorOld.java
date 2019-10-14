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

import jloda.util.Basic;
import jloda.util.FileLineBytesIterator;
import megan.util.SAMFileFilter;

import java.io.IOException;
import java.util.Iterator;

/**
 * iterates over a file of alignments and returns them in SAM format
 * Daniel Huson, 4.2015
 *
 * @deprecated
 */
public class SAM2SAMIteratorOld implements ISAMIterator {
    private final FileLineBytesIterator samFileIterator;
    private final AlignedIterator alignedIterator; // iterators over all sam lines delivered by the samFileIterator that have an alignment
    private final int maxMatchesPerRead;

    private byte[] firstOfNext = new byte[0];
    private int lengthOfFirstOfNext;
    private byte[] result = new byte[100000];
    private int length = 0;
    private int matchesInResult = 0;

    private boolean parseLongReads;

    /**
     * constructor
     *
     * @param fileName
     * @param maxMatchesPerRead
     * @throws IOException
     */
    public SAM2SAMIteratorOld(String fileName, int maxMatchesPerRead) throws IOException {
        this.maxMatchesPerRead = maxMatchesPerRead;

        if (!SAMFileFilter.getInstance().accept(fileName))
            throw new IOException("File not in SAM format (or incorrect file suffix?): " + fileName);
        samFileIterator = new FileLineBytesIterator(fileName);
        // skip header lines:
        while (samFileIterator.hasNext() && samFileIterator.peekNextByte() == '@')
            samFileIterator.next();
        alignedIterator = new AlignedIterator(samFileIterator);
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
        return lengthOfFirstOfNext > 0 || alignedIterator.hasNext();
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
        return samFileIterator.getMaximumProgress();
    }

    @Override
    public long getProgress() {
        return samFileIterator.getProgress();
    }

    @Override
    public void close() throws IOException {
        samFileIterator.close();
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

        while (alignedIterator.hasNext()) {
            byte[] line = alignedIterator.next();
            int lineLength = alignedIterator.getLineLength();
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
     * Two lines belong to the same query if the first word is the same (the query name) and the second word is an integer flag that same 7&8 bit.
     * This indicate that the sequences are the same either first or second template, i.e. mate pair
     *
     * @param a
     * @param b
     * @return true, if identical up to first tab
     */
    private boolean sameQuery(byte[] a, byte[] b) {
        int top = Math.min(a.length, b.length);
        int pos = 0;
        while (pos < top) {
            if (a[pos] != b[pos]) {
                return false;
            }
            if (a[pos] == '\t')
                break;
            pos++;
        }
        if (pos >= a.length || pos >= b.length || b[pos] != '\t')
            return false;
        pos++;
        int posA = pos;
        while (posA < a.length && !Character.isWhitespace(a[posA]))
            posA++;
        if (posA == pos)
            return false;
        int posB = pos;
        while (posB < b.length && !Character.isWhitespace(b[posB]))
            posB++;
        if (posB == pos)
            return false;
        final int flagA = Basic.parseInt(new String(a, pos, posA - pos));
        final int flagB = Basic.parseInt(new String(b, pos, posB - pos));
        return (flagA & 192) == (flagB & 192); // have same 7th and 8th bit
    }

    @Override
    public void setParseLongReads(boolean longReads) {
        this.parseLongReads = longReads;
    }

    @Override
    public boolean isParseLongReads() {
        return parseLongReads;
    }

    /**
     * iterator that only return SAM lines for which an alignment is present
     */
    static class AlignedIterator implements Iterator<byte[]> {
        final private FileLineBytesIterator iterator;
        private byte[] next = null;
        private int length = 0;

        AlignedIterator(final FileLineBytesIterator iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            } else {
                while (iterator.hasNext()) {
                    byte[] line = iterator.next();
                    length = iterator.getLineLength();

                    final String secondToken = Basic.getTokenFromTabSeparatedLine(line, 1);
                    if (Basic.isInteger(secondToken)) {
                        final int flags = Basic.parseInt(secondToken);
                        if ((flags & 4) != 0) {
                            continue; // this is an unmapped read
                        }
                    }
                    next = line;
                    return true;
                }
                return false;
            }
        }

        @Override
        public byte[] next() {
            if (hasNext()) {
                byte[] result = next;
                next = null;
                return result;
            } else
                return null;
        }

        @Override
        public void remove() {

        }

        int getLineLength() {
            return length;
        }
    }
}
