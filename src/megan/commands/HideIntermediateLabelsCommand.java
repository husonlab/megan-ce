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

import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICheckBoxCommand;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class HideIntermediateLabelsCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        return getViewer() != null && ((ViewerBase) getViewer()).isShowIntermediateLabels();
    }

    public String getSyntax() {
        return "show intermediate=<bool>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("show intermediate=");
        boolean visible = np.getBoolean();
        np.matchIgnoreCase(";");
        ((ViewerBase) getViewer()).setShowIntermediateLabels(visible);
    }

    public void actionPerformed(ActionEvent event) {
        execute("show intermediate=" + (!isSelected()) + ";");
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Show Intermediate Labels";
    }

    public String getDescription() {
        return "Show intermediate labels at nodes of degree 2";
    }

    public boolean isCritical() {
        return true;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Empty16.gif");
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


