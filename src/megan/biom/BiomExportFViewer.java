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
package megan.biom;

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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;

/**
 * export a fviewer analysis in biom format
 * Daniel Huson, 7.2012
 */
public class BiomExportFViewer {
    /**
     * export taxon name to counts mapping
     *
     * @param dir
     * @param file
     * @param progressListener
     * @return lines written
     */
    public static int apply(Director dir, String cName, File file, ProgressListener progressListener) throws IOException, CanceledException {
        BiomData biomData = new BiomData(file.getPath());

        biomData.setType(BiomData.AcceptableTypes.Function_table.toString());
        biomData.setMatrix_type(BiomData.AcceptableMatrixTypes.dense.toString());
        biomData.setMatrix_element_type(BiomData.AcceptableMatrixElementTypes.Int.toString());
        biomData.setComment(cName + " classification computed by MEGAN");

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
        biomData.setColumns(colList.toArray(new Map[colList.size()]));

        final NodeSet selectedNodes = viewer.getSelectedNodes();
        if (selectedNodes.size() == 0) {
            throw new IOException("No nodes selected");
        }

        progressListener.setSubtask("Processing " + cName + " nodes");
        progressListener.setMaximum(selectedNodes.size());
        progressListener.setProgress(0);

        final LinkedList<Map> rowList = new LinkedList<>();
        final LinkedList<int[]> dataList = new LinkedList<>();

        visitSelectedLeavesRec(viewer, viewer.getTree().getRoot(), selectedNodes, new Vector<String>(), rowList, dataList, progressListener);
        int numberOfRows = rowList.size();
        biomData.setRows(rowList.toArray(new Map[numberOfRows]));

        biomData.setShape(new int[]{numberOfRows, numberOfCols});

        int[][] data = new int[numberOfRows][];
        int j = 0;
        for (int[] dataRow : dataList) {
            data[j++] = dataRow;
        }
        biomData.setData(data);

        System.err.println("Writing file: " + file);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            biomData.write(w);
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
                                               LinkedList<Map> rowList, LinkedList<int[]> dataList, ProgressListener progressListener) throws CanceledException {
        if (v.getOutDegree() > 0 || selected.contains(v)) {
            Integer classId = (Integer) v.getInfo();
            String className = v == viewer.getTree().getRoot() ? "Root" : viewer.getClassification().getName2IdMap().get(classId);
            path.addElement(className);

            if (selected.contains(v)) {
                NodeData data = viewer.getNodeData(v);
                if (data != null) {
                    int[] values;
                    if (v.getOutDegree() == 0)
                        values = data.getSummarized();
                    else
                        values = data.getAssigned();
                    Map rowItem = new StringMap();
                    rowItem.put("id", "" + classId);
                    Map metadata = new StringMap();
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
                visitSelectedLeavesRec(viewer, e.getTarget(), selected, path, rowList, dataList, progressListener);
            }
            path.setSize(path.size() - 1);
        }
    }
}
