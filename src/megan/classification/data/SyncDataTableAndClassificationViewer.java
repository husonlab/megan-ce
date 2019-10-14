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
package megan.classification.data;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NotOwnerException;
import jloda.util.Pair;
import megan.core.DataTable;
import megan.viewer.ClassificationViewer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * sync between summary and functional viewer
 * Daniel Huson, 4.2015
 */
public class SyncDataTableAndClassificationViewer {
    /**
     * sync collapsed nodes
     *
     * @param table
     * @param classificationViewer
     */
    public static void syncCollapsedFromSummary2Viewer(final DataTable table, final ClassificationViewer classificationViewer) {
        final String classificationName = classificationViewer.getClassName();

        // Sync collapsed nodes:
        if (table.getCollapsed(classificationName) != null && table.getCollapsed(classificationName).size() > 0) {
            classificationViewer.getCollapsedIds().clear();
            classificationViewer.getCollapsedIds().addAll(table.getCollapsed(classificationName));
        }
    }

    /**
     * sync the formatting (and collapsed nodes) from the summary to the fviewer viewer
     *
     * @param table
     * @param classificationViewer
     */
    public static boolean syncFormattingFromSummary2Viewer(DataTable table, ClassificationViewer classificationViewer) {
        final String classificationName = classificationViewer.getClassName();
        boolean changed = false;

        // can't use nexus parser here because it swallows the ' quotes
        String nodeFormats = table.getNodeFormats(classificationName);
        // System.err.println("Node Format: "+nodeFormats);
        if (nodeFormats != null) {
            int state = 0;
            int idA = 0, idB = 0;
            int formatA = 0;
            for (int pos = 0; pos < nodeFormats.length(); pos++) {
                char ch = nodeFormats.charAt(pos);
                switch (state) {
                    case 0: // skipping spaces before class id
                        if (!Character.isSpaceChar(ch)) {
                            state++;
                            idA = pos;
                        }
                        break;
                    case 1: // scanning class id
                        idB = pos;
                        if (ch == ':')
                            state++;
                        break;
                    case 2: // skipping spaces before start of format
                        if (!Character.isSpaceChar(ch)) {
                            state++;
                            formatA = pos;
                        }
                        break;

                    case 3: // scanning format
                        if (ch == ';') {
                            int formatB = pos;
                            if (idA < idB && formatA < formatB) {
                                Integer taxId = Integer.parseInt(nodeFormats.substring(idA, idB).trim());
                                classificationViewer.getDirtyNodeIds().add(taxId);
                                String format = nodeFormats.substring(formatA, formatB + 1).trim();
                                // System.err.println("got: <"+taxId+">: <"+format+">");
                                for (Node v : classificationViewer.getNodes(taxId)) {
                                    try {
                                        classificationViewer.getNV(v).read(format);
                                        changed = true;
                                    } catch (IOException ignored) {

                                    }
                                }
                            }
                            state = 0;
                        }
                        break;
                }
            }
        }

        String edgeFormats = table.getEdgeFormats(classificationName);
        //System.err.println("Edge Format: "+edgeFormats);
        if (edgeFormats != null) {
            int state = 0;
            int id1A = 0, id1B = 0;
            int id2A = 0, id2B = 0;

            int formatA = 0;
            for (int pos = 0; pos < edgeFormats.length(); pos++) {
                char ch = edgeFormats.charAt(pos);
                switch (state) {
                    case 0: // skipping spaces before class id1
                        if (!Character.isSpaceChar(ch)) {
                            state++;
                            id1A = pos;
                        }
                        break;
                    case 1: // scaning class id
                        id1B = pos;
                        if (ch == ',')
                            state++;
                        break;
                    case 2: // skipping spaces before class id2
                        if (!Character.isSpaceChar(ch)) {
                            state++;
                            id2A = pos;
                        }
                        break;
                    case 3: // scaning class id
                        id2B = pos;
                        if (ch == ':')
                            state++;
                        break;
                    case 4: // skipping spaces before start of format
                        if (!Character.isSpaceChar(ch)) {
                            state++;
                            formatA = pos;
                        }
                        break;

                    case 5: // scanning format
                        if (ch == ';') {
                            int formatB = pos;
                            if (id1A < id1B && id2A < id2B && formatA < formatB) {
                                Integer taxId1 = Integer.parseInt(edgeFormats.substring(id1A, id1B).trim());
                                Integer taxId2 = Integer.parseInt(edgeFormats.substring(id2A, id2B).trim());
                                classificationViewer.getDirtyEdgeIds().add(new Pair<>(taxId1, taxId2));

                                String format = edgeFormats.substring(formatA, formatB + 1).trim();

                                for (Node v : classificationViewer.getNodes(taxId1)) {
                                    for (Node w : classificationViewer.getNodes(taxId2)) {
                                        Edge e = v.getCommonEdge(w);
                                        try {
                                            if (e != null) {
                                                classificationViewer.getEV(e).read(format);
                                                changed = true;
                                            }
                                        } catch (IOException ignored) {

                                        }
                                    }
                                }
                            }
                            state = 0;
                        }
                        break;
                }
            }
        }
        return changed;
    }


    /**
     * sync formatting (and collapsed nodes)  from fviewer viewer to summary
     *
     * @param classificationViewer
     * @param table
     */
    static public void syncFormattingFromViewer2Summary(ClassificationViewer classificationViewer, DataTable table) {
        final String classificationName = classificationViewer.getClassName();
        if (classificationViewer.getDirtyNodeIds().size() > 0) {
            StringBuilder buf = new StringBuilder();
            for (Integer fviewerId : classificationViewer.getDirtyNodeIds()) {
                try {
                    Node v = classificationViewer.getANode(fviewerId);
                    if (v != null) {
                        String format = classificationViewer.getNV(v).toString(false);
                        buf.append(fviewerId).append(":").append(format);
                    }
                } catch (NotOwnerException ignored) {
                }
            }
            table.setNodeFormats(classificationName, buf.toString());
        } else
            table.setNodeFormats(classificationName, null);

        if (classificationViewer.getDirtyEdgeIds().size() > 0) {
            StringBuilder buf = new StringBuilder();
            for (Pair<Integer, Integer> pair : classificationViewer.getDirtyEdgeIds()) {
                Edge e = null;
                Set<Node> nodes = classificationViewer.getNodes(pair.getFirst());
                if (nodes != null) {
                    for (Node v : nodes) {
                        for (Node w : classificationViewer.getNodes(pair.getSecond())) {
                            e = v.getCommonEdge(w);
                            if (e != null)
                                break;
                        }
                        if (e != null)
                            break;
                    }
                }
                if (e != null) {
                    String format = classificationViewer.getEV(e).toString(false);
                    buf.append(pair.getFirst()).append(",").append(pair.getSecond()).append(":").append(format);
                }
            }
            table.setEdgeFormats(classificationName, buf.toString());
        } else
            table.setEdgeFormats(classificationName, null);


        table.setNodeStyle(classificationName, classificationViewer.getNodeDrawer().getStyle().toString());

        // Sync collapsed nodes:
        Set<Integer> collapsed = new HashSet<>(classificationViewer.getCollapsedIds());
        if (collapsed.size() == 0)
            collapsed.add(-1); // collapsed must contain atleast one element, otherwise will be ignored
        table.setCollapsed(classificationName, collapsed);
    }
}
