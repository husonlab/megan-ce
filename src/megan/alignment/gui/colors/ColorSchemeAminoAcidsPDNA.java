/*
 * ColorSchemeAminoAcidsPDNA.java Copyright (C) 2020. Daniel H. Huson
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
package megan.alignment.gui.colors;

import java.awt.*;

/**
 * amino acid color scheme
 * Daniel Huson, 9.2011
 */
public class ColorSchemeAminoAcidsPDNA extends ColorSchemeBase implements IColorScheme {
    /**
     * gets the background color for the amino-acid
     * PDNA groups: LVIMC AGSTP FYW EDNQKRH
     *
     * @param ch
     * @return color
     */
    public Color getBackground(int ch) {
        ch = Character.toUpperCase(ch);
        switch (ch) {
            case 'I':
            case 'L':
            case 'V':
            case 'M':
            case 'C':
                return getDefinedColor(ch, 0x15C015);   //green
            case 'A':
            case 'G':
            case 'S':
            case 'T':
            case 'P':
                return getDefinedColor(ch, 0xF09048); // orange
            case 'F':
            case 'Y':
            case 'W':
                return getDefinedColor(ch, 0x80A0F0); // blue
            case 'R':
            case 'N':
            case 'D':
            case 'Q':
            case 'E':
            case 'H':
            case 'K':
                return getDefinedColor(ch, 0xF01505); // red
            default:
                return getDefinedColor(ch, 0x778899); // Light Slate Gray
        }
    }
}
