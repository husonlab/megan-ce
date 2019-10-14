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
package megan.alignment.gui;

import java.util.LinkedList;

/**
 * selected block in alignment
 * Daniel Huson, 9.2011
 */
public class SelectedBlock {
    private int firstRow;
    private int lastRow;
    private int firstCol;
    private int lastCol;

    private int totalRows = 0;
    private int totalCols = 0;

    private final LinkedList<ISelectionListener> selectionListeners = new LinkedList<>();

    public SelectedBlock() {
        firstRow = 0;
        lastRow = -1;
        firstCol = 0;
        lastCol = -1;
    }

    public int getNumberOfSelectedRows() {
        return lastRow - firstRow + 1;
    }

    public int getNumberOfSelectedCols() {
        return lastCol - firstCol + 1;
    }

    public void selectAll() {
        firstRow = 0;
        lastRow = totalRows - 1;
        firstCol = 0;
        lastCol = totalCols - 1;
    }

    public void clear() {
        firstRow = 0;
        lastRow = -1;
        firstCol = 0;
        lastCol = -1;
        fireSelectionChanged();
    }

    public void selectRow(int row) {
        firstRow = row;
        lastRow = row;
        firstCol = 0;
        lastCol = totalCols - 1;
        fireSelectionChanged();
    }

    public void selectRows(int firstRow, int lastRow) {
        this.firstRow = firstRow;
        this.lastRow = lastRow;
        firstCol = 0;
        lastCol = totalCols - 1;
        fireSelectionChanged();
    }

    public void selectCol(int col, boolean wholeCodon) {
        firstRow = 0;
        lastRow = totalRows - 1;
        if (wholeCodon) {
            firstCol = Math.max(0, col - (col % 3));
            lastCol = firstCol + 2;
        } else {
            firstCol = col;
            lastCol = col;
        }
        fireSelectionChanged();
    }

    public void selectCols(int firstCol, int lastCol, boolean wholeCodon) {
        firstRow = 0;
        lastRow = totalRows - 1;
        if (wholeCodon) {
            this.firstCol = Math.max(0, firstCol - (firstCol % 3));
            this.lastCol = Math.min(getTotalCols() - 1, lastCol + 2 - (lastCol % 3));
        } else {
            this.firstCol = firstCol;
            this.lastCol = lastCol;
        }
        fireSelectionChanged();
    }

    public void select(int firstRow, int firstCol, int lastRow, int lastCol, boolean wholeCodon) {
        this.firstRow = Math.max(0, firstRow);
        this.lastRow = Math.min(lastRow, getTotalRows() - 1);
        if (wholeCodon) {
            this.firstCol = Math.max(0, firstCol - (firstCol % 3));
            this.lastCol = Math.min(getTotalCols() - 1, lastCol + 2 - (lastCol % 3));
        } else {
            this.firstCol = Math.max(0, firstCol);
            this.lastCol = Math.min(lastCol, getTotalCols() - 1);
        }
        fireSelectionChanged();

        // System.err.println(this);
    }

    public String toString() {
        return "Selection: rows=" + firstRow + " - " + lastRow + ", cols=" + firstCol + " - " + lastCol;
    }

    /**
     * extend a selection
     *
     * @param toRow (-1 indicate extend cols only)
     * @param toCol (-1 indicates extend rows only)
     */
    public void extendSelection(int toRow, int toCol) {
        if (toRow != -1) {
            if (toRow > totalRows)
                toRow = totalRows;
            if (toRow < firstRow)
                firstRow = toRow;
            else if (toRow >= lastRow)
                lastRow = toRow;
        }
        if (toCol != -1) {
            if (toCol > totalCols)
                toCol = totalCols;
            if (toCol < firstCol)
                firstCol = toCol;
            else if (toCol >= lastCol)
                lastCol = toCol;
        }
        fireSelectionChanged();
    }

    /**
     * reduce a selection
     *
     * @param toRow (-1 indicates reduce columns only)
     * @param toCol (-1 indicates reduces rows only)
     */
    public void reduceSelection(int toRow, int toCol) {
        if (toRow > totalRows)
            toRow = totalRows;
        if (toCol > totalCols)
            toCol = totalCols;

        boolean firstRowBest = false;
        if (Math.abs(toRow - firstRow) < Math.abs(toRow - lastRow)) {
            firstRowBest = true;
        }
        int bestRowScore = Math.min(Math.abs(toRow - firstRow), Math.abs(toRow - lastRow));

        boolean firstColBest = false;
        if (Math.abs(toCol - firstCol) < Math.abs(toCol - lastCol)) {
            firstColBest = true;
        }
        int bestColScore = Math.min(Math.abs(toCol - firstCol), Math.abs(toCol - lastCol));

        if (toRow != -1 && (toCol == -1 || bestRowScore < bestColScore)) {
            if (firstRowBest)
                firstRow = toRow;
            else
                lastRow = toRow;
            fireSelectionChanged();
        } else if (toCol != -1) {
            if (firstColBest)
                firstCol = toCol;
            else
                lastCol = toCol;
            fireSelectionChanged();
        }
    }

    public boolean isSelected(int row, int col) {
        return row >= firstRow && row <= lastRow && col >= firstCol && col <= lastCol;
    }

    public int getFirstRow() {
        return firstRow;
    }

    public void setFirstRow(int firstRow) {
        this.firstRow = firstRow;
    }

    public int getLastRow() {
        return lastRow;
    }

    public void setLastRow(int lastRow) {
        this.lastRow = lastRow;
    }

    public int getFirstCol() {
        return firstCol;
    }

    public void setFirstCol(int firstCol) {
        this.firstCol = firstCol;
    }

    public int getLastCol() {
        return lastCol;
    }

    public void setLastCol(int lastCol) {
        this.lastCol = lastCol;
    }

    public boolean isSelected() {
        return firstRow <= lastRow && firstCol <= lastCol;
    }

    public boolean contains(int row, int col) {
        return row >= firstRow && row <= lastRow && col >= firstCol && col <= lastCol;
    }

    private int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    private int getTotalCols() {
        return totalCols;
    }

    public void setTotalCols(int totalCols) {
        this.totalCols = totalCols;
    }

    public void addSelectionListener(ISelectionListener selectionListener) {
        selectionListeners.add(selectionListener);
    }

    public void removeSelectionListener(ISelectionListener selectionListener) {
        selectionListeners.remove(selectionListener);
    }

    public void fireSelectionChanged() {
        for (ISelectionListener selectionListener : selectionListeners) {
            selectionListener.doSelectionChanged(isSelected(), firstRow, firstCol, lastRow, lastCol);
        }
    }

    public boolean isSelectedCol(int col) {
        return col >= firstCol && col <= lastCol;
    }

    public boolean isSelectedRow(int row) {
        return row >= firstRow && row <= lastRow;
    }
}
