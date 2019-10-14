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
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import java.util.HashMap;

/**
 * checks for possible chimeric read
 * Daniel Huson, 9.2017
 */
class ChimericCheck {

    /**
     * report any long reads that look like they may be chimeric
     *
     * @param readName
     * @param v
     * @param taxa2intervals
     * @param totalCovered
     */
    public static void apply(String readName, Node v, HashMap<Integer, IntervalList> taxa2intervals, int totalCovered, int readLength) {
        final int ancestorRank = TaxonomyData.getTaxonomicRank(TaxonomyData.getLowestAncestorWithMajorRank((int) v.getInfo()));
        if (v.getInDegree() > 0 && v.getFirstInEdge().getSource().getInDegree() > 0 // keep root and top level nodes
                && (/*ancestorRank == 1 || */ ancestorRank == TaxonomicLevels.getSpeciesId() || ancestorRank == TaxonomicLevels.getGenusId())) // keep nothing genus or below
            return;

        final String taxonName = TaxonomyData.getName2IdMap().get((int) v.getInfo());

        if (taxonName.contains("unclassified"))
            return;

        String message = null;

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            final int taxon1 = (Integer) e.getTarget().getInfo();
            final String taxonName1 = TaxonomyData.getName2IdMap().get(taxon1);
            if (!taxonName1.contains("unclassified")) {
                final IntervalList intervals1 = taxa2intervals.get(taxon1);
                if (intervals1 != null) {
                    final int covered1 = intervals1.getCovered();
                    double minProportionOfAlignedBasedCoveredPerSide = 0.2;
                    int minNumberOfBasesCoveredPerSide = 1000;
                    if (covered1 >= minNumberOfBasesCoveredPerSide && covered1 >= minProportionOfAlignedBasedCoveredPerSide * totalCovered) {
                        final int min1 = intervals1.computeMin();
                        final int max1 = intervals1.computeMax();
                        for (Edge f = v.getNextOutEdge(e); f != null; f = v.getNextOutEdge(f)) {
                            final int taxon2 = (Integer) f.getTarget().getInfo();
                            final String taxonName2 = TaxonomyData.getName2IdMap().get(taxon2);
                            if (!taxonName2.contains("unclassified")) {
                                final IntervalList intervals2 = taxa2intervals.get(taxon2);
                                if (intervals2 != null) {
                                    final Integer covered2 = intervals2.getCovered();
                                    double minProportionOfBasesCoveredBothSides = 0.6;
                                    if (readLength == 0 || covered1 + covered2 >= minProportionOfBasesCoveredBothSides * readLength) {
                                        // 0.8;
                                        double minProportionOfAlignedBasesCoveredBothSides = 0;
                                        if (covered2 >= minNumberOfBasesCoveredPerSide && covered2 >= minProportionOfAlignedBasedCoveredPerSide * totalCovered
                                                && covered1 + covered2 >= minProportionOfAlignedBasesCoveredBothSides * totalCovered) {
                                            final int min2 = intervals2.computeMin();
                                            final int max2 = intervals2.computeMax();
                                            if (max1 <= min2 || max2 <= min1) {
                                                if (message == null) {
                                                    final int rank = TaxonomyData.getTaxonomicRank((int) v.getInfo());
                                                    String rankName = TaxonomicLevels.getName(rank);
                                                    if (rankName == null)
                                                        rankName = "below " + TaxonomicLevels.getName(ancestorRank);
                                                    message = String.format("Possible chimeric read: '%s' (%,d bp): [%s] %s: %s (%,d bp) vs %s (%,d bp)",
                                                            readName, readLength, rankName, taxonName, taxonName1, covered1, taxonName2, covered2);
                                                } else
                                                    return; // more than one pair, problably not a simple chimeric read
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (message != null)
            System.err.println(message);
    }
}
