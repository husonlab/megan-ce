/*
 * ChooseFilesCommand.java Copyright (C) 2022 Daniel H. Huson
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
package megan.dialogs.reanalyze.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.ProgramProperties;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;
import megan.importblast.ImportBlastDialog;
import megan.importblast.commands.CommandBase;
import megan.main.MeganProperties;
import megan.util.MeganAndRMAFileFilter;
import megan.util.MeganizedDAAFileFilter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * choose files to reanalyze
 * Daniel Huson, 12.2019
 */
public class ChooseFilesCommand extends CommandBase implements ICommand {
    /**
     * parses the given command and executes it
     *
     * @param np
     */
    @Override
    public void apply(NexusStreamParser np) {
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

        final File lastOpenFile = ProgramProperties.getFile(MeganProperties.BLASTFILE);
        MeganAndRMAFileFilter meganRmaDaaFileFilter = new MeganAndRMAFileFilter();
        meganRmaDaaFileFilter.setAllowGZipped(true);
        meganRmaDaaFileFilter.setAllowZipped(true);
        meganRmaDaaFileFilter.add(MeganizedDAAFileFilter.getInstance());
        java.util.List<File> files = ChooseFileDialog.chooseFilesToOpen(importBlastDialog, lastOpenFile, meganRmaDaaFileFilter, meganRmaDaaFileFilter, event, "Open meganized DAA and RMA file(s)");

        if (files.size() > 0) {
			importBlastDialog.setBlastFileName(StringUtils.toString(files, "\n"));
			importBlastDialog.getBlastFileNameField().setText(StringUtils.toString(files, "\n"));
			ProgramProperties.put(MeganProperties.BLASTFILE, files.get(0));
		}
    }

    public String getName() {
        return "Browse...";
    }

    final public static String ALTNAME = "Browse File...";

    public String getAltName() {
        return ALTNAME;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Choose input file(s) to reanalyze";
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
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return false;
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
