/*
 * SetTreeDrawerCommand.java Copyright (C) 2021. Daniel H. Huson
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

import jloda.swing.commands.ICommand;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * sets the tree drawer
 * Daniel Huson, 5.2015
 */
public class SetTreeDrawerCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set drawer=");
        String drawerName = np.getWordMatchesIgnoringCase(Basic.toString(ViewerBase.DiagramType.values(), " "));
        np.matchIgnoreCase(";");

        ViewerBase viewer = (ViewerBase) getViewer();
        viewer.setDrawerType(drawerName);

        viewer.updateTree();
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set drawer={" + Basic.toString(ViewerBase.DiagramType.values(), ",") + "};";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {

    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return null;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set the tree drawer";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
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
        return getViewer() instanceof ViewerBase;
    }
}
