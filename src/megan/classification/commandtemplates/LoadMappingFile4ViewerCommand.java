/*
 *  Copyright (C) 2018 Daniel H. Huson
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
package megan.classification.commandtemplates;

import jloda.gui.ChooseFileDialog;
import jloda.gui.commands.CommandBase;
import jloda.gui.commands.ICommand;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.TextFileFilter;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.fx.NotificationsInSwing;
import megan.importblast.ImportBlastDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * loads a mapping file for the given fViewer and mapType
 * Daniel Huson, 3.2014
 */
public class LoadMappingFile4ViewerCommand extends CommandBase implements ICommand {
    final private String cName;
    final private IdMapper.MapType mapType;

    public LoadMappingFile4ViewerCommand(String cName, IdMapper.MapType mapType) {
        this.cName = cName;
        this.mapType = mapType;
    }

    /**
     * commandline syntax
     *
     * @return
     */
    @Override
    public String getSyntax() {
        return null;
    }

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
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        final File lastOpenFile = ProgramProperties.getFile(ClassificationManager.getMapFileKey(cName, mapType));
        getDir().notifyLockInput();
        ImportBlastDialog dialog = (ImportBlastDialog) getParent();
        final String[] suffixes = (mapType == IdMapper.MapType.Accession ? new String[]{"map", "abin", "map.gz"} : new String[]{"map", "bin", "map.gz"});
        final File file = ChooseFileDialog.chooseFileToOpen(dialog, lastOpenFile, new TextFileFilter(suffixes, false),
                new TextFileFilter(suffixes, true), ev, "Open " + mapType + " File");
        getDir().notifyUnlockInput();

        if (file != null) {
            if (file.exists() && file.canRead()) {
                ProgramProperties.put(ClassificationManager.getMapFileKey(cName, mapType), file);
                execute("load mapFile='" + file.getPath() + "' mapType=" + mapType + " cName=" + cName + ";");
            } else
                NotificationsInSwing.showError(getViewer().getFrame(), "Failed to open file: " + file.getPath());
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
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Open16.gif");
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
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public static String getAltName(String cName, IdMapper.MapType mapType) {
        return "Load " + mapType + " mapping file for " + cName;
    }

    public String getAltName() {
        return getAltName(cName, mapType);
    }

    public static String getName(String cName, IdMapper.MapType mapType) {
        return "Load " + mapType + " mapping file";
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return getName(cName, mapType);
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Load a file that maps " + mapType + " ids to " + cName + " ids";
    }
}
