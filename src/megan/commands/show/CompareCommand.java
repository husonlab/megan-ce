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
package megan.commands.show;

import jloda.swing.commands.ICommand;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.dialogs.compare.CompareWindow;
import megan.dialogs.compare.Comparer;
import megan.main.MeganProperties;
import megan.util.MeganFileFilter;
import megan.util.MeganizedDAAFileFilter;
import megan.util.RMAFileFilter;
import megan.viewer.MainViewer;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

public class CompareCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "compare mode={" + Basic.toString(Comparer.COMPARISON_MODE.values(), "|") + "}" +
                " readAssignmentMode={" + Basic.toString(Document.ReadAssignmentMode.values(), "|") + "}" +
                " [keep1={false|true}] [ignoreUnassigned={false|true}] [pid=<number> ...] [meganFile=<filename> ...];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        final Director dir = getDir();
        final Document doc = dir.getDocument();
        final ProgressListener progress = doc.getProgressListener();

        if (!doc.neverOpenedReads)
            throw new Exception("Internal error: document already used");

        doc.neverOpenedReads = false;
        np.matchIgnoreCase("compare");
        final Comparer comparer = new Comparer();
        if (np.peekMatchIgnoreCase("mode")) {
            np.matchIgnoreCase("mode=");
            comparer.setMode(np.getWordMatchesIgnoringCase(Basic.toString(Comparer.COMPARISON_MODE.values(), " ")));
        }
        Document.ReadAssignmentMode readAssignmentMode = Document.ReadAssignmentMode.readCount;
        if (np.peekMatchIgnoreCase("readAssignmentMode")) {
            np.matchIgnoreCase("readAssignmentMode=");
            readAssignmentMode = Document.ReadAssignmentMode.valueOfIgnoreCase(np.getWordMatchesIgnoringCase(Basic.toString(Document.ReadAssignmentMode.values(), " ")));
        }

        if (np.peekMatchIgnoreCase("keep1")) {
            np.matchIgnoreCase("keep1=");
            comparer.setKeep1(np.getBoolean());
        }

        if (np.peekMatchIgnoreCase("ignoreUnassigned")) {
            np.matchIgnoreCase("ignoreUnassigned=");
            comparer.setIgnoreUnassigned(np.getBoolean());
        }
        final java.util.List<Director> toDelete = new LinkedList<>();
        try {
            if (np.peekMatchIgnoreCase("pid")) {
                np.matchIgnoreCase("pid=");
                do {
                    int pid = np.getInt();
                    Director newDir = (Director) ProjectManager.getProject(pid);
                    if (newDir == null)
                        throw new IOException("No such project id: " + pid);
                    if (newDir.getDocument().getNumberOfReads() == 0)
                        throw new IOException("No reads found in file: '" + newDir.getDocument().getMeganFile().getFileName() + "' (id=" + pid + ")");

                    comparer.addDirector(newDir);
                    if (np.peekMatchIgnoreCase(",")) // for backward compatibility
                        np.matchAnyTokenIgnoreCase(",");
                } while (!np.peekMatchAnyTokenIgnoreCase("; meganFile"));
            }
            if (np.peekMatchIgnoreCase("meganFile")) {
                np.matchIgnoreCase("meganFile=");
                final ArrayList<String> files = new ArrayList<>();
                do {
                    String fileName = np.getWordRespectCase();

                    if (fileName.contains("::")) {
                        files.add(fileName);
                    } else {
                        files.addAll(RecursiveFileLister.apply(fileName, new MeganFileFilter(), new RMAFileFilter(), MeganizedDAAFileFilter.getInstance()));
                    }

                    if (np.peekMatchIgnoreCase(","))
                        np.matchAnyTokenIgnoreCase(",");   // for backward compatibility
                } while (!np.peekMatchIgnoreCase(";"));
                np.matchIgnoreCase(";");

                progress.setProgress(0);
                progress.setMaximum(files.size());
                for (String fileName : files) {
                    progress.setTasks("Comparison", "Loading files");
                    final Director newDir = Director.newProject(false, true);
                    if (newDir != null) {
                        newDir.executeImmediately("open file='" + fileName + "' readOnly=true;update;", newDir.getMainViewer().getCommandManager());
                        if (newDir.getDocument().getNumberOfReads() == 0) {
                            throw new IOException("No reads found in file: '" + fileName + "'");
                        }
                        comparer.addDirector(newDir);
                        toDelete.add(newDir);
                    }
                    progress.incrementProgress();
                }
            }
            doc.getMeganFile().setFileName(ProjectManager.getUniqueName("Comparison.megan"));

            doc.clearReads();
            doc.setReadAssignmentMode(readAssignmentMode);
            comparer.computeComparison(doc.getSampleAttributeTable(), doc.getDataTable(), progress);
            doc.setNumberReads(doc.getDataTable().getTotalReads());
            doc.getMeganFile().setEmbeddedSourceFiles(doc.getSampleAttributeTable().getSourceFiles());
            doc.processReadHits();
            doc.setTopPercent(100);
            doc.setMinScore(0);
            doc.setMinSupportPercent(0);
            doc.setMinSupport(1);
            doc.setMaxExpected(10000);
            doc.getActiveViewers().addAll(doc.getDataTable().getClassification2Class2Counts().keySet());
            doc.setDirty(true);
            doc.getMeganFile().setFileType(MeganFile.Type.MEGAN_SUMMARY_FILE);
            final MainViewer mainViewer = dir.getMainViewer();
            if (mainViewer != null) {
                if (mainViewer.isVisible()) {
                    mainViewer.getNodeDrawer().setStyle(ProgramProperties.get(MeganProperties.COMPARISON_STYLE, ""), NodeDrawer.Style.BarChart);
                    mainViewer.collapseToDefault();
                    mainViewer.setDoReInduce(true);
                    mainViewer.setDoReset(true);
                    mainViewer.setVisible(true);
                    doc.loadColorTableFromDataTable();
                }
            }
        } finally {
            for (Director aDir : toDelete) {
                aDir.close();
            }
        }
    }

    /**
     * display the dialog and then execute the command entered, if any
     *
     * @param event
     */
    public void actionPerformed(ActionEvent event) {
        final Director newDir = Director.newProject();

        boolean ok = false;
        final CompareWindow compareWindow;
        try {
            compareWindow = new CompareWindow(newDir.getMainViewer().getFrame(), newDir, null);

            if (!compareWindow.isCanceled()) {
                final String command = compareWindow.getCommand();
                if (command != null) {
                    newDir.execute(command, newDir.getCommandManager());
                    ok = true;
                }
            }
            if (!ok) {
                try {
                    newDir.close();
                } catch (CanceledException e) {
                    e.printStackTrace();
                }
                ProjectManager.removeProject(newDir);
            }
        } catch (CanceledException e) {
            Basic.caught(e); // won't happen because we don't preload files here
        }
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Compare...";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    public String getDescription() {
        return "Open compare dialog to produce a comparison of multiple datasets";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/New16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}

