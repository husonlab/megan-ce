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
import jloda.swing.commands.ICheckBoxCommand;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.util.StatusBar;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.BlastMode;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.commandtemplates.SetAnalyse4ViewerCommand;
import megan.classification.data.ClassificationCommandHelper;
import megan.core.Director;
import megan.core.Document;
import megan.importblast.commands.*;
import megan.main.MeganProperties;
import megan.parsers.blast.BlastFileFormat;
import megan.parsers.blast.BlastModeUtils;

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

    private final JTextField minScoreField = new JTextField(8);
    private final JTextField maxExpectedField = new JTextField(8);
    private final JTextField minPercentIdentityField = new JTextField(8);

    private final JTextField topPercentField = new JTextField(8);
    private final JTextField minSupportField = new JTextField(8);
    private final JTextField minSupportPercentField = new JTextField(8);

    private final JComboBox<String> lcaAlgorithmComboBox = new JComboBox<>();
    private final JComboBox<String> readAssignmentModeComboBox = new JComboBox<>();


    private final JTextField lcaCoveragePercentField = new JTextField(8);
    private final JTextField minComplexityField = new JTextField(8);
    private final JTextField minPercentReadCoveredField = new JTextField(8);
    private final JTextField minPercentReferenceCoveredField = new JTextField(8);


    private boolean usePairedReads = false;
    private String pairedReadSuffix1 = ProgramProperties.get(MeganProperties.PAIRED_READ_SUFFIX1, "");
    private String pairedReadSuffix2 = ProgramProperties.get(MeganProperties.PAIRED_READ_SUFFIX2, "");

    private boolean longReads = false;

    private boolean useReadMagnitudes = false;
    private boolean parseTaxonNames = ProgramProperties.get(MeganProperties.PARSE_TAXON_NAMES, true);

    private boolean usePercentIdentityFilter = false;

    private String contaminantsFileName = ProgramProperties.get(MeganProperties.CONTAMINANT_FILE, "");

    private boolean useContaminantsFilter = false;

    private final JTextArea blastFileNameField = new JTextArea();

    private final FormatCBox formatCBox = new FormatCBox();

    private final ModeCBox alignmentModeCBox = new ModeCBox();

    private final JTextField shortDescriptionField = new JTextField(8);

    private final JTextArea readFileNameField = new JTextArea();

    private final JTextField meganFileNameField = new JTextField();

    private final JTextField maxNumberOfMatchesPerReadField = new JTextField(8);
    private final JLabel maxNumberOfMatchesPerReadLabel = new JLabel("Max number of matches per read:");

    private final JCheckBox useCompressionCBox = new JCheckBox();

    private final JTabbedPane tabbedPane = new JTabbedPane();

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
        this(parent, dir, ClassificationManager.getAllSupportedClassifications(), title);
    }

    /**
     * constructor
     *
     * @param parent
     * @param dir
     * @param cNames0
     * @param title
     */
    public ImportBlastDialog(Component parent, Director dir, Collection<String> cNames0, final String title) {
        this.dir = dir;
        final boolean showTaxonomyPane;

        this.cNames.addAll(cNames0);
        if (this.cNames.contains(Classification.Taxonomy)) {
            showTaxonomyPane = true;
            this.cNames.remove(Classification.Taxonomy);
        } else
            showTaxonomyPane = false;

        dir.addViewer(this);
        setIconImages(ProgramProperties.getProgramIconImages());

        commandManager = new CommandManager(dir, this, new String[]{"megan.commands", "megan.importblast.commands"}, !ProgramProperties.isUseGUI());
        commandManager.addCommands(this, ClassificationCommandHelper.getImportBlastCommands(cNames0), true);

        setMinScore(Document.DEFAULT_MINSCORE);
        setTopPercent(Document.DEFAULT_TOPPERCENT);
        setMaxExpected(Document.DEFAULT_MAXEXPECTED);
        setMinPercentIdentity(Document.DEFAULT_MIN_PERCENT_IDENTITY);
        setMinSupportPercent(0);
        setMinSupport(Document.DEFAULT_MINSUPPORT);
        setMinPercentReadToCover(Document.DEFAULT_MIN_PERCENT_READ_TO_COVER);
        setMinPercentReferenceToCover(Document.DEFAULT_MIN_PERCENT_REFERENCE_TO_COVER);
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
        setMinPercentReadToCover(doc.getMinPercentReadToCover());
        setMinPercentReferenceToCover(doc.getMinPercentReferenceToCover());
        setMinComplexity(doc.getMinComplexity());
        setUsePercentIdentityFilter(doc.isUseIdentityFilter());
        setLCACoveragePercent(doc.getLcaCoveragePercent());
        setLcaAlgorithm(doc.getLcaAlgorithm());

        // set opposite then call command to toggle; this is to setup LCA for long reads
        if (doc.isLongReads()) {
            setLongReads(!doc.isLongReads());
            commandManager.getCommand(SetLongReadsCommand.NAME).actionPerformed(null);
        } else
            setLongReads(false);

        ArrayList<String> toDelete = new ArrayList<>();
        for (String cName : doc.getActiveViewers()) { // turn of classifications for which mappers have not been loaded
            if (!cName.equals(Classification.Taxonomy) && !ClassificationManager.get(cName, true).getIdMapper().hasActiveAndLoaded())
                toDelete.add(cName);
        }
        doc.getActiveViewers().removeAll(toDelete);

        setLocationRelativeTo(parent);
        setTitle(title);
        setModal(true);
        setSize(900, 700);

        getContentPane().setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        addFilesTab(tabbedPane);

        if (showTaxonomyPane)
            tabbedPane.addTab(Classification.Taxonomy, new ViewerPanel(commandManager, Classification.Taxonomy));
        for (String fName : cNames) {
            tabbedPane.addTab(fName, new ViewerPanel(commandManager, fName));
        }

        addLCATab(tabbedPane);

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

        tabbedPane.addChangeListener(event -> tabNumber.setText("Tab " + (tabbedPane.getSelectedIndex() + 1) + " of " + tabbedPane.getTabCount()));

        getCommandManager().updateEnableState();
    }

    protected void addFilesTab(JTabbedPane tabbedPane) {
        tabbedPane.addTab("Files", new FilesPanel(this));
    }

    protected void addLCATab(JTabbedPane tabbedPane) {
        tabbedPane.addTab("LCA Params", new LCAParametersPanel(this));
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

    protected double getMinScore() {
        double value = Document.DEFAULT_MINSCORE;
        try {
            value = Double.parseDouble(minScoreField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    private void setMinScore(double value) {
        minScoreField.setText("" + (float) value);
    }

    protected double getTopPercent() {
        double value = Document.DEFAULT_TOPPERCENT;
        try {
            value = Double.parseDouble(topPercentField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    private void setTopPercent(double value) {
        topPercentField.setText("" + (float) value);
    }

    protected double getMaxExpected() {
        double value = Document.DEFAULT_MAXEXPECTED;
        try {
            value = Double.parseDouble(maxExpectedField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    private void setMaxExpected(double value) {
        maxExpectedField.setText("" + (float) value);
    }

    protected double getMinPercentIdentity() {
        double value = Document.DEFAULT_MIN_PERCENT_IDENTITY;
        try {
            value = Double.parseDouble(minPercentIdentityField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    private void setMinPercentIdentity(double value) {
        minPercentIdentityField.setText("" + (float) value);
    }

    protected int getMinSupport() {
        int value = Document.DEFAULT_MINSUPPORT;
        try {
            value = Integer.parseInt(minSupportField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return Math.max(1, value);
    }

    private void setMinSupport(int value) {
        minSupportField.setText("" + Math.max(0, value));
    }

    protected float getMinSupportPercent() {
        float value = 0;
        try {
            value = Basic.parseFloat(minSupportPercentField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return Math.max(0f, value);
    }

    private void setMinSupportPercent(float value) {
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

    private String shortDescription = "";

    protected String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    private String blastFileName = "";

    public void setBlastFileName(String name) {
        blastFileName = name;
    }

    public String getBlastFileName() {
        return blastFileName;
    }

    private String readFileName = "";

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

    private String meganFileName = "";

    public void setMeganFileName(String name) {
        meganFileName = name;
    }

    private String getMeganFileName() {
        return meganFileName;
    }

    public boolean isUsePairedReads() {
        return usePairedReads;
    }

    public void setUsePairedReads(boolean usePairedReads) {
        this.usePairedReads = usePairedReads;
    }

    public boolean isParseTaxonNames() {
        return parseTaxonNames;
    }

    public void setParseTaxonNames(boolean parseTaxonNames) {
        this.parseTaxonNames = parseTaxonNames;
        ProgramProperties.put(MeganProperties.PARSE_TAXON_NAMES, parseTaxonNames);
    }


    public String getContaminantsFileName() {
        return contaminantsFileName;
    }

    public void setContaminantsFileName(String contaminantsFileName) {
        this.contaminantsFileName = contaminantsFileName;
    }

    public boolean isUseContaminantsFilter() {
        return contaminantsFileName != null && useContaminantsFilter;
    }

    public void setUseContaminantsFilter(boolean useContaminantsFilter) {
        this.useContaminantsFilter = useContaminantsFilter;
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

    public Document.LCAAlgorithm getLcaAlgorithm() {
        return Document.LCAAlgorithm.valueOfIgnoreCase((String) lcaAlgorithmComboBox.getSelectedItem());
    }

    public void setReadAssignmentMode(Document.ReadAssignmentMode readAssignmentMode) {
        readAssignmentModeComboBox.setSelectedItem(readAssignmentMode.toString());
    }

    protected Document.ReadAssignmentMode getReadAssignmentMode() {
        return Document.ReadAssignmentMode.valueOfIgnoreCase((String) readAssignmentModeComboBox.getSelectedItem());
    }

    public void setLcaAlgorithm(Document.LCAAlgorithm lcaAlgorithm) {
        lcaAlgorithmComboBox.setSelectedItem(lcaAlgorithm.toString());
    }

    protected double getLCACoveragePercent() {
        double value = Document.DEFAULT_LCA_COVERAGE_PERCENT;
        try {
            value = Basic.parseDouble(lcaCoveragePercentField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    private void setLCACoveragePercent(double value) {
        lcaCoveragePercentField.setText("" + (float) Math.max(0, Math.min(100, value)));
    }

    public JTextField getLcaCoveragePercentField() {
        return lcaCoveragePercentField;
    }

    public JTextField getMinPercentReadToCoverField() {
        return minPercentReadCoveredField;
    }

    protected double getMinPercentReadToCover() {
        double value = Document.DEFAULT_MIN_PERCENT_READ_TO_COVER;
        try {
            value = Basic.parseDouble(minPercentReadCoveredField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    private void setMinPercentReadToCover(double value) {
        minPercentReadCoveredField.setText("" + (float) Math.max(0, Math.min(100, value)));
    }

    public JTextField getMinPercentReferenceToCoverField() {
        return minPercentReferenceCoveredField;
    }

    protected double getMinPercentReferenceToCover() {
        double value = Document.DEFAULT_MIN_PERCENT_REFERENCE_TO_COVER;
        try {
            value = Basic.parseDouble(minPercentReferenceCoveredField.getText());
        } catch (NumberFormatException e) {
            Basic.caught(e);
        }
        return value;
    }

    private void setMinPercentReferenceToCover(double value) {
        minPercentReferenceCoveredField.setText("" + (float) Math.max(0, Math.min(100, value)));
    }

    public JTextField getMaxNumberOfMatchesPerReadField() {
        return maxNumberOfMatchesPerReadField;
    }

    public JLabel getMaxNumberOfMatchesPerReadLabel() {
        return maxNumberOfMatchesPerReadLabel;
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

    public JComboBox<String> getLcaAlgorithmComboBox() {
        return lcaAlgorithmComboBox;
    }

    public JComboBox<String> getReadAssignmentModeComboBox() {
        return readAssignmentModeComboBox;
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
     * gets the list of selected cNames
     *
     * @return all selected fnames
     */
    protected Collection<String> getSelectedFNames() {
        final ArrayList<String> result = new ArrayList<>();
        for (String cName : cNames) {
            final ICheckBoxCommand command = (ICheckBoxCommand) commandManager.getCommand(SetAnalyse4ViewerCommand.getName(cName));
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

    protected String getPairedReadSuffix1() {
        return pairedReadSuffix1;
    }

    public void setPairedReadSuffix2(String pairedReadSuffix2) {
        this.pairedReadSuffix2 = pairedReadSuffix2;
        ProgramProperties.put(MeganProperties.PAIRED_READ_SUFFIX2, pairedReadSuffix2);
    }

    public String getPairedReadSuffix2() {
        return pairedReadSuffix2;
    }

    public boolean isLongReads() {
        return longReads;
    }

    public void setLongReads(boolean longReads) {
        this.longReads = longReads;
    }

    /**
     * apply import from blast
     */
    public void apply() throws CanceledException, IOException {
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

            final String fileName = Basic.getFirstLine(blastFileName);
            if (blastFormat.equals(BlastFileFormat.Unknown.toString())) {
                String formatName = BlastFileFormat.detectFormat(this, fileName, true).toString();
                if (formatName != null)
                    blastFormat = BlastFileFormat.valueOf(formatName).toString();
                else
                    throw new IOException("Failed to detect BLAST format for file: " + fileName);
            }
            if (blastMode.equals(BlastMode.Unknown.toString())) {
                BlastMode mode = BlastModeUtils.detectMode(this, fileName, true);
                if (mode == null) // user canceled
                    throw new CanceledException();
                else if (mode == BlastMode.Unknown)
                    throw new IOException("Failed to detect BLAST mode for file: " + fileName);
                else
                    blastMode = mode.toString();
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
        if (!isLongReads()) {
            try {
                maxNumberOfMatchesPerRead = Integer.parseInt(getMaxNumberOfMatchesPerReadField().getText());
                ProgramProperties.put("MaxNumberMatchesPerRead", maxNumberOfMatchesPerRead);
            } catch (NumberFormatException ex) {
                maxNumberOfMatchesPerRead = ProgramProperties.get("MaxNumberMatchesPerRead", 50);
            }
            buf.append(" maxMatches=").append(maxNumberOfMatchesPerRead);
        }
        buf.append(" minScore=").append(getMinScore());
        buf.append(" maxExpected=").append(getMaxExpected());
        buf.append(" minPercentIdentity=").append(getMinPercentIdentity());
        buf.append(" topPercent=").append(getTopPercent());
        buf.append(" minSupportPercent=").append(getMinSupportPercent());
        buf.append(" minSupport=").append(getMinSupport());
        buf.append(" lcaAlgorithm=").append(getLcaAlgorithm().toString());
        if (getLcaAlgorithm() == Document.LCAAlgorithm.weighted || getLcaAlgorithm() == Document.LCAAlgorithm.longReads)
            buf.append(" lcaCoveragePercent=").append(getLCACoveragePercent());
        buf.append(" minPercentReadToCover=").append(getMinPercentReadToCover());
        buf.append(" minPercentReferenceToCover=").append(getMinPercentReferenceToCover());
        buf.append(" minComplexity=").append(getMinComplexity());
        buf.append(" useIdentityFilter=").append(isUsePercentIdentityFilter());

        buf.append(" readAssignmentMode=").append(getReadAssignmentMode());

        buf.append(" fNames=").append(Basic.toString(getSelectedFNames(), " "));

        if (isLongReads())
            buf.append(" longReads=").append(isLongReads());

        if (isUsePairedReads())
            buf.append(" paired=").append(isUsePairedReads());

        if (isUsePairedReads()) {
            String pattern1 = getPairedReadSuffix1();
            //String pattern2 = ProgramProperties.get(MeganProperties.PAIRED_READ_SUFFIX2, "");
            buf.append(" pairSuffixLength=").append(pattern1.length());
        }

        if (isUseContaminantsFilter() && getContaminantsFileName() != null) {
            buf.append(" contaminantsFile='").append(getContaminantsFileName()).append("'");
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
