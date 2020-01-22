/*
 * Utilities.java Copyright (C) 2020. Daniel H. Huson
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
 *
 */
package megan.parsers.blast;

/**
 * some utilities
 * Daniel Huson, 4.2016
 */
public class Utilities {
    /**
     * append the cigar string
     *
     * @param alignedQuery
     * @param alignedReference
     * @param buffer
     */
    public static void appendCigar(String alignedQuery, String alignedReference, StringBuilder buffer) {
        char cigarState = 'M'; // M in match, D deletion, I insertion
        int count = 0;
        for (int i = 0; i < alignedQuery.length(); i++) {
            if (alignedQuery.charAt(i) == '-') {
                if (cigarState == 'D') {
                    count++;
                } else if (count > 0) {
                    buffer.append(count).append(cigarState);
                    cigarState = 'D';
                    count = 1;
                }
            } else if (alignedReference.charAt(i) == '-') {
                if (cigarState == 'I') {
                    count++;
                } else if (count > 0) {
                    buffer.append(count).append(cigarState);
                    cigarState = 'I';
                    count = 1;
                }
            } else {  // match or mismatch
                if (cigarState == 'M') {
                    count++;
                } else if (count > 0) {
                    buffer.append(count).append(cigarState);
                    cigarState = 'M';
                    count = 1;
                }
            }
        }
        if (count > 0) {
            buffer.append(count).append(cigarState);
        }
    }

    /**
     * append the MD string
     *
     * @param alignedQuery
     * @param alignedReference
     * @param buffer
     */
    public static void appendMDString(final String alignedQuery, final String alignedReference, final StringBuilder buffer) {
        buffer.append("MD:Z:");
        int countMatches = 0;
        boolean inDeletion = false;
        for (int i = 0; i < alignedQuery.length(); i++) {
            final char qChar = alignedQuery.charAt(i);
            final char rChar = alignedReference.charAt(i);

            if (qChar == '-') { // gap in query
                if (countMatches > 0) {
                    buffer.append(countMatches);
                    countMatches = 0;
                }
                if (!inDeletion) {
                    buffer.append("^");
                    inDeletion = true;
                }
                buffer.append(rChar);
            } else if (rChar != '-') {  // match or mismatch
                if (qChar == rChar) {
                    countMatches++;
                } else {
                    if (inDeletion)
                        buffer.append("0");
                    if (countMatches > 0) {
                        buffer.append(countMatches);
                        countMatches = 0;
                    }
                    buffer.append(rChar);
                }
                if (inDeletion)
                    inDeletion = false;
            }
            // else alignedReference[i] == '-': this has no effect
        }
        if (countMatches > 0)
            buffer.append(countMatches);
        else if (inDeletion)
            buffer.append(0);
    }

    /**
     * compute edit distance from alignment
     *
     * @param alignedQuery
     * @param alignedReference
     * @return edit distance
     */
    public static int computeEditDistance(String alignedQuery, String alignedReference) {
        int distance = 0;
        for (int i = 0; i < alignedQuery.length(); i++) {
            if (alignedQuery.charAt(i) == '-' || alignedReference.charAt(i) == '-' || alignedQuery.charAt(i) != alignedReference.charAt(i))
                distance++;
        }
        return distance;
    }
}
