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
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.dialogs.importcsv.ImportCSVWindow;
import megan.dialogs.parameters.ParametersDialogSmall;
import megan.inspector.InspectorWindow;
import megan.main.MeganProperties;
import megan.parsers.CSVReadsHitsParser;
import megan.parsers.CSVSummaryParser;
import megan.viewer.MainViewer;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class ImportCSVCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "import csv={reads|summary} separator={comma|tab} file=<fileName> fNames={" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|")
                + ",...} [topPercent=<num>] [minScore=<num>] [minSupportPercent=<num>]  [minSupport=<num>] [multiplier=<number>];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        final Director dir = getDir();
        final MainViewer viewer = dir.getMainViewer();
        final Document doc = dir.getDocument();

        np.matchIgnoreCase("import csv=");
        String choice = np.getWordMatchesIgnoringCase("reads summary");

        boolean tabSeparator = false;
        if (np.peekMatchIgnoreCase("separator")) {
            np.matchIgnoreCase("separator=");
            if (np.getWordMatchesIgnoringCase("comma tab").equals("tab"))
                tabSeparator = true;
        }

        np.matchIgnoreCase("file=");
        final String fileName = np.getAbsoluteFileName();

        final List<String> cNames = new LinkedList<>();
        np.matchIgnoreCase("fNames=");
        while (!np.peekMatchIgnoreCase(";")) {
            String cName = np.getWordRespectCase();
            if (ClassificationManager.getAllSupportedClassifications().contains(cName)) {
                cNames.add(cName);
            } else {
                np.pushBack();
                break;
            }
        }

        if (choice.equalsIgnoreCase("reads")) {
            float topPercent = -1;
            float minScore = -1;
            int minSupport = -1;
            float minSupportPercent = -1;
            float minComplexity = -1;
            if (np.peekMatchIgnoreCase("topPercent")) {
                np.matchIgnoreCase("topPercent=");
                topPercent = (float) np.getDouble();
            }
            if (np.peekMatchIgnoreCase("minScore")) {
                np.matchIgnoreCase("minScore=");
                minScore = (float) np.getDouble();
            }
            if (np.peekMatchIgnoreCase("minSupportPercent")) {
                np.matchIgnoreCase("minSupportPercent=");
                minSupportPercent = (float) np.getDouble(0, 100);
            }
            if (np.peekMatchIgnoreCase("minSupport")) {
                np.matchIgnoreCase("minSupport=");
                minSupport = np.getInt();
            }
            np.matchIgnoreCase(";");

            if (!ProgramProperties.isUseGUI() || doc.neverOpenedReads) {
                doc.neverOpenedReads = false;
                doc.clearReads();
                if (topPercent != -1)
                    doc.setTopPercent(topPercent);
                if (minScore != -1)
                    doc.setMinScore(minScore);
                if (minSupportPercent != -1)
                    doc.setMinSupportPercent(minSupportPercent);
                if (minSupport != -1)
                    doc.setMinSupport(minSupport);

                CSVReadsHitsParser.apply(fileName, doc, cNames.toArray(new String[0]), tabSeparator);

                if (dir.getViewerByClass(InspectorWindow.class) != null)
                    ((InspectorWindow) dir.getViewerByClass(InspectorWindow.class)).clear();
                viewer.collapseToDefault();
                doc.getMeganFile().setFileName(Basic.replaceFileSuffix(fileName, ".megan"));
                doc.getMeganFile().setFileType(MeganFile.Type.MEGAN_SUMMARY_FILE);
                doc.getActiveViewers().clear();
                doc.getActiveViewers().addAll(cNames);
                doc.processReadHits();
                if (doc.getNumberOfReads() > 0)
                    doc.setDirty(true);
                viewer.getNodeDrawer().setStyle(doc.getNumberOfSamples() > 1 ? NodeDrawer.Style.PieChart : NodeDrawer.Style.Circle);
                viewer.setDoReInduce(true);
                viewer.setDoReset(true);
                dir.executeImmediately("update;", viewer.getCommandManager());
                NotificationsInSwing.showInformation(String.format("Imported %,d reads from file '%s'", +doc.getNumberOfReads(), fileName));
            } else {
                Director newDir = Director.newProject();
                newDir.getMainViewer().getFrame().setVisible(true);
                newDir.getMainViewer().setDoReInduce(true);
                newDir.getMainViewer().setDoReset(true);
                newDir.execute("import csv=reads separator=" + (tabSeparator ? "tab" : "comma") + " file='"
                                + fileName + "' fNames=" + Basic.toString(cNames, " ")
                                + " topPercent=" + topPercent + " minScore=" + minScore + " minSupportPercent=" + minSupportPercent
                                + " minSupport=" + minSupport + ";",
                        newDir.getMainViewer().getCommandManager());
            }
        } else // csv-summary
        {
            long multiplier = 1;
            if (np.peekMatchIgnoreCase("multiplier")) {
                np.matchIgnoreCase("multiplier=");
                multiplier = np.getInt(1, Integer.MAX_VALUE);
            }
            np.matchIgnoreCase(";");
            if (!ProgramProperties.isUseGUI() || doc.neverOpenedReads) {
                doc.neverOpenedReads = false;
                doc.clearReads();

                CSVSummaryParser.apply(fileName, doc, cNames.toArray(new String[0]), tabSeparator, multiplier);
                if (dir.getViewerByClass(InspectorWindow.class) != null)
                    ((InspectorWindow) dir.getViewerByClass(InspectorWindow.class)).clear();
                viewer.collapseToDefault();
                doc.getMeganFile().setFileName(Basic.getFileBaseName(fileName) + ".megan");
                doc.getMeganFile().setFileType(MeganFile.Type.MEGAN_SUMMARY_FILE);
                if (doc.getNumberOfReads() > 0)
                    doc.setDirty(true);
                if (doc.getNumberOfSamples() > 1)
                    viewer.getNodeDrawer().setStyle(ProgramProperties.get(MeganProperties.COMPARISON_STYLE, ""), NodeDrawer.Style.PieChart);
                viewer.setDoReInduce(true);
                viewer.setDoReset(true);
                dir.executeImmediately("update;", viewer.getCommandManager());
                NotificationsInSwing.showInformation(String.format("Imported %,d reads from file '%s'", +doc.getNumberOfReads(), fileName));
            } else {
                Director newDir = Director.newProject();
                newDir.getMainViewer().getFrame().setVisible(true);
                newDir.getMainViewer().setDoReInduce(true);
                newDir.getMainViewer().setDoReset(true);
                newDir.executeImmediately("import csv=summary separator=" + (tabSeparator ? "tab" : "comma") + " file='"
                        + fileName + "' fNames=" + Basic.toString(cNames, " ")
                        + (multiplier != 1 ? " multiplier=" + multiplier : "") + ";", newDir.getMainViewer().getCommandManager());
            }
        }
    }

    public void actionPerformed(ActionEvent event) {
        File lastOpenFile = ProgramProperties.getFile(MeganProperties.CSVFILE);

        List<File> files = ChooseFileDialog.chooseFilesToOpen(getViewer().getFrame(), lastOpenFile,
                new TextFileFilter("csv", "tsv", "csv", "tab"), new TextFileFilter("csv", "tsv", "csv", "tab"), event, "Open CSV file");
        if (files.size() > 0) {

            File file = files.get(0);
            ImportCSVWindow importCSVWindow = new ImportCSVWindow(getViewer(), getDir());
            importCSVWindow.setTabSeparator(CSVSummaryParser.guessTabSeparator(file));
            importCSVWindow.setDoReadsHits(CSVSummaryParser.getTokensPerLine(file, importCSVWindow.isTabSeparator() ? "\t" : ",") == 3);
            if (importCSVWindow.apply()) {
                String template;
                if (importCSVWindow.isDoReadsHits()) {
                    ParametersDialogSmall dialog = new ParametersDialogSmall(getViewer().getFrame(), getDir());
                    if (!dialog.isOk())
                        return;
                    float topPercent = (float) dialog.getTopPercent();
                    float minScore = (float) dialog.getMinScore();
                    int minSupport = dialog.getMinSupport();
                    float minSupportPercent = dialog.getMinSupportPercent();

                    template = ("import csv=reads separator=" + (importCSVWindow.isTabSeparator() ? "tab" : "comma") + " file='XXXXXXXX' fNames="
                            + Basic.toString(importCSVWindow.getSelectedCNames(), " ")
                            + " topPercent=" + topPercent + " minScore=" + minScore + " minSupportPercent=" + minSupportPercent + " minSupport=" + minSupport + ";\n");
                } else {
                    template = ("import csv=summary separator=" + (importCSVWindow.isTabSeparator() ? "tab" : "comma") + " file='XXXXXXXX' fNames="
                            + Basic.toString(importCSVWindow.getSelectedCNames(), " ")
                            + (importCSVWindow.getMultiplier() != 1 ? " multiplier=" + importCSVWindow.getMultiplier() : "") + ";\n");
                }
                StringBuilder buf = new StringBuilder();
                for (File aFile : files) {
                    String cName = Basic.protectBackSlashes(aFile.getPath());
                    buf.append(template.replaceAll("XXXXXXXX", cName));
                }
                execute(buf.toString());
            }
            ProgramProperties.put(MeganProperties.CSVFILE, file.getPath());
        }
    }

    public boolean isApplicable() {
        return getViewer() != null;
    }

    public String getName() {
        return "Text (CSV) Format...";
    }

    public String getAltName() {
        return "Import Text (CSV) Format...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Import16.gif");
    }

    public String getDescription() {
        return "Load data in CSV (comma- or tab-separated value) format: READ_NAME,CLASS-NAME,SCORE or CLASS,COUNT(,COUNT...)";
    }

    public boolean isCritical() {
        return true;
    }
}

