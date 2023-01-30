/*
 * ScaleByAssignedCommand.java Copyright (C) 2023 Daniel H. Huson
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

import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ViewerBase;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ScaleByAssignedCommand extends CommandBase implements ICheckBoxCommand {

    public boolean isSelected() {
        ViewerBase viewer = (ViewerBase) getViewer();
        return (viewer != null) && (viewer.getNodeDrawer().getScaleBy() == NodeDrawer.ScaleBy.Assigned);
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set scaleBy=");
        String value = np.getWordMatchesIgnoringCase(StringUtils.toString(NodeDrawer.ScaleBy.values(), " "));
        np.matchIgnoreCase(";");
        ViewerBase viewer = (ViewerBase) getViewer();
        viewer.getNodeDrawer().setScaleBy(value);
        viewer.updateTree();
    }

    public boolean isApplicable() {
        return true;
    }

    public boolean isCritical() {
        return true;
    }

    public String getSyntax() {
		return "set scaleBy={" + StringUtils.toString(NodeDrawer.ScaleBy.values(), "|") + "};";
    }

    public void actionPerformed(ActionEvent event) {
        if (isSelected())
            execute("set scaleBy=" + NodeDrawer.ScaleBy.None + ";");
        else
            execute("set scaleBy=" + NodeDrawer.ScaleBy.Assigned + ";");
    }

    public String getName() {
        return "Scale Nodes By Assigned";
    }

    public String getDescription() {
        return "Scale nodes by number of reads assigned";
    }

    public ImageIcon getIcon() {
        return null;
    }
}

