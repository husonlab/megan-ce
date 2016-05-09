/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import jloda.util.Basic;
import megan.util.RDPAssignmentDetailsFileFilter;

import java.io.IOException;
import java.util.TreeSet;


/**
 * parses a RDP assignment details file into SAM format
 * Daniel Huson, 4.2015
 */
public class RDPAssignmentDetails2SAMIterator extends SAMIteratorBase implements ISAMIterator {
    private final TreeSet<Match> matches = new TreeSet<>(new Match());
    private byte[] matchesText = new byte[10000];
    private int matchesTextLength = 0;

    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     */
    public RDPAssignmentDetails2SAMIterator(String fileName, int maxNumberOfMatchesPerRead) throws IOException {
        super(fileName, maxNumberOfMatchesPerRead);
        if (!RDPAssignmentDetailsFileFilter.getInstance().accept(fileName)) {
            close();
            throw new IOException("Not a RDP assignment details file: " + fileName);
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

        matchesTextLength = 0;

        final String line = nextLine();
        final String[] tokens = Basic.split(line, ';');

        int matchId = 0; // used to distinguish between matches when sorting
        matches.clear();

        int whichToken = 0;
        String queryName = tokens[whichToken++].trim();
        String direction = tokens[whichToken++];

        StringBuilder path = new StringBuilder();
        // add one match block for each percentage given:
        try {
            while (whichToken < tokens.length) {
                if (whichToken < tokens.length) {
                    String name = tokens[whichToken++];
                    if (name.equals("Root"))
                        name = "root";
                    path.append(name).append(";");
                    String scoreString = tokens[whichToken++];
                    if (!scoreString.endsWith("%")) {
                        System.err.println("Expected percentage in: " + line);
                        break;
                    }
                    float bitScore = Basic.parseFloat(scoreString);

                    if (matches.size() < getMaxNumberOfMatchesPerRead() || bitScore > matches.last().bitScore) {
                        Match match = new Match();
                        match.bitScore = bitScore;
                        match.id = matchId++;

                        String ref = Basic.toString(tokens, 0, whichToken, ";") + ";";
                        match.samLine = makeSAM(queryName, path.toString(), bitScore, ref);
                        matches.add(match);
                        if (matches.size() > getMaxNumberOfMatchesPerRead())
                            matches.remove(matches.last());
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Error parsing file near line: " + getLineNumber());
            if (incrementNumberOfErrors() >= getMaxNumberOfErrors())
                throw new RuntimeException("Too many errors");
        }

        if (matches.size() == 0) { // no matches, so return query name only
            if (queryName.length() > matchesText.length) {
                matchesText = new byte[2 * queryName.length()];
            }
            for (int i = 0; i < queryName.length(); i++)
                matchesText[matchesTextLength++] = (byte) queryName.charAt(i);
            matchesText[matchesTextLength++] = '\n';
            return 0;
        } else {
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
            return matches.size();
        }
    }

    /**
     * gets the matches text
     *
     * @return matches text
     */
    @Override
    public byte[] getMatchesText() {
        return matchesText;
    }

    /**
     * length of matches text
     *
     * @return length of text
     */
    @Override
    public int getMatchesTextLength() {
        return matchesTextLength;
    }

    /**
     * make a SAM line
     */
    private String makeSAM(String queryName, String refName, float bitScore, String line) throws IOException {

        return String.format("%s\t0\t%s\t0\t255\t*\t*\t0\t0\t*\t*\tAS:i:%d\t", queryName, refName, Math.round(bitScore)) + String.format("AL:Z:%s\t", Basic.replaceSpaces(line, ' '));
    }
}
