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

import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.clipboard.ClipboardBase;
import megan.core.Director;
import megan.inspector.InspectorWindow;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;

public class ListAsTextCommand extends ClipboardBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        InspectorWindow inspectorWindow = (InspectorWindow) getViewer();
        JTree dataTree = inspectorWindow.getDataTree();

        StringBuilder builder = new StringBuilder();

        TreePath[] selectedPaths = dataTree.getSelectionPaths();
        if (selectedPaths != null) {
            for (TreePath selectedPath : selectedPaths) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                builder.append(node.toString()).append("\n");
            }
        }
        if (builder.toString().length() > 0) {
            Director.showMessageWindow();
            System.out.println("\n" + builder.toString());
        }
    }

    public boolean isApplicable() {
        InspectorWindow inspectorWindow = (InspectorWindow) getViewer();
        return inspectorWindow != null && inspectorWindow.hasSelectedNodes();
    }

    private static final String NAME = "As Text...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "List as text";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/History16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}

