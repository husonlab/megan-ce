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

package megan.dialogs.lrinspector;

import megan.data.IMatchBlock;
import megan.util.interval.Interval;
import megan.util.interval.IntervalTree;

import java.util.List;

/**
 * some utilities
 * Created by huson on 2/21/17.
 */
public class Utilities {
    /**
     * analyses a set of matches
     *
     * @param matches
     * @return values associated with these matches
     */
    public static Values analyze(IntervalTree<IMatchBlock> matches) {
        final Values values = new Values();

        int currentStart = 0;
        int currentEnd = -1;
        float currentSumOfBitScores = 0;
        int currentSumOfLengths = 0;

        for (Interval<IMatchBlock> interval : matches) {
            // determine whether there is a better match that contains this one and if so, don't use this one
            {
                final List<Interval<IMatchBlock>> others = matches.getIntervals(interval.getStart(), interval.getEnd());
                boolean covered = false;
                for (Interval<IMatchBlock> other : others) {
                    if (other != interval && other.contains(interval) &&
                            (other.getData().getBitScore() > interval.getData().getBitScore()
                                    || other.getData().getBitScore() == interval.getData().getBitScore() && other.getData().getUId() < interval.getData().getUId())) {
                        covered = true;
                        break;
                    }
                }
                if (covered)
                    continue;
            }

            values.hits++;

            if (currentEnd == -1) { // the very first match
                currentStart = interval.getStart();
            } else if (interval.getStart() > currentEnd - 5) { // run of overlapping matches has ended
                values.coverage += (currentEnd - currentStart + 1);
                values.disjointScore += (currentSumOfBitScores * (currentEnd - currentStart + 1)) / (float) currentSumOfLengths;
                currentStart = interval.getStart();
                currentSumOfBitScores = 0;
                currentSumOfLengths = 0;
            }
            // else extending current run

            currentEnd = interval.getEnd();
            currentSumOfBitScores += interval.getData().getBitScore();
            currentSumOfLengths += interval.length();
            values.totalBitScore += interval.getData().getBitScore();
            values.maxScore = Math.max(values.maxScore, interval.getData().getBitScore());
        }
        // process the last segment:
        if (currentEnd != -1) {
            values.coverage += (currentEnd - currentStart + 1);
            values.disjointScore += (currentSumOfBitScores * (currentEnd - currentStart + 1)) / (float) currentSumOfLengths;
        }

        return values;
    }

    public static class Values {
        public float disjointScore;
        public float totalBitScore;
        public float maxScore;
        public int hits;
        public int coverage;
    }
}
