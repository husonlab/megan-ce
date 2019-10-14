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
package megan.parsers.sam;

import jloda.util.Basic;
import jloda.util.BlastMode;
import jloda.util.SequenceUtils;
import jloda.util.Single;
import megan.util.BlosumMatrix;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * a match in SAM format
 * Daniel Huson, 3.2011
 */
public class SAMMatch implements megan.rma3.IMatch {
    private static final int ALIGNMENT_FOLD = 120;

    private final String pairedReadSuffix1;
    private final String pairedReadSuffix2;
    private final BlastMode mode;

    public static final boolean warnAboutProblems = true;

    private String[] tokens = new String[12];

    /*
    0	QNAME	String
    1	FLAG	Int
    2	RNAME	String 
    3	POS	Int
    4	MAPQ	Int
    5	CIGAR	String
    6	RNEXT	String
    7	PNEXT	Int
    8	TLEN	Int
    9	SEQ	String
    10	QUAL	String Regexp/Range [!-?A-~]{1,255} [0,216 -1] \*|[!-()+-<>-~][!-~]* [0,229 -1][0,28 -1] \*|([0-9]+[MIDNSHPX=])+ \*|=|[!-()+-<>-~][!-~]* [0,229 -1] [-229 +1,229 -1] \*|[A-Za-z=.]+ [!-~]+
    Brief description
    Query template NAME bitwise FLAG Reference sequence NAME 1-based leftmost mapping POSition MAPping Quality
    CIGAR string Ref. name of the mate/next fragment Position of the mate/next fragment observed Template LENgth
    fragment SEQuence ASCII of Phred-scaled base QUALity+33
     */

    private String queryName;
    private int alignedQueryStart;
    private int alignedQueryEnd;

    private int flag;
    private String refName;
    private int pos;
    private int mapQuality;
    private String cigarString;
    private String RNext;
    private int PNext;
    private int TLength;
    private String sequence;
    private String quality;
    private final Map<String, Object> optionalFields = new HashMap<>();

    private Cigar cigar;

    /**
     * constructor
     */
    public SAMMatch(BlastMode mode) {
        this(mode, null, null);
    }

    /**
     * constructor
     *
     * @param pairedReadSuffix1
     * @param pairedReadSuffix2
     */
    public SAMMatch(BlastMode mode, String pairedReadSuffix1, String pairedReadSuffix2) {
        this.mode = mode;
        this.pairedReadSuffix1 = pairedReadSuffix1;
        this.pairedReadSuffix2 = pairedReadSuffix2;
    }

    /**
     * erase the match
     */
    @Override
    public void clear() {
        queryName = null;
        alignedQueryStart = 0;
        alignedQueryEnd = 0;
        flag = 0;
        refName = null;
        pos = 0;
        mapQuality = 0;
        cigarString = null;
        RNext = null;
        PNext = 0;
        TLength = 0;
        sequence = null;
        quality = null;
        optionalFields.clear();
        cigar = null;
    }

    /**
     * parse a line of SAM format
     *
     * @param aLine
     * @return matchBlock
     */
    public void parse(byte[] aLine, int length) throws IOException {
        int numberOfTokens = 0;
        int start = 0;
        while (start < length) {
            int end = start;
            while (aLine[end] != '\t' && end < length)
                end++;
            if (numberOfTokens == tokens.length) {
                String[] tmp = new String[2 * tokens.length];
                System.arraycopy(tokens, 0, tmp, 0, tokens.length);
                tokens = tmp;
            }
            tokens[numberOfTokens++] = Basic.toString(aLine, start, end - start);
            start = end + 1;
        }
        parse(tokens, numberOfTokens);
    }

    /**
     * parse a line of SAM format
     *
     * @param aLine
     * @return matchBlock
     */
    @Override
    public void parse(String aLine) throws IOException {
        String[] tokens = aLine.trim().split("\t");
        parse(tokens, tokens.length);
        /*
        int numberOfTokens = 0;
        int start = 0;
        while (start < aLine.length()) {
            int end = aLine.indexOf('\t', start);
            if (end == -1)
                end = aLine.length();
            if (numberOfTokens == tokens.length) {
                String[] tmp = new String[2 * tokens.length];
                System.arraycopy(tokens, 0, tmp, 0, tokens.length);
                tokens = tmp;
            }
            tokens[numberOfTokens++] = aLine.substring(start, end);
            start = end + 1;
        }
        parse(tokens, numberOfTokens);
        */
    }

    /**
     * parse a line of SAM format
     *
     * @param tokens
     * @return matchBlock
     */
    public void parse(String[] tokens, int numberOfTokens) throws IOException {
        if (numberOfTokens < 11) {
            throw new IOException("Too few tokens in line: " + numberOfTokens);
        }
        setQueryName(tokens[0]);
        setFlag(Basic.parseInt(tokens[1]));
        setRefName(tokens[2]);
        setPos(Basic.parseInt(tokens[3]));
        setMapQuality(Basic.parseInt(tokens[4]));
        setCigarString(tokens[5]);
        setRNext(tokens[6]);
        setPNext(Basic.parseInt(tokens[7]));
        setTLength(Math.abs(Basic.parseInt(tokens[8])));
        //setSequence(isReverseComplemented() ? SequenceUtils.getReverseComplement(tokens[9]) : tokens[9]);
        setSequence(tokens[9].toUpperCase());

        setQuality(tokens[10]);

        for (int i = 11; i < numberOfTokens; i++) {
            final String word = tokens[i];
            int pos1 = word.indexOf(':');
            int pos2 = word.indexOf(':', pos1 + 1);
            if (pos2 == -1)
                throw new IOException("Failed to parse: " + word);
            final String[] three = new String[]{word.substring(0, pos1), word.substring(pos1 + 1, pos2), word.substring(pos2 + 1)};

            final Object object;
            switch (three[1].charAt(0)) {
                case 'A': //character
                    object = three[2].charAt(0);
                    break;
                case 'i': // integer
                    object = Integer.parseInt(three[2]);
                    break;
                case 'f': // float
                    object = Float.parseFloat(three[2]);
                    break;
                case 'Z': //string
                    object = three[2];
                    break;
                case 'H': // hex string
                    object = Integer.valueOf(three[2]);
                    break;
                default:
                    throw new IOException("Failed to parse: " + word);
            }
            optionalFields.put(three[0], object);
        }

        Flag theFlag = new Flag(flag);
        if (pairedReadSuffix1 != null && !theFlag.isFirstFragment() && !getQueryName().endsWith(pairedReadSuffix1))
            setQueryName(getQueryName() + pairedReadSuffix1);
        if (pairedReadSuffix2 != null && !theFlag.isLastFragment() && !getQueryName().endsWith(pairedReadSuffix2))
            setQueryName(getQueryName() + pairedReadSuffix2);

        alignedQueryStart = determineQueryStart();

        alignedQueryEnd = determineQueryEnd(alignedQueryStart);
    }

    /**
     * determine the query start position
     *
     * @return query start
     */
    private int determineQueryStart() {
        int queryStart = 1;
        Object obj = optionalFields.get("ZS");
        if (obj instanceof Integer) {
            queryStart = (Integer) obj;
        } else {
            // first need to trim:
            if (mode != BlastMode.BlastX) {
                if (getCigar().getCigarElements().size() > 0) {
                    CigarElement element = getCigar().getCigarElement(0);
                    if (element.getOperator() == CigarOperator.S || element.getOperator() == CigarOperator.H) {
                        queryStart = element.getLength() + 1;
                    }
                }
            } else {
                if (getCigar().getCigarElements().size() > 0) {
                    CigarElement element = getCigar().getCigarElement(0);
                    if (element.getOperator() == CigarOperator.S || element.getOperator() == CigarOperator.H) {
                        queryStart = 3 * element.getLength() + 1;
                    }
                }
            }
        }
        return queryStart;
    }

    /**
     * determine the query end position
     *
     * @return query end
     */
    private int determineQueryEnd(int alignedQueryStart) {
        final Object zq = optionalFields.get("ZQ");
        if (zq instanceof Integer) {
            return (Integer) zq;
        }

        int alignedQueryLength = computeAlignedQuerySegmentLength(getSequence());
        if (mode == BlastMode.BlastX) {
            final Object df = optionalFields.get("ZF");
            final boolean reverse = (df instanceof Integer && ((Integer) df) < 0);
            if (reverse)
                return alignedQueryStart - alignedQueryLength + 1;
        }
        return alignedQueryStart + alignedQueryLength - 1;
    }

    /**
     * return string representation
     *
     * @return as string
     */
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getQueryName()).append("\t");
        buffer.append(getFlag()).append("\t");
        buffer.append(getRefName()).append("\t");
        buffer.append(getPos()).append("\t");
        buffer.append(getMapQuality()).append("\t");
        buffer.append(getCigarString()).append("\t");
        buffer.append(getRNext()).append("\t");
        buffer.append(getPNext()).append("\t");
        buffer.append(getTLength()).append("\t");
        buffer.append(getSequence()).append("\t");
        buffer.append(getQuality());
        for (String a : getOptionalFields().keySet()) {
            Object value = getOptionalFields().get(a);
            buffer.append("\t").append(a).append(":").append(getType(value)).append(":").append(value);
        }
        return buffer.toString();
    }

    /**
     * gets the type code for a value
     *
     * @param value
     * @return type
     */
    private char getType(Object value) {
        if (value instanceof Integer)
            return 'i';
        else if (value instanceof Float)
            return 'f';
        else if (value instanceof Character)
            return 'A';
        else if (value instanceof String)
            return 'Z';
        else
            return '?';
    }

    /**
     * gets match as blast alignment text
     *
     * @return blast alignment text
     */
    public String getBlastAlignmentText() {
        return getBlastAlignmentText(null);
    }

    /**
     * gets match as blast alignment text
     *
     * @param percentIdentity variable to save percent identity to in the case of a blastN match
     * @return blast alignment text
     */
    public String getBlastAlignmentText(final Single<Float> percentIdentity) {
        switch (mode) {
            case BlastX:
                return getBlastXAlignment(percentIdentity);
            case BlastP:
                return getBlastPAlignment(percentIdentity);
            default:
            case BlastN:
                return getBlastNAlignment(percentIdentity);
        }
    }

    /**
     * return a BlastNText alignment
     *
     * @return
     */
    private String getBlastNAlignment(final Single<Float> percentIdentity) {
        final int editDistance = getEditDistance();
        final String query = getSequence();

        final String[] aligned = computeAlignment(query);
        if (aligned[0].equals("No alignment")) {
            return shortDescription();
        }

        if (isReverseComplemented()) {
            aligned[0] = SequenceUtils.getReverseComplement(aligned[0]);
            aligned[1] = SequenceUtils.getReverse(aligned[1]);
            aligned[2] = SequenceUtils.getReverseComplement(aligned[2]);
        }

        int identities = 0;
        for (int i = 0; i < aligned[1].length(); i++) {
            if (aligned[1].charAt(i) == '|')
                identities++;
        }
        int alignmentLength = aligned[1].length();

        String alignedQuery = getUngappedSequence(aligned[0]);
        int queryLength = alignedQuery.length();
        int refLength = getUngappedLength(aligned[2]);

        int gaps = 2 * aligned[0].length() - queryLength - getUngappedLength(aligned[2]);

        StringBuilder buffer = new StringBuilder();
        buffer.append(String.format(">%s\n", Basic.fold(refName, ALIGNMENT_FOLD)));
        {
            final int len = getRefLength();
            if (len >= refLength)
                buffer.append(String.format("\tLength = %d\n\n", len));
            else
                buffer.append(String.format("\tLength >= %d\n\n", (getPos() + refLength - 1)));
        }
        if (optionalFields.get("AS") != null && optionalFields.get("AS") instanceof Integer) {
            if (optionalFields.get("ZR") != null && optionalFields.get("ZR") instanceof Integer && optionalFields.get("ZE") != null && optionalFields.get("ZE") instanceof Float) {
                int bitScore = getBitScore();
                int rawScore = getRawScore();
                float expect = getExpected();
                if (expect == 0)
                    buffer.append(String.format(" Score = %d bits (%d), Expect = 0\n", bitScore, rawScore));
                else
                    buffer.append(String.format(" Score = %d bits (%d), Expect = %.1g\n", bitScore, rawScore, expect));
            } else {
                buffer.append(String.format(" Score = %d\n", optionalFields.get("AS")));
            }
        } else
            buffer.append(String.format("MapQuality = %d  EditDistance=%d\n", getMapQuality(), editDistance));
        float pIdentity = 100f * identities / alignmentLength;
        if (percentIdentity != null)
            percentIdentity.set(pIdentity);
        buffer.append(String.format(" Identities = %d/%d (%d%%), Gaps = %d/%d (%d%%)\n", identities, alignmentLength, Math.round(pIdentity), gaps, alignmentLength, Math.round(100.0 * gaps / queryLength)));

        buffer.append(" Strand = Plus / ").append(isReverseComplemented() ? "Minus" : "Plus").append("\n");
        int qStart = getAlignedQueryStart();
        int qEnd = 0;
        int sStart = !isReverseComplemented() ? getPos() : (getPos() + refLength - 1);
        int sEnd = 0;
        for (int pos = 0; pos < Objects.requireNonNull(aligned[0]).length(); pos += ALIGNMENT_FOLD) {
            if (getSequence() != null && aligned[0] != null) {
                int qAdd = Math.min(ALIGNMENT_FOLD, aligned[0].length() - pos);
                String qPart = aligned[0].substring(pos, pos + qAdd);
                int qGaps = countGapsDashDotStar(qPart);
                qEnd = qStart + (qAdd - qGaps) - 1;
                buffer.append(String.format("\nQuery:%9d  %s  %d\n", qStart, qPart, qEnd));
                qStart = qEnd + 1;
            }
            if (aligned[1] != null) {
                int mAdd = Math.min(ALIGNMENT_FOLD, aligned[1].length() - pos);
                String mPart = aligned[1].substring(pos, pos + mAdd);
                buffer.append(String.format("                 %s\n", mPart));
            }
            if (aligned[2] != null) {
                int sAdd = Math.min(ALIGNMENT_FOLD, aligned[2].length() - pos);
                String sPart = aligned[2].substring(pos, pos + sAdd).toUpperCase();
                int sGaps = countGapsDashDotStar(sPart);
                if (!isReverseComplemented())
                    sEnd = sStart + (sAdd - sGaps) - 1;
                else
                    sEnd = sStart - (sAdd - sGaps) + 1;
                buffer.append(String.format("Sbjct:%9d  %s  %d\n", sStart, sPart, sEnd));
                if (!isReverseComplemented())
                    sStart = sEnd + 1;
                else
                    sStart = sEnd - 1;
            }
        }
        if (qEnd > 0 && qEnd != getAlignedQueryStart() + queryLength - 1 && warnAboutProblems)
            System.err.println("Internal error writing BLAST format: qEnd=" + qEnd + ", should be: " + (getAlignedQueryStart() + queryLength - 1));
        if (aligned[2] != null) {
            int sEndShouldBe = (!isReverseComplemented() ? (getPos() + refLength - 1) : getPos());
            if (sEnd > 0 && sEnd != sEndShouldBe && warnAboutProblems)
                System.err.println("Internal error writing BLAST format: sEnd=" + sEnd + ", should be: " + sEndShouldBe);
        }
        return buffer.toString();
    }

    /**
     * return a BlastPText alignment
     *
     * @return
     */
    private String getBlastPAlignment(final Single<Float> percentIdentity) {
        final int editDistance = getEditDistance();
        final String query = getSequence();

        final String[] aligned = computeAlignment(query);
        if (aligned[0].equals("No alignment")) {
            return shortDescription();
        }

        int identities = 0;
        for (int i = 0; i < aligned[1].length(); i++) {
            if (aligned[1].charAt(i) != '+' && aligned[1].charAt(i) != ' ')
                identities++;
        }
        int alignmentLength = aligned[1].length();

        String alignedQuery = getUngappedSequence(aligned[0]);
        int queryLength = alignedQuery.length();
        int refLength = getUngappedLength(aligned[2]);

        int gaps = 2 * aligned[0].length() - queryLength - getUngappedLength(aligned[2]);

        StringBuilder buffer = new StringBuilder();
        buffer.append(String.format(">%s\n", Basic.fold(refName, ALIGNMENT_FOLD)));
        {
            final int len = getRefLength();
            if (len >= refLength)
                buffer.append(String.format("\tLength = %d\n\n", len));
            else
                buffer.append(String.format("\tLength >= %d\n\n", (getPos() + refLength - 1)));
        }
        if (optionalFields.get("AS") != null && optionalFields.get("AS") instanceof Integer) {
            if (optionalFields.get("ZR") != null && optionalFields.get("ZR") instanceof Integer && optionalFields.get("ZE") != null && optionalFields.get("ZE") instanceof Float) {
                int bitScore = getBitScore();
                int rawScore = getRawScore();
                float expect = getExpected();
                if (expect == 0)
                    buffer.append(String.format(" Score = %d bits (%d), Expect = 0\n", bitScore, rawScore));
                else
                    buffer.append(String.format(" Score = %d bits (%d), Expect = %.1g\n", bitScore, rawScore, expect));
            } else {
                buffer.append(String.format(" Score = %d\n", optionalFields.get("AS")));
            }
        } else
            buffer.append(String.format("MapQuality = %d  EditDistance=%d\n", getMapQuality(), editDistance));
        int numberOfPositives = alignmentLength - Basic.countOccurrences(aligned[1], ' ');
        float pIdentity = 100f * identities / alignmentLength;
        if (percentIdentity != null)
            percentIdentity.set(pIdentity);

        buffer.append(String.format(" Identities = %d/%d (%d%%), Positives = %d/%d (%.0f%%), Gaps = %d/%d (%d%%)\n",
                identities, alignmentLength, Math.round(pIdentity),
                numberOfPositives, alignmentLength, (100.0 * (numberOfPositives) / alignmentLength),
                gaps, alignmentLength, Math.round(100.0 * gaps / queryLength)));

        int qStart = getAlignedQueryStart();
        int qEnd = 0;
        int sStart = getPos();
        int sEnd = 0;
        for (int pos = 0; pos < Objects.requireNonNull(aligned[0]).length(); pos += ALIGNMENT_FOLD) {
            if (getSequence() != null && aligned[0] != null) {
                int qAdd = Math.min(ALIGNMENT_FOLD, aligned[0].length() - pos);
                String qPart = aligned[0].substring(pos, pos + qAdd);
                int qGaps = countGapsDashDot(qPart);
                qEnd = qStart + (qAdd - qGaps) - 1;
                buffer.append(String.format("\nQuery:%9d  %s  %d\n", qStart, qPart, qEnd));
                qStart = qEnd + 1;
            }
            if (aligned[1] != null) {
                int mAdd = Math.min(ALIGNMENT_FOLD, aligned[1].length() - pos);
                String mPart = aligned[1].substring(pos, pos + mAdd);
                buffer.append(String.format("                 %s\n", mPart));
            }
            if (aligned[2] != null) {
                int sAdd = Math.min(ALIGNMENT_FOLD, aligned[2].length() - pos);
                String sPart = aligned[2].substring(pos, pos + sAdd).toUpperCase();
                int sGaps = countGapsDashDot(sPart);
                sEnd = sStart + (sAdd - sGaps) - 1;
                buffer.append(String.format("Sbjct:%9d  %s  %d\n", sStart, sPart, sEnd));
                sStart = sEnd + 1;
            }
        }
        if (qEnd > 0 && qEnd != getAlignedQueryStart() + queryLength - 1 && warnAboutProblems)
            System.err.println("Internal error writing BLAST format: qEnd=" + qEnd + ", should be: " + (getAlignedQueryStart() + queryLength - 1));
        if (aligned[2] != null) {
            int sEndShouldBe = (getPos() + refLength - 1);
            if (sEnd > 0 && sEnd != sEndShouldBe && warnAboutProblems)
                System.err.println("Internal error writing BLAST format: sEnd=" + sEnd + ", should be: " + sEndShouldBe);
        }
        return buffer.toString();
    }

    /**
     * return a BlastText alignment
     *
     * @return
     */
    private String getBlastXAlignment(final Single<Float> percentIdentity) {
        final int editDistance = getEditDistance();
        final String query = getSequence();

        final String[] aligned = computeAlignment(query);
        if (aligned[0].equals("No alignment")) {
            return shortDescription();
        }

        int identities = 0;
        for (int i = 0; i < aligned[1].length(); i++) {
            if (aligned[1].charAt(i) != '+' && aligned[1].charAt(i) != ' ')
                identities++;
        }
        final int alignmentLength = aligned[1].length();

        final int queryLengthForGapCalculation = getUngappedSequence(aligned[0]).length(); // query length as required for gaps calculation, this differs from actual length that must take frame shifts into account
        final int refLength = getUngappedLength(aligned[2]);

        final int gaps = 2 * aligned[0].length() - queryLengthForGapCalculation - getUngappedLength(aligned[2]);

        final StringBuilder buffer = new StringBuilder();
        buffer.append(String.format(">%s\n", Basic.fold(refName, ALIGNMENT_FOLD)));

        {
            final int len = getRefLength();
            if (len >= refLength)
                buffer.append(String.format("\tLength = %d\n\n", len));
            else
                buffer.append(String.format("\tLength >= %d\n\n", (getPos() + refLength - 1)));
        }

        // get query frame:
        final int qFrame;
        {
            final Object obj = optionalFields.get("ZF");
            if (obj instanceof Integer) {
                qFrame = (Integer) obj;
            } else
                qFrame = 0;
        }
        final int qJump = (qFrame >= 0 ? 3 : -3);

        if (optionalFields.get("AS") != null && optionalFields.get("AS") instanceof Integer) {
            if (optionalFields.get("ZR") != null && optionalFields.get("ZR") instanceof Integer && optionalFields.get("ZE") != null && optionalFields.get("ZE") instanceof Float) {
                int bitScore = getBitScore();
                int rawScore = getRawScore();
                float expect = getExpected();
                if (expect == 0)
                    buffer.append(String.format(" Score = %d bits (%d), Expect = 0\n", bitScore, rawScore));
                else
                    buffer.append(String.format(" Score = %d bits (%d), Expect = %.1g\n", bitScore, rawScore, expect));
            } else {
                buffer.append(String.format(" Score = %d\n", optionalFields.get("AS")));
            }
        } else
            buffer.append(String.format("MapQuality = %d  EditDistance=%d\n", getMapQuality(), editDistance));
        int numberOfPositives = alignmentLength - Basic.countOccurrences(aligned[1], ' ');
        float pIdentity = 100f * identities / alignmentLength;
        if (percentIdentity != null)
            percentIdentity.set(pIdentity);

        buffer.append(String.format(" Identities = %d/%d (%d%%), Positives = %d/%d (%.0f%%), Gaps = %d/%d (%d%%)\n",
                identities, alignmentLength, Math.round(pIdentity),
                numberOfPositives, alignmentLength, (100.0 * (numberOfPositives) / alignmentLength),
                gaps, alignmentLength, Math.round((100.0 * gaps / queryLengthForGapCalculation))));
        if (qFrame != 0)
            buffer.append(String.format(" Frame = %+d\n", qFrame));

        int qEnd = 0;
        final int sStart = !isReverseComplemented() ? getPos() : (getPos() + refLength - 1);
        int sStartPart = sStart;
        int sEnd = 0;
        int qStartPart = determineQueryStart();
        for (int pos = 0; pos < Objects.requireNonNull(aligned[0]).length(); pos += ALIGNMENT_FOLD) {
            if (getSequence() != null && aligned[0] != null) {
                int qAdd = Math.min(ALIGNMENT_FOLD, aligned[0].length() - pos);
                String qPart = aligned[0].substring(pos, pos + qAdd);
                int qGaps = countGapsDashDot(qPart);
                int qFrameShiftChange = countFrameShiftChange(qPart);

                qEnd = qStartPart + qJump * (qAdd - qGaps) + (qFrame > 0 ? qFrameShiftChange : -qFrameShiftChange) + (qFrame > 0 ? -1 : 1);
                buffer.append(String.format("\nQuery:%9d  %s  %d\n", qStartPart, qPart, qEnd));
                qStartPart = qEnd + (qFrame < 0 ? -1 : 1);
            }
            if (aligned[1] != null) {
                int mAdd = Math.min(ALIGNMENT_FOLD, aligned[1].length() - pos);
                String mPart = aligned[1].substring(pos, pos + mAdd);
                buffer.append(String.format("                 %s\n", mPart));
            }
            if (aligned[2] != null) {
                int sAdd = Math.min(ALIGNMENT_FOLD, aligned[2].length() - pos);
                String sPart = aligned[2].substring(pos, pos + sAdd).toUpperCase();
                int sGaps = countGapsDashDot(sPart);
                if (!isReverseComplemented())
                    sEnd = sStartPart + (sAdd - sGaps) - 1;
                else
                    sEnd = sStartPart - (sAdd - sGaps) + 1;
                buffer.append(String.format("Sbjct:%9d  %s  %d\n", sStartPart, sPart, sEnd));
                if (!isReverseComplemented())
                    sStartPart = sEnd + 1;
                else
                    sStartPart = sEnd - 1;
            }
        }

        if (qEnd > 0 && qEnd != alignedQueryEnd && warnAboutProblems) {
            // System.err.println(buffer.toString());
            System.err.println("Internal error writing BLAST format: query length is incorrect");
        }
        if (aligned[2] != null) {
            int sEndShouldBe = (!isReverseComplemented() ? (getPos() + refLength - 1) : getPos());
            if (sEnd > 0 && sEnd != sEndShouldBe && warnAboutProblems)
                System.err.println("Internal error writing BLAST format: sEnd=" + sEnd + ", should be: " + sEndShouldBe);
        }
        return buffer.toString();
    }

    private int countFrameShiftChange(String qPart) {
        int count = 0;
        for (int i = 0; i < qPart.length(); i++) {
            if (qPart.charAt(i) == '\\') // forward shift
                count -= 2;
            else if (qPart.charAt(i) == '/') // reverse shift
                count -= 4;
        }
        return count;
    }

    /**
     * gets a short description of a match
     * This is used for BlastTab and similar incomplete formats
     *
     * @return short description
     */
    private String shortDescription() {
        StringBuilder buffer = new StringBuilder();
        if (refName.length() > 0)
            buffer.append(String.format(">%s\n", Basic.fold(refName, ALIGNMENT_FOLD)));
        {
            if (getRefLength() > 9)
                buffer.append(String.format("\tLength = %d\n\n", getRefLength()));
            else
                buffer.append("\n");
        }
        {
            boolean hasFirst = false;
            boolean hasSecond = false;
            if (optionalFields.get("AS") != null && optionalFields.get("AS") instanceof Integer) {
                buffer.append(String.format(" Score = %d", getBitScore()));
                if (optionalFields.get("ZR") != null && optionalFields.get("ZR") instanceof Integer) {
                    buffer.append(String.format(" bits (%d)", getRawScore()));
                }
                hasFirst = true;
            }

            if (optionalFields.get("ZE") != null && optionalFields.get("ZE") instanceof Float) {
                if (hasFirst)
                    buffer.append(",");
                if (getExpected() == 0)
                    buffer.append(" Expect = 0");
                else
                    buffer.append(String.format(" Expect = %.1g", getExpected()));
                hasSecond = true;
            }
            if (hasFirst || hasSecond)
                buffer.append("\n");
        }
        {
            if (optionalFields.get("AL") != null)
                buffer.append(optionalFields.get("AL").toString()).append("\n");
        }
        return buffer.toString();
    }


    private String getUngappedSequence(String s) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < s.length(); i++)
            if (!isGap(s.charAt(i)))
                buffer.append(s.charAt(i));
        return buffer.toString();
    }

    /**
     * get the ungapped length of a gapped string
     *
     * @param s
     * @return ungapped length
     */
    private int getUngappedLength(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++)
            if (!isGap(s.charAt(i)))
                count++;
        return count;
    }

    /**
     * get the edit distance
     *
     * @return edit distance
     */
    private int getEditDistance() {
        Integer value = (Integer) getOptionalFields().get("NM");
        return value != null ? value : 0;
    }

    @Override
    public int getBitScore() {
        try {
            return (Integer) optionalFields.get("AS");
        } catch (Exception ex) {
            return 0;
        }
    }

    private int getRawScore() {
        try {
            return (Integer) optionalFields.get("ZR");
        } catch (Exception ex) {
            return 0;
        }
    }


    @Override
    public float getExpected() {
        try {
            return (Float) optionalFields.get("ZE");
        } catch (Exception ex) {
            return 0;
        }
    }

    @Override
    public int getPercentIdentity() {
        try {
            return (Integer) optionalFields.get("ZI");
        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * count the number of gaps ('-', '.', '*') in a sequence
     *
     * @param sequence
     * @return number of gaps
     */
    private static int countGapsDashDotStar(String sequence) {
        int count = 0;
        for (int i = 0; i < sequence.length(); i++) {
            int a = sequence.charAt(i);
            if (a == '-' || a == '.' || a == '*')
                count++;
        }
        return count;
    }

    /**
     * count the number of gaps ('-', '.') in a sequence
     *
     * @param sequence
     * @return number of gaps
     */
    private static int countGapsDashDot(String sequence) {
        int count = 0;
        for (int i = 0; i < sequence.length(); i++) {
            int a = sequence.charAt(i);
            if (a == '-' || a == '.')
                count++;
        }
        return count;
    }

    /**
     * compute three line description of alignment (query, midline, subject)
     *
     * @param query trimmed query string
     * @return alignment
     */
    private String[] computeAlignment(String query) {
        if (getCigar().getCigarElements().size() == 0) // not available
        {
            return new String[]{"No alignment", "mapQ=0 (not uniquely mapped)", ""};
        }

        final String[] pair = computeAlignmentPair(query);
        if (pair[0].equals("No alignment"))
            return pair;

        final String gappedQuerySequence = pair[0];
        final String gappedReferenceSequence = pair[1];

        final StringBuilder midBuffer = new StringBuilder();
        int top = Math.min(gappedQuerySequence.length(), gappedReferenceSequence.length());
        switch (mode) {
            case BlastX:
            case BlastP:
                for (int i = 0; i < top; i++) {
                    byte a = (byte) Character.toUpperCase(gappedQuerySequence.charAt(i));
                    byte b = (byte) Character.toUpperCase(gappedReferenceSequence.charAt(i));
                    if (Character.isLetter(a) && Character.isLetter(b)) {
                        if (a == b)
                            midBuffer.append((char) a);
                        else if (BlosumMatrix.getBlosum62().getScore(a, b) > 0)
                            midBuffer.append('+');
                        else
                            midBuffer.append(' ');
                    } else
                        midBuffer.append(" ");
                }
                break;
            default:
            case BlastN:
                for (int i = 0; i < top; i++) {
                    if (Character.isLetter(gappedQuerySequence.charAt(i)) && gappedQuerySequence.charAt(i) == gappedReferenceSequence.charAt(i))
                        midBuffer.append("|");
                    else if (isGap(gappedQuerySequence.charAt(i)) || isGap(gappedReferenceSequence.charAt(i)))
                        midBuffer.append(" ");
                    else
                        midBuffer.append(" ");
                }
        }

        return new String[]{gappedQuerySequence, midBuffer.toString(), gappedReferenceSequence};
    }

    /**
     * compute two line description of alignment  (query, subject)
     *
     * @param query trimmed query string
     * @return alignment
     */
    private String[] computeAlignmentPair(String query) {
        if (getCigar().getCigarElements().size() == 0) // not available
        {
            return new String[]{"No alignment", "mapQ=0 (not uniquely mapped)", ""};
        }
        if (query.equals("*") || query.length() == 0)
            return new String[]{"No alignment", "no string stored"};

        boolean hardClippedPositionsHaveBeenInserted = (query.charAt(0) == 0);
        // hard clipped positions have been inserted into query string as 0's, must advance position when parsing leading hard clip

        final StringBuilder gappedQueryBuffer = new StringBuilder();
        final StringBuilder gappedReferenceBuffer = new StringBuilder();

        int posQuery = 0;


        for (CigarElement element : getCigar().getCigarElements()) {
            for (int i = 0; i < element.getLength(); i++) {
                final char queryChar = (posQuery < query.length() ? query.charAt(posQuery) : 0);
                // todo: should not need to check whether posQuery is in range, but minimap produces files that cause this problem


                switch (element.getOperator()) {
                    case D:
                        gappedQueryBuffer.append("-");
                        gappedReferenceBuffer.append("?");
                        break;
                    case M:
                        gappedQueryBuffer.append(queryChar);
                        gappedReferenceBuffer.append("?");
                        posQuery++;
                        break;
                    case I:
                        gappedQueryBuffer.append(queryChar);
                        gappedReferenceBuffer.append("-");
                        posQuery++;
                        break;
                    case N:
                        gappedQueryBuffer.append(".");
                        gappedReferenceBuffer.append("?");
                        break;
                    case S:
                        if (!hardClippedPositionsHaveBeenInserted)
                            posQuery++;
                        break;
                    case H:
                        if (hardClippedPositionsHaveBeenInserted)
                            posQuery++; //hard clipped positions have been inserted into query string (as 0's), must advance position
                        break;
                    case P:
                        gappedQueryBuffer.append("*");
                        gappedReferenceBuffer.append("*");
                        break;
                    case EQ:
                        gappedQueryBuffer.append(queryChar);
                        gappedReferenceBuffer.append(queryChar);
                        posQuery++;
                        break;
                    case X:
                        gappedQueryBuffer.append(queryChar);
                        gappedReferenceBuffer.append("?");
                        posQuery++;
                        break;
                }
            }
        }

        final String gappedQuerySequence = gappedQueryBuffer.toString();
        final String gappedReferenceSequence;

        String mdString = (String) getOptionalFields().get("MD");
        if (mdString == null)
            mdString = (String) getOptionalFields().get("md");

        if (mdString != null) {
            gappedReferenceSequence = Diff.getReference(mdString, gappedQuerySequence, gappedReferenceBuffer.toString());
        } else
            gappedReferenceSequence = gappedReferenceBuffer.toString();

        return new String[]{gappedQuerySequence, gappedReferenceSequence};
    }

    /**
     * compute the aligned query segment length
     *
     * @param query query sequence
     * @return aligned query length
     */
    private int computeAlignedQuerySegmentLength(String query) {
        if (query.equals("*") || query.length() == 0)
            return 0;

        int length = 0;
        for (CigarElement element : getCigar().getCigarElements()) {
            for (int i = 0; i < element.getLength(); i++) {
                switch (element.getOperator()) {
                    case D:
                        break;
                    case M:
                        length++;
                        break;
                    case I:
                        length++;
                        break;
                    case N:
                        break;
                    case S:
                        break;
                    case H:
                        break;
                    case P:
                        break;
                    case EQ:
                        length++;
                        break;
                    case X:
                        length++;
                        break;
                }
            }
        }
        if (mode == BlastMode.BlastX) {
            length *= 3;
            for (int i = 0; i < query.length(); i++) {
                char ch = query.charAt(i);
                if (ch == '/') // reverse shift by 1
                    length -= 4; // single letter is counted above as 3 nucleotides, but this is  a reverse shift by 1, so above we overcounted by 4
                else if (ch == '\\') // forward shift by 1
                    length -= 2; // a single letter is counted above as 3 nucleotides, but this is only a forward shift by 1, so above we overcounted by 2
            }
        }

        return length;
    }

    private boolean isGap(char c) {
        return c == '.' || c == '-';
    }

    @Override
    public String getQueryName() {
        return queryName;
    }

    private void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    private int getFlag() {
        return flag;
    }

    private void setFlag(int flag) {
        this.flag = flag;
    }

    @Override
    public String getRefName() {
        return refName;
    }

    private void setRefName(String refName) {
        this.refName = refName;
    }

    private int getPos() {
        return pos;
    }

    private void setPos(int pos) {
        this.pos = pos;
    }

    private int getMapQuality() {
        return mapQuality;
    }

    private void setMapQuality(int mapQuality) {
        this.mapQuality = mapQuality;
    }

    private String getCigarString() {
        return cigarString;
    }

    private void setCigarString(String cigarString) {
        this.cigarString = cigarString;
        setCigar(TextCigarCodec.getSingleton().decode(cigarString));
    }

    private String getRNext() {
        return RNext;
    }

    private void setRNext(String RNext) {
        this.RNext = RNext;
    }

    private int getPNext() {
        return PNext;
    }

    private void setPNext(int PNext) {
        this.PNext = PNext;
    }

    public int getTLength() {
        return TLength;
    }

    private void setTLength(int TLength) {
        this.TLength = TLength;
    }

    private String getSequence() {
        return sequence;
    }

    private void setSequence(String sequence) {
        this.sequence = sequence;
    }

    private String getQuality() {
        return quality;
    }

    private void setQuality(String quality) {
        this.quality = quality;
    }

    private Map<String, Object> getOptionalFields() {
        return optionalFields;
    }

    private Cigar getCigar() {
        return cigar;
    }

    private void setCigar(Cigar cigar) {
        this.cigar = cigar;
    }

    private boolean isReverseComplemented() {
        return (getFlag() & 0x10) != 0;
    }

    /**
     * returns true, if is match
     *
     * @return true if match
     */
    public boolean isMatch() {
        return !(refName == null || refName.equals("*"));
    }

    public int getAlignedQueryStart() {
        return alignedQueryStart;
    }

    public int getAlignedQueryEnd() {
        return alignedQueryEnd;
    }

    public int getRefLength() {
        Object obj = optionalFields.get("ZL");
        if (obj instanceof Integer)
            return (Integer) optionalFields.get("ZL");
        else
            return 0;
    }
}
