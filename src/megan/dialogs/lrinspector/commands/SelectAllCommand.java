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
package megan.dialogs.lrinspector.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.dialogs.lrinspector.LRInspectorViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * * selection command
 * * Daniel Huson, 4.2017
 */
public class SelectAllCommand extends CommandBase implements ICommand {
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("select what=");
        String what = np.getWordMatchesIgnoringCase(" all none compatible invert");
        np.matchIgnoreCase(";");

        final LRInspectorViewer viewer = (LRInspectorViewer) getViewer();
        if (viewer.getController() != null) {
            switch (what) {
                case "all":
                    viewer.selectAll();
                    break;
                case "none":
                    viewer.selectNone();
                    break;
                case "invert":
                    viewer.invertSelectionAlignments();
                    break;
                case "compatible":
                    viewer.selectAllCompatible(true);
                    break;
            }
        }
    }

    public String getSyntax() {
        return "select what={all|none|invert|compatible};";
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("select what=all;");
    }

    public boolean isApplicable() {
        return true;
    }


    public String getName() {
        return "Select All";
    }

    public String getDescription() {
        return "Select";
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
