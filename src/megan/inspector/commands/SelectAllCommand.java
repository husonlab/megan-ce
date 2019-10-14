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
package megan.inspector.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.inspector.InspectorWindow;

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
        return "select items={all|none}";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {

        np.matchIgnoreCase("select items=");
        String what = np.getWordMatchesIgnoringCase("all none");
        np.matchRespectCase(";");

        final InspectorWindow inspectorWindow = (InspectorWindow) getViewer();
        switch (what) {
            case "all":
                inspectorWindow.getDataTree().setSelectionInterval(0, inspectorWindow.getDataTree().getRowCount());
                break;
            case "none":
                inspectorWindow.getDataTree().clearSelection();
                break;
        }
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("select items=all;");
    }

    public boolean isApplicable() {
        return getViewer() instanceof InspectorWindow;
    }

    public String getName() {
        return "Select All";
    }

    public String getDescription() {
        return "Select nodes";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Empty16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}
