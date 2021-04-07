/*
 * AutoLabelLayoutCommand.java Copyright (C) 2021. Daniel H. Huson
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
package megan.viewer.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class AutoLabelLayoutCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        return getViewer() != null && ((ClassificationViewer) getViewer()).getAutoLayoutLabels();
    }


    public String getSyntax() {
        return "set autoLayoutLabels={true|false};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set autoLayoutLabels=");
        ((ClassificationViewer) getViewer()).setAutoLayoutLabels(np.getBoolean());
        np.matchIgnoreCase(";");
    }

    public void actionPerformed(ActionEvent event) {
        execute("set autoLayoutLabels=" + (!isSelected()) + ";");
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Layout Labels";
    }

    public String getDescription() {
        return "Layout labels";
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

