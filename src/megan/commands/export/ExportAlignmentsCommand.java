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

import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentExporter;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.commands.CommandBase;
import megan.core.ClassificationType;
import megan.core.Document;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;

/**
 * export all alignments for the selected nodes
 */
public class ExportAlignmentsCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "export what=alignment file=<filename> data={" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|") + "}" +
                " classId={number[,number...]|selected} [asConsensus={false|true}]\n" +
                "\t[useEachReadOnlyOnce={true|false}] [useEachReferenceOnlyOnce={true|false}] [includeInsertions={true|false}]\n" +
                "\t[refSeqOnly={false|true}] [contractGaps={false|true}] [translateCDNA={false|true}] [minReads={number}] [minLength={number}] [minCoverage={number}];";
    }

    public void apply(NexusStreamParser np) throws Exception {

        np.matchIgnoreCase("export what=alignment file=");
        String fileName = np.getWordFileNamePunctuation();
        np.matchIgnoreCase("data=");
        String classificationName = np.getWordMatchesRespectingCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));
        np.matchIgnoreCase("classId=");
        boolean useSelected = false;
        Set<Integer> classIds = new HashSet<>();
        if (np.peekMatchIgnoreCase("selected")) {
            np.matchIgnoreCase("selected");
            useSelected = true;
        } else {
            while (true) {
                classIds.add(np.getInt());
                if (!np.peekMatchIgnoreCase(","))
                    break;
                np.matchIgnoreCase(",");
            }
        }
        boolean asConsensus = false;
        if (np.peekMatchIgnoreCase("asConsensus")) {
            np.matchIgnoreCase("asConsensus=");
            asConsensus = np.getBoolean();
            ProgramProperties.put("ExAlignmentAsConsensus", asConsensus);
        }
        boolean useEachReadOnlyOnce = true;
        if (np.peekMatchIgnoreCase("useEachReadOnlyOnce")) {
            np.matchIgnoreCase("useEachReadOnlyOnce=");
            useEachReadOnlyOnce = np.getBoolean();
            ProgramProperties.put("ExAlignmentUseEachReadOnce", useEachReadOnlyOnce);
        }
        boolean useEachReferenceOnlyOnce = false;
        if (np.peekMatchIgnoreCase("useEachReferenceOnlyOnce")) {
            np.matchIgnoreCase("useEachReferenceOnlyOnce=");
            useEachReferenceOnlyOnce = np.getBoolean();
            ProgramProperties.put("ExAlignmentUseEachRefOnce", useEachReferenceOnlyOnce);
        }
        /*
        boolean includeInsertions = true;
        if (np.peekMatchIgnoreCase("includeInsertions")) {
            np.matchIgnoreCase("includeInsertions=");
            includeInsertions = np.getBoolean();
        }
        */
        boolean refSeqOnly = false;
        if (np.peekMatchIgnoreCase("refSeqOnly")) {
            np.matchIgnoreCase("refSeqOnly=");
            refSeqOnly = np.getBoolean();
            ProgramProperties.put("ExAlignmentUseRefSeqOnly", refSeqOnly);
        }
        /*
        boolean contractGaps = false;
        if (np.peekMatchIgnoreCase("contractGaps")) {
            np.matchIgnoreCase("contractGaps=");
            contractGaps = np.getBoolean();
        }
        */

        boolean blastXAsProtein = false;
        if (np.peekMatchIgnoreCase("translateCDNA")) {
            np.matchIgnoreCase("translateCDNA=");
            blastXAsProtein = np.getBoolean();
            ProgramProperties.put("ExAlignmentTranslateCDNA", blastXAsProtein);
        }

        int minReads = 0;
        if (np.peekMatchIgnoreCase("minReads")) {
            np.matchIgnoreCase("minReads=");
            minReads = np.getInt(0, 1000000);
            ProgramProperties.put("ExAlignmentMinReads", minReads);
        }

        int minLength = 0;
        if (np.peekMatchIgnoreCase("minLength")) {
            np.matchIgnoreCase("minLength=");
            minLength = np.getInt(0, 1000000000);
            ProgramProperties.put("ExAlignmentMinLength", minLength);
        }

        double minCoverage = 0;
        if (np.peekMatchIgnoreCase("minCoverage")) {
            np.matchIgnoreCase("minCoverage=");
            minCoverage = np.getDouble(0, 1000000);
            ProgramProperties.put("ExAlignmentMinCoverage", (float) minCoverage);
        }
        np.matchIgnoreCase(";");

        if (useSelected) {
            if (classificationName.equals(ClassificationType.Taxonomy.toString())) {
                MainViewer mainViewer = getDir().getMainViewer();
                classIds.addAll(mainViewer.getSelectedNodeIds());
            } else {
                ClassificationViewer viewer = (ClassificationViewer) getDir().getViewerByClassName(classificationName);
                if (viewer != null) {
                    classIds.addAll(viewer.getSelectedNodeIds());
                }
            }
        }

        int totalReads = 0;
        int totalFiles = 0;

        if (classIds.size() > 0) {
            final Document doc = getDir().getDocument();

            doc.getProgressListener().setTasks("Alignment export", "");

            AlignmentExporter alignmentExporter = new AlignmentExporter(doc, getViewer().getFrame());
            alignmentExporter.setUseEachReferenceOnlyOnce(useEachReferenceOnlyOnce);

            Classification classification = (classificationName.equals(Classification.Taxonomy) ? null : ClassificationManager.get(classificationName, true));

            for (Integer classId : classIds) {
                final String className;
                if (getViewer() instanceof MainViewer) {
                    className = TaxonomyData.getName2IdMap().get(classId);
                } else if (classification != null) {
                    className = classification.getName2IdMap().get(classId);
                } else
                    className = "Unknown";

                alignmentExporter.loadData(classificationName, classId, className, refSeqOnly, doc.getProgressListener());

                Pair<Integer, Integer> numberOfReadsAndFiles = alignmentExporter.exportToFiles(totalFiles, fileName,
                        useEachReadOnlyOnce, blastXAsProtein, asConsensus, minReads, minLength, minCoverage,
                        doc.getProgressListener());
                totalReads += numberOfReadsAndFiles.getFirst();
                totalFiles = numberOfReadsAndFiles.getSecond();
            }

            doc.getProgressListener().close();
            NotificationsInSwing.showInformation(getViewer().getFrame(), "Wrote " + totalReads + " sequences to " + totalFiles + " files");
        }
        System.err.println("Export Alignments: done");
    }

    public void actionPerformed(ActionEvent event) {
        final String classificationName;
        if (getViewer() instanceof MainViewer)
            classificationName = ClassificationType.Taxonomy.toString();
        else if (getViewer() instanceof ClassificationViewer) {
            ClassificationViewer viewer = (ClassificationViewer) getViewer();
            classificationName = viewer.getClassName();
        } else
            return;

        final JDialog dialog = new JDialog(getViewer().getFrame());
        dialog.setModal(true);
        dialog.setLocationRelativeTo(getViewer().getFrame());
        dialog.setSize(500, 400);

        dialog.setTitle("Export Alignments for " + classificationName + " - " + ProgramProperties.getProgramName());

        final JTextField outDirectory = new JTextField();
        outDirectory.setText(ProgramProperties.get("ExAlignmentDir", System.getProperty("user.home")));

        final JTextField outFileTemplate = new JTextField();
        outFileTemplate.setText(ProgramProperties.get("ExAlignmentFile", "alignment-%c-%r.fasta"));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel topMiddlePanel = new JPanel();
        topMiddlePanel.setLayout(new BoxLayout(topMiddlePanel, BoxLayout.Y_AXIS));

        JPanel middlePanel = new JPanel();
        middlePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Output files"));

        middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.Y_AXIS));
        JPanel m1 = new JPanel();
        m1.setLayout(new BorderLayout());
        m1.add(new JLabel("Directory:"), BorderLayout.WEST);
        m1.add(outDirectory, BorderLayout.CENTER);
        outDirectory.setToolTipText("Set directory for files to be written to");
        m1.add(new JButton(new AbstractAction("Browse...") {
            public void actionPerformed(ActionEvent actionEvent) {
                File file = chooseDirectory(actionEvent, outDirectory.getText());
                if (file != null)
                    outDirectory.setText(file.getPath());
            }
        }), BorderLayout.EAST);
        middlePanel.add(m1);

        JPanel m2 = new JPanel();
        m2.setLayout(new BorderLayout());
        m2.add(new JLabel("File name:"), BorderLayout.WEST);
        m2.add(outFileTemplate, BorderLayout.CENTER);
        outFileTemplate.setToolTipText("Set name of file to save to. A %c is replaced by node name, %r by reference name and %n by file number.");
        middlePanel.add(m2);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));

        final JCheckBox useEachReadOnlyOnce = new JCheckBox(new AbstractAction("Use Each Read Only Once") {
            public void actionPerformed(ActionEvent actionEvent) {
            }
        });
        useEachReadOnlyOnce.setSelected(ProgramProperties.get("ExAlignmentUseEachReadOnce", true));
        useEachReadOnlyOnce.setToolTipText("Allow each read only to occur in one alignment, the deepest one containing it");
        buttons.add(useEachReadOnlyOnce);

        final JCheckBox useEachReferenceOnlyOnce = new JCheckBox(new AbstractAction("Use Each Reference Only Once") {
            public void actionPerformed(ActionEvent actionEvent) {
            }
        });
        useEachReferenceOnlyOnce.setSelected(ProgramProperties.get("ExAlignmentUseEachRefOnce", true));
        useEachReferenceOnlyOnce.setToolTipText("Allow each reference sequence to appear only once (and not for multiple nodes)");
        buttons.add(useEachReferenceOnlyOnce);

        /*
        final JCheckBox includeInsertions = new JCheckBox(new AbstractAction("Include Insertions") {
            public void actionPerformed(ActionEvent actionEvent) {
            }
        });
        includeInsertions.setSelected(true);
        includeInsertions.setToolTipText("Include insertions into the reference sequence in alignment");
        buttons.add(includeInsertions);
        */

        final JCheckBox refSeqOnly = new JCheckBox(new AbstractAction("Use Only Matches to RefSeqs") {
            public void actionPerformed(ActionEvent actionEvent) {
            }
        });
        refSeqOnly.setSelected(ProgramProperties.get("ExAlignmentUseRefSeqOnly", false));
        refSeqOnly.setToolTipText("Only alignment to reference sequences that have a refSeq id");
        buttons.add(refSeqOnly);

        /*
        final JCheckBox contractGaps = new JCheckBox(new AbstractAction("Contract Gaps") {
            public void actionPerformed(ActionEvent actionEvent) {
            }
        });
        contractGaps.setSelected(false);
        contractGaps.setToolTipText("Contract runs of gaps and replace by length in square brackets");
        buttons.add(contractGaps);
         */
        final JCheckBox cDNAAsProteins = new JCheckBox(new AbstractAction("Translate cDNA as proteins") {
            public void actionPerformed(ActionEvent actionEvent) {
            }
        });
        cDNAAsProteins.setSelected(ProgramProperties.get("ExAlignmentTranslateCDNA", false));
        cDNAAsProteins.setToolTipText("Translate cDNA sequences to proteins");
        buttons.add(cDNAAsProteins);

        final JCheckBox asConsensus = new JCheckBox(new AbstractAction("As Consensus") {
            public void actionPerformed(ActionEvent actionEvent) {
            }
        });
        asConsensus.setSelected(ProgramProperties.get("ExAlignmentAsConsensus", false));
        asConsensus.setToolTipText("Save consensus ");
        buttons.add(asConsensus);

        buttons.add(Box.createVerticalStrut(20));

        final JPanel numbersPanel = new JPanel();
        numbersPanel.setLayout(new GridLayout(3, 4));
        buttons.add(numbersPanel);
        final JTextField minReadsField = new JTextField(6);
        minReadsField.setMaximumSize(new Dimension(100, 20));
        minReadsField.setText("" + ProgramProperties.get("ExAlignmentMinReads", 1));

        numbersPanel.add(newSingleLine(Box.createHorizontalGlue(), new JLabel("Minimum reads: ")));
        numbersPanel.add(newSingleLine(minReadsField, Box.createHorizontalGlue()));
        minReadsField.setToolTipText("Discard all alignments that do not have the specify minimum number of reads");

        final JTextField minLengthField = new JTextField(6);
        minLengthField.setMaximumSize(new Dimension(100, 20));
        minLengthField.setText("" + ProgramProperties.get("ExAlignmentMinLength", 1));
        numbersPanel.add(newSingleLine(Box.createHorizontalGlue(), new JLabel("Minimum length: ")));
        numbersPanel.add(newSingleLine(minLengthField, Box.createHorizontalGlue()));
        minLengthField.setToolTipText("Discard all alignments that do not have the specify minimum length");

        final JTextField minCoverageField = new JTextField(6);
        minCoverageField.setMaximumSize(new Dimension(100, 20));
        minCoverageField.setText("" + (float) (ProgramProperties.get("ExAlignmentMinCoverage", 1.0)));
        numbersPanel.add(newSingleLine(Box.createHorizontalGlue(), new JLabel("Minimum Coverage: ")));
        numbersPanel.add(newSingleLine(minCoverageField, Box.createHorizontalGlue()));
        minCoverageField.setToolTipText("Discard all alignments that do not have the specify minimum coverage");

        middlePanel.add(buttons);

        topMiddlePanel.add(middlePanel);

        mainPanel.add(topMiddlePanel, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        bottomPanel.setLayout(new BorderLayout());

        JPanel b1 = new JPanel();

        b1.add(new JButton(new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent actionEvent) {
                dialog.setVisible(false);
            }
        }));

        b1.setLayout(new BoxLayout(b1, BoxLayout.X_AXIS));
        JButton applyButton = new JButton(new AbstractAction("Apply") {
            public void actionPerformed(ActionEvent actionEvent) {
                dialog.setVisible(false);

                String fileName = (new File(outDirectory.getText().trim(), outFileTemplate.getText().trim())).getPath();

                if (fileName.length() > 0) {
                    String command = "export what=alignment file='" + fileName + "'";
                    command += " data=" + classificationName;
                    command += " classId=selected";
                    command += " asConsensus=" + asConsensus.isSelected();
                    command += " useEachReadOnlyOnce=" + useEachReadOnlyOnce.isSelected();
                    command += " useEachReferenceOnlyOnce=" + useEachReferenceOnlyOnce.isSelected();
                    // command += " includeInsertions=" + includeInsertions.isSelected();
                    command += " refSeqOnly=" + refSeqOnly.isSelected();
                    // command += " contractGaps=" + contractGaps.isSelected();
                    command += " translateCDNA=" + cDNAAsProteins.isSelected();
                    {
                        final String text = minReadsField.getText();
                        if (Basic.isInteger(text) && Basic.parseInt(text) > 0)
                            command += " minReads=" + text;
                    }
                    {
                        final String text = minLengthField.getText();
                        if (Basic.isInteger(text) && Basic.parseInt(text) > 0)
                            command += " minLength=" + text;
                    }
                    {
                        final String text = minCoverageField.getText();
                        if (Basic.isDouble(text) && Basic.parseDouble(text) > 0)
                            command += " minCoverage=" + text;
                    }
                    command += ";";
                    ProgramProperties.put("ExAlignmentDir", outDirectory.getText().trim());
                    ProgramProperties.put("ExAlignmentFile", outFileTemplate.getText().trim());
                    execute(command);
                }
            }
        });
        b1.add(applyButton);
        dialog.getRootPane().setDefaultButton(applyButton);

        bottomPanel.add(b1, BorderLayout.EAST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(mainPanel, BorderLayout.CENTER);
        dialog.validate();

        dialog.setVisible(true);
    }

    private static JPanel newSingleLine(Component left, Component right) {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(left);
        panel.add(right);
        return panel;
    }

    /**
     * choose the directory for export of files
     *
     * @param event
     * @param fileName
     * @return directory
     */
    private File chooseDirectory(ActionEvent event, String fileName) {
        File file = null;
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
            }
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
        }
        return file;
    }


    public boolean isApplicable() {
        return getViewer() instanceof ViewerBase && getDir().getDocument().getMeganFile().hasDataConnector() && ((ViewerBase) getViewer()).getSelectedNodes().size() > 0;
    }

    private static final String NAME = "Alignments...";

    public String getName() {
        return NAME;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

    public String getDescription() {
        return "Calculate and export alignments for all selected leaves";
    }

    public boolean isCritical() {
        return true;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }
}

