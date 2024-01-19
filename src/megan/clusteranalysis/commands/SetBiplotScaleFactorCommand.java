/*
 * SetBiplotSizeCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.clusteranalysis.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.NumberUtils;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;
import megan.clusteranalysis.ClusterViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * set scale factor
 * Daniel Huson, 1.2023
 */
public class SetBiplotScaleFactorCommand extends CommandBase implements ICommand {

    /**
	 * parses the given command and executes it
	 */
	@Override
	public void apply(NexusStreamParser np) throws Exception {
		final ClusterViewer viewer = getViewer();

		np.matchIgnoreCase("set biPlotScaleFactor=");
		 var number = np.getDouble(0.001, 100.0);
		np.matchIgnoreCase(";");
		viewer.getPcoaTab().setBiPlotScaleFactor(number);
	}

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set biPlotScaleFactor=<num>;";
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
        return "Set BiPlot Scale Factor...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Scale factor for BiPlot arrows";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null; //ResourceManager.getIcon("PC1vPC2_16.gif");
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
	 */
    public void actionPerformed(ActionEvent ev) {
		final var viewer = getViewer();

		var result = JOptionPane.showInputDialog(viewer.getFrame(), "Set BiPlot scale factor: ", StringUtils.removeTrailingZerosAfterDot(viewer.getPcoaTab().getBiPlotScaleFactor()));
		if (result != null && NumberUtils.isDouble(result)) {
			var value = NumberUtils.parseDouble(result);
			if (value <= 0.001 || value > 100.0)
				NotificationsInSwing.showError(viewer.getFrame(), "Input '" + value + "' out of range: 0.001 -- 100");
			else
				executeImmediately("set biPlotScaleFactor=" + value + ";");
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
