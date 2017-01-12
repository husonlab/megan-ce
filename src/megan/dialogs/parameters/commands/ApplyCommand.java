/*
 *  Copyright (C) 2017 Daniel H. Huson
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

import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.data.IConnector;
import megan.dialogs.parameters.ParametersDialog;
import megan.util.ReadMagnitudeParser;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * command
 * Daniel Huson, 11.2010
 */
public class ApplyCommand extends CommandBase implements ICommand {
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
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return null;
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        final ParametersDialog parametersDialog = (ParametersDialog) getParent();
        final Director dir = (Director) getDir();

        parametersDialog.setVisible(false);

        if (dir.getDocument().getMeganFile().hasDataConnector()) {
            ReadMagnitudeParser.setEnabled(parametersDialog.isUseMagnitudes());

            int numberOfMatches = 0;
            try {
                final IConnector connector = dir.getDocument().getMeganFile().getDataConnector();
                numberOfMatches = connector.getNumberOfMatches();
            } catch (IOException e) {
                Basic.caught(e);
            }
            if (numberOfMatches > 10000000) {
                int result = JOptionPane.showConfirmDialog(MainViewer.getLastActiveFrame(),
                        String.format("This sample contains %,d matches, processing may take a long time, proceed?", numberOfMatches),
                        "Very large dataset, proceed?", JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION)
                    return;
            }
        }
        parametersDialog.setCanceled(false);
    }

    public static final String NAME = "Apply";

    public String getName() {
        return NAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Rerun the LCA analysis using the set parameters";
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
        ParametersDialog viewer = (ParametersDialog) getParent();
        return viewer != null;
    }
}
