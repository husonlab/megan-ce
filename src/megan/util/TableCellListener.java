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
package megan.util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/*
 *  This class listens for changes made to the data in the table via the
 *  TableCellEditor. When editing is started, the value of the cell is saved
 *  When editing is stopped the new value is saved. When the oold and new
 *  values are different, then the provided Action is invoked.
 *
 *  The source of the Action is a TableCellListener instance.
 *  author: unknown
 */
public class TableCellListener implements PropertyChangeListener, Runnable {
    private final JTable table;
    private Action action;

    private int row;
    private int column;
    private Object oldValue;
    private Object newValue;

    /**
     * Create a TableCellListener.
     *
     * @param table  the table to be monitored for data changes
     * @param action the Action to invoke when cell data is changed
     */
    public TableCellListener(JTable table, Action action) {
        this.table = table;
        this.action = action;

        this.table.addPropertyChangeListener(this);
    }

    /**
     * Create a TableCellListener with a copy of all the data relevant to
     * the replace of data for a given cell.
     *
     * @param row      the row of the changed cell
     * @param column   the column of the changed cell
     * @param oldValue the old data of the changed cell
     * @param newValue the new data of the changed cell
     */
    private TableCellListener(JTable table, int row, int column, Object oldValue, Object newValue) {
        this.table = table;
        this.row = row;
        this.column = column;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Get the column that was last edited.
     *
     * @return the column that was edited
     */
    private int getColumn() {
        return column;
    }

    /**
     * Get the new value in the cell.
     *
     * @return the new value in the cell
     */
    private Object getNewValue() {
        return newValue;
    }

    /**
     * Get the old value of the cell.
     *
     * @return the old value of the cell
     */
    private Object getOldValue() {
        return oldValue;
    }

    /**
     * Get the row that was last edited.
     *
     * @return the row that was edited
     */
    private int getRow() {
        return row;
    }

    /**
     * Get the table of the cell that was changed.
     *
     * @return the table of the cell that was changed
     */
    private JTable getTable() {
        return table;
    }

    //
//  Implement the PropertyChangeListener interface
//
    public void propertyChange(PropertyChangeEvent e) {
        //  A cell has started/stopped editing

        if ("tableCellEditor".equals(e.getPropertyName())) {
            if (table.isEditing()) {
                processEditingStarted();
            } else {
                processEditingStopped();
            }
        }
    }

    /*
     *  Save information of the cell about to be edited
     */
    private void processEditingStarted() {
        //  The invokeLater is necessary because the editing row and editing
        //  column of the table have not been set when the "tableCellEditor"
        //  PropertyChangeEvent is fired.
        //  This results in the "run" method being invoked

        SwingUtilities.invokeLater(this);
    }

    /*
     *  See above.
     */
    public void run() {
        row = table.convertRowIndexToModel(table.getEditingRow());
        // row = table.getEditingRow();

        column = table.convertColumnIndexToModel(table.getEditingColumn());

        oldValue = table.getModel().getValueAt(row, column);
        newValue = null;
    }

    /*
     *	Update the Cell history when necessary
     */
    private void processEditingStopped() {
        if (row >= table.getRowCount() || column >= table.getColumnCount())
            return;

        newValue = table.getModel().getValueAt(row, column);

        //  The data has changed, invoke the supplied Action

        if (!newValue.equals(oldValue)) {
            //  Make a copy of the data in case another cell starts editing
            //  while processing this replace

            TableCellListener tcl = new TableCellListener(
                    getTable(), getRow(), getColumn(), getOldValue(), getNewValue());

            ActionEvent event = new ActionEvent(
                    tcl,
                    ActionEvent.ACTION_PERFORMED,
                    "");
            action.actionPerformed(event);
        }
    }
}
