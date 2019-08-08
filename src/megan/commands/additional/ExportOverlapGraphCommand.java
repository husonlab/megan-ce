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
package megan.commands.additional;

import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.ResourceManager;
import jloda.swing.util.TextFileFilter;
import jloda.util.Basic;
import jloda.util.BlastMode;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.assembly.ReadAssembler;
import megan.assembly.ReadData;
import megan.assembly.ReadDataCollector;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.core.Document;
import megan.data.IReadBlockIterator;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

/**
 * assemble all reads associated with a selected node
 * Daniel Huson, 5.2015
 */
public class ExportOverlapGraphCommand extends CommandBase implements ICommand {

    public String getSyntax() {
        return "export overlapGraph file=<name> [minOverlap=<number>] [showGraph={false|true}];";
    }

    // nt minReads, double minCoverage, int minLength,
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export overlapGraph file=");

        final String overlapGraphFile = np.getWordFileNamePunctuation();

        final int minOverlap;
        if (np.peekMatchIgnoreCase("minOverlap")) {
            np.matchIgnoreCase("minOverlap=");
            minOverlap = np.getInt(1, 1000000);
        } else
            minOverlap = ProgramProperties.get("AssemblyMinOverlap", 20);

        boolean showGraph = false;
        if (np.peekMatchIgnoreCase("showGraph")) {
            np.matchIgnoreCase("showGraph=");
            showGraph = np.getBoolean();
        }

        np.matchIgnoreCase(";");

        final Director dir = getDir();
        final Document doc = dir.getDocument();
        final ClassificationViewer viewer = (ClassificationViewer) getViewer();

        String message = "";
        if (viewer.getSelectedIds().size() > 0) {
            final ReadAssembler readAssembler = new ReadAssembler(true);

            try (IReadBlockIterator it = doc.getConnector().getReadsIteratorForListOfClassIds(viewer.getClassName(), viewer.getSelectedIds(), 0, 10, true, true)) {
                final String label = viewer.getClassName() + ". Id(s): " + Basic.toString(viewer.getSelectedIds(), ", ");
                final List<ReadData> readData = ReadDataCollector.apply(it, doc.getProgressListener());
                readAssembler.computeOverlapGraph(label, minOverlap, readData, doc.getProgressListener());

                if (overlapGraphFile != null) {
                    try (final Writer w = new BufferedWriter(new FileWriter(overlapGraphFile))) {
                        Pair<Integer, Integer> counts = readAssembler.writeOverlapGraph(w);
                        System.err.println("Graph written to: " + overlapGraphFile);
                        message = "Wrote " + counts.getFirst() + " nodes and " + counts.getSecond() + " edges";
                    }
                }
                if (showGraph)
                    readAssembler.showOverlapGraph(dir, doc.getProgressListener());
            }
        } else
            message = "Nothing selected";
        if (message.length() > 0) {
            NotificationsInSwing.showInformation(getViewer().getFrame(), message);

        }
    }

    public void actionPerformed(ActionEvent event) {
        final Document doc = getDir().getDocument();

        File lastOpenFile = ProgramProperties.getFile("OverlapGraphFile");
        String fileName = doc.getMeganFile().getName();
        if (fileName == null)
            fileName = "Untitled";
        else
            fileName = Basic.toCleanName(fileName);
        if (lastOpenFile != null) {
            fileName = new File(lastOpenFile.getParent(), fileName).getPath();
        }
        fileName = Basic.replaceFileSuffix(fileName, "-overlap.gml");

        File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), new File(fileName), new TextFileFilter(".gml"), new TextFileFilter(".gml"), event, "Save overlap graph", ".gml");

        if (file != null) {
            if (Basic.getFileSuffix(file.getName()) == null)
                file = Basic.replaceFileSuffix(file, ".gml");
            ProgramProperties.put("OverlapGraphFile", file);
            execute("export overlapGraph file='" + file.getPath() + "' minOverlap=" + ProgramProperties.get("AssemblyMinOverlap", 20) + " showGraph=false;");
        }
    }

    public boolean isApplicable() {
        final Document doc = getDir().getDocument();

        return getViewer() instanceof ClassificationViewer && ((ClassificationViewer) getViewer()).getNumberSelectedNodes() > 0 && doc.getMeganFile().hasDataConnector() && doc.getBlastMode().equals(BlastMode.BlastX);
    }

    public String getName() {
        return "Overlap Graph...";
    }

    public String getDescription() {
        return "Build and export the overlap graph for selected nodes";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}
