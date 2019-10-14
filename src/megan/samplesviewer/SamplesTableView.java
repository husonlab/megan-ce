/*
 *  SamplesTableView.java Copyright (C) 2019 Daniel H. Huson
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
import javafx.beans.InvalidationListener;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import jloda.fx.control.table.MyTableView;
import jloda.swing.director.IDirector;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgressSilent;
import jloda.util.Triplet;
import megan.core.Director;
import megan.core.Document;
import megan.core.SampleAttributeTable;
import megan.fx.FXSwingUtilities;
import megan.fx.PopupMenuFX;
import megan.util.GraphicsUtilities;

import javax.swing.*;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class SamplesTableView {
    private static final Set<String> seenFXExceptions = new HashSet<>();
    private static final Thread.UncaughtExceptionHandler fxExceptionHandler = (t, e) -> {
        if (!seenFXExceptions.contains(e.getMessage())) {
            seenFXExceptions.add(e.getMessage());
            System.err.println("FX Exception: " + e.getMessage());
        }
    };

    private final SamplesViewer samplesViewer;
    private final JFXPanel jFXPanel = new JFXPanel();

    private MyTableView tableView;

    private int selectedSamplesCount = 0;
    private int selectedAttributesCount = 0;

    private boolean initialized = false;

    private long initialUpdate = Long.MAX_VALUE;


    private final Object lock = new Object();

    /**
     * constructor
     */
    public SamplesTableView(SamplesViewer samplesViewer) {
        this.samplesViewer = samplesViewer;
        Platform.runLater(() -> initFxLater(jFXPanel));
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

    public List<String> getSelectedSamples() {
        if (tableView != null)
            return new ArrayList<>(tableView.getSelectedRows());
        else
            return new ArrayList<>();
    }

    public List<String> getSelectedAttributes() {
        if (tableView != null)
            return new ArrayList<>(tableView.getSelectedCols());
        else
            return new ArrayList<>();
    }

    public void lockUserInput() {
        if (tableView != null) {
            ensureFXThread(() -> tableView.setEditable(false));
        }
    }

    public void unlockUserInput() {
        if (tableView != null)
            ensureFXThread(() -> tableView.setEditable(true));
    }

    /**
     * create the main node
     *
     * @return panel
     */
    private Node createMainNode() {
        tableView = new MyTableView();
        tableView.setAllowRenameCol(true);
        //tableView.setAllowReorderRow(true); // todo: throws expections
        tableView.getUnrenameableCols().add(SampleAttributeTable.SAMPLE_ID);
        tableView.setAllowDeleteCol(true);
        tableView.getUndeleteableCols().add(SampleAttributeTable.SAMPLE_ID);

        tableView.updateProperty().addListener((c, o, n) -> {
            if (n.longValue() > initialUpdate) {
                Platform.runLater(() -> {
                    SwingUtilities.invokeLater(this::syncFromViewToDocument);
                    if (!samplesViewer.getDocument().isDirty()) {
                        samplesViewer.getDocument().setDirty(true);
                        samplesViewer.getDir().notifyUpdateViewer(IDirector.TITLE);
                    }
                });
            }
        });
        tableView.setAllowAddCol(true);

        tableView.setAdditionColHeaderMenuItems((col) -> {
            //tableView.getSelectionModel().clearSelection();
            //tableView.selectColumn(col,true);
            return (new PopupMenuFX(GUIConfiguration.getAttributeColumnHeaderPopupConfiguration(), samplesViewer.getCommandManager())).getItems();
        });

        tableView.setAdditionRowHeaderMenuItems((rows) -> {
            tableView.selectRows(rows, true);
            return (new PopupMenuFX(GUIConfiguration.getSampleRowHeaderPopupConfiguration(), samplesViewer.getCommandManager())).getItems();
        });

        tableView.getSelectionModel().getSelectedCells().addListener((InvalidationListener) (e) -> {
            updateNumberOfSelectedRowsAndCols();
            SwingUtilities.invokeLater(() -> samplesViewer.updateView(Director.ENABLE_STATE));
        });

        tableView.getSelectedRows().addListener((InvalidationListener) (e) -> {
            updateNumberOfSelectedRowsAndCols();
            SwingUtilities.invokeLater(() -> samplesViewer.updateView(Director.ENABLE_STATE));
        });
        initialUpdate = Long.MAX_VALUE;

        return tableView;
    }

    private void setColorForSelected(Color color) {
        if (tableView != null) {
            for (TablePosition position : tableView.getSelectedCells()) {
                final String attribute = tableView.getColName(position.getColumn());
                final String value = tableView.getValue(position.getRow(), position.getColumn());
                samplesViewer.getDocument().getChartColorManager().setAttributeStateColor(attribute, value, FXSwingUtilities.getColorAWT(color));
            }
        }
    }

    /**
     * initialize JavaFX
     *
     * @param jfxPanel
     */
    private void initFxLater(JFXPanel jfxPanel) {
        if (!initialized) {
            if (Thread.getDefaultUncaughtExceptionHandler() != fxExceptionHandler)
                Thread.setDefaultUncaughtExceptionHandler(fxExceptionHandler);
            synchronized (lock) {
                if (!initialized) {
                    try {
                        final BorderPane rootNode = new BorderPane();
                        jfxPanel.setScene(new Scene(rootNode, 600, 600));

                        final Node main = createMainNode();
                        rootNode.setCenter(main);
                        BorderPane.setMargin(main, new Insets(3, 3, 3, 3));

                        // String css = NotificationsInSwing.getControlStylesheetURL();
                        // if (css != null)
                        //    jfxPanel.getScene().getStylesheets().add(css);
                    } finally {
                        initialized = true;
                    }
                }
            }
        }
    }

    public MyTableView getTableView() {
        return tableView;
    }

    public void copyToClipboard() {
        if (tableView != null)
            ensureFXThread(() -> tableView.copyToClipboard());
    }

    /**
     * paste into table
     */
    public void pasteClipboard() {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final String contents = clipboard.getString().trim().replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        final String[] lines = contents.split("\n");
        paste(lines);
    }

    /**
     * pastes lines into table
     *
     * @param lines
     */
    private void paste(String[] lines) {
        if (tableView != null) {
            ensureFXThread(() -> {
                if (lines.length > 0 && tableView.getSelectedCells().size() > 0) {
                    final Set<Pair<Integer, Integer>> selectedPairs = new HashSet<>();
                    final BitSet rows = new BitSet();
                    final BitSet cols = new BitSet();
                    for (TablePosition position : tableView.getSelectedCells()) {
                        final int row = position.getRow();
                        final int col = position.getColumn();
                        selectedPairs.add(new Pair<>(row, col));
                        rows.set(row);
                        cols.set(col);
                    }

                    int row = rows.nextSetBit(0);
                    for (String line : lines) {
                        int col = cols.nextSetBit(1);
                        String[] values = line.trim().split("\t");
                        for (String value : values) {
                            value = value.trim();
                            // move to next col that is selected in this row:
                            while (col != -1 && !selectedPairs.contains(new Pair<>(row, col)))
                                col = cols.nextSetBit(col + 1);
                            if (col != -1) {
                                tableView.setValue(row, col, value);
                                col = cols.nextSetBit(col + 1);
                            } else
                                break;
                        }
                        row = rows.nextSetBit(row + 1);
                        if (row == -1)
                            break;
                    }
                }
            });
        }
    }

    /**
     * pastes lines into table guided by an attribute
     *
     * @param attribute
     * @throws IOException
     */
    public void pasteClipboardByAttribute(String attribute) throws IOException {
        if (tableView != null && tableView.getSelectedCells().size() > 0) {
            ensureFXThread(() -> {
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                final BitSet rows = new BitSet();
                for (TablePosition position : tableView.getSelectedCells()) {
                    rows.set(position.getRow());
                }
                String contents = clipboard.getString().trim().replaceAll("\r\n", "\n").replaceAll("\r", "\n");
                String[] lines = contents.split("\n");
                if (lines.length > 0) {
                    final int guideCol = tableView.getColIndex(attribute);
                    final Map<String, String> attributeValue2Line = new HashMap<>();
                    int inputLineNumber = 0;

                    String[] toPaste = new String[tableView.getSelectedRows().size()];
                    int expandedLineNumber = 0;
                    for (int row = rows.nextSetBit(1); row != -1; row = rows.nextSetBit(row + 1)) {
                        String value = tableView.getValue(row, guideCol);
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
                        System.err.println("Mismatch between number of lines pasted (" + lines.length +
                                ") and number of attribute values (" + attributeValue2Line.size() + ")");
                        return;
                    }
                    paste(toPaste);
                }
            });
        }
    }

    /**
     * add a new column
     *
     * @param index
     */
    public void addNewColumn(final int index, String name) {
        ensureFXThread(() -> tableView.addCol(index, name));
    }

    /**
     * delete columns
     */
    public void deleteColumns(final String... attributes) {
        if (tableView != null)
            ensureFXThread(() -> {
                for (String col : attributes) {
                    tableView.deleteCol(col);
                }
            });
    }

    /**
     * select cells for an attribute and value
     *
     * @param attribute
     * @param value
     */
    public void selectCellsByValue(final String attribute, final String value) {
        if (tableView != null)
            ensureFXThread(() -> tableView.selectByValue(attribute, value));
    }

    private void updateNumberOfSelectedRowsAndCols() {
        selectedSamplesCount = getSelectedSamples().size();
        selectedAttributesCount = getSelectedAttributes().size();
    }

    public int getCountSelectedSamples() {
        return selectedSamplesCount;
    }

    public int getCountSelectedAttributes() {
        return selectedAttributesCount;
    }

    public void syncFromViewToDocument() {
        // System.err.println("Syncing to document (update: "+tableView.getUpdate()+")");

        final SampleAttributeTable sampleAttributeTable = samplesViewer.getSampleAttributeTable();

        if (sampleAttributeTable.getSampleOrder().size() != tableView.getRowNames().size())
            throw new RuntimeException("Table size mismatch");
        sampleAttributeTable.getSampleOrder().clear();
        sampleAttributeTable.getSampleOrder().addAll(tableView.getRowNames());

        final ArrayList<String> toDelete = new ArrayList<>();
        for (String attribute : sampleAttributeTable.getAttributeOrder()) {
            if (!sampleAttributeTable.isSecretAttribute(attribute))
                toDelete.add(attribute);
        }
        for (String attribute : toDelete)
            sampleAttributeTable.removeAttribute(attribute);

        final List<String> secretAttributes = new ArrayList<>(sampleAttributeTable.getAttributeOrder());
        sampleAttributeTable.getAttributeOrder().removeAll(secretAttributes); // will put these back at the end of the list

        for (String attribute : tableView.getColNames()) {
            final Map<String, Object> sampleValueMap = new HashMap<>();
            for (String sample : tableView.getRowNames())
                sampleValueMap.put(sample, tableView.getValue(sample, attribute));
            sampleAttributeTable.addAttribute(attribute, sampleValueMap, false, false);
        }
        sampleAttributeTable.getAttributeOrder().addAll(secretAttributes);

        final StringWriter w = new StringWriter();
        try {
            sampleAttributeTable.write(w, false, true);
        } catch (IOException e) {
            Basic.caught(e);
        }
        initialUpdate = tableView.getUpdate();

        //System.err.println("Doc: "+w.toString());
    }

    public void syncFromDocumentToView() {
        if (tableView != null) {
            ensureFXThread(() -> {
                initialUpdate = Long.MAX_VALUE;

                //System.err.println("Syncing to view");

                final SampleAttributeTable sampleAttributeTable = samplesViewer.getSampleAttributeTable();

                final ArrayList<String> samples = sampleAttributeTable.getSampleOrder();
                final ArrayList<String> userAttributes = sampleAttributeTable.getUnhiddenAttributes();

                tableView.pausePostingUpdates();
                try {
                    tableView.createRowsAndCols(samples, userAttributes);

                    for (int row = 0; row < samples.size(); row++) {
                        final String sample = samples.get(row);
                        for (int col = 0; col < userAttributes.size(); col++) {
                            final String attribute = userAttributes.get(col);
                            final Object value = sampleAttributeTable.get(sample, attribute);
                            tableView.setValue(row, col, value != null ? value.toString() : "?");
                        }
                        tableView.setRowGraphic(sample, GraphicsUtilities.makeSampleIconFX(samplesViewer.getDocument(), sample, true, true, 16));
                    }
                } finally {
                    initialUpdate = tableView.getUpdate() + 1;
                    tableView.resumePostingUpdates();
                }
            });
        }
    }

    public String getASelectedAttribute() {
        if (getCountSelectedAttributes() > 0)
            return getSelectedAttributes().get(0);
        else
            return null;
    }

    public String getASelectedSample() {
        if (getCountSelectedSamples() > 0)
            return getSelectedSamples().get(0);
        else
            return null;
    }

    public void renameRow(String sampleName, String newName) {
        if (tableView != null) {
            Platform.runLater(() -> tableView.renameRow(sampleName, newName));
        }
    }

    public void clear() {
        if (tableView != null) {
            ensureFXThread(() -> tableView.clear());
        }
    }

    public int getAttributeCount() {
        if (tableView != null)
            return tableView.getColCount();
        else
            return 0;
    }

    public TableColumn<MyTableView.MyTableRow, ?> getAttribute(int index) {
        if (tableView != null)
            return tableView.getCol(index);
        else
            return null;
    }

    public ArrayList<String> getAttributes() {
        if (tableView != null)
            return tableView.getColNames();
        else
            return new ArrayList<>();
    }

    public ArrayList<String> getSamples() {
        if (tableView != null)
            return tableView.getRowNames();
        else
            return new ArrayList<>();
    }

    public int getSampleCount() {
        if (tableView != null)
            return tableView.getRowCount();
        else
            return 0;
    }

    public void selectAll(boolean select) {
        if (tableView != null) {
            ensureFXThread(() -> {
                if (select)
                    tableView.getSelectionModel().selectAll();
                else
                    tableView.getSelectionModel().clearSelection();
            });
        }
    }

    public void selectSample(String sample, boolean select) {
        if (tableView != null) {
            ensureFXThread(() -> tableView.selectRow(sample, select));
        }
    }

    public void selectSamples(Collection<String> samples, boolean select) {
        if (tableView != null) {
            ensureFXThread(() -> tableView.selectRowHeaders(samples, select));
        }
    }

    public void selectByValue(String attribute, Object value) {
        if (tableView != null) {
            ensureFXThread(() -> tableView.selectByValue(attribute, value.toString()));
        }
    }

    public void selectAttribute(String attribute, boolean select) {
        if (tableView != null) {
            ensureFXThread(() -> tableView.selectCol(attribute, select));
        }
    }

    public void scrollToSample(String sample) {
        if (tableView != null) {
            ensureFXThread(() -> {
                if (sample == null)
                    tableView.scrollToRow(0);
                else
                    tableView.scrollToRow(sample);
            });
        }
    }

    public boolean isDirty() {
        if (tableView != null)
            return tableView.getUpdate() > initialUpdate;
        else
            return false;
    }

    public Triplet<String, String, String> getSingleSelectedCell() {
        if (tableView != null)
            return tableView.getSingleSelectedCell();
        else
            return null;
    }

    public int getUpdate() {
        if (tableView != null)
            return tableView.getRowCount();
        else
            return 0;
    }

    private static void ensureFXThread(Runnable runnable) {
        if (Platform.isFxApplicationThread())
            runnable.run();
        else
            Platform.runLater(runnable);
    }

    public void moveSamples(boolean up, Collection<String> samples) {
        Platform.runLater(() -> {
            final Document doc = samplesViewer.getDocument();
            SwingUtilities.invokeLater(() -> {
                try {
                    doc.getSampleAttributeTable().moveSamples(up, samples);
                    doc.setProgressListener(new ProgressSilent());
                    doc.reorderSamples(doc.getSampleAttributeTable().getSampleOrder());
                    samplesViewer.getDir().executeImmediately("update reinduce=true;" +
                            "select samples name='" + Basic.toString(samples, "' '") + "';", samplesViewer.getCommandManager());
                    selectSamples(samples, true);
                } catch (Exception e) {
                    Basic.caught(e);
                }
            });
        });
    }

    public ArrayList<Integer> getSelectedSamplesIndices() {
        if (tableView != null)
            return tableView.getSelectedRowIndices();
        else
            return new ArrayList<>();
    }

}
