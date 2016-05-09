/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import jloda.gui.ChooseFileDialog;
import jloda.gui.commands.ICommand;
import jloda.gui.director.IDirectableViewer;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.TextFileFilter;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.export.CSVExporter;
import megan.fx.NotificationsInSwing;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

public class ExportCSVCommand extends CommandBase implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export what=csv");

        final Director dir = getDir();
        final IDirectableViewer viewer = getViewer();
        final Document doc = dir.getDocument();
        final String classificationName;

        if (viewer instanceof ClassificationViewer)
            classificationName = ((ClassificationViewer) viewer).getClassName();
        else
            classificationName = Classification.Taxonomy;


        final List<String> formats = CSVExporter.getFormats(classificationName, doc.getMeganFile().hasDataConnector());

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
            if (np.getWordMatchesIgnoringCase("assigned summarized").equalsIgnoreCase("summarized"))
                reportSummarized = true;
        }

        np.matchIgnoreCase("file=");
        String outputFile = np.getAbsoluteFileName();
        np.matchIgnoreCase(";");

        int count = CSVExporter.apply(viewer.getFrame(), dir, doc.getProgressListener(), new File(outputFile), format, separator, reportSummarized);

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
        Director dir = getDir();
        String formatName = null;
        String countChoice = null;

        if (getViewer() instanceof MainViewer) {
            List<String> formats = CSVExporter.getFormats(Classification.Taxonomy, getDoc().getMeganFile().hasDataConnector());

            formatName = CSVExporter.showFormatInputDialog(getViewer().getFrame(), formats);
            if (formatName == null)
                return;
            if (formatName.contains("count")) {
                String[] countNames = new String[]{"assigned", "summarized"};
                String previous = ProgramProperties.get("CSVCount", "assigned");
                countChoice = (String) JOptionPane.showInputDialog(getViewer().getFrame(),
                        "Choose count to use", "Export read assignments to CSV file",
                        JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), countNames, previous);
                if (countChoice == null)
                    return;
                ProgramProperties.put("CSVCount", countChoice);

            }
        } else if (getViewer() instanceof ClassificationViewer) {
            String classificationName = ((ClassificationViewer) getViewer()).getClassName();
            List<String> formats = CSVExporter.getFormats(classificationName, getDoc().getMeganFile().hasDataConnector());

            formatName = CSVExporter.showFormatInputDialog(getViewer().getFrame(), formats);
            if (formatName == null)
                return;
        }
        String separator;
        {
            String[] separatorNames = new String[]{"tab", "comma"};
            String previousSeparator = ProgramProperties.get("CSVSeparator", "tab");
            separator = (String) JOptionPane.showInputDialog(getViewer().getFrame(),
                    "Choose separator", "Export read assignments to CSV file",
                    JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), separatorNames, previousSeparator);
            if (separator == null)
                return;
            ProgramProperties.put("CSVSeparator", separator);
        }

        String name = Basic.replaceFileSuffix(dir.getDocument().getTitle(), "-ex.txt");

        File lastOpenFile = new File(name);
        String lastDir = ProgramProperties.get("CSVDirectory", "");
        if (lastDir.length() > 0) {
            lastOpenFile = new File(lastDir, name);
        }

        File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new TextFileFilter(), new TextFileFilter(), event, "Save as CSV (delimiter-separated values)", ".txt");

        if (file != null) {
            String cmd;
            cmd = ("export what=CSV format=" + formatName + " separator=" + separator
                    + (countChoice == null ? "" : " counts=" + countChoice) + " file='" + file.getPath() + "';");
            execute(cmd);
            ProgramProperties.put("CSVDirectory", file.getParent());
        }
    }

    public String getName() {
        return "CSV Format...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Export16.gif");
    }

    public String getDescription() {
        return "Export assignments of reads to nodes to a CSV (comma or tab-separated value) file";
    }
}

