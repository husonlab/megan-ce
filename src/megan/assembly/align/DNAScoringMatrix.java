/*
 *  Copyright (C) 2016 Daniel H. Huson
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

package megan.assembly.align;

/**
 * Basic DNA scoring matrix
 * Daniel Huson, 8.2014
 */
public class DNAScoringMatrix implements IScoringMatrix {
    private final int[][] matrix = new int[128][128];

    public DNAScoringMatrix(int matchScore, int mismatchScore) {
        for (int i = 0; i < 128; i++) {
            matrix[i][i] = matchScore;
            for (int j = i + 1; j < 128; j++)
                matrix[i][j] = matrix[j][i] = mismatchScore;
        }
    }

    /**
     * get score for letters a and b
     *
     * @param a
     * @param b
     * @return score
     */
    public int getScore(byte a, byte b) {
        return matrix[a][b];
    }

    @Override
    public int[][] getMatrix() {
        return matrix;
    }
}
