/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import jloda.gui.ChooseFileDialog;
import jloda.gui.commands.ICommand;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.TextFileFilter;
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
        return "import metaData=<file> [format={metaDataMapping}];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        final Director dir = getDir();
        final Document doc = dir.getDocument();
        final SamplesViewer samplesViewer = (SamplesViewer) getDir().getViewerByClass(SamplesViewer.class);


        np.matchIgnoreCase("import metaData=");
        String fileName = np.getAbsoluteFileName();

        String format = "metaDataMapping";
        if (np.peekMatchIgnoreCase("format")) {
            np.matchIgnoreCase("format=");
            format = np.getWordMatchesIgnoringCase("metaDataMapping");
        }

        np.matchIgnoreCase(";");

        if (false) { // todo: fix this
            if (ProgramProperties.isUseGUI()) {
                int result = JOptionPane.showConfirmDialog(getViewer().getFrame(), "Overwrite existing metadata?", "Overwrite existing metadata?", JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == JOptionPane.CANCEL_OPTION)
                    return;
                else if (result == JOptionPane.YES_OPTION) {
                    System.err.println("Existing metadata cleared");
                    doc.getSampleAttributeTable().clear();
                } else {
                    System.err.println("Overwriting metadata");
                }
            } else
                doc.getSampleAttributeTable().clear();
        }

        if (samplesViewer != null) {
            samplesViewer.getSamplesTable().getDataGrid().save(samplesViewer.getSampleAttributeTable(), null);
        }

        doc.getSampleAttributeTable().read(new FileReader(fileName), doc.getSampleNames(), false);

        doc.setDirty(true);
        if (samplesViewer != null) {
            samplesViewer.getSamplesTable().syncFromDocument();
        }
    }

    public void actionPerformed(ActionEvent event) {
        File lastOpenFile = ProgramProperties.getFile("MetaDataFilePath");

        File file = ChooseFileDialog.chooseFileToOpen(getViewer().getFrame(), lastOpenFile, new TextFileFilter(), new TextFileFilter(), event, "Open metadata mapping file");
        if (file != null && file.length() > 0) {
            execute("import metadata='" + file.getPath() + "';show window=samplesViewer;");
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
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Import16.gif");
    }

    public String getDescription() {
        return "Import a metadata mapping file (as defined in http://qiime.org/documentation/file_formats.html)";
    }

    public boolean isCritical() {
        return true;
    }
}

