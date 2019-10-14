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

import jloda.util.interval.Interval;
import jloda.util.interval.IntervalTree;
import megan.classification.IdMapper;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
 * assignment using multi-gene best hit
 * Daniel Huson, 2017
 */
public class AssignmentUsingMultiGeneBestHit implements IMultiAssignmentAlgorithm {
    private final IntervalTree<IMatchBlock> allMatches;
    private final IntervalTree<IMatchBlock> reverseMatches;
    private final Set<Integer> additionalClassIds = new HashSet<>();

    private final String cName;

    private int minOverlap = 18;

    /**
     * constructor
     *
     * @param cName
     */
    public AssignmentUsingMultiGeneBestHit(String cName) {
        this.cName = cName;
        allMatches = new IntervalTree<>();
        reverseMatches = new IntervalTree<>();
    }

    /**
     * computes the id for a read from its matches
     * matches
     *
     * @param activeMatches
     * @param readBlock
     * @return id or 0
     */
    public int computeId(BitSet activeMatches, IReadBlock readBlock) {
        additionalClassIds.clear();

        if (readBlock.getNumberOfMatches() == 0)
            return IdMapper.NOHITS_ID;
        if (activeMatches.cardinality() == 0)
            return IdMapper.UNASSIGNED_ID;

        int result = IdMapper.UNASSIGNED_ID;
        final IntervalTree<IMatchBlock> acceptedMatches = computeAcceptedMatches(activeMatches, readBlock);
        for (Interval<IMatchBlock> interval : acceptedMatches) {
            final int id = interval.getData().getId(cName);
            if (result == IdMapper.UNASSIGNED_ID && id > 0)
                result = id;
            else
                additionalClassIds.add(id);
        }
        return result;
    }

    /**
     * get additional classes found for this read
     *
     * @param index
     * @param numberOfClassifications used to set length of arrays returned in list
     * @param list                    of assignment arrays for use in DataProcessor
     * @return total number of classes
     */
    @Override
    public int getAdditionalClassIds(int index, int numberOfClassifications, ArrayList<int[]> list) {
        for (int classId : additionalClassIds) {
            final int[] array = new int[numberOfClassifications];
            array[index] = classId;
            list.add(array);
        }
        return additionalClassIds.size();
    }

    /**
     * get the LCA of two ids
     *
     * @param id1
     * @param id2
     * @return LCA of id1 and id2
     */
    @Override
    public int getLCA(int id1, int id2) {
        throw new RuntimeException("getLCA() called for assignment using best hit");
    }

    /**
     * computes set of matches accepted for determining the class ids for this read
     *
     * @param activeMatches
     * @param readBlock
     * @return number of ids
     */
    private IntervalTree<IMatchBlock> computeAcceptedMatches(BitSet activeMatches, IReadBlock readBlock) {
        if (activeMatches == null) {
            activeMatches = new BitSet();
            for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
                activeMatches.set(i);
            }
        }

        allMatches.clear();
        reverseMatches.clear();
        for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
            if (matchBlock.getId(cName) > 0) {
                if (matchBlock.getAlignedQueryStart() <= matchBlock.getAlignedQueryEnd()) {
                    allMatches.add(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock);
                } else
                    reverseMatches.add(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock);
            }
        }

        // remove all matches covered by stronger ones
        for (int i = 0; i < 2; i++) {
            final IntervalTree<IMatchBlock> matches = (i == 0 ? allMatches : reverseMatches);
            final ArrayList<Interval<IMatchBlock>> toDelete = new ArrayList<>();
            for (final Interval<IMatchBlock> interval : matches) {
                final IMatchBlock match = interval.getData();
                for (final Interval<IMatchBlock> otherInterval : matches.getIntervals(interval)) {
                    final IMatchBlock other = otherInterval.getData();
                    if (otherInterval.overlap(interval) > 0.5 * interval.length() &&
                            (other.getBitScore() > match.getBitScore() || other.getBitScore() == match.getBitScore() && other.getUId() < match.getUId()))
                        toDelete.add(interval);
                }
            }
            if (toDelete.size() > 0) {
                matches.removeAll(toDelete);
                toDelete.clear();
            }
        }
        allMatches.addAll(reverseMatches.intervals());
        return allMatches;
    }

    public int getMinOverlap() {
        return minOverlap;
    }

    public void setMinOverlap(int minOverlap) {
        this.minOverlap = minOverlap;
    }
}
