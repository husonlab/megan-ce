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
package megan.rma2;

import jloda.util.Basic;
import jloda.util.Pair;
import megan.io.IInputReader;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * readblock getLetterCodeIterator over all reads in a given class
 * Daniel Huson, 9.2010
 */
public class ClassReadIdIteratorRMA2 implements Iterator<Pair<Integer, List<Long>>> {
    private final IInputReader classDumpReader;
    private final ClassificationBlockRMA2 classificationBlockRMA2;
    private final Iterator<Integer> iterator; // keys
    private final int numberOfClasses;
    private int classesProcessed = 0;

    private boolean error = false;

    /**
     * constructor
     *
     * @param file
     * @throws java.io.IOException
     */
    public ClassReadIdIteratorRMA2(String classification, File file) throws IOException {
        RMA2File rma2File = new RMA2File(file);

        classificationBlockRMA2 = new ClassificationBlockRMA2(classification);
        classificationBlockRMA2.load(rma2File.getClassificationIndexReader(classification));
        numberOfClasses = classificationBlockRMA2.getKeySet().size();
        iterator = classificationBlockRMA2.getKeySet().iterator();

        classDumpReader = rma2File.getClassificationDumpReader(classification);
    }

    /**
     * get a string reporting stats
     *
     * @return stats string
     */
    public String getStats() {
        return "Classes: " + numberOfClasses;
    }

    /**
     * close associated file or database
     */
    public void close() throws IOException {
        if (classDumpReader != null)
            classDumpReader.close();
    }

    /**
     * gets the maximum progress value
     *
     * @return maximum progress value
     */
    public int getMaximumProgress() {
        return numberOfClasses;
    }

    /**
     * gets the current progress value
     *
     * @return current progress value
     */
    public int getProgress() {
        return classesProcessed;
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the getLetterCodeIterator has more elements.
     */
    public boolean hasNext() {
        return !error && iterator.hasNext();
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     * @throws java.util.NoSuchElementException iteration has no more elements.
     */
    public Pair<Integer, List<Long>> next() {
        try {
            classesProcessed++;
            Integer key = iterator.next();
            long pos = classificationBlockRMA2.getPos(key);
            int count = classificationBlockRMA2.getSum(key);

            classDumpReader.seek(pos);
            List<Long> list = new LinkedList<>();
            for (int i = 0; i < count; i++)
                list.add(classDumpReader.readLong());

            return new Pair<>(key, list);
        } catch (Exception e) {
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
