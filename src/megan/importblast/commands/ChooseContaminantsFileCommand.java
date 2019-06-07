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
package megan.importblast.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.importblast.ImportBlastDialog;
import megan.main.MeganProperties;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * choose command
 * Daniel Huson, 11.2017
 */
public class ChooseContaminantsFileCommand extends CommandBase implements ICommand {
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
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        if (getViewer() instanceof ImportBlastDialog) {
            File lastOpenFile = ProgramProperties.getFile(MeganProperties.CONTAMINANT_FILE);

            getDir().notifyLockInput();
            File file = ChooseFileDialog.chooseFileToOpen(getViewer().getFrame(), lastOpenFile, new TextFileFilter(), new TextFileFilter(), ev, "Open Contaminants File");
            getDir().notifyUnlockInput();

            if (file != null && file.exists() && file.canRead()) {
                ProgramProperties.put(MeganProperties.CONTAMINANT_FILE, file.getAbsolutePath());
                ((ImportBlastDialog) getViewer()).setContaminantsFileName(file.getPath());
                ((ImportBlastDialog) getViewer()).setUseContaminantsFilter(true);
                getCommandManager().updateEnableState(UseContaminantsFilterCommand.NAME);
                getCommandManager().updateEnableState(ListContaminantsCommand.NAME);
            }
        }
    }

    final public static String NAME = "Load Contaminants File...";

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return NAME;
    }


    final public static String DESCRIPTION = "Loads a list of contaminant taxon names or Ids from a file (one per line)";

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Open16.gif");
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
        return getViewer() instanceof ImportBlastDialog;
    }
}
