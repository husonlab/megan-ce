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
package megan.viewer.commands.collapse;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class UncollapseSubTreeCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        if (((ClassificationViewer) getViewer()).getSelectedNodes().size() > 0)
            ((ClassificationViewer) getViewer()).setPreviousNodeIdsOfInterest(((ClassificationViewer) getViewer()).getSelectedNodeIds());
        execute("uncollapse nodes=subtree;");
    }

    public boolean isApplicable() {
        return getViewer() != null && ((ClassificationViewer) getViewer()).getSelectedNodes().size() > 0;
    }

    public String getName() {
        return "Uncollapse Subtree";
    }

    public String getDescription() {
        return "Uncollapse whole subtree beneath selected nodes";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("UncollapseSubTree16.gif");
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);
    }

    public boolean isCritical() {
        return true;
    }
}

