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
package megan.dialogs.profile.commands;

import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICheckBoxCommand;
import jloda.util.parse.NexusStreamParser;
import megan.core.Document;
import megan.dialogs.profile.TaxonomicProfileDialog;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * command
 * Daniel Huson, 8.2014
 */
public class UseReadSpreadingMethodCommand extends CommandBase implements ICheckBoxCommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase(getSyntax());
        TaxonomicProfileDialog viewer = (TaxonomicProfileDialog) getParent();
        viewer.setMethod(TaxonomicProfileDialog.ProfileMethod.ReadSpreading);
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "set method=" + TaxonomicProfileDialog.ProfileMethod.ReadSpreading + ";";
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        executeImmediately(getSyntax());
    }

    public static final String NAME = "Read-Spreading Method";

    public String getName() {
        return NAME;
    }


    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Use the read-spreading profile method";
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
        final Document doc = ((MainViewer) getDir().getMainViewer()).getDocument();
        return doc.getNumberOfReads() > 0 && doc.getMeganFile().hasDataConnector();
    }


    @Override
    public boolean isSelected() {
        TaxonomicProfileDialog viewer = (TaxonomicProfileDialog) getViewer();
        return viewer != null && viewer.getMethod() != null && viewer.getMethod() == TaxonomicProfileDialog.ProfileMethod.ReadSpreading;
    }

}
