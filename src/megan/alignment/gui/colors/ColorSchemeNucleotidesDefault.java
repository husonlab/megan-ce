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
public class ColorSchemeNucleotidesDefault extends ColorSchemeBase implements IColorScheme {
    /**
     * gets the background color for the amino-acid
     *
     * @param ch
     * @return color
     */
    public Color getBackground(int ch) {
        switch (ch) {
            case 'a':
            case 'A':
                return getDefinedColor(ch, 0x64F73F);   // green
            case 'c':
            case 'C':
                return getDefinedColor(ch, 0xFFB340);   // orange
            case 'g':
            case 'G':
                return getDefinedColor(ch, 0xEB413C); // red
            case 't':
            case 'T':
            case 'u':
            case 'U':
                return getDefinedColor(ch, 0x3C88EE);  // blue
            case '-':
                return getDefinedColor(ch, 0x778899); // Light Slate Gray
            default:
                return Color.LIGHT_GRAY;
        }
    }
}
