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

package megan.algorithms;

import javafx.concurrent.Task;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.util.interval.Interval;
import megan.util.interval.IntervalTree;

import java.util.ArrayList;

/**
 * computes interval tree of all matches to keep for a read block
 * Created by huson on 3/29/17.
 */
public class IntervalTree4Matches {
    /**
     * selects the matches to keep for a given read and puts them into an interval tree
     *
     * @param readBlock
     * @param task      can be null
     * @return interval tree
     */
    public static IntervalTree<IMatchBlock> computeIntervalTree(IReadBlock readBlock, Task task) {
        final IntervalTree<IMatchBlock> intervalTree = new IntervalTree<>();

        for (int m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
            intervalTree.add(new Interval<>(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock));
            if (task != null && task.isCancelled())
                break;
        }
        return intervalTree;
    }

    /**
     * extracts the set of dominating matches
     *
     * @param intervals
     * @param cNames                 dominator must have value of each of these for which the dominated does
     * @param classificationToReport if this is set to some classification, check only this for domination
     * @return dominating intervals
     */
    public static IntervalTree<IMatchBlock> extractDominatingIntervals(IntervalTree<IMatchBlock> intervals, String[] cNames, String classificationToReport) {

        if (!classificationToReport.equalsIgnoreCase("all")) {
            for (String cName : cNames) {
                if (cName.equalsIgnoreCase(classificationToReport)) {
                    cNames = new String[]{cName}; // only need to dominate on this classification
                    break;
                }
            }
        }

        final IntervalTree<IMatchBlock> allMatches = new IntervalTree<>();
        final IntervalTree<IMatchBlock> reverseMatches = new IntervalTree<>();
        for (IMatchBlock matchBlock : intervals.values()) {
            if (matchBlock.getAlignedQueryStart() <= matchBlock.getAlignedQueryEnd()) {
                allMatches.add(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock);
            } else
                reverseMatches.add(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock);
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
                            (other.getBitScore() > match.getBitScore() || other.getBitScore() == match.getBitScore() && other.getUId() < match.getUId())) {
                        boolean ok = true; // check that other interval has all annotations that this one has, otherwise it doesn't really dominate
                        for (String cName : cNames) {
                            if (match.getId(cName) > 0 && other.getId(cName) <= 0) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok)
                            toDelete.add(interval);
                    }
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
}
