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
import megan.util.interval.Interval;
import megan.util.interval.IntervalTree;

import java.util.*;

/**
 * computes the taxon assignment for a read, using the LCA algorithm for long reads
 * Daniel Huson, 2.2017
 */
public class AssignmentUsingLongReadLCA extends AssignmentUsingLCAForTaxonomy implements IAssignmentAlgorithm {
    private final Map<Integer, IntervalTree<IMatchBlock>> taxonId2Intervals;
    private final Map<Integer, Float> taxonId2BitScore;

    private float topPercent;

    private int[][] array;

    private int minOverlap = 18;

    /**
     * constructor
     *
     * @param cName
     * @param useIdentityFilter
     * @param topPercent        this only works if matches are not already prefiltered by top percent
     */
    public AssignmentUsingLongReadLCA(String cName, boolean useIdentityFilter, float topPercent) {
        super(cName, useIdentityFilter);

        taxonId2Intervals = new HashMap<>();
        taxonId2BitScore = new HashMap<>();

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
            computeTaxonId2MultiGeneScore(activeMatches, readBlock);

            int numberOfAddresses = 0;
            if (taxonId2BitScore.size() > 0) {
                int arrayLength = 0;
                {
                    for (Integer taxId : taxonId2BitScore.keySet()) {
                        if (arrayLength == array.length) // need to grow array
                        {
                            final int[][] tmp = new int[2 * arrayLength][2];
                            System.arraycopy(array, 0, tmp, 0, array.length);
                            array = tmp;
                        }
                        array[arrayLength][0] = Math.round(taxonId2BitScore.get(taxId));
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
     * computes multi-gene score
     * @param activeMatches
     * @param readBlock
     * @return
     */
    public Map<Integer, Float> computeTaxonId2MultiGeneScore(BitSet activeMatches, IReadBlock readBlock) {
        if (activeMatches == null) {
            activeMatches = new BitSet();
            for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++)
                activeMatches.set(i);
        }

        if (readBlock.getReadHeader().contains("R1.94")) {
            System.err.println(readBlock.getReadName());
        }

        // 1. separate alignments by query coordinates into bins
        taxonId2Intervals.clear();
        taxonId2BitScore.clear();

        // first ignore disabled taxa:
        for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
            final int id = matchBlock.getTaxonId();
            if (!idMapper.isDisabled(id)) {
                IntervalTree<IMatchBlock> intervals = taxonId2Intervals.get(id);
                if (intervals == null) {
                    intervals = new IntervalTree<>();
                    taxonId2Intervals.put(id, intervals);
                }
                intervals.add(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock);
            }
        }
        // if nothing found, try again without ignoring disabled taxa:
        if (taxonId2Intervals.size() == 0) {
            for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
                final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                final int id = matchBlock.getTaxonId();
                IntervalTree<IMatchBlock> intervals = taxonId2Intervals.get(id);
                if (intervals == null) {
                    intervals = new IntervalTree<>();
                    taxonId2Intervals.put(id, intervals);
                }
                intervals.add(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock);
            }
        }

        // delete all contained matches
        for (IntervalTree<IMatchBlock> intervals : taxonId2Intervals.values()) {
            // delete any contained matches of lesser score:
            final Set<Interval<IMatchBlock>> toDelete = new HashSet<>();
            for (Interval<IMatchBlock> interval : intervals) {
                final List<Interval<IMatchBlock>> overlappers = intervals.getIntervals(interval);
                final IMatchBlock match = interval.getData();
                for (Interval<IMatchBlock> intervalOther : overlappers) {
                    final IMatchBlock other = intervalOther.getData();
                    if (intervalOther.contains(interval) && (other.getBitScore() > match.getBitScore() || (other.getBitScore() == match.getBitScore()) && other.getUId() < match.getUId()))
                        toDelete.add(interval);
                }
            }
            if (toDelete.size() > 0)
                intervals.removeAll(toDelete);
        }

        for (Integer taxonId : taxonId2Intervals.keySet()) {
            final IntervalTree<IMatchBlock> intervals = taxonId2Intervals.get(taxonId);
            int segmentStart = -1;
            int segmentEnd = 0;
            int sumOfLengths = 0;
            float sumOfScores = 0;

            for (Interval<IMatchBlock> interval : intervals) {
                // this is the first item, initialize:
                if (segmentStart == -1) {
                    segmentStart = interval.getStart();
                    segmentEnd = interval.getEnd();
                    sumOfLengths = interval.length();
                    sumOfScores = interval.getData().getBitScore();
                }
                // if we have just finished a gene-segment, add all gene bit scores to total ones and then clear gene scores:
                else if (interval.getStart() > segmentEnd) {
                    final float bitScore = sumOfScores / sumOfLengths * (segmentEnd - segmentStart);
                    if (taxonId2BitScore.get(taxonId) == null)
                        taxonId2BitScore.put(taxonId, bitScore);
                    else
                        taxonId2BitScore.put(taxonId, bitScore + taxonId2BitScore.get(taxonId));
                    segmentStart = interval.getStart();
                    segmentEnd = interval.getEnd();
                    sumOfLengths = interval.length();
                    sumOfScores = interval.getData().getBitScore();
                } else // another overlapping alignment
                {
                    segmentEnd = interval.getEnd();
                    sumOfLengths += interval.length();
                    sumOfScores += interval.getData().getBitScore();
                }
            }
            // do the last segment:
            if (sumOfScores > 0) {
                final float bitScore = sumOfScores / sumOfLengths * (segmentEnd - segmentStart);
                if (taxonId2BitScore.get(taxonId) == null)
                    taxonId2BitScore.put(taxonId, bitScore);
                else
                    taxonId2BitScore.put(taxonId, bitScore + taxonId2BitScore.get(taxonId));
            }

        }
        return taxonId2BitScore;
    }

    public float getTopPercent() {
        return topPercent;
    }

    public void setTopPercent(float topPercent) {
        this.topPercent = topPercent;
    }

    public int getMinOverlap() {
        return minOverlap;
    }

    public void setMinOverlap(int minOverlap) {
        this.minOverlap = minOverlap;
    }
}


