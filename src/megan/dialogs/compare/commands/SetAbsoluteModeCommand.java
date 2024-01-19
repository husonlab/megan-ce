/*
 * SetAbsoluteModeCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.dialogs.compare.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICheckBoxCommand;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;
import megan.dialogs.compare.CompareWindow;
import megan.dialogs.compare.Comparer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * command
 * Daniel Huson, 11.2010
 */
public class SetAbsoluteModeCommand extends CommandBase implements ICheckBoxCommand {
    public boolean isSelected() {
        CompareWindow viewer = (CompareWindow) getParent();
        return viewer != null && viewer.getMode().equals(Comparer.COMPARISON_MODE.ABSOLUTE);
    }


    /**
     * parses the given command and executes it
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("set mode=");
        String mode = np.getLabelRespectCase();
        np.matchIgnoreCase(";");

        CompareWindow viewer = (CompareWindow) getParent();
        viewer.setMode(Comparer.COMPARISON_MODE.valueOfIgnoreCase(mode));
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
		return "set mode={" + StringUtils.toString(Comparer.COMPARISON_MODE.values(), "|") + "};";
    }

    /**
     * action to be performed
     *
	 */
    @Override
    public void actionPerformed(ActionEvent ev) {
        executeImmediately("set mode=" + (Comparer.COMPARISON_MODE.ABSOLUTE) + ";");
        CompareWindow viewer = (CompareWindow) getParent();
        viewer.getCommandManager().updateEnableState();
    }


    final public static String NAME = "Use Absolute Counts";

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return NAME;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Set comparison mode";
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
        CompareWindow viewer = (CompareWindow) getParent();
        return viewer != null;
    }
}
