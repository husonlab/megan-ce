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
package megan.alignment.gui;

import jloda.util.SequenceUtils;

/**
 * represents a lane of an alignment
 * Daniel Huson, 9.2011
 */
public class Lane {
    private final Alignment alignment;
    private final String name;
    private int leadingGaps;
    private String block;
    private int trailingGaps;
    private final String unalignedPrefix;
    private final String unalignedSuffix;
    private final String toolTip;
    private final String text;

    /**
     * constructor
     *
     * @param name
     * @param text
     * @param toolTip
     * @param sequence
     */
    public Lane(Alignment alignment, String name, String text, String toolTip, String sequence) {
        this(alignment, name, text, toolTip, "", sequence, "");
    }

    /**
     * constructor
     *
     * @param name
     * @param text
     * @param toolTip
     * @param sequence
     */
    public Lane(Alignment alignment, String name, String text, String toolTip, String unalignedPrefix, String sequence, String unalignedSuffix) {
        this.alignment = alignment;
        this.name = name;
        this.text = text;
        if (toolTip != null)
            this.toolTip = toolTip;
        else
            this.toolTip = name;
        this.unalignedPrefix = unalignedPrefix;
        this.unalignedSuffix = unalignedSuffix;

        int firstNonGapPos = 0;
        while (firstNonGapPos < sequence.length() && sequence.charAt(firstNonGapPos) == '-')
            firstNonGapPos++;
        if (firstNonGapPos == sequence.length()) {
            leadingGaps = firstNonGapPos;
            block = "";
            trailingGaps = 0;
        } else {
            int lastNonGapPos = sequence.length() - 1;
            while (lastNonGapPos >= 0 && sequence.charAt(lastNonGapPos) == '-')
                lastNonGapPos--;
            leadingGaps = firstNonGapPos;
            setBlock(sequence.substring(firstNonGapPos, lastNonGapPos + 1));
            trailingGaps = sequence.length() - lastNonGapPos - 1;
        }
    }

    /**
     * constructor
     *
     * @param text
     * @param toolTip
     * @param unalignedPrefix
     * @param leadingGaps     number of leading gaps
     * @param block           - block from first to last non-gap symbols
     * @param trailingGaps    number of trailing gaps
     * @param unalignedSuffix
     */
    protected Lane(Alignment alignment, String name, String text, String toolTip, String unalignedPrefix, int leadingGaps, String block, int trailingGaps, String unalignedSuffix) {
        this.alignment = alignment;
        this.name = name;
        this.text = text;
        if (toolTip != null)
            this.toolTip = toolTip;
        else
            this.toolTip = name;
        this.leadingGaps = leadingGaps;
        this.unalignedPrefix = unalignedPrefix;
        setBlock(block);
        this.unalignedSuffix = unalignedSuffix;
        this.trailingGaps = trailingGaps;
    }

    public String getName() {
        return name;
    }

    public int getLeadingGaps() {
        return leadingGaps;
    }

    /**
     * get trailing gaps
     *
     * @return trailing gaps
     */
    public int getTrailingGaps() {
        return trailingGaps;
    }

    public String getUnalignedPrefix() {
        return unalignedPrefix;
    }

    public String getBlock() {
        return block;
    }

    public String getUnalignedSuffix() {
        return unalignedSuffix;
    }

    public int getFirstNonGapPosition() {
        return getLeadingGaps();
    }

    public int getLastNonGapPosition() {
        return getLeadingGaps() + getBlock().length();
    }

    public void setLeadingGaps(int leadingGaps) {
        this.leadingGaps = leadingGaps;
    }

    public void setTrailingGaps(int trailingGaps) {
        this.trailingGaps = trailingGaps;
    }

    public void setBlock(String block) {
        this.block = block;  // don't replace spaces here, will break reference sequence
    }

    /**
     * get the character at the given position
     *
     * @param pos
     * @return char at position
     */
    public char charAt(int pos) {
        if (pos < getLeadingGaps() || pos >= getLength() - getTrailingGaps())
            return 0;
        else if (alignment == null || !alignment.isTranslate())
            return block.charAt(pos - getLeadingGaps());
        else {
            int which = pos - getLeadingGaps();
            if ((which % 3) == 0) {
                if (which + 2 < block.length()) {
                    return (char) SequenceUtils.getAminoAcid(block.charAt(which), block.charAt(which + 1), block.charAt(which + 2));
                } else
                    return block.charAt(which);
            } else
                return ' ';
        }
    }

    public String toStringIncludingLeadingAndTrailingGaps() {
        StringBuilder buf = new StringBuilder();
        buf.append("-".repeat(Math.max(0, getLeadingGaps())));
        buf.append(block);
        buf.append("-".repeat(Math.max(0, getTrailingGaps())));
        return buf.toString();
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (getLeadingGaps() > 0)
            buf.append("[").append(getLeadingGaps()).append("]");
        buf.append(block);
        if (getTrailingGaps() > 0)
            buf.append("[").append(getTrailingGaps()).append("]");
        return buf.toString();
    }

    public int getLength() {
        return leadingGaps + trailingGaps + this.block.length();
    }

    /**
     * is there an unaligned character at this position?
     *
     * @param col
     * @return true, if unaligned char available for this position
     */
    public boolean hasUnalignedCharAt(int col) {
        if (col < getFirstNonGapPosition()) {
            int firstUnalignedPrefixPos = getFirstNonGapPosition() - unalignedPrefix.length();
            return col > firstUnalignedPrefixPos;
        } else if (col >= getLastNonGapPosition()) {
            int lastUnalignedSuffixPos = getLastNonGapPosition() + unalignedSuffix.length();
            return col < lastUnalignedSuffixPos;
        }
        return false;
    }

    /**
     * get the unaligned char at the given position
     *
     * @param col
     * @return unaligned char
     */
    public char getUnalignedCharAt(int col) {
        col--;
        if (col < getFirstNonGapPosition()) {
            int firstUnalignedPrefixPos = getFirstNonGapPosition() - unalignedPrefix.length();
            if (col >= firstUnalignedPrefixPos)
                return unalignedPrefix.charAt(col - firstUnalignedPrefixPos);
        } else if (col >= getLastNonGapPosition() - 1) {
            return unalignedSuffix.charAt(col - getLastNonGapPosition() + 1);
        }
        return '-';
    }

    public String getText() {
        return text;
    }

    public String getToolTip() {
        return toolTip;
    }

    /**
     * gets the whole aligned sequence  with all leading and trailing gaps
     *
     * @return sequence
     */
    public String getSequence() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < getLength(); i++) {
            char ch = charAt(i);
            if (Character.isLetter(ch) || ch == '?')
                buf.append(charAt(i));
            else
                buf.append('-');
        }
        return buf.toString();
    }

    public void trimFromEnd(int newLength) {
        int length = leadingGaps + block.length();
        trailingGaps = newLength - length;
    }
}
