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
package megan.viewer.gui;

import jloda.swing.util.ColorTable;

import java.awt.*;

/**
 * Color gradient
 * Daniel Huson, 5.2017
 */
public class ColorGradient {
    static private final Color separatorColor = new Color(200, 255, 200);

    private final ColorTable colorTable;
    private final int maxCount;
    private final double inverseLogCount;
    private final double inverseSqrtCount;

    /**
     * constructor
     *
     * @param colorTable
     * @param maxCount
     */
    public ColorGradient(ColorTable colorTable, int maxCount) {
        this.colorTable = colorTable;
        this.maxCount = maxCount;
        inverseLogCount = 1.0 / Math.log(maxCount + 1);
        if (maxCount > 0)
            inverseSqrtCount = 1.0 / Math.sqrt(maxCount);
        else
            inverseSqrtCount = 1;
    }

    /**
     * get color on linear scale
     *
     * @param count
     * @return color
     */
    public Color getColor(int count) {
        if (maxCount == 0)
            return Color.WHITE;
        return colorTable.getColor(count, maxCount);
    }

    /**
     * get sqrt scale color
     *
     * @param count
     * @return color
     */
    public Color getColorSqrtScale(int count) {
        if (maxCount == 0)
            return Color.WHITE;
        return colorTable.getColorSqrtScale(count, inverseSqrtCount);
    }

    /**
     * get color on log scale
     *
     * @param count
     * @return color
     */
    public Color getColorLogScale(int count) {
        if (maxCount == 0)
            return Color.WHITE;
        return colorTable.getColorLogScale(count, inverseLogCount);
    }

    /**
     * gets the max count
     *
     * @return max count
     */
    public int getMaxCount() {
        return maxCount;
    }


    /**
     * get border color used to separate different regions
     *
     * @return separator color
     */
    public Color getSeparatorColor() {
        return separatorColor;
    }
}
