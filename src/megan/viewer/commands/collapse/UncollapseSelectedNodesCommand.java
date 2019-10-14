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

public class UncollapseSelectedNodesCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "uncollapse nodes={selected|all|subtree};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("uncollapse nodes=");
        String what = np.getWordMatchesIgnoringCase("all selected subtree");
        np.matchIgnoreCase(";");

        if (what.equalsIgnoreCase("selected"))
            ((ClassificationViewer) getViewer()).uncollapseSelectedNodes(false);
        else if (what.equalsIgnoreCase("subtree"))
            ((ClassificationViewer) getViewer()).uncollapseSelectedNodes(true);
        else if (what.equalsIgnoreCase("all"))
            ((ClassificationViewer) getViewer()).uncollapseAll();

        ((ClassificationViewer) getViewer()).getDocument().setDirty(true);
    }

    public void actionPerformed(ActionEvent event) {
        if (((ClassificationViewer) getViewer()).getSelectedNodes().size() > 0)
            ((ClassificationViewer) getViewer()).setPreviousNodeIdsOfInterest(((ClassificationViewer) getViewer()).getSelectedNodeIds());
        execute("uncollapse nodes=selected;");
    }

    public boolean isApplicable() {
        return getViewer() != null && ((ClassificationViewer) getViewer()).getSelectedNodes().size() > 0;
    }

    public String getName() {
        return "Uncollapse";
    }

    public String getDescription() {
        return "Uncollapse selected nodes";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("UncollapseTree16.gif");
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    public boolean isCritical() {
        return true;
    }
}

