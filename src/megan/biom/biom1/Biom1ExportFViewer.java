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
package megan.biom.biom1;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.graph.NodeSet;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.core.Director;
import megan.viewer.ClassificationViewer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * export a fviewer analysis in biom format
 * Daniel Huson, 7.2012
 */
public class Biom1ExportFViewer {
    /**
     * export taxon name to counts mapping
     *
     * @param dir
     * @param file
     * @param progressListener
     * @return lines written
     */
    public static int apply(Director dir, String cName, File file, ProgressListener progressListener) throws IOException, CanceledException {
        Biom1Data biom1Data = new Biom1Data(file.getPath());

        biom1Data.setType(Biom1Data.AcceptableTypes.Function_table.toString());
        biom1Data.setMatrix_type(Biom1Data.AcceptableMatrixTypes.dense.toString());
        biom1Data.setMatrix_element_type(Biom1Data.AcceptableMatrixElementTypes.Int.toString());
        biom1Data.setComment(cName + " classification computed by MEGAN");

        ClassificationViewer viewer = (ClassificationViewer) dir.getViewerByClassName(cName);
        if (viewer == null)
            throw new IOException(cName + " Viewer not open");

        java.util.List<String> names = dir.getDocument().getSampleNames();
        int numberOfCols = names.size();
        LinkedList<Map> colList = new LinkedList<>();
        for (String name : names) {
            Map colItem = new StringMap();
            colItem.put("id", Basic.getFileNameWithoutPath(Basic.getFileBaseName(name)));
            colItem.put("metadata", new StringMap());
            colList.add(colItem);
        }
        biom1Data.setColumns(colList.toArray(new Map[0]));

        final NodeSet selectedNodes = viewer.getSelectedNodes();
        if (selectedNodes.size() == 0) {
            throw new IOException("No nodes selected");
        }

        progressListener.setSubtask("Processing " + cName + " nodes");
        progressListener.setMaximum(selectedNodes.size());
        progressListener.setProgress(0);

        final LinkedList<Map> rowList = new LinkedList<>();
        final LinkedList<float[]> dataList = new LinkedList<>();

        visitSelectedLeavesRec(viewer, viewer.getTree().getRoot(), selectedNodes, new Vector<>(), rowList, dataList, new HashSet<>(), progressListener);
        int numberOfRows = rowList.size();
        biom1Data.setRows(rowList.toArray(new Map[numberOfRows]));

        biom1Data.setShape(new int[]{numberOfRows, numberOfCols});

        float[][] data = new float[numberOfRows][];
        int j = 0;
        for (float[] dataRow : dataList) {
            data[j++] = dataRow;
        }
        biom1Data.setData(data);

        System.err.println("Writing file: " + file);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            biom1Data.write(w);
        }
        return numberOfRows;
    }

    /**
     * recursively visit all the selected leaves
     *
     * @param viewer
     * @param v
     * @param selected
     * @param path
     * @param rowList
     * @param dataList
     */
    private static void visitSelectedLeavesRec(ClassificationViewer viewer, Node v, NodeSet selected, Vector<String> path,
                                               LinkedList<Map> rowList, LinkedList<float[]> dataList, Set<Integer> seen, ProgressListener progressListener) throws CanceledException {
        if (v.getOutDegree() > 0 || selected.contains(v)) {
            Integer classId = (Integer) v.getInfo();

            if (!seen.contains(classId)) {
                seen.add(classId);

                String className = v == viewer.getTree().getRoot() ? "Root" : viewer.getClassification().getName2IdMap().get(classId);
                path.addElement(className);

                if (selected.contains(v)) {
                    NodeData data = viewer.getNodeData(v);
                    if (data != null) {
                        final float[] values;
                        if (v.getOutDegree() == 0)
                            values = data.getSummarized();
                        else
                            values = data.getAssigned();
                        final StringMap<Object> rowItem = new StringMap<>();

                        rowItem.put("id", "" + classId);

                        final StringMap<Object> metadata = new StringMap<>();
                        ArrayList<String> classification = new ArrayList<>(path.size());
                        classification.addAll(path);
                        metadata.put("taxonomy", classification);
                        rowItem.put("metadata", metadata);
                        rowList.add(rowItem);
                        dataList.add(values);
                    }
                    progressListener.incrementProgress();
                }

                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                    visitSelectedLeavesRec(viewer, e.getTarget(), selected, path, rowList, dataList, seen, progressListener);
                }
                path.setSize(path.size() - 1);
            }
        }
    }
}
