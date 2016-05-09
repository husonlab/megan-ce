/*
 *  Copyright (C) 2016 Daniel H. Huson
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
public class ColorSchemeAminoAcidsDefault extends ColorSchemeBase implements IColorScheme {
    /**
     * gets the background color for the amino-acid
     *
     * @param ch
     * @return color
     */
    public Color getBackground(int ch) {
        ch = Character.toUpperCase(ch);
        switch (ch) {
            case 'A':
                return getDefinedColor(ch, 0xeee9e9);
            case 'R':
                return getDefinedColor(ch, 0x145AFF);
            case 'N':
                return getDefinedColor(ch, 0x00DCDC);
            case 'D':
                return getDefinedColor(ch, 0xE60A0A);
            case 'C':
                return getDefinedColor(ch, 0xE6E600);
            case 'Q':
                return getDefinedColor(ch, 0x00DCDC);
            case 'E':
                return getDefinedColor(ch, 0xE60A0A);
            case 'G':
                return getDefinedColor(ch, 0xEBEBEB);
            case 'H':
                return getDefinedColor(ch, 0x8282D2);
            case 'I':
                return getDefinedColor(ch, 0x0F820F);
            case 'L':
                return getDefinedColor(ch, 0x0F820F);
            case 'K':
                return getDefinedColor(ch, 0x145AFF);
            case 'M':
                return getDefinedColor(ch, 0xE6E600);
            case 'F':
                return getDefinedColor(ch, 0x3232AA);
            case 'P':
                return getDefinedColor(ch, 0xDC9682);
            case 'S':
                return getDefinedColor(ch, 0xFA9600);
            case 'T':
                return getDefinedColor(ch, 0xFA9600);
            case 'W':
                return getDefinedColor(ch, 0xB45AB4);
            case 'Y':
                return getDefinedColor(ch, 0x3232AA);
            case 'V':
                return getDefinedColor(ch, 0x0F820F);
            default:
                return getDefinedColor(ch, 0x778899); // Light Slate Gray
        }
    }
}
