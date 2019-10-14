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

package megan.util;

import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.graph.NodeSet;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.classification.Classification;
import megan.core.Director;
import megan.viewer.ClassificationViewer;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Exports selected nodes in stamp profile
 * Created by huson on 1/13/16.
 */
public class ExportStamp {
    private static final String[] ranks =
            {TaxonomicLevels.Domain,
                    TaxonomicLevels.Phylum,
                    TaxonomicLevels.Class,
                    TaxonomicLevels.Order,
                    TaxonomicLevels.Family,
                    TaxonomicLevels.Genus,
                    TaxonomicLevels.Species
            };
    private static final char[] letters = {'k', 'p', 'c', 'o', 'f', 'g', 's'};

    /**
     * apply the exporter
     *
     * @param dir
     * @param cName
     * @param file
     * @param allLevels
     * @param progressListener
     * @return lines exported
     * @throws IOException
     * @throws CanceledException
     */
    public static int apply(Director dir, String cName, File file, boolean allLevels, ProgressListener progressListener) throws IOException, CanceledException {
        final ClassificationViewer viewer = (ClassificationViewer) dir.getViewerByClassName(cName);
        final boolean taxonomy = cName.equalsIgnoreCase(Classification.Taxonomy);
        if (viewer == null)
            throw new IOException(cName + " Viewer not open");

        final NodeSet selectedNodes = viewer.getSelectedNodes();
        if (selectedNodes.size() == 0) {
            throw new IOException("No nodes selected");
        }
        System.err.println("Writing file: " + file);
        progressListener.setSubtask("Processing " + cName + " nodes");
        progressListener.setMaximum(selectedNodes.size());
        progressListener.setProgress(0);

        int maxRankIndex = 0;
        if (allLevels) {
            if (taxonomy) {
                maxRankIndex = determineMaxTaxonomicRankIndex(selectedNodes);
                System.err.println("Exporting " + (maxRankIndex + 1) + " taxonomic levels down to rank of '" + ranks[maxRankIndex] + "'");
            } else {
                maxRankIndex = determineMaxFunctionalIndex(selectedNodes);
                System.err.println("Exporting " + (maxRankIndex + 1) + " functional levels");
            }
        }

        final int numberOfLevels = maxRankIndex + 1;

        // header format:
        // Level_1	Observation Ids	sample0 sample1 sample2...

        int numberOfRows = 0;
        int nodesSkipped = 0;

        final int numberOfColumns = dir.getDocument().getNumberOfSamples();

        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            // write header line:
            for (int i = 0; i < numberOfLevels; i++)
                w.write("Level_" + (i + 1) + "\t");
            w.write("Observation Ids");
            for (String sample : dir.getDocument().getSampleNames()) {
                w.write("\t" + sample);
            }
            w.write("\n");
            for (Node v : selectedNodes) {
                final String name = viewer.getLabel(v);
                final Integer classId = (Integer) v.getInfo();
                if (maxRankIndex > 1) {
                    final String path = taxonomy ? makePath(v, maxRankIndex) : makeFunctionalPath(viewer, v, maxRankIndex);
                    if (path != null)
                        w.write(String.format("%s\tID%d", path, classId));
                    else {
                        if (nodesSkipped < 5)
                            System.err.println("Skipping node: " + name);
                        else if (nodesSkipped == 5)
                            System.err.println("Skipping more nodes...");
                        nodesSkipped++;
                        continue;
                    }
                } else
                    w.write(String.format("%s\tID%d", name, classId));
                NodeData data = viewer.getNodeData(v);
                if (v.getOutDegree() == 0) {
                    for (int i = 0; i < numberOfColumns; i++)
                        w.write("\t" + data.getSummarized(i));
                } else {
                    for (int i = 0; i < numberOfColumns; i++)
                        w.write("\t" + data.getAssigned(i));
                }
                w.write("\n");
                numberOfRows++;
            }
        }
        System.err.println("Nodes skipped: " + nodesSkipped);
        return numberOfRows;
    }


    /**
     * get all ranks above, or null, if incomplete
     *
     * @param v
     * @param rankIndex
     * @return all ranks or null
     */
    private static String makePath(final Node v, int rankIndex) {
        final ArrayList<String> list = new ArrayList<>();

        // first fill in names that are missing and the end of the path, e.g.
        // if path only leads to Phylum Proteobacteria, then all levels below Proteobacteria are labeled "(Proteobacteria)"
        {
            int topRankIndex = -1;
            String topRankTaxonName = null;
            Node w = v;
            while (topRankIndex == -1) {
                final Integer taxonId = (Integer) w.getInfo();
                final int rank = TaxonomyData.getTaxonomicRank(taxonId);
                if (rank != 0) {
                    final String rankName = TaxonomicLevels.getName(rank);
                    final int index = Basic.getIndex(rankName, ranks);
                    if (index != -1) {
                        topRankIndex = index;
                        topRankTaxonName = TaxonomyData.getName2IdMap().get(taxonId);
                    }
                }
                if (w.getInDegree() == 1)
                    w = w.getFirstInEdge().getSource();
                else
                    break;
            }

            while (rankIndex > topRankIndex) {
                list.add(letters[rankIndex] + "__(" + topRankTaxonName + ")");
                rankIndex--;
            }
        }

        Node w = v;
        while (rankIndex >= 0) {
            final Integer taxonId = (Integer) w.getInfo();
            final int rank = TaxonomyData.getTaxonomicRank(taxonId);
            if (rank != 0) {
                final String rankName = TaxonomicLevels.getName(rank);
                final int index = Basic.getIndex(rankName, ranks);
                if (index >= 0) {
                    String previousName = (list.size() > 0 ? list.get(list.size() - 1).substring(3) : null);
                    while (rankIndex > index) {
                        list.add(letters[rankIndex] + "__(" + previousName + ")"); // fill in missing intermediate ranks
                        rankIndex--;
                    }
                    if (index == rankIndex) {
                        list.add(letters[rankIndex] + "__" + TaxonomyData.getName2IdMap().get(taxonId));
                        rankIndex--;
                    }
                }
            }
            if (w.getInDegree() == 1)
                w = w.getFirstInEdge().getSource();
            else
                break;
        }
        if (rankIndex == -1)
            return Basic.toString(Basic.reverseList(list), "\t");
        return null;
    }

    /**
     * determine the max taxonomic rank index
     *
     * @param selectedNodes
     * @return max taxonomic level
     */
    private static int determineMaxTaxonomicRankIndex(NodeSet selectedNodes) {
        int maxRankIndex = -1;
        for (Node v : selectedNodes) {
            int rank = TaxonomyData.getTaxonomicRank((Integer) v.getInfo());
            if (rank != 0) {
                String rankName = TaxonomicLevels.getName(rank);
                int index = Basic.getIndex(rankName, ranks);
                if (index > maxRankIndex)
                    maxRankIndex = index;
            }
        }
        return maxRankIndex;
    }

    /**
     * determine the number of levels
     *
     * @param selectedNodes
     * @return number of levels
     */
    private static int determineMaxFunctionalIndex(NodeSet selectedNodes) {
        int levels = 0;
        for (Node v : selectedNodes) {
            int distance2root = 0;
            while (true) {
                distance2root++;
                if (v.getFirstInEdge() != null)
                    v = v.getFirstInEdge().getSource();
                else
                    break;
            }
            levels = Math.max(levels, distance2root);
        }
        return levels - 1; // substract 1 because we will suppress the root
    }

    /**
     * make a functional path
     *
     * @param viewer
     * @param v
     * @param maxRankIndex
     * @return path
     */
    private static String makeFunctionalPath(ClassificationViewer viewer, Node v, int maxRankIndex) {
        final ArrayList<String> path = new ArrayList<>(maxRankIndex);

        while (true) {
            path.add(viewer.getLabel(v));
            if (v.getFirstInEdge() != null)
                v = v.getFirstInEdge().getSource();
            else
                break;
            if (v.getFirstInEdge() == null)
                break; // this node is the root and we want to suppress the root
        }

        final StringBuilder buf = new StringBuilder();

        int missing = maxRankIndex - path.size();
        buf.append(path.remove(path.size() - 1));
        buf.append("\t-".repeat(Math.max(0, missing)));
        while (path.size() > 0) {
            buf.append("\t").append(path.remove(path.size() - 1));
        }
        return buf.toString();
    }



}
