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

package megan.util;

import java.awt.*;

/**
 * Some utilities for colors
 * Daniel Huson, 12.2015
 */
class ColorUtilities {

    /**
     * interpolate colors
     * Source: http://stackoverflow.com/questions/340209/generate-colors-between-red-and-green-for-a-power-meter
     *
     * @param start
     * @param end
     * @param p     number between 0 and 1
     * @return color
     */
    public static Color interpolateHSB(Color start, Color end, float p) {
        float[] startHSB = Color.RGBtoHSB(start.getRed(), start.getGreen(), start.getBlue(), null);
        float[] endHSB = Color.RGBtoHSB(end.getRed(), end.getGreen(), end.getBlue(), null);

        float brightness = (startHSB[2] + endHSB[2]) / 2;
        float saturation = (startHSB[1] + endHSB[1]) / 2;

        float hueMax;
        float hueMin;
        if (startHSB[0] > endHSB[0]) {
            hueMax = startHSB[0];
            hueMin = endHSB[0];
        } else {
            hueMin = startHSB[0];
            hueMax = endHSB[0];
        }

        float hue = ((hueMax - hueMin) * p) + hueMin;

        return Color.getHSBColor(hue, saturation, brightness);
    }

    public static Color interpolateRGB(Color start, Color end, float p) {

        if (p <= 0.5) {
            final float a = 2 * (0.5f - p);
            final float b = (int) (2f * p * 255f);
            return new Color(
                    (int) (a * start.getRed() + b),
                    (int) (a * start.getGreen() + b),
                    (int) (a * start.getBlue() + b),
                    (start.getAlpha() + end.getAlpha()) / 2);
        } else {
            final float a = 2 * (p - 0.5f);
            final float b = (int) (2f * (1f - p) * 255f);
            return new Color(
                    (int) (a * end.getRed() + b),
                    (int) (a * end.getGreen() + b),
                    (int) (a * end.getBlue() + b),
                    (start.getAlpha() + end.getAlpha()) / 2);
        }




/*
        final float a=(p>=0.5?1:1-2*p); // p=0: a=1, p>=0.5: a=0
        final float b=(p<=0.5?1:2*(p-0.5f)); // p<=0.5: b=0, p==1: b=1

        return new Color((int)(a*start.getRed()+b*end.getRed()),
                (int)(a*start.getGreen()+b*end.getGreen()),
                (int)(a*start.getBlue()+b*end.getBlue()),
               (start.getAlpha()+end.getAlpha())/2);
               */
    }
}
