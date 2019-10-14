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
import jloda.util.Pair;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.data.TextStorageReader;
import megan.io.IInputReader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * readblock getLetterCodeIterator over all reads in a given class
 * Daniel Huson, 9.2010
 */
public class ReadBlockIteratorRMA2 implements IReadBlockIterator {
    private final TextStorageReader textStorageReader;
    private final IInputReader dataIndexReader;
    private final IInputReader classDumpReader;
    private final RMA2Formatter rma2Formatter;
    private final boolean wantReadText;
    private final boolean wantMatchData;
    private final boolean wantMatchText;
    private final float minScore;
    private final float maxExpected;
    private boolean error = false;

    private final List<Pair<Integer, Long>> classes = new LinkedList<>();
    private Pair<Integer, Long> currentClass;
    private int currentCount = 0;

    private long totalReads;
    private long countReads = 0;

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
    public ReadBlockIteratorRMA2(String classification, Collection<Integer> classIds, boolean wantReadText, boolean wantMatchData, boolean wantMatchText, float minScore, float maxExpected, File file) throws IOException {
        this.wantReadText = wantReadText;
        this.wantMatchData = wantMatchData;
        this.wantMatchText = wantMatchText;
        this.minScore = minScore;
        this.maxExpected = maxExpected;

        RMA2File rma2File = new RMA2File(file);
        dataIndexReader = rma2File.getDataIndexReader();
        InfoSection infoSection = rma2File.loadInfoSection();
        rma2Formatter = infoSection.getRMA2Formatter();

        if (wantReadText || wantMatchText)
            textStorageReader = new TextStorageReader(infoSection.getLocationManager(file));
        else
            textStorageReader = null;

        Map<Integer, Pair<Integer, Long>> map = ClassificationBlockRMA2.getCountAndPos(rma2File, classification);
        for (Integer classId : classIds) {
            Pair<Integer, Long> pair = map.get(classId);
            if (pair != null && pair.getSecond() >= 0) {
                classes.add(pair);
                totalReads += pair.getFirst();
            }
        }

        // if(pair!=null)
        //     System.err.println("classId: "+classId+" size: "+pair.getFirst()+" dumpPos: "+pair.getSecond());

        if (totalReads > 0) {
            currentClass = classes.remove(0);
            currentCount = 0;
            classDumpReader = rma2File.getClassificationDumpReader(classification);
            classDumpReader.seek(currentClass.getSecond());
        } else
            classDumpReader = null;
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
            if (classDumpReader != null)
                classDumpReader.close();
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
        return totalReads;
    }

    /**
     * gets the current progress value
     *
     * @return current progress value
     */
    public long getProgress() {
        return countReads;
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the getLetterCodeIterator has more elements.
     */
    public boolean hasNext() {
        if (error || currentClass == null)
            return false;
        if (currentCount < currentClass.getFirst())
            return true;
        currentClass = null;
        while (classes.size() > 0) {
            currentCount = 0;
            Pair<Integer, Long> next = classes.remove(0);
            if (next != null && next.getFirst() > 0 && next.getSecond() >= 0) {
                try {
                    classDumpReader.seek(next.getSecond());
                } catch (IOException e) {
                    Basic.caught(e);
                    return false;
                }
                currentClass = next;
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     * @throws java.util.NoSuchElementException iteration has no more elements.
     */
    public IReadBlock next() {
        try {
            currentCount++;
            countReads++;
            return ReadBlockRMA2.read(rma2Formatter, classDumpReader.readLong(), wantReadText, wantMatchData, wantMatchText, minScore, maxExpected, textStorageReader, dataIndexReader);
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
}
