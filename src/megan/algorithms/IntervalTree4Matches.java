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

import javafx.concurrent.Task;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.util.interval.Interval;
import megan.util.interval.IntervalTree;

import java.util.HashSet;
import java.util.Set;

/**
 * computes interval tree of all matches to keep for a read block
 * Created by huson on 3/29/17.
 */
public class IntervalTree4Matches {
    /**
     * selects the matches to keep for a given read and puts them into an interval tree
     *
     * @param readBlock
     * @param cNames
     * @param task        can be null
     * @return interval tree
     */
    public static IntervalTree<IMatchBlock> computeIntervalTree(IReadBlock readBlock, String[] cNames, Task task) {
        final IntervalTree<IMatchBlock> intervalTree = new IntervalTree<>();

        final Set<Interval<IMatchBlock>> toRemove = new HashSet<>();

        for (int m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
            final Interval<IMatchBlock> matchInterval = new Interval<>(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock);
            final int matchHash = matchBlock.getText().hashCode();
            final boolean matchReverse = (matchBlock.getAlignedQueryStart() > matchBlock.getAlignedQueryEnd());
            boolean ok = true;

            // check whether we need to keep this match, i.e. make sure it isn't overlapped by a stronger one
            for (Interval<IMatchBlock> otherInterval : intervalTree.getIntervals(matchInterval)) {
                if (!toRemove.contains(otherInterval)) {
                    final IMatchBlock otherBlock = otherInterval.getData();
                    final boolean otherReverse = (otherBlock.getAlignedQueryStart() > otherBlock.getAlignedQueryEnd());

                    if (matchReverse == otherReverse) {
                        final int otherHash = otherBlock.getText().hashCode();

                        if (dominates(cNames, otherInterval, otherHash, matchInterval, matchHash)) {
                            ok = false;
                        } else if (dominates(cNames, matchInterval, matchHash, otherInterval, otherHash)) {
                            toRemove.add(otherInterval);
                        }
                    }
                }
                if (task != null && task.isCancelled())
                    return intervalTree;
            }
            if (ok)
                intervalTree.add(matchInterval); // added incrementally
            if (toRemove.size() > Math.max(1000, 0.5 * intervalTree.size())) {
                intervalTree.removeAll(toRemove); // remove triggers complete rebuild of interval tree, that is why we do this in batches
                toRemove.clear();
            }
        }

        if (toRemove.size() > 0)
            intervalTree.removeAll(toRemove); // remove any remaining ones to be removed
        return intervalTree;
    }

    /**
     * determines whether match associated with interval1 dominates the match associated with interval2
     *
     * @param interval1
     * @param hash1
     * @param interval2
     * @param hash2
     * @return true, if first match dominates the second
     */
    private static boolean dominates(String[] cNames, Interval<IMatchBlock> interval1, int hash1, Interval<IMatchBlock> interval2, int hash2) {
        if (interval1.overlap(interval2) >= 0.95 * interval2.length()) {
            final IMatchBlock match1 = interval1.getData();
            final IMatchBlock match2 = interval2.getData();
            if (match1.getBitScore() / interval1.length() > match2.getBitScore() / interval2.length() ||
                    match1.getBitScore() / interval1.length() == match2.getBitScore() / interval2.length() && (hash1 < hash2 || (hash1 == hash2 && match1.getUId() < match2.getUId()))) {
                boolean hasUniqueClassification = false;
                for (String cName : cNames) {
                    if (match1.getId(cName) <= 0 && match2.getId(cName) > 0) {
                        hasUniqueClassification = true;
                        break;
                    }
                }
                return !hasUniqueClassification;
            }
        }
        return false;
    }

}
