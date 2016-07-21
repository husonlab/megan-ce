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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * iterator over all values following an occurrence of tag in aLine.
 * Created by huson on 7/21/16.
 */
public class TaggedValueIterator implements Iterator<String> {
    private final String aLine;
    private final String[] tags;
    private int tagNumber;
    private int tagPos;

    private String nextResult;

    /**
     * iterator over all values following an occurrence of tag in aLine.
     * Example: aLine= gi|4444|gi|5555  and tag=gi|  with return 4444 and then 5555
     * Value consists of letters, digits or underscore
     *
     * @param aLine
     * @param tags
     * @return iterator
     */
    public TaggedValueIterator(final String aLine, final String... tags) {
        this(aLine, false, tags);
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
        this.aLine = aLine;
        this.tags = tags;
        tagNumber = 0;
        tagPos = -1;
        nextResult = getNextResult();

        if (attemptFirstWord) {
            int a = 0;
            while (a < aLine.length()) {
                if (aLine.charAt(a) == '>' || Character.isWhitespace(aLine.charAt(a)))
                    a++;
                else
                    break;
            }
            int b = a + 1;
            while (b < aLine.length()) {
                if (Character.isLetterOrDigit(aLine.charAt(b)) || aLine.charAt(b) == '_')
                    b++;
                else
                    break;
            }
            if (b - a > 4) {
                nextResult = aLine.substring(a, b);
            }
        }
    }

    @Override
    public boolean hasNext() {
        return nextResult != null;
    }

    @Override
    public String next() {
        if (nextResult == null)
            throw new NoSuchElementException();
        final String result = nextResult;
        nextResult = getNextResult();
        return result;
    }

    /**
     * gets the next result
     *
     * @return next result or null
     */
    private String getNextResult() {
        while (tagNumber < tags.length) {
            if (tagPos == -1)
                tagPos = aLine.indexOf(tags[tagNumber]);
            else
                tagPos = aLine.indexOf(tags[tagNumber], tagPos + 1);

            if (tagPos != -1)
                break;
            else {
                tagNumber++;
            }
        }
        if (tagPos == -1 || tagNumber == tags.length)
            return null;
        else
            tagPos += tags[tagNumber].length(); // assume that pos points to begin of tag, skip past tag
        int b = tagPos + 1;
        while (b < aLine.length() && (Character.isLetterOrDigit(aLine.charAt(b)) || aLine.charAt(b) == '_'))
            b++;
        return aLine.substring(tagPos, b);
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


    public static void main(String[] args) {
        final String aLine = "> HEllO gi|446150911|ref|WP_000228766.1| MULTISPECIES: DNA-directed RNA polymerase subunit beta' [Streptococcus]gi|41018114|sp|Q97NQ8.1|RPOC_STRPN RecName: Full=DNA-directed RNA polymerase subunit beta'; Short=RNAP subunit beta'; AltName: Full=RNA polymerase subunit beta'; AltName: Full=Transcriptase subunit beta'gi|226699509|sp|B1I8R3.1|RPOC_STRPI RecName: Full=DNA-directed RNA polymerase subunit beta'; Short=RNAP subunit beta'; AltName: Full=RNA polymerase subunit beta'; AltName: Full=Transcriptase subunit beta'gi|226699510|sp|B2IM39.1|RPOC_STRPS RecName: Full=DNA-directed RNA polymerase subunit beta'; Short=RNAP subunit beta'; AltName: Full=RNA polymerase subunit beta'; AltName: Full=Transcriptase subunit beta'gi|254765335|sp|C1CA05.1|RPOC_STRP7 RecName: Full=DNA-directed RNA polymerase subunit beta'; Short=RNAP subunit beta'; AltName: Full=RNA polymerase subunit beta'; AltName: Full=Transcriptase subunit beta'gi|254765338|sp|C1CGP3.1|RPOC_STRZJ RecName: Full=DNA-directed RNA polymerase subunit beta'; Short=RNAP subunit beta'; AltName: Full=RNA polymerase subunit beta'; AltName: Full=Transcriptase subunit beta'gi|254765340|sp|C1CTL3.1|RPOC_STRZT RecName: Full=DNA-directed RNA polymerase subunit beta'; Short=RNAP subunit beta'; AltName: Full=RNA polymerase subunit beta'; AltName: Full=Transcriptase subunit beta'gi|14973466|gb|AAK76027.1| DNA-directed RNA polymerase, beta' subunit [Streptococcus pneumoniae TIGR4]gi|147755682|gb|EDK62728.1| DNA-directed RNA polymerase subunit beta' [Streptococcus pneumoniae SP11-BS70]gi|147761006|gb|EDK67975.1| DNA-directed RNA polymerase subunit beta' [Streptococcus pneumoniae SP18-BS74]gi|147922259|gb|EDK73380.1| DNA-directed RNA polymerase subunit beta' [Streptococcus pneumoniae SP3-BS71]gi|147924619";

        System.err.println("Line: " + aLine);

        System.err.print("Values:");
        for (Iterator<String> it = new TaggedValueIterator(aLine, false, "gi|", "ref|", "gb|"); it.hasNext(); ) {
            System.err.print(" " + it.next());
        }
        System.err.println();
    }
}
