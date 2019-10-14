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
package megan.clusteranalysis.commands;

import jloda.graph.Node;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.commands.CommandBase;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

public class CopyNodeLabelCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        ClusterViewer viewer = (ClusterViewer) getViewer();

        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (Node v : viewer.getGraphView().getSelectedNodes()) {
            String label = viewer.getGraphView().getLabel(v);
            if (label != null) {
                if (first)
                    first = false;
                else
                    buf.append(" ");
                buf.append(label);
            }
        }
        if (buf.toString().length() > 0) {
            StringSelection selection = new StringSelection(buf.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        }
    }

    public boolean isApplicable() {
        return ((ClusterViewer) getViewer()).getGraphView() != null && ((ClusterViewer) getViewer()).getGraphView().getSelectedNodes().size() > 0;
    }

    public String getName() {
        return "Copy Node Label";
    }

    public String getDescription() {
        return "Copy the node label";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Copy16.gif");
    }

    public boolean isCritical() {
        return false;
    }
}

