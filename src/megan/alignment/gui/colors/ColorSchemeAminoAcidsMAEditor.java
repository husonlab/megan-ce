/*
 *  Copyright (C) 2017 Daniel H. Huson
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
public class ColorSchemeAminoAcidsMAEditor extends ColorSchemeBase implements IColorScheme {
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
            case 'A':
            case 'G':
                return getDefinedColor(ch, 0x77DD88);
            case 'C':
                return getDefinedColor(ch, 0x99EE66);
            case 'D':
            case 'E':
            case 'N':
            case 'Q':
                return getDefinedColor(ch, 0x55BB33);
            case 'I':
            case 'L':
            case 'M':
            case 'V':
                return getDefinedColor(ch, 0x66BBFF);
            case 'F':
            case 'W':
            case 'Y':
                return getDefinedColor(ch, 0x9999FF);
            case 'H':
                return getDefinedColor(ch, 0x5555FF);
            case 'K':
            case 'R':
                return getDefinedColor(ch, 0xFFCC77);
            case 'P':
                return getDefinedColor(ch, 0xEEAAAA);
            case 'S':
            case 'T':
                return getDefinedColor(ch, 0xFF4455);
            default:
                return getDefinedColor(ch, 0x778899); // Light Slate Gray
        }
    }
}
