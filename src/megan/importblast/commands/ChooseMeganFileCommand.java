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
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.importblast.ImportBlastDialog;
import megan.main.MeganProperties;
import megan.util.RMAFileFilter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * choose MEGAN file
 * Daniel Huson, 11.2010
 */
public class ChooseMeganFileCommand extends CommandBase implements ICommand {
    public void apply(NexusStreamParser np) throws Exception {
    }

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
        ImportBlastDialog importBlastDialog = (ImportBlastDialog) getParent();
        File lastOpenFile;
        String name = importBlastDialog.getBlastFileName();
        if (name.length() > 0)
            lastOpenFile = new File(Basic.replaceFileSuffix(name, ".rma6"));
        else
            lastOpenFile = new File(ProgramProperties.getFile(MeganProperties.SAVEFILE), "Untitled.rma6");

        File file = ChooseFileDialog.chooseFileToSave(importBlastDialog, lastOpenFile, new RMAFileFilter(), new RMAFileFilter(), event, "Save MEGAN file", ".rma6");

        if (file != null) {
            ProgramProperties.put(MeganProperties.SAVEFILE, file);
            importBlastDialog.setMeganFileName(file.getPath());
            importBlastDialog.getMeganFileNameField().setText(file.getPath());
        }
    }


    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Browse...";
    }

    final public static String ALTNAME = "Browse MEGAN File...";

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getAltName() {
        return ALTNAME;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Choose location to save MEGAN file";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/SaveAs16.gif");
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
