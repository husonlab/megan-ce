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

import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.graph.NodeSet;
import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.viewer.TaxonomyData;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class ListPathCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "list paths nodes=selected [outFile=<name>];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("list paths nodes=selected");
        final String fileName;
        if (np.peekMatchIgnoreCase("outFile")) {
            np.matchIgnoreCase("outFile=");
            fileName = np.getWordFileNamePunctuation();
        } else
            fileName = null;
        np.matchIgnoreCase(";");

        final ViewerBase viewer = (ViewerBase) getViewer();
        final Classification classification = ClassificationManager.get(viewer.getClassName(), true);
        final boolean isTaxonomy = (viewer.getClassName().equals(Classification.Taxonomy));

        final Set<Node> nodes = new HashSet<>();
        {
            final NodeSet selected = viewer.getSelectedNodes();
            nodes.addAll(selected);
            for (Node v : selected) {
                // for a given node, add all that have same id
                nodes.addAll(viewer.getNodes((Integer) v.getInfo()));
            }
        }

        final BufferedWriter writer = new BufferedWriter(fileName == null ? new OutputStreamWriter(System.out) : new FileWriter(fileName));
        int count = 0;
        try {
            for (Node v : nodes) {
                if (isTaxonomy) {
                    final Integer id = (Integer) v.getInfo();
                    if (id != null) {
                        final String path = TaxonomyData.getPath(id, false);
                        if (path != null)
                            writer.write(path + " ");
                        else
                            writer.write(classification.getName2IdMap().get(id) + " ");
                    }
                } else {
                    LinkedList<String> list = new LinkedList<>();
                    while (true) {
                        Integer id = (Integer) v.getInfo();
                        if (id != null) {
                            list.add(classification.getName2IdMap().get(id));
                        }
                        if (v.getInDegree() > 0)
                            v = v.getFirstInEdge().getSource();
                        else {
                            break;
                        }
                    }
                    for (Iterator<String> it = list.descendingIterator(); it.hasNext(); ) {
                        writer.write(it.next() + "; ");
                    }
                }

                final NodeData data = viewer.getNodeData(v);
                if (data != null) {
                    final float[] summarized;

                    if (data.getSummarized() != null)
                        summarized = data.getSummarized();
                    else
                        summarized = data.getAssigned();
                    if (summarized != null && summarized.length >= 1) {
                        writer.write("\t" + Basic.toString(summarized, ", "));
                    }
                }

                writer.newLine();
                count++;

            }
        } finally {
            if (fileName != null)
                writer.close();
            else
                writer.flush();
        }
        if (fileName != null && count > 0)
            NotificationsInSwing.showInformation(getViewer().getFrame(), "Lines written to file: " + count);
    }

    public void actionPerformed(ActionEvent event) {
        executeImmediately("show window=message;");
        execute("list paths nodes=selected;");
    }

    public boolean isApplicable() {
        return getViewer() instanceof ViewerBase && ((ViewerBase) getViewer()).getSelectedNodes().size() > 0;
    }

    public String getName() {
        return "List Paths...";
    }

    public String getDescription() {
        return "List path from root to node for all selected";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/History16.gif");
    }

    public boolean isCritical() {
        return true;
    }
}

