/*
 *  Copyright (C) 2017 Daniel H. Huson
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
package megan.dialogs.parameters;

import jloda.gui.commands.CommandManager;
import jloda.gui.commands.ICheckBoxCommand;
import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.commandtemplates.SetUseLCA4ViewerCommand;
import megan.classification.data.ClassificationCommandHelper;
import megan.core.Director;
import megan.core.Document;
import megan.daa.connector.DAAConnector;
import megan.dialogs.parameters.commands.ApplyCommand;
import megan.dialogs.parameters.commands.CancelCommand;
import megan.importblast.commands.SetUseIdentityFilterCommand;
import megan.parsers.blast.BlastMode;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * load data from a blast file
 * Daniel Huson, 8.2008, 4.2017
 */
public class ParametersDialog extends JDialog {
    final JTextField minScoreField = new JTextField(8);

    private final JTextField topPercentField = new JTextField(8);
    private final JTextField maxExpectedField = new JTextField(8);
    private final JTextField minSupportField = new JTextField(8);
    private final JTextField minSupportPercentField = new JTextField(8);
    private final JTextField minPercentIdentityField = new JTextField(8);
    private final JTextField minComplexityField = new JTextField(8);
    private final JTextField weightedLCAPercentField = new JTextField(8);

    private final JTextField minPercentReadToCoverField = new JTextField(8);


    private final JComboBox<String> lcaAlgorithmComboBox = new JComboBox<>();

    private final JCheckBox pairReadsCBox = new JCheckBox("Use Paired Reads");
    private final JCheckBox longReadsCBox = new JCheckBox("Parse as Long Reads");

    private final JComboBox<String> readAssignmentModeComboBox = new JComboBox<>();


    private final JCheckBox usePercentIdentityCBox = new JCheckBox("Use 16S Percent Identity Filter");

    private final Set<String> activeFNames = new HashSet<>();
    private boolean canceled = true;

    private final CommandManager commandManager;

    private boolean avoidBounce = false;

    /**
     * constructor
     *
     * @param parent
     * @param dir
     */
    public ParametersDialog(Component parent, Director dir) {
        super();

        commandManager = new CommandManager(dir, this, new String[]{"megan.dialogs.parameters.commands"}, !ProgramProperties.isUseGUI());
        commandManager.addCommands(this, ClassificationCommandHelper.getImportBlastCommands(), true);

        setLocationRelativeTo(parent);
        setTitle("Change LCA Parameters - MEGAN");
        setModal(true);
        setSize(500, 800);

        final Document doc = dir.getDocument();

        setMinScore(doc.getMinScore());
        setMaxExpected(doc.getMaxExpected());
        setMinPercentIdentity(doc.getMinPercentIdentity());
        setTopPercent(doc.getTopPercent());
        setMinSupportPercent(doc.getMinSupportPercent());
        setMinSupport(doc.getMinSupportPercent() > 0 ? 0 : doc.getMinSupport());

        lcaAlgorithmComboBox.setEditable(false);
        for (Document.LCAAlgorithm algorithm : Document.LCAAlgorithm.values()) {
            lcaAlgorithmComboBox.addItem(algorithm.toString());
        }
        setLcaAlgorithm(doc.getLcaAlgorithm());

        setWeightedLCAPercent(doc.getWeightedLCAPercent());

        readAssignmentModeComboBox.setEditable(false);
        for (Document.ReadAssignmentMode readAssignmentMode : Document.ReadAssignmentMode.values()) {
            readAssignmentModeComboBox.addItem(readAssignmentMode.toString());
        }
        setReadAssignmentMode(doc.getReadAssignmentMode());

        setMinPercentReadToCover(doc.getMinPercentReadToCover());
        setMinComplexity(doc.getMinComplexity());
        setLongReads(doc.isLongReads());
        setPairedReads(doc.isPairedReads());
        setUsePercentIdentity(doc.isUseIdentityFilter());

        activeFNames.addAll(doc.getActiveViewers());
        activeFNames.remove(Classification.Taxonomy);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(makeLCAParametersPanel(doc), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(2, 20, 2, 20));
        bottomPanel.add(Box.createHorizontalGlue());
        final JButton cancelButton = (JButton) commandManager.getButton(CancelCommand.NAME);
        bottomPanel.add(cancelButton);
        final JButton applyButton = (JButton) commandManager.getButton(ApplyCommand.NAME);
        getRootPane().setDefaultButton(applyButton);
        bottomPanel.add(applyButton);

        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        // do this again to trigger code:
        setLcaAlgorithm(doc.getLcaAlgorithm());
        setReadAssignmentMode(doc.getReadAssignmentMode());

        if (doc.getMinComplexity() < 0)
            minComplexityField.setEnabled(false);
    }

    /**
     * show the dialog
     *
     * @return true, if command entered
     */
    public boolean apply() {
        setVisible(true);
        return !isCanceled();
    }

    /**
     * construct the parameters panel
     */
    private JPanel makeLCAParametersPanel(final Document doc) {
        final JTabbedPane tabbedPane = new JTabbedPane();

        // first tab:
        {
            final JPanel aPanel = new JPanel();
            aPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("LCA and analysis parameters:"), BorderFactory.createEmptyBorder(3, 10, 1, 10)));

            final String[] cNames = ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy().toArray(new String[ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy().size()]);

            aPanel.setLayout(new GridLayout(18 + (cNames.length + 1) / 2, 2));

            aPanel.add(new JLabel("Min Score:"));
            aPanel.add(minScoreField);
            minScoreField.setToolTipText("Ignore all matches whose bit score lies below this threshold");
            minScoreField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }

                public void removeUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }

                public void changedUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }
            });

            aPanel.add(new JLabel("Max Expected:"));
            aPanel.add(maxExpectedField);
            maxExpectedField.setToolTipText("Ignore all matches whose expected values lie above this threshold");
            maxExpectedField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }

                public void removeUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }

                public void changedUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }
            });

            aPanel.add(new JLabel("Min Percent Identity:"));
            aPanel.add(minPercentIdentityField);
            minPercentIdentityField.setToolTipText("Ignore all matches whose percent identity lies below this threshold");
            minPercentIdentityField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }

                public void removeUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }

                public void changedUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }
            });


            aPanel.add(new JLabel(" "));
            aPanel.add(new JLabel(" "));

            aPanel.add(new JLabel("Top Percent:"));
            aPanel.add(topPercentField);
            topPercentField.setToolTipText("Match must lie within this percentage of the best score attained for a read to be considered for taxonomic analysis");
            topPercentField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }

                public void removeUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }

                public void changedUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }
            });

            aPanel.add(new JLabel(" "));
            aPanel.add(new JLabel(" "));

            aPanel.add(new JLabel("Min Support Percent:"));
            aPanel.add(minSupportPercentField);
            minSupportPercentField.setToolTipText("Minimum number of reads that a taxon must obtain as a percentage of total reads assigned");
            minSupportPercentField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent event) {
                    if (!avoidBounce) {
                        avoidBounce = true;
                        minSupportField.setText("1");
                        avoidBounce = false;
                    }
                    commandManager.updateEnableState();
                }

                public void removeUpdate(DocumentEvent event) {
                    if (!avoidBounce) {
                        avoidBounce = true;
                        minSupportField.setText("1");
                        avoidBounce = false;
                    }
                    commandManager.updateEnableState();
                }

                public void changedUpdate(DocumentEvent event) {
                    if (!avoidBounce) {
                        avoidBounce = true;
                        minSupportField.setText("1");
                        avoidBounce = false;
                    }
                    commandManager.updateEnableState();
                }
            });

            aPanel.add(new JLabel("Min Support:"));
            aPanel.add(minSupportField);
            minSupportField.setToolTipText("Minimum number of reads (or base pairs, when using long-read mode) that a taxon must obtain");
            minSupportField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent event) {
                    if (!avoidBounce) {
                        avoidBounce = true;
                        minSupportPercentField.setText("0 (off)");
                        avoidBounce = false;
                    }
                    commandManager.updateEnableState();
                }

                public void removeUpdate(DocumentEvent event) {
                    if (!avoidBounce) {
                        avoidBounce = true;
                        minSupportPercentField.setText("0 (off)");
                        avoidBounce = false;
                    }
                    commandManager.updateEnableState();
                }

                public void changedUpdate(DocumentEvent event) {
                    if (!avoidBounce) {
                        avoidBounce = true;
                        minSupportPercentField.setText("0 (off)");
                        avoidBounce = false;
                    }
                    commandManager.updateEnableState();
                }
            });


            aPanel.add(new JLabel(" "));
            aPanel.add(new JLabel(" "));

            aPanel.add(new JLabel("Min Complexity:"));
            aPanel.add(minComplexityField);
            minComplexityField.setToolTipText("Minimum complexity for a read to be considered non-repetitive");
            minComplexityField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }

                public void removeUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }

                public void changedUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }
            });


            aPanel.add(new JLabel(" "));
            aPanel.add(new JLabel(" "));

            aPanel.add(new JLabel("LCA Algorithm:"));

            aPanel.add(lcaAlgorithmComboBox);
            lcaAlgorithmComboBox.setToolTipText("Set the LCA algorithm to be used for taxonomic binning");
            lcaAlgorithmComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (lcaAlgorithmComboBox.getSelectedItem() != null) {
                        ProgramProperties.put("SelectedLCAAlgorithm", getLcaAlgorithm().toString());
                        weightedLCAPercentField.setEnabled(getLcaAlgorithm().equals(Document.LCAAlgorithm.weighted)
                                || getLcaAlgorithm().equals(Document.LCAAlgorithm.longReads));
                    }
                    switch (Document.LCAAlgorithm.valueOfIgnoreCase(lcaAlgorithmComboBox.getSelectedItem().toString())) {
                        case naive:
                            lcaAlgorithmComboBox.setToolTipText("Naive LCA for taxonomic binning: fast algorithm applicable to short reads");
                            break;
                        case weighted:
                            lcaAlgorithmComboBox.setToolTipText("Weighted LCA for taxonomic binning: slower algorithm applicable to short reads, slightly more specific than naive LCA");
                            break;
                        case longReads:
                            lcaAlgorithmComboBox.setToolTipText("Long Reads LCA for taxonomic and functional binning of long reads and contigs");
                            break;
                        default:
                            lcaAlgorithmComboBox.setToolTipText("Select LCA algorithm");
                    }
                }
            });

            aPanel.add(new JLabel("Percent to cover:"));
            aPanel.add(weightedLCAPercentField);
            weightedLCAPercentField.setToolTipText("Percent of weight to cover by weighted LCA or long reads LCA");
            weightedLCAPercentField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }

                public void removeUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }

                public void changedUpdate(DocumentEvent event) {
                    commandManager.updateEnableState();
                }
            });
            weightedLCAPercentField.setText("" + doc.getWeightedLCAPercent());
            weightedLCAPercentField.setEnabled(getLcaAlgorithm().equals(Document.LCAAlgorithm.weighted)
                    || getLcaAlgorithm().equals(Document.LCAAlgorithm.longReads));

            aPanel.add(new JLabel("Read Assignment Mode:"));

            aPanel.add(readAssignmentModeComboBox);
            readAssignmentModeComboBox.setToolTipText("Read assignment mode: determines what is shown as number of assigned reads in taxonomy analysis");
            readAssignmentModeComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (readAssignmentModeComboBox.getSelectedItem() != null) {
                        ProgramProperties.put("ReadAssignmentModeComboBox", getReadAssignmentMode().toString());
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
                }
            });

            aPanel.add(new JLabel(" "));
            aPanel.add(new JLabel(" "));

            aPanel.add(pairReadsCBox);
            pairReadsCBox.setToolTipText("Process paired reads together (will only work if reads were imported as pairs)");

            aPanel.add(longReadsCBox);
            longReadsCBox.setToolTipText("EXPERIMENTAL. Reads parsed as 'long reads'");
            longReadsCBox.setEnabled(doc.getConnector() instanceof DAAConnector); // can only change this if is DAA file because reads are sorted during
            longReadsCBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    lcaAlgorithmComboBox.setSelectedItem(lcaAlgorithmComboBox.getItemAt(longReadsCBox.isSelected() ? 2 : 0));
                    ProgramProperties.put("SelectedLCAAlgorithm", getLcaAlgorithm().toString());
                }
            });

            aPanel.add(usePercentIdentityCBox);
            usePercentIdentityCBox.setToolTipText(SetUseIdentityFilterCommand.DESCRIPTION);
            usePercentIdentityCBox.setEnabled(doc.getBlastMode().equals(BlastMode.BlastN));
            aPanel.add(new JLabel(" "));


            aPanel.add(new JLabel(" "));
            aPanel.add(new JLabel(" "));

            final Set<String> allClassifications = new HashSet<>();
            try {
                allClassifications.addAll(Arrays.asList(doc.getConnector().getAllClassificationNames()));
            } catch (IOException e) {
                Basic.caught(e);
            }
            allClassifications.addAll(activeFNames);

            for (final String cName : cNames) {
                final JCheckBox checkBox = new JCheckBox();
                checkBox.setAction(new AbstractAction("Analyze using " + cName) {
                    public void actionPerformed(ActionEvent e) {
                        if (checkBox.isSelected())
                            activeFNames.add(cName);
                        else
                            activeFNames.remove(cName);
                    }
                });
                checkBox.setToolTipText("Perform functional analysis using " + cName);
                checkBox.setSelected(activeFNames.contains(cName));
                checkBox.setEnabled(allClassifications.contains(cName));

                aPanel.add(checkBox);
            }
            if ((cNames.length % 2) == 1)
                aPanel.add(new JLabel(" "));

            tabbedPane.add(aPanel, "Parameters");
        }

        // second tab:
        {
            final JPanel aPanel = new JPanel();
            aPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Advanced settings:"), BorderFactory.createEmptyBorder(3, 10, 1, 10)));
            aPanel.setLayout(new BoxLayout(aPanel, BoxLayout.Y_AXIS));
            aPanel.setToolTipText("These are application-wide settings determining the algorithm used for classification.\n" +
                    "By default, the 'best hit' algorithm is used.\n" +
                    "Select a box to use the 'LCA' algorithm instead.\n" +
                    "We do not recommend selecting any of these boxes.\n");

            try {
                for (String cName : doc.getConnector().getAllClassificationNames()) {
                    if (!ClassificationManager.getDefaultClassificationsList().contains(cName) && ClassificationManager.getAllSupportedClassifications().contains(cName)) {
                        final JPanel bPanel = new JPanel();
                        bPanel.setLayout(new BoxLayout(bPanel, BoxLayout.X_AXIS));
                        final ICommand useLCACommand = commandManager.getCommand(SetUseLCA4ViewerCommand.getAltName(cName));
                        final AbstractButton useLCAButton = commandManager.getButton(useLCACommand);
                        if (useLCAButton instanceof ICheckBoxCommand)
                            useLCAButton.setSelected(((ICheckBoxCommand) useLCACommand).isSelected());
                        useLCAButton.setEnabled(useLCACommand.isApplicable());
                        useLCAButton.setText("Use LCA to analyze " + cName + " data");
                        bPanel.add(useLCAButton);
                        aPanel.add(bPanel);
                    }
                }
            } catch (IOException e) {
                Basic.caught(e);
            }
            aPanel.add(Box.createVerticalGlue());
            {
                JPanel line = new JPanel();
                line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));
                line.add(new JLabel("Min Percent Read To Cover: "));
                minPercentReadToCoverField.setText("" + doc.getMinPercentReadToCover());
                minPercentReadToCoverField.setMaximumSize(new Dimension(100, 26));
                line.add(minPercentReadToCoverField);
                minPercentReadToCoverField.setToolTipText("Minimum percent of read that has to be covered by alignments for read to be binned");
                minPercentReadToCoverField.getDocument().addDocumentListener(new DocumentListener() {
                    public void insertUpdate(DocumentEvent event) {
                        commandManager.updateEnableState();
                    }

                    public void removeUpdate(DocumentEvent event) {
                        commandManager.updateEnableState();
                    }

                    public void changedUpdate(DocumentEvent event) {
                        commandManager.updateEnableState();
                    }
                });
                aPanel.add(line);
            }
            aPanel.add(Box.createVerticalGlue());
            aPanel.add(Box.createVerticalGlue());

            tabbedPane.add("Advanced", aPanel);
        }

        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(tabbedPane, BorderLayout.CENTER);
        return panel;
    }

    public double getMinScore() {
        double value = Document.DEFAULT_MINSCORE;
        try {
            value = Double.parseDouble(minScoreField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    public double getTopPercent() {
        double value = Document.DEFAULT_TOPPERCENT;
        try {
            value = Double.parseDouble(topPercentField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return Math.max(0, Math.min(100, value));
    }

    public double getMaxExpected() {
        double value = Document.DEFAULT_MAXEXPECTED;
        try {
            value = Double.parseDouble(maxExpectedField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    public double getMinComplexity() {
        double value = Document.DEFAULT_MINCOMPLEXITY;
        try {
            value = Double.parseDouble(minComplexityField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    public float getMinPercentIdentity() {
        float value = Document.DEFAULT_MIN_PERCENT_IDENTITY;
        try {
            value = Float.parseFloat(minPercentIdentityField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return Math.max(0, Math.min(100, value));
    }

    public void setMinScore(double value) {
        minScoreField.setText("" + (float) value);
    }

    public void setTopPercent(double value) {
        topPercentField.setText("" + (float) value);
    }

    public void setMaxExpected(double value) {
        maxExpectedField.setText("" + (float) value);
    }

    public int getMinSupport() {
        int value = Document.DEFAULT_MINSUPPORT;
        try {
            value = Integer.parseInt(minSupportField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return Math.max(1, value);
    }

    public void setMinSupport(int value) {
        minSupportField.setText("" + Math.max(1, value));
    }

    public float getMinSupportPercent() {
        float value = 0;
        try {
            value = Basic.parseFloat(minSupportPercentField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return Math.max(0, value);
    }

    public void setMinSupportPercent(float value) {
        minSupportPercentField.setText("" + Math.max(0f, value) + (value <= 0 ? " (off)" : ""));
    }

    public void setMinComplexity(double value) {
        minComplexityField.setText("" + (float) value);
    }

    public void setMinPercentReadToCover(double value) {
        minPercentReadToCoverField.setText("" + (float) value);
    }

    public double getMinPercentReadToCover() {
        double value = Document.DEFAULT_MIN_PERCENT_READ_TO_COVER;
        try {
            value = Double.parseDouble(minPercentReadToCoverField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return Math.max(0, Math.min(100, value));
    }

    public void setMinPercentIdentity(double value) {
        minPercentIdentityField.setText("" + (float) value);
    }

    public boolean isLongReads() {
        return longReadsCBox.isSelected();
    }

    public void setLongReads(boolean longReads) {
        longReadsCBox.setSelected(longReads);
    }

    public boolean isPairedReads() {
        return pairReadsCBox.isSelected();
    }

    public void setPairedReads(boolean pairedReads) {
        pairReadsCBox.setSelected(pairedReads);
    }

    public boolean isUsePercentIdentity() {
        return usePercentIdentityCBox.isSelected();
    }

    public void setUsePercentIdentity(boolean usePercentIdentity) {
        usePercentIdentityCBox.setSelected(usePercentIdentity);
    }

    public Document.LCAAlgorithm getLcaAlgorithm() {
        return Document.LCAAlgorithm.valueOf((String) lcaAlgorithmComboBox.getSelectedItem());
    }

    public void setLcaAlgorithm(Document.LCAAlgorithm lcaAlgorithm) {
        lcaAlgorithmComboBox.setSelectedItem(lcaAlgorithm.toString());
    }

    public Document.ReadAssignmentMode getReadAssignmentMode() {
        return Document.ReadAssignmentMode.valueOf((String) readAssignmentModeComboBox.getSelectedItem());
    }

    public void setReadAssignmentMode(Document.ReadAssignmentMode readAssignmentMode) {
        readAssignmentModeComboBox.setSelectedItem(readAssignmentMode.toString());
    }

    public float getWeightedLCAPercent() {
        float value = Document.DEFAULT_WEIGHTED_LCA_PERCENT;
        try {
            value = Basic.parseFloat(weightedLCAPercentField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return Math.min(100, Math.max(0, value));
    }

    public void setWeightedLCAPercent(float value) {
        weightedLCAPercentField.setText("" + Math.max(0f, value) + (value <= 0 ? " (off)" : ""));
    }

    public String getParameterString() {
        return " minSupportPercent=" + getMinSupportPercent() +
                " minSupport=" + getMinSupport() + " minScore=" + getMinScore() + " maxExpected=" + getMaxExpected()
                + " minPercentIdentity=" + getMinPercentIdentity() + " topPercent=" + getTopPercent() +
                " lcaAlgorithm=" + getLcaAlgorithm().toString() + (getLcaAlgorithm() == Document.LCAAlgorithm.weighted || getLcaAlgorithm() == Document.LCAAlgorithm.longReads ? " weightedLCAPercent=" + getWeightedLCAPercent() : "") +
                " minPercentReadToCover=" + getMinPercentReadToCover() + " minComplexity=" + getMinComplexity() + " longReads=" + isLongReads() +
                " pairedReads=" + isPairedReads() + " useIdentityFilter=" + isUsePercentIdentity()
                + " readAssignmentMode=" + getReadAssignmentMode()
                + " fNames=" + Basic.toString(activeFNames, " ");
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
