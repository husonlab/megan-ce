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
package megan.viewer;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.util.Basic;
import jloda.util.Pair;
import megan.core.ClassificationType;
import megan.core.DataTable;
import megan.core.Document;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * sync between summary and taxonomy viewer
 * Daniel Huson, 6.2010
 */
public class SyncDataTableAndTaxonomy {

    /**
     * sync taxonomy formatting (and collapsed nodes) from summary to document
     *
     * @param table
     * @param viewer
     */
    public static void syncFormattingFromSummary2Viewer(DataTable table, MainViewer viewer) {
        Document doc = viewer.getDir().getDocument();
        // can't use nexus parser here because it swallows the ' quotes
        final String nodeFormats = table.getNodeFormats(ClassificationType.Taxonomy.toString());
        // System.err.println("Node Format: "+nodeFormats);
        if (nodeFormats != null) {
            int state = 0;
            int idA = 0, idB = 0;
            int formatA = 0;
            for (int pos = 0; pos < nodeFormats.length(); pos++) {
                char ch = nodeFormats.charAt(pos);
                switch (state) {
                    case 0: // skipping spaces before taxon id
                        if (!Character.isSpaceChar(ch)) {
                            state++;
                            idA = pos;
                        }
                        break;
                    case 1: // scaning taxon id
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
                                viewer.getDirtyNodeIds().add(taxId);
                                Node v = viewer.getTaxId2Node(taxId);
                                if (v != null) {
                                    String format = nodeFormats.substring(formatA, formatB + 1).trim();
                                    try {
                                        viewer.getNV(v).read(format);
                                    } catch (Exception e) {
                                        Basic.caught(e);
                                    }
                                }
                            }
                            state = 0;
                        }
                        break;
                }
            }
        }

        final String edgeFormats = table.getEdgeFormats(ClassificationType.Taxonomy.toString());
        //System.err.println("Edge Format: "+edgeFormats);
        if (edgeFormats != null) {
            int state = 0;
            int taxId1A = 0, taxId1B = 0;
            int taxId2A = 0, taxId2B = 0;

            int formatA = 0;
            for (int pos = 0; pos < edgeFormats.length(); pos++) {
                char ch = edgeFormats.charAt(pos);
                switch (state) {
                    case 0: // skipping spaces before taxon id1
                        if (!Character.isSpaceChar(ch)) {
                            state++;
                            taxId1A = pos;
                        }
                        break;
                    case 1: // scanning taxon id
                        taxId1B = pos;
                        if (ch == ',')
                            state++;
                        break;
                    case 2: // skipping spaces before taxon id2
                        if (!Character.isSpaceChar(ch)) {
                            state++;
                            taxId2A = pos;
                        }
                        break;
                    case 3: // scanning taxon id
                        taxId2B = pos;
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
                            if (taxId1A < taxId1B && taxId2A < taxId2B && formatA < formatB) {
                                Integer taxId1 = Integer.parseInt(edgeFormats.substring(taxId1A, taxId1B).trim());
                                Integer taxId2 = Integer.parseInt(edgeFormats.substring(taxId2A, taxId2B).trim());

                                String format = edgeFormats.substring(formatA, formatB + 1).trim();
                                // System.err.println("got: <"+taxId1+">+<"+taxId2+">: <"+format+">");
                                Node v = viewer.getTaxId2Node(taxId1);
                                Node w = viewer.getTaxId2Node(taxId2);
                                viewer.getDirtyEdgeIds().add(new Pair<>(taxId1, taxId2));
                                if (v != null && w != null) {
                                    Edge e = v.getCommonEdge(w);
                                    if (e != null) {
                                        try {
                                            viewer.getEV(e).read(format);
                                        } catch (IOException ignored) {

                                        }
                                    }
                                }
                            }
                            state = 0;
                        }
                        break;
                }
                //System.err.println("pos="+pos+", ch="+ch+", state="+state);
            }
        }
    }

    /**
     * sync collapsed nodes from summary to main viewer
     *
     * @param table
     * @param mainViewer
     */
    public static void syncCollapsedFromSummaryToTaxonomyViewer(DataTable table, MainViewer mainViewer) {
        Document doc = mainViewer.getDir().getDocument();
        // Sync collapsed nodes:
        mainViewer.getCollapsedIds().clear();
        if (table.getCollapsed(ClassificationType.Taxonomy.toString()) != null)
            mainViewer.getCollapsedIds().addAll(table.getCollapsed(ClassificationType.Taxonomy.toString()));
    }

    /**
     * sync formatting of taxonomy nodes and edges (and also set of collapsed nodes)    to summary
     *
     * @param viewer
     * @param table
     */
    static public void syncFormattingFromViewer2Summary(MainViewer viewer, DataTable table) {
        Document doc = viewer.getDir().getDocument();
        if (viewer.getDirtyNodeIds().size() > 0) {
            StringBuilder buf = new StringBuilder();
            for (Integer taxId : viewer.getDirtyNodeIds()) {
                Node v = viewer.getTaxId2Node(taxId);
                if (v != null) {
                    String format = viewer.getNV(v).toString(false);
                    buf.append(taxId).append(":").append(format);
                }
            }
            table.setNodeFormats(ClassificationType.Taxonomy.toString(), buf.toString());
        } else
            table.setNodeFormats(ClassificationType.Taxonomy.toString(), null);

        if (viewer.getDirtyEdgeIds().size() > 0) {
            StringBuilder buf = new StringBuilder();
            for (Pair<Integer, Integer> pair : viewer.getDirtyEdgeIds()) {
                Node v = viewer.getTaxId2Node(pair.getFirst());
                Node w = viewer.getTaxId2Node(pair.getSecond());
                if (v != null && w != null) {
                    Edge e = v.getCommonEdge(w);
                    if (e != null) {
                        String format = viewer.getEV(e).toString(false);
                        buf.append(pair.getFirst()).append(",").append(pair.getSecond()).append(":").append(format);
                    }
                }
            }
            table.setEdgeFormats(ClassificationType.Taxonomy.toString(), buf.toString());
        } else
            table.setEdgeFormats(ClassificationType.Taxonomy.toString(), null);

        table.setNodeStyle(ClassificationType.Taxonomy.toString(), viewer.getNodeDrawer().getStyle().toString());

        // Sync collapsed nodes:
        Set<Integer> collapsed = new HashSet<>(viewer.getCollapsedIds());
        table.setCollapsed(ClassificationType.Taxonomy.toString(), collapsed);
    }
}
