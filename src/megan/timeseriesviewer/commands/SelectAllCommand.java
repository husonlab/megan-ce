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
package megan.timeseriesviewer.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.timeseriesviewer.DataJTable;
import megan.timeseriesviewer.TimeSeriesViewer;

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
        return "set select={all|none|sample|name=<string>|row=<number>|col=<number>};";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set select=");

        TimeSeriesViewer viewer = (TimeSeriesViewer) getViewer();
        DataJTable dataJTable = viewer.getDataJTable();

        if (np.peekMatchIgnoreCase("all")) {
            np.matchIgnoreCase("all");
            dataJTable.selectAll();
        } else if (np.peekMatchIgnoreCase("none")) {
            np.matchIgnoreCase("none");
            dataJTable.clearSelection();
        } else if (np.peekMatchIgnoreCase("name")) {
            np.matchIgnoreCase("name=");
            String name = np.getWordRespectCase();
            dataJTable.select(name, true);
        }
        np.matchIgnoreCase(";");
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("set select=all;");
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}
