/*
 * ZoomCommand.java Copyright (C) 2021. Daniel H. Huson
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
package megan.alignment.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * font size
 * Daniel Huson, 8.2011
 */
public class ZoomCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("expand axis=");
        String axis = np.getWordMatchesIgnoringCase("horizontal vertical both");
        np.matchIgnoreCase("what=");
        String what;
        if (np.peekMatchAnyTokenIgnoreCase("in out reset selection fit"))
            what = np.getWordMatchesIgnoringCase("in out reset selection fit");
        else
            what = "" + (float) np.getDouble();
        np.matchIgnoreCase(";");
        AlignmentViewer viewer = (AlignmentViewer) getViewer();
        viewer.getAlignmentViewerPanel().zoom(axis, what, null);
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "expand axis={horizontal|vertical|both} what={in|out|reset|selection|fit|<number>};";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
    }

    public String getName() {
        return "Zoom";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Zoom";
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
        return true;
    }
}
