/*
 * ColorSchemeAminoAcidsPDNA.java Copyright (C) 2023 Daniel H. Huson
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
public class ColorSchemeAminoAcidsPDNA extends ColorSchemeBase implements IColorScheme {
    /**
     * gets the background color for the amino-acid
     * PDNA groups: LVIMC AGSTP FYW EDNQKRH
     *
     * @return color
     */
    public Color getBackground(int ch) {
        ch = Character.toUpperCase(ch);
        return switch (ch) {
            case 'I', 'L', 'V', 'M', 'C' -> getDefinedColor(ch, 0x15C015);   //green
            case 'A', 'G', 'S', 'T', 'P' -> getDefinedColor(ch, 0xF09048); // orange
            case 'F', 'Y', 'W' -> getDefinedColor(ch, 0x80A0F0); // blue
            case 'R', 'N', 'D', 'Q', 'E', 'H', 'K' -> getDefinedColor(ch, 0xF01505); // red
            default -> getDefinedColor(ch, 0x778899); // Light Slate Gray
        };
    }
}
