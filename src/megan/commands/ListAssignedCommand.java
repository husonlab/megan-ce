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

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.graph.NodeSet;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import jloda.util.Single;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Document;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;

public class ListAssignedCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "list assigned nodes={all|selected} [outFile=<name>];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("list assigned nodes=");
        String what = np.getWordMatchesIgnoringCase("all selected");
        final String fileName;
        if (np.peekMatchIgnoreCase("outFile")) {
            np.matchIgnoreCase("outFile=");
            fileName = np.getWordFileNamePunctuation();
        } else
            fileName = null;
        np.matchIgnoreCase(";");

        final ViewerBase viewer = (ViewerBase) getViewer();
        final Document doc = viewer.getDocument();
        if (doc.getMeganFile().getFileName() == null) {
            throw new IOException("No input file\n");
        }

        final Classification classification = ClassificationManager.get(getViewer().getClassName(), true);

        final NodeSet selectedNodes = (what.equalsIgnoreCase("selected") ? viewer.getSelectedNodes() : null);
        final Writer w = new BufferedWriter(fileName == null ? new OutputStreamWriter(System.out) : new FileWriter(fileName));
        final Single<Integer> countLines = new Single<>();

        final ProgressListener progress = doc.getProgressListener();
        progress.setTasks("List", "Assignments");
        progress.setMaximum(viewer.getTree().getNumberOfNodes());
        progress.setProgress(0);

        try {
            w.write("########## Begin of summary for file: " + doc.getMeganFile().getFileName() + "\n");
            w.write("Samples: " + doc.getNumberOfSamples() + "\n");
            w.write("Reads total: " + doc.getNumberOfReads() + "\n");
            countLines.set(3);

            if (viewer.getTree().getRoot() != null) {
                w.write("Assigned at nodes:" + "\n");
                countLines.set(countLines.get() + 1);

                listAssignedRec(viewer, classification, selectedNodes, viewer.getTree().getRoot(), 0, w, countLines, progress);
            }
            w.write("########## End of summary for file: " + doc.getMeganFile().getFileName() + "\n");
        } finally {
            if (fileName != null)
                w.close();
            else
                w.flush();
        }
        if (fileName != null && countLines.get() > 0)
            NotificationsInSwing.showInformation(getViewer().getFrame(), "Lines written to file: " + countLines.get());
    }


    /**
     * recursively print a summary
     *
     * @param selectedNodes
     * @param v
     * @param indent
     */
    private void listAssignedRec(ViewerBase viewer, Classification classification, NodeSet selectedNodes, Node v, int indent, Writer outs, final Single<Integer> countLines, ProgressListener progress) throws IOException, CanceledException {
        progress.incrementProgress();

        final int id = (Integer) v.getInfo();

        if ((selectedNodes == null || selectedNodes.contains(v))) {
            final String name = classification.getName2IdMap().get(id);
            NodeData data = (viewer.getNodeData(v));
            if (data.getCountSummarized() > 0) {
                for (int i = 0; i < indent; i++)
                    outs.write(" ");
                outs.write(name + ": " + Basic.toString(data.getSummarized(), ",") + "\n");
                countLines.set(countLines.get() + 1);
            }
        }
        if (viewer.getCollapsedIds().contains(id)) {
            return;
        }
        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            listAssignedRec(viewer, classification, selectedNodes, f.getOpposite(v), indent + 2, outs, countLines, progress);
        }
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("show window=message;");
        final ViewerBase viewer = (ViewerBase) getViewer();
        if (viewer.getSelectedNodes().size() == 0)
            execute("list assigned nodes=all;");
        else
            execute("list assigned nodes=selected;");
    }

    public boolean isApplicable() {
        return getDoc().getNumberOfReads() > 0 && getViewer() != null && getViewer() instanceof ViewerBase;
    }

    public String getName() {
        return "List Assigned...";
    }

    public String getDescription() {
        return "List assigned counts for selected nodes of tree";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/History16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}

