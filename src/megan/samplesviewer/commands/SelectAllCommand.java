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

/**
 * * selection command
 * * Daniel Huson, 11.2010
 */
public class SelectAllCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "select {all|none|similar|fromPrevious} [name=<string>] [value=<string>];";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(final NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("select");
        final String what = np.getWordMatchesIgnoringCase("all none similar fromPrevious");
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
        return true;
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
