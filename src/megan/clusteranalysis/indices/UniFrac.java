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
package megan.clusteranalysis.indices;

import jloda.graph.Node;
import megan.clusteranalysis.tree.Distances;
import megan.viewer.ViewerBase;

import java.io.IOException;

/**
 * unweighted UniFrac distance
 * Daniel Huson, 9.2012
 */
public class UniFrac {
    public static final String TOPOLOGICAL_UNIFRAC = "Unweighted-UniFrac";

    /**
     * apply the named computation to the taxonomy
     *
     * @param viewer
     * @param method
     * @param threshold
     * @param distances
     * @return number of nodes used to compute value
     * @throws IOException
     */
    public static int apply(final ViewerBase viewer, String method, final int threshold, final Distances distances) throws IOException {
        System.err.println("Computing " + method + " distances");

        for (int s = 1; s <= distances.getNtax(); s++) {
            for (int t = s + 1; t <= distances.getNtax(); t++) {
                distances.set(s, t, 0);
            }
        }
        int countNodes = 0;
        for (Node v = viewer.getTree().getFirstNode(); v != null; v = v.getNext()) {
            if (v.getOutDegree() != 1 && (Integer) v.getInfo() > 0)  // only use proper nodes
            {
                countNodes++;
                int[] summarized = viewer.getNodeData(v).getSummarized();
                for (int s = 1; s <= distances.getNtax(); s++) {
                    for (int t = s + 1; t <= distances.getNtax(); t++) {
                        if ((summarized[s - 1] < threshold) != (summarized[t - 1] < threshold))
                            distances.increment(s, t);
                    }
                }
            }
        }
        if (countNodes > 0) {
            for (int s = 1; s <= distances.getNtax(); s++) {
                for (int t = s + 1; t <= distances.getNtax(); t++) {
                    distances.set(s, t, distances.get(s, t) / countNodes);
                }
            }
        }
        return countNodes;
    }
}
