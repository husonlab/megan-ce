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

package megan.parsers.blast;

import jloda.util.Pair;
import megan.util.interval.Interval;
import megan.util.interval.IntervalTree;

import java.util.Set;

/**
 * post process set of parsed matches
 * Created by huson on 2/20/17.
 */
public class PostProcessMatches {

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
    public static int apply(String queryName, Pair<byte[], Integer> matchesTextAndLength, boolean parseLongReads, IntervalTree<SAMIteratorBase.Match> matchesIntervalTree, Set<SAMIteratorBase.Match> matches) {
        if (parseLongReads) {
            matches.clear();
            for (Interval<SAMIteratorBase.Match> interval : matchesIntervalTree) {
                final SAMIteratorBase.Match match = interval.getData();
                boolean covered = false;
                for (Interval<SAMIteratorBase.Match> other : matchesIntervalTree.getIntervals(interval)) {
                    final SAMIteratorBase.Match otherMatch = other.getData();

                    if ((other.contains(interval) && 0.90 * otherMatch.bitScore > match.bitScore)) {
                        //   || (other.equals(interval) && (otherMatch.bitScore>match.bitScore || (otherMatch.bitScore==match.bitScore && otherMatch.samLine.compareTo(match.samLine)<0)))){
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
        } else {
            for (SAMIteratorBase.Match match : matches) {
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
            return matches.size();
        }
    }
}
