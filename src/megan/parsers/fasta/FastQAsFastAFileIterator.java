/*
 *  Copyright (C) 2017 Daniel H. Huson
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
package megan.parsers.fasta;

import jloda.util.Basic;
import jloda.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * an getLetterCodeIterator over a multi-fastQ file   returning fastA records
 * Daniel Huson, 10.2011
 */
public class FastQAsFastAFileIterator implements IFastAIterator {
    private final BufferedReader r;
    private boolean isClosed = false;
    private long endOfLineBytes;
    private long nextHeaderPosition;

    private String nextLine;

    private final long fileLength; // todo: probably a bug to use this!
    private long currentHeaderPosition;

    private final long maxProgress;

    /**
     * constructor
     *
     * @param fileName
     * @throws java.io.IOException
     */
    public FastQAsFastAFileIterator(String fileName) throws IOException {
        File file = new File(fileName);
        fileLength = file.length();
        maxProgress = Basic.guessUncompressedSizeOfFile(fileName);
        r = new BufferedReader(new InputStreamReader(Basic.getInputStreamPossiblyZIPorGZIP(fileName)));
        endOfLineBytes = Basic.determineEndOfLinesBytes(file);
        nextLine = r.readLine();
    }


    /**
     * has next fastA record?
     *
     * @return true, if another fastA record available
     */
    public boolean hasNext() {
        return !isClosed && nextLine != null;
    }

    /**
     * gets next fastA record
     *
     * @return header and sequence
     */
    public Pair<String, String> next() {
        try {
            currentHeaderPosition = nextHeaderPosition;
            String readHeader = nextLine;
            nextHeaderPosition += readHeader.length() + endOfLineBytes;
            readHeader = ">" + readHeader.substring(1);
            String readSequence = r.readLine();
            nextHeaderPosition += readSequence.length() + endOfLineBytes;
            String comments = r.readLine();
            nextHeaderPosition += comments.length() + endOfLineBytes;
            if (comments.startsWith("+")) {
                String qualityValues = r.readLine();
                nextHeaderPosition += qualityValues.length() + endOfLineBytes;
            }
            nextLine = r.readLine();
            return new Pair<>(readHeader, readSequence);

        } catch (IOException e) {
            Basic.caught(e);
        }
        return null;
    }

    /**
     * gets the position of the current fastA record
     *
     * @return position
     */
    public long getPosition() {
        return currentHeaderPosition;
    }

    /**
     * gets the number of bytes associated with the current fastA record. This can be used to access the record directly
     *
     * @return size of current record in file
     */
    public long getNumberOfBytes() {
        if (nextHeaderPosition > currentHeaderPosition)
            return nextHeaderPosition - currentHeaderPosition;
        else
            return fileLength - currentHeaderPosition;
    }

    public void remove() {
    }

    public void close() throws IOException {
        if (!isClosed) {
            isClosed = true;
            r.close();
        }
    }

    /**
     * gets the maximum progress value
     *
     * @return maximum progress value
     */
    public long getMaximumProgress() {
        return maxProgress;
    }

    /**
     * gets the current progress value
     *
     * @return current progress value
     */
    public long getProgress() {
        return nextHeaderPosition;
    }
}
