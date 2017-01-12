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
public class ColorSchemeAminoAcidsClustalX extends ColorSchemeBase implements IColorScheme {
    /**
     * gets the background color for the amino-acid
     *
     * @param ch
     * @return color
     */
    public Color getBackground(int ch) {
        ch = Character.toUpperCase(ch);
        // source: http://www.jalview.org/help/html/colourSchemes/clustal.html
        switch (ch) {
            case 'A':
            case 'C':
            case 'I':
            case 'L':
            case 'M':
            case 'F':
            case 'W':
            case 'V':
                return getDefinedColor(ch, 0x80A0F0);
            case 'K':
            case 'R':
                return getDefinedColor(ch, 0xF01505);
            case 'N':
            case 'Q':
            case 'S':
            case 'T':
                return getDefinedColor(ch, 0x15C015);
            case 'D':
            case 'E':
                return getDefinedColor(ch, 0xC048C0);
            case 'G':
                return getDefinedColor(ch, 0xF09048);
            case 'P':
                return getDefinedColor(ch, 0xC0C000);
            case 'H':
            case 'Y':
                return getDefinedColor(ch, 0x15A4A4);
            default:
                return getDefinedColor(ch, 0x778899); // Light Slate Gray
        }
    }
}
