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

package megan.parsers.blast;

import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.interval.Interval;
import jloda.util.interval.IntervalTree;

import java.util.List;
import java.util.Set;

/**
 * post process set of parsed matches
 * Daniel Huson, Feb 2017
 */
public class PostProcessMatches {
    private final static float defaultMinPercentToCoverToStronglyDominate = 90f;
    private final static float defaultTopPercentScoreToStronglyDominate = 90f;

    private float minProportionCoverToStronglyDominate = Math.min(1f, (float) ProgramProperties.get("MinPercentCoverToStronglyDominate", defaultMinPercentToCoverToStronglyDominate) / 100.0f);
    private float topProportionScoreToStronglyDominate = Math.min(1f, (float) ProgramProperties.get("TopPercentScoreToStronglyDominate", defaultTopPercentScoreToStronglyDominate) / 100.0f);

    private boolean parseLongReads = false;

    /**
     * constructor
     */
    public PostProcessMatches() {
    }

    /**
     * post process set of parsed matches
     *
     * @param queryName
     * @param matchesTextAndLength
     * @param parseLongReads
     * @param matchesIntervalTree
     * @param matches
     * @return number of matches returned
     */
    public int apply(String queryName, Pair<byte[], Integer> matchesTextAndLength, boolean parseLongReads, IntervalTree<Match> matchesIntervalTree, Set<Match> matches, List<Match> listOfMatches) {
        if (listOfMatches != null && listOfMatches.size() > 0) // this overrides all other considerations
        {
            byte[] matchesText = matchesTextAndLength.getFirst();
            int matchesTextLength = 0;
            for (Match match : listOfMatches) {
                byte[] bytes = match.samLine.getBytes();
                if (matchesTextLength + bytes.length + 1 >= matchesText.length) {
                    byte[] tmp = new byte[2 * (matchesTextLength + bytes.length + 1)];
                    System.arraycopy(matchesText, 0, tmp, 0, matchesTextLength);
                    matchesText = tmp;
                }
                System.arraycopy(bytes, 0, matchesText, matchesTextLength, bytes.length);
                matchesTextLength += bytes.length;
                matchesText[matchesTextLength++] = '\n';
            }
            matchesTextAndLength.set(matchesText, matchesTextLength);
            //System.err.println("Match: "+ Basic.toString(matchesText,0,matchesTextAndLength.get2()));
            return matches.size();
        }

        if (parseLongReads && matchesIntervalTree != null) {
            matches.clear();
            for (Interval<Match> interval : matchesIntervalTree) {
                final Match match = interval.getData();
                boolean covered = false;
                for (Interval<Match> other : matchesIntervalTree.getIntervals(interval)) {
                    final Match otherMatch = other.getData();

                    if (other.overlap(interval) > minProportionCoverToStronglyDominate * interval.length() && topProportionScoreToStronglyDominate * otherMatch.bitScore > match.bitScore) {
                        covered = true;
                        break;
                    }
                }
                if (!covered)
                    matches.add(interval.getData());
            }
        }

        byte[] matchesText = matchesTextAndLength.getFirst();
        int matchesTextLength = 0;

        if (matches.size() == 0) { // no matches, so return query name only
            if (queryName.length() > matchesTextAndLength.getFirst().length) {
                matchesTextAndLength.setFirst(new byte[2 * queryName.length()]);
            }
            for (int i = 0; i < queryName.length(); i++) {
                matchesTextAndLength.getFirst()[matchesTextLength++] = (byte) queryName.charAt(i);

            }
            matchesTextAndLength.getFirst()[matchesTextLength++] = '\n';
            matchesTextAndLength.set(matchesText, matchesTextLength);
            return 0;
        } else { // short reads
            for (Match match : matches) {
                byte[] bytes = match.samLine.getBytes();
                if (matchesTextLength + bytes.length + 1 >= matchesText.length) {
                    byte[] tmp = new byte[2 * (matchesTextLength + bytes.length + 1)];
                    System.arraycopy(matchesText, 0, tmp, 0, matchesTextLength);
                    matchesText = tmp;
                }
                System.arraycopy(bytes, 0, matchesText, matchesTextLength, bytes.length);
                matchesTextLength += bytes.length;
                matchesText[matchesTextLength++] = '\n';
            }
            matchesTextAndLength.set(matchesText, matchesTextLength);
            //System.err.println("Match: "+ Basic.toString(matchesText,0,matchesTextAndLength.get2()));
            return matches.size();
        }
    }

    public float getMinProportionCoverToStronglyDominate() {
        return minProportionCoverToStronglyDominate;
    }

    public float getTopProportionScoreToStronglyDominate() {
        return topProportionScoreToStronglyDominate;
    }

    public boolean isParseLongReads() {
        return parseLongReads;
    }

    public void setParseLongReads(boolean parseLongReads) {
        this.parseLongReads = parseLongReads;
        if (parseLongReads) {
            final float minPercentCoverToStronglyDominate = (float) ProgramProperties.get("MinPercentCoverToStronglyDominate", defaultMinPercentToCoverToStronglyDominate);
            if (minPercentCoverToStronglyDominate != defaultMinPercentToCoverToStronglyDominate)
                System.err.println("Using MinPercentCoverToStonglyDominate=" + minPercentCoverToStronglyDominate);
            minProportionCoverToStronglyDominate = minPercentCoverToStronglyDominate / 100.0f;

            final float topPercentScoreToStronglyDominate = (float) ProgramProperties.get("TopPercentScoreToStronglyDominate", defaultTopPercentScoreToStronglyDominate);
            if (topPercentScoreToStronglyDominate != defaultTopPercentScoreToStronglyDominate)
                System.err.println("Using TopPercentScoreToStronglyDominate=" + topPercentScoreToStronglyDominate);
            topProportionScoreToStronglyDominate = topPercentScoreToStronglyDominate / 100.0f;

            System.err.println(String.format("Input domination filter: MinPercentCoverToStronglyDominate=%.1f and TopPercentScoreToStronglyDominate=%.1f", minPercentCoverToStronglyDominate, topPercentScoreToStronglyDominate));
        }
    }
}
