/*
 * ColorSchemeAminoAcidsZappo.java Copyright (C) 2023 Daniel H. Huson
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
public class ColorSchemeAminoAcidsZappo extends ColorSchemeBase implements IColorScheme {
    /**
     * gets the background color for the amino-acid
     * http://www.jalview.org/help.html
     *
     * @return color
     */
    public Color getBackground(int ch) {
        ch = Character.toUpperCase(ch);
        return switch (ch) {
            case 'I', 'L', 'V', 'A', 'M' -> getDefinedColor(ch, 0xFFAFAF);
            case 'F', 'Y', 'W' -> getDefinedColor(ch, 0xFFC800);
            case 'H', 'K', 'R' -> getDefinedColor(ch, 0x6464FF);
            case 'D', 'E' -> getDefinedColor(ch, 0xFF0000);
            case 'S', 'T', 'N', 'Q' -> getDefinedColor(ch, 0x00FF00);
            case 'G', 'P' -> getDefinedColor(ch, 0xFFFFFF);
            case 'C' -> getDefinedColor(ch, 0xFFFF00);
            default -> getDefinedColor(ch, 0x778899); // Light Slate Gray
        };
    }
}
