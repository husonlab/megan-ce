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
import jloda.swing.window.NotificationsInSwing;
import jloda.util.*;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.export.ExportAlignedReads2GFF3Format;
import megan.dialogs.lrinspector.LRInspectorViewer;
import megan.main.Version;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * export reads in GFF format
 * Daniel Huson, 3.2017
 */
public class ExportReadsToGFFCommand extends CommandBase implements ICommand {

    public String getSyntax() {
        return "export what=GFF file=<file-name> [classification={all|" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|") + "] [excludeIncompatible={false|true}] [excludeDominated={true|false}]";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export what=GFF file=");
        final String fileName = np.getWordFileNamePunctuation();
        final String classification;
        if (np.peekMatchIgnoreCase("classification")) {
            np.matchIgnoreCase("classification=");
            classification = np.getWordMatchesIgnoringCase("all " + Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));
        } else
            classification = "all";

        final boolean excludeIncompatible;
        if (np.peekMatchIgnoreCase("excludeIncompatible")) {
            np.matchIgnoreCase("excludeIncompatible=");
            excludeIncompatible = np.getBoolean();
        } else
            excludeIncompatible = false;
        final boolean excludeDominated;
        if (np.peekMatchIgnoreCase("excludeDominated")) {
            np.matchIgnoreCase("excludeDominated=");
            excludeDominated = np.getBoolean();
        } else
            excludeDominated = true;

        np.matchIgnoreCase(";");

        if (getViewer() instanceof ClassificationViewer) {
            final ClassificationViewer viewer = (ClassificationViewer) getViewer();
            final Document doc = viewer.getDocument();
            final Pair<Long, Long> counts = ExportAlignedReads2GFF3Format.apply(viewer, new File(fileName), classification, excludeIncompatible, excludeDominated, doc.getProgressListener());
            NotificationsInSwing.showInformation(viewer.getFrame(), "Number of reads exported: " + counts.getFirst() + ", alignments exported: " + counts.getSecond());
        } else if (getViewer() instanceof LRInspectorViewer) {
            final LRInspectorViewer viewer = (LRInspectorViewer) getViewer();
            final Document doc = viewer.getDir().getDocument();
            final Pair<Long, Long> counts = ExportAlignedReads2GFF3Format.apply(viewer, new File(fileName), classification, excludeIncompatible, excludeDominated, doc.getProgressListener());
            NotificationsInSwing.showInformation(viewer.getFrame(), "Number of reads exported: " + counts.getFirst() + ", alignments exported: " + counts.getSecond());
        }
    }

    public void actionPerformed(ActionEvent event) {

        final boolean canExport;
        final boolean canExcludeIncompatible;
        {
            if (getViewer() instanceof MainViewer) {
                canExcludeIncompatible = true;
                canExport = (((MainViewer) getViewer()).getNumberSelectedNodes() > 0);
            } else if (getViewer() instanceof ClassificationViewer) {
                canExcludeIncompatible = false;
                canExport = (((ClassificationViewer) getViewer()).getNumberSelectedNodes() > 0);
            } else if (getViewer() instanceof LRInspectorViewer) {
                final LRInspectorViewer viewer = (LRInspectorViewer) getViewer();
                canExcludeIncompatible = viewer.getClassificationName().equals(Classification.Taxonomy) && viewer.someSelectedItemHasTaxonLabelsShowing();
                canExport = viewer.someSelectedItemHasAnyLabelsShowing();
            } else {
                canExcludeIncompatible = false;
                canExport = false;
            }
        }

        final Triplet<Boolean, Boolean, String> options = getOptions(getViewer().getFrame(), canExport, canExcludeIncompatible);
        if (options != null) {

            String name = Basic.replaceFileSuffix(((Director) getDir()).getDocument().getTitle(), "-" + ExportAlignedReads2GFF3Format.getShortName(options.getThird()) + ".gff");
            String lastGFFFile = ProgramProperties.get("lastGFFFile", "");
            File lastOpenFile = new File((new File(lastGFFFile)).getParent(), name);

            final File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new TextFileFilter(".gff"), new TextFileFilter(".gff"), event, "Save read annotations to file", ".gff");

            if (file != null) {
                ProgramProperties.put("lastGFFFile", file.getPath());
                execute("export what=GFF file='" + file.getPath() + "'" + (options.getThird() != null ? " classification=" + options.getThird() : "") +
                        (options.getFirst() ? " excludeIncompatible=true" : "") + (options.getSecond() ? " excludeDominated=true" : "") + ";");
            }
        }
    }

    /**
     * gets options from user
     *
     * @param frame
     * @return options
     */
    private Triplet<Boolean, Boolean, String> getOptions(JFrame frame, boolean canExport, boolean canExcludeIncompatible) {
        final JDialog dialog = new JDialog();
        {
            dialog.setModal(true);
            dialog.setTitle("Export in GFF3 Format - " + Version.NAME);
            dialog.setIconImages(ProgramProperties.getProgramIconImages());
            dialog.setLocationRelativeTo(frame);
            dialog.setSize(500, 160);
            dialog.getContentPane().setLayout(new BorderLayout());
        }
        final JPanel topPane = new JPanel();
        {
            topPane.setLayout(new BoxLayout(topPane, BoxLayout.X_AXIS));
            topPane.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            topPane.add(new JLabel("Select export options:"));
            dialog.getContentPane().add(topPane, BorderLayout.NORTH);
        }

        final JPanel middlePanel = new JPanel();
        {
            middlePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4),
                    BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(4, 4, 4, 4))));
            middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.Y_AXIS));
            dialog.getContentPane().add(middlePanel, BorderLayout.CENTER);
        }

        final JComboBox<String> classificationCBox = new JComboBox<>();
        {
            classificationCBox.setEditable(false);
            classificationCBox.addItem("All");
            for (String classification : ClassificationManager.getAllSupportedClassifications())
                classificationCBox.addItem(classification);
            classificationCBox.setSelectedItem(ProgramProperties.get("GFFExportClassification", Classification.Taxonomy));

            final JPanel aLine = new JPanel();
            aLine.setLayout(new BoxLayout(aLine, BoxLayout.X_AXIS));
            aLine.setMaximumSize(new Dimension(256, 60));
            aLine.add(new JLabel("Classification:"));
            aLine.add(classificationCBox);
            classificationCBox.setToolTipText("Choose classification to export. Use 'all' to export all alignments.");
            middlePanel.add(aLine);
        }

        final JCheckBox excludeIncompatible = new JCheckBox("Exclude taxonomically incompatible genes");
        final JCheckBox excludeDominated = new JCheckBox("Exclude dominated genes");
        {
            excludeIncompatible.setToolTipText("Don't report a gene if its taxon is incompatible with the taxon assigned to the read.");
            excludeIncompatible.setEnabled(canExport && canExcludeIncompatible);
            excludeIncompatible.setSelected(ProgramProperties.get("GFFExportExcludeIncompatible", false));
            excludeDominated.setToolTipText("Don't report any genes that are dominated by better ones.");
            excludeDominated.setEnabled(canExport);
            excludeDominated.setSelected(ProgramProperties.get("GFFExportExcludeDominated", true));

            middlePanel.add(excludeIncompatible);
            middlePanel.add(excludeDominated);
        }

        final JPanel bottomPanel = new JPanel();
        {
            bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
            bottomPanel.add(Box.createHorizontalGlue());
            if (!canExport) {
                bottomPanel.add(new JLabel("Nothing to export."));
            }
            bottomPanel.add(Box.createHorizontalGlue());
            dialog.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        }

        final JButton cancel = new JButton(new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });
        bottomPanel.add(cancel);

        final Single<Triplet<Boolean, Boolean, String>> result = new Single<>(null);
        final JButton apply = new JButton(new AbstractAction("Apply") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ProgramProperties.put("GFFExportClassification", (String) classificationCBox.getSelectedItem());
                ProgramProperties.put("GFFExportExcludeIncompatible", excludeIncompatible.isSelected());
                ProgramProperties.put("GFFExportExcludeDominated", excludeDominated.isSelected());
                result.set(new Triplet<>(excludeIncompatible.isSelected(), excludeDominated.isSelected(), (String) classificationCBox.getSelectedItem()));
                dialog.setVisible(false);
            }
        });
        apply.setEnabled(canExport);
        bottomPanel.add(apply);

        dialog.setVisible(true);
        return result.get();
    }

    public boolean isApplicable() {
        if (((Director) getDir()).getDocument().getBlastMode() == BlastMode.BlastX) {
            if (getViewer() instanceof ClassificationViewer) {
                final ClassificationViewer viewer = (ClassificationViewer) getViewer();
                return viewer.getDocument().isLongReads() && viewer.getNumberSelectedNodes() > 0;
            } else if (getViewer() instanceof LRInspectorViewer) {
                return ((LRInspectorViewer) getViewer()).getNumberOfSelectedItems() > 0;
            }
        }
        return false;
    }

    private static final String NAME = "Annotations in GFF Format...";

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return "Export selected long reads and their aligned genes in GFF3 format";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
