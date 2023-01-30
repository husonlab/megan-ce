/*
 * ColorSchemeNucleotidesDefault.java Copyright (C) 2023 Daniel H. Huson
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
package megan.alignment.gui.colors;

import java.awt.*;

/**
 * amino acid color scheme
 * Daniel Huson, 9.2011
 */
public class ColorSchemeNucleotidesDefault extends ColorSchemeBase implements IColorScheme {
    /**
     * gets the background color
     *
     * @return color
     */
    public Color getBackground(int ch) {
        return switch (ch) {
            case 'a', 'A' -> getDefinedColor(ch, 0x64F73F);   // green
            case 'c', 'C' -> getDefinedColor(ch, 0xFFB340);   // orange
            case 'g', 'G' -> getDefinedColor(ch, 0xEB413C); // red
            case 't', 'T', 'u', 'U' -> getDefinedColor(ch, 0x3C88EE);  // blue
            case '-' -> getDefinedColor(ch, 0x778899); // Light Slate Gray
            default -> Color.LIGHT_GRAY;
        };
    }
}
