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
package megan.dialogs.compare.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.dialogs.compare.CompareWindow;
import megan.main.MeganProperties;
import megan.util.MeganAndRMAFileFilter;
import megan.util.MeganizedDAAFileFilter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * the add files command
 * Daniel Huson, 9.2105
 */
public class AddFilesCommand extends CommandBase implements ICommand {
    /**
     * constructor
     */
    public AddFilesCommand() {
    }

    /**
     * constructor
     *
     * @param dir
     */
    public AddFilesCommand(IDirector dir) {
        setDir(dir);
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return NAME;
    }

    public static final String NAME = "Add Files...";

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Add files for comparison";
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
     * parses the given command and executes it
     *
     * @param np
     * @throws IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("add file=");
        final String fileName = np.getAbsoluteFileName();
        np.matchIgnoreCase(";");
        Basic.checkFileReadableNonEmpty(fileName);
        CompareWindow viewer = (CompareWindow) getParent();
        viewer.addFile(fileName);
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        File lastOpenFile = ProgramProperties.getFile(MeganProperties.MEGANFILE);

        MeganAndRMAFileFilter meganAndRMAFileFilter = new MeganAndRMAFileFilter();
        meganAndRMAFileFilter.setAllowGZipped(true);
        meganAndRMAFileFilter.setAllowZipped(true);
        meganAndRMAFileFilter.add(MeganizedDAAFileFilter.getInstance());
        getDir().notifyLockInput();

        CompareWindow viewer = (CompareWindow) getParent();

        Collection<File> files;
        try {
            files = ChooseFileDialog.chooseFilesToOpen(viewer, lastOpenFile, meganAndRMAFileFilter, meganAndRMAFileFilter, ev, "Add MEGAN file");
        } finally {
            getDir().notifyUnlockInput();
        }

        if (files.size() > 0) {
            StringBuilder buf = new StringBuilder();
            for (File file : files) {
                if (file != null && file.exists() && file.canRead()) {
                    ProgramProperties.put(MeganProperties.MEGANFILE, file.getAbsolutePath());
                    buf.append("add file='").append(file.getPath()).append("';");
                }
            }
            execute(buf.toString());
        }
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
        return true;
    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "add file=<filename>;";
    }
}
