/*
 *  Copyright (C) 2018 Daniel H. Huson
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
package megan.commands;

import jloda.graph.NodeSet;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ShowLabelsCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "show labels=selected;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        ViewerBase viewer = (ViewerBase) getViewer();
        NodeSet selected = viewer.getSelectedNodes();
        if (selected.size() == 0)
            viewer.showNodeLabels(true);
        else
            viewer.showLabels(selected, true);
        viewer.repaint();
    }

    public void actionPerformed(ActionEvent event) {
        execute(getSyntax());
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Node Labels On";
    }

    public String getDescription() {
        return "Show labels for selected nodes";
    }

    public ImageIcon getIcon() {
        return null;
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

