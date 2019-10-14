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
package megan.dialogs.extractor.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.dialogs.extractor.ExtractReadsViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;

/**
 * choose directory
 * Daniel Huson, 11.2010
 */
public class ChooseDirectoryCommand extends CommandBase implements ICommand {
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
        File file = null;
        String fileName = null;
        if (((Director) getDir()).getDocument().getMeganFile().getFileName() != null && !((Director) getDir()).getDocument().getMeganFile().isMeganServerFile())
            fileName = new File(((Director) getDir()).getDocument().getMeganFile().getFileName()).getParent();
        if (ProgramProperties.isMacOS() && (event != null && (event.getModifiers() & ActionEvent.SHIFT_MASK) == 0)) {
            //Use native file dialog on mac
            java.awt.FileDialog dialog = new java.awt.FileDialog(getViewer().getFrame(), "Open output directory", java.awt.FileDialog.LOAD);
            dialog.setFilenameFilter((dir, name) -> true);
            if (fileName != null) {
                dialog.setDirectory(fileName);
                //dialog.setFile(fileName);
            }
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            dialog.setVisible(true);
            System.setProperty("apple.awt.fileDialogForDirectories", "false");

            if (dialog.getFile() != null) {
                file = new File(dialog.getDirectory(), dialog.getFile());
            } else
                return;
        } else {
            JFileChooser chooser = new JFileChooser(fileName);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileName != null)
                chooser.setSelectedFile(new File(fileName));
            chooser.setAcceptAllFileFilterUsed(true);

            int result = chooser.showOpenDialog(getViewer().getFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();
            }
        }
        if (file != null) {
            if (!file.isDirectory())
                file = file.getParentFile();
            ((ExtractReadsViewer) getViewer()).getOutDirectory().setText(file.getPath());
        }
    }

    /**
     * gets the longest common prefix or null
     *
     * @param files
     * @return lcp
     */
    private String getLongestCommonPrefix(File[] files) {
        String first = Basic.getFileBaseName(files[0].getName());
        for (int i = 0; i < first.length(); i++) {
            for (int j = 1; j < files.length; j++) {
                File file = files[j];
                String name = Basic.getFileBaseName(file.getName());
                if (name.length() < i || name.charAt(i) != first.charAt(i)) {
                    if (i > 0)
                        return first.substring(0, i);
                    else
                        return null;
                }
            }
        }
        return first;
    }


    public String getName() {
        return "Browse...";
    }

    final public static String ALTNAME = "Browse Extractor";

    public String getAltName() {
        return ALTNAME;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Browse to determine the output directory";
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

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
