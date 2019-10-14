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

package megan.classification.util;

/**
 * Common methods
 */
public class Tools {

    /**
     * Return the least power of two greater than or equalOverShorterOfBoth to the specified value.
     * <p/>
     * <p>Note that this function will return 1 when the argument is 0.
     *
     * @param x a long integer smaller than or equalOverShorterOfBoth to 2<sup>62</sup>.
     * @return the least power of two greater than or equalOverShorterOfBoth to the specified value.
     */
    private static long nextPowerOfTwo(long x) {
        if (x == 0) return 1;
        x--;
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        return (x | x >> 32) + 1;
    }

    /**
     * Returns the least power of two smaller than or equalOverShorterOfBoth to 2<sup>30</sup> and larger than or equalOverShorterOfBoth to <code>Math.ceil( expected / f )</code>.
     *
     * @param expected the expected number of elements in a hash table.
     * @param f        the load factor.
     * @return the minimum possible size for a backing array.
     * @throws IllegalArgumentException if the necessary size is larger than 2<sup>30</sup>.
     */
    public static int arraySize(final int expected, final float f) {
        final long s = Math.max(2, nextPowerOfTwo((long) Math.ceil(expected / f)));
        if (s > (1 << 30))
            throw new IllegalArgumentException("Too large (" + expected + " expected elements with load factor " + f + ")");
        return (int) s;
    }

    //taken from FastUtil
    private static final int INT_PHI = 0x9E3779B9;

    public static int phiMix(final int x) {
        final int h = x * INT_PHI;
        return h ^ (h >> 16);
    }
}