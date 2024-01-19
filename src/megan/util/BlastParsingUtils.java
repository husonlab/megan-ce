/*
 * BlastParsingUtils.java Copyright (C) 2024 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package megan.util;

import jloda.util.NumberUtils;
import megan.alignment.Blast2Alignment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.StringTokenizer;

/**
 * methods to help parse blast strings
 * Daniel Huson, 3.2018
 */
public class BlastParsingUtils {
    /**
     * removes all of string from the second occurrence of the given word onward
     *
     * @return truncated string
     */
    public static String truncateBeforeSecondOccurrence(String text, String word) {
        int pos = text.indexOf(word);
        if (pos == -1)
            return text;
        pos = text.indexOf(word, pos + 1);
        if (pos == -1)
            return text;
        else
            return text.substring(0, pos);
    }

    /**
     * grab the total query string
     *
     * @return query string
	 */
    public static String grabQueryString(String text) throws IOException {
        BufferedReader r = new BufferedReader(new StringReader(text));
        String aLine;
        StringBuilder buf = new StringBuilder();
        boolean passedScore = false;
        while ((aLine = r.readLine()) != null) {
            aLine = aLine.trim();
            if (aLine.startsWith("Score")) {
                if (!passedScore)
                    passedScore = true;
                else
                    break;
            }
            if (aLine.startsWith("Query")) {
                String[] words = aLine.split(" +");
                buf.append(words[2]);
            }
        }
        return buf.toString().replaceAll("\n", "").replaceAll("\r", "");
    }

    /**
     * grab the total subject string
     *
     * @return subject string
	 */
    public static String grabSubjectString(String text) throws IOException {
        BufferedReader r = new BufferedReader(new StringReader(text));
        String aLine;
        StringBuilder buf = new StringBuilder();
        boolean passedScore = false;
        while ((aLine = r.readLine()) != null) {
            aLine = aLine.trim();
            if (aLine.startsWith("Score")) {
                if (!passedScore)
                    passedScore = true;
                else
                    break;
            }
            if (aLine.startsWith("Sbjct")) {
                String[] words = aLine.split(" +");
                buf.append(words[2]);
            }
        }
        return buf.toString().replaceAll("\n", "").replaceAll("\r", "");
    }

    /**
     * grab the next  token after the one in key
     *
     * @return next token
     */
    public static String grabNext(String text, String key, String key2) {
        int pos = text.indexOf(key);
        int length = key.length();
        if (pos == -1 && key2 != null) {
            pos = text.indexOf(key2);
            length = key2.length();
        }
        if (pos == -1)
            return null;
        else
            return new StringTokenizer(text.substring(pos + length).trim()).nextToken();
    }

    /**
     * grab the next three tokens after the one in key
     *
     * @return next token
     */
    public static String[] grabNext3(String text, String key, String key2) {
        int pos = text.indexOf(key);
        int length = key.length();
        if (pos == -1 && key2 != null) {
            pos = text.indexOf(key2);
            length = key2.length();
        }
        if (pos == -1)
            return null;
        else {
            String[] result = new String[3];
            StringTokenizer st = new StringTokenizer(text.substring(pos + length).trim());
            for (int i = 0; i < 3; i++) {
                if (st.hasMoreTokens())
                    result[i] = st.nextToken();
                else
                    return null; // ran out of tokens, return null
            }
            return result;
        }
    }

    /**
     * grab the last token of the last line that contains the given key and is passed the first occurrence of "Score"
     *
     * @return token
     */
    public static String grabLastInLinePassedScore(String text, String key) throws IOException {
        int scorePos = text.indexOf("Score");
        if (scorePos == -1)
            throw new IOException("Token not found: 'Score'");

        int end = text.lastIndexOf(key);
        if (end == -1)
            throw new IOException("Token not found: '" + key + "'");
        if (end < scorePos)
            throw new IOException("Token not found before 'Score': '" + key + "'");

        end = text.indexOf("\n", end);
        if (end == -1)
            end = text.length() - 1;
        // skip other preceding white space
        while (end > 0 && Character.isWhitespace(text.charAt(end)))
            end--;
        // end is now last letter of token
        int start = end;
        // find white space before last token:
        while (start > 0 && !Character.isWhitespace(text.charAt(start)))
            start--;
        start += 1;
        // start is now first letter of token
        return text.substring(start, end + 1);
    }

    /**
     * guesses the blast type
     *
     * @return blast type
     */
    public static String guessBlastType(String blastText) {
        if (blastText == null || !blastText.contains("Query"))
            return Blast2Alignment.UNKNOWN;
        if (blastText.contains("Frame=") || (blastText.contains("Frame =")))
            return Blast2Alignment.BLASTX;
        if (blastText.contains("Strand=") || blastText.contains("Strand ="))
            return Blast2Alignment.BLASTN;
        return Blast2Alignment.BLASTP;
    }

    /**
     * remove the header from a blast text (but keeping Length statement, if present)
     *
     * @return headerless blastText
     */
    public static String removeReferenceHeaderFromBlastMatch(String blastText) {
        int index = blastText.indexOf("Length");
        if (index == -1)
            index = blastText.indexOf("Score");
        if (index > 0)
            return blastText.substring(index);
        else
            return blastText;
    }

    public static int getStartSubject(String text) {
		return NumberUtils.parseInt(BlastParsingUtils.grabNext(text, "Sbjct:", "Sbjct"));
    }

    public static int getEndSubject(String text) throws IOException {
		return NumberUtils.parseInt(BlastParsingUtils.grabLastInLinePassedScore(text, "Sbjct"));
    }
}
