/*
 * SetScaleByLogCommand.java Copyright (C) 2021. Daniel H. Huson
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
 *
 */
package megan.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.util.ScalingType;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class SetScaleByLogCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        ViewerBase viewer = (ViewerBase) getViewer();
        return viewer.getNodeDrawer().getScalingType() == ScalingType.LOG;
    }

    public String getSyntax() {
        return null;
    }

    public void apply(NexusStreamParser np) throws Exception {
    }

    public void actionPerformed(ActionEvent event) {
        execute("set scale=log;");
    }

    public boolean isApplicable() {
        return getViewer() instanceof ViewerBase;
    }

    public String getName() {
        return "Log Scale";
    }

    public String getDescription() {
        return "Show values on log scale";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("LogScale16.gif");
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

