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
package megan.chart.drawers;

import java.awt.*;

/**
 * a black to green color gradient
 * Daniel Huson, 10.2010
 */
public class Black2RedGradient {
    static private final Color separatorColor = new Color(150, 150, 150);
    private final int maxCount;
    private final double factor;

    /**
     * setup the green gradient
     *
     * @param maxCount
     */
    public Black2RedGradient(int maxCount) {
        this.maxCount = maxCount;
        factor = maxCount / Math.log(maxCount);
    }

    /**
     * get color on linear scale
     *
     * @param count
     * @return color
     */
    private Color getColor(int count) {
        if (maxCount == 0)
            return Color.BLACK;
        if (count > maxCount)
            count = maxCount;
        int scaled = Math.min(255, (int) Math.round(255.0 / maxCount * count));
        if (count > 0)
            scaled = Math.max(1, scaled);
        return new Color(scaled, 0, 0);
    }


    /**
     * get color on log scale
     *
     * @param count
     * @return color
     */
    public Color getLogColor(int count) {
        int value = (int) Math.max(0, (Math.round(factor * Math.log(count))));
        if (count > 0)
            value = Math.max(1, value);
        return getColor(value);
    }

    /**
     * get border color used to separate different regions
     *
     * @return separator color
     */
    public static Color getSeparatorColor() {
        return separatorColor;
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
     * this is used in the node drawer of the main viewer
     *
     * @param count
     * @param maxReads
     * @param inverLogMaxReads
     * @return color on a log scale
     */
    public static Color getColorLogScale(int count, double maxReads, double inverLogMaxReads) {
        int value = (int) (Math.round(maxReads * inverLogMaxReads * Math.log(count)));
        value = Math.max(0, Math.min(255, (int) Math.round(255.0 / maxReads * value)));
        if (count > 0)
            value = Math.max(1, value);
        return new Color(value, 0, 0);

    }

    /**
     * get color on linear scale
     *
     * @param count
     * @return color
     */
    public static Color getColor(int count, int maxCount) {
        if (maxCount == 0)
            return Color.WHITE;
        if (count > maxCount)
            count = maxCount;
        int scaled = Math.min(255, (int) Math.round(255.0 / maxCount * count));
        if (count > 0)
            scaled = Math.max(1, scaled);
        return new Color(scaled, 0, 0);
    }
}
