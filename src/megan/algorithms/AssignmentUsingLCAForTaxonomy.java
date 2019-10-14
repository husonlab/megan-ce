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

import jloda.graph.Node;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.classification.data.ClassificationFullTree;
import megan.classification.data.Name2IdMap;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * computes the taxon assignment for a read, using the LCA algorithm
 * Daniel Huson, 7.2014
 * todo: merge with AssignmentUsingLCA
 */
public class AssignmentUsingLCAForTaxonomy implements IAssignmentAlgorithm {
    private String[] addresses;
    private final BitSet activeSet;
    private final Map<Character, Integer> ch2weight;

    private final boolean useIdentityFilter;
    private float proportionToCover = 1;

    private final ClassificationFullTree fullTree;
    private final IdMapper idMapper;
    private final Name2IdMap name2IdMap;

    private final boolean ignoreAncestralTaxa;

    /**
     * constructor
     *
     * @param cName
     * @param useIdentityFilter
     * @param percentToCover
     */
    public AssignmentUsingLCAForTaxonomy(String cName, boolean useIdentityFilter, float percentToCover) {
        this(cName, useIdentityFilter, percentToCover, true);
    }

    /**
     * constructor
     *
     * @param cName
     * @param useIdentityFilter
     * @param percentToCover
     * @param ignoreAncestralTaxa
     */
    public AssignmentUsingLCAForTaxonomy(String cName, boolean useIdentityFilter, float percentToCover, boolean ignoreAncestralTaxa) {
        fullTree = ClassificationManager.get(cName, false).getFullTree();
        idMapper = ClassificationManager.get(cName, true).getIdMapper();
        name2IdMap = ClassificationManager.get(cName, false).getIdMapper().getName2IdMap();
        addresses = new String[1000];
        activeSet = new BitSet();
        ch2weight = new HashMap<>(Character.MAX_VALUE, 1f);

        this.useIdentityFilter = useIdentityFilter;
        this.proportionToCover = percentToCover / 100f;

        this.ignoreAncestralTaxa = ignoreAncestralTaxa;
    }

    /**
     * determine the taxon id of a read from its matches
     *
     * @param activeMatches
     * @param readBlock
     * @return taxon id
     */
    public int computeId(BitSet activeMatches, IReadBlock readBlock) {
        if (readBlock.getNumberOfMatches() == 0)
            return IdMapper.NOHITS_ID;
        if (activeMatches.cardinality() == 0)
            return IdMapper.UNASSIGNED_ID;

        // compute addresses of all hit taxa:
        if (activeMatches.cardinality() > 0) {

            boolean hasDisabledMatches = false;

            // collect the addresses of all non-disabled taxa:
            int numberOfAddresses = 0;
            for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
                final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                int id = matchBlock.getTaxonId();
                if (id > 0) {
                    if (!idMapper.isDisabled(id)) {
                        final String address = fullTree.getAddress(id);
                        if (address != null) {
                            if (numberOfAddresses >= addresses.length) {
                                String[] tmp = new String[2 * addresses.length];
                                System.arraycopy(addresses, 0, tmp, 0, addresses.length);
                                addresses = tmp;
                            }
                            addresses[numberOfAddresses++] = address;
                        }
                    } else
                        hasDisabledMatches = true;
                }
            }

            // if there only matches to disabled taxa, then use them:
            if (numberOfAddresses == 0 && hasDisabledMatches) {
                for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
                    final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                    int id = matchBlock.getTaxonId();
                    if (id > 0) {
                        final String address = fullTree.getAddress(id);
                        if (address != null) {
                            if (numberOfAddresses >= addresses.length) {
                                String[] tmp = new String[2 * addresses.length];
                                System.arraycopy(addresses, 0, tmp, 0, addresses.length);
                                addresses = tmp;
                            }
                            addresses[numberOfAddresses++] = address;
                        }
                    }
                }
            }

            // compute LCA using addresses:
            if (numberOfAddresses > 0) {
                final int id;
                if (proportionToCover == 1) {
                    final String address = LCAAddressing.getCommonPrefix(addresses, numberOfAddresses, ignoreAncestralTaxa);
                    id = fullTree.getAddress2Id(address);
                } else {
                    final int weightToCover = (int) Math.min(numberOfAddresses, Math.ceil(proportionToCover * numberOfAddresses));
                    final String address = getPrefixCoveringWeight(weightToCover, addresses, numberOfAddresses);
                    id = fullTree.getAddress2Id(address);
                }
                if (id > 0) {
                    if (useIdentityFilter) {
                        return AssignmentUsingLCAForTaxonomy.adjustByPercentIdentity(id, activeMatches, readBlock, fullTree, name2IdMap);
                    }
                    return id;
                }
            }
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
    public int computeNaiveLCA(Collection<Integer> taxonIds) {
        if (taxonIds.size() == 0)
            return IdMapper.NOHITS_ID;
        else if (taxonIds.size() == 1)
            return taxonIds.iterator().next();

        if (taxonIds.size() > addresses.length) {  // grow, if necessary
            addresses = new String[taxonIds.size()];
        }

        int numberOfAddresses = 0;

        // compute addresses of all hit taxa:
        for (Integer id : taxonIds) {
            if (!idMapper.isDisabled(id)) {
                final String address = fullTree.getAddress(id);
                if (address != null) {
                    addresses[numberOfAddresses++] = address;
                }
            }
        }

        // compute LCA using addresses:
        if (numberOfAddresses > 0) {
            final String address = LCAAddressing.getCommonPrefix(addresses, numberOfAddresses, ignoreAncestralTaxa);
            return fullTree.getAddress2Id(address);
        }
        return IdMapper.UNASSIGNED_ID;
    }

    /**
     * get the LCA of two ids, not ignoring the case that one may be the lca of the other
     *
     * @param id1
     * @param id2
     * @return LCA of id1 and id2
     */
    @Override
    public int getLCA(int id1, int id2) {
        if (id1 == 0)
            return id2;
        else if (id2 == 0)
            return id1;
        else
            return fullTree.getAddress2Id(LCAAddressing.getCommonPrefix(new String[]{fullTree.getAddress(id1), fullTree.getAddress(id2)}, 2, ignoreAncestralTaxa));
    }

    /**
     * moves reads to higher taxa if the percent identity that they have is not high enough for the given taxonomic rank
     *
     * @param taxId
     * @param activeMatches
     * @param readBlock
     * @param tree
     * @return original or modified taxId
     */
    public static int adjustByPercentIdentity(int taxId, BitSet activeMatches, IReadBlock readBlock, ClassificationFullTree tree, Name2IdMap name2IdMap) {
        float bestPercentIdentity = 0;
        for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
            if (matchBlock.getPercentIdentity() > bestPercentIdentity)
                bestPercentIdentity = matchBlock.getPercentIdentity();
        }
        if (bestPercentIdentity >= 99 || bestPercentIdentity == 0)
            return taxId;

        boolean changed;
        do {
            changed = false;
            boolean ok = true;
            int rank = name2IdMap.getRank(taxId);
            switch (rank) {
                case 100: // species
                case 101: // subspecies
                    if (bestPercentIdentity < 99)
                        ok = false;
                    break;
                case 99: // species group
                case 98: // genus
                    if (bestPercentIdentity < 97)
                        ok = false;
                    break;
                case 5: // family
                    if (bestPercentIdentity < 95)
                        ok = false;
                    break;
                case 4: // order
                    if (bestPercentIdentity < 90)
                        ok = false;
                    break;
                case 3: // class
                    if (bestPercentIdentity < 85)
                        ok = false;
                    break;
                case 2: // phylum
                    if (bestPercentIdentity < 80)
                        ok = false;
                    break;
                default:
                case 0: // no rank
                    ok = false;
            }
            if (!ok) // must go up tree:
            {
                Node v = tree.getANode(taxId);
                if (v != null && v.getInDegree() > 0) {
                    Node w = v.getFirstInEdge().getSource();
                    taxId = (Integer) w.getInfo();
                    changed = true;
                }
            }
        } while (changed);
        return taxId;
    }


    /**
     * given a set of addresses, returns the longest prefix that equals or exceeds  the given weight threshold
     *
     * @param addresses
     * @return prefix
     */
    private String getPrefixCoveringWeight(int weightToCover, String[] addresses, int length) {
        activeSet.clear();
        ch2weight.clear();

        for (int i = 0; i < length; i++) {
            activeSet.set(i);
        }

        final StringBuilder buf = new StringBuilder();

        for (int pos = 0; ; pos++) {
            for (int i = activeSet.nextSetBit(0); i != -1; i = activeSet.nextSetBit(i + 1)) {
                if (pos == addresses[i].length()) {
                    activeSet.set(i, false); // run out of symbols
                    //  weightToCover -= 1;   // this node lies on route to best node, so it is covered and its weight can  be removed from weightToCover
                } else {
                    char ch = addresses[i].charAt(pos);
                    ch2weight.merge(ch, 1, Integer::sum);
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
            return "";
    }
}

