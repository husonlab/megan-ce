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
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.core.Document;
import megan.util.ExportStamp;
import megan.util.StampFileFilter;
import megan.viewer.MainViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * export as STAMP profile
 * Daniel Huson, 1.2016
 */
public class ExportStampProfileCommand extends CommandBase implements ICommand {
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export format=stamp");

        Director dir = getDir();
        Document doc = dir.getDocument();

        np.matchIgnoreCase("data=");
        String data = np.getWordMatchesIgnoringCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));

        np.matchIgnoreCase("file=");
        String outputFile = np.getAbsoluteFileName();
        boolean allLevels = false;
        if (np.peekMatchIgnoreCase("allLevels")) {
            np.matchIgnoreCase("allLevels=");
            allLevels = np.getBoolean();
        }
        np.matchIgnoreCase(";");
        final int numberOfRows = ExportStamp.apply(getDir(), data, new File(outputFile), allLevels, doc.getProgressListener());
        NotificationsInSwing.showInformation(getViewer().getFrame(), String.format("Exported %,d rows in STAMP profile format", numberOfRows));
    }

    public boolean isApplicable() {
        return getDoc().getNumberOfReads() > 0 && getViewer() instanceof ViewerBase
                && ((ViewerBase) getViewer()).getSelectedNodes().size() > 0;
    }

    public boolean isCritical() {
        return true;
    }

    public String getSyntax() {
        return "export format=stamp data={" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|") + "} file=<filename>;";
    }

    public void actionPerformed(ActionEvent event) {
        if (!(getViewer() instanceof ViewerBase))
            return;

        final ViewerBase viewer = (ViewerBase) getViewer();
        final Director dir = getDir();

        final String choice;
        if (viewer instanceof MainViewer)
            choice = Classification.Taxonomy;
        else
            choice = (getViewer()).getClassName();

        boolean allLevels = false;
        switch (JOptionPane.showConfirmDialog(viewer.getFrame(), "Export all levels above selected nodes as well?", "Export to STAMP option", JOptionPane.YES_NO_CANCEL_OPTION)) {
                case JOptionPane.CANCEL_OPTION:
                    return;
                case JOptionPane.YES_OPTION:
                    allLevels = true;
        }

        String name = Basic.replaceFileSuffix(dir.getDocument().getTitle(), "-" + choice + ".spf");

        File lastOpenFile = new File(name);
        String lastDir = ProgramProperties.get("StampDirectory", "");
        if (lastDir.length() > 0) {
            lastOpenFile = new File(lastDir, name);
        }

        final File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new StampFileFilter(), new StampFileFilter(), event, "Save as STAMP profile file", ".spf");

        if (file != null) {
            if (viewer.getSelectedNodes().size() == 0)
                executeImmediately("select nodes=leaves;");
            ProgramProperties.put("StampDirectory", file.getParent());
            execute("export format=stamp data=" + choice + " file='" + file.getPath() + "' allLevels=" + allLevels + ";");
        }
    }

    public String getName() {
        return "STAMP Format...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Export selected nodes as a STAMP profile file (.spf)";

    }
}

