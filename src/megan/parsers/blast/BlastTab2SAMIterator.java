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

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;


/**
 * parses a blast Tab file into SAM format
 * Daniel Huson, 4.2015
 */
public class BlastTab2SAMIterator extends SAMIteratorBase implements ISAMIterator {

    private byte[] matchesText = new byte[10000];
    private int matchesTextLength = 0;

    private TreeSet<Match> matches = new TreeSet<>(new Match());

    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     */
    public BlastTab2SAMIterator(String fileName, int maxNumberOfMatchesPerRead) throws IOException {
        super(fileName, maxNumberOfMatchesPerRead);
        setSkipCommentLines(true);
        final String[] lines = Basic.getFirstLinesFromFile(new File(fileName), 2);
        if (lines != null && lines.length == 2 && lines[1].split("\t").length < 11) {
            close();
            throw new IOException("File not a BLAST file in tabular format: " + fileName);
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

        String line = nextLine();
        final String queryName = Basic.getReadName(line);
        pushBackLine(line);

        int matchId = 0; // used to distinguish between matches when sorting
        matches.clear();

        // get all matches for given query:
        try {
            while (hasNextLine()) {
                // move to next match or next query:
                line = nextLine();

                if (line == null)// at end of file
                    break;

                if (line.startsWith("# "))
                    continue; // is a comment line
                if (line.startsWith("@") || line.startsWith((">")))
                    line = line.substring(1);

                if (!(line.startsWith(queryName) && Character.isWhitespace(line.charAt(queryName.length())))) { // at start of next query
                    pushBackLine(line);
                    break;
                }

                String[] tokens = Basic.split(line, '\t');
                if (tokens.length == 1)
                    continue;

                final String refName = tokens[1];
                if (!Basic.isFloat(tokens[2]))
                    throw new IOException("Expected float (percent identity), got: " + tokens[2]);
                float identity = (Float.parseFloat(tokens[2]));
                if (!Basic.isInteger(tokens[3]))
                    throw new IOException("Expected integer (length), got: " + tokens[3]);
                int alignmentLength = (Integer.parseInt(tokens[3]));
                if (!Basic.isInteger(tokens[4]))
                    throw new IOException("Expected integer (mismatches), got: " + tokens[4]);
                int mismatches = (Integer.parseInt(tokens[4]));
                if (!Basic.isInteger(tokens[5]))
                    throw new IOException("Expected integer (gap openings), got: " + tokens[5]);
                int gapOpenings = (Integer.parseInt(tokens[5]));
                if (!Basic.isInteger(tokens[6]))
                    throw new IOException("Expected integer (query start), got: " + tokens[6]);
                int queryStart = (Integer.parseInt(tokens[6]));
                if (!Basic.isInteger(tokens[7]))
                    throw new IOException("Expected integer (query end), got: " + tokens[7]);
                int queryEnd = (Integer.parseInt(tokens[7]));
                if (!Basic.isInteger(tokens[8]))
                    throw new IOException("Expected integer (subject start), got: " + tokens[8]);
                int subjStart = (Integer.parseInt(tokens[8]));
                if (!Basic.isInteger(tokens[9]))
                    throw new IOException("Expected integer (subject end), got: " + tokens[9]);
                int subjEnd = (Integer.parseInt(tokens[9]));
                if (!Basic.isFloat(tokens[10]))
                    throw new IOException("Expected float (expected), got: " + tokens[10]);
                float expect = (Float.parseFloat(tokens[10]));
                if (!Basic.isFloat(tokens[11]))
                    throw new IOException("Expected float (bit score), got: " + tokens[11]);
                float bitScore = (Float.parseFloat(tokens[11]));


                if (matches.size() < getMaxNumberOfMatchesPerRead() || bitScore > matches.last().bitScore) {
                    Match match = new Match();
                    match.bitScore = bitScore;
                    match.id = matchId++;
                    match.samLine = makeSAM(queryName, refName, bitScore, expect, identity, queryStart, subjStart, subjEnd, line);
                    matches.add(match);
                    if (matches.size() > getMaxNumberOfMatchesPerRead())
                        matches.remove(matches.last());
                }
            }
        } catch (Exception ex) {
            System.err.println("Error parsing file near line: " + getLineNumber() + ": " + ex.getMessage());
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
    private String makeSAM(String queryName, String refName, float bitScore, float expect, float percentIdentity, int queryStart, int referenceStart, int referenceEnd, String line) throws IOException {
        final StringBuilder buffer = new StringBuilder();

        buffer.append(queryName).append("\t");
        boolean reverseComplemented = (referenceStart > referenceEnd);
        if (reverseComplemented) {
            buffer.append(0x10); // SEQ is reverse complemented
        } else
            buffer.append(0);
        buffer.append("\t");
        buffer.append(refName).append("\t");
        if (reverseComplemented)
            buffer.append(referenceEnd).append("\t");
        else
            buffer.append(referenceStart).append("\t");
        buffer.append("255\t");

        buffer.append("*\t");
        buffer.append("*\t");
        buffer.append("0\t");
        buffer.append("0\t");
        buffer.append("*\t");
        buffer.append("*\t");

        buffer.append(String.format("AS:i:%d\t", (int) Math.round(bitScore)));
        buffer.append(String.format("ZE:f:%g\t", expect));
        buffer.append(String.format("ZI:i:%d\t", (int) Math.round(percentIdentity)));
        buffer.append(String.format("ZS:i:%s\t", queryStart));
        buffer.append(String.format("AL:Z:%s\t", Basic.replaceSpaces(line, ' ')));

        return buffer.toString();
    }
}
