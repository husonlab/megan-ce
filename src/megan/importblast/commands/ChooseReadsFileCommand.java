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
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.FastaFileFilter;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.importblast.ImportBlastDialog;
import megan.main.MeganProperties;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * choose reads file
 * Daniel Huson, 11.2010
 */
public class ChooseReadsFileCommand extends CommandBase implements ICommand {
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
        File lastOpenFile = ProgramProperties.getFile(MeganProperties.READSFILE);
        if (lastOpenFile != null) {
            lastOpenFile = new File(lastOpenFile.getParentFile(),
                    Basic.replaceFileSuffix(lastOpenFile.getName(), ".fna"));
        }
        final FastaFileFilter fastAFileFilter = new FastaFileFilter();
        fastAFileFilter.add("fastq");
        fastAFileFilter.add("fnq");
        fastAFileFilter.add("faq");

        fastAFileFilter.setAllowGZipped(true);
        fastAFileFilter.setAllowZipped(true);

        List<File> files = ChooseFileDialog.chooseFilesToOpen(importBlastDialog, lastOpenFile, fastAFileFilter, fastAFileFilter, event, "Open reads file(s)");

        if (files.size() > 0) {
            ProgramProperties.put(MeganProperties.READSFILE, files.get(0).getPath());
            try {
                for (File file : files) {
                    if (!file.exists())
                        throw new IOException("No such file: " + file);
                    if (!file.canRead())
                        throw new IOException("Cannot read file: " + file);
                }
                importBlastDialog.setReadFileName(Basic.toString(files, "\n"));
                importBlastDialog.getReadFileNameField().setText(Basic.toString(files, "\n"));
            } catch (IOException ex) {
                NotificationsInSwing.showError(getViewer().getFrame(), "Failed to load file: " + ex.getMessage());
            }
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

    final public static String ALTNAME = "Browse Reads File...";

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
        return "Choose READS file(s) (in FastA format)";
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
        return true;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        ImportBlastDialog importBlastDialog = (ImportBlastDialog) getParent();
        return !Objects.requireNonNull(importBlastDialog.getFormatCBox().getSelectedItem()).toString().equalsIgnoreCase("daa");
    }
}
