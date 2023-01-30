/*
 * DrawMetersCommand.java Copyright (C) 2023 Daniel H. Huson
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
package megan.commands.compare;

import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.viewer.ViewerBase;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class DrawMetersCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        ViewerBase viewer = (ViewerBase) getViewer();
        return viewer != null && viewer.getNodeDrawer().getStyle() == NodeDrawer.Style.BarChart;
    }

    public String getSyntax() {
        return null;
    }

    public void actionPerformed(ActionEvent event) {
        execute("set nodeDrawer=" + NodeDrawer.Style.BarChart + ";");
    }

    public String getName() {
        return "Draw Bars";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Meters16.gif");
    }

    public String getDescription() {
        return "Draw nodes as bars";
    }


    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return getDoc().getNumberOfReads() > 0;
    }

    /**
     * parses the given command and executes it
     */
    public void apply(NexusStreamParser np) {
    }
}

