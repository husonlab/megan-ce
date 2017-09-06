/*
 *  Copyright (C) 2017 Daniel H. Huson
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

import jloda.graph.Node;
import megan.algorithms.LCAAddressing;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.classification.data.ClassificationFullTree;
import megan.classification.data.Name2IdMap;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * maintains static access to the taxonomy
 * Daniel Huson, 4.2015
 */
public class TaxonomyData {
    private static Classification taxonomyClassification;

    /**
     * explicitly load the taxonomy classification
     */
    public static void load() {
        taxonomyClassification = ClassificationManager.get(Classification.Taxonomy, true);
    }

    /**
     * set the taxonomy classification
     *
     * @param taxonomyClassification
     */
    public static void setTaxonomyClassification(Classification taxonomyClassification) {
        TaxonomyData.taxonomyClassification = taxonomyClassification;
    }

    /**
     * gets the taxonomy names to ids
     *
     * @return taxonomy names to ids
     */
    public static Name2IdMap getName2IdMap() {
        return taxonomyClassification.getIdMapper().getName2IdMap();
    }

    /**
     * gets the taxonomy tree
     *
     * @return tree
     */
    public static ClassificationFullTree getTree() {
        return taxonomyClassification.getFullTree();
    }

    /**
     * is taxonomy data available?
     *
     * @return true, if available
     */
    public static boolean isAvailable() {
        return taxonomyClassification != null;
    }

    /**
     * is this taxon, or one of its ancestors, disabled? Taxa that are disabled are ignored by LCA algorithm
     *
     * @param taxonId
     * @return true, if disabled
     */
    public static boolean isTaxonDisabled(Integer taxonId) {
        return taxonId == null || (taxonId > 0 && taxonomyClassification.getIdMapper().getDisabledIds().contains(taxonId));
    }

    /**
     * get all currently disabled taxa. Note that any taxon below a disabled taxon is also considered disabled
     *
     * @return all disabled taxa
     */
    public static Set<Integer> getDisabledTaxa() {
        if (taxonomyClassification != null && taxonomyClassification.getIdMapper() != null)
            return taxonomyClassification.getIdMapper().getDisabledIds();
        else
            return new HashSet<>();
    }

    public static int getTaxonomicRank(Integer id) {
        return taxonomyClassification.getIdMapper().getName2IdMap().getRank(id);
    }

    public static void setTaxonomicRank(Integer id, byte rank) {
        taxonomyClassification.getIdMapper().getName2IdMap().setRank(id, rank);
    }

    /**
     * gets the closest ancestor that has a major rank
     *
     * @param id
     * @return rank of this node, if is major, otherwise of closest ancestor
     */
    public static int getLowestAncestorWithMajorRank(Integer id) {
        if (id <= 0)
            return id;

        while (true) {
            int rank = getTaxonomicRank(id);
            if (TaxonomicLevels.isMajorRank(rank))
                return id;
            String address = getAddress(id);
            if (address == null || address.length() == 0)
                return 1;
            address = address.substring(0, address.length() - 1);
            if (address.length() == 0)
                return 1;
            id = getAddress2Id(address);
            if (id <= 0)
                return 1;
        }

    }

    public static String getAddress(Integer id) {
        return taxonomyClassification.getFullTree().getAddress(id);
    }

    public static Integer getAddress2Id(String address) {
        return taxonomyClassification.getFullTree().getAddress2Id(address);
    }

    public static boolean isAncestor(int higherTaxonId, int lowerTaxonId) {
        String higherAddress = getAddress(higherTaxonId);
        String lowerAddress = getAddress(lowerTaxonId);
        return higherAddress != null && (lowerAddress == null || lowerAddress.startsWith(higherAddress));
    }

    /**
     * returns the LCA of a set of taxon ids
     *
     * @param taxonIds
     * @param removeAncestors
     * @return id
     */
    public static int getLCA(Set<Integer> taxonIds, boolean removeAncestors) {
        if (taxonIds.size() == 0)
            return IdMapper.NOHITS_ID;

        final Set<String> addresses = new HashSet<>();

        int countKnownTaxa = 0;

        // compute addresses of all hit taxa:
        for (Integer taxonId : taxonIds) {
            if (taxonId > 0 && !isTaxonDisabled(taxonId)) {
                String address = getAddress(taxonId);
                if (address != null) {
                    addresses.add(address);
                    countKnownTaxa++;
                }
            }
        }

        // compute LCA using addresses:
        if (countKnownTaxa > 0) {
            final String address = LCAAddressing.getCommonPrefix(addresses, removeAncestors);
            return getAddress2Id(address);
        }

        // although we had some hits, couldn't make an assignment
        return IdMapper.UNASSIGNED_ID;
    }

    /**
     * get the path string associated with a taxon
     *
     * @param taxId
     * @return path string or null
     */
    public static String getPath(int taxId, boolean majorRanksOnly) {

        final String expectedPath = "DPCOFGS";
        int expectedIndex = 0;

        final Node v = taxonomyClassification.getFullTree().getANode(taxId);
        if (v != null) {
            final LinkedList<Node> path = new LinkedList<>();
            {
                Node w = v;
                while (true) {
                    if (!majorRanksOnly || TaxonomicLevels.isMajorRank(taxonomyClassification.getId2Rank().get((Integer) w.getInfo())))
                        path.push(w);
                    if (w.getInDegree() > 0)
                        w = w.getFirstInEdge().getSource();
                    else
                        break;
                }
            }
            StringBuilder buf = new StringBuilder();

            for (Node w : path) {
                Integer id = (Integer) w.getInfo();
                if (id != null) {
                    if (majorRanksOnly) {
                        String letters = TaxonomicLevels.getName(taxonomyClassification.getId2Rank().get((Integer) w.getInfo()));

                        final char key = Character.toUpperCase(letters.charAt(0));
                        while (expectedIndex < expectedPath.length() && key != expectedPath.charAt(expectedIndex)) {
                            char missing = expectedPath.charAt(expectedIndex);
                            if (buf.length() > 0)
                                buf.append(" ");
                            buf.append("[").append(missing == 'D' ? "SK" : missing).append("] unknown;");
                            expectedIndex++;
                        }
                        expectedIndex++;

                        if (letters.equals("Domain"))
                            letters = "SK";
                        else
                            letters = letters.substring(0, 1);

                        if (buf.length() > 0)
                            buf.append(" ");
                        buf.append("[").append(letters).append("] ").append(taxonomyClassification.getName2IdMap().get(id)).append(";");
                    } else {
                        if (buf.length() > 0)
                            buf.append(" ");
                        buf.append(taxonomyClassification.getName2IdMap().get(id)).append(";");
                    }
                }
            }
            return buf.toString();
        } else
            return null;
    }

    /**
     * gets the path, or id, if path not found
     *
     * @param taxId
     * @return path or id
     */
    public static String getPathOrId(int taxId, boolean majorRanksOnly) {
        String path = getPath(taxId, majorRanksOnly);
        return path != null ? path : "" + taxId;
    }

}
