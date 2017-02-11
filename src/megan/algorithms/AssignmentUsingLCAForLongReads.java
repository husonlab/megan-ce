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
package megan.algorithms;

import megan.classification.IdMapper;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import net.sf.picard.util.IntervalTree;

import java.util.*;

/**
 * computes the taxon assignment for a read, using the LCA algorithm for long reads
 * Daniel Huson, 2.2017
 */
public class AssignmentUsingLCAForLongReads extends AssignmentUsingLCAForTaxonomy implements IAssignmentAlgorithm {
    private final IntervalTree<IMatchBlock> intervalTree;
    private final Map<Integer, Float> totalTaxonId2BitScore;
    private final Map<Integer, Float> geneTaxonId2BitScore;

    private final float topPercent;

    private int[][] array;

    /**
     * constructor
     *
     * @param cName
     * @param useIdentityFilter
     * @param topPercent        this only works if matches are not already prefiltered by top percent
     */
    public AssignmentUsingLCAForLongReads(String cName, boolean useIdentityFilter, float topPercent) {
        super(cName, useIdentityFilter);
        intervalTree = new IntervalTree<>();
        totalTaxonId2BitScore = new HashMap<>();
        geneTaxonId2BitScore = new HashMap<>();
        this.topPercent = topPercent;
        array = new int[1000][2];
    }

    /**
     * determine the taxon id of a read from its matches
     *
     * @param activeMatches
     * @param readBlock
     * @return taxon id
     */
    @Override
    public int computeId(BitSet activeMatches, IReadBlock readBlock) {
        if (readBlock.getNumberOfMatches() == 0)
            return IdMapper.NOHITS_ID;

        // compute addresses of all hit taxa:
        if (activeMatches.cardinality() > 0) {
            computeTotalDisjointScore(activeMatches, readBlock);

            int numberOfAddresses = 0;
            if (totalTaxonId2BitScore.size() > 0) {
                int arrayLength = 0;
                {
                    for (Integer taxId : totalTaxonId2BitScore.keySet()) {
                        if (arrayLength == array.length) // need to grow array
                        {
                            final int[][] tmp = new int[2 * arrayLength][2];
                            System.arraycopy(array, 0, tmp, 0, array.length);
                            array = tmp;
                        }
                        array[arrayLength][0] = Math.round(totalTaxonId2BitScore.get(taxId));
                        array[arrayLength][1] = taxId;
                        arrayLength++;
                    }
                }
                Arrays.sort(array, 0, arrayLength, new Comparator<int[]>() {
                    @Override
                    public int compare(int[] a, int[] b) {
                        if (a[0] > b[0])
                            return -1;
                        else if (a[0] < b[0])
                            return 1;
                        else if (a[1] < b[1])
                            return -1;
                        else if (a[1] > b[1])
                            return 1;
                        else
                            return 0;
                    }
                });
                float threshold = (float) (1.0 - topPercent / 100.0) * array[0][0];
                addresses[numberOfAddresses++] = fullTree.getAddress(array[0][1]);
                for (int i = 1; i < arrayLength; i++) {
                    if (array[i][0] >= threshold) {
                        if (numberOfAddresses == addresses.length) // need to grow array
                        {
                            final String[] tmp = new String[2 * numberOfAddresses];
                            System.arraycopy(tmp, 0, addresses, 0, addresses.length);
                            addresses = tmp;
                        }
                        addresses[numberOfAddresses++] = fullTree.getAddress(array[i][1]);
                    }
                }
            }

            // compute LCA using addresses:
            if (numberOfAddresses > 0) {
                final String address = LCAAddressing.getCommonPrefix(addresses, numberOfAddresses, true);
                int taxId = fullTree.getAddress2Id(address);
                if (taxId > 0) {
                    if (useIdentityFilter) {
                        taxId = adjustByPercentIdentity(taxId, activeMatches, readBlock, fullTree, name2idMap);
                    }
                    return taxId;
                }
            }
        }
        // although we had some hits, couldn't make an assignment
        return IdMapper.UNASSIGNED_ID;
    }

    /**
     * computes the mapping of taxon ids to total disjoint bit score
     *
     * @param activeMatches
     * @param readBlock
     * @return mapping
     */
    public Map<Integer, Float> computeTotalDisjointScore(BitSet activeMatches, IReadBlock readBlock) {
        if (activeMatches == null) {
            activeMatches = new BitSet();
            for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++)
                activeMatches.set(i);
        }
        // 1. separate alignments by query coordinates into bins
        // 2. for each taxon present, compute sum of bitscores across bins
        // 3. do lca on this

        // 1. separate alignments by query coordinates into bins
        intervalTree.clear();
        // first ignore disabled taxa:
        for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
            int id = matchBlock.getTaxonId();
            if (!idMapper.isDisabled(id)) {
                int a = matchBlock.getAlignedQueryStart();
                int b = matchBlock.getAlignedQueryEnd();
                if (a > b) {
                    int tmp = b;
                    b = a;
                    a = tmp;
                    if (a + 10 < b)
                        a += 10;
                    if (a < b - 10)
                        b -= 10;
                }
                intervalTree.put(a, b, matchBlock);
            }
        }
        // if nothing found, try again without ignoring disabled taxa:
        if (intervalTree.size() == 0) {
            for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
                final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                int a = matchBlock.getAlignedQueryStart();
                int b = matchBlock.getAlignedQueryEnd();
                if (a > b) {
                    int tmp = b;
                    b = a;
                    a = tmp;
                    if (a + 10 < b)
                        a += 10;
                    if (a < b - 10)
                        b -= 10;
                }
                intervalTree.put(a, b, matchBlock);
            }
        }

        // set taxon to bit score on a gene-by-gene basis:
        IntervalTree.Node<IMatchBlock> prev = null;
        totalTaxonId2BitScore.clear();
        geneTaxonId2BitScore.clear();
        for (IntervalTree.Node<IMatchBlock> node : intervalTree) {
            // if we have just finished a gene, add all gene bit scores to total ones and then clear gene scores:
            if (prev != null && node.getStart() >= prev.getEnd()) {
                for (Integer taxId : geneTaxonId2BitScore.keySet()) {
                    if (totalTaxonId2BitScore.keySet().contains(taxId))
                        totalTaxonId2BitScore.put(taxId, totalTaxonId2BitScore.get(taxId) + geneTaxonId2BitScore.get(taxId));
                    else
                        totalTaxonId2BitScore.put(taxId, geneTaxonId2BitScore.get(taxId));
                }
                geneTaxonId2BitScore.clear();
            }
            final IMatchBlock matchBlock = node.getValue();
            final int taxonId = matchBlock.getTaxonId();
            if (!geneTaxonId2BitScore.keySet().contains(taxonId) || matchBlock.getBitScore() > geneTaxonId2BitScore.get(taxonId))
                geneTaxonId2BitScore.put(taxonId, matchBlock.getBitScore());
            prev = node;
        }
        // process last gene:
        for (Integer taxId : geneTaxonId2BitScore.keySet()) {
            if (totalTaxonId2BitScore.keySet().contains(taxId))
                totalTaxonId2BitScore.put(taxId, totalTaxonId2BitScore.get(taxId) + geneTaxonId2BitScore.get(taxId));
            else
                totalTaxonId2BitScore.put(taxId, geneTaxonId2BitScore.get(taxId));
        }

        return totalTaxonId2BitScore;
    }
}

