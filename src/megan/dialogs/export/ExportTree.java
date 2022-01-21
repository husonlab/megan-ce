/*
 * ExportTree.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.dialogs.export;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.util.StringUtils;
import megan.viewer.ViewerBase;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;

/**
 * export the taxonomic analysis as a leaf-labeled tree
 * Daniel Huson, 7.2012
 */
public class ExportTree {
    /**
     * export the tree
     *
     * @param viewer
     * @param writer
     * @param showInternalLabels
     * @param showUnassignedLabels
     * @param simplify
     * @return number of nodes written
     * @throws IOException
     */
    public static int apply(ViewerBase viewer, Writer writer, boolean showInternalLabels, boolean showUnassignedLabels, boolean simplify) throws IOException {
        final PhyloTree tree = viewer.getTree();
        Node root = tree.getRoot();

        if (root == null)
            return 0;

        NodeSet toUse = null;
        if (viewer.getSelectedNodes().size() > 0) {
            toUse = new NodeSet(tree);
            visitNodesToUseRec(viewer, root, toUse);

            while (!viewer.getSelected(root)) {
                Node w = null;
                for (Edge e = root.getFirstOutEdge(); e != null; e = root.getNextOutEdge(e)) {
                    if (toUse.contains(e.getTarget())) {
                        if (w == null)
                            w = e.getTarget();
                        else {
                            w = null;
                            break;
                        }
                    }
                }
                if (w != null) // there is exactly one node below the current root that should be used, move to it
                    root = w;
                else
                    break;
            }
        }

        final int countNodes = writeAsTreeRec(viewer, toUse, root, writer, showInternalLabels, showUnassignedLabels, simplify, 0);
        writer.write(";\n");
        return countNodes;
    }

    /**
     * recursively visit all nodes that we need to use due to user selection
     *
     * @param viewer
     * @param v
     * @param toUse
     * @return true, if v to be used
     */
    private static boolean visitNodesToUseRec(ViewerBase viewer, Node v, NodeSet toUse) {
        boolean use = viewer.getSelected(v);
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            if (visitNodesToUseRec(viewer, e.getTarget(), toUse))
                use = true;
        }
        if (use)
            toUse.add(v);
        return use;
    }

    /**
     * recursively write the tree
     *
     * @param v
     * @param writer
     * @param showInternalLabels
     * @param showUnassignedNodes
     * @param count
     * @return number of labeled nodes
     * @throws IOException
     */
    private static int writeAsTreeRec(ViewerBase viewer, NodeSet toUse, Node v, Writer writer, boolean showInternalLabels, boolean showUnassignedNodes, boolean simplify, int count) throws IOException {
        if (v.getOutDegree() == 0) {
			writer.write(StringUtils.toCleanName(viewer.getLabel(v)));
			count++;
        } else {
            LinkedList<Edge> toVisit = new LinkedList<>();
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                if ((toUse != null && toUse.contains(w))
                        || (toUse == null && (showUnassignedNodes || !(w.getInfo() instanceof Integer && (Integer) w.getInfo() <= 0)))) {
                    toVisit.add(e);
                }
            }
            if (!simplify || toVisit.size() > 1)
                writer.write("(");
            boolean first = true;
            for (Edge e : toVisit) {
                Node w = e.getTarget();
                if (first)
                    first = false;
                else
                    writer.write(",");
                count = writeAsTreeRec(viewer, toUse, w, writer, showInternalLabels, showUnassignedNodes, simplify, count);
            }
            if (!simplify || toVisit.size() > 1)
                writer.write(")");

            if (showInternalLabels && viewer.getLabel(v) != null && (!simplify || count != 1)) {
				writer.write(StringUtils.toCleanName(viewer.getLabel(v)));
				count++;
            }

        }
        return count;
    }
}
