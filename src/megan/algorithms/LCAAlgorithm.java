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

import jloda.util.Basic;
import megan.classification.IdMapper;
import megan.viewer.TaxonomyData;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * implements the naive and weighted LCA algorithms
 * Daniel Huson, 8.2014
 *
 * @deprecated
 */
class LCAAlgorithm {
    private String[] addresses = new String[1000];
    private int[] weights = new int[1000];
    private final BitSet toRemove = new BitSet();

    /**
     * returns the LCA of a set of taxon ids
     *
     * @param taxonIds
     * @return id
     */
    public int computeNaiveLCA(Collection<Integer> taxonIds) {
        if (taxonIds.size() == 0)
            return IdMapper.NOHITS_ID;
        else if (taxonIds.size() == 1)
            return taxonIds.iterator().next();

        if (taxonIds.size() > addresses.length) {  // grow, if necessary
            addresses = new String[taxonIds.size()];
            weights = new int[taxonIds.size()];
        }

        int numberOfAddresses = 0;

        // compute addresses of all hit taxa:
        for (Integer taxonId : taxonIds) {
            if (!TaxonomyData.isTaxonDisabled(taxonId)) {
                String address = TaxonomyData.getAddress(taxonId);
                if (address != null) {
                    addresses[numberOfAddresses++] = address;
                }
            }
        }

        // compute LCA using addresses:
        if (numberOfAddresses > 0) {
            final String address = LCAAddressing.getCommonPrefix(addresses, numberOfAddresses, true);
            return TaxonomyData.getAddress2Id(address);
        }

        // although we had some hits, couldn't make an assignment
        return IdMapper.UNASSIGNED_ID;
    }

    /**
     * returns the LCA of a set of taxon ids
     *
     * @param taxonIds
     * @return id
     */
    public int computeNaiveLCA(final int[] taxonIds, final int length) {
        if (length == 0)
            return IdMapper.NOHITS_ID;
        else if (length == 1)
            return taxonIds[0];

        if (taxonIds.length > addresses.length) {  // grow, if necessary
            addresses = new String[taxonIds.length];
            weights = new int[taxonIds.length];
        }

        int numberOfAddresses = 0;

        // compute addresses of all hit taxa:
        for (int i = 0; i < length; i++) {
            int taxonId = taxonIds[i];
            if (!TaxonomyData.isTaxonDisabled(taxonId)) {
                String address = TaxonomyData.getAddress(taxonId);
                if (address != null) {
                    addresses[numberOfAddresses++] = address;
                }
            }
        }

        // compute LCA using addresses:
        if (numberOfAddresses > 0) {
            final String address = LCAAddressing.getCommonPrefix(addresses, numberOfAddresses, true);
            return TaxonomyData.getAddress2Id(address);
        }

        // although we had some hits, couldn't make an assignment
        return IdMapper.UNASSIGNED_ID;
    }

    /**
     * computes the weighted LCA
     *
     * @param tax2weight
     * @return
     */
    public int computeWeightedLCA(final Map<Integer, Integer> tax2weight, final double proportionOfWeightToCover) {
        if (tax2weight.size() == 0)
            return IdMapper.UNASSIGNED_ID;
        if (tax2weight.size() == 1)
            return tax2weight.keySet().iterator().next();

        if (tax2weight.size() > addresses.length) {  // grow, if necessary
            addresses = new String[tax2weight.size()];
            weights = new int[tax2weight.size()];
        }

        int length = 0;
        int aTaxon = 0;
        int totalWeight = 0;
        for (Integer taxonId : tax2weight.keySet()) {
            if (taxonId > 0) {
                String address = TaxonomyData.getAddress(taxonId);
                Integer weight = tax2weight.get(taxonId);
                if (address != null && weight != null) {
                    addresses[length] = address;
                    weights[length] = weight;
                    totalWeight += weight;
                    if (length == 0)
                        aTaxon = taxonId;
                    length++;
                }
                /*
                else
                    System.err.println("(Taxon mapping error: TaxonId="+taxonId+" address="+address+" weight="+weight+")");
                    */
            }
        }
        if (length == 0)
            return IdMapper.UNASSIGNED_ID;
        else if (length == 1)
            return aTaxon;
        try {
            final int weightToCover = Math.min(totalWeight, (int) Math.ceil(proportionOfWeightToCover * totalWeight));
            final String address = getCommonPrefix(weightToCover, addresses, weights, length);
            if (address != null) {
                return TaxonomyData.getAddress2Id(address);
                /*
                                int result=TaxonomyTree.address2TaxId.get(address);
                if(result==77643) {
                         System.err.println("Returning taxonId="+result+": originalTaxa: " + Basic.toString(tax2weight.keySet(), ",") +
                                 "\noriginalWeights:  " + Basic.toString(tax2weight.values(), ",") + "\nLength: " + length +
                                 "\nweights to cover: "+(weights==null?null:Basic.toString(weights,0,length,","))+
                                 "\nWeight to cover:  " + weightToCover+" totalWeight: "+totalWeight);
                }
                                return result;
                */
            }
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return 1; // assign to root
    }

    private final BitSet activeSet = new BitSet();
    private final Map<Character, Integer> ch2weight = new HashMap<>(Character.MAX_VALUE, 1f);

    /**
     * given a set of addresses, returns the longest prefix that equals or exceeds  the given weight threshold
     *
     * @param addresses
     * @return prefix
     */
    private String getCommonPrefix(int weightToCover, String[] addresses, int[] weights, int length) {
        ch2weight.clear();
        activeSet.clear();
        for (int i = 0; i < length; i++) {
            activeSet.set(i);
        }

        final StringBuilder buf = new StringBuilder();

        for (int pos = 0; ; pos++) {
            for (int i = activeSet.nextSetBit(0); i != -1; i = activeSet.nextSetBit(i + 1)) {
                if (pos == addresses[i].length()) {
                    activeSet.set(i, false); // run out of symbols
                    // todo: the next line does not work because it can lead to the weightToCover decreasing all the way to zero
                    // weightToCover -= weights[i];   // this node lies on route to best node, so it is covered and its weight can  be removed from weightToCover
                } else {
                    char ch = addresses[i].charAt(pos);
                    Integer count = ch2weight.get(ch);
                    if (count == null)
                        ch2weight.put(ch, weights[i]);
                    else
                        ch2weight.put(ch, count + weights[i]);
                }
            }
            if (activeSet.cardinality() == 0)
                break;

            // determine the heaviest character
            Character bestCh = null;
            int bestCount = 0;
            for (Character ch : ch2weight.keySet()) {
                Integer weight = ch2weight.get(ch);
                if (weight != null && weight > bestCount) {
                    bestCh = ch;
                    bestCount = weight;
                }
            }

            if (bestCount >= weightToCover && bestCh != null)
                buf.append(bestCh);
            else
                break;

            for (int i = activeSet.nextSetBit(0); i != -1; i = activeSet.nextSetBit(i + 1)) {
                if (addresses[i].charAt(pos) != bestCh)   // no length problem here, if address too short then it will not be active
                    activeSet.set(i, false);   // not on best path, remove from active nodes
            }
            if (activeSet.cardinality() == 0)
                break;
            ch2weight.clear();
        }

        String result = buf.toString();

        if (result.length() > 0) {
            return result;
        } else
            return null;
    }
}
