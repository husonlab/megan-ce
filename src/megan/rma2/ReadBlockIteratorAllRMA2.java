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
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.data.TextStorageReader;
import megan.io.IInputReader;

import java.io.File;
import java.io.IOException;

/**
 * readblock getLetterCodeIterator over all reads, containing location information
 * Daniel Huson, 3.2012
 */
public class ReadBlockIteratorAllRMA2 implements IReadBlockIterator {
    private final TextStorageReader textStorageReader;
    private final IInputReader dataIndexReader;
    private final RMA2Formatter rma2Formatter;
    private final boolean wantReadText;
    private final boolean wantMatchData;
    private final boolean wantMatchText;
    private final float minScore;
    private final float maxExpected;

    private int countReads = 0;

    private boolean error = false;

    /**
     * constructor
     *
     * @param wantReadText
     * @param wantMatchData
     * @param wantMatchText
     * @param minScore
     * @param file
     * @throws java.io.IOException
     */
    public ReadBlockIteratorAllRMA2(boolean wantReadText, boolean wantMatchData, boolean wantMatchText, float minScore, float maxExpected, File file) throws IOException {
        this.wantReadText = wantReadText;
        this.wantMatchData = wantMatchData;
        this.wantMatchText = wantMatchText;
        this.minScore = minScore;
        this.maxExpected = maxExpected;

        RMA2File rma2File = new RMA2File(file);
        dataIndexReader = rma2File.getDataIndexReader();
        InfoSection infoSection = rma2File.loadInfoSection();
        rma2Formatter = infoSection.getRMA2Formatter();

        if (wantReadText || wantMatchText) {
            textStorageReader = new TextStorageReader(infoSection.getLocationManager(file));
        } else
            textStorageReader = null;
    }

    /**
     * get a string reporting stats
     *
     * @return stats string
     */
    public String getStats() {
        return "Reads: " + countReads;
    }

    /**
     * close associated file or database
     */
    public void close() {
        try {
            if (textStorageReader != null)
                textStorageReader.closeAllFiles();
            if (dataIndexReader != null)
                dataIndexReader.close();
        } catch (IOException e) {
            Basic.caught(e);
        }
    }

    /**
     * gets the maximum progress value
     *
     * @return maximum progress value
     */
    public long getMaximumProgress() {
        try {
            return dataIndexReader.length();
        } catch (IOException e) {
            Basic.caught(e);
            return -1;
        }
    }

    /**
     * gets the current progress value
     *
     * @return current progress value
     */
    public long getProgress() {
        try {
            return dataIndexReader.getPosition();
        } catch (IOException e) {
            Basic.caught(e);
            return -1;
        }
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the getLetterCodeIterator has more elements.
     */
    public boolean hasNext() {
        try {
            return !error && dataIndexReader.getPosition() < dataIndexReader.length();
        } catch (IOException e) {
            Basic.caught(e);
            return false;
        }
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     * @throws java.util.NoSuchElementException iteration has no more elements.
     */
    public IReadBlock next() {
        try {
            countReads++;
            return ReadBlockRMA2.read(rma2Formatter, -1, wantReadText, wantMatchData, wantMatchText, minScore, maxExpected, textStorageReader, dataIndexReader);
        } catch (IOException e) {
            Basic.caught(e);
            error = true;
            return null;
        }
    }

    /**
     * not implemented
     */
    public void remove() {
    }

    public void setWantLocationData(boolean wantLocationData) {
        rma2Formatter.setWantLocationData(wantLocationData);
    }
}
