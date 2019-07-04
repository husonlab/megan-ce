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
package megan.viewer.commands;

import jloda.graph.Node;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class EditNodeLabelCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        int numToEdit = ((ClassificationViewer) getViewer()).getSelectedNodes().size();
        boolean changed = false;
        for (Node v : ((ClassificationViewer) getViewer()).getSelectedNodes()) {
            if (numToEdit > 5) {
                int result = JOptionPane.showConfirmDialog(getViewer().getFrame(), "There are " + numToEdit +
                                " more selected labels, edit next?", "Question", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon());
                if (result == JOptionPane.NO_OPTION)
                    break;
            }
            numToEdit--;
            String label = ((ClassificationViewer) getViewer()).getLabel(v);
            label = JOptionPane.showInputDialog(getViewer().getFrame(), "Edit Node Label:", label);
            if (label != null && !label.equals(((ClassificationViewer) getViewer()).getLabel(v))) {
                if (label.length() > 0)
                    ((ClassificationViewer) getViewer()).setLabel(v, label);
                else
                    ((ClassificationViewer) getViewer()).setLabel(v, null);
                changed = true;
            }
        }
        if (changed)
            ((ClassificationViewer) getViewer()).repaint();
    }

    public boolean isApplicable() {
        return getViewer() != null && ((ClassificationViewer) getViewer()).getSelectedNodes().size() > 0;
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

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }
}

