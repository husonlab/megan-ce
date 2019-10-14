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
package megan.classification.util;

import java.util.Arrays;
import java.util.Comparator;

/**
 * some additional utils for parsing taxon names and similar
 * Daniel Huson, 1.2009
 */
public class MultiWords {
    private final static int DEFAULT_MIN_LENGTH = 2;
    private final static int DEFAULT_MAX_LENGTH = 128;
    private int[][] pairs;
    private int[] starts;
    private int[] ends;

    private String line;

    public MultiWords() {
        pairs = new int[DEFAULT_MAX_LENGTH][2];
        starts = new int[DEFAULT_MAX_LENGTH];
        ends = new int[DEFAULT_MAX_LENGTH];
    }

    /**
     * gets all the sets of one or more consecutive words, in decreasing order of length.
     * Assumes minimum length is 2 and maximum length is 80
     *
     * @param str
     * @return sets of one or more consecutive words
     */
    public int compute(String str) {
        return compute(str, DEFAULT_MIN_LENGTH, DEFAULT_MAX_LENGTH);
    }

    /**
     * gets all the sets of one or more consecutive words, in decreasing order of length
     *
     * @param line
     * @param minLength
     * @param maxLength
     * @return sets of one or more consecutive words
     */

    public int compute(String line, int minLength, int maxLength) {
        this.line = line;

        int countStarts = 0;
        int countEnds = 0;

        for (int i = 0; i < line.length(); i++) {
            if ((i == 0 || !Character.isLetterOrDigit(line.charAt(i - 1))) && (Character.isLetterOrDigit(line.charAt(i)))) {
                if (countStarts == starts.length)
                    starts = grow(starts);
                starts[countStarts++] = i;
            }
            if ((i == line.length() - 1 || !Character.isLetterOrDigit(line.charAt(i + 1)))
                    && (Character.isLetterOrDigit(line.charAt(i)))) {
                if (countEnds == ends.length)
                    ends = grow(ends);
                ends[countEnds++] = i + 1;
            }
            if (line.charAt(i) == ')' || line.charAt(i) == ']' || line.charAt(i) == '}') {
                if (countEnds == ends.length)
                    ends = grow(ends);
                ends[countEnds++] = i + 1;
            }
        }
        int count = 0;

        for (int i = 0; i < countStarts; i++) {
            int start = starts[i];
            for (int j = 0; j < countEnds; j++) {
                int end = ends[j];
                if (end - start >= minLength && end - start <= maxLength) {
                    if (count == pairs.length) {
                        pairs = grow(pairs);
                    }
                    pairs[count][0] = start;
                    pairs[count++][1] = end;
                }
            }
        }

        Arrays.sort(pairs, 0, count, (a, b) -> {
            int aLen = a[1] - a[0];
            int bLen = b[1] - b[0];
            if (aLen > bLen)
                return -1;
            else if (aLen < bLen)
                return 1;
            if (a[0] < b[0])
                return -1;
            else if (a[0] > b[0])
                return 1;
            else
                return 0; // have same length and start, must be equalOverShorterOfBoth
        });
        return count;
    }

    /**
     * get the i-th pair
     *
     * @param i
     * @return pair
     */
    public int[] getPair(int i) {
        return pairs[i];
    }

    /**
     * get the i-th word
     *
     * @param i
     * @return word
     */
    public String getWord(int i) {
        return line.substring(pairs[i][0], pairs[i][1]);
    }

    private static int[] grow(final int[] array) {
        final int[] result = new int[Math.max(10, 2 * array.length)];
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }

    private static int[][] grow(final int[][] array) {
        final int[][] result = new int[Math.max(10, 2 * array.length)][2];
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }
}
