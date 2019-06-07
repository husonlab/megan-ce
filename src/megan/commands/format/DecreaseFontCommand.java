/*
 *  Copyright (C) 2019 Daniel H. Huson
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
package megan.commands.format;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * set size
 * Daniel Huson, 4.2011
 */
public class DecreaseFontCommand extends CommandBase implements ICommand {

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Decrease Font Size";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Decrease the font size";
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
        return KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        execute("set fontSize=decrease;");
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
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return null;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        if (getViewer() instanceof ViewerBase)
            return ((ViewerBase) getViewer()).getGraph().getNumberOfNodes() > 0;
        else
            return getViewer() instanceof ClusterViewer && ((ClusterViewer) getViewer()).getGraphView() != null && ((ClusterViewer) getViewer()).getGraphView().getGraph().getNumberOfNodes() > 0;
    }

    /**
     * gets the command needed to undo this command
     *
     * @return undo command
     */
    public String getUndo() {
        return null;
    }
}
