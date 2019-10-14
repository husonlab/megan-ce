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
package megan.commands;

import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;
import megan.samplesviewer.SamplesViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;

public class ImportMetaDataCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "import metaData=<file> [clearExisting={true|false}];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        final Director dir = getDir();
        final Document doc = dir.getDocument();
        final SamplesViewer samplesViewer = (SamplesViewer) getDir().getViewerByClass(SamplesViewer.class);

        np.matchIgnoreCase("import metaData=");
        String fileName = np.getAbsoluteFileName();

        final boolean clearExisting;
        if (np.peekMatchIgnoreCase("clearExisting")) {
            np.matchIgnoreCase("clearExisting=");
            clearExisting = np.getBoolean();
        } else
            clearExisting = true;

        np.matchIgnoreCase(";");

        if (clearExisting) {
            System.err.println("Cleared existing metadata");
            if (samplesViewer != null) {
                samplesViewer.getSamplesTableView().clear();
            }
            doc.getSampleAttributeTable().clear();
        } else {
            System.err.println("Overwriting metadata");
            if (samplesViewer != null)
                samplesViewer.getSamplesTableView().syncFromViewToDocument();
        }

        final int oldNumberOfSamples = doc.getNumberOfSamples();
        final int oldNumberOfAttributes = doc.getSampleAttributeTable().getNumberOfAttributes();

        doc.getSampleAttributeTable().read(new FileReader(fileName), doc.getSampleNames(), false);
        if (doc.getSampleAttributeTable().getSampleOrder().size() != oldNumberOfSamples) {
            doc.getSampleAttributeTable().setSampleOrder(doc.getSampleNames());
        }

        if (!doc.getSampleAttributeTable().getSampleOrder().equals(doc.getSampleNames())) {
            doc.reorderSamples(doc.getSampleAttributeTable().getSampleOrder());
        }

        doc.setDirty(true);
        if (samplesViewer != null) {
            samplesViewer.getSamplesTableView().syncFromDocumentToView();
        }

        NotificationsInSwing.showInformation(getViewer().getFrame(), "Number of attributes imported: " + (doc.getSampleAttributeTable().getNumberOfAttributes() - oldNumberOfAttributes));
    }

    public void actionPerformed(ActionEvent event) {
        final File lastOpenFile = ProgramProperties.getFile("MetaDataFilePath");
        final File file = ChooseFileDialog.chooseFileToOpen(getViewer().getFrame(), lastOpenFile, new TextFileFilter(".csv"), new TextFileFilter(".csv"), event, "Open metadata mapping file");
        if (file != null && file.length() > 0) {
            int result = JOptionPane.showConfirmDialog(getViewer().getFrame(), "Clear existing metadata?", "Clear existing metadata?", JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon());
            if (result == JOptionPane.CANCEL_OPTION)
                return;
            execute("import metadata='" + file.getPath() + "' clearExisting=" + (result == JOptionPane.YES_OPTION) + ";show window=samplesViewer;");
            ProgramProperties.put("MetaDataFilePath", file.getPath());
        }
    }

    public boolean isApplicable() {
        Director dir = getDir();
        Document doc = dir.getDocument();
        return doc.getNumberOfSamples() > 0;
    }

    public String getName() {
        return "Metadata...";
    }

    public String getAltName() {
        return "Import Metadata...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Import16.gif");
    }

    public String getDescription() {
        return "Import a metadata mapping file (as defined in http://qiime.org/documentation/file_formats.html)";
    }

    public boolean isCritical() {
        return true;
    }
}

