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
package megan.clusteranalysis.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * choose number of triplot vectors
 * Daniel Huson, 4.2015
 */
public class SetTriplotSizeCommand extends CommandBase implements ICommand {

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        final ClusterViewer viewer = getViewer();
        int max = viewer.getDir().getDocument().getSampleAttributeTable().getNumericalAttributes(null).size();

        np.matchIgnoreCase("set triplotSize=");
        int number = np.getInt(0, max);
        np.matchIgnoreCase(";");

        viewer.getPcoaTab().setTriplotSize(number);
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set triplotSize=<num>;";
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
        try {
            return getViewer().isPCoATab() && getViewer().getPcoaTab().getPCoA().getEigenValues() != null;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "TriPlot Size...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set the number of tri-plot vectors to show";
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
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        final ClusterViewer viewer = getViewer();
        int max = viewer.getDir().getDocument().getSampleAttributeTable().getNumericalAttributes(null).size();
        int number = Math.min(max, viewer.getPcoaTab().getTriplotSize());

        String result = JOptionPane.showInputDialog(viewer.getFrame(), "Number of tri-plot vectors (0-" + max + "): ", number);
        if (result != null && Basic.isInteger(result)) {
            final int value = Basic.parseInt(result);
            if (value < 0 || value > max)
                NotificationsInSwing.showError(viewer.getFrame(), "Input '" + value + "' out of range: 0 -- " + max);
            else executeImmediately("set triplotSize=" + value + ";");
        }
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
