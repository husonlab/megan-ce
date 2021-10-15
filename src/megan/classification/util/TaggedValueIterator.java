/*
 * TaggedValueIterator.java Copyright (C) 2021. Daniel H. Huson
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

package megan.classification.util;

import jloda.util.ProgramProperties;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * iterator over all values following an occurrence of tag in aLine.
 * Created by huson on 7/21/16.
 */
public class TaggedValueIterator implements Iterator<String>, java.lang.Iterable<String> {
    private String aLine;
    private final String[] tags;
    private int tagPos;
    private final boolean attemptFirstWord;
    private boolean enabled;

    private String nextResult;

   // private final boolean allowMinusInAccession= ProgramProperties.get("AllowMinusInAccession",false);

    /**
     * iterator over all values following an occurrence of tag in aLine.
     * Example: aLine= gi|4444|gi|5555  and tag=gi|  with return 4444 and then 5555
     * Value consists of letters, digits or underscore
     *
     * @param attemptFirstWord
     * @param tags
     * @return iterator
     */
    public TaggedValueIterator(final boolean attemptFirstWord, final boolean enabled, final String... tags) {
        this(null, attemptFirstWord, enabled, tags);
    }

    /**
     * iterator over all values following an occurrence of tag in aLine.
     * Example: aLine= gi|4444|gi|5555  and tag=gi|  with return 4444 and then 5555
     * Value consists of letters, digits or underscore
     *
     * @param aLine
     * @param attemptFirstWord if true, attempts to parse the first word in a fasta header string as a value
     * @param tags
     * @return iterator
     */
    public TaggedValueIterator(final String aLine, final boolean attemptFirstWord, final String... tags) {
        this(aLine, attemptFirstWord, true, tags);
    }


    /**
     * iterator over all values following an occurrence of tag in aLine.
     * Example: aLine= gi|4444|gi|5555  and tag=gi|  with return 4444 and then 5555
     * Value consists of letters, digits or underscore
     *
     * @param aLine
     * @param attemptFirstWord if true, attempts to parse the first word in a fasta header string as a value
     * @param enabled
     * @param tags
     * @return iterator
     */
    public TaggedValueIterator(final String aLine, final boolean attemptFirstWord, final boolean enabled, final String... tags) {
        this.attemptFirstWord = attemptFirstWord;
        this.enabled = enabled;
        this.tags = tags;
        if (aLine != null)
            restart(aLine);
    }

    public TaggedValueIterator iterator() {
        return this;
    }

    /**
     * restart the iterator with a new string
     *
     * @param aLine
     */
    public TaggedValueIterator restart(String aLine) {
        this.aLine = aLine;
        tagPos = 0;
        nextResult = getNextResult();

        if (attemptFirstWord) {
            var a = 0;
            while (a < aLine.length()) {
                if (aLine.charAt(a) == '>' || aLine.charAt(a) == '@' || Character.isWhitespace(aLine.charAt(a)))
                    a++;
                else
                    break;
            }
            var b = a + 1;
            // while(b <aLine.length() && Character.isLetterOrDigit(aLine.charAt(b)) || aLine.charAt(b) == '_' || (allowMinusInAccession && aLine.charAt(b) == '-')) {
            while(b <aLine.length() && Character.isLetterOrDigit(aLine.charAt(b)) || aLine.charAt(b) == '_') {
                b++;
            }
            if (b - a > 4) {
                nextResult = aLine.substring(a, b);
            }
            tagPos = b;
        }
        return this;
    }

    @Override
    public boolean hasNext() {
        return nextResult != null;
    }

    @Override
    public String next() {
        if (nextResult == null)
            throw new NoSuchElementException();
        final var result = nextResult;
        nextResult = getNextResult();
        return result;
    }

    @Override
    public void remove() {

    }

    /**
     * gets the next result
     *
     * @return next result or null
     */
    private String getNextResult() {
        loop:
        while (tagPos < aLine.length()) {
            if (attemptFirstWord && aLine.charAt(tagPos) == 1) {
                tagPos++;
                break;
            }
            for (var tag : tags) {
                if (match(aLine, tagPos, tag)) {
                    tagPos += tag.length();
                    break loop;
                }
            }
            tagPos++;
        }
        if (tagPos >= aLine.length())
            return null;

        var b = tagPos + 1;
        // while(b <aLine.length() && Character.isLetterOrDigit(aLine.charAt(b)) || aLine.charAt(b) == '_' || (allowMinusInAccession && aLine.charAt(b) == '-')) {
        while(b <aLine.length() && Character.isLetterOrDigit(aLine.charAt(b)) || aLine.charAt(b) == '_') {
            b++;
        }
         var result = aLine.substring(tagPos, b);
        tagPos = b;
        return result;
    }

    /**
     * does the query match the string starting at the offset
     *
     * @param string
     * @param offset
     * @param query
     * @return true, if string starts with query at offset
     */
    private static boolean match(final String string, final int offset, final String query) {
        if (string.length() - offset < query.length())
            return false;

        for (var i = 0; i < query.length(); i++) {
            if (string.charAt(offset + i) != query.charAt(i))
                return false;
        }
        return true;
    }

    /**
     * gets the first element or null
     *
     * @return first or null
     */
    public String getFirst() {
        if (hasNext())
            return next();
        else
            return null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
