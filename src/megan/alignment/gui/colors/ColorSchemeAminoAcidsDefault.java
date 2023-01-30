/*
 * ColorSchemeAminoAcidsDefault.java Copyright (C) 2023 Daniel H. Huson
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
public class ColorSchemeAminoAcidsDefault extends ColorSchemeBase implements IColorScheme {
    /**
     * gets the background color for the amino-acid
     *
     * @return color
     */
    public Color getBackground(int ch) {
        ch = Character.toUpperCase(ch);
        return switch (ch) {
            case 'A' -> getDefinedColor(ch, 0xeee9e9);
            case 'R' -> getDefinedColor(ch, 0x145AFF);
            case 'N' -> getDefinedColor(ch, 0x00DCDC);
            case 'D' -> getDefinedColor(ch, 0xE60A0A);
            case 'C' -> getDefinedColor(ch, 0xE6E600);
            case 'Q' -> getDefinedColor(ch, 0x00DCDC);
            case 'E' -> getDefinedColor(ch, 0xE60A0A);
            case 'G' -> getDefinedColor(ch, 0xEBEBEB);
            case 'H' -> getDefinedColor(ch, 0x8282D2);
            case 'I' -> getDefinedColor(ch, 0x0F820F);
            case 'L' -> getDefinedColor(ch, 0x0F820F);
            case 'K' -> getDefinedColor(ch, 0x145AFF);
            case 'M' -> getDefinedColor(ch, 0xE6E600);
            case 'F' -> getDefinedColor(ch, 0x3232AA);
            case 'P' -> getDefinedColor(ch, 0xDC9682);
            case 'S' -> getDefinedColor(ch, 0xFA9600);
            case 'T' -> getDefinedColor(ch, 0xFA9600);
            case 'W' -> getDefinedColor(ch, 0xB45AB4);
            case 'Y' -> getDefinedColor(ch, 0x3232AA);
            case 'V' -> getDefinedColor(ch, 0x0F820F);
            default -> getDefinedColor(ch, 0x778899); // Light Slate Gray
        };
    }
}
