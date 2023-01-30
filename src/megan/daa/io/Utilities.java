/*
 * Utilities.java Copyright (C) 2023 Daniel H. Huson
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

package megan.daa.io;

/**
 * basic utilities
 * Daniel Huson, 8.2015
 */
public class Utilities {
    /**
     * determines whether two byte arrays are equalOverShorterOfBoth over the whole minimum of their two lengths
     *
     * @return true, if shared indices have same value
     */
    public static boolean equalOverShorterOfBoth(byte[] a, byte[] b) {
        int top = Math.min(a.length, b.length);

        for (int i = 0; i < top; i++) {
            if (a[i] != b[i])
                return false;
        }
        return true;
    }

    /**
     * copies the src to the target, resizing the target, if necessary
     *
     * @return result, possibly resized
     */
    public static byte[] copy(byte[] src, byte[] target) {
        if (target.length < src.length) {
            target = new byte[src.length];
        }
        System.arraycopy(src, 0, target, 0, src.length);
        return target;
    }

    /**
     * computes percent identity
     *
     * @return percent identity
     */
    public static int computePercentIdentity(DAAMatchRecord match) {
        return match.getIdentities() * 100 / match.getLen();
    }
}
