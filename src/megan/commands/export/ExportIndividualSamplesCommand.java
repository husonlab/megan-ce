/*
 * ExportIndividualSamplesCommand.java Copyright (C) 2019. Daniel H. Huson
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
package megan.commands.export;

import jloda.swing.commands.ICommand;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Document;
import megan.core.MeganFile;
import megan.main.MeganProperties;
import megan.util.MeganFileFilter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Collections;

public class ExportIndividualSamplesCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "extract directory=<target-directory> replace={true|false};";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("extract directory=");
        final String directory = np.getWordFileNamePunctuation();
        final boolean replace;
        if (np.peekMatchIgnoreCase("replace")) {
            np.matchIgnoreCase("replace=");
            replace = np.getBoolean();
        } else
            replace = true;
        np.matchIgnoreCase(";");

        for (String name : getDoc().getSampleNames()) {
            final File file = new File(directory, name + ".megan");
            if (!replace && file.exists()) {
                NotificationsInSwing.showWarning("File exists: " + file + ", won't replace");
            } else {
                try (Writer w = new BufferedWriter(new FileWriter(file))) {
                    Document newDocument = new Document();

                    newDocument.getMeganFile().setFile(file.getPath(), MeganFile.Type.MEGAN_SUMMARY_FILE);
                    newDocument.extractSamples(Collections.singleton(name), getDoc());
                    newDocument.setNumberReads(newDocument.getDataTable().getTotalReads());
                    System.err.println("Number of reads: " + newDocument.getNumberOfReads());
                    newDocument.processReadHits();
                    newDocument.setTopPercent(100);
                    newDocument.setMinScore(0);
                    newDocument.setMaxExpected(10000);
                    newDocument.setMinSupport(1);

                    newDocument.getDataTable().setParameters(newDocument.getParameterString());
                    newDocument.getDataTable().write(w);
                    newDocument.getSampleAttributeTable().write(w, false, true);

                }
            }
        }
    }

    public void actionPerformed(ActionEvent event) {
        if (getDoc().getMeganFile().hasDataConnector()) {
            final File savedFile = ProgramProperties.getFile(MeganProperties.SAVEFILE);
            final File directory = (savedFile != null ? savedFile.getParentFile() : null);
            final File lastOpenFile = Basic.replaceFileSuffix(new File(directory, getDoc().getSampleNames().get(0)), ".megan");
            final File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new MeganFileFilter(), new MeganFileFilter(), event, "Extract MEGAN files", ".megan");

            if (file != null) {
                ProgramProperties.put(MeganProperties.SAVEFILE, file);
                execute("extract directory='" + file.getParent() + "' replace=true;");
            }
        }
    }

    public boolean isApplicable() {
        return getDoc().getNumberOfReads() > 0 && getDoc().getMeganFile().hasDataConnector();
    }

    public String getName() {
        return "All Individual Samples...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Extract and export all contained samples as individual files";
    }

    public boolean isCritical() {
        return true;
    }
}

