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
package megan.commands.export;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.core.Director;
import megan.core.Document;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

public class ExportMetaDataCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "export metaData=<file> [format={metaDataMapping}];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        Director dir = (Director) getDir();
        Document doc = dir.getDocument();

        np.matchIgnoreCase("export metaData=");
        String fileName = np.getAbsoluteFileName();

        String format = "metaDataMapping";
        if (np.peekMatchIgnoreCase("format")) {
            np.matchIgnoreCase("format=");
            format = np.getWordMatchesIgnoringCase("metaDataMapping");
        }

        np.matchIgnoreCase(";");

        Writer w = new FileWriter(fileName);
        doc.getSampleAttributeTable().write(w, false, false);
        w.close();
    }

    public void actionPerformed(ActionEvent event) {
        String name = ((Director) getDir()).getDocument().getMeganFile().getName();
        File lastOpenFile = ProgramProperties.getFile("MetaDataFilePath");
        if (lastOpenFile != null) {
            lastOpenFile = new File(lastOpenFile.getParent(), Basic.replaceFileSuffix(name, "-metadata.txt"));
        } else
            lastOpenFile = new File(Basic.replaceFileSuffix(name, "-metadata.txt"));

        File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new TextFileFilter(), new TextFileFilter(), event, "Export metadata mapping file");
        if (file != null) {
            execute("export metadata='" + file.getPath() + "';");
            ProgramProperties.put("MetaDataFilePath", file.getPath());
        }
    }

    public boolean isApplicable() {
        Director dir = (Director) getDir();
        Document doc = dir.getDocument();
        return doc.getNumberOfSamples() > 0;
    }

    public String getName() {
        return "Metadata...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Export a metadata mapping file (as defined in http://qiime.org/documentation/file_formats.html)";
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}

