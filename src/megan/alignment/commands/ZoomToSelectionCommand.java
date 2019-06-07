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
package megan.alignment.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;
import megan.commands.CommandBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ZoomToSelectionCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("expand axis=both what=selection;");
    }

    public boolean isApplicable() {
        AlignmentViewer viewer = (AlignmentViewer) getViewer();
        return viewer.getSelectedBlock().isSelected();
    }

    final static public String NAME = "Zoom To Selection";

    public String getName() {
        return "Zoom To Selection";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("ZoomToSelection16.gif");
    }

    public String getDescription() {
        return "Zoom to selection";
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    public boolean isCritical() {
        return true;
    }
}

