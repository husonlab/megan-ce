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
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.biom.BIOMImporter;
import megan.core.Director;
import megan.core.Document;
import megan.inspector.InspectorWindow;
import megan.main.MeganProperties;
import megan.util.Biom1FileFilter;
import megan.viewer.MainViewer;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImportBIOMCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "import format=biom file=<fileName> [type={TAXONOMY|KEGG|SEED|UNKNOWN}];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        Director dir = getDir();
        MainViewer viewer = dir.getMainViewer();
        Document doc = dir.getDocument();

        np.matchIgnoreCase("import format=");
        String choice = np.getWordMatchesIgnoringCase("biom");
        if (!choice.equalsIgnoreCase("biom"))
            throw new IOException("Unsupported format: " + choice);

        np.matchIgnoreCase("file=");
        final String fileName = np.getAbsoluteFileName();

        String type;
        if (np.peekMatchIgnoreCase("type")) {
            np.matchIgnoreCase("type=");
            type = np.getWordMatchesIgnoringCase("taxonomy kegg seed unknown");
        } else
            type = "unknown";

        np.matchIgnoreCase(";");

        if (!ProgramProperties.isUseGUI() || doc.neverOpenedReads) {
            doc.neverOpenedReads = false;
            doc.clearReads();

            BIOMImporter.apply(fileName, doc, type);

            if (dir.getViewerByClass(InspectorWindow.class) != null)
                ((InspectorWindow) dir.getViewerByClass(InspectorWindow.class)).clear();
            getDir().getMainViewer().getCollapsedIds().clear();

            doc.getMeganFile().setFileName(Basic.replaceFileSuffix(fileName, ".megan"));
            final String docName = Basic.replaceFileSuffix(Basic.getFileNameWithoutPath(fileName), "");
            if (doc.getSampleNames().size() == 1 && !doc.getSampleNames().get(0).equals(docName)) {
                doc.getDataTable().changeSampleName(0, docName);
            }
            doc.processReadHits();
            doc.setDirty(true);
            viewer.getNodeDrawer().setStyle(doc.getNumberOfSamples() > 1 ? NodeDrawer.Style.PieChart : NodeDrawer.Style.Circle);
            viewer.setDoReInduce(true);
            viewer.setDoReset(true);
        } else {
            Director newDir = Director.newProject();
            newDir.getMainViewer().getFrame().setVisible(true);
            newDir.getMainViewer().setDoReInduce(true);
            newDir.getMainViewer().setDoReset(true);
            newDir.execute("import format=biom file='" + fileName + "' type='" + type + "';", newDir.getMainViewer().getCommandManager());
        }
    }

    public void actionPerformed(ActionEvent event) {
        File lastOpenFile = ProgramProperties.getFile(MeganProperties.CSVFILE);

        List<File> files = ChooseFileDialog.chooseFilesToOpen(getViewer().getFrame(), lastOpenFile, new Biom1FileFilter(), new Biom1FileFilter(), event, "Open BIOM 1.0 file");
        if (files != null && files.size() > 0) {
            final String[] choices = new String[]{"Taxonomy", "KEGG", "SEED", "Unknown"};
            String choice = ProgramProperties.get("BIOMImportType", "Unknown");
            choice = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "Choose data type", "MEGAN choice", JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), choices, choice);
            if (choice != null) {
                ProgramProperties.put("BIOMImportType", choice);

                final StringBuilder buf = new StringBuilder();
                for (File file : files) {
                    buf.append("import format=biom file='").append(file.getPath()).append("' type='").append(choice).append("';");
                }
                execute(buf.toString());
                ProgramProperties.put(MeganProperties.CSVFILE, files.get(0).getPath());
            }
        }
    }

    public boolean isApplicable() {
        return getViewer() != null;
    }

    public String getName() {
        return "BIOM1 Format...";
    }

    public String getAltName() {
        return "Import BIOM1 Format...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Import16.gif");
    }

    public String getDescription() {
        return "Import samples from a table in BIOM 1.0 (JSON) format (see http://biom-format.org/documentation/format_versions/biom-1.0.html)";
    }

    public boolean isCritical() {
        return true;
    }
}

