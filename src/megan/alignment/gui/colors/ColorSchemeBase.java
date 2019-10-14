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
import java.util.HashMap;
import java.util.Map;

/**
 * based class for color scheme
 * Daniel Huson, 4.2012
 */
abstract class ColorSchemeBase {
    private final Map<Integer, Color> definedColors = new HashMap<>();

    /**
     * get the foreground color
     *
     * @param ch
     * @return black
     */
    public Color getColor(int ch) {
        return Color.BLACK;
    }

    /**
     * get the background color for a character
     *
     * @param ch
     * @return color
     */
    abstract public Color getBackground(int ch);


    /**
     * gets the amino acid color for a character
     *
     * @param ch
     * @param rgb the color to use for this amino acid
     * @return amino acid color
     */
    Color getDefinedColor(int ch, int rgb) {
        Color result = definedColors.get(ch);
        if (result == null) {
            result = new Color(rgb);
            definedColors.put(ch, result);
        }
        return result;
    }
}
