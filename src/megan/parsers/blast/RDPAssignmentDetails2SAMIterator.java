/*
 * RDPAssignmentDetails2SAMIterator.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.parsers.blast;

import jloda.swing.window.NotificationsInSwing;
import jloda.util.NumberUtils;
import jloda.util.Pair;
import jloda.util.StringUtils;
import megan.util.RDPAssignmentDetailsFileFilter;

import java.io.IOException;
import java.util.TreeSet;


/**
 * parses a RDP assignment details file into SAM format
 * Daniel Huson, 4.2015
 */
public class RDPAssignmentDetails2SAMIterator extends SAMIteratorBase implements ISAMIterator {
    private final TreeSet<Match> matches = new TreeSet<>(new Match());
    private final Pair<byte[], Integer> matchesTextAndLength = new Pair<>(new byte[10000], 0);

    /**
     * constructor
     *
	 */
    public RDPAssignmentDetails2SAMIterator(String fileName, int maxNumberOfMatchesPerRead) throws IOException {
        super(fileName, maxNumberOfMatchesPerRead);
        if (!RDPAssignmentDetailsFileFilter.getInstance().accept(fileName)) {
            NotificationsInSwing.showWarning("Might not be a 'RDP assignment details' file: " + fileName);
        }
        // skip all header lines:
        while (hasNextLine()) {
            if (nextLine().length() == 0)
                break;
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

        final var line = nextLine();

        try {
            var containsSemiColons = line.contains(";");
            var containsTabs = line.contains("\t");

            if (containsSemiColons) {
				final String[] tokens = StringUtils.split(line, ';');

                int matchId = 0; // used to distinguish between matches when sorting
                matches.clear();

                int whichToken = 0;
                final String queryName = tokens[whichToken++].trim();
                final String direction = tokens[whichToken++];

                final StringBuilder path = new StringBuilder();
                // add one match block for each percentage given:
                while (whichToken < tokens.length) {
                    String name = tokens[whichToken++];
                    if (name.equals("Root"))
                        name = "root";
                    path.append(name).append(";");
                    String scoreString = tokens[whichToken++];
                    if (!scoreString.endsWith("%")) {
                        System.err.println("Expected percentage in: " + line);
                        break;
                    }
					float bitScore = NumberUtils.parseFloat(scoreString);

                    final Match match = new Match();
                    match.bitScore = bitScore;
                    match.id = matchId++;

					final String ref = StringUtils.toString(tokens, 0, whichToken, ";") + ";";
                    match.samLine = makeSAM(queryName, path.toString(), bitScore, ref);
                    matches.add(match);
                }
                return getPostProcessMatches().apply(queryName, matchesTextAndLength, isParseLongReads(), null, matches, null);
            } else if (containsTabs) {
				final String[] tokens = StringUtils.split(line, '\t');

                int matchId = 0; // used to distinguish between matches when sorting
                matches.clear();

                int whichToken = 0;
                final String queryName = tokens[whichToken++].trim();

                var path = new StringBuilder();
                var foundRoot = false;
                // add one match block for each percentage given:
                while (whichToken < tokens.length) {
                    String name = tokens[whichToken++];
                    if (name.equals("Root")) {
                        name = "root";
                        foundRoot = true;
                    }
                    if (!foundRoot)
                        continue;
                    path.append(name).append(";");
                    String scoreString = tokens[whichToken++];
                    if (!scoreString.endsWith("%")) {
                        System.err.println("Expected percentage in: " + line);
                        break;
                    }
					var bitScore = NumberUtils.parseFloat(scoreString.substring(0, scoreString.length() - 1));

                    var match = new Match();
                    match.bitScore = bitScore;
                    match.id = matchId++;

					final String ref = StringUtils.toString(tokens, 0, whichToken, ";") + ";";
                    match.samLine = makeSAM(queryName, path.toString(), bitScore, ref);
                    matches.add(match);
                }
                return getPostProcessMatches().apply(queryName, matchesTextAndLength, isParseLongReads(), null, matches, null);
            }
        } catch (Exception ex) {
            System.err.println("Error parsing file near line: " + getLineNumber());
            if (incrementNumberOfErrors() >= getMaxNumberOfErrors())
                throw new RuntimeException("Too many errors");
        }
        return 0;
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
    private String makeSAM(String queryName, String refName, float bitScore, String line) {

        return String.format("%s\t0\t%s\t0\t255\t*\t*\t0\t0\t*\t*\tAS:i:%d\t", queryName, refName, Math.round(bitScore)) + String.format("AL:Z:%s\t", StringUtils.replaceSpaces(line, ' '));
    }
}
