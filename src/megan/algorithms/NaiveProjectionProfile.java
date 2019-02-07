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
package megan.algorithms;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.viewer.ClassificationViewer;
import megan.viewer.TaxonomicLevels;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Compute a taxonomic profile by naive projection
 */
public class NaiveProjectionProfile {
    /**
     * compute a taxonomic profile at a given taxonomic rank using naive projection
     *
     * @param viewer
     * @param rankName
     * @return mapping of each taxon to a count
     * todo: needs fixing
     */
    public static Map<Integer, float[]> compute(final ClassificationViewer viewer, final String rankName, final float minPercent) {

        int rank = TaxonomicLevels.getId(rankName);
        final Set<Integer> nodeIdsAtGivenRank = ClassificationManager.get(Classification.Taxonomy, true).getFullTree().getNodeIdsAtGivenRank(rank, false);

        final int numberOfSamples = viewer.getDocument().getNumberOfSamples();

        final Map<Integer, float[]> profile = new HashMap<>();

        final PhyloTree tree = viewer.getTree();
        final Node root = tree.getRoot();

        final float[] rootAssigned = new float[numberOfSamples];
        for (int i = 0; i < numberOfSamples; i++)
            rootAssigned[i] = ((NodeData) root.getData()).getAssigned(i);

        // recursively process the tree:
        computeRec(root, rootAssigned, profile, nodeIdsAtGivenRank, numberOfSamples);

        // copy not assigned etc:
        for (Edge e = root.getFirstOutEdge(); e != null; e = root.getNextOutEdge(e)) {
            Node w = e.getTarget();
            if (((Integer) w.getInfo()) <= 0) {
                final float[] assigned = new float[numberOfSamples];
                for (int i = 0; i < numberOfSamples; i++)
                    assigned[i] = ((NodeData) w.getData()).getAssigned(i);
                profile.put(((Integer) w.getInfo()), assigned);
            }
        }

        final int[] totalInitiallyAssigned = new int[numberOfSamples];
        for (Integer taxId : profile.keySet()) {
            if (taxId > 0) {
                float[] counts = profile.get(taxId);
                for (int i = 0; i < counts.length; i++) {
                    totalInitiallyAssigned[i] += counts[i];
                }
            }
        }

        float[] minSupport = new float[numberOfSamples];
        for (int i = 0; i < numberOfSamples; i++) {
            minSupport[i] = (totalInitiallyAssigned[i] / 100.0f) * minPercent;
        }

        float[] unassigned = profile.get(IdMapper.UNASSIGNED_ID);
        if (unassigned == null) {
            unassigned = new float[numberOfSamples];
            for (int i = 0; i < numberOfSamples; i++)
                unassigned[i] = 0;
            profile.put(IdMapper.UNASSIGNED_ID, unassigned);
        }

        {
            Set<Integer> toDelete = new HashSet<>();
            for (Integer taxonId : profile.keySet()) {
                if (taxonId > 0) {
                    float[] array = profile.get(taxonId);
                    boolean hasEntry = false;
                    for (int i = 0; i < array.length; i++) {
                        if (array[i] != 0) {
                            if (array[i] < minSupport[i]) {
                                unassigned[i] += array[i];
                                array[i] = 0;
                            } else {
                                hasEntry = true;
                                break;
                            }
                        }
                    }
                    if (!hasEntry)
                        toDelete.add(taxonId);
                }
            }
            profile.keySet().removeAll(toDelete);
        }


        final int[] totalProjected = new int[numberOfSamples];
        final int[] lostCount = new int[numberOfSamples];

        {
            for (Integer taxId : profile.keySet()) {
                if (taxId > 0) {
                    float[] counts = profile.get(taxId);
                    for (int i = 0; i < counts.length; i++) {
                        totalProjected[i] += counts[i];
                    }
                }
            }

            for (int i = 0; i < numberOfSamples; i++) {
                lostCount[i] = totalInitiallyAssigned[i] - totalProjected[i];

                System.err.println("Sample " + i + ":");
                System.err.println(String.format("Reads:    %,10.0f", viewer.getDocument().getDataTable().getSampleSizes()[i]));
                System.err.println(String.format("Assigned: %,10d", totalInitiallyAssigned[i]));
                System.err.println(String.format("Projected:%,10d", totalProjected[i]));
                System.err.println(String.format("Lost:     %,10d", lostCount[i]));
            }
        }

        for (int i = 0; i < numberOfSamples; i++)
            unassigned[i] += lostCount[i];

        System.err.println("Total projected: " + Basic.toString(totalProjected, ", "));
        System.err.println("Total lost:      " + Basic.toString(lostCount, ", "));

        return profile;
    }

    /**
     * recursively compute profile
     *
     * @param v
     * @param countFromAbove
     * @param profile
     * @param H
     * @param numberOfSamples
     */
    private static void computeRec(Node v, float[] countFromAbove, Map<Integer, float[]> profile, Set<Integer> H, int numberOfSamples) {
        final int taxId = (Integer) v.getInfo();
        final NodeData vData = (NodeData) v.getData();

        if (H.contains(taxId)) { // is a node at the chosen rank, save profile
            float[] counts = new float[numberOfSamples];
            for (int i = 0; i < counts.length; i++) {
                counts[i] = countFromAbove[i] + (vData.getSummarized(i) - vData.getAssigned(i)); // below=summarized-assigned
            }
            profile.put(taxId, counts);
        } else {
            // determine how many below:
            boolean hasChild = false;
            final int[] belowV = new int[numberOfSamples];
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                if (((Integer) w.getInfo()) > 0) {
                    final NodeData wData = (NodeData) w.getData();
                    for (int i = 0; i < numberOfSamples; i++) {
                        belowV[i] += wData.getSummarized(i);
                    }
                    hasChild = true;
                }
            }

            if (!hasChild) { // has no child, these reads will be lost
            } else { // there are some children push down counts:
                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                    final Node w = e.getTarget();
                    if (((Integer) w.getInfo()) > 0) {
                        final NodeData wData = (NodeData) w.getData();
                        float[] count = new float[numberOfSamples];
                        for (int i = 0; i < numberOfSamples; i++) {
                            if (belowV[i] > 0) {
                                final double fraction = (double) wData.getSummarized(i) / (double) belowV[i];
                                count[i] = wData.getAssigned(i) + (int) (countFromAbove[i] * fraction);
                            }
                        }
                        computeRec(w, count, profile, H, numberOfSamples);
                    }
                }
            }
        }
    }
}
