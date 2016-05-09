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

package megan.samplesviewer;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import jloda.gui.director.IDirector;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgressSilent;
import jloda.util.ResourceManager;
import megan.core.Document;
import megan.fx.NotificationsInSwing;
import megan.fx.PopupMenuFX;
import megan.fx.Utilities;
import megan.util.GraphicsUtilities;
import org.controlsfx.control.spreadsheet.*;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * Samples metadata spreadsheet
 * Daniel Huson, 9.2015
 */
public class SamplesSpreadSheet {
    private static DataFormat fmt = new DataFormat("SpreadsheetView");
    private final SamplesViewer samplesViewer;
    private JFXPanel jFXPanel;
    private SpreadsheetView spreadsheetView;
    private ContextMenu columnContextMenu;
    private ContextMenu rowContextMenu;
    private int originalRowContextMenuLength;

    private int numberOfSelectedRows = 0;
    private int numberOfSelectedCols = 0;
    private int numberOfSelectedColsIncludingSamplesCol = 0;

    private boolean isEditable = true;

    private boolean initialized = false;

    private DataGrid dataGrid;

    private final Object lock = new Object();

    /**
     * constructor
     */
    public SamplesSpreadSheet(SamplesViewer samplesViewer) {
        this.samplesViewer = samplesViewer;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                initSwingLater();
            }
        });
    }

    /**
     * get the panel
     *
     * @return panel
     */
    public JFXPanel getPanel() {
        for (int count = 0; count < 100; count++) {
            if (initialized)
                break;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Basic.caught(e);
            }
        }
        return jFXPanel;
    }

    /**
     * sync spreadsheet from document
     */
    public void syncFromDocument() {
        Platform.runLater(new Runnable() {
            public void run() {
                dataGrid.resetGridBase();
                dataGrid.load(samplesViewer.getSampleAttributeTable());
                spreadsheetView.setGrid(dataGrid.getGridBase());
                updateView();
                for (SpreadsheetColumn column : getSpreadsheetView().getColumns())
                    column.fitColumn();
            }
        });
    }

    /**
     * update the view
     */
    public void updateView() {
        if (!Platform.isFxApplicationThread())
            System.err.println("updateView(): Not in FX thread!");
        spreadsheetView.getColumnPickers().clear();
        if (dataGrid.getRowCount() > 0) {
            spreadsheetView.getFixedRows().add(0);
            for (int col = 0; col < dataGrid.getColumnCount(); col++) {
                SpreadsheetCell cell = (SpreadsheetCell) ((ObservableList) spreadsheetView.getGrid().getRows().get(0)).get(col);
                cell.setEditable(false);
                cell.getStyleClass().add("first-cell");
                final int theCol = col;
                spreadsheetView.getColumnPickers().put(col, new Picker() {
                    public void onClick() {
                        final BitSet selectedColumns = getSelectedAttributesIndices();
                        if (!selectedColumns.get(theCol)) {
                            spreadsheetView.getSelectionModel().clearSelection();
                            selectedColumns.clear();
                            selectColumn(theCol);
                            selectedColumns.set(theCol);
                        }
                        samplesViewer.getCommandManager().updateEnableStateFXItems();

                        final Point p = MouseInfo.getPointerInfo().getLocation();
                        columnContextMenu.show(spreadsheetView.getScene().getWindow(), p.x + 5, p.y + 5);

                    }
                });
            }
        }
        spreadsheetView.getRowPickers().clear();
        if (dataGrid.getRowCount() > 0) {
            for (int row = 1; row < dataGrid.getRowCount(); row++) {
                final String sample = dataGrid.getRowName(row);
                final int theRow = row;
                spreadsheetView.getRowPickers().put(row, new Picker() {
                    public void onClick() {
                        final BitSet selectedRows = getSelectedSampleIndices();
                        if (!selectedRows.get(theRow) || getNumberOfSelectedCols() > 0) {
                            spreadsheetView.getSelectionModel().clearSelection();
                            selectedRows.clear();
                            selectRow(theRow);
                            selectedRows.set(theRow);
                        }
                        samplesViewer.getCommandManager().updateEnableStateFXItems();


                        while (rowContextMenu.getItems().size() > originalRowContextMenuLength) { // remove previous copy of color menu item
                            rowContextMenu.getItems().remove(rowContextMenu.getItems().size() - 1);
                        }

                        final Color color = Utilities.getColorFX(samplesViewer.getDocument().getChartColorManager().getSampleColor(sample));

                        final ColorPicker colorPicker = new ColorPicker(color);
                        colorPicker.setStyle("-fx-background-color: white;");

                        final CustomMenuItem changeColor = new CustomMenuItem(colorPicker);
                        changeColor.setHideOnClick(false);
                        colorPicker.setOnAction(new EventHandler<ActionEvent>() {
                            public void handle(ActionEvent t) {
                                spreadsheetView.getSelectionModel().clearSelection();
                                selectRow(theRow);

                                samplesViewer.getDocument().getSampleAttributeTable().putSampleColor(sample, Utilities.getColorAWT(colorPicker.getValue()));
                                rowContextMenu.hide();
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        samplesViewer.getDocument().getDir().notifyUpdateViewer(IDirector.ALL);
                                    }
                                });
                            }
                        });

                        rowContextMenu.getItems().add(changeColor);

                        final Point p = MouseInfo.getPointerInfo().getLocation();
                        rowContextMenu.show(spreadsheetView.getScene().getWindow(), p.x + 5, p.y + 5);
                    }
                });
                spreadsheetView.getGrid().getRows().get(row).get(0).setGraphic(GraphicsUtilities.makeSampleIconFX(samplesViewer.getDocument(), sample, true, true, 12));
            }
        }

        if (dataGrid.getColumnCount() > 0) {
            spreadsheetView.getFixedColumns().add(spreadsheetView.getColumns().get(0));
            for (int row = 0; row < dataGrid.getRowCount(); row++) {
                final SpreadsheetCell cell = (SpreadsheetCell) ((ObservableList) spreadsheetView.getGrid().getRows().get(row)).get(0);
                cell.setEditable(false);
                cell.getStyleClass().add("first-cell");
            }
            for (int row = 1; row < dataGrid.getRowCount(); row++) {
                final int rowF = row;
                for (int col = 1; col < dataGrid.getColumnCount(); col++) {
                    final int colF = col;
                    final SpreadsheetCell cell = (SpreadsheetCell) ((ObservableList) spreadsheetView.getGrid().getRows().get(row)).get(col);
                    cell.textProperty().addListener(new ChangeListener<Object>() {
                        @Override
                        public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                            // write back to attributes:
                            final String sample = dataGrid.getRowName(rowF);
                            final String attribute = dataGrid.getColumnName(colF);
                            samplesViewer.getDocument().getSampleAttributeTable().put(sample, attribute, newValue);
                        }
                    });
                }
            }
        }

        spreadsheetView.setShowRowHeader(true);
        spreadsheetView.setShowColumnHeader(true);
        spreadsheetView.setEditable(isEditable);
        spreadsheetView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        for (SpreadsheetColumn column : spreadsheetView.getColumns())
            column.fitColumn();
    }

    /**
     * select the given column
     *
     * @param col
     */
    private void selectColumn(int col) {
        for (int row = 0; row < dataGrid.getRowCount(); row++)
            spreadsheetView.getSelectionModel().select(row, spreadsheetView.getColumns().get(col));
    }

    /**
     * select the given row
     *
     * @param row
     */
    private void selectRow(int row) {
        spreadsheetView.getSelectionModel().select(row, spreadsheetView.getColumns().get(0));
    }

    /**
     * get selected columns (excluding row headers)
     *
     * @return number of columns, excluding headers
     */
    public List<String> getSelectedAttributes() {
        final List<String> list = new ArrayList<>();
        final BitSet seen = new BitSet();
        seen.set(0);
        try {
            if (spreadsheetView != null) { // is null at init
                final ObservableList selectedCells = spreadsheetView.getSelectionModel().getSelectedCells();
                for (Object obj : selectedCells) {
                    final int col = ((TablePosition) obj).getColumn();
                    if (!seen.get(col)) {
                        String attribute = spreadsheetView.getGrid().getRows().get(0).get(col).getItem().toString();
                        list.add(attribute);
                        seen.set(col);
                    }
                }
            }
        } catch (Exception ex) {
            if (Platform.isFxApplicationThread())
                Basic.caught(ex);
        }
        return list;
    }

    /**
     * get selected columns
     *
     * @return
     */
    public List<String> getSelectedColumns() {
        final List<String> list = new ArrayList<>();
        final BitSet seen = new BitSet();
        try {
            if (spreadsheetView != null) { // is null at init
                final ObservableList selectedCells = spreadsheetView.getSelectionModel().getSelectedCells();
                for (Object obj : selectedCells) {
                    final int col = ((TablePosition) obj).getColumn();
                    if (!seen.get(col)) {
                        String attribute = spreadsheetView.getGrid().getRows().get(0).get(col).getItem().toString();
                        list.add(attribute);
                        seen.set(col);
                    }
                }
            }
        } catch (Exception ex) {
            if (Platform.isFxApplicationThread())
                Basic.caught(ex);
        }
        return list;
    }

    /**
     * is the row header column selected?
     *
     * @return true, if first column selected
     */
    public boolean isRowHeaderSelected() {
        final ObservableList selectedCells = spreadsheetView.getSelectionModel().getSelectedCells();
        try {
            for (Object obj : selectedCells) {
                final int col = ((TablePosition) obj).getColumn();
                if (col == 0)
                    return true;
            }
        } catch (Exception ex) {
            if (Platform.isFxApplicationThread())
                Basic.caught(ex);
        }
        return false;
    }

    /**
     * get selected columns
     *
     * @return
     */
    public BitSet getSelectedAttributesIndices() {
        final BitSet set = new BitSet();
        final ObservableList selectedCells = spreadsheetView.getSelectionModel().getSelectedCells();
        try {
            for (Object obj : selectedCells) {
                final int col = ((TablePosition) obj).getColumn();
                if (col > 0)
                    set.set(col);
            }
        } catch (Exception ex) {
            if (Platform.isFxApplicationThread())
                Basic.caught(ex);
        }
        return set;
    }

    /**
     * get the selected rows
     *
     * @return selected samples
     */
    public BitSet getSelectedSampleIndices() {
        final ObservableList selectedCells = spreadsheetView.getSelectionModel().getSelectedCells();
        BitSet set = new BitSet();
        try {
            for (Object obj : selectedCells) {
                final int row = ((TablePosition) obj).getRow();
                if (row > 0)
                    set.set(row);
            }
        } catch (Exception ex) {
            if (Platform.isFxApplicationThread())
                Basic.caught(ex);
        }
        return set;
    }

    /**
     * get the selected rows
     *
     * @return selected samples
     */
    public List<String> getSelectedSamples() {
        final ObservableList selectedCells = spreadsheetView.getSelectionModel().getSelectedCells();
        final List<String> set = new LinkedList<>();
        final BitSet seen = new BitSet();
        seen.set(0); // skip first row

        try {
            for (Object obj : selectedCells) {
                final int row = ((TablePosition) obj).getRow();
                if (!seen.get(row)) {
                    String sample = getDataGrid().getRows().get(row).get(0).getItem().toString();
                    set.add(sample);
                    seen.set(row);
                }
            }
        } catch (Exception ex) {
            if (Platform.isFxApplicationThread())
                Basic.caught(ex);
        }
        return set;
    }

    /**
     * get the selected rows in sample order
     *
     * @return selected samples
     */
    public List<String> getSelectedSamplesInOrder() {
        Set<String> selectedSamples = new HashSet<>();
        selectedSamples.addAll(getSelectedSamples());

        ArrayList<String> samples = new ArrayList<>(selectedSamples.size());
        for (String sample : samplesViewer.getSampleAttributeTable().getSampleOrder()) {
            if (selectedSamples.contains(sample))
                samples.add(sample);
        }
        return samples;
    }

    /**
     * gets all selected cells
     *
     * @return set of selected cells (row,col)
     */
    private Set<Pair<Integer, Integer>> getSelectedPairs() {
        final ObservableList selectedCells = spreadsheetView.getSelectionModel().getSelectedCells();
        final Set<Pair<Integer, Integer>> set = new HashSet<>();

        try {
            for (Object obj : selectedCells) {
                set.add(new Pair<>(((TablePosition) obj).getRow(), ((TablePosition) obj).getColumn()));
            }
        } catch (Exception ex) {
            if (Platform.isFxApplicationThread())
                Basic.caught(ex);
        }
        return set;
    }


    /**
     * get a selected column
     *
     * @return
     */
    public String getASelectedColumn() {
        final ObservableList selectedCells = spreadsheetView.getSelectionModel().getSelectedCells();
        try {
            if (selectedCells.size() > 0) {
                final int col = ((TablePosition) selectedCells.get(0)).getColumn();
                return spreadsheetView.getGrid().getRows().get(0).get(col).getItem().toString();
            }
        } catch (Exception ex) {
            if (Platform.isFxApplicationThread())
                Basic.caught(ex);
        }
        return null;
    }

    /**
     * get a selected column index
     *
     * @return index or -1
     */
    public int getASelectedColumnIndex() {
        final ObservableList selectedCells = spreadsheetView.getSelectionModel().getSelectedCells();
        try {
            if (selectedCells.size() > 0) {
                return ((TablePosition) selectedCells.get(0)).getColumn();
            }
        } catch (Exception ex) {
            if (Platform.isFxApplicationThread())
                Basic.caught(ex);
        }
        return -1;
    }

    public void lockUserInput() {
        spreadsheetView.setEditable(false);
    }

    public void unlockUserInput() {
        spreadsheetView.setEditable(isEditable);
    }

    /**
     * create the main node
     *
     * @return panel
     */
    private Node createMainNode() {
        dataGrid = new DataGrid();
        spreadsheetView = new SpreadsheetView(dataGrid.getGridBase()) {
            /**
             * Create a menu on rightClick with two options: Copy/Paste This can be
             * overridden by developers for custom behavior.
             *
             * @return the ContextMenu to use.
             */
            @Override
            public ContextMenu getSpreadsheetViewContextMenu() {
                final ContextMenu contextMenu = super.getSpreadsheetViewContextMenu();
                ContextMenu tmp = new PopupMenuFX(GUIConfiguration.getMainPopupConfiguration(), samplesViewer.getCommandManager());
                contextMenu.getItems().addAll(tmp.getItems());
                contextMenu.getItems().add(new SeparatorMenuItem());

                final ColorPicker colorPicker = new ColorPicker();
                colorPicker.setStyle("-fx-background-color: white;");

                final CustomMenuItem changeColor = new CustomMenuItem(colorPicker);
                changeColor.setHideOnClick(false);
                colorPicker.setOnAction(new EventHandler<ActionEvent>() {
                    public void handle(ActionEvent t) {
                        TablePosition pos = getSpreadsheetView().getSelectionModel().getFocusedCell();
                        if (pos.getColumn() > 0) {
                            setColorForSelected(colorPicker.getValue());
                        }
                        contextMenu.hide();
                    }
                });

                contextMenu.getItems().add(changeColor);
                return contextMenu;
            }
        };
        updateView();
        return spreadsheetView;
    }

    private void setColorForSelected(Color color) {
        final ObservableList selectedCells = spreadsheetView.getSelectionModel().getSelectedCells();
        for (Object obj : selectedCells) {
            TablePosition p = (TablePosition) obj;
            int row = p.getRow();
            int col = p.getColumn();
            String attribute = dataGrid.getColumnName(col);
            String value = dataGrid.getValue(row, col);

            samplesViewer.getDocument().getChartColorManager().setAttributeStateColor(attribute, value, Utilities.getColorAWT(color));
        }
    }

    /**
     * initialize swing
     */
    private void initSwingLater() {
        jFXPanel = new JFXPanel();
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initFxLater(jFXPanel);
            }
        });
    }

    /**
     * initialize JavaFX
     *
     * @param jfxPanel
     */
    private void initFxLater(JFXPanel jfxPanel) {
        if (!initialized) {
            synchronized (lock) {
                if (!initialized) {
                    try {
                        final BorderPane rootNode = new BorderPane();
                        jfxPanel.setScene(new Scene(rootNode, 600, 600));

                        final Node main = createMainNode();
                        rootNode.setCenter(main);
                        BorderPane.setMargin(main, new Insets(3, 3, 3, 3));

                        String css = NotificationsInSwing.getControlStylesheetURL();
                        if (css != null)
                            jfxPanel.getScene().getStylesheets().add(css);
                        css = getControlStylesheetURL();
                        if (css != null) {
                            jfxPanel.getScene().getStylesheets().add(css);
                            spreadsheetView.getStylesheets().add(css);
                        }
                        columnContextMenu = new PopupMenuFX(GUIConfiguration.getAttributeColumnHeaderPopupConfiguration(), samplesViewer.getCommandManager());
                        rowContextMenu = new PopupMenuFX(GUIConfiguration.getSampleRowHeaderPopupConfiguration(), samplesViewer.getCommandManager());
                        originalRowContextMenuLength = rowContextMenu.getItems().size();
                    } finally {
                        initialized = true;
                    }
                }
            }
        }
    }

    /**
     * get the style sheet URL
     *
     * @return
     */
    public static String getControlStylesheetURL() {
        URL url = ResourceManager.getCssURL("samplesviewer.css");
        if (url != null) {
            return url.toExternalForm();
        }
        return null;
    }

    public SpreadsheetView getSpreadsheetView() {
        return spreadsheetView;
    }

    public DataGrid getDataGrid() {
        return dataGrid;
    }

    /**
     * copy selected cells to clip board
     */
    public void copyClipboard() {
        final ArrayList<GridChange> list = new ArrayList<>();
        final ObservableList selectedCells = spreadsheetView.getSelectionModel().getSelectedCells();
        final StringBuilder buf = new StringBuilder();

        int previousRow = -1;
        for (Object obj : selectedCells) {
            TablePosition p = (TablePosition) obj;
            int row = p.getRow();
            SpreadsheetCell cell = (SpreadsheetCell) ((ObservableList) spreadsheetView.getGrid().getRows().get(row)).get(p.getColumn());
            list.add(new GridChange(cell.getRow(), cell.getColumn(), null, cell.getItem() == null ? null : cell.getItem().toString()));
            if (previousRow == -1)
                previousRow = row;
            else if (row != previousRow) {
                buf.append("\n");
                previousRow = row;
            } else
                buf.append("\t");
            buf.append(cell.getItem().toString().replaceAll("\\s+", " "));
        }
        buf.append("\n");

        Platform.runLater(new Runnable() {
            public void run() {
                ClipboardContent contents = new ClipboardContent();
                contents.put(fmt, list);
                contents.putString(buf.toString());
                Clipboard.getSystemClipboard().setContent(contents);
            }
        });
    }

    /**
     * paste into table
     */
    public void pasteClipboard() {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.getContent(fmt) != null) {
            spreadsheetView.pasteClipboard();
        } else {
                String contents = clipboard.getString().trim().replaceAll("\r\n", "\n").replaceAll("\r", "\n");
                String[] lines = contents.split("\n");
            paste(lines);
            }
        }

    /**
     * pastes lines into table guided by an attribute
     *
     * @param attribute
     * @throws IOException
     */
    public void pasteClipboardByAttribute(String attribute) throws IOException {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        Set<Pair<Integer, Integer>> selectedPairs = getSelectedPairs();
        if (selectedPairs.size() > 0) {
            BitSet rows = getSelectedSampleIndices();

            String contents = clipboard.getString().trim().replaceAll("\r\n", "\n").replaceAll("\r", "\n");
            String[] lines = contents.split("\n");
            if (lines.length > 0) {
                final int guideCol = dataGrid.findColumn(attribute);
                final Map<String, String> attributeValue2Line = new HashMap<>();
                int inputLineNumber = 0;

                String[] toPaste = new String[getNumberOfSelectedSamples()];
                int expandedLineNumber = 0;
                for (int row = rows.nextSetBit(1); row != -1; row = rows.nextSetBit(row + 1)) {
                    String value = dataGrid.getValue(row, guideCol);
                    if (!attributeValue2Line.containsKey(value)) {
                        if (inputLineNumber == lines.length)
                            break;
                        toPaste[expandedLineNumber++] = lines[inputLineNumber];
                        attributeValue2Line.put(value, lines[inputLineNumber++]);
                    } else {
                        if (expandedLineNumber == toPaste.length)
                            break;
                        toPaste[expandedLineNumber++] = attributeValue2Line.get(value);
                    }
                }
                if (attributeValue2Line.size() != lines.length) {
                    throw new IOException("Mismatch between number of lines pasted (" + lines.length +
                            ") and number of attribute values (" + attributeValue2Line.size() + ")");
                }
                paste(toPaste);
            }
        }
    }

    /**
     * pastes lines into table
     *
     * @param lines
     */
    private void paste(String[] lines) {
        if (lines.length > 0) {
            Set<Pair<Integer, Integer>> selectedPairs = getSelectedPairs();
            if (selectedPairs.size() > 0) {
                BitSet rows = getSelectedSampleIndices();
                BitSet cols = getSelectedAttributesIndices();

                int row = rows.nextSetBit(1);
                for (String line : lines) {
                    int col = cols.nextSetBit(1);
                    String[] values = line.trim().split("\t");
                    for (String value : values) {
                        value = value.trim();
                        // move to next col that is selected in this row:
                        while (col != -1 && !selectedPairs.contains(new Pair<>(row, col)))
                            col = cols.nextSetBit(col + 1);
                        if (col != -1) {
                            final SpreadsheetCell cell = getSpreadsheetView().getGrid().getRows().get(row).get(col);
                            boolean succeed = cell.getCellType().match(value);
                            if (succeed) {
                                getSpreadsheetView().getGrid().setCellValue(cell.getRow(), cell.getColumn(),
                                        cell.getCellType().convertValue(value));
                            }
                            col = cols.nextSetBit(col + 1);
                        } else
                            break;
                    }
                    row = rows.nextSetBit(row + 1);
                    if (row == -1)
                        break;
                }
            }
        }
    }


    /**
     * sort by the selected column
     *
     * @param attribute
     * @param ascending
     */
    public void sortByColumn(final String attribute, final boolean ascending) {
        Platform.runLater(new Runnable() {
            public void run() {
                dataGrid.save(samplesViewer.getSampleAttributeTable(), null);
                if (samplesViewer.getSampleAttributeTable().sortSamplesByAttribute(attribute, ascending)) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            final Document doc = samplesViewer.getDocument();
                            try {
                                doc.setProgressListener(new ProgressSilent());
                                doc.reorderSamples(doc.getSampleAttributeTable().getSampleOrder());
                                System.err.println("Order: " + Basic.toString(doc.getSampleAttributeTable().getSampleOrder(), ","));
                                doc.getDir().execute("update reinduce=true;", samplesViewer.getCommandManager());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * add a new column
     *
     * @param column
     */
    public void addNewColumn(final int column, String newName) {
        if (newName.length() > 0) {
            if (samplesViewer.getSampleAttributeTable().getAttributeOrder().contains(newName)) {
                int num = 1;
                while (samplesViewer.getSampleAttributeTable().getAttributeOrder().contains(newName + "." + num)) {
                    num++;
                }
                newName = newName + "." + num;
            }
            final String attribute = newName;

            Platform.runLater(new Runnable() {
                public void run() {
                    dataGrid.save(samplesViewer.getSampleAttributeTable(), new DataGrid.TableEdit(DataGrid.Operation.Append, column, attribute));
                }
            });
        }
    }

    /**
     * delete columns
     */
    public void deleteColumns(final String[] attributes) {
        Platform.runLater(new Runnable() {
            public void run() {
                dataGrid.save(samplesViewer.getSampleAttributeTable(), new DataGrid.TableEdit(DataGrid.Operation.Delete, -1, attributes));
            }
        });
    }

    /**
     * hide named columns
     */
    public void hideColumns(final String[] attributes) {
        Platform.runLater(new Runnable() {
            public void run() {
                dataGrid.save(samplesViewer.getSampleAttributeTable(), new DataGrid.TableEdit(DataGrid.Operation.Hide, -1, attributes));
                getSpreadsheetView().getSelectionModel().clearSelection();
            }
        });
    }

    /**
     * unhide named columns
     */
    public void unhideColumns(final String[] attributes) {
        Platform.runLater(new Runnable() {
            public void run() {
                dataGrid.save(samplesViewer.getSampleAttributeTable(), new DataGrid.TableEdit(DataGrid.Operation.Unhide, -1, attributes));
                dataGrid.load(samplesViewer.getSampleAttributeTable());
                selectColumns(attributes);
            }
        });
    }

    /**
     * move named columns
     */
    public void moveColumns(final boolean left, final String[] attributes) {
        Platform.runLater(new Runnable() {
            public void run() {
                dataGrid.save(samplesViewer.getSampleAttributeTable(), new DataGrid.TableEdit(left ? DataGrid.Operation.MoveLeft : DataGrid.Operation.MoveRight, -1, attributes));
                dataGrid.load(samplesViewer.getSampleAttributeTable());
                selectColumns(attributes);
            }
        });
    }

    /**
     * move named samples
     */
    public void moveSamples(final boolean up, final String[] samples) {
        Platform.runLater(new Runnable() {
            public void run() {
                dataGrid.save(samplesViewer.getSampleAttributeTable(), new DataGrid.TableEdit(up ? DataGrid.Operation.MoveSamplesUp : DataGrid.Operation.MoveSamplesDown, -1, samples));
                dataGrid.load(samplesViewer.getSampleAttributeTable());
                selectRows(samples);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        final Document doc = samplesViewer.getDocument();
                        try {
                            doc.setProgressListener(new ProgressSilent());
                            doc.reorderSamples(doc.getSampleAttributeTable().getSampleOrder());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });
    }

    /**
     * rename an attribute
     *
     * @param attribute
     * @param newName
     */
    public void renameAttribute(final String attribute, final String newName) {
        Platform.runLater(new Runnable() {
            public void run() {
                int col = dataGrid.findColumn(attribute);
                if (col > 0) {
                    String name = newName;
                    if (getDataGrid().getAttributesOrder().contains(name)) {
                        int count = 1;
                        while (getDataGrid().getAttributesOrder().contains(name))
                            count++;
                        name += "." + count;
                    }
                    dataGrid.save(samplesViewer.getSampleAttributeTable(), new DataGrid.TableEdit(DataGrid.Operation.RenameAttribute, col, name));
                    dataGrid.load(samplesViewer.getSampleAttributeTable());
                    selectColumn(col);
                }
            }
        });
    }

    /**
     * rename a sample
     *
     * @param sample
     * @param newName
     */
    public void renameSample(final String sample, final String newName) {
        Platform.runLater(new Runnable() {
            public void run() {
                try {
                    String name = newName;
                    Document doc = samplesViewer.getDocument();

                    if (doc.getSampleNames().contains(name)) {
                        int count = 1;
                        while (doc.getSampleNames().contains(name + "." + count))
                            count++;
                        name += "." + count;
                    }
                    dataGrid.save(samplesViewer.getSampleAttributeTable(), null);
                    if (doc.getSampleAttributeTable().getSampleLabel(sample) != null && doc.getSampleAttributeTable().getSampleLabel(sample).equals(sample))
                        doc.getSampleAttributeTable().putSampleLabel(sample, name);
                    doc.renameSample(sample, name);
                    dataGrid.load(samplesViewer.getSampleAttributeTable());
                    selectRow(dataGrid.findRow(newName));
                } catch (IOException ex) {
                    Basic.caught(ex);
                }
            }
        });
    }


    /**
     * get all hidden columns associated with the given ones
     *
     * @return hidden
     */
    public Collection<String> getHiddenAssociatedWithColumns() {
        List<String> result = new LinkedList<>();
        if (isRowHeaderSelected()) {
            int which = 0;
            while (which < dataGrid.getAttributesOrder().size()) {
                String next = dataGrid.getAttributesOrder().get(which);
                if (samplesViewer.getSampleAttributeTable().isHiddenAttribute(next)) {
                    result.add(next);
                } else
                    break;
                which++;
            }
        }
        try {
            for (String attribute : getSelectedAttributes()) {
                int which = dataGrid.getAttributesOrder().indexOf(attribute) - 1;
                while (which >= 0) {
                    String next = dataGrid.getAttributesOrder().get(which);
                    if (samplesViewer.getSampleAttributeTable().isHiddenAttribute(next)) {
                        result.add(next);
                    } else
                        break;
                    which--;
                }
                which = dataGrid.getAttributesOrder().indexOf(attribute) + 1;
                while (which > 0 && which < dataGrid.getAttributesOrder().size()) {
                    String next = dataGrid.getAttributesOrder().get(which);
                    if (samplesViewer.getSampleAttributeTable().isHiddenAttribute(next)) {
                        result.add(next);
                    } else
                        break;
                    which++;
                }
            }
        } catch (Exception ex) {
            if (Platform.isFxApplicationThread())
                Basic.caught(ex);
        }
        return result;
    }

    /**
     * is unhide applicable to current selection?
     *
     * @return true, if unhide applicable
     */
    public boolean unhideIsApplicable() {

        try {
            if (getSelectedColumns().size() == 1 && getSelectedAttributes().size() == 0) { // there are no visible columns and the row header is selected...
                int which = 0;
                if (which < dataGrid.getAttributesOrder().size()) {
                    String next = dataGrid.getAttributesOrder().get(which);
                    if (samplesViewer.getSampleAttributeTable().isHiddenAttribute(next))
                        return true;
                }
                return false;
            }

            for (String attribute : getSelectedAttributes()) {
                int which = dataGrid.getAttributesOrder().indexOf(attribute) - 1;
                if (which >= 0) {
                    String next = dataGrid.getAttributesOrder().get(which);
                    if (samplesViewer.getSampleAttributeTable().isHiddenAttribute(next)) {
                        return true;
                    }
                }
                which = dataGrid.getAttributesOrder().indexOf(attribute) + 1;
                if (which > 0 && which < dataGrid.getAttributesOrder().size()) {
                    String next = dataGrid.getAttributesOrder().get(which);
                    if (samplesViewer.getSampleAttributeTable().isHiddenAttribute(next)) {
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            if (Platform.isFxApplicationThread())
                Basic.caught(ex);
        }
        return false;
    }

    /**
     * select cells for an attribute and value
     *
     * @param attribute
     * @param value
     */
    public void selectCellsByValue(final String attribute, final String value) {
        Platform.runLater(new Runnable() {
            public void run() {
                int col = dataGrid.findColumn(attribute);
                if (col != -1) {
                    SpreadsheetColumn column = spreadsheetView.getColumns().get(col);

                    for (int row = 1; row < dataGrid.getRowCount(); row++) {
                        if (dataGrid.getValue(row, col).equals(value)) {
                            spreadsheetView.getSelectionModel().select(row, column);
                        }
                    }
                }
            }
        });
    }

    void updateNumberOfSelectedRowsAndCols() {
        numberOfSelectedRows = getSelectedSamples().size();
        numberOfSelectedCols = getSelectedAttributes().size();
        numberOfSelectedColsIncludingSamplesCol = getSelectedColumns().size();
    }

    public int getNumberOfSelectedSamples() {
        return numberOfSelectedRows;
    }

    public int getNumberOfSelectedCols() {
        return numberOfSelectedCols;
    }

    public int getNumberOfSelectedColsIncludingSamplesCol() {
        return numberOfSelectedColsIncludingSamplesCol;
    }

    public void selectColumns(String[] names) {
        getSpreadsheetView().getSelectionModel().clearSelection();
        for (String name : names) {
            int col = dataGrid.findColumn(name);
            if (col != 1)
                selectColumn(col);
        }
    }

    public void selectRows(String[] names) {
        getSpreadsheetView().getSelectionModel().clearSelection();
        for (String name : names) {
            int row = dataGrid.findRow(name);
            if (row != 1)
                selectRow(row);
        }
    }

}
