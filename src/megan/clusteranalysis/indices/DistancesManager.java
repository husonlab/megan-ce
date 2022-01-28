/*
 * DistancesManager.java Copyright (C) 2022 Daniel H. Huson
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

package megan.clusteranalysis.indices;

import jloda.util.CanceledException;
import megan.clusteranalysis.tree.Distances;
import megan.viewer.ClassificationViewer;
import megan.viewer.MainViewer;

import java.util.Arrays;

public class DistancesManager {
    private static String[] names;

    /**
     * apply the named method
     *
     * @param method
     * @param viewer
     * @param distances
     */
    public static void apply(String method, final ClassificationViewer viewer, final Distances distances) throws CanceledException {
        if (method.equalsIgnoreCase(UniFrac.UnweightedUniformUniFrac)) {
            UniFrac.applyUnweightedUniformUniFrac((MainViewer) viewer, 1, distances);
        } else if (method.equalsIgnoreCase(UniFrac.WeightedUniformUniFrac)) {
            UniFrac.applyWeightedUniformUniFrac(viewer, distances);
        } else if (method.equalsIgnoreCase(JensenShannonDivergence.NAME)) {
            JensenShannonDivergence.apply(viewer, distances);
        } else if (method.equalsIgnoreCase(PearsonDistance.NAME)) {
            PearsonDistance.apply(viewer, distances);
        } else if (method.equalsIgnoreCase(EuclideanDistance.NAME)) {
            EuclideanDistance.apply(viewer, distances);
        } else if (method.equalsIgnoreCase(KulczynskiDistance.NAME)) {
            KulczynskiDistance.apply(viewer, distances);
        } else if (method.equalsIgnoreCase(ChiSquareDistance.NAME)) {
            ChiSquareDistance.apply(viewer, distances);
        } else if (method.equalsIgnoreCase(HellingerDistance.NAME)) {
            HellingerDistance.apply(viewer, distances);
        } else if (method.equalsIgnoreCase(GoodallsDistance.NAME)) {
            GoodallsDistance.apply(viewer, method, distances);
        } else // Bray-Curtis
        {
            BrayCurtisDissimilarity.apply(viewer, distances);
        }
    }

    /**
     * get names of all known distance calculations
     *
     * @return names
     */
    public static String[] getAllNames() {
        if (names == null) {
            names = new String[]{
                    UniFrac.UnweightedUniformUniFrac, UniFrac.WeightedUniformUniFrac,
                    JensenShannonDivergence.NAME,
                    PearsonDistance.NAME,
                    EuclideanDistance.NAME,
                    KulczynskiDistance.NAME,
                    ChiSquareDistance.NAME,
                    HellingerDistance.NAME,
                    GoodallsDistance.NAME,
                    BrayCurtisDissimilarity.NAME};
            Arrays.sort(names);
        }
        return names;

    }
}
