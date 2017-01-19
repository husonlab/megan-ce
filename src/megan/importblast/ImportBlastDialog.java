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
package megan.importblast;

import jloda.gui.StatusBar;
import jloda.gui.commands.CommandManager;
import jloda.gui.commands.ICheckBoxCommand;
import jloda.gui.director.IDirectableViewer;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.commandtemplates.SetAnalyse4ViewerCommand;
import megan.classification.data.ClassificationCommandHelper;
import megan.core.Director;
import megan.core.Document;
import megan.fx.NotificationsInSwing;
import megan.importblast.commands.ApplyCommand;
import megan.importblast.commands.CancelCommand;
import megan.importblast.commands.NextTabCommand;
import megan.importblast.commands.PreviousTabCommand;
import megan.main.MeganProperties;
import megan.parsers.blast.BlastFileFormat;
import megan.parsers.blast.BlastMode;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * load data from a blast file
 * Daniel Huson, 8.2008
 */
public class ImportBlastDialog extends JDialog implements IDirectableViewer {
    private final Director dir;
    private boolean isLocked = false;
    private boolean isUpToDate = true;

    final JTextField minScoreField = new JTextField(8);
    final JTextField maxExpectedField = new JTextField(8);
    final JTextField minPercentIdentityField = new JTextField(8);

    final JTextField topPercentField = new JTextField(8);
    final JTextField minSupportField = new JTextField(8);
    final JTextField minSupportPercentField = new JTextField(8);
    final JTextField weightedLCAPercentField = new JTextField(8);
    final JTextField minComplexityField = new JTextField(8);

    private boolean usePairedReads = false;
    private String pairedReadSuffix1 = ProgramProperties.get(MeganProperties.PAIRED_READ_SUFFIX1, "");
    private String pairedReadSuffix2 = ProgramProperties.get(MeganProperties.PAIRED_READ_SUFFIX2, "");

    private boolean useReadMagnitudes = false;
    private boolean parseTaxonNames = ProgramProperties.get(MeganProperties.PARSE_TAXON_NAMES, true);

    private boolean usePercentIdentityFilter = false;

    private boolean weightedLCA = false;

    private final JTextArea blastFileNameField = new JTextArea();

    private final FormatCBox formatCBox = new FormatCBox();

    private final ModeCBox alignmentModeCBox = new ModeCBox();

    private final JTextField shortDescriptionField = new JTextField(8);

    private final JTextArea readFileNameField = new JTextArea();

    private final JTextField meganFileNameField = new JTextField();

    final JTextField maxNumberOfMatchesPerReadField = new JTextField(8);

    final JCheckBox useCompressionCBox = new JCheckBox();

    final JTabbedPane tabbedPane = new JTabbedPane();

    private String result = null;

    private final ArrayList<String> cNames = new ArrayList<>();

    private final CommandManager commandManager;

    /**
     * constructor
     *
     * @param parent
     * @param dir
     */
    public ImportBlastDialog(Component parent, Director dir, final String title) {
        this.dir = dir;
        dir.addViewer(this);

        if (ProgramProperties.getProgramIcon() != null)
            setIconImage(ProgramProperties.getProgramIcon().getImage());

        commandManager = new CommandManager(dir, this, new String[]{"megan.commands", "megan.importblast.commands"}, !ProgramProperties.isUseGUI());
        commandManager.addCommands(this, ClassificationCommandHelper.getImportBlastCommands(), true);

        setMinScore(Document.DEFAULT_MINSCORE);
        setTopPercent(Document.DEFAULT_TOPPERCENT);
        setMaxExpected(Document.DEFAULT_MAXEXPECTED);
        setMinPercentIdentity(Document.DEFAULT_MIN_PERCENT_IDENTITY);
        setMinSupportPercent(0);
        setMinSupport(Document.DEFAULT_MINSUPPORT);
        setMinComplexity(Document.DEFAULT_MINCOMPLEXITY);

        final Document doc = dir.getDocument();

        final String parameters = ProgramProperties.get(MeganProperties.DEFAULT_PROPERTIES, "");
        doc.parseParameterString(parameters);
        setMinScore(doc.getMinScore());
        setTopPercent(doc.getTopPercent());
        setMaxExpected(doc.getMaxExpected());
        setMinPercentIdentity(doc.getMinPercentIdentity());
        setMinSupportPercent(doc.getMinSupportPercent());
        setMinSupport(doc.getMinSupportPercent() > 0 ? 0 : doc.getMinSupport());
        setMinComplexity(doc.getMinComplexity());
        setUsePercentIdentityFilter(doc.isUseIdentityFilter());
        setWeightedLCAPercent(doc.getWeightedLCAPercent());
        setWeightedLCA(doc.isWeightedLCA());

        ArrayList<String> toDelete = new ArrayList<>();
        for (String cName : doc.getActiveViewers()) { // turn of classifications for which mappers have not been loaded
            if (!cName.equals(Classification.Taxonomy) && !ClassificationManager.get(cName, true).getIdMapper().hasActiveAndLoaded())
                toDelete.add(cName);
        }
        doc.getActiveViewers().removeAll(toDelete);

        setLocationRelativeTo(parent);
        setTitle(title);
        setModal(true);
        setSize(900, 550);

        getContentPane().setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        addFilesTab(tabbedPane);

        tabbedPane.addTab("Taxonomy", new ViewerPanel(commandManager, Classification.Taxonomy));
        for (String cName : ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy()) {
            cNames.add(cName);
            tabbedPane.addTab(cName, new ViewerPanel(commandManager, cName));
        }
        tabbedPane.addTab("LCA Params", new LCAParametersPanel(this));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalStrut(20));
        final JLabel tabNumber = new JLabel("Tab 1 of " + tabbedPane.getTabCount());
        tabNumber.setForeground(Color.DARK_GRAY);
        buttonPanel.add(tabNumber);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(commandManager.getButton(PreviousTabCommand.ALTNAME));
        buttonPanel.add(commandManager.getButton(NextTabCommand.ALTNAME));
        JButton cancelButton = (JButton) commandManager.getButton(CancelCommand.ALTNAME);
        buttonPanel.add(cancelButton);
        JButton applyButton = (JButton) commandManager.getButton(ApplyCommand.ALTNAME);
        buttonPanel.add(applyButton);
        getRootPane().setDefaultButton(applyButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        getContentPane().add(new StatusBar(false), BorderLayout.SOUTH);

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                tabNumber.setText("Tab " + (tabbedPane.getSelectedIndex() + 1) + " of " + tabbedPane.getTabCount());
            }
        });

        getCommandManager().updateEnableState();
    }

    public void addFilesTab(JTabbedPane tabbedPane) {
        tabbedPane.addTab("Files", new FilesPanel(this));
    }

    /**
     * show the dialog and return the entered command string, or null
     *
     * @return command string or null
     */
    public String showAndGetCommand() {
        setVisible(true);
        return result;
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

    public void setMinScore(double value) {
        minScoreField.setText("" + (float) value);
    }

    public double getTopPercent() {
        double value = Document.DEFAULT_TOPPERCENT;
        try {
            value = Double.parseDouble(topPercentField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    public void setTopPercent(double value) {
        topPercentField.setText("" + (float) value);
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

    public void setMaxExpected(double value) {
        maxExpectedField.setText("" + (float) value);
    }

    public double getMinPercentIdentity() {
        double value = Document.DEFAULT_MIN_PERCENT_IDENTITY;
        try {
            value = Double.parseDouble(minPercentIdentityField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    public void setMinPercentIdentity(double value) {
        minPercentIdentityField.setText("" + (float) value);
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
        return Math.max(0f, value);
    }

    public void setMinSupportPercent(float value) {
        minSupportPercentField.setText("" + Math.max(0f, value) + (value <= 0 ? " (off)" : ""));
    }

    public double getMinComplexity() {
        double value = Document.DEFAULT_MINCOMPLEXITY;
        try {
            value = Basic.parseDouble(minComplexityField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    public void setMinComplexity(double value) {
        minComplexityField.setText("" + (float) value + (value <= 0 ? " (off)" : ""));
    }

    String shortDescription = "";

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    String blastFileName = "";

    public void setBlastFileName(String name) {
        blastFileName = name;
    }

    public String getBlastFileName() {
        return blastFileName;
    }

    String readFileName = "";

    public void setReadFileName(final String name) {
        readFileName = name;
    }

    public void addReadFileName(String name) {
        if (readFileName == null || readFileName.length() == 0)
            readFileName = name;
        else
            readFileName += "\n" + name;
    }

    public String getReadFileName() {
        return readFileName;
    }

    String meganFileName = "";

    public void setMeganFileName(String name) {
        meganFileName = name;
    }

    public String getMeganFileName() {
        return meganFileName;
    }

    public boolean isUsePairedReads() {
        return usePairedReads;
    }

    public void setUsePairedReads(boolean usePairedReads) {
        this.usePairedReads = usePairedReads;
    }

    public boolean isUseReadMagnitudes() {
        return useReadMagnitudes;
    }

    public void setUseReadMagnitudes(boolean useReadMagnitudes) {
        this.useReadMagnitudes = useReadMagnitudes;
    }

    public boolean isParseTaxonNames() {
        return parseTaxonNames;
    }

    public void setParseTaxonNames(boolean parseTaxonNames) {
        this.parseTaxonNames = parseTaxonNames;
        ProgramProperties.put(MeganProperties.PARSE_TAXON_NAMES, parseTaxonNames);
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public boolean isUsePercentIdentityFilter() {
        return usePercentIdentityFilter;
    }

    public void setUsePercentIdentityFilter(boolean usePercentIdentityFilter) {
        this.usePercentIdentityFilter = usePercentIdentityFilter;
    }

    public boolean isWeightedLCA() {
        return weightedLCA;
    }

    public void setWeightedLCA(boolean weightedLCA) {
        this.weightedLCA = weightedLCA;
    }

    public double getWeightedLCAPercent() {
        double value = Document.DEFAULT_WEIGHTED_LCA_PERCENT;
        try {
            value = Basic.parseDouble(weightedLCAPercentField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    public void setWeightedLCAPercent(double value) {
        weightedLCAPercentField.setText("" + (float) value);
    }

    public JTextField getWeightedLCAPercentField() {
        return weightedLCAPercentField;
    }

    public JTextField getMaxNumberOfMatchesPerReadField() {
        return maxNumberOfMatchesPerReadField;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ModeCBox getAlignmentModeCBox() {
        return alignmentModeCBox;
    }

    public JTextArea getBlastFileNameField() {
        return blastFileNameField;
    }

    public JTextField getShortDescriptionField() {
        return shortDescriptionField;
    }


    public JTextArea getReadFileNameField() {
        return readFileNameField;
    }

    public JTextField getMeganFileNameField() {
        return meganFileNameField;
    }

    public FormatCBox getFormatCBox() {
        return formatCBox;
    }

    public JTextField getMinScoreField() {
        return minScoreField;
    }

    public JTextField getTopPercentField() {
        return topPercentField;
    }

    public JTextField getMaxExpectedField() {
        return maxExpectedField;
    }

    public JTextField getMinPercentIdentityField() {
        return minPercentIdentityField;
    }

    public JTextField getMinSupportField() {
        return minSupportField;
    }

    public JTextField getMinSupportPercentField() {
        return minSupportPercentField;
    }

    public JTextField getMinComplexityField() {
        return minComplexityField;
    }

    public JCheckBox getUseCompressionCBox() {
        return useCompressionCBox;
    }

    /**
     * is viewer uptodate?
     *
     * @return uptodate
     */
    public boolean isUptoDate() {
        return isUpToDate;
    }

    /**
     * return the frame associated with the viewer
     *
     * @return frame
     */
    public JFrame getFrame() {
        return new JFrame("Import Blast Dialog");
    }

    /**
     * ask view to rescan itself. This is method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     *
     * @param what what should be updated? Possible values: Director.ALL or Director.TITLE
     */
    public void updateView(String what) {
        isUpToDate = false;
        commandManager.updateEnableState();
        isUpToDate = true;
    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        isLocked = true;
        getTabbedPane().setEnabled(false);
        if (commandManager != null)
            commandManager.setEnableCritical(false);
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        isLocked = false;
        if (commandManager != null)
            commandManager.setEnableCritical(true);
        getTabbedPane().setEnabled(true);
        setCursor(Cursor.getDefaultCursor());
    }

    /**
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return isLocked;
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() throws CanceledException {
        dir.removeViewer(this);
        setVisible(false);
    }

    /**
     * set uptodate state
     *
     * @param flag
     */
    public void setUptoDate(boolean flag) {
        isUpToDate = flag;
    }

    /**
     * gets the list of selected fNames
     *
     * @return all selected fnames
     */
    public Collection<String> getSelectedFNames() {
        ArrayList<String> result = new ArrayList<>();
        for (String cName : cNames) {
            ICheckBoxCommand command = (ICheckBoxCommand) commandManager.getCommand(SetAnalyse4ViewerCommand.getName(cName));
            if (command.isSelected())
                result.add(cName);
        }
        return result;
    }

    /**
     * get the name of the class
     *
     * @return class name
     */
    @Override
    public String getClassName() {
        return "ImportBlastDialog";
    }

    public void setPairedReadSuffix1(String pairedReadSuffix1) {
        this.pairedReadSuffix1 = pairedReadSuffix1;
        ProgramProperties.put(MeganProperties.PAIRED_READ_SUFFIX1, pairedReadSuffix1);
    }

    public String getPairedReadSuffix1() {
        return pairedReadSuffix1;
    }

    public void setPairedReadSuffix2(String pairedReadSuffix2) {
        this.pairedReadSuffix2 = pairedReadSuffix2;
        ProgramProperties.put(MeganProperties.PAIRED_READ_SUFFIX2, pairedReadSuffix2);
    }

    public String getPairedReadSuffix2() {
        return pairedReadSuffix2;
    }

    /**
     * apply import from blast
     */
    public void apply() {
        ClassificationManager.get(Classification.Taxonomy, true).getIdMapper().setUseTextParsing(isParseTaxonNames());

        final String blastFileName = getBlastFileName();
        ProgramProperties.put(MeganProperties.BLASTFILE, new File(Basic.getLinesFromString(blastFileName).get(0)));
        final String readFileName = getReadFileName();
        String meganFileName = getMeganFileName();
        if (!meganFileName.startsWith("MS:::") && !(meganFileName.toLowerCase().endsWith(".rma")
                || meganFileName.toLowerCase().endsWith(".rma1") || meganFileName.toLowerCase().endsWith(".rma2")
                || meganFileName.toLowerCase().endsWith(".rma3") || meganFileName.toLowerCase().endsWith(".rma6")))
            meganFileName += ".rma";
        ProgramProperties.put(MeganProperties.MEGANFILE, new File(meganFileName));

        String blastFormat = getFormatCBox().getSelectedFormat();

        String blastMode = getAlignmentModeCBox().getSelectedMode();

        StringBuilder buf = new StringBuilder();
        buf.append("import blastFile=");

        if (blastFileName.length() > 0) {
            {
                java.util.List<String> fileNames = Basic.getLinesFromString(blastFileName);

                boolean first = true;
                for (String name : fileNames) {
                    File file = new File(name);
                    if (!(file.exists() && file.canRead())) {
                        NotificationsInSwing.showError(this, "Failed to open BLAST file for reading: " + name);
                        return;
                    }
                    if (first)
                        first = false;
                    else
                        buf.append(", ");
                    buf.append("'").append(name).append("'");
                }
            }

            if (blastFileName.length() > 0) {
                final String fileName = Basic.getFirstLine(blastFileName);
                if (blastFormat.equals(BlastFileFormat.Unknown.toString())) {
                    try {
                        String formatName = BlastFileFormat.detectFormat(this, fileName, true).toString();
                        if (formatName != null)
                            blastFormat = BlastFileFormat.valueOf(formatName).toString();
                        else
                            throw new IOException("Failed to detect BLAST format for file: " + fileName);
                    } catch (IOException e) {
                        Basic.caught(e);
                        return;
                    }
                }
                if (blastMode.equals(BlastMode.Unknown.toString())) {
                    try {
                        String modeName = BlastMode.detectMode(this, fileName, true).toString();
                        if (modeName != null)
                            blastMode = BlastMode.valueOf(modeName).toString();
                        else
                            throw new IOException("Failed to detect BLAST mode for file: " + fileName);
                    } catch (IOException e) {
                        Basic.caught(e);
                        return;
                    }
                }
            }

            if (readFileName.length() > 0) {
                buf.append(" fastaFile=");
                java.util.List<String> fileNames = Basic.getLinesFromString(readFileName);

                boolean first = true;
                for (String name : fileNames) {
                    File file = new File(name);
                    if (!(file.exists() && file.canRead())) {
                        NotificationsInSwing.showError(this, "Failed to open READs file for reading: " + name);
                        return;
                    }
                    if (first)
                        first = false;
                    else
                        buf.append(", ");
                    buf.append("'").append(name).append("'");
                }
            }
        }

        buf.append(" meganFile='").append(meganFileName).append("'");

        buf.append(" useCompression=").append(getUseCompressionCBox().isSelected());
        ProgramProperties.put("UseCompressInRMAFiles", getUseCompressionCBox().isSelected());

        buf.append(" format=").append(blastFormat);
        buf.append(" mode=").append(blastMode);

        int maxNumberOfMatchesPerRead;
        try {
            maxNumberOfMatchesPerRead = Integer.parseInt(getMaxNumberOfMatchesPerReadField().getText());
            ProgramProperties.put("MaxNumberMatchesPerRead", maxNumberOfMatchesPerRead);
        } catch (NumberFormatException ex) {
            maxNumberOfMatchesPerRead = ProgramProperties.get("MaxNumberMatchesPerRead", 50);
        }
        buf.append(" maxMatches=").append(maxNumberOfMatchesPerRead);
        buf.append(" minScore=").append(getMinScore());
        buf.append(" maxExpected=").append(getMaxExpected());
        buf.append(" minPercentIdentity=").append(getMinPercentIdentity());
        buf.append(" topPercent=").append(getTopPercent());
        if (getMinSupportPercent() > 0)
            buf.append(" minSupportPercent=").append(getMinSupportPercent());
        buf.append(" minSupport=").append(getMinSupport());
        buf.append(" weightedLCA=").append(isWeightedLCA());
        buf.append(" weightedLCAPercent=").append(getWeightedLCAPercent());
        buf.append(" minComplexity=").append(getMinComplexity());
        buf.append(" useIdentityFilter=").append(isUsePercentIdentityFilter());

        buf.append(" fNames=").append(Basic.toString(getSelectedFNames(), " "));

        buf.append(" paired=").append(isUsePairedReads());

        if (isUsePairedReads()) {
            String pattern1 = getPairedReadSuffix1();
            //String pattern2 = ProgramProperties.get(MeganProperties.PAIRED_READ_SUFFIX2, "");
            buf.append(" pairSuffixLength=").append(pattern1.length());
        }
        if (isUseReadMagnitudes()) {
            buf.append(" hasMagnitudes=").append(isUseReadMagnitudes());
        }

        if (getShortDescription().length() > 0)
            buf.append(" description='").append(getShortDescription()).append("'");

        buf.append(";");

        // delete existing files before we start the computation:
        File file = new File(meganFileName);
        if (file.exists()) {
            System.err.println("Removing file " + file.getPath() + ": " + file.delete());
            File rmazFile = new File(Basic.getFileSuffix(file.getPath()) + ".rmaz");
            if (rmazFile.exists())
                System.err.println("Removing file " + rmazFile.getPath() + ": " + rmazFile.delete());
            rmazFile = new File(file.getPath() + "z");
            if (rmazFile.exists())
                System.err.println("Removing file " + rmazFile.getPath() + ": " + rmazFile.delete());
        }

        setResult(buf.toString());
    }

    public boolean isAppliable() {
        return getBlastFileName().trim().length() > 0 && getMeganFileName().trim().length() > 0;
    }
}
