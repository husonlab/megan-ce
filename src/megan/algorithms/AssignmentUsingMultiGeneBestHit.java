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

import jloda.util.Pair;
import megan.classification.IdMapper;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;

import java.util.ArrayList;
import java.util.BitSet;

/**
 * assignment using best hit
 * Created by huson on 1/22/16.
 */
public class AssignmentUsingMultiGeneBestHit implements IMultiAssignmentAlgorithm {
    private final ArrayList<ListItem> intervalList;

    private int numberOfSegments;
    private final ArrayList<Integer> classIds = new ArrayList<>();

    private final ArrayList<Pair<Float, Integer>> scoreAndIds = new ArrayList<>();

    private final String cName;

    private int minOverlap = 18;

    /**
     * constructor
     *
     * @param cName
     */
    public AssignmentUsingMultiGeneBestHit(String cName, String fileName) {
        this.cName = cName;
        intervalList = new ArrayList<>();

        // System.err.println("Using 'best hit'  assignment on " + cName);
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
        if (activeMatches.cardinality() == 0)
            return IdMapper.NOHITS_ID;
        numberOfSegments = computeClasses(activeMatches, readBlock);
        if (classIds.size() > 0)
            return classIds.get(0);
        else
            return IdMapper.UNASSIGNED_ID;
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
     * computes the mapping of taxon ids to total disjoint bit score
     *
     * @param activeMatches
     * @param readBlock
     * @return mapping
     */
    private int computeClasses(BitSet activeMatches, IReadBlock readBlock) {
        classIds.clear();

        if (activeMatches == null) {
            activeMatches = new BitSet();
            for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++)
                activeMatches.set(i);
        }
        // 1. separate alignments by query coordinates into bins
        // 2. for each taxon present, compute sum of bitscores across bins
        // 3. do lca on this

        // 1. separate alignments by query coordinates into bins
        intervalList.clear();
        // first ignore disabled taxa:
        for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
            intervalList.add(new ListItem(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock));
        }

        intervalList.sort(ListItem.comparator());

        // call functions on a gene-by-gene basis:
        int lengthSoFar = 0;

        int numberOfSegments = 1;
        for (ListItem item : intervalList) {
            // if we have just finished a gene, add all gene bit scores to total ones and then clear gene scores:
            if (item.getStart() > lengthSoFar - minOverlap) {
                if (scoreAndIds.size() > 0)
                    classIds.add(scoreAndIds.get(0).getSecond());
                scoreAndIds.clear();
                numberOfSegments++;
            }
            final IMatchBlock matchBlock = item.getMatchBlock();
            final int classId = matchBlock.getId(cName);
            if (classId != 0) {
                scoreAndIds.add(new Pair<>(matchBlock.getBitScore(), classId));
            }
            lengthSoFar = Math.max(lengthSoFar, item.getEnd());
        }

        // process last gene:
        if (scoreAndIds.size() > 0)
            classIds.add(scoreAndIds.get(0).getSecond());
        scoreAndIds.clear();
        return numberOfSegments;
    }

    @Override
    public int getOtherClassIds(int index, int numberOfClassifications, ArrayList<int[]> list) {
        for (int t = 1; t < classIds.size(); t++) {
            int[] array = new int[numberOfClassifications];
            array[index] = classIds.get(t);
            list.add(array);
        }
        return numberOfSegments;
    }

    public int getMinOverlap() {
        return minOverlap;
    }

    public void setMinOverlap(int minOverlap) {
        this.minOverlap = minOverlap;
    }
}
