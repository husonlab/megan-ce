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

import java.util.LinkedList;
import java.util.List;

/**
 * use this to reconstruct the ref sequence from the query sequence
 * Daniel Huson, 3.2011
 */
public class Diff {
    enum Type {
        MATCH, REPLACE, INSERT
    }

    /**
     * reconstruct the reference string from the query, a template of the ref and the diff
     *
     * @param diff
     * @param gappedQuerySequence
     * @param gappedReferenceSequenceTemplate aligned reference sequence with ?'s for unknown bases
     * @return reference
     */
    public static String getReference(String diff, String gappedQuerySequence, String gappedReferenceSequenceTemplate) {
        if (diff == null)
            return gappedQuerySequence;
        final List<DiffElement> differences = decode(diff);

        StringBuilder buffer = new StringBuilder();
        int posQuery = 0;
        int posRef = 0;
        for (DiffElement element : differences) {
            // skip gaps in reference sequence:
            while (isGap(gappedReferenceSequenceTemplate.charAt(posRef))) {
                buffer.append(gappedReferenceSequenceTemplate.charAt(posRef));
                posQuery++;
                posRef++;
            }
            switch (element.type) {
                case MATCH: {
                    for (int i = 0; i < element.count; i++) {
                        while (isGap(gappedReferenceSequenceTemplate.charAt(posRef))) {
                            buffer.append(gappedReferenceSequenceTemplate.charAt(posRef));
                            posQuery++;
                            posRef++;
                        }
                        buffer.append(gappedQuerySequence.charAt(posQuery));
                        posQuery++;
                        posRef++;
                    }
                    break;
                }
                case REPLACE: {
                    final String what = element.what.toLowerCase();
                    buffer.append(what);
                    posQuery++;
                    posRef++;
                    break;
                }
                case INSERT: {
                    final String what = element.what.toLowerCase();
                    buffer.append(what);
                    posQuery += what.length();
                    posRef += what.length();
                    break;
                }
                default:
                    System.err.println("Unknown Diff element: " + element);
            }
        }
        if (differences.size() > 0 && buffer.length() < gappedQuerySequence.length()) { // todo: added this in an attempt to deal with the fact that the cigar and MD fields may not match in length
            if (SAMMatch.warnAboutProblems) {
                System.err.println("\n(Problem parsing SAM format, alignment may be incorrect: buffer.length=" + buffer.length() + ", gappedQuery.length=" + gappedQuerySequence.length() + ")");
            }
            while (buffer.length() < gappedQuerySequence.length()) {
                buffer.append(gappedQuerySequence.charAt(posQuery++));
            }
        }

        return buffer.toString();
    }

    /**
     * decode a string of differences
     *
     * @param diff
     * @return list of difference elements
     */
    private static List<DiffElement> decode(final String diff) {
        /*
        The MD field aims to achieve SNP/indel calling without looking at the reference. For example, a string �10A5^AC6� means
        from the leftmost reference base in the alignment, there are 10 matches followed by an A on the reference which is different
        from the aligned read base; the next 5 reference bases are matches followed by a 2bp deletion from the reference;
        the deleted sequence is AC; the last 6 bases are matches. The MD field ought to match the CIGAR string.
    */

        final List<DiffElement> differences = new LinkedList<>();

        int pos = 0;
        while (pos < diff.length()) {
            int ch = diff.charAt(pos);
            if (Character.isDigit(ch)) {
                int a = pos;
                while (pos < diff.length() && Character.isDigit(diff.charAt(pos)))
                    pos++;
                Integer i = Basic.parseInt(diff.substring(a, pos));
                DiffElement element = new DiffElement();
                element.type = Type.MATCH;
                element.count = i;
                differences.add(element);
            } else if (isLetterOrStartOrFrameShift(ch) || ch == '.') {
                DiffElement element = new DiffElement();
                element.type = Type.REPLACE;
                element.what = "" + (char) ch;
                differences.add(element);
                pos++;
            } else if (ch == '^') {
                pos++;
                int a = pos;
                while (pos < diff.length() && (isLetterOrStartOrFrameShift(diff.charAt(pos)) || diff.charAt(pos) == '.'))
                    pos++;
                DiffElement element = new DiffElement();
                element.type = Type.INSERT;
                element.what = diff.substring(a, pos);
                differences.add(element);
                if (pos < diff.length() && diff.charAt(pos) == '0')
                    pos++; // consume 0 that was used to indicate end of insert
            } else {
                if (!Character.isWhitespace(diff.charAt(pos)))
                    System.err.println("Warning: illegal char in diff string: '" + diff.charAt(pos) + "' (code: " + (int) diff.charAt(pos) + ")");
                pos++;
            }
        }
        return differences;
    }

    static class DiffElement {
        Type type;
        int count;
        String what;
    }

    private static boolean isLetterOrStartOrFrameShift(int c) {
        return Character.isLetter(c) || c == '*' || c == '\\' || c == '/';
    }

    private static boolean isGap(char c) {
        return c == '*' || c == '.' || c == '-';
    }

    public static String getLongestContainedPrefix(String reconstructedReference, String reference) {
        for (int i = reconstructedReference.length(); i >= 0; i--) {
            int pos = reference.indexOf(reconstructedReference.substring(0, i));
            if (pos >= 0) {
                return "Reconstr.: " + reconstructedReference.substring(0, i) + "|" + reconstructedReference.substring(i) + "\n" + "Reference: " + reference.substring(pos, pos + i) + "|" + reference.substring(pos + i) + "\n";
            }
        }
        return "No prefix contained";
    }
}
