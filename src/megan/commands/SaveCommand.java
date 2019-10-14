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
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;
import megan.classification.data.SyncDataTableAndClassificationViewer;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.main.MeganProperties;
import megan.samplesviewer.SamplesViewer;
import megan.util.MeganFileFilter;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;
import megan.viewer.SyncDataTableAndTaxonomy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;


public class SaveCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "save file=<filename> [summary={true|false}];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        MainViewer viewer = getDir().getMainViewer();
        Director dir = getDir();
        Document doc = dir.getDocument();
        String fileName = null;
        try {
            np.matchIgnoreCase("save file=");
            fileName = np.getAbsoluteFileName();
            boolean summary = true;
            if (np.peekMatchIgnoreCase("summary")) {
                np.matchIgnoreCase("summary=");
                summary = np.getBoolean();
            }
            np.matchIgnoreCase(";");
            File file = new File(fileName);
            doc.getProgressListener().setTasks("Saving MEGAN file", file.getName());

            if (summary) {
                final SamplesViewer sampleViewer = (SamplesViewer) getDir().getViewerByClass(SamplesViewer.class);
                if (sampleViewer != null) {

                    sampleViewer.getSamplesTableView().syncFromViewToDocument();
                }

                if (viewer != null)
                    SyncDataTableAndTaxonomy.syncFormattingFromViewer2Summary(viewer, doc.getDataTable());
                for (String cName : ClassificationManager.getAllSupportedClassifications()) {
                    if (dir.getViewerByClassName(ClassificationViewer.getClassName(cName)) != null && dir.getViewerByClassName(ClassificationViewer.getClassName(cName)) instanceof ClassificationViewer) {
                        ClassificationViewer classificationViewer = (ClassificationViewer) dir.getViewerByClassName(ClassificationViewer.getClassName(cName));
                        SyncDataTableAndClassificationViewer.syncFormattingFromViewer2Summary(classificationViewer, doc.getDataTable());
                    }
                }

                try (FileWriter writer = new FileWriter(fileName)) {
                    if (!doc.getChartColorManager().isUsingProgramColors()) {
                        doc.getDataTable().setColorTable(doc.getChartColorManager().getColorTableName(), doc.getChartColorManager().isColorByPosition(), doc.getChartColorManager().getHeatMapTable().getName());
                        doc.getDataTable().setColorEdits(doc.getChartColorManager().getColorEdits());
                    }

                    doc.getDataTable().setParameters(doc.getParameterString());
                    doc.getDataTable().write(writer);
                    doc.getSampleAttributeTable().write(writer, false, true);
                }
                if (doc.getMeganFile().getFileType() == MeganFile.Type.UNKNOWN_FILE)
                    doc.getMeganFile().setFileType(MeganFile.Type.MEGAN_SUMMARY_FILE);
                if (doc.getMeganFile().isMeganSummaryFile()) {
                    dir.setDirty(false);
                    doc.getMeganFile().setFileName(file.getPath());
                }
            } else {
                throw new IOException("RMA and DAA files can only be saved as summary files");
            }

            System.err.println("done");
            MeganProperties.addRecentFile(file);
        } catch (IOException ex) {
            NotificationsInSwing.showError(Objects.requireNonNull(viewer).getFrame(), "Save file '" + fileName + "'failed: " + ex, Integer.MAX_VALUE);
            throw ex;
        }
    }

// todo: inAskToSave mechanism needs to be repaired

    public void actionPerformed(ActionEvent event) {
        Director dir = getDir();
        final boolean inAskToSave = event.getActionCommand().equals("askToSave");

        String fileName = dir.getDocument().getMeganFile().getFileName();
        File lastOpenFile = null;
        if (fileName != null)
            lastOpenFile = new File(fileName);
        if (lastOpenFile == null) {
            String name = dir.getTitle();
            if (!name.endsWith(".meg") && !name.endsWith(".megan"))
                name += ".megan";
            if (ProgramProperties.getFile(MeganProperties.SAVEFILE) != null)
                lastOpenFile = new File(ProgramProperties.getFile(MeganProperties.SAVEFILE), name);
            else
                lastOpenFile = new File(name);
        }

        File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), lastOpenFile, new MeganFileFilter(), new MeganFileFilter(), event, "Save MEGAN file", ".megan");

        if (file != null) {
            ProgramProperties.put(MeganProperties.SAVEFILE, file);
            String cmd = "save file='" + file.getPath() + "' summary=true;";
            if (inAskToSave)
                executeImmediately(cmd);  // we are already in a thread, use immediate execution
            else
                execute(cmd);
        }
    }

    // if in ask to save, modify event source to tell calling method can see that user has canceled

    void replyUserHasCanceledInAskToSave(ActionEvent event) {
        ((Boolean[]) event.getSource())[0] = true;
    }

    public boolean isApplicable() {
        return getDoc().getNumberOfReads() > 0 && getDoc().getMeganFile().isMeganSummaryFile();
    }

    final public static String NAME = "Save As...";

    public String getName() {
        return NAME;
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/SaveAs16.gif");
    }

    public String getDescription() {
        return "Save current data set";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    public boolean isCritical() {
        return true;
    }
}

