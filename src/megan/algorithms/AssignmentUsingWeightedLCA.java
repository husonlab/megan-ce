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
import jloda.util.ProgramProperties;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.classification.data.ClassificationFullTree;
import megan.classification.data.Name2IdMap;
import megan.daa.connector.MatchBlockDAA;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;

import java.util.*;

/**
 * computes the assignment for a read, using the Weighted LCA algorithm
 * This is essentially the same algorithm that is used in MetaScope
 * <p>
 * Daniel Huson, 3.2016
 */
public class AssignmentUsingWeightedLCA implements IAssignmentAlgorithm {
    private final String cName;
    private final boolean cNameIsTaxonomy;
    private final ClassificationFullTree fullTree;
    private final Name2IdMap name2IdMap;
    private final IdMapper idMapper;

    private final int[] refId2weight;
    private final Map<String, Integer> ref2weight; // map reference sequence to number of reads associated with it
    private final Taxon2SpeciesMapping taxon2SpeciesMapping;

    private final boolean useIdentityFilter;
    private final float percentToCover;
    private final boolean allowBelowSpeciesAssignment = ProgramProperties.get("allowWeightedLCABelowSpecies", false);

    private final Map<Character, Integer> ch2weight = new HashMap<>(Character.MAX_VALUE, 1f);

    private WeightedAddress[] addressingArray = new WeightedAddress[0];

    private boolean ignoreAncestors = true; // alignments to ancestors are considered ok

    /**
     * constructor
     *
     * @param cName
     * @param refId2Weight
     * @param ref2weight
     * @param percentToCover
     */
    public AssignmentUsingWeightedLCA(final String cName, final int[] refId2Weight, final Map<String, Integer> ref2weight, final Taxon2SpeciesMapping taxon2SpeciesMapping, final float percentToCover, final boolean useIdentityFilter) {
        this.cName = cName;
        this.useIdentityFilter = useIdentityFilter;
        fullTree = ClassificationManager.get(cName, true).getFullTree();
        idMapper = ClassificationManager.get(cName, true).getIdMapper();
        name2IdMap = ClassificationManager.get(cName, true).getName2IdMap();
        cNameIsTaxonomy = (cName.equals(Classification.Taxonomy));
        this.refId2weight = refId2Weight;
        this.ref2weight = ref2weight;
        this.taxon2SpeciesMapping = taxon2SpeciesMapping;

        this.percentToCover = (percentToCover >= 99.9999 ? 100 : percentToCover);

        addressingArray = resizeArray(addressingArray, 1000); // need to call this method so that each element is set
    }

    /**
     * determine the taxon id of a read from its matches
     *
     * @param activeMatches
     * @param readBlock
     * @return taxon id
     */
    public int computeId(final BitSet activeMatches, final IReadBlock readBlock) {
        if (readBlock.getNumberOfMatches() == 0)
            return IdMapper.NOHITS_ID;
        if (activeMatches.cardinality() == 0)
            return IdMapper.UNASSIGNED_ID;

        // compute addresses of all hit taxa:
        if (activeMatches.cardinality() > 0) {
            int arrayLength = 0;

            boolean hasDisabledMatches = false;

            // collect the addresses of all non-disabled taxa:
            for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
                final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                int taxId = (cNameIsTaxonomy ? matchBlock.getTaxonId() : matchBlock.getId(cName));

                if (taxId > 0) {
                    if (!allowBelowSpeciesAssignment) {
                        taxId = taxon2SpeciesMapping.getSpeciesOrReturnTaxonId(taxId);
                    }

                    if (!idMapper.isDisabled(taxId)) {
                        final String address = fullTree.getAddress(taxId);
                        if (address != null) {
                            if (arrayLength >= addressingArray.length)
                                addressingArray = resizeArray(addressingArray, 2 * addressingArray.length);

                            if (ref2weight != null) {
                                final String ref = matchBlock.getTextFirstWord();
                                Integer weight = ref != null ? ref2weight.get(ref) : null;
                                if (weight == null)
                                    weight = 1;
                                addressingArray[arrayLength++].set(address, weight);
                            } else {
                                final int refId = ((MatchBlockDAA) matchBlock).getSubjectId();
                                int weight = Math.max(1, refId2weight[refId]);
                                try {
                                    addressingArray[arrayLength++].set(address, weight);
                                } catch (NullPointerException ex) {
                                    Basic.caught(ex);
                                    throw ex;
                                }
                            }
                        }
                    } else
                        hasDisabledMatches = true;
                }
            }

            // if there only matches to disabled taxa, then use them:
            if (arrayLength == 0 && hasDisabledMatches) {
                for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
                    final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                    int taxId = (cNameIsTaxonomy ? matchBlock.getTaxonId() : matchBlock.getId(cName));
                    if (taxId > 0) {
                        if (!allowBelowSpeciesAssignment) {
                            taxId = taxon2SpeciesMapping.getSpeciesOrReturnTaxonId(taxId);
                        }

                        if (!idMapper.isDisabled(taxId)) {
                            final String address = fullTree.getAddress(taxId);
                            if (address != null) {
                                if (arrayLength >= addressingArray.length)
                                    addressingArray = resizeArray(addressingArray, 2 * addressingArray.length);

                                if (ref2weight != null) {
                                    final String ref = matchBlock.getTextFirstWord();
                                    Integer weight = ref2weight.get(ref);
                                    if (weight == null)
                                        weight = 1;
                                    addressingArray[arrayLength++].set(address, weight);

                                } else {
                                    final int refId = ((MatchBlockDAA) matchBlock).getSubjectId();
                                    int weight = Math.max(1, refId2weight[refId]);
                                    addressingArray[arrayLength++].set(address, weight);
                                }
                            }
                        }
                    }
                }
            }

            // compute LCA using addresses:
            if (arrayLength > 0) {
                final String address = computeWeightedLCA(percentToCover, addressingArray, arrayLength);
                int id = fullTree.getAddress2Id(address);
                if (id > 0) {
                    if (useIdentityFilter) {
                        return AssignmentUsingLCAForTaxonomy.adjustByPercentIdentity(id, activeMatches, readBlock, fullTree, name2IdMap);
                    }
                    if (allowBelowSpeciesAssignment)
                        return id;
                    else
                        return taxon2SpeciesMapping.getSpeciesOrReturnTaxonId(id);
                }
            }
        }

        // although we had some hits, couldn't make an assignment
        return IdMapper.UNASSIGNED_ID;
    }

    /**
     * get the LCA of two ids
     *
     * @param id1
     * @param id2
     * @return LCA of id1 and id2, not ignoring the case that one may be the lca of the other
     */
    @Override
    public int getLCA(int id1, int id2) {
        if (id1 == 0)
            return id2;
        else if (id2 == 0)
            return id1;
        else
            return fullTree.getAddress2Id(LCAAddressing.getCommonPrefix(new String[]{fullTree.getAddress(id1), fullTree.getAddress(id2)}, 2, false));
    }

    /**
     * compute the weight LCA for a set of taxa and weights
     *
     * @param percentToCover
     * @param taxon2weight
     * @return LCA address
     */
    public String computeWeightedLCA(final float percentToCover, final Map<Integer, Integer> taxon2weight) {
        int arrayLength = 0;
        for (Integer taxonId : taxon2weight.keySet()) {
            String address = fullTree.getAddress(taxonId);
            if (address != null) {
                if (arrayLength >= addressingArray.length) {
                    addressingArray = resizeArray(addressingArray, 2 * addressingArray.length);
                }
                addressingArray[arrayLength++].set(address, taxon2weight.get(taxonId));
            }
            // else
            //     System.err.println("Unknown taxonId: "+taxonId);
        }
        return computeWeightedLCA(percentToCover, addressingArray, arrayLength);
    }

    /**
     * compute the address of the weighted LCA
     *
     * @param percentToCover
     * @param array
     * @param origLength
     * @return address or ""
     */
    private String computeWeightedLCA(final float percentToCover, final WeightedAddress[] array, final int origLength) {
        if (origLength == 0)
            return "";
        // sort:
        Arrays.sort(array, 0, origLength, (a, b) -> a.address.compareTo(b.address));
        // setup links:
        for (int i = 0; i < origLength - 1; i++) {
            array[i].next = array[i + 1];
        }
        array[origLength - 1].next = null;

        final WeightedAddress head = new WeightedAddress(null, 0); // head.next points to first element of list, but head is NOT the first element
        head.next = array[0];

        int length = mergeIdentical(head, origLength);

        int totalWeight = getTotalWeight(head);
        int weightToCover = (int) Math.min(totalWeight,Math.ceil((totalWeight / 100.0) * percentToCover));

        for (int pos = 0; ; pos++) { // look at next letter after current prefix
            ch2weight.clear();
            // determine weights for each letter at pos, remove any addresses that equal the prefix:
            {
                WeightedAddress prev = head; // we are using a single-linked list, so need to update prev.next to delete current
                for (WeightedAddress current = head.next; current != null; current = current.next) {
                    final String address = current.address;
                    if (pos == address.length()) { // current has run out of symbols
                        if (--length == 0) // run out of addresses, return  prefix
                            return address.substring(0, pos);
                        prev.next = current.next;
                        if (ignoreAncestors) {
                            // this node lies on route to best node, so it is covered and its weight can  be removed from totalWeight
                            totalWeight -= current.weight;
                            weightToCover = ((int) Math.min(totalWeight,Math.ceil((totalWeight / 100.0) * percentToCover)));
                            // Note: prev does not change
                        }
                    } else {
                        final char ch = address.charAt(pos);
                        final Integer count = ch2weight.get(ch);
                        ch2weight.put(ch, count == null ? current.weight : count + current.weight);
                        prev = current;
                    }
                }
            }

            // determine the heaviest character
            // no way that weight can be null
            char bestCh = 0;
            int bestCount = 0;
            for (char ch : ch2weight.keySet()) {
                int weight = ch2weight.get(ch);
                if (weight > bestCount) {
                    bestCh = ch;
                    bestCount = weight;
                }
            }

            if (bestCount < weightToCover) // best count no longer good enough, return current prefix
                return head.next.getAddress().substring(0, pos);

            // remove all that do not match the heaviest character:
            {
                WeightedAddress prev = head;
                for (WeightedAddress current = head.next; current != null; current = current.next) {
                    final String address = current.address;
                    if (address.charAt(pos) != bestCh) { // remove the current from the list
                        if (--length == 0)
                            return address.substring(0, pos);
                        prev.next = current.next;
                        // Note: prev does not change
                    } else
                        prev = current;
                }
            }
        }
    }

    /**
     * merge identical entries, using max weight for identical taxa. After running this, still have start=0
     *
     * @param length
     * @return new length
     */
    private static int mergeIdentical(final WeightedAddress headPtr, int length) {
        for (WeightedAddress a = headPtr.next; a != null; a = a.next) {
            for (WeightedAddress b = a.next; b != null; b = b.next) {
                if (a.getAddress().equals(b.getAddress())) {
                    if (b.weight > a.weight) // keep the maximum weight, NOT the sum
                        a.weight = b.weight;
                    a.next = b.next;
                    length--;
                } else
                    break;
            }
        }
        return length;
    }

    /**
     * compute total weight.
     *
     * @param head
     * @return sum of weights
     */
    private static int getTotalWeight(final WeightedAddress head) {
        int totalWeight = 0;
        for (WeightedAddress a = head.next; a != null; a = a.next) {
            totalWeight += a.weight;
        }
        return totalWeight;
    }

    /**
     * converts an address to numbers of easier display
     *
     * @param address
     * @return as numbers
     */
    private static String toNumbers(String address) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < address.length(); i++)
            buf.append(String.format("%d.", (int) address.charAt(i)));
        return buf.toString();
    }

    /**
     * utility for resizing an array of weighted addresses
     *
     * @param array
     * @param size
     * @return new array
     */
    private static WeightedAddress[] resizeArray(WeightedAddress[] array, int size) {
        final WeightedAddress[] result = new WeightedAddress[size];
        System.arraycopy(array, 0, result, 0, array.length);
        for (int i = array.length; i < result.length; i++)
            result[i] = new WeightedAddress();
        return result;
    }

    public float getPercentToCover() {
        return percentToCover;
    }

    public ClassificationFullTree getFullTree() {
        return fullTree;
    }

    public boolean isIgnoreAncestors() {
        return ignoreAncestors;
    }

    public void setIgnoreAncestors(boolean ignoreAncestors) {
        this.ignoreAncestors = ignoreAncestors;
    }

    /**
     * address and weight
     */
    public static class WeightedAddress {
        private String address;
        private int weight;
        private WeightedAddress next;

        /**
         * default constructor
         */
        public WeightedAddress() {
        }

        /**
         * constructor
         *
         * @param address
         * @param weight
         */
        public WeightedAddress(String address, int weight) {
            this.address = address;
            this.weight = weight;
        }

        void set(String address, int weight) {
            this.address = address;
            this.weight = weight;
        }

        String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        public String toString() {
            return "[" + toNumbers(address) + "," + weight + "]";
        }
    }
}
