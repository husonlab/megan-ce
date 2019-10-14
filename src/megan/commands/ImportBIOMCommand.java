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
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.biom.biom1.BIOM1Importer;
import megan.biom.biom2.Biom2Importer;
import megan.core.Director;
import megan.core.Document;
import megan.inspector.InspectorWindow;
import megan.main.MeganProperties;
import megan.util.BiomFileFilter;
import megan.viewer.MainViewer;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImportBIOMCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "import format=biom file=<fileName> [type={TAXONOMY|KEGG|SEED|UNKNOWN}] [taxonomyIgnorePath={false|true}];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        final Director dir = getDir();
        final MainViewer viewer = dir.getMainViewer();
        final Document doc = dir.getDocument();

        np.matchIgnoreCase("import format=");
        final String choice = np.getWordMatchesIgnoringCase("biom");
        if (!choice.equalsIgnoreCase("biom"))
            throw new IOException("Unsupported format: " + choice);

        np.matchIgnoreCase("file=");
        final String fileName = np.getAbsoluteFileName();

        final String type;
        if (np.peekMatchIgnoreCase("type")) {
            np.matchIgnoreCase("type=");
            type = np.getWordMatchesIgnoringCase("taxonomy kegg seed unknown");
        } else
            type = "";

        final boolean taxonomyIgnorePath;
        if (np.peekMatchIgnoreCase("taxonomyIgnorePath")) {
            np.matchIgnoreCase("taxonomyIgnorePath=");
            taxonomyIgnorePath = np.getBoolean();
        } else
            taxonomyIgnorePath = false;


        np.matchIgnoreCase(";");

        if (!ProgramProperties.isUseGUI() || doc.neverOpenedReads) {
            doc.neverOpenedReads = false;
            doc.clearReads();

            if (BiomFileFilter.isBiom1File(fileName))
                BIOM1Importer.apply(fileName, doc, type, taxonomyIgnorePath);
            else if (BiomFileFilter.isBiom2File(fileName))
                Biom2Importer.apply(fileName, doc, type, taxonomyIgnorePath);

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
            doc.getSampleAttributeTable().setSampleOrder(doc.getSampleNames());
        } else {
            final Director newDir = Director.newProject();
            newDir.getMainViewer().getFrame().setVisible(true);
            newDir.getMainViewer().setDoReInduce(true);
            newDir.getMainViewer().setDoReset(true);
            newDir.execute("import format=biom file='" + fileName + "'" + (!type.equals("") ? " type=" + type + "" : "")
                    + (taxonomyIgnorePath ? " taxonomyIgnorePath=true" : "") + ";", newDir.getMainViewer().getCommandManager());
        }
    }

    public void actionPerformed(ActionEvent event) {
        final File lastOpenFile = ProgramProperties.getFile(MeganProperties.BIOMFILE);

        final List<File> files = ChooseFileDialog.chooseFilesToOpen(getViewer().getFrame(), lastOpenFile, new BiomFileFilter(), new BiomFileFilter(), event, "Open BIOM file(s)");

        if (files.size() > 0) {

            final String[] choices = new String[]{"Taxonomy", "KEGG", "SEED", "Unknown"};
            final String[] taxonomyAssignmentAlgorithm = new String[]{"Match taxonomic path (more conservative)", "Match most specific node (more specific)"};

            String choice = null;
            String algorithm = null;
            boolean taxonomyIgnorePath = false;

            final StringBuilder buf = new StringBuilder();
            for (File file : files) {
                final boolean isBiom1File = BiomFileFilter.isBiom1File(file.getPath());
                if (choice == null && isBiom1File) {
                    choice = ProgramProperties.get("BIOMImportType", "Unknown");
                    choice = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "Choose data type", "MEGAN choice", JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), choices, choice);
                    if (choice != null)
                        ProgramProperties.put("BIOMImportType", choice);
                    else
                        return; // canceled
                }
                if (algorithm == null && !isBiom1File) {
                    algorithm = ProgramProperties.get("BIOMImportTaxonomyAssignment", taxonomyAssignmentAlgorithm[0]);

                    algorithm = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "How to map assignments to taxonomy", "MEGAN choice", JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), taxonomyAssignmentAlgorithm, algorithm);
                    if (algorithm != null)
                        ProgramProperties.put("BIOMImportTaxonomyAssignment", algorithm);
                    else
                        return; // canceled
                    if (algorithm.equals(taxonomyAssignmentAlgorithm[1]))
                        taxonomyIgnorePath = true;
                }

                buf.append("import format=biom file='").append(file.getPath()).append("'");
                if (choice != null && isBiom1File)
                    buf.append(" type=").append(choice);
                if (taxonomyIgnorePath)
                    buf.append(" taxonomyIgnorePath=true");
                buf.append(";");
            }
            execute(buf.toString());
            ProgramProperties.put(MeganProperties.BIOMFILE, files.get(0).getPath());
        }
    }

    public boolean isApplicable() {
        return getViewer() != null;
    }

    public String getName() {
        return "BIOM Format...";
    }

    public String getAltName() {
        return "Import BIOM Format...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Import16.gif");
    }

    public String getDescription() {
        return "Import samples from a table in BIOM 1.0 or BIOM 2.1 format (see http://biom-format.org)";
    }

    public boolean isCritical() {
        return true;
    }
}

