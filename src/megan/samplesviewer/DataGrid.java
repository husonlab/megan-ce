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

package megan.samplesviewer;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import jloda.util.Basic;
import megan.core.SampleAttributeTable;
import org.controlsfx.control.spreadsheet.GridBase;
import org.controlsfx.control.spreadsheet.SpreadsheetCell;
import org.controlsfx.control.spreadsheet.SpreadsheetCellType;

import java.util.*;

/**
 * data grid
 */
public class DataGrid {
    private GridBase gridBase;
    private byte[] originalBytes = null;
    private final ArrayList<String> attributesOrder = new ArrayList<>(); // order of unhidden and hidden attributes
    private final ArrayList<String> samplesOrder = new ArrayList<>();

    private final ArrayList<String> secretAttributesOrder = new ArrayList<>(); // order of secret attributes

    private final Map<String, Map<String, Object>> hiddenAttribute2Data = new HashMap<>(); // such as color etc
    private final Map<String, Map<String, Object>> secretAttribute2Data = new HashMap<>(); // such as color etc

    /**
     * default constructor
     */
    public DataGrid() {
        gridBase = new GridBase(0, 0);
    }

    public void clear() {
        samplesOrder.clear();
        attributesOrder.clear();
        secretAttributesOrder.clear();
        secretAttribute2Data.clear();
        hiddenAttribute2Data.clear();
        gridBase.setRows(new LinkedList<ObservableList<SpreadsheetCell>>());
    }

    /**
     * reload data so that it is considered "clean"
     *
     * @param sampleAttributeTable
     */
    public void reload(SampleAttributeTable sampleAttributeTable) {
        originalBytes = null;
        load(sampleAttributeTable);

    }

    /**
     * load grid from sample attributes table
     *
     * @param sampleAttributeTable
     */
    public void load(SampleAttributeTable sampleAttributeTable) {
        if (originalBytes == null)
            originalBytes = sampleAttributeTable.getBytes();
        samplesOrder.clear();
        samplesOrder.addAll(sampleAttributeTable.getSampleOrder());

        attributesOrder.clear();
        hiddenAttribute2Data.clear();

        for (String attribute : sampleAttributeTable.getAttributeOrder()) {
            if (!sampleAttributeTable.isSecretAttribute(attribute)) {
                attributesOrder.add(attribute);

                if (sampleAttributeTable.isHiddenAttribute(attribute)) {
                    Map<String, Object> sample2value = new HashMap<>();
                    for (String sample : sampleAttributeTable.getSampleOrder()) {
                        sample2value.put(sample, sampleAttributeTable.get(sample, attribute));
                    }
                    hiddenAttribute2Data.put(attribute, sample2value);
                }
            }
        }

        for (String attribute : sampleAttributeTable.getAttributeOrder()) { // secret attributes always come last
            if (sampleAttributeTable.isSecretAttribute(attribute)) {
                secretAttributesOrder.add(attribute);
                Map<String, Object> sample2value = new HashMap<>();
                for (String sample : sampleAttributeTable.getSampleOrder()) {
                    sample2value.put(sample, sampleAttributeTable.get(sample, attribute));
                }
                secretAttribute2Data.put(attribute, sample2value);
            }
        }

        final List<ObservableList<SpreadsheetCell>> rows = new LinkedList<>();

        // row headers:
        {
            final ObservableList<String> rowHeaders = gridBase.getRowHeaders();
            rowHeaders.clear();
            rowHeaders.add("");

            for (int r = 1; r <= sampleAttributeTable.getNumberOfSamples(); r++)
                rowHeaders.add(String.format("%d", r));
        }
        // col headers:
        {
            final ObservableList<String> colHeaders = gridBase.getColumnHeaders();
            colHeaders.add("");

            int c = 1;
            for (String attribute : attributesOrder) {
                if (!sampleAttributeTable.isSecretAttribute(attribute)) // not secret property
                {
                    if (!sampleAttributeTable.isHiddenAttribute(attribute))
                        colHeaders.add(getAlpha(c));
                    c++;
                }
            }
        }

        // first row (contains all attribute names):
        {
            final ObservableList<SpreadsheetCell> row = FXCollections.observableArrayList();
            int c = 0;
            {
                SpreadsheetCell cell = SpreadsheetCellType.STRING.createCell(0, c++, 1, 1, SampleAttributeTable.SAMPLE_ID);
                row.add(cell);
            }
            for (String attribute : attributesOrder) {
                if (!sampleAttributeTable.isHiddenAttribute(attribute))// not hidden property
                {
                    SpreadsheetCell cell = SpreadsheetCellType.STRING.createCell(0, c++, 1, 1, attribute);
                    row.add(cell);
                }
            }
            rows.add(row);
        }

        // add all other rows:
        {
            int r = 1;
            for (String sample : sampleAttributeTable.getSampleOrder()) {
                final ObservableList<SpreadsheetCell> row = FXCollections.observableArrayList();
                SpreadsheetCell firstCell = SpreadsheetCellType.STRING.createCell(r, 0, 1, 1, sample);
                firstCell.setEditable(false);
                row.add(firstCell);

                int c = 1;
                for (String attribute : attributesOrder) {
                    if (!sampleAttributeTable.isHiddenAttribute(attribute))// not hidden property
                    {
                        Object label = sampleAttributeTable.get(sample, attribute);
                        SpreadsheetCell cell = SpreadsheetCellType.STRING.createCell(r, c++, 1, 1, label != null ? label.toString() : "");
                        row.add(cell);
                    }
                }
                rows.add(row);
                r++;
            }
        }
        gridBase.setRows(new LinkedList<ObservableList<SpreadsheetCell>>());
        gridBase.setRows(rows);
    }

    /**
     * save grid to sample attributesOrder table
     *
     * @param sampleAttributeTable
     * @param edit
     */
    public void save(SampleAttributeTable sampleAttributeTable, TableEdit edit) {
        sampleAttributeTable.clear();
        sampleAttributeTable.getSampleOrder().addAll(samplesOrder);


        if ((attributesOrder.size() - hiddenAttribute2Data.size()) == 0) { // all attributes are hidden!
            if (edit != null && edit.getOp() == Operation.Append && edit.getCol() == 0) {
                sampleAttributeTable.addAttribute(edit.getName(), "", true);
                for (String attribute : attributesOrder) {
                    if (sampleAttributeTable.isHiddenAttribute(attribute)) {
                        sampleAttributeTable.addAttribute(attribute, hiddenAttribute2Data.get(attribute), true, true);
                    }
                }
            }
            if (edit != null && edit.getOp() == Operation.Unhide) {
                sampleAttributeTable.addAttribute(edit.getName(), "", true);
                for (String attribute : attributesOrder) {
                    if (sampleAttributeTable.isHiddenAttribute(attribute)) { // hidden
                        final Map<String, Object> sample2value = hiddenAttribute2Data.get(attribute);
                        String originalName = attribute.replaceAll(" \\[hidden\\]$", "");
                        sampleAttributeTable.addAttribute(originalName, sample2value, true, true);
                    }
                }
            }
        } else { // some attributes visible
            int col = 1;
            for (String attribute : attributesOrder) {
                if (sampleAttributeTable.isHiddenAttribute(attribute)) { // hidden
                    final Map<String, Object> sample2value = hiddenAttribute2Data.get(attribute);
                    if (edit != null && edit.getOp() == Operation.Unhide) {
                        String originalName = attribute.replaceAll(" \\[hidden\\]$", "");
                        if (Arrays.asList(edit.getNames()).contains(originalName))
                            attribute = originalName;
                    }
                    sampleAttributeTable.addAttribute(attribute, sample2value, true, true);
                } else {
                    Map<String, Object> sample2value = new HashMap<>();
                    for (int row = 1; row < gridBase.getRowCount(); row++) {
                        String sample = getRowName(row);
                        Object item = gridBase.getRows().get(row).get(col).getItem();
                        sample2value.put(sample, item != null ? item.toString() : "");
                    }
                    if (edit != null && edit.getOp() == Operation.Hide && Arrays.asList(edit.getNames()).contains(attribute) && !sampleAttributeTable.isHiddenAttribute(attribute))
                        attribute = attribute + " [hidden]";
                    else if (edit != null && edit.getOp() == Operation.RenameAttribute && col == edit.getCol())
                        attribute = edit.getName();

                    sampleAttributeTable.addAttribute(attribute, sample2value, true, true);

                    if (edit != null && edit.getOp() == Operation.Append && col == edit.getCol()) {
                        sampleAttributeTable.addAttribute(edit.getName(), "", true);
                    }
                    col++;
                }
            }
            if (edit != null && edit.getOp() == Operation.Delete) {
                for (String attribute : edit.getNames()) {
                    sampleAttributeTable.removeAttribute(attribute);
                }
            }
        }

        if (edit != null && (edit.getOp() == Operation.MoveLeft || edit.getOp() == Operation.MoveRight)) {
            Set<String> toMove = new HashSet<>();
            toMove.addAll(Arrays.asList(edit.getNames()));
            String[] oldOrder = attributesOrder.toArray(new String[attributesOrder.size()]);
            String[] newOrder = new String[attributesOrder.size()];
            if (edit.getOp() == Operation.MoveLeft) {
                oldOrder = Basic.reverse(oldOrder);
            }
            if (!toMove.contains(oldOrder[oldOrder.length - 1])) {
                int oldPos = 0;
                int newPos = 0;
                while (oldPos < oldOrder.length) {
                    if (toMove.contains(oldOrder[oldPos])) {
                        int oldNext = oldPos;
                        while (oldNext < oldOrder.length && toMove.contains(oldOrder[oldNext])) {
                            oldNext++;
                        }
                        if (oldNext < oldOrder.length) {
                            newOrder[newPos++] = oldOrder[oldNext];
                        }
                        for (int i = oldPos; i < oldNext; i++) {
                            newOrder[newPos++] = oldOrder[i];
                        }
                        oldPos = oldNext + 1;
                    } else
                        newOrder[newPos++] = oldOrder[oldPos++];
                }
                if (newPos != oldPos)
                    System.err.println("Internal error: " + newPos + "!=" + oldPos);
                attributesOrder.clear();
                if (edit.getOp() == Operation.MoveLeft) {
                    newOrder = Basic.reverse(newOrder);
                }
                attributesOrder.addAll(Arrays.asList(newOrder));
                sampleAttributeTable.getAttributeOrder().clear();
                sampleAttributeTable.getAttributeOrder().addAll(attributesOrder);
            }
        }

        if (edit != null && (edit.getOp() == Operation.MoveSamplesDown || edit.getOp() == Operation.MoveSamplesUp)) {
            Set<String> toMove = new HashSet<>();
            toMove.addAll(Arrays.asList(edit.getNames()));
            String[] oldOrder = samplesOrder.toArray(new String[samplesOrder.size()]);
            String[] newOrder = new String[samplesOrder.size()];
            if (edit.getOp() == Operation.MoveSamplesUp) {
                oldOrder = Basic.reverse(oldOrder);
            }
            if (!toMove.contains(oldOrder[oldOrder.length - 1])) {
                int oldPos = 0;
                int newPos = 0;
                while (oldPos < oldOrder.length) {
                    if (toMove.contains(oldOrder[oldPos])) {
                        int oldNext = oldPos;
                        while (oldNext < oldOrder.length && toMove.contains(oldOrder[oldNext])) {
                            oldNext++;
                        }
                        if (oldNext < oldOrder.length) {
                            newOrder[newPos++] = oldOrder[oldNext];
                        }
                        for (int i = oldPos; i < oldNext; i++) {
                            newOrder[newPos++] = oldOrder[i];
                        }
                        oldPos = oldNext + 1;
                    } else
                        newOrder[newPos++] = oldOrder[oldPos++];
                }
                if (newPos != oldPos)
                    System.err.println("Internal error: " + newPos + "!=" + oldPos);
                samplesOrder.clear();
                if (edit.getOp() == Operation.MoveSamplesUp) {
                    newOrder = Basic.reverse(newOrder);
                }
                samplesOrder.addAll(Arrays.asList(newOrder));
                sampleAttributeTable.getSampleOrder().clear();
                sampleAttributeTable.getSampleOrder().addAll(samplesOrder);
            }
        }


        // save the secret attributes
        for (String attribute : secretAttributesOrder) {
            sampleAttributeTable.addAttribute(attribute, secretAttribute2Data.get(attribute), true, true);
        }
    }

    /**
     * gets column header value
     *
     * @param i
     * @return A, B, C etc
     */
    private static String getAlpha(int i) {

        String result = "";
        while (i > 0) {
            i--;
            int r = i % 26;
            char digit = (char) (r + 65);
            result = digit + result;
            i = (i - r) / 26;
        }
        return result;
    }

    /**
     * has the data changed?
     *
     * @param sampleAttributeTable
     * @return true, if changed since first opening
     */
    public boolean isChanged(SampleAttributeTable sampleAttributeTable) {
        if (originalBytes == null)
            return false;
        byte[] currentBytes = sampleAttributeTable.getBytes();

        if (originalBytes.length != currentBytes.length)
            return true;
        for (int i = 0; i < originalBytes.length; i++)
            if (originalBytes[i] != currentBytes[i])
                return true;
        return false;
    }

    public void resetGridBase() {
        gridBase.setRows(FXCollections.<ObservableList<SpreadsheetCell>>emptyObservableList());
    }

    public GridBase getGridBase() {
        return gridBase;
    }

    public int getRowCount() {
        return gridBase.getRowCount();
    }

    public int getColumnCount() {
        return gridBase.getColumnCount();
    }

    public ObservableList<ObservableList<SpreadsheetCell>> getRows() {
        return gridBase.getRows();
    }

    public String getColumnName(int c) {
        return getRows().get(0).get(c).getItem().toString();
    }

    public String getRowName(int r) {
        return getRows().get(r).get(0).getItem().toString();
    }

    /**
     * get the value of a cell
     *
     * @param row
     * @param col
     * @return value as string
     */
    public String getValue(int row, int col) {
        return gridBase.getRows().get(row).get(col).getItem().toString();
    }

    /**
     * get the value of a cell
     *
     * @param rowName
     * @param colName
     * @return value as string
     */
    public String getValue(String rowName, String colName) {
        int row = findRow(rowName);
        int col = findColumn(colName);
        if (row >= 0 && col >= 0)
            return gridBase.getRows().get(row).get(col).getItem().toString();
        else
            return null;
    }

    public ArrayList<String> getAttributesOrder() {
        return attributesOrder;
    }

    /**
     * get the named column
     *
     * @param name
     * @return column index or -1
     */
    public int findColumn(String name) {
        for (int col = 0; col < getColumnCount(); col++)
            if (getColumnName(col).equals(name))
                return col;
        return -1;
    }

    /**
     * get the named row
     *
     * @param name
     * @return column index or -1
     */
    public int findRow(String name) {
        for (int row = 0; row < getRowCount(); row++)
            if (getRowName(row).equals(name))
                return row;
        return -1;
    }

    /**
     * get the samples in their current order
     *
     * @return samples
     */
    public List<String> getSamplesOrder() {
        final List<String> list = new ArrayList<>(getRowCount() - 1);
        try {
            for (int row = 1; row < getRowCount(); row++)
                list.add(getRowName(row));
        } catch (Exception ex) {
            if (Platform.isFxApplicationThread())
                Basic.caught(ex);
        }
        return list;
    }

    /**
     * set a hidden attribute
     *
     * @param attribute
     * @param sample2value
     */
    public void setHiddenAttribute(String attribute, Map<String, Object> sample2value) {
        hiddenAttribute2Data.put(attribute, sample2value);
        if (attributesOrder.contains(attribute))
            attributesOrder.add(attribute);
    }

    // possible edit operations:
    public enum Operation {
        Append, Delete, Hide, Unhide, MoveLeft, MoveRight, MoveSamplesUp, RenameAttribute, MoveSamplesDown
    }

    /**
     * simple table edits to be performed on save
     */
    public static class TableEdit {
        private final Operation op;
        private final int col;
        private final String[] names;

        /**
         * constructor
         *
         * @param op
         * @param col
         */
        public TableEdit(Operation op, int col, String... name) {
            this.op = op;
            this.col = col;
            this.names = name;
        }

        public Operation getOp() {
            return op;
        }

        public int getCol() {
            return col;
        }

        public String getName() {
            return names[0];
        }

        public String[] getNames() {
            return names;
        }
    }

}

