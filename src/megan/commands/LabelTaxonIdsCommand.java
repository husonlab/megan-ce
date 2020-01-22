/*
 * LabelTaxonIdsCommand.java Copyright (C) 2020. Daniel H. Huson
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
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class LabelTaxonIdsCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        ViewerBase viewer = (ViewerBase) getViewer();
        return viewer != null && viewer.isNodeLabelIds();
    }

    public String getSyntax() {
        return null;
    }

    public void actionPerformed(ActionEvent event) {
        execute("nodeLabels ids=" + (!isSelected()) + ";");
    }

    public String getName() {
        return "Show IDs";
    }

    public String getDescription() {
        return "Display the NCBI ids of taxa";
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public ImageIcon getIcon() {
        return null;
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
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
        return true;
    }
}

