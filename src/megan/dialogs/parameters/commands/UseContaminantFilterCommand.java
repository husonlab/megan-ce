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
package megan.dialogs.parameters.commands;

import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.dialogs.parameters.ParametersDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * list contaminants
 * Daniel Huson, 11.2017
 */
public class UseContaminantFilterCommand extends CommandBase implements ICheckBoxCommand {
    @Override
    public boolean isSelected() {
        return getParent() instanceof ParametersDialog && ((ParametersDialog) getParent()).isUseContaminantsFilter();
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
     * parses and applies the command
     *
     * @param np
     * @throws Exception
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        ((ParametersDialog) getParent()).setUseContaminantsFilter(!isSelected());
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {

    }

    /**
     * /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return NAME;
    }

    final public static String NAME = "Use Contaminant Filter";


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Use contaminant filtering";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/About16.gif");
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
        return getParent() instanceof ParametersDialog && (((ParametersDialog) getParent()).hasContaminants()
                || ((ParametersDialog) getParent()).getContaminantsFileName() != null);
    }
}
