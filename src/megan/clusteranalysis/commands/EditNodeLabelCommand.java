/*
 * EditNodeLabelCommand.java Copyright (C) 2024 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.clusteranalysis.commands;

import jloda.graph.Node;
import jloda.swing.commands.ICommand;
import jloda.swing.graphview.GraphView;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class EditNodeLabelCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) {
    }

    public void actionPerformed(ActionEvent event) {
        final GraphView graphView = getViewer().getGraphView();
        int numToEdit = graphView.getSelectedNodes().size();
        boolean changed = false;
        for (Node v : graphView.getSelectedNodes()) {
            if (numToEdit > 5) {
                int result = JOptionPane.showConfirmDialog(graphView.getFrame(), "There are " + numToEdit +
                        " more selected labels, edit next?", "Question", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.NO_OPTION)
                    break;
            }
            numToEdit--;
            String label = graphView.getLabel(v);
            label = JOptionPane.showInputDialog(graphView.getFrame(), "Edit Node Label:", label);
            if (label != null && !label.equals(graphView.getLabel(v))) {
                if (label.length() > 0)
                    graphView.setLabel(v, label);
                else
                    graphView.setLabel(v, null);
                changed = true;
            }
        }
        if (changed)
            graphView.repaint();
    }

    public boolean isApplicable() {

        final GraphView graphView = getViewer().getGraphView();
        return graphView != null && graphView.getSelectedNodes().size() > 0;
    }

    public String getName() {
        return "Edit Node Label";
    }

    public String getDescription() {
        return "Edit the node label";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Command16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return null;
    }
}

