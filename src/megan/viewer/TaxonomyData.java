/*
 *  Copyright (C) 2015 Daniel H. Huson
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

import megan.algorithms.LCAAddressing;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.classification.data.ClassificationFullTree;
import megan.classification.data.Name2IdMap;

import java.util.HashSet;
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
        return taxonId == null || taxonomyClassification.getIdMapper().getDisabledIds().contains(taxonId);
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
            return new HashSet<Integer>();
    }

    public static int getTaxonomicRank(Integer id) {
        return taxonomyClassification.getIdMapper().getName2IdMap().getRank(id);
    }

    public static void setTaxonomicRank(Integer id, byte rank) {
        taxonomyClassification.getIdMapper().getName2IdMap().setRank(id, rank);
    }

    public static String getAddress(Integer id) {
        return taxonomyClassification.getFullTree().getAddress(id);
    }

    public static Integer getAddress2Id(String address) {
        return taxonomyClassification.getFullTree().getAddress2Id(address);
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
}
