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
package megan.alignment.gui.colors;

import java.awt.*;

/**
 * amino acid color scheme
 * Daniel Huson, 9.2011
 */
public class ColorSchemeAminoAcidsCINEMA extends ColorSchemeBase implements IColorScheme {
    /**
     * gets the background color for the amino-acid
     *
     * @param ch
     * @return color
     */
    public Color getBackground(int ch) {
        ch = Character.toUpperCase(ch);
        // source: http://www.bioinformatics.nl/~berndb/aacolour.html
        switch (ch) {
            case 'H':
            case 'K':
            case 'R':
                return getDefinedColor(ch, 0x00FFFF);
            case 'D':
            case 'E':
                return getDefinedColor(ch, 0xFF0000);
            case 'S':
            case 'T':
            case 'N':
            case 'Q':
                return getDefinedColor(ch, 0x00FF00);
            case 'A':
            case 'V':
            case 'I':
            case 'L':
            case 'M':
                return getDefinedColor(ch, 0xBBBBBB);
            case 'F':
            case 'W':
            case 'Y':
                return getDefinedColor(ch, 0xFF00FF);
            case 'P':
            case 'G':
                return getDefinedColor(ch, 0x996600);
            case 'C':
                return getDefinedColor(ch, 0xFFFF00);
            default:
                return getDefinedColor(ch, 0x778899); // Light Slate Gray
        }
    }
}
