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
import jloda.swing.director.IDirectableViewer;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.RememberingComboBox;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.export.CSVExporter;
import megan.viewer.ClassificationViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Objects;

public class ExportCSVCommand extends CommandBase implements ICommand {
    private static final String EXPORT_CHOICE = "CSVExportChoice";
    private static final String COUNT_CHOICE = "CSVCount";
    private static final String SEPARATOR_CHOICE = "CSVSeperator";

    public enum Choice {assigned, summarized}

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export what=csv");

        final Director dir = getDir();
        final IDirectableViewer viewer = getViewer();
        final Document doc = dir.getDocument();
        final String classificationName;

        if (viewer instanceof ClassificationViewer)
            classificationName = viewer.getClassName();
        else
            classificationName = Classification.Taxonomy;

        final List<String> formats = CSVExporter.getFormats(classificationName, doc);

        np.matchIgnoreCase("format=");
        final String format = np.getWordMatchesIgnoringCase(Basic.toString(formats, " "));

        char separator = '\t';
        if (np.peekMatchIgnoreCase("separator")) {
            np.matchIgnoreCase("separator=");
            if (np.getWordMatchesIgnoringCase("comma tab").equalsIgnoreCase("comma"))
                separator = ',';
        }
        boolean reportSummarized = false;
        if (np.peekMatchIgnoreCase("counts")) {
            np.matchIgnoreCase("counts=");
            if (np.getWordMatchesIgnoringCase(Basic.toString(Choice.values(), " ")).equalsIgnoreCase(Choice.summarized.toString()))
                reportSummarized = true;
        }

        np.matchIgnoreCase("file=");
        String outputFile = np.getAbsoluteFileName();
        np.matchIgnoreCase(";");

        int count = CSVExporter.apply(dir, doc.getProgressListener(), new File(outputFile), format, separator, reportSummarized);

        NotificationsInSwing.showInformation(getViewer().getFrame(), "Wrote " + count + " line(s) to file: " + outputFile);
    }

    public boolean isApplicable() {
        return getViewer() instanceof ViewerBase && ((ViewerBase) getViewer()).getSelectedNodes().size() > 0;
    }

    public boolean isCritical() {
        return true;
    }

    public String getSyntax() {
        return "export what=CSV format={format} [separator={comma|tab}] [counts={assigned|summarized}] file=<filename>;";
    }

    public void actionPerformed(ActionEvent event) {
        final Director dir = getDir();

        final String[] choice = showChoices(getViewer().getFrame(), dir.getDocument(), getViewer().getClassName());
        if (choice == null)
            return;
        final String formatName = choice[0];
        final String countChoice = choice[1];
        final String separator = choice[2];

        final String name = Basic.replaceFileSuffix(dir.getDocument().getTitle(), "-ex.txt");

        File lastOpenFile = new File(name);
        final String lastDir = ProgramProperties.get("CSVDirectory", "");
        if (lastDir.length() > 0) {
            lastOpenFile = new File(lastDir, name);
        }

        final File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new TextFileFilter(), new TextFileFilter(), event, "Save as CSV (delimiter-separated values)", ".txt");

        if (file != null) {
            final String cmd = ("export what=CSV format=" + formatName + " separator=" + separator
                    + (countChoice == null ? "" : " counts=" + countChoice) + " file='" + file.getPath() + "';");
            execute(cmd);
            ProgramProperties.put("CSVDirectory", file.getParent());
        }
    }

    public String getName() {
        return "Text (CSV) Format...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Export assignments of reads to nodes to a CSV (comma or tab-separated value) file";
    }

    /**
     * show the choices dialog
     *
     * @param parent
     * @param classification
     * @return choices or null
     */
    private static String[] showChoices(Component parent, Document doc, String classification) {
        final boolean doTaxonomy = classification.equalsIgnoreCase(Classification.Taxonomy);

        final List<String> formats = CSVExporter.getFormats(classification, doc);
        final JLabel label0 = new JLabel("Choose data to export:  ");
        label0.setToolTipText("Choose data to export");
        final RememberingComboBox choice0 = new RememberingComboBox();
        choice0.setEditable(false);
        choice0.addItems(formats);
        choice0.setToolTipText("Choose data to export");
        if (choice0.getItemCount() > 0)
            choice0.setSelectedItem(ProgramProperties.get(EXPORT_CHOICE, choice0.getItemAt(0)));

        final JLabel label1 = new JLabel("Choose count to use:  ");
        label1.setToolTipText("Choose count to use, summarized or assigned");
        final RememberingComboBox choice1 = new RememberingComboBox();
        choice1.setEditable(false);
        choice1.addItem(Choice.assigned.toString());
        choice1.addItem(Choice.summarized.toString());
        choice1.setToolTipText("Choose count to use, summarized or assigned");
        if (choice1.getItemCount() > 0)
            choice1.setSelectedItem(ProgramProperties.get(COUNT_CHOICE, choice1.getItemAt(0)));

        final JLabel label2 = new JLabel("Choose separator to use:  ");
        label2.setToolTipText("Choose separator to use");
        final RememberingComboBox choice2 = new RememberingComboBox();
        choice2.setEditable(false);
        choice2.addItem("tab");
        choice2.addItem("comma");
        choice2.setToolTipText("Choose separator to use");
        if (choice2.getItemCount() > 0)
            choice2.setSelectedItem(ProgramProperties.get(SEPARATOR_CHOICE, choice2.getItemAt(0)));

        final JPanel myPanel = new JPanel();
        myPanel.setLayout(new GridLayout((doTaxonomy ? 3 : 2), 2));
        myPanel.add(label0);
        myPanel.add(choice0);

        if (doTaxonomy) {
            myPanel.add(label1);
            myPanel.add(choice1);
        }

        myPanel.add(label2);
        myPanel.add(choice2);

        final int result = JOptionPane.showConfirmDialog(parent, myPanel, "MEGAN - Export to CSV", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                ProgramProperties.getProgramIcon());
        if (result == JOptionPane.OK_OPTION) {
            ProgramProperties.put(EXPORT_CHOICE, Objects.requireNonNull(choice0.getSelectedItem()).toString());
            ProgramProperties.put(COUNT_CHOICE, Objects.requireNonNull(choice1.getSelectedItem()).toString());
            ProgramProperties.put(SEPARATOR_CHOICE, Objects.requireNonNull(choice2.getSelectedItem()).toString());

            return new String[]{choice0.getCurrentText(false), choice1.getCurrentText(false), choice2.getCurrentText(false)};
        }
        return null;
    }
}

