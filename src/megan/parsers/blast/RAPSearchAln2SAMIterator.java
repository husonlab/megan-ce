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
import megan.util.RAPSearch2AlnFileFilter;

import java.io.IOException;
import java.util.TreeSet;


/**
 * parses a RAPSearch2 ALN files into SAM format
 * Daniel Huson, 4.2015
 */
public class RAPSearchAln2SAMIterator extends SAMIteratorBase implements ISAMIterator {
    private final static String vsString = " vs ";

    private final Pair<byte[], Integer> matchesTextAndLength = new Pair<>(new byte[10000], 0);

    private final TreeSet<Match> matches = new TreeSet<>(new Match());
    private final IntervalTree<Match> matchesIntervalTree = new IntervalTree<>();

    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     */
    public RAPSearchAln2SAMIterator(String fileName, int maxNumberOfMatchesPerRead) throws IOException {
        super(fileName, maxNumberOfMatchesPerRead);
        if (!RAPSearch2AlnFileFilter.getInstance().accept(fileName)) {
            NotificationsInSwing.showWarning("Might not be a RapSearch2 .aln file: " + fileName);
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
        String queryLine = getNextLineContains(vsString);
        if (queryLine == null)
            return -1; // at end of file

        final String queryName = Basic.swallowLeadingGreaterSign(Basic.getFirstWord(queryLine));
        pushBackLine(queryLine);

        int matchId = 0; // used to distinguish between matches when sorting
        matches.clear();
        matchesTextAndLength.setSecond(0);
        matchesIntervalTree.clear();

        // get all matches for given query:
        try {
            while (hasNextLine()) {
                queryLine = getNextLineContains(vsString);
                if (queryLine == null)
                    break; // end of file
                String currentQueryName = Basic.swallowLeadingGreaterSign(Basic.getFirstWord(queryLine));
                if (!currentQueryName.equals(queryName)) {
                    pushBackLine(queryLine); // start of next query
                    break;
                }

                final RapSearchMatch match = new RapSearchMatch();

                match.parseHeader(queryLine);
                match.parseLines(nextLine(), nextLine(), nextLine());


                if (isParseLongReads()) { // when parsing long reads we keep alignments based on local critera
                    match.samLine = makeSAM(queryName, match.referenceName, -1, match.bitScore, match.expected, 0, match.identity, match.frame, match.queryStart, match.queryEnd, match.refStart, match.refEnd, match.querySequence, match.refSequence);
                    matchesIntervalTree.add(new Interval<>(match.queryStart, match.queryEnd, match));
                } else {
                    if (matches.size() < getMaxNumberOfMatchesPerRead() || match.bitScore > matches.last().bitScore) {
                        match.id = matchId++;
                        match.samLine = makeSAM(queryName, match.referenceName, -1, match.bitScore, match.expected, 0, match.identity, match.frame, match.queryStart, match.queryEnd, match.refStart, match.refEnd, match.querySequence, match.refSequence);
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

        return getPostProcessMatches().apply(queryName, matchesTextAndLength, isParseLongReads(), matchesIntervalTree, matches, null);
    }

    /**
     * /**
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
    private String makeSAM(String queryName, String refName, int referenceLength, float bitScore, float expect, int rawScore, float percentIdentity, int frame, int queryStart, int queryEnd, int referenceStart, int referenceEnd, String alignedQuery, String alignedReference) {
        if (alignedQuery.contains("."))
            alignedQuery = alignedQuery.replaceAll("\\.", "X");

        //  if(alignedReference.contains("."))
        //      alignedReference=alignedReference.replaceAll("\\.","-");

        final StringBuilder buffer = new StringBuilder();
        buffer.append(queryName).append("\t");
        buffer.append(0);
        buffer.append("\t");
        buffer.append(refName).append("\t");
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
        buffer.append(String.format("ZF:i:%d\t", frame));
        buffer.append(String.format("ZS:i:%s\t", queryStart));

        Utilities.appendMDString(alignedQuery, alignedReference, buffer);

        return buffer.toString();
    }

    /**
     * a rapsearch match
     */
    static class RapSearchMatch extends Match {
        String readName;
        String referenceName;
        String referenceLine;
        int queryStart;
        int queryEnd;
        String querySequence;
        int refStart;
        int refEnd;
        String refSequence;
        float bitScore;
        float expected;
        int frame;
        int length;
        float identity;
        boolean isNoHit = false;

        final static String Query = "Query:";
        final static String Subject = "Sbjct:";
        final static String noHitString = "NO HIT";
        final static String bitsString = "bits=";
        final static String evalueString = "log(E-value)=";
        final static String evalueStringAlt = "log(Evalue)=";
        final static String identityString = "identity=";
        final static String lengthString = "aln-len=";
        final static String lengthStringAlt = "alnlen=";
        final static String frameString = "nFrame=";

        /**
         * parses the header line.
         *
         * @param aLine
         * @return true, if is hit, false if is no hit
         * @throws IOException
         */
        boolean parseHeader(String aLine) throws IOException {
            referenceLine = aLine;
            int index = aLine.indexOf(vsString);
            if (index <= 0) {
                index = aLine.indexOf(noHitString);
                if (index <= 0)
                    throw new IOException("Token 'vs' or 'NO HIT' not found in line: " + aLine);
                else
                    isNoHit = true;
            }
            readName = aLine.substring(aLine.charAt(0) == '>' ? 1 : 0, index).trim();
            if (isNoHit)
                return false;

            String suffix = aLine.substring(index + vsString.length()).trim();
            index = suffix.indexOf(" ");
            if (index <= 0)
                throw new IOException("Token ' ' not found after ' vs ' in line: " + aLine);
            referenceName = suffix.substring(0, index).trim();
            suffix = suffix.substring(index + 1).trim();
            String[] tokens = suffix.split(" ");

            if (tokens[0].startsWith(bitsString) && Basic.isFloat(tokens[0].substring(bitsString.length())))
                bitScore = Float.parseFloat(tokens[0].substring(bitsString.length()));
            else
                throw new IOException("Failed to parse  '" + bitsString + "' in: " + aLine);

            if (tokens[1].startsWith(evalueString) && Basic.isFloat(tokens[1].substring(evalueString.length())))
                expected = (float) Math.pow(10, Float.parseFloat(tokens[1].substring(evalueString.length())));
            else if (tokens[1].startsWith(evalueStringAlt) && Basic.isFloat(tokens[1].substring(evalueStringAlt.length())))
                expected = (float) Math.pow(10, Float.parseFloat(tokens[1].substring(evalueStringAlt.length())));
            else
                throw new IOException("Failed to parse '" + evalueString + "' or '" + evalueStringAlt + "' in: " + aLine);

            if (tokens[2].startsWith(identityString) && Basic.isFloat(tokens[2].substring(identityString.length(), tokens[2].length() - 1)))
                identity = Float.parseFloat(tokens[2].substring(identityString.length(), tokens[2].length() - 1));
            else
                throw new IOException("Failed to parse '" + identityString + "' in: " + aLine);

            if (tokens[3].startsWith(lengthString) && Basic.isInteger(tokens[3].substring(lengthString.length())))
                length = Integer.parseInt(tokens[3].substring(lengthString.length()));
            else if (tokens[3].startsWith(lengthStringAlt) && Basic.isInteger(tokens[3].substring(lengthStringAlt.length())))
                length = Integer.parseInt(tokens[3].substring(lengthStringAlt.length()));
            else
                throw new IOException("Failed to parse '" + lengthString + "' or '" + lengthStringAlt + "' in: " + aLine);

            if (tokens[6].startsWith(frameString) && Basic.isInteger(tokens[6].substring(frameString.length()))) {
                int f = Integer.parseInt(tokens[6].substring(frameString.length()));
                if (f < 3)
                    frame = f + 1;  // 0,1,2->1,2,3
                else
                    frame = f - 6;      // 3,4,5-> -3, -2, -1
            } else
                throw new IOException("Failed to parse '" + frameString + "' in: " + aLine);
            return true;
        }

        /**
         * parse the lines containing the match
         *
         * @param queryLine
         * @param midLine
         * @param subjectLine
         * @throws IOException
         */
        void parseLines(String queryLine, String midLine, String subjectLine) throws IOException {
            if (!queryLine.startsWith(Query))
                throw new IOException("Token '" + Query + "' not found in line: " + queryLine);
            String[] queryTokens = queryLine.split("\\s+");
            if (queryTokens.length != 4)
                throw new IOException("Wrong number of tokens: " + queryTokens.length + " in query line: " + queryLine);
            queryStart = Basic.parseInt(queryTokens[1]);
            querySequence = queryTokens[2];
            queryEnd = Basic.parseInt(queryTokens[3]);
            if (!subjectLine.startsWith(Subject))
                throw new IOException("Token '" + Subject + "' not found in line: " + midLine);
            String[] subjTokens = subjectLine.split("\\s+");
            if (subjTokens.length != 4)
                throw new IOException("Wrong number of tokens: " + subjTokens.length + " in subject line: " + subjectLine);
            refStart = Basic.parseInt(subjTokens[1]);
            refSequence = subjTokens[2];
            refEnd = Basic.parseInt(subjTokens[3]);
        }
    }
}
