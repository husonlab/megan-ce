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
package megan.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * selector where to match regular expressions in find
 * Daniel Huson, 4.2010
 */
public class FindSelection {
    public boolean useReadName = false;
    public boolean useReadHeader = false;
    public boolean useReadSequence = false;
    public boolean useMatchText = false;

    /**
     * does the given readBlock match the given pattern
     *
     * @param readBlock
     * @param pattern
     * @return true, if match
     */
    static public boolean doesMatch(FindSelection findSelection, IReadBlock readBlock, Pattern pattern) {
        try {
            if (findSelection.useReadName && matches(pattern, readBlock.getReadName()))
                return true;
            if (findSelection.useReadHeader && matches(pattern, readBlock.getReadHeader()))
                return true;
            if (findSelection.useReadSequence && matches(pattern, readBlock.getReadSequence()))
                return true;
            if (findSelection.useMatchText) {
                for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
                    if (matches(pattern, readBlock.getMatchBlock(i).getText()))
                        return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * does label match pattern?
     *
     * @param pattern
     * @param label
     * @return true, if match
     */
    private static boolean matches(Pattern pattern, String label) {
        if (label == null)
            label = "";
        Matcher matcher = pattern.matcher(label);
        return matcher.find();
    }

}
