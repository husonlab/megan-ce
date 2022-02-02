/*
 * ReadMagnitudeParser.java Copyright (C) 2022 Daniel H. Huson
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
import jloda.util.StringUtils;

/**
 * parses the weight of a read from the fasta header line
 * Daniel Huson, 2.2012
 */
public class ReadMagnitudeParser {
    private final static String WEIGHT_TAG = "weight";
    private final static String MAGNITUDE_TAG = "magnitude";

    private static boolean enabled = false;
    private static boolean underScoreEnabled = false;

    private static boolean warned = false;

    /**
     * attempt to parse the weight from the info line of a read
     *
     * @return weight or 1
     */
    public static int parseMagnitude(String aLine) {
        return parseMagnitude(aLine, enabled);
    }

    /**
     * attempt to parse the weight from the info line of a read
     */
    public static int parseMagnitude(String aLine, boolean enabled) {
        if (aLine != null) {
            if (underScoreEnabled) {
                if (!warned) {
                    System.err.println("Using underscore parsing of magnitudes - only use with CREST");
                    warned = true;
                }
				String firstWord = StringUtils.getFirstWord(aLine);
				int pos = firstWord.lastIndexOf('_');
                if (NumberUtils.isInteger(firstWord.substring(pos + 1)))
                    return Math.max(1, Integer.parseInt(firstWord.substring(pos + 1)));
            }
            if (enabled) {
                int pos = aLine.indexOf(MAGNITUDE_TAG);
                int next = pos + MAGNITUDE_TAG.length();
                if (pos == -1) {
                    pos = aLine.indexOf(WEIGHT_TAG);
                    next = pos + WEIGHT_TAG.length(); // "weight".length();
                }
                if (pos >= 0 && next < aLine.length() && (aLine.charAt(next) == '|' || aLine.charAt(next) == '=')) {
                    int end = next + 1;
                    while (end < aLine.length() && Character.isDigit(aLine.charAt(end)))
                        end++;
                    String number = aLine.substring(next + 1, end);
                    if (NumberUtils.isInteger(number))
                        return Math.max(1, NumberUtils.parseInt(number));
                }
            }
        }
        return 1;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        ReadMagnitudeParser.enabled = enabled;
    }

    public static boolean isUnderScoreEnabled() {
        return underScoreEnabled;
    }

    public static void setUnderScoreEnabled(boolean underScoreEnabled) {
        ReadMagnitudeParser.underScoreEnabled = underScoreEnabled;
    }
}
