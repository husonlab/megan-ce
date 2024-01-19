/*
 * SortReadsAlphabeticallyCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.inspector.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.parse.NexusStreamParser;
import megan.inspector.InspectorWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * command
 * Daniel Huson, 11.2010
 */
public class SortReadsAlphabeticallyCommand extends CommandBase implements ICheckBoxCommand {

    public boolean isSelected() {
        InspectorWindow inspectorWindow = (InspectorWindow) getViewer();
        return inspectorWindow.isSortReadsAlphabetically();
    }

    /**
     * parses the given command and executes it
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set sortreads=");
        String which = np.getWordMatchesIgnoringCase("alphabetically no");
        np.matchIgnoreCase(";");

        InspectorWindow inspectorWindow = (InspectorWindow) getViewer();

        inspectorWindow.setSortReadsAlphabetically(which.equalsIgnoreCase("alphabetically"));
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set sortreads={alphabetically|no};";
    }

    /**
     * action to be performed
     *
	 */
    @Override
    public void actionPerformed(ActionEvent ev) {
        executeImmediately("set sortreads=" + (isSelected() ? "no" : "alphabetically") + ";");
    }


    public String getName() {
        return "Sort Reads Alphabetically";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Sort reads alphabetically";
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
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
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
        InspectorWindow inspectorWindow = (InspectorWindow) getViewer();

        return inspectorWindow != null;
    }
}
