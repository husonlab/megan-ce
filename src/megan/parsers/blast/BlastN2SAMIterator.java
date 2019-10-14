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
import jloda.util.interval.Interval;
import jloda.util.interval.IntervalTree;
import megan.util.BlastNTextFileFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;


/**
 * parses a blastn files into SAM format
 * Daniel Huson, 4.2015
 */
public class BlastN2SAMIterator extends SAMIteratorBase implements ISAMIterator {
    private final static String NEW_QUERY = "Query=";
    private final static String NEW_MATCH = ">";
    private final static String QUERY = "Query";
    private final static String SUBJECT = "Sbjct";
    private final static String SCORE = "Score";
    private final static String EXPECT = "Expect";
    private final static String LENGTH = "Length";
    private final static String IDENTITIES = "Identities";
    private final static String STRAND = "Strand";
    private final static String EQUALS = "=";

    private final Pair<byte[], Integer> matchesTextAndLength = new Pair<>(new byte[10000], 0);

    private final ArrayList<String> refHeaderLines = new ArrayList<>(1000);

    private final TreeSet<Match> matches = new TreeSet<>(new Match());
    private final IntervalTree<Match> matchesIntervalTree = new IntervalTree<>();
    private List<Match> listOfMatches = null; // if we want to iterate over all matches in the order they were obtained, then must set this to non-null

    private long numberOfReads = 0;

    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     */
    public BlastN2SAMIterator(String fileName, int maxNumberOfMatchesPerRead) throws IOException {
        super(fileName, maxNumberOfMatchesPerRead);
        if (!BlastNTextFileFilter.getInstance().accept(fileName)) {
            NotificationsInSwing.showWarning("Might not be a BLASTN file in TEXT format: " + fileName);
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

        String queryLine = getNextLineStartsWith(NEW_QUERY);
        if (queryLine == null)
            return -1; // at end of file

        final String queryName;
        {
            numberOfReads++;
            final String name = getNextToken(queryLine, NEW_QUERY).trim();
            queryName = (name.length() == 0 ? "Read" + numberOfReads : name);
        }

        matchesTextAndLength.setSecond(0);
        matches.clear();
        if (listOfMatches != null)
            listOfMatches.clear();
        matchesIntervalTree.clear();

        int matchId = 0; // used to distinguish between matches when sorting

        // get all matches for given query:
        try {
            while (hasNextLine()) {
                // move to next match or next query:
                String line = getNextLineStartsWith(NEW_QUERY, NEW_MATCH);

                if (line == null)// at end of file
                    break;

                if (line.startsWith(NEW_QUERY)) { // at start of next query
                    pushBackLine(line);
                    break;
                }

                // line is at start of new match
                // collect all the reference header lines:
                refHeaderLines.clear();
                while (true) {
                    if (startsWith(line, LENGTH))
                        break;
                    else
                        refHeaderLines.add(line.replaceAll("\\s+", " "));
                    line = nextLine().trim();
                }
                final int referenceLength = Basic.parseInt(getNextToken(line, LENGTH, EQUALS));
                final String refName = Basic.swallowLeadingGreaterSign(Basic.toString(refHeaderLines, " "));
                // Blast text downloaded from NBCI might have some text before the alignment starts:
                do {
                    line = skipEmptyLines();
                    if (line.startsWith("Score ="))
                        break;
                    else
                        line = nextLine().trim();
                }
                while (hasNext());


                boolean hasAnotherAlignmentAgainstReference = true;
                while (hasAnotherAlignmentAgainstReference) {
                    hasAnotherAlignmentAgainstReference = false;

                    final float bitScore = Basic.parseFloat(getNextToken(line, SCORE, EQUALS));
                    final int rawScore = Basic.parseInt(getNextToken(line, "("));
                    final float expect = Basic.parseFloat(getNextToken(line, EXPECT, EQUALS)); // usually Expect = but can also be Expect(2)=
                    line = nextLine();
                    final float percentIdentities = Basic.parseFloat(getNextToken(line, IDENTITIES, "("));
                    line = nextLine();
                    final String queryDirection = getNextLetters(line, STRAND, "=");
                    final String refDirection = getNextToken(line, "/");

                    // skip line containing Strand
                    String[] queryLineTokens = getNextLineStartsWith(QUERY).split("\\s+"); // split on white space
                    int queryStart = Basic.parseInt(queryLineTokens[1]);
                    StringBuilder queryBuf = new StringBuilder();
                    queryBuf.append(queryLineTokens[2]);
                    int queryEnd = Basic.parseInt(queryLineTokens[3]);

                    if (!hasNextLine())
                        break;
                    nextLine(); // skip middle line
                    String[] subjectLineTokens = getNextLineStartsWith(SUBJECT).split("\\s+");
                    int subjStart = Basic.parseInt(subjectLineTokens[1]);
                    StringBuilder subjBuf = new StringBuilder();
                    subjBuf.append(subjectLineTokens[2]);
                    int subjEnd = Basic.parseInt(subjectLineTokens[3]);
                    // if match is broken over multiple lines, collect all parts of match:
                    while (hasNextLine()) {
                        line = skipEmptyLines();
                        if (line == null)
                            break; // at EOF...
                        if (line.startsWith(NEW_QUERY)) { // at new query
                            pushBackLine(line);
                            break;
                        } else if (line.startsWith(NEW_MATCH)) { // start of new match
                            pushBackLine(line);
                            break;
                        } else if (line.startsWith(SCORE)) { // there is another match to the same query
                            if (isParseLongReads()) {
                                hasAnotherAlignmentAgainstReference = true; //  also report other matches to same query
                                break;
                            } else
                                pushBackLine(getNextLineStartsWith(NEW_QUERY)); // skip other matches to same query
                        } else if (line.startsWith(QUERY)) { // match continues...
                            queryLineTokens = line.split("\\s+");
                            queryBuf.append(queryLineTokens[2]);
                            queryEnd = Basic.parseInt(queryLineTokens[3]);
                            subjectLineTokens = getNextLineStartsWith(SUBJECT).split("\\s+");
                            subjBuf.append(subjectLineTokens[2]);
                            subjEnd = Basic.parseInt(subjectLineTokens[3]);
                        }
                    }

                    if (isParseLongReads()) { // when parsing long reads we keep alignments based on local critera
                        Match match = new Match();
                        match.bitScore = bitScore;
                        match.id = matchId++;
                        match.samLine = makeSAM(queryName, queryDirection, refName, referenceLength, refDirection, bitScore, expect, rawScore, percentIdentities, queryStart, queryEnd, subjStart, subjEnd, queryBuf.toString(), subjBuf.toString());
                        matchesIntervalTree.add(new Interval<>(queryStart, queryEnd, match));
                    } else {
                        if (matches.size() < getMaxNumberOfMatchesPerRead() || bitScore > matches.last().bitScore) {
                            Match match = new Match();
                            match.bitScore = bitScore;
                            match.id = matchId++;
                            match.samLine = makeSAM(queryName, queryDirection, refName, referenceLength, refDirection, bitScore, expect, rawScore, percentIdentities, queryStart, queryEnd, subjStart, subjEnd, queryBuf.toString(), subjBuf.toString());
                            matches.add(match);
                            if (matches.size() > getMaxNumberOfMatchesPerRead())
                                matches.remove(matches.last());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Error parsing file near line: " + getLineNumber() + ": " + ex.getMessage());
            if (incrementNumberOfErrors() >= getMaxNumberOfErrors())
                throw new RuntimeException("Too many errors");
        }

        return getPostProcessMatches().apply(queryName, matchesTextAndLength, isParseLongReads(), matchesIntervalTree, matches, listOfMatches);
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
    public static String makeSAM(String queryName, String queryDirection, String refName, int referenceLength, String refDirection, float bitScore, float expect, int rawScore, float percentIdentity, int queryStart, int queryEnd, int referenceStart, int referenceEnd, String alignedQuery, String alignedReference) throws IOException {

        final boolean reverseComplemented;

        if (queryDirection.equals("Plus")) {
            if (refDirection.equals("Minus")) {
                alignedQuery = Basic.getReverseComplement(alignedQuery);
                alignedReference = Basic.getReverseComplement(alignedReference);
                reverseComplemented = true;
            } else
                reverseComplemented = false;
        } else {
            if (refDirection.equals("Minus")) // query minus, ref minus
                throw new IOException("Can't parse match with Strand = Minus / Minus");
            else { // query minus, ref plus
                int tmp = queryStart;
                queryStart = queryEnd;
                queryEnd = tmp;
                tmp = referenceStart;
                referenceStart = referenceEnd;
                referenceEnd = tmp;
                reverseComplemented = true;
            }
        }

        final StringBuilder buffer = new StringBuilder();

        buffer.append(queryName).append("\t");
        if (reverseComplemented)
            buffer.append(0x10); // SEQ is reverse complemented
        else
            buffer.append(0);

        buffer.append("\t");

        buffer.append(refName).append("\t");
        if (reverseComplemented)
            buffer.append(referenceEnd).append("\t");
        else
            buffer.append(referenceStart).append("\t");
        buffer.append("255\t");

        Utilities.appendCigar(alignedQuery, alignedReference, buffer);

        buffer.append("\t");
        buffer.append("*\t");
        buffer.append("0\t");
        buffer.append("0\t");
        buffer.append(alignedQuery.replaceAll("-", "")).append("\t");
        buffer.append("*\t");

        buffer.append(String.format("AS:i:%d\t", Math.round(bitScore)));
        buffer.append(String.format("NM:i:%d\t", Utilities.computeEditDistance(alignedQuery, alignedReference)));
        buffer.append(String.format("ZL:i:%d\t", referenceLength));

        buffer.append(String.format("ZR:i:%d\t", rawScore));
        buffer.append(String.format("ZE:f:%g\t", expect));
        buffer.append(String.format("ZI:i:%d\t", Math.round(percentIdentity)));
        buffer.append(String.format("ZS:i:%s\t", queryStart));

        Utilities.appendMDString(alignedQuery, alignedReference, buffer);

        return buffer.toString();
    }


    public void setReportAllMatchesInOriginalOrder(boolean report) {
        listOfMatches = (report ? new ArrayList<>() : null);
    }

    public boolean isReportAllMatchesInOriginalOrder() {
        return listOfMatches != null;
    }
}
