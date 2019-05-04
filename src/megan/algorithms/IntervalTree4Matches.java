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

import javafx.concurrent.Task;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.ProgressListener;
import jloda.util.interval.Interval;
import jloda.util.interval.IntervalTree;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * computes interval tree of all matches to keep for a read block
 * Created by huson on 3/29/17.
 */
public class IntervalTree4Matches {
    private final static float defaultMinPercentCoverToDominate = 50f;

    /**
     * selects the matches to keep for a given read and puts them into an interval tree
     *
     * @param readBlock
     * @param task      can be null
     * @param progress
     * @return interval tree
     */
    public static IntervalTree<IMatchBlock> computeIntervalTree(IReadBlock readBlock, Task task, ProgressListener progress) throws CanceledException {
        final IntervalTree<IMatchBlock> intervalTree = new IntervalTree<>();

        if (progress != null) {
            progress.setMaximum(readBlock.getNumberOfAvailableMatchBlocks());
            progress.setProgress(0);
        }

        for (int m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
            intervalTree.add(new Interval<>(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock));
            if (task != null && task.isCancelled())
                break;
            if (progress != null)
                progress.incrementProgress();
        }
        return intervalTree;
    }

    /**
     * extracts the set of dominating matches. A match is considered dominated, if more than 50% (default value) is covered by a match that has a better bit score, or the same bit score, but shorter length
     *
     * @param intervals              input
     * @param cNames                 dominator must have value of each of these for which the dominated does
     * @param classificationToReport if this is set to some classification, check only this for domination
     * @return dominating intervals
     */
    public static IntervalTree<IMatchBlock> extractDominatingIntervals(IntervalTree<IMatchBlock> intervals, String[] cNames, String classificationToReport) {
        final double dominationProportion = ProgramProperties.get("MinPercentCoverToDominate", defaultMinPercentCoverToDominate) / 100;

        if (!classificationToReport.equalsIgnoreCase("all")) {
            for (String cName : cNames) {
                if (cName.equalsIgnoreCase(classificationToReport)) {
                    cNames = new String[]{cName}; // only need to dominate on this classification
                    break;
                }
            }
        }

        final IntervalTree<IMatchBlock> allMatches = new IntervalTree<>(); // initially all forward matches, at the end, all resulting matches
        final IntervalTree<IMatchBlock> reverseMatches = new IntervalTree<>(); // all reverse matches
        for (IMatchBlock matchBlock : intervals.values()) {
            if (matchBlock.getAlignedQueryStart() <= matchBlock.getAlignedQueryEnd()) {
                allMatches.add(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock);
            } else
                reverseMatches.add(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock);
        }

        // these will be reused in the loop:
        final ArrayList<Pair<Interval<IMatchBlock>, Interval<IMatchBlock>>> pairs = new ArrayList<>(); // dominator,dominated pairs
        final Set<Interval<IMatchBlock>> dominated = new HashSet<>(); // dominated

        // remove all dominated matches
        for (int i = 0; i < 2; i++) {
            final IntervalTree<IMatchBlock> matches = (i == 0 ? allMatches : reverseMatches);

            while (matches.size() > 1) {
                // determine list of pairs of (dominator, dominated)
                pairs.clear();
                dominated.clear();

                for (final Interval<IMatchBlock> interval : matches) {
                    final IMatchBlock match = interval.getData();
                    for (final Interval<IMatchBlock> otherInterval : matches.getIntervals(interval)) {
                        final IMatchBlock other = otherInterval.getData();
                        if (otherInterval.overlap(interval) > dominationProportion * interval.length() &&
                                (other.getBitScore() > match.getBitScore() || other.getBitScore() == match.getBitScore() &&
                                        (other.getLength() < match.getLength() || (other.getLength() == match.getLength() && other.getUId() < match.getUId())))) {
                            boolean ok = true; // check that other interval has all annotations that this one has, otherwise it doesn't really dominate
                            for (String cName : cNames) {
                                if (match.getId(cName) > 0 && other.getId(cName) <= 0) {
                                    ok = false;
                                    break;
                                }
                            }
                            if (ok) {
                                pairs.add(new Pair<>(otherInterval, interval));
                                dominated.add(interval);
                                break; // found an other that dominates match...
                            }
                        }
                    }
                }

                // remove any match that is dominated by an undominated match:
                final Set<Interval<IMatchBlock>> toRemove = new HashSet<>();
                for (Pair<Interval<IMatchBlock>, Interval<IMatchBlock>> pair : pairs) {
                    if (!dominated.contains(pair.get1())) {
                        toRemove.add(pair.get2()); // first is not dominated and it dominates the second, so remove second
                    }
                }
                if (toRemove.size() > 0) {
                    final ArrayList<Interval<IMatchBlock>> toKeep = new ArrayList<>(matches.size());
                    for (Interval<IMatchBlock> interval : matches.getAllIntervals(false)) { // get unsorted intervals
                        if (!toRemove.contains(interval))
                            toKeep.add(interval);
                    }
                    matches.setAll(toKeep);
                } else
                    break; // no change
            }
        }
        allMatches.addAll(reverseMatches.intervals());
        return allMatches;
    }

    /**
     * extracts the set of strongly dominating matches. A match is considered strongly dominated, if  90% (default value) is covered by a match that has a bit score that is 10% better
     *
     * @param intervals              input
     * @param cNames                 dominator must have value of each of these for which the dominated does
     * @param classificationToReport if this is set to some classification, check only this for domination
     * @return dominating intervals
     */
    public static IntervalTree<IMatchBlock> extractStronglyDominatingIntervals(IntervalTree<IMatchBlock> intervals, String[] cNames, String classificationToReport) {
        final float minPercentCoverToDominate = (float) ProgramProperties.get("MinPercentCoverToStronglyDominate", 90f);
        final float minProportionCoverToDominate = minPercentCoverToDominate / 100.0f;

        final float topPercentScoreToDominate = (float) ProgramProperties.get("TopPercentScoreToStronglyDominate", 10f);
        final float scoreFactor = 1f - (topPercentScoreToDominate / 100.0f);

        if (!classificationToReport.equalsIgnoreCase("all")) {
            for (String cName : cNames) {
                if (cName.equalsIgnoreCase(classificationToReport)) {
                    cNames = new String[]{cName}; // only need to dominate on this classification
                    break;
                }
            }
        }

        final IntervalTree<IMatchBlock> allMatches = new IntervalTree<>(); // initially all foward matches, at the end, all resulting matches
        final IntervalTree<IMatchBlock> reverseMatches = new IntervalTree<>(); // all reverse matches
        for (IMatchBlock matchBlock : intervals.values()) {
            if (matchBlock.getAlignedQueryStart() <= matchBlock.getAlignedQueryEnd()) {
                allMatches.add(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock);
            } else
                reverseMatches.add(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock);
        }

        // these will be reused in the loop:
        final ArrayList<Pair<Interval<IMatchBlock>, Interval<IMatchBlock>>> pairs = new ArrayList<>(); // dominator,dominated pairs
        final Set<Interval<IMatchBlock>> dominated = new HashSet<>(); // dominated

        // remove all dominated matches
        for (int i = 0; i < 2; i++) {
            final IntervalTree<IMatchBlock> matches = (i == 0 ? allMatches : reverseMatches);

            while (matches.size() > 1) {
                // determine list of pairs of (dominator, dominated)
                pairs.clear();
                dominated.clear();

                for (final Interval<IMatchBlock> interval : matches) {
                    final IMatchBlock match = interval.getData();
                    for (final Interval<IMatchBlock> otherInterval : matches.getIntervals(interval)) {
                        final IMatchBlock other = otherInterval.getData();
                        if (otherInterval.overlap(interval) > minProportionCoverToDominate * interval.length() && scoreFactor * other.getBitScore() > match.getBitScore()) {
                            boolean ok = true; // check that other interval has all annotations that this one has, otherwise it doesn't really dominate
                            for (String cName : cNames) {
                                if (match.getId(cName) > 0 && other.getId(cName) <= 0) {
                                    ok = false;
                                    break;
                                }
                            }
                            if (ok) {
                                pairs.add(new Pair<>(otherInterval, interval));
                                dominated.add(interval);
                                break; // found an other that dominates match...
                            }
                        }
                    }
                }

                // remove any match that is dominated by an undominated match:
                final Set<Interval<IMatchBlock>> toRemove = new HashSet<>();
                for (Pair<Interval<IMatchBlock>, Interval<IMatchBlock>> pair : pairs) {
                    if (!dominated.contains(pair.get1())) {
                        toRemove.add(pair.get2()); // first is not dominated and it dominates the second, so remove second
                    }
                }
                if (toRemove.size() > 0) {
                    final ArrayList<Interval<IMatchBlock>> toKeep = new ArrayList<>(matches.size());
                    for (Interval<IMatchBlock> interval : matches.getAllIntervals(false)) { // get unsorted intervals
                        if (!toRemove.contains(interval))
                            toKeep.add(interval);
                    }
                    matches.setAll(toKeep);
                } else
                    break; // no change
            }
        }
        allMatches.addAll(reverseMatches.intervals());
        return allMatches;
    }
}
