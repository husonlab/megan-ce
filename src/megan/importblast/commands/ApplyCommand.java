/*
 * ApplyCommand.java Copyright (C) 2020. Daniel H. Huson
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
package megan.importblast.commands;

import jloda.swing.commands.ICommand;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.parse.NexusStreamParser;
import megan.importblast.ImportBlastDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * apply
 * Daniel Huson, 11.2010, 3.2105
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
     * @param event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        final ImportBlastDialog importBlastDialog = (ImportBlastDialog) getParent();
        try {
            importBlastDialog.apply();
        } catch (CanceledException ex) {
            System.err.println("USER CANCELED");
        } catch (IOException ex) {
            Basic.caught(ex);
        } finally {
            try {
                if (importBlastDialog != null)
                    importBlastDialog.destroyView();
            } catch (CanceledException e) {
                Basic.caught(e);
            }
        }
    }


    public String getName() {
        return "Apply";
    }

    final public static String ALTNAME = "Apply Import BLAST";

    public String getAltName() {
        return ALTNAME;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Process the named BLAST (or other comparison) and READ files and produce a new MEGAN file";
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
        ImportBlastDialog importBlastDialog = (ImportBlastDialog) getParent();

        return importBlastDialog.isAppliable();
    }
}
