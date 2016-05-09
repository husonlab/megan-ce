/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class SetMaxNodeHeightCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "set maxNodeHeight=<number>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set maxNodeHeight=");
        int radius = np.getInt(1, 1000);
        np.matchIgnoreCase(";");
        ViewerBase viewer = (ViewerBase) getViewer();
        viewer.setMaxNodeRadius(radius);
        if (viewer instanceof MainViewer)
            ((MainViewer) viewer).setDoReInduce(true);
    }

    public void actionPerformed(ActionEvent event) {
        String input = JOptionPane.showInputDialog(getViewer().getFrame(), "Enter max node height in pixels", ((ClassificationViewer) getViewer()).getMaxNodeRadius());
        if (input != null && Basic.isInteger(input)) {
            execute("set maxNodeHeight=" + input + ";");
        }
    }

    public boolean isApplicable() {
        return getViewer() instanceof ViewerBase;
    }

    public String getName() {
        return "Set Max Node Height...";
    }

    public String getDescription() {
        return "Set the maximum node height in pixels";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }
}


