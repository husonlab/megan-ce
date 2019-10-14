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
import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ProgressDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.core.ClassificationType;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.main.MeganProperties;
import megan.util.MeganAndRMAFileFilter;
import megan.util.MeganizedDAAFileFilter;
import megan.viewer.MainViewer;
import megan.viewer.SyncDataTableAndTaxonomy;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * the open file command
 * Daniel Huson, 6.2010
 */
public class OpenFileCommand extends CommandBase implements ICommand {
    private static long timeOfLastOpen = 0;

    /**
     * constructor
     */
    public OpenFileCommand() {
    }

    /**
     * constructor
     *
     * @param dir
     */
    public OpenFileCommand(IDirector dir) {
        setDir(dir);
    }


    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return "open file=<filename> [readOnly={false|true}];";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        final Director dir = getDir();
        final Document doc = dir.getDocument();
        final MainViewer viewer = dir.getMainViewer();

        if (ProgramProperties.isUseGUI() && (doc.getNumberOfSamples() > 0 || !doc.neverOpenedReads || doc.isDirty())) {
            final Director newDir = Director.newProject();
            newDir.getMainViewer().setDoReInduce(true);
            newDir.getMainViewer().setDoReset(true);
            newDir.getMainViewer().getFrame().requestFocus(); // todo: recently added in an attempt to fix problem that window opens behind old one
            newDir.execute(np.getQuotedTokensRespectCase(null, ";") + ";", newDir.getMainViewer().getCommandManager());
        } else {
            try {
                SwingUtilities.invokeLater(() -> viewer.getFrame().toFront());
                np.matchIgnoreCase("open file=");

                String fileName = np.getAbsoluteFileName();
                if (fileName.contains("'"))
                    NotificationsInSwing.showWarning(viewer.getFrame(), "File name or path contains a single quote ', this will cause problems, please change!");

                boolean readOnly = false;
                if (np.peekMatchIgnoreCase("readOnly=")) {
                    np.matchIgnoreCase("readOnly=");
                    readOnly = np.getBoolean();
                }
                np.matchIgnoreCase(";");

                doc.closeConnector(); // close connector if it open

                final MeganFile meganFile = doc.getMeganFile();

                meganFile.setFileFromExistingFile(fileName, readOnly);

                meganFile.checkFileOkToRead(); // will throw IOException if there is a problem

                if (!meganFile.isMeganSummaryFile() && meganFile.hasDataConnector()) {
                    if (meganFile.isMeganServerFile())
                        meganFile.setReadOnly(true);
                    if (!meganFile.isReadOnly() && MeganFile.isUIdContainedInSetOfOpenFiles(meganFile.getName(), meganFile.getConnector().getUId())) {
                        NotificationsInSwing.showWarning(viewer.getFrame(), "File already open: " + meganFile.getFileName() + "\nWill open read-only");
                        meganFile.setReadOnly(true);
                    }
                }

                doc.getProgressListener().setMaximum(-1);
                doc.getProgressListener().setProgress(-1);
                if (doc.getProgressListener() instanceof ProgressDialog) {
                    doc.getProgressListener().setTasks("Opening file", meganFile.getName());
                    ((ProgressDialog) doc.getProgressListener()).show();
                }
                viewer.getCollapsedIds().clear();
                doc.loadMeganFile(); // note that megan3 summary and comparison files get converted to megan4 files here

                if (meganFile.isMeganSummaryFile()) {
                    SyncDataTableAndTaxonomy.syncCollapsedFromSummaryToTaxonomyViewer(doc.getDataTable(), viewer);
                    if (doc.getDataTable().getTotalReads() > 0) {
                        doc.setNumberReads(doc.getDataTable().getTotalReads());

                        if (doc.getDataTable().getNumberOfSamples() == 1) {
                            viewer.getNodeDrawer().setStyle(doc.getDataTable().getNodeStyle(ClassificationType.Taxonomy.toString()), NodeDrawer.Style.Circle);

                        } else {
                            viewer.getNodeDrawer().setStyle(doc.getDataTable().getNodeStyle(ClassificationType.Taxonomy.toString()), NodeDrawer.Style.PieChart);
                        }
                    } else {
                        throw new IOException("File is either empty or format is too old: " + meganFile.getName());
                    }
                } else if (meganFile.hasDataConnector()) {
                    if (doc.getDataTable().getNumberOfSamples() > 0) {
                        SyncDataTableAndTaxonomy.syncCollapsedFromSummaryToTaxonomyViewer(doc.getDataTable(), viewer);
                        if (doc.getDataTable().getTotalReads() > 0) {
                            doc.setNumberReads(doc.getDataTable().getTotalReads());
                        }
                        viewer.getNodeDrawer().setStyle(doc.getDataTable().getNodeStyle(ClassificationType.Taxonomy.toString()), NodeDrawer.Style.Circle);
                        // make sure we use the correct name for the sample:
                        doc.getDataTable().setSamples(new String[]{Basic.getFileBaseName(meganFile.getName())}, doc.getDataTable().getSampleUIds(), doc.getDataTable().getSampleSizes(), doc.getDataTable().getBlastModes());
                    }
                } else
                    throw new IOException("Old MEGAN2 format, not supported by this version of MEGAN");

                viewer.setDoReInduce(true);
                viewer.setDoReset(true);

                doc.neverOpenedReads = false;
                if (!dir.isInternalDocument())
                    MeganProperties.addRecentFile(meganFile.getFileName());
                doc.setDirty(false);
                if (!meganFile.isMeganSummaryFile() && meganFile.hasDataConnector())
                    MeganFile.addUIdToSetOfOpenFiles(meganFile.getName(), meganFile.getConnector().getUId());
                if (System.currentTimeMillis() - timeOfLastOpen > 5000) {
                    NotificationsInSwing.showInformation(String.format("Opened file '%s' with %,d reads", Basic.getFileNameWithoutPath(fileName), doc.getNumberOfReads()), 5000);
                } else
                    System.err.println(String.format("Opened file '%s' with %,d reads", fileName, doc.getNumberOfReads()));
                timeOfLastOpen = System.currentTimeMillis();
                if (!ProgramProperties.isUseGUI())
                    executeImmediately("update;");
            } catch (Exception ex) {
                // NotificationsInSwing.showError(viewer.getFrame(), "Open file failed: " + ex);
                doc.getMeganFile().setFileName(null);
                if (doc.neverOpenedReads && ProjectManager.getNumberOfProjects() > 1) {
                    System.err.println("Closing window...");
                    dir.close();
                }
                throw ex;
            }
        }
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {
        File lastOpenFile = ProgramProperties.getFile(MeganProperties.MEGANFILE);

        MeganAndRMAFileFilter meganAndRMAFileFilter = new MeganAndRMAFileFilter();
        meganAndRMAFileFilter.setAllowGZipped(true);
        meganAndRMAFileFilter.setAllowZipped(true);
        meganAndRMAFileFilter.add(MeganizedDAAFileFilter.getInstance());
        getDir().notifyLockInput();

        Collection<File> files;
        try {
            files = ChooseFileDialog.chooseFilesToOpen(getViewer().getFrame(), lastOpenFile, meganAndRMAFileFilter, meganAndRMAFileFilter, ev, "Open MEGAN file");
        } finally {
            getDir().notifyUnlockInput();
        }

        if (files.size() > 0) {
            final StringBuilder buf = new StringBuilder();
            for (File file : files) {
                if (file != null && file.exists() && file.canRead()) {
                    ProgramProperties.put(MeganProperties.MEGANFILE, file.getAbsolutePath());
                    buf.append("open file='").append(file.getPath()).append("';");
                }
            }
            execute(buf.toString());
        }
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return true;
    }


    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return "Open...";
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Open a MEGAN file (ending on .rma, .meg or .megan)";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Open16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}
