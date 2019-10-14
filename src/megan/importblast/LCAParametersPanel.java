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
package megan.importblast;

import jloda.swing.commands.CommandManager;
import jloda.util.ProgramProperties;
import megan.core.Document;
import megan.importblast.commands.SetUseComplexityFilterCommand;
import megan.importblast.commands.SetUseIdentityFilterCommand;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * panel for setting LCA parameters
 * Daniel Huson, 12.2012
 */
public class LCAParametersPanel extends JPanel {
    /**
     * construct the parameters panel
     */
    public LCAParametersPanel(final ImportBlastDialog dialog) {
        final CommandManager commandManager = dialog.getCommandManager();

        setLayout(new BorderLayout());

        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(18, 2));

        centerPanel.add(new JLabel("Min Score:"));
        centerPanel.add(dialog.getMinScoreField());
        dialog.getMinScoreField().setToolTipText("Minimal bitscore that a match must attain");

        centerPanel.add(new JLabel("Max Expected:"));
        centerPanel.add(dialog.getMaxExpectedField());
        dialog.getMaxExpectedField().setToolTipText("Ignore all matches whose expected values lie above this threshold");

        centerPanel.add(new JLabel("Min Percent Identity:"));
        centerPanel.add(dialog.getMinPercentIdentityField());
        dialog.getMaxExpectedField().setToolTipText("Ignore all matches whose min percent identity lie above this threshold");

        centerPanel.add(new JLabel(" "));
        centerPanel.add(new JLabel(" "));

        centerPanel.add(new JLabel("Top Percent:"));
        centerPanel.add(dialog.getTopPercentField());
        dialog.getTopPercentField().setToolTipText("Match must lie within this percentage of the best score attained for a read");

        centerPanel.add(new JLabel(" "));
        centerPanel.add(new JLabel(" "));

        centerPanel.add(new JLabel("Min Support Percent:"));
        centerPanel.add(dialog.getMinSupportPercentField());
        dialog.getMinSupportPercentField().setToolTipText("Minimum number of reads that a taxon must obtain as a percentage of total reads assigned");

        centerPanel.add(new JLabel("Min Support:"));
        centerPanel.add(dialog.getMinSupportField());
        dialog.getMinSupportField().setToolTipText("Minimum number of reads that a taxon must obtain");

        {
            centerPanel.add(new JLabel(" "));
            centerPanel.add(new JLabel(" "));

            final AbstractButton button = commandManager.getButton(SetUseComplexityFilterCommand.NAME);
            button.setText(button.getText() + ":");
            centerPanel.add(button);
            centerPanel.add(dialog.getMinComplexityField());
            dialog.getMinComplexityField().setToolTipText("Minimum complexity for a read to be considered non-repetitive\nComputed as compression ratio between 0 and 1");

            centerPanel.add(new JLabel(" "));
            centerPanel.add(new JLabel(" "));
        }

        {
            centerPanel.add(new JLabel("LCA Algorithm:"));

            final JComboBox<String> lcaAlgorithmComboBox = dialog.getLcaAlgorithmComboBox();
            lcaAlgorithmComboBox.setEditable(false);
            for (Document.LCAAlgorithm algorithm : Document.LCAAlgorithm.values()) {
                lcaAlgorithmComboBox.addItem(algorithm.toString());
            }

            lcaAlgorithmComboBox.addActionListener(e -> {
                if(lcaAlgorithmComboBox.getSelectedItem()!=null) {
                    switch (Document.LCAAlgorithm.valueOfIgnoreCase(lcaAlgorithmComboBox.getSelectedItem().toString())) {
                        case naive:
                            dialog.getLcaCoveragePercentField().setText("100");
                            lcaAlgorithmComboBox.setToolTipText("Naive LCA for taxonomic binning: fast algorithm applicable to short reads");
                            break;
                        case weighted:
                            dialog.getLcaCoveragePercentField().setText("80");
                            lcaAlgorithmComboBox.setToolTipText("Weighted LCA for taxonomic binning: slower algorithm applicable to short reads, slightly more specific than naive LCA");
                            break;
                        case longReads:
                            dialog.getLcaCoveragePercentField().setText("80");
                            lcaAlgorithmComboBox.setToolTipText("Long Reads LCA for taxonomic and functional binning of long reads and contigs");
                            break;
                        default:
                            lcaAlgorithmComboBox.setToolTipText("Select LCA algorithm");
                    }
                }

                if (lcaAlgorithmComboBox.getSelectedItem() != null) {
                    final Document.LCAAlgorithm algorithm = Document.LCAAlgorithm.valueOfIgnoreCase((String) lcaAlgorithmComboBox.getSelectedItem());
                    dialog.setLcaAlgorithm(algorithm != null ? algorithm : Document.DEFAULT_LCA_ALGORITHM_SHORT_READS);
                } else
                    dialog.setLcaAlgorithm(Document.DEFAULT_LCA_ALGORITHM_SHORT_READS);
                ProgramProperties.put("SelectedLCAAlgorithm" + (dialog.isLongReads() ? "LongReads" : "ShortReads"), dialog.getLcaAlgorithm().toString());
            });


            lcaAlgorithmComboBox.setSelectedItem(0);
            lcaAlgorithmComboBox.setToolTipText("Set LCA algorithm for taxonomic binning");
            centerPanel.add(lcaAlgorithmComboBox);

            centerPanel.add(new JLabel("Percent to cover:"));
            dialog.getLcaCoveragePercentField().setToolTipText("Percent of weight to cover by weighted LCA");

            centerPanel.add(dialog.getLcaCoveragePercentField());
            dialog.getLcaCoveragePercentField().setToolTipText("Percent of weight to cover by weighted LCA or long read LCA");
        }

        {
            centerPanel.add(new JLabel("Read Assignment mode:"));

            final JComboBox<String> readAssignmentModeComboBox = dialog.getReadAssignmentModeComboBox();
            readAssignmentModeComboBox.setEditable(false);
            for (Document.ReadAssignmentMode readAssignmentMode : Document.ReadAssignmentMode.values()) {
                readAssignmentModeComboBox.addItem(readAssignmentMode.toString());
            }
            centerPanel.add(readAssignmentModeComboBox);
            readAssignmentModeComboBox.setToolTipText("Read assignment mode: determines what is shown as number of assigned reads in taxonomy analysis");
            readAssignmentModeComboBox.addActionListener(e -> {
                if (readAssignmentModeComboBox.getSelectedItem() != null) {
                    ProgramProperties.put("ReadAssignmentModeComboBox", readAssignmentModeComboBox.toString());
                }
                switch (Document.ReadAssignmentMode.valueOfIgnoreCase(readAssignmentModeComboBox.getSelectedItem().toString())) {
                    case readCount:
                        readAssignmentModeComboBox.setToolTipText("Display read counts as 'assigned reads' in taxonomy viewer");
                        break;
                    case readLength:
                        readAssignmentModeComboBox.setToolTipText("Display sum of read lengths as 'assigned reads' in taxonomy viewer");
                        break;
                    case alignedBases:
                        readAssignmentModeComboBox.setToolTipText("Display number of aligned bases as 'assigned reads' in taxonomy viewer");
                        break;
                    case readMagnitude:
                        readAssignmentModeComboBox.setToolTipText("Display sum of read magnitudes as 'assigned reads' in taxonomy viewer");
                        break;
                    default:
                        readAssignmentModeComboBox.setToolTipText("Select what to display as 'assigned reads' in taxonomy viewer");
                }
            });
            centerPanel.add(new JLabel(" "));
            centerPanel.add(new JLabel(" "));
        }

        {
            centerPanel.add(commandManager.getButton(SetUseIdentityFilterCommand.NAME));
            centerPanel.add(new JLabel(" "));
        }

        {
            centerPanel.add(new JLabel("Min Percent Read To Cover:"));
            centerPanel.add(dialog.getMinPercentReadToCoverField());
            dialog.getMinSupportPercentField().setToolTipText("Minimum percent of read that has to be covered by alignments for read to be binned");
        }
        {
            centerPanel.add(new JLabel("Min Percent Reference To Cover:"));
            centerPanel.add(dialog.getMinPercentReferenceToCoverField());
            dialog.getMinSupportPercentField().setToolTipText("Minimum percent of reference that has to be covered by alignments for reference to be considered");
        }


        JPanel three = new JPanel();
        three.setLayout(new BoxLayout(three, BoxLayout.X_AXIS));
        outerPanel.setBorder(BorderFactory.createTitledBorder("LCA and analysis parameters"));

        three.add(Box.createHorizontalGlue());
        three.add(centerPanel);
        three.add(Box.createHorizontalGlue());

        outerPanel.add(three);
        add(outerPanel, BorderLayout.CENTER);
    }

}
