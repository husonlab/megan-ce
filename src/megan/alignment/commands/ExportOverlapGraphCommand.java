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
package megan.alignment.commands;


import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.FastaFileFilter;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.alignment.AlignmentViewer;
import megan.assembly.alignment.AlignmentAssembler;
import megan.core.Director;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * save overlap graph
 * Daniel Huson, 5.2015
 */
public class ExportOverlapGraphCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "export overlapGraph file=<filename> [minOverap=<number>] [showGraph={false|true}];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("export overlapGraph file=");
        String fileName = np.getAbsoluteFileName();

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

        try {
            System.err.println("Building overlap graph and exporting to file: " + fileName);

            final AlignmentViewer viewer = (AlignmentViewer) getViewer();
            final Director dir = (Director) getDir();
            final Writer w = new FileWriter(fileName);
            final AlignmentAssembler alignmentAssembler = new AlignmentAssembler();
            alignmentAssembler.computeOverlapGraph(minOverlap, viewer.getAlignment(), dir.getDocument().getProgressListener());
            final Pair<Integer, Integer> nodesAndEdges = alignmentAssembler.writeOverlapGraph(w);
            w.close();
            NotificationsInSwing.showInformation(viewer.getFrame(), "Wrote " + nodesAndEdges.get1() + " nodes and " + nodesAndEdges.get2() + " edges");
            if (showGraph)
                alignmentAssembler.showOverlapGraph(dir, dir.getDocument().getProgressListener());
        } catch (IOException e) {
            NotificationsInSwing.showError("Export overlap file FAILED: " + e.getMessage());
        }
    }

    public void actionPerformed(ActionEvent event) {
        File lastOpenFile = ProgramProperties.getFile("OverlapGraphFile");
        String fileName = ((AlignmentViewer) getViewer()).getAlignment().getName();
        if (fileName == null)
            fileName = "Untitled";
        else
            fileName = Basic.toCleanName(fileName);
        if (lastOpenFile != null) {
            fileName = new File(lastOpenFile.getParent(), fileName).getPath();
        }
        fileName = Basic.replaceFileSuffix(fileName, "-overlap.gml");

        File file = ChooseFileDialog.chooseFileToSave(getViewer().getFrame(), new File(fileName), new FastaFileFilter(), new FastaFileFilter(), event, "Save contigs file", ".fasta");

        if (file != null) {
            if (Basic.getFileSuffix(file.getName()) == null)
                file = Basic.replaceFileSuffix(file, ".gml");
            ProgramProperties.put("OverlapGraphFile", file);
            execute("export overlapGraph file='" + file.getPath() + "' minOverlap=" + ProgramProperties.get("AssemblyMinOverlap", 20) + " showGraph=false;");
        }
    }

    public boolean isApplicable() {
        return ((AlignmentViewer) getViewer()).getAlignment().getLength() > 0;
    }

    public String getName() {
        return "Overlap Graph...";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Export16.gif");
    }

    public String getDescription() {
        return "Build and save overlap graph";
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
