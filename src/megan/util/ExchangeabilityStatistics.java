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

/**
 * some simple calculations in the context of  Exchangeability Statistics
 */
class ExchangeabilityStatistics {
    /**
     * computes probility to see a split of taxa alignments along a read
     * Example:   A A A A | B B B B
     * What is that probability that exactly a alignments to taxon A are then followed by b alignments to taxon B
     *
     * @param a
     * @param b
     * @return
     */
    public static double getProbability(int a, int b) {
        return 2.0 / binomialCoefficient(a + b, b);
    }

    /**
     * compute a binomial coefficient
     *
     * @param n
     * @param k
     * @return
     */
    private static long binomialCoefficient(int n, int k) {
        double result = 1;

        for (int i = 1; i <= k; i++) {
            result *= (double) (n + 1 - i) / (double) i;
        }
        return Math.round(result);
    }
}
