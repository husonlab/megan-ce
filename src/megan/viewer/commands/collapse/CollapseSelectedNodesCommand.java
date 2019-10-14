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

public class CollapseSelectedNodesCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "collapse nodes={selected|top};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        if (((ClassificationViewer) getViewer()).getSelectedNodes().size() > 0)
            ((ClassificationViewer) getViewer()).setPreviousNodeIdsOfInterest(((ClassificationViewer) getViewer()).getSelectedNodeIds());

        np.matchIgnoreCase("collapse nodes=");
        String what = np.getWordMatchesIgnoringCase("selected top");
        if (what.equals("selected"))
            ((ClassificationViewer) getViewer()).collapseSelectedNodes();
        else if (what.equals("top"))
            ((ClassificationViewer) getViewer()).collapseToTop();
        ((ClassificationViewer) getViewer()).getDocument().setDirty(true);
    }

    public void actionPerformed(ActionEvent event) {
        execute("collapse nodes=selected;");
    }

    public boolean isApplicable() {
        return getViewer() != null && ((ClassificationViewer) getViewer()).getSelectedNodes().size() > 0;
    }

    public String getName() {
        return "Collapse";
    }

    public String getDescription() {
        return "Collapse nodes";
    }

    public boolean isCritical() {
        return true;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("CollapseTree16.gif");
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}

