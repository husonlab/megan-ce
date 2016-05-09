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
package megan.commands.show;

import jloda.gui.commands.ICommand;
import jloda.gui.director.ProjectManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.dialogs.compare.CompareWindow;
import megan.dialogs.compare.Comparer;
import megan.main.MeganProperties;
import megan.viewer.MainViewer;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

public class CompareCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "compare mode={" + Basic.toString(Comparer.COMPARISON_MODE.values(), "|") + "} [keep1={false|true}]"
                + " [ignoreUnassigned={false|true}] [pid=<number> ...] [meganFile=<filename> ...];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        final Director dir = getDir();
        final Document doc = dir.getDocument();

        if (!doc.neverOpenedReads)
            throw new Exception("Internal error: document already used");

        doc.neverOpenedReads = false;
        np.matchIgnoreCase("compare");
        final Comparer comparer = new Comparer();
        if (np.peekMatchIgnoreCase("mode")) {
            np.matchIgnoreCase("mode=");
            comparer.setMode(np.getWordMatchesIgnoringCase(Basic.toString(Comparer.COMPARISON_MODE.values(), " ")));
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
                while (true) {
                    int pid = np.getInt();
                    Director newDir = (Director) ProjectManager.getProject(pid);
                    if (newDir == null)
                        throw new IOException("No such project id: " + pid);
                    comparer.addDirector(newDir);
                    if (np.peekMatchIgnoreCase(",")) // for backward compatibility
                        np.matchAnyTokenIgnoreCase(",");
                    if (np.peekMatchAnyTokenIgnoreCase("; meganFile"))
                        break;
                }
            }
            if (np.peekMatchIgnoreCase("meganFile")) {
                np.matchIgnoreCase("meganFile=");
                ArrayList<String> files = new ArrayList<>();
                while (true) {
                    String fileName = np.getWordRespectCase();
                    if (!fileName.contains("::") && !(new File(fileName)).isFile())
                        throw new IOException("File not found: " + fileName);
                    files.add(fileName);
                    if (np.peekMatchIgnoreCase(","))
                        np.matchAnyTokenIgnoreCase(",");   // for backward compatibility
                    if (np.peekMatchIgnoreCase(";"))
                        break;
                }
                np.matchIgnoreCase(";");

                doc.getProgressListener().setTasks("Comparison", "Loading files");
                doc.getProgressListener().setMaximum(files.size());
                for (String fileName : files) {
                    Director newDir = Director.newProject(false, true);
                    newDir.executeImmediately("open file='" + fileName + "' readOnly=true;update;", newDir.getMainViewer().getCommandManager());

                    comparer.addDirector(newDir);
                    toDelete.add(newDir);
                    doc.getProgressListener().incrementProgress();
                }
            }
            doc.getMeganFile().setFileName(ProjectManager.getUniqueName("Comparison.megan"));

            doc.clearReads();
            comparer.computeComparison(doc.getSampleAttributeTable(), doc.getDataTable(), doc.getProgressListener());
            doc.setNumberReads(doc.getDataTable().getTotalReads());
            doc.processReadHits();
            doc.setTopPercent(100);
            doc.setMinScore(0);
            doc.setMinSupportPercent(0);
            doc.setMinSupport(1);
            doc.setMaxExpected(10000);
            doc.getActiveViewers().addAll(doc.getDataTable().getClassification2Class2Counts().keySet());
            doc.setDirty(true);
            doc.getMeganFile().setFileType(MeganFile.Type.MEGAN_SUMMARY_FILE);
            MainViewer mainViewer = dir.getMainViewer();
            mainViewer.getNodeDrawer().setStyle(ProgramProperties.get(MeganProperties.COMPARISON_STYLE, ""), NodeDrawer.Style.BarChart);
            mainViewer.setDoReInduce(true);
            mainViewer.setDoReset(true);
            mainViewer.setVisible(true);
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
        CompareWindow compareWindow = new CompareWindow(getViewer().getFrame(), getDir(), null);
        if (!compareWindow.isCanceled()) {
            final Director newDir = Director.newProject();
            newDir.getMainViewer().getFrame().setVisible(true);
            newDir.getMainViewer().setDoReInduce(true);
            newDir.getMainViewer().setDoReset(true);
            String command = compareWindow.getCommand();
            if (command != null)
                newDir.execute(command, newDir.getCommandManager());
        }
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return "Compare...";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    public String getDescription() {
        return "Open compare dialog to produce a comparison of multiple datasets";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/New16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}

