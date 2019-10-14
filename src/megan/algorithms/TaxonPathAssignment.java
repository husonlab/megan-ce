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
import jloda.util.Pair;
import megan.classification.IdMapper;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import java.io.IOException;
import java.util.*;

/**
 * computes the taxon assignment path for a read, with percent support
 * Daniel Huson, 1.2009
 */
public class TaxonPathAssignment {
    /**
     * determine the taxon id of a read from its matches
     *
     * @param readBlock
     * @return id
     */
    private static List<Pair<Integer, Float>> computeTaxPath(IReadBlock readBlock, BitSet activeMatches) {
        List<Pair<Integer, Float>> result = new LinkedList<>();

        if (readBlock.getNumberOfMatches() == 0) {
            Pair<Integer, Float> pair = new Pair<>(IdMapper.NOHITS_ID, 100f);
            result.add(pair);
            return result;
        }

        Map<Node, Integer> node2count = new HashMap<>();
        int totalCount = 0;

        // compute addresses of all hit taxa:
        if (activeMatches.cardinality() > 0) {
            // collect the addresses of all non-disabled taxa:
            for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
                final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                int taxonId = matchBlock.getTaxonId();
                if (taxonId > 0) {
                    if (!TaxonomyData.isTaxonDisabled(taxonId)) {
                        totalCount++;
                        Node v = TaxonomyData.getTree().getANode(taxonId);
                        while (v != null) {
                            node2count.merge(v, 1, Integer::sum);
                            if (v.getInDegree() > 0)
                                v = v.getFirstInEdge().getSource();
                            else
                                v = null;
                        }
                    }
                }
            }

            if (totalCount == 0) // try again allowing disabled taxa
            {
                for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
                    final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                    int taxonId = matchBlock.getTaxonId();

                    if (taxonId > 0) {
                        totalCount++;
                        Node v = TaxonomyData.getTree().getANode(taxonId);
                        while (v != null) {
                            node2count.merge(v, 1, Integer::sum);
                            if (v.getInDegree() > 0)
                                v = v.getFirstInEdge().getSource();
                            else
                                v = null;
                        }
                    }
                }
            }
        }

        if (totalCount == 0) {
            Pair<Integer, Float> pair = new Pair<>(IdMapper.UNASSIGNED_ID, 100f);
            result.add(pair);
            return result;
        }

        Node v = TaxonomyData.getTree().getRoot();
        while (v != null) {
            Integer count = node2count.get(v);
            if (count == null)
                count = 0;
            float percent = Math.min(100f, Math.round(100f * count / (float) totalCount));
            Pair<Integer, Float> pair = new Pair<>((Integer) v.getInfo(), percent);
            result.add(pair);
            int bestCount = 0;
            Node bestChild = null;
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                Node w = e.getTarget();
                count = node2count.get(w);
                if (count != null && count > bestCount) {
                    bestChild = w;
                    bestCount = count;
                }
            }
            v = bestChild;
        }
        return result;
    }

    /**
     * report the taxonomic path and percent
     *
     * @param readBlock
     * @param activeMatchesForTaxa
     * @param showTaxonIds
     * @param showRank
     * @param useOfficialRanksOnly
     * @throws IOException
     */
    public static String getPathAndPercent(IReadBlock readBlock, BitSet activeMatchesForTaxa, boolean showTaxonIds, boolean showRank, boolean useOfficialRanksOnly) {
        final StringBuilder buf = new StringBuilder();
        final List<Pair<Integer, Float>> path = TaxonPathAssignment.computeTaxPath(readBlock, activeMatchesForTaxa);

        final String expectedPath = "dpcofgs";
        int expectedIndex = 0;

        for (Pair<Integer, Float> pair : path) {
            final Integer taxId = pair.getFirst();
            final String taxonName = (showTaxonIds ? "" + taxId : TaxonomyData.getName2IdMap().get(taxId)).replaceAll(";", "\\;");

            if (taxonName.equals("root"))
                continue;

            final int rank = TaxonomyData.getTaxonomicRank(taxId);

            String rankName;
            if (rank != 0) {
                rankName = TaxonomicLevels.getName(rank);
                if (rankName == null)
                    rankName = "?";
            } else {
                rankName = "?";
            }
            if (useOfficialRanksOnly && rankName.equals("?"))
                continue;

            if (showRank && !rankName.equals("?")) {
                char letter = Character.toLowerCase(rankName.charAt(0));
                if (rank == 127) // domain
                    letter = 'd';

                if (useOfficialRanksOnly) {
                    while (expectedIndex < expectedPath.length() && letter != expectedPath.charAt(expectedIndex)) {
                        buf.append(String.format("%c__unknown; %d;", expectedPath.charAt(expectedIndex), (int) (float) pair.getSecond()));
                        expectedIndex++;
                    }
                    expectedIndex++;
                }

                buf.append(String.format("%c__%s; %d;", letter, taxonName, (int) (float) pair.getSecond()));

            } else
                buf.append(" ").append(taxonName).append("; ").append((int) (float) pair.getSecond()).append(";");
        }
        return buf.toString();
    }
}


