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
import jloda.util.Pair;
import megan.util.RDPStandaloneFileFilter;

import java.io.IOException;
import java.util.TreeSet;


/**
 * parses a RDP assignment details file into SAM format
 * Daniel Huson, 4.2015
 */
public class RDPStandalone2SAMIterator extends SAMIteratorBase implements ISAMIterator {
    private final TreeSet<Match> matches = new TreeSet<>(new Match());
    private final Pair<byte[], Integer> matchesTextAndLength = new Pair<>(new byte[10000], 0);

    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     */
    public RDPStandalone2SAMIterator(String fileName, int maxNumberOfMatchesPerRead) throws IOException {
        super(fileName, maxNumberOfMatchesPerRead);
        if (!RDPStandaloneFileFilter.getInstance().accept(fileName)) {
            NotificationsInSwing.showWarning("Might not be a 'RDP standalone' file: " + fileName);
        }
    }

    /**
     * is there more data?
     *
     * @return true, if more data available
     */
    @Override
    public boolean hasNext() {
        return hasNextLine();
    }

    /**
     * gets the next matches
     *
     * @return number of matches
     */
    public int next() {
        if (!hasNextLine())
            return -1;

        matchesTextAndLength.setSecond(0);

        String line = nextLine();
        while (hasNextLine() && line.startsWith("#")) {
            line = nextLine();
        }

        if (line == null)
            return -1;

        final String[] tokens = line.replaceAll("\t\t", "\t").split("\t");

        if (tokens.length < 4) {
            System.err.println("Too few tokens in line: " + line);
            throw new RuntimeException("Too many errors");
        }

        int whichToken = 0;

        final String queryName = tokens[whichToken++];

        int matchId = 0; // used to distinguish between matches when sorting
        matches.clear();

        final StringBuilder path = new StringBuilder();
        // add one match block for each percentage given:
        try {
            while (whichToken < tokens.length) {
                String name = tokens[whichToken++];
                if (name.startsWith("\""))
                    name = name.substring(1, name.length() - 1);
                if (name.equals("Root"))
                    name = "root";
                path.append(name).append(";");
                final String rank = tokens[whichToken++];
                // rank ignored...

                final String scoreString = tokens[whichToken++];
                float bitScore = 100 * Basic.parseFloat(scoreString);

                final Match match = new Match();
                match.bitScore = bitScore;
                match.id = matchId++;

                final String ref = Basic.toString(tokens, 0, whichToken, ";") + ";";
                match.samLine = makeSAM(queryName, path.toString(), bitScore, ref);
                matches.add(match);
            }
        } catch (Exception ex) {
            System.err.println("Error parsing file near line: " + getLineNumber());
            if (incrementNumberOfErrors() >= getMaxNumberOfErrors())
                throw new RuntimeException("Too many errors");
        }

        return getPostProcessMatches().apply(queryName, matchesTextAndLength, isParseLongReads(), null, matches, null);
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
     * make a SAM line
     */
    private String makeSAM(String queryName, String refName, float bitScore, String line) throws IOException {
        return String.format("%s\t0\t%s\t0\t255\t*\t*\t0\t0\t*\t*\tAS:i:%d\t", queryName, refName, Math.round(bitScore)) + String.format("AL:Z:%s\t", Basic.replaceSpaces(line, ' '));
    }
}
