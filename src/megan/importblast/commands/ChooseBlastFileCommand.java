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
import jloda.swing.util.BlastFileFilter;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.FastaFileFilter;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.importblast.ImportBlastDialog;
import megan.main.MeganProperties;
import megan.util.LastMAFFileFilter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * choose blast file
 * Daniel Huson, 11.2010
 */
public class ChooseBlastFileCommand extends CommandBase implements ICommand {
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

        final File lastOpenFile = ProgramProperties.getFile(MeganProperties.BLASTFILE);

        BlastFileFilter blastFileFilter = new BlastFileFilter();
        blastFileFilter.add("rdp");
        blastFileFilter.add("out");
        blastFileFilter.add("sam");
        blastFileFilter.add("log");
        blastFileFilter.add("aln");
        blastFileFilter.add("m8");
        blastFileFilter.add("daa");
        blastFileFilter.add(LastMAFFileFilter.getInstance());
        blastFileFilter.setAllowGZipped(true);
        blastFileFilter.setAllowZipped(true);
        java.util.List<File> files = ChooseFileDialog.chooseFilesToOpen(importBlastDialog, lastOpenFile, blastFileFilter, blastFileFilter, event, "Open BLAST (RDP, Silva or SAM) file(s)");

        if (files.size() > 0) {
            importBlastDialog.setBlastFileName(Basic.toString(files, "\n"));
            importBlastDialog.getBlastFileNameField().setText(Basic.toString(files, "\n"));

            final FastaFileFilter fastaFileFilter = new FastaFileFilter();
            fastaFileFilter.add(".fastq");
            fastaFileFilter.add(".fnq");
            fastaFileFilter.add(".fq");

            importBlastDialog.setReadFileName("");
            for (File aFile : files) {
                String fileName = Basic.getAnExistingFileWithGivenExtension(aFile.getPath(), fastaFileFilter.getFileExtensions());
                if (fileName == null)
                    fileName = Basic.getAnExistingFileWithGivenExtension(Basic.getFilePath(ProgramProperties.get(MeganProperties.READSFILE, ""), aFile.getName()), fastaFileFilter.getFileExtensions());
                if (fileName != null)
                    importBlastDialog.addReadFileName(fileName);
            }
            importBlastDialog.getReadFileNameField().setText(importBlastDialog.getReadFileName());

            final String fileName = (files.size() > 1 ? Basic.getCommonPrefix(files.toArray(new File[0]), "out") : files.get(0).getName());

            final File meganFile = makeNewRMAFile(files.get(0).getParentFile(), fileName);

            importBlastDialog.setMeganFileName(meganFile.getPath());
            importBlastDialog.getMeganFileNameField().setText(meganFile.getPath());
            ProgramProperties.put(MeganProperties.BLASTFILE, files.get(0));
        }
    }

    /**
     * make a new MEGAN file name
     *
     * @param directory
     * @param fileName
     * @return
     */
    private File makeNewRMAFile(File directory, String fileName) {
        int count = 0;
        while (true) {
            File meganFile = new File(directory, Basic.replaceFileSuffix(Basic.getFileNameWithoutZipOrGZipSuffix(fileName), (count > 0 ? "-" + count : "") + ".rma6"));
            if (!meganFile.exists())
                return meganFile;
            count++;
        }
    }

    public String getName() {
        return "Browse...";
    }

    final public static String ALTNAME = "Browse BLAST File...";

    public String getAltName() {
        return ALTNAME;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Choose input file(s)";
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
