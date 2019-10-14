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

import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.BlastMode;
import jloda.util.Pair;
import jloda.util.interval.Interval;
import jloda.util.interval.IntervalTree;
import megan.parsers.sam.SAMMatch;
import megan.util.SAMFileFilter;

import java.io.IOException;
import java.util.TreeSet;


/**
 * parses a SAM File in SAM format
 * Daniel Huson, 2.2017
 */
public class SAM2SAMIterator extends SAMIteratorBase implements ISAMIterator {
    private final Pair<byte[], Integer> matchesTextAndLength = new Pair<>(new byte[10000000], 0);

    private final TreeSet<Match> matches = new TreeSet<>(new Match());
    private final IntervalTree<Match> matchesIntervalTree = new IntervalTree<>();

    private String currentMatchLine = null;

    private final SAMMatch samMatch;

    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     */
    protected SAM2SAMIterator(String fileName, int maxNumberOfMatchesPerRead, BlastMode blastMode) throws IOException {
        super(fileName, maxNumberOfMatchesPerRead);
        samMatch = new SAMMatch(blastMode);
        if (!SAMFileFilter.getInstance().accept(fileName)) {
            NotificationsInSwing.showWarning("Might not be a SAM file: " + fileName);
        }

        // skip header lines
        while (hasNextLine()) {
            String line = nextLine();
            if (!line.startsWith("@")) {
                pushBackLine(line);
                break;
            }
        }

        moveToNextSAMLine();
    }

    /**
     * is there more data?
     *
     * @return true, if more data available
     */
    @Override
    public boolean hasNext() {
        return currentMatchLine != null;
    }

    /**
     * gets the next matches
     *
     * @return number of matches
     */
    public int next() {
        if (currentMatchLine == null)
            return -1; // at end of file

        final String firstQuery = currentMatchLine;

        int matchId = 0; // used to distinguish between matches when sorting
        matches.clear();
        matchesTextAndLength.setSecond(0);
        matchesIntervalTree.clear();

        // get all matches for given query:
        try {
            while (true) {
                if (currentMatchLine != null && sameQuery(currentMatchLine, firstQuery)) {
                    samMatch.parse(currentMatchLine);

                    if (samMatch.isMatch()) {
                        final Match match = new Match();
                        match.bitScore = samMatch.getBitScore();
                        match.id = matchId++;
                        match.samLine = currentMatchLine;

                        if (isParseLongReads()) { // when parsing long reads we keep alignments based on local critera
                            matchesIntervalTree.add(new Interval<>(samMatch.getAlignedQueryStart(), samMatch.getAlignedQueryEnd(), match));
                        } else {
                            if (matches.size() < getMaxNumberOfMatchesPerRead() || samMatch.getBitScore() > matches.last().bitScore) {
                                matches.add(match);
                                if (matches.size() > getMaxNumberOfMatchesPerRead())
                                    matches.remove(matches.last());
                            }
                        }
                    }
                    moveToNextSAMLine();
                } else // new query or last alignment, in either case return
                {
                    break;
                }
            }

        } catch (Exception ex) {
            System.err.println("Error parsing file near line: " + getLineNumber() + ": " + ex.getMessage());
            if (incrementNumberOfErrors() >= getMaxNumberOfErrors())
                throw new RuntimeException("Too many errors");
        }

        return getPostProcessMatches().apply(firstQuery, matchesTextAndLength, isParseLongReads(), matchesIntervalTree, matches, null);
    }

    /**
     * move to the next match
     */
    private void moveToNextSAMLine() {
        if (hasNextLine()) {
            currentMatchLine = nextLine();
            // this skips over any empty lines:
            while (currentMatchLine != null && currentMatchLine.trim().length() == 0) {
                if (hasNextLine())
                    currentMatchLine = nextLine();
                else
                    currentMatchLine = null;
            }

        } else {
            currentMatchLine = null;
        }
    }

    /**
     * gets the matches text
     *
     * @return matches text
     */
    @Override
    public byte[] getMatchesText() {
        return matchesTextAndLength.getFirst();
    }

    /**
     * length of matches text
     *
     * @return length of text
     */
    @Override
    public int getMatchesTextLength() {
        return matchesTextAndLength.getSecond();
    }

    /**
     * do these two SAM lines refer to the same query sequence?
     *
     * @param samA
     * @param samB
     * @return true, if same query
     */
    private boolean sameQuery(String samA, String samB) {
        String[] tokensA = Basic.split(samA, '\t', 2);
        String[] tokensB = Basic.split(samB, '\t', 2);

        // not the same name, return false
        if (tokensA.length >= 1 && tokensB.length >= 1 && !tokensA[0].equals(tokensB[0]))
            return false;

        // check whether they are different "templates", that is, first and last of a read pair
        try {
            final int flagA = Basic.parseInt(tokensA[1]);
            final int flagB = Basic.parseInt(tokensB[1]);
            return (flagA & 192) == (flagB & 192); // second token is 'flag', must have same 7th and 8th bit for same query
        } catch (Exception ex) {
            return true;
        }
    }
}

