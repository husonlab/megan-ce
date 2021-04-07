/*
 * RowCompressor.java Copyright (C) 2021. Daniel H. Huson
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
 *
 */
package megan.alignment.gui;

import jloda.util.Pair;

import java.util.*;

/**
 * row compressor is used to fit multiple reads into a single row. Is used for mapping visualization of reads, rather than alignment view
 * Daniel Huson, 1.2011
 */
public class RowCompressor {
    private final Alignment alignment;
    private final Vector<List<Integer>> compressedRow2Reads = new Vector<>();
    private boolean enabled = false;

    /**
     * constructor
     *
     * @param alignment
     */
    public RowCompressor(Alignment alignment) {
        this.alignment = alignment;
    }

    /**
     * updates the row to compress row mapping
     */
    public void update() {
        enabled = true;
        compressedRow2Reads.clear();
        SortedSet<Pair<Integer, Integer>> sortedReads = new TreeSet<>();

        int numberOfCompressedRows = 0;
        int[] ends = new int[alignment.getNumberOfSequences()];

        // sort reads by start position
        for (int read = 0; read < alignment.getNumberOfSequences(); read++) {
            int start = alignment.getLane(read).getFirstNonGapPosition();
            sortedReads.add(new Pair<>(start, read));
        }

        // go through reads by start position and place in compressed rows
        for (Pair<Integer, Integer> pair : sortedReads) {
            int read = pair.getSecond();
            int start = alignment.getLane(read).getFirstNonGapPosition();
            boolean done = false;
            for (int row = 0; !done && row < numberOfCompressedRows; row++) {
                if (start > ends[row] + 10) {
                    compressedRow2Reads.get(row).add(read);
                    ends[row] = alignment.getLane(read).getLastNonGapPosition();
                    done = true;
                }
            }
            if (!done) {
                ends[numberOfCompressedRows] = alignment.getLane(read).getLastNonGapPosition();
                List<Integer> reads = new LinkedList<>();
                reads.add(read);
                compressedRow2Reads.add(numberOfCompressedRows, reads);
                numberOfCompressedRows++;
            }
        }
    }

    /**
     * erase
     */
    public void clear() {
        compressedRow2Reads.clear();
        enabled = false;
    }

    /**
     * gets the number of compressed rows
     *
     * @return number of compressed rows
     */
    public int getNumberRows() {
        if (enabled)
            return compressedRow2Reads.size();
        else
            return alignment.getNumberOfSequences();
    }

    /**
     * maps a row to all its reads
     *
     * @param compressedRow
     * @return reads
     */
    public List<Integer> getCompressedRow2Reads(int compressedRow) {
        if (enabled && compressedRow < compressedRow2Reads.size())
            return compressedRow2Reads.get(compressedRow);
        else
            return Collections.singletonList(compressedRow);
    }

    /**
     * gets the original row of the alignment. Is simply the row, in the alignment view, else a read
     *
     * @param row
     * @param col
     * @return row if alignment, else read
     */
    public int getRead(int row, int col) {
        if (enabled && row < compressedRow2Reads.size()) {
            for (int read : compressedRow2Reads.get(row)) {
                Lane lane = alignment.getLane(read);
                if (lane.getFirstNonGapPosition() <= col && lane.getLastNonGapPosition() >= col)
                    return read;
            }
            return -1;
        } else {
            Lane lane = alignment.getLane(row);
            if (lane != null && lane.getFirstNonGapPosition() <= col && lane.getLastNonGapPosition() >= col)
                return row;
        }
        return -1;
    }

    /**
     * gets the row for a given read
     *
     * @param readId
     * @return row
     */
    public int getRow(int readId) {
        if (enabled) {
            for (int i = 0; i < compressedRow2Reads.size(); i++) {
                List<Integer> reads = compressedRow2Reads.elementAt(i);
                if (reads.contains(readId))
                    return i;
            }
            return -1;
        } else
            return readId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * move selected rows up
     *
     * @param firstRow
     * @param lastRow
     * @return true, if moved up
     */
    public boolean moveUp(int firstRow, int lastRow) {
        lastRow = Math.min(lastRow, getNumberRows());
        if (firstRow <= 0 || firstRow > lastRow)
            return false;
        else {
            List<Integer>[] array = new List[getNumberRows()];
            for (int i = 0; i < getNumberRows(); i++)
                array[i] = getCompressedRow2Reads(i);
            List<Integer> replaced = array[firstRow - 1];
            System.arraycopy(array, firstRow, array, firstRow - 1, lastRow + 1 - firstRow);
            array[lastRow] = replaced;
            compressedRow2Reads.clear();
            compressedRow2Reads.addAll(Arrays.asList(array));
            return true;
        }

    }

    /**
     * move the selected rows of sequences down one
     *
     * @param firstRow
     * @param lastRow
     * @return true, if moved
     */
    public boolean moveDown(int firstRow, int lastRow) {
        firstRow = Math.max(0, firstRow);
        if (lastRow >= getNumberRows() - 1)
            return false;
        else {
            List<Integer>[] array = new List[getNumberRows()];
            for (int i = 0; i < getNumberRows(); i++)
                array[i] = getCompressedRow2Reads(i);
            List<Integer> replaced = array[lastRow + 1];

            System.arraycopy(array, firstRow, array, firstRow + 1, lastRow + 1 - firstRow);
            array[firstRow] = replaced;
            compressedRow2Reads.clear();
            compressedRow2Reads.addAll(Arrays.asList(array));
            return true;
        }
    }
}
