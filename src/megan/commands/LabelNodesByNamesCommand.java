/*
 * LabelNodesByNamesCommand.java Copyright (C) 2023 Daniel H. Huson
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
package megan.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class LabelNodesByNamesCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        ViewerBase viewer = (ViewerBase) getViewer();
        return viewer != null && viewer.isNodeLabelNames();
    }

    public String getSyntax() {
        return "nodeLabels [names=<bool>] [ids=<bool>] [assigned=<bool>] [summarized=<bool>];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        ViewerBase viewer = (ViewerBase) getViewer();
        np.matchIgnoreCase("nodeLabels");
        if (np.peekMatchIgnoreCase("names")) {
            np.matchIgnoreCase("names=");
            viewer.setNodeLabelNames(np.getBoolean());
        }
        if (np.peekMatchIgnoreCase("ids")) {
            np.matchIgnoreCase("ids=");
            viewer.setNodeLabelIds(np.getBoolean());
        }
        if (np.peekMatchIgnoreCase("assigned")) {
            np.matchIgnoreCase("assigned=");
            viewer.setNodeLabelAssigned(np.getBoolean());
        }
        if (np.peekMatchIgnoreCase("summarized")) {
            np.matchIgnoreCase("summarized=");
            viewer.setNodeLabelSummarized(np.getBoolean());
        }
        np.matchRespectCase(";");
        viewer.setupNodeLabels(false);
        viewer.repaint();
    }

    public void actionPerformed(ActionEvent event) {
        execute("nodeLabels names=" + (!isSelected()) + ";");
    }


    public String getName() {
        return "Show Names";
    }

    public String getDescription() {
        return "Determine what to label nodes with";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public ImageIcon getIcon() {
        return null;
    }


    public boolean isApplicable() {
        return true;
    }

    public boolean isCritical() {
        return true;
    }
}

