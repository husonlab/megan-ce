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
package megan.samplesviewer.commands;

import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.gui.director.ProjectManager;
import jloda.util.parse.NexusStreamParser;
import megan.samplesviewer.SamplesViewer;
import org.controlsfx.control.spreadsheet.SpreadsheetColumn;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * * selection command
 * * Daniel Huson, 11.2010
 */
public class SelectAllCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "select {all|none|similar|fromPrevious|commentLike|numerical|uninformative} [name=<string>] [value=<string>];";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(final NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("select");
        final String what = np.getWordMatchesIgnoringCase("all none similar commentLike numerical uninformative fromPrevious");
        final String name;
        final String value;
        if (what.equalsIgnoreCase("similar")) {
            np.matchIgnoreCase("name=");
            name = np.getWordRespectCase();
            if (np.peekMatchIgnoreCase("value=")) {
                np.matchIgnoreCase("value=");
                value = np.getWordRespectCase();
            } else
                value = null;
        } else {
            name = null;
            value = null;
        }
        np.matchIgnoreCase(";");

        javafx.application.Platform.runLater(new Runnable() {
            public void run() {
                final SamplesViewer viewer = (SamplesViewer) getViewer();

                switch (what) {
                    case "all":
                        viewer.getSamplesTable().getSpreadsheetView().getSelectionModel().selectAll();
                        break;
                    case "none":
                        viewer.getSamplesTable().getSpreadsheetView().getSelectionModel().clearSelection();
                        break;
                    case "commentLike": {
                        int count = 0;
                        for (int col = 0; col < viewer.getSamplesTable().getDataGrid().getColumnCount(); col++) {
                            final String attribute = viewer.getSamplesTable().getDataGrid().getColumnName(col);

                            int min = Integer.MAX_VALUE;
                            int max = 0;
                            for (int row = 0; row < viewer.getSamplesTable().getDataGrid().getRowCount(); row++) {
                                String sample = viewer.getSamplesTable().getDataGrid().getRowName(row);
                                Object value = viewer.getSampleAttributeTable().get(sample, attribute);
                                if (value != null) {
                                    String string = value.toString().trim();
                                    if (string.length() > 0) {
                                        min = Math.min(min, string.length());
                                        max = Math.max(max, string.length());
                                    }
                                }
                            }
                            if (max - min > 100) {
                                final SpreadsheetColumn column = viewer.getSamplesTable().getSpreadsheetView().getColumns().get(col);
                                for (int row = 0; row < viewer.getSamplesTable().getDataGrid().getRowCount(); row++) {
                                    viewer.getSamplesTable().getSpreadsheetView().getSelectionModel().select(row, column);
                                }
                                count++;
                            }
                        }
                        if (count > 0)
                            System.err.println("Selected " + count + " columns");
                        break;
                    }
                    case "numerical": {
                        int count = 0;
                        final Collection<String> numericalAttributes = viewer.getSampleAttributeTable().getNumericalAttributes();
                        for (int col = 0; col < viewer.getSamplesTable().getDataGrid().getColumnCount(); col++) {
                            String attribute = viewer.getSamplesTable().getDataGrid().getColumnName(col);
                            if (numericalAttributes.contains(attribute)) {
                                final SpreadsheetColumn column = viewer.getSamplesTable().getSpreadsheetView().getColumns().get(col);
                                for (int row = 0; row < viewer.getSamplesTable().getDataGrid().getRowCount(); row++) {
                                    viewer.getSamplesTable().getSpreadsheetView().getSelectionModel().select(row, column);
                                }
                                count++;
                            }

                        }
                        if (count > 0)
                            System.err.println("Selected " + count + " columns");
                        break;
                    }
                    case "uninformative": {
                        int count = 0;
                        for (int col = 0; col < viewer.getSamplesTable().getDataGrid().getColumnCount(); col++) {
                            final String attribute = viewer.getSamplesTable().getDataGrid().getColumnName(col);

                            final Set<String> values = new HashSet<>();
                            for (int row = 0; row < viewer.getSamplesTable().getDataGrid().getRowCount(); row++) {
                                String sample = viewer.getSamplesTable().getDataGrid().getRowName(row);
                                Object value = viewer.getSampleAttributeTable().get(sample, attribute);
                                if (value != null) {
                                    String string = value.toString().trim();
                                    if (string.length() > 0) {
                                        values.add(string);
                                    }
                                }
                            }
                            if (values.size() <= 1 || values.size() == viewer.getSamplesTable().getDataGrid().getRowCount()) {
                                final SpreadsheetColumn column = viewer.getSamplesTable().getSpreadsheetView().getColumns().get(col);
                                for (int row = 0; row < viewer.getSamplesTable().getDataGrid().getRowCount(); row++) {
                                    viewer.getSamplesTable().getSpreadsheetView().getSelectionModel().select(row, column);
                                }
                                count++;
                            }
                        }
                        if (count > 0)
                            System.err.println("Selected " + count + " columns");
                        break;
                    }
                    case "similar":
                        viewer.getSamplesTable().selectCellsByValue(name, value);
                        break;
                    case "fromPrevious":
                        int row1 = -1;
                        for (int row = 0; row < viewer.getSamplesTable().getDataGrid().getRowCount(); row++) {
                            String sample = viewer.getSamplesTable().getDataGrid().getRowName(row);
                            if (ProjectManager.getPreviouslySelectedNodeLabels().contains(sample)) {
                                SpreadsheetColumn column = viewer.getSamplesTable().getSpreadsheetView().getColumns().get(0);
                                viewer.getSamplesTable().getSpreadsheetView().getSelectionModel().select(row, column);
                                row1 = row;
                            }
                        }
                        if (row1 != -1) {
                            viewer.getSamplesTable().getSpreadsheetView().scrollToRow(row1);
                            viewer.getSamplesTable().getSpreadsheetView().scrollToColumnIndex(0);
                        }

                        int col1 = -1;
                        for (int col = 0; col < viewer.getSamplesTable().getDataGrid().getColumnCount(); col++) {
                            String attribute = viewer.getSamplesTable().getDataGrid().getColumnName(col);
                            if (ProjectManager.getPreviouslySelectedNodeLabels().contains(attribute)) {
                                SpreadsheetColumn column = viewer.getSamplesTable().getSpreadsheetView().getColumns().get(col);
                                viewer.getSamplesTable().getSpreadsheetView().getSelectionModel().select(0, column);
                                col1 = col;
                            }
                        }
                        if (col1 != -1) {
                            viewer.getSamplesTable().getSpreadsheetView().scrollToRow(0);
                            viewer.getSamplesTable().getSpreadsheetView().scrollToColumnIndex(col1);
                        }
                }
            }
        });
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("select all;");
    }

    public boolean isApplicable() {
        return getViewer() instanceof SamplesViewer;
    }

    public String getName() {
        return "Select All";
    }

    public String getDescription() {
        return "Selection";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }
}
