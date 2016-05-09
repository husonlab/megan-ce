/*
 *  Copyright (C) 2015 Daniel H. Huson
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

/**
 * access all tagged accessions in a line
 * Daniel Huson, 3.2016
 */
public class MultiTaggedAccessions {
    private final boolean attemptFirstWord;
    private final String[] idTags;

    private boolean enabled;

    private int[][] pairs;
    private String line;

    /**
     * constructor
     *
     * @param attemptFirstWord
     */
    public MultiTaggedAccessions(boolean attemptFirstWord, String[] idTags) {
        this(attemptFirstWord, idTags, true);
    }

    /**
     * constructor
     *
     * @param attemptFirstWord
     * @param idTags
     * @param enabled
     */
    public MultiTaggedAccessions(boolean attemptFirstWord, String[] idTags, boolean enabled) {
        this.attemptFirstWord = attemptFirstWord;
        this.idTags = idTags;
        this.enabled = enabled && (attemptFirstWord || idTags.length > 0);

        pairs = new int[128][2];
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * compute all accessions that follow any one of the tags
     *
     * @param line
     * @return number of accessions found
     */
    public int compute(String line) {
        this.line = line;
        int count = 0;

        if (attemptFirstWord) {
            int a = 0;
            while (a < line.length()) {
                if (line.charAt(a) == '>' || Character.isWhitespace(line.charAt(a)))
                    a++;
                else
                    break;
            }
            int b = a + 1;
            while (b < line.length()) {
                if (Character.isLetterOrDigit(line.charAt(b)) || line.charAt(b) == '_')
                    b++;
                else
                    break;
            }
            if (b - a > 4) {
                pairs[count][0] = a;
                pairs[count++][1] = b;
            }
        }

        if (idTags.length > 0) {
            for (String tag : idTags) {
                int b;
                for (int a = line.indexOf(tag); a != -1; a = line.indexOf(tag, b + 1)) {
                    a += tag.length();
                    b = a + 1;
                    while (b < line.length() && (Character.isLetterOrDigit(line.charAt(b)) || line.charAt(b) == '_'))
                        b++;
                    if (b > a) {
                        if (count == pairs.length)
                            pairs = MultiWords.grow(pairs);
                        pairs[count][0] = a;
                        pairs[count++][1] = b;
                    }
                }

            }
        }
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

    /**
     * get the first accession in the given line
     *
     * @param line
     * @return first or null
     */
    public String getFirst(String line) {
        int count = compute(line);
        if (count > 0)
            return getWord(0);
        else
            return null;
    }
}
