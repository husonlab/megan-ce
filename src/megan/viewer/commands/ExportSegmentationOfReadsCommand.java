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

package megan.viewer.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.analysis.TaxonomicSegmentation;
import megan.core.Document;
import megan.dialogs.export.analysis.SegmentationOfReadsExporter;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomicLevels;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/**
 * export taxonomic segmentation of reads
 * Daniel Huson, 8.2018
 */
public class ExportSegmentationOfReadsCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "export segmentedReads file=<filename> [rank={" + Basic.toString(TaxonomicLevels.getAllMajorRanks(), "|") + "|next}" +
                " [switchPenalty=<number>] [compatibleFactor=<number>] [incompatibleFactor=<number>];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export segmentedReads file=");
        String fileName = np.getAbsoluteFileName();

        final String rankName;
        if (np.peekMatchIgnoreCase("rank")) {
            np.matchIgnoreCase("rank=");
            rankName = np.getWordMatchesIgnoringCase(Basic.toString(TaxonomicLevels.getAllMajorRanks(), " ") + " next");
        } else
            rankName = "next";
        final int rank = (rankName.equals("next") ? 0 : TaxonomicLevels.getId(rankName));

        final TaxonomicSegmentation taxonomicSegmentation = new TaxonomicSegmentation();
        if (np.peekMatchIgnoreCase("switchPenalty")) {
            np.matchIgnoreCase("switchPenalty=");
            taxonomicSegmentation.setSwitchPenalty((float) np.getDouble(0.0, 1000000.0));
        }
        if (np.peekMatchIgnoreCase("compatibleFactor")) {
            np.matchIgnoreCase("compatibleFactor=");
            taxonomicSegmentation.setCompatibleFactor((float) np.getDouble(0, 1000.0));
        }
        if (np.peekMatchIgnoreCase("incompatibleFactor")) {
            np.matchIgnoreCase("incompatibleFactor=");
            taxonomicSegmentation.setIncompatibleFactor((float) np.getDouble(0, 1000.0));
        }
        np.matchIgnoreCase(";");

        try {
            final MainViewer viewer = (MainViewer) getViewer();
            final Document doc = viewer.getDocument();
            final int count = SegmentationOfReadsExporter.export(doc.getProgressListener(), viewer.getClassification().getName(), viewer.getSelectedIds(), rank, doc.getConnector(), fileName, taxonomicSegmentation);

            NotificationsInSwing.showInformation("Exported segmentation of reads: " + count);

        } catch (IOException e) {
            NotificationsInSwing.showError("Export segmentation of reads failed: " + e.getMessage());
        }
    }

    public void actionPerformed(ActionEvent event) {
        if (getViewer() instanceof MainViewer) {
            final MainViewer viewer = (MainViewer) getViewer();

            final JDialog frame = new JDialog();
            frame.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
            frame.setLocationRelativeTo(viewer.getFrame());
            frame.setSize(280, 180);
            final JPanel pane = new JPanel();
            pane.setLayout(new BorderLayout());
            pane.setBorder(new EmptyBorder(2, 5, 2, 3));
            frame.getContentPane().add(pane);
            pane.add(new JLabel("Setup read-segmentation DP"), BorderLayout.NORTH);
            final JPanel center = new JPanel();
            center.setLayout(new GridLayout(4, 2));
            center.setBorder(new EtchedBorder());
            pane.add(center, BorderLayout.CENTER);
            center.add(new JLabel("Rank:"));
            final JComboBox<String> rankMenu = new JComboBox<>(Basic.toArray("next " + " " + Basic.toString(TaxonomicLevels.getAllMajorRanks(), " ")));
            rankMenu.setSelectedItem(ProgramProperties.get("SegmentationRank", "next"));
            rankMenu.setEditable(false);
            rankMenu.setToolTipText("Use 'next' to always use the next rank down below rank at which read is assigned");
            center.add(rankMenu);
            center.add(new JLabel("Switch penalty:"));
            final JTextField switchPenaltyField = new JTextField();
            switchPenaltyField.setMaximumSize(new Dimension(40, 12));
            switchPenaltyField.setText("" + (float) ProgramProperties.get("SegmentationSwitchPenalty", TaxonomicSegmentation.defaultSwitchPenalty));
            switchPenaltyField.setToolTipText("Penalty for switching to a different taxon");
            center.add(switchPenaltyField);
            center.add(new JLabel("Compatible factor:"));
            final JTextField compatibleFactorField = new JTextField();
            compatibleFactorField.setText("" + (float) ProgramProperties.get("SegmentationCompatibleFactor", TaxonomicSegmentation.defaultCompatibleFactor));
            compatibleFactorField.setMaximumSize(new Dimension(40, 12));
            compatibleFactorField.setToolTipText("Factor for weighting bitscore of alignment with same or compatible taxon");
            center.add(compatibleFactorField);
            center.add(new JLabel("Incompatible factor:"));
            final JTextField incompatibleFactorField = new JTextField();
            incompatibleFactorField.setText("" + (float) ProgramProperties.get("SegmentationIncompatibleFactor", TaxonomicSegmentation.defaultIncompatibleFactor));
            incompatibleFactorField.setMaximumSize(new Dimension(40, 12));
            incompatibleFactorField.setToolTipText("Factor for weighting bitscore of alignment with incompatible taxon");
            center.add(incompatibleFactorField);

            // options
            final JPanel bottom = new JPanel();
            bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
            bottom.add(Box.createHorizontalGlue());
            bottom.add(new JButton(new AbstractAction("Cancel") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(false);
                }
            }));
            bottom.add(new JButton(new AbstractAction("Apply") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(false);
                    final String fileName = Basic.replaceFileSuffix(viewer.getDocument().getMeganFile().getFileName(), "-%i-%t-segmentation.txt");
                    File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), new File(fileName), new TextFileFilter(), new TextFileFilter(), e, "Save segmentation of reads file", ".txt");

                    if (file != null) {
                        if (Basic.getFileSuffix(file.getName()) == null)
                            file = Basic.replaceFileSuffix(file, ".fasta");

                        if (rankMenu.getSelectedItem() != null)
                            ProgramProperties.put("SegmentationRank", rankMenu.getSelectedItem().toString());
                        ProgramProperties.put("SegmentationSwitchPenalty", Basic.parseFloat(switchPenaltyField.getText()));
                        ProgramProperties.put("SegmentationCompatibleFactor", Basic.parseFloat(compatibleFactorField.getText()));
                        ProgramProperties.put("SegmentationIncompatibleFactor", Basic.parseFloat(incompatibleFactorField.getText()));

                        execute("export segmentedReads file='" + file.getPath() + "'"
                                + " rank=" + rankMenu.getSelectedItem()
                                + " switchPenalty=" + Basic.parseFloat(switchPenaltyField.getText()) + " compatibleFactor=" + Basic.parseFloat(compatibleFactorField.getText())
                                + " incompatibleFactor=" + Basic.parseFloat(incompatibleFactorField.getText())
                                + ";");
                    }
                }
            }));
            pane.add(bottom, BorderLayout.SOUTH);

            frame.setVisible(true);
        }
    }


    public boolean isApplicable() {
        if (getViewer() instanceof MainViewer) {
            final MainViewer viewer = (MainViewer) getViewer();
            return viewer.getSelectedNodes().size() > 0 && viewer.getDocument().getMeganFile().hasDataConnector();

        } else
            return false;
    }

    public String getName() {
        return "Export Segmentation of Reads...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Export segmentation of reads, use %t or %i in filename for to save each class into a different file";
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

}