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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A blosum matrix
 * Daniel Huson, 11.2011
 */
public class BlosumMatrix {
    private final int[][] matrix;

    private static BlosumMatrix BLOSUM90;
    private static BlosumMatrix BLOSUM80;
    private static BlosumMatrix BLOSUM62;
    private static BlosumMatrix BLOSUM50;
    private static BlosumMatrix BLOSUM45;

    private final static String BLOSUM90_INPUT =
            "A  R  N  D  C  Q  E  G  H  I  L  K  M  F  P  S  T  W  Y  V  B  J  Z  X  * \n" +
                    "A  5 -2 -2 -3 -1 -1 -1  0 -2 -2 -2 -1 -2 -3 -1  1  0 -4 -3 -1 -2 -2 -1 -1 -6 \n" +
                    "R -2  6 -1 -3 -5  1 -1 -3  0 -4 -3  2 -2 -4 -3 -1 -2 -4 -3 -3 -2 -3  0 -1 -6 \n" +
                    "N -2 -1  7  1 -4  0 -1 -1  0 -4 -4  0 -3 -4 -3  0  0 -5 -3 -4  5 -4 -1 -1 -6 \n" +
                    "D -3 -3  1  7 -5 -1  1 -2 -2 -5 -5 -1 -4 -5 -3 -1 -2 -6 -4 -5  5 -5  1 -1 -6 \n" +
                    "C -1 -5 -4 -5  9 -4 -6 -4 -5 -2 -2 -4 -2 -3 -4 -2 -2 -4 -4 -2 -4 -2 -5 -1 -6 \n" +
                    "Q -1  1  0 -1 -4  7  2 -3  1 -4 -3  1  0 -4 -2 -1 -1 -3 -3 -3 -1 -3  5 -1 -6 \n" +
                    "E -1 -1 -1  1 -6  2  6 -3 -1 -4 -4  0 -3 -5 -2 -1 -1 -5 -4 -3  1 -4  5 -1 -6 \n" +
                    "G  0 -3 -1 -2 -4 -3 -3  6 -3 -5 -5 -2 -4 -5 -3 -1 -3 -4 -5 -5 -2 -5 -3 -1 -6 \n" +
                    "H -2  0  0 -2 -5  1 -1 -3  8 -4 -4 -1 -3 -2 -3 -2 -2 -3  1 -4 -1 -4  0 -1 -6 \n" +
                    "I -2 -4 -4 -5 -2 -4 -4 -5 -4  5  1 -4  1 -1 -4 -3 -1 -4 -2  3 -5  3 -4 -1 -6 \n" +
                    "L -2 -3 -4 -5 -2 -3 -4 -5 -4  1  5 -3  2  0 -4 -3 -2 -3 -2  0 -5  4 -4 -1 -6 \n" +
                    "K -1  2  0 -1 -4  1  0 -2 -1 -4 -3  6 -2 -4 -2 -1 -1 -5 -3 -3 -1 -3  1 -1 -6 \n" +
                    "M -2 -2 -3 -4 -2  0 -3 -4 -3  1  2 -2  7 -1 -3 -2 -1 -2 -2  0 -4  2 -2 -1 -6 \n" +
                    "F -3 -4 -4 -5 -3 -4 -5 -5 -2 -1  0 -4 -1  7 -4 -3 -3  0  3 -2 -4  0 -4 -1 -6 \n" +
                    "P -1 -3 -3 -3 -4 -2 -2 -3 -3 -4 -4 -2 -3 -4  8 -2 -2 -5 -4 -3 -3 -4 -2 -1 -6 \n" +
                    "S  1 -1  0 -1 -2 -1 -1 -1 -2 -3 -3 -1 -2 -3 -2  5  1 -4 -3 -2  0 -3 -1 -1 -6 \n" +
                    "T  0 -2  0 -2 -2 -1 -1 -3 -2 -1 -2 -1 -1 -3 -2  1  6 -4 -2 -1 -1 -2 -1 -1 -6 \n" +
                    "W -4 -4 -5 -6 -4 -3 -5 -4 -3 -4 -3 -5 -2  0 -5 -4 -4 11  2 -3 -6 -3 -4 -1 -6 \n" +
                    "Y -3 -3 -3 -4 -4 -3 -4 -5  1 -2 -2 -3 -2  3 -4 -3 -2  2  8 -3 -4 -2 -3 -1 -6 \n" +
                    "V -1 -3 -4 -5 -2 -3 -3 -5 -4  3  0 -3  0 -2 -3 -2 -1 -3 -3  5 -4  1 -3 -1 -6 \n" +
                    "B -2 -2  5  5 -4 -1  1 -2 -1 -5 -5 -1 -4 -4 -3  0 -1 -6 -4 -4  5 -5  0 -1 -6 \n" +
                    "J -2 -3 -4 -5 -2 -3 -4 -5 -4  3  4 -3  2  0 -4 -3 -2 -3 -2  1 -5  4 -4 -1 -6 \n" +
                    "Z -1  0 -1  1 -5  5  5 -3  0 -4 -4  1 -2 -4 -2 -1 -1 -4 -3 -3  0 -4  5 -1 -6 \n" +
                    "X -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -6 \n" +
                    "* -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6  1 \n";

    private final static String BLOSUM80_INPUT =
            "A  R  N  D  C  Q  E  G  H  I  L  K  M  F  P  S  T  W  Y  V  B  J  Z  X  * \n" +
                    "A  5 -2 -2 -2 -1 -1 -1  0 -2 -2 -2 -1 -1 -3 -1  1  0 -3 -2  0 -2 -2 -1 -1 -6 \n" +
                    "R -2  6 -1 -2 -4  1 -1 -3  0 -3 -3  2 -2 -4 -2 -1 -1 -4 -3 -3 -1 -3  0 -1 -6 \n" +
                    "N -2 -1  6  1 -3  0 -1 -1  0 -4 -4  0 -3 -4 -3  0  0 -4 -3 -4  5 -4  0 -1 -6 \n" +
                    "D -2 -2  1  6 -4 -1  1 -2 -2 -4 -5 -1 -4 -4 -2 -1 -1 -6 -4 -4  5 -5  1 -1 -6 \n" +
                    "C -1 -4 -3 -4  9 -4 -5 -4 -4 -2 -2 -4 -2 -3 -4 -2 -1 -3 -3 -1 -4 -2 -4 -1 -6 \n" +
                    "Q -1  1  0 -1 -4  6  2 -2  1 -3 -3  1  0 -4 -2  0 -1 -3 -2 -3  0 -3  4 -1 -6 \n" +
                    "E -1 -1 -1  1 -5  2  6 -3  0 -4 -4  1 -2 -4 -2  0 -1 -4 -3 -3  1 -4  5 -1 -6 \n" +
                    "G  0 -3 -1 -2 -4 -2 -3  6 -3 -5 -4 -2 -4 -4 -3 -1 -2 -4 -4 -4 -1 -5 -3 -1 -6 \n" +
                    "H -2  0  0 -2 -4  1  0 -3  8 -4 -3 -1 -2 -2 -3 -1 -2 -3  2 -4 -1 -4  0 -1 -6 \n" +
                    "I -2 -3 -4 -4 -2 -3 -4 -5 -4  5  1 -3  1 -1 -4 -3 -1 -3 -2  3 -4  3 -4 -1 -6 \n" +
                    "L -2 -3 -4 -5 -2 -3 -4 -4 -3  1  4 -3  2  0 -3 -3 -2 -2 -2  1 -4  3 -3 -1 -6 \n" +
                    "K -1  2  0 -1 -4  1  1 -2 -1 -3 -3  5 -2 -4 -1 -1 -1 -4 -3 -3 -1 -3  1 -1 -6 \n" +
                    "M -1 -2 -3 -4 -2  0 -2 -4 -2  1  2 -2  6  0 -3 -2 -1 -2 -2  1 -3  2 -1 -1 -6 \n" +
                    "F -3 -4 -4 -4 -3 -4 -4 -4 -2 -1  0 -4  0  6 -4 -3 -2  0  3 -1 -4  0 -4 -1 -6 \n" +
                    "P -1 -2 -3 -2 -4 -2 -2 -3 -3 -4 -3 -1 -3 -4  8 -1 -2 -5 -4 -3 -2 -4 -2 -1 -6 \n" +
                    "S  1 -1  0 -1 -2  0  0 -1 -1 -3 -3 -1 -2 -3 -1  5  1 -4 -2 -2  0 -3  0 -1 -6 \n" +
                    "T  0 -1  0 -1 -1 -1 -1 -2 -2 -1 -2 -1 -1 -2 -2  1  5 -4 -2  0 -1 -1 -1 -1 -6 \n" +
                    "W -3 -4 -4 -6 -3 -3 -4 -4 -3 -3 -2 -4 -2  0 -5 -4 -4 11  2 -3 -5 -3 -3 -1 -6 \n" +
                    "Y -2 -3 -3 -4 -3 -2 -3 -4  2 -2 -2 -3 -2  3 -4 -2 -2  2  7 -2 -3 -2 -3 -1 -6 \n" +
                    "V  0 -3 -4 -4 -1 -3 -3 -4 -4  3  1 -3  1 -1 -3 -2  0 -3 -2  4 -4  2 -3 -1 -6 \n" +
                    "B -2 -1  5  5 -4  0  1 -1 -1 -4 -4 -1 -3 -4 -2  0 -1 -5 -3 -4  5 -4  0 -1 -6 \n" +
                    "J -2 -3 -4 -5 -2 -3 -4 -5 -4  3  3 -3  2  0 -4 -3 -1 -3 -2  2 -4  3 -3 -1 -6 \n" +
                    "Z -1  0  0  1 -4  4  5 -3  0 -4 -3  1 -1 -4 -2  0 -1 -3 -3 -3  0 -3  5 -1 -6 \n" +
                    "X -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -6 \n" +
                    "* -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6 -6  1 \n";

    private final static String BLOSUM62_INPUT =
            "A R N D C Q E G H I L K M F P S T W Y V B Z X *\n" +
                    "A 4 -1 -2 -2 0 -1 -1 0 -2 -1 -1 -1 -1 -2 -1 1 0 -3 -2 0 -2 -1 0 -4 \n" +
                    "R -1 5 0 -2 -3 1 0 -2 0 -3 -2 2 -1 -3 -2 -1 -1 -3 -2 -3 -1 0 -1 -4 \n" +
                    "N -2 0 6 1 -3 0 0 0 1 -3 -3 0 -2 -3 -2 1 0 -4 -2 -3 3 0 -1 -4 \n" +
                    "D -2 -2 1 6 -3 0 2 -1 -1 -3 -4 -1 -3 -3 -1 0 -1 -4 -3 -3 4 1 -1 -4 \n" +
                    "C 0 -3 -3 -3 9 -3 -4 -3 -3 -1 -1 -3 -1 -2 -3 -1 -1 -2 -2 -1 -3 -3 -2 -4 \n" +
                    "Q -1 1 0 0 -3 5 2 -2 0 -3 -2 1 0 -3 -1 0 -1 -2 -1 -2 0 3 -1 -4 \n" +
                    "E -1 0 0 2 -4 2 5 -2 0 -3 -3 1 -2 -3 -1 0 -1 -3 -2 -2 1 4 -1 -4 \n" +
                    "G 0 -2 0 -1 -3 -2 -2 6 -2 -4 -4 -2 -3 -3 -2 0 -2 -2 -3 -3 -1 -2 -1 -4 \n" +
                    "H -2 0 1 -1 -3 0 0 -2 8 -3 -3 -1 -2 -1 -2 -1 -2 -2 2 -3 0 0 -1 -4 \n" +
                    "I -1 -3 -3 -3 -1 -3 -3 -4 -3 4 2 -3 1 0 -3 -2 -1 -3 -1 3 -3 -3 -1 -4 \n" +
                    "L -1 -2 -3 -4 -1 -2 -3 -4 -3 2 4 -2 2 0 -3 -2 -1 -2 -1 1 -4 -3 -1 -4 \n" +
                    "K -1 2 0 -1 -3 1 1 -2 -1 -3 -2 5 -1 -3 -1 0 -1 -3 -2 -2 0 1 -1 -4 \n" +
                    "M -1 -1 -2 -3 -1 0 -2 -3 -2 1 2 -1 5 0 -2 -1 -1 -1 -1 1 -3 -1 -1 -4 \n" +
                    "F -2 -3 -3 -3 -2 -3 -3 -3 -1 0 0 -3 0 6 -4 -2 -2 1 3 -1 -3 -3 -1 -4 \n" +
                    "P -1 -2 -2 -1 -3 -1 -1 -2 -2 -3 -3 -1 -2 -4 7 -1 -1 -4 -3 -2 -2 -1 -2 -4 \n" +
                    "S 1 -1 1 0 -1 0 0 0 -1 -2 -2 0 -1 -2 -1 4 1 -3 -2 -2 0 0 0 -4 \n" +
                    "T 0 -1 0 -1 -1 -1 -1 -2 -2 -1 -1 -1 -1 -2 -1 1 5 -2 -2 0 -1 -1 0 -4 \n" +
                    "W -3 -3 -4 -4 -2 -2 -3 -2 -2 -3 -2 -3 -1 1 -4 -3 -2 11 2 -3 -4 -3 -2 -4 \n" +
                    "Y -2 -2 -2 -3 -2 -1 -2 -3 2 -1 -1 -2 -1 3 -3 -2 -2 2 7 -1 -3 -2 -1 -4 \n" +
                    "V 0 -3 -3 -3 -1 -2 -2 -3 -3 3 1 -2 1 -1 -2 -2 0 -3 -1 4 -3 -2 -1 -4 \n" +
                    "B -2 -1 3 4 -3 0 1 -1 0 -3 -4 0 -3 -3 -2 0 -1 -4 -3 -3 4 1 -1 -4 \n" +
                    "Z -1 0 0 1 -3 3 4 -2 0 -3 -3 1 -1 -3 -1 0 -1 -3 -2 -2 1 4 -1 -4 \n" +
                    "X 0 -1 -1 -1 -2 -1 -1 -1 -1 -1 -1 -1 -1 -1 -2 0 0 -2 -1 -1 -1 -1 -1 -4 \n" +
                    "* -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 1 \n";

    private final static String BLOSUM50_INPUT =
            "A  R  N  D  C  Q  E  G  H  I  L  K  M  F  P  S  T  W  Y  V  B  Z  X  * \n" +
                    "A 5 -2 -1 -2 -1 -1 -1  0 -2 -1 -2 -1 -1 -3 -1  1  0 -3 -2  0 -2 -1 -1 -5  \n" +
                    "R -2  7 -1 -2 -4  1  0 -3  0 -4 -3  3 -2 -3 -3 -1 -1 -3 -1 -3 -1  0 -1 -5  \n" +
                    "N -1 -1  7  2 -2  0  0  0  1 -3 -4  0 -2 -4 -2  1  0 -4 -2 -3  4  0 -1 -5  \n" +
                    "D -2 -2  2  8 -4  0  2 -1 -1 -4 -4 -1 -4 -5 -1  0 -1 -5 -3 -4  5  1 -1 -5  \n" +
                    "C -1 -4 -2 -4 13 -3 -3 -3 -3 -2 -2 -3 -2 -2 -4 -1 -1 -5 -3 -1 -3 -3 -2 -5  \n" +
                    "Q -1  1  0  0 -3  7  2 -2  1 -3 -2  2  0 -4 -1  0 -1 -1 -1 -3  0  4 -1 -5  \n" +
                    "E -1  0  0  2 -3  2  6 -3  0 -4 -3  1 -2 -3 -1 -1 -1 -3 -2 -3  1  5 -1 -5  \n" +
                    "G 0 -3  0 -1 -3 -2 -3  8 -2 -4 -4 -2 -3 -4 -2  0 -2 -3 -3 -4 -1 -2 -2 -5  \n" +
                    "H -2  0  1 -1 -3  1  0 -2 10 -4 -3  0 -1 -1 -2 -1 -2 -3  2 -4  0  0 -1 -5  \n" +
                    "I -1 -4 -3 -4 -2 -3 -4 -4 -4  5  2 -3  2  0 -3 -3 -1 -3 -1  4 -4 -3 -1 -5  \n" +
                    "L -2 -3 -4 -4 -2 -2 -3 -4 -3  2  5 -3  3  1 -4 -3 -1 -2 -1  1 -4 -3 -1 -5  \n" +
                    "K -1  3  0 -1 -3  2  1 -2  0 -3 -3  6 -2 -4 -1  0 -1 -3 -2 -3  0  1 -1 -5  \n" +
                    "M -1 -2 -2 -4 -2  0 -2 -3 -1  2  3 -2  7  0 -3 -2 -1 -1  0  1 -3 -1 -1 -5  \n" +
                    "F -3 -3 -4 -5 -2 -4 -3 -4 -1  0  1 -4  0  8 -4 -3 -2  1  4 -1 -4 -4 -2 -5  \n" +
                    "P -1 -3 -2 -1 -4 -1 -1 -2 -2 -3 -4 -1 -3 -4 10 -1 -1 -4 -3 -3 -2 -1 -2 -5  \n" +
                    "S 1 -1  1  0 -1  0 -1  0 -1 -3 -3  0 -2 -3 -1  5  2 -4 -2 -2  0  0 -1 -5  \n" +
                    "T 0 -1  0 -1 -1 -1 -1 -2 -2 -1 -1 -1 -1 -2 -1  2  5 -3 -2  0  0 -1  0 -5  \n" +
                    "W -3 -3 -4 -5 -5 -1 -3 -3 -3 -3 -2 -3 -1  1 -4 -4 -3 15  2 -3 -5 -2 -3 -5  \n" +
                    "Y -2 -1 -2 -3 -3 -1 -2 -3  2 -1 -1 -2  0  4 -3 -2 -2  2  8 -1 -3 -2 -1 -5  \n" +
                    "V 0 -3 -3 -4 -1 -3 -3 -4 -4  4  1 -3  1 -1 -3 -2  0 -3 -1  5 -4 -3 -1 -5  \n" +
                    "B -2 -1  4  5 -3  0  1 -1  0 -4 -4  0 -3 -4 -2  0  0 -5 -3 -4  5  2 -1 -5  \n" +
                    "Z -1  0  0  1 -3  4  5 -2  0 -3 -3  1 -1 -4 -1  0 -1 -2 -2 -3  2  5 -1 -5  \n" +
                    "X -1 -1 -1 -1 -2 -1 -1 -2 -1 -1 -1 -1 -1 -2 -2 -1  0 -3 -1 -1 -1 -1 -1 -5  \n" +
                    "* -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5  1  \n";

    private final static String BLOSUM45_INPUT =
            "A  R  N  D  C  Q  E  G  H  I  L  K  M  F  P  S  T  W  Y  V  B  J  Z  X  * \n" +
                    "A  5 -2 -1 -2 -1 -1 -1  0 -2 -1 -1 -1 -1 -2 -1  1  0 -2 -2  0 -1 -1 -1 -1 -5 \n" +
                    "R -2  7  0 -1 -3  1  0 -2  0 -3 -2  3 -1 -2 -2 -1 -1 -2 -1 -2 -1 -3  1 -1 -5 \n" +
                    "N -1  0  6  2 -2  0  0  0  1 -2 -3  0 -2 -2 -2  1  0 -4 -2 -3  5 -3  0 -1 -5 \n" +
                    "D -2 -1  2  7 -3  0  2 -1  0 -4 -3  0 -3 -4 -1  0 -1 -4 -2 -3  6 -3  1 -1 -5 \n" +
                    "C -1 -3 -2 -3 12 -3 -3 -3 -3 -3 -2 -3 -2 -2 -4 -1 -1 -5 -3 -1 -2 -2 -3 -1 -5 \n" +
                    "Q -1  1  0  0 -3  6  2 -2  1 -2 -2  1  0 -4 -1  0 -1 -2 -1 -3  0 -2  4 -1 -5 \n" +
                    "E -1  0  0  2 -3  2  6 -2  0 -3 -2  1 -2 -3  0  0 -1 -3 -2 -3  1 -3  5 -1 -5 \n" +
                    "G  0 -2  0 -1 -3 -2 -2  7 -2 -4 -3 -2 -2 -3 -2  0 -2 -2 -3 -3 -1 -4 -2 -1 -5 \n" +
                    "H -2  0  1  0 -3  1  0 -2 10 -3 -2 -1  0 -2 -2 -1 -2 -3  2 -3  0 -2  0 -1 -5 \n" +
                    "I -1 -3 -2 -4 -3 -2 -3 -4 -3  5  2 -3  2  0 -2 -2 -1 -2  0  3 -3  4 -3 -1 -5 \n" +
                    "L -1 -2 -3 -3 -2 -2 -2 -3 -2  2  5 -3  2  1 -3 -3 -1 -2  0  1 -3  4 -2 -1 -5 \n" +
                    "K -1  3  0  0 -3  1  1 -2 -1 -3 -3  5 -1 -3 -1 -1 -1 -2 -1 -2  0 -3  1 -1 -5 \n" +
                    "M -1 -1 -2 -3 -2  0 -2 -2  0  2  2 -1  6  0 -2 -2 -1 -2  0  1 -2  2 -1 -1 -5 \n" +
                    "F -2 -2 -2 -4 -2 -4 -3 -3 -2  0  1 -3  0  8 -3 -2 -1  1  3  0 -3  1 -3 -1 -5 \n" +
                    "P -1 -2 -2 -1 -4 -1  0 -2 -2 -2 -3 -1 -2 -3  9 -1 -1 -3 -3 -3 -2 -3 -1 -1 -5 \n" +
                    "S  1 -1  1  0 -1  0  0  0 -1 -2 -3 -1 -2 -2 -1  4  2 -4 -2 -1  0 -2  0 -1 -5 \n" +
                    "T  0 -1  0 -1 -1 -1 -1 -2 -2 -1 -1 -1 -1 -1 -1  2  5 -3 -1  0  0 -1 -1 -1 -5 \n" +
                    "W -2 -2 -4 -4 -5 -2 -3 -2 -3 -2 -2 -2 -2  1 -3 -4 -3 15  3 -3 -4 -2 -2 -1 -5 \n" +
                    "Y -2 -1 -2 -2 -3 -1 -2 -3  2  0  0 -1  0  3 -3 -2 -1  3  8 -1 -2  0 -2 -1 -5 \n" +
                    "V  0 -2 -3 -3 -1 -3 -3 -3 -3  3  1 -2  1  0 -3 -1  0 -3 -1  5 -3  2 -3 -1 -5 \n" +
                    "B -1 -1  5  6 -2  0  1 -1  0 -3 -3  0 -2 -3 -2  0  0 -4 -2 -3  5 -3  1 -1 -5 \n" +
                    "J -1 -3 -3 -3 -2 -2 -3 -4 -2  4  4 -3  2  1 -3 -2 -1 -2  0  2 -3  4 -2 -1 -5 \n" +
                    "Z -1  1  0  1 -3  4  5 -2  0 -3 -2  1 -1 -3 -1  0 -1 -2 -2 -3  1 -2  5 -1 -5 \n" +
                    "X -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -5 \n" +
                    "* -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5 -5  1 \n";

    /**
     * constructor
     */
    public BlosumMatrix() {
        matrix = new int[128][128];
        for (int[] row : matrix) {
            Arrays.fill(row, 0, row.length, -20);
        }
    }

    /**
     * get the score for two letters
     *
     * @param a
     * @param b
     * @return score, or -20, if matrix not defined for (a,b)
     */
    public int getScore(byte a, byte b) {
        return matrix[a][b];
    }

    public static BlosumMatrix get(String which) throws IOException {
        if (which.equalsIgnoreCase("BLOSUM90"))
            return getBlosum90();
        else if (which.equalsIgnoreCase("BLOSUM80"))
            return getBlosum80();
        else if (which.equalsIgnoreCase("BLOSUM62"))
            return getBlosum62();
        else if (which.equalsIgnoreCase("BLOSUM50"))
            return getBlosum50();
        else if (which.equalsIgnoreCase("BLOSUM45"))
            return getBlosum45();
        throw new IOException("Unrecognized BLOSUM matrix: " + which);
    }

    /**
     * get the blosum 90 matrix
     *
     * @return blosum 90
     */
    private static BlosumMatrix getBlosum90() {
        try {
            if (BLOSUM90 == null) {
                BLOSUM90 = new BlosumMatrix();
                BLOSUM90.load(new StringReader(BLOSUM90_INPUT));
            }
            return BLOSUM90;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * get the blosum 80 matrix
     *
     * @return blosum 80
     */
    private static BlosumMatrix getBlosum80() {
        try {
            if (BLOSUM80 == null) {
                BLOSUM80 = new BlosumMatrix();
                BLOSUM80.load(new StringReader(BLOSUM80_INPUT));
            }
            return BLOSUM80;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * get the blosum 62 matrix
     *
     * @return blosum 62
     */
    public static BlosumMatrix getBlosum62() {
        try {
            if (BLOSUM62 == null) {
                BLOSUM62 = new BlosumMatrix();
                BLOSUM62.load(new StringReader(BLOSUM62_INPUT));
            }
            return BLOSUM62;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * get the blosum 50 matrix
     *
     * @return blosum 50
     */
    private static BlosumMatrix getBlosum50() {
        try {
            if (BLOSUM50 == null) {
                BLOSUM50 = new BlosumMatrix();
                BLOSUM50.load(new StringReader(BLOSUM50_INPUT));
            }
            return BLOSUM50;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * get the blosum 45 matrix
     *
     * @return blosum 45
     */
    private static BlosumMatrix getBlosum45() {
        try {
            if (BLOSUM45 == null) {
                BLOSUM45 = new BlosumMatrix();
                BLOSUM45.load(new StringReader(BLOSUM45_INPUT));
            }
            return BLOSUM45;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * load a matrix
     *
     * @param r0
     * @throws IOException
     */
    private void load(Reader r0) throws IOException {
        BufferedReader r = new BufferedReader(r0);

        char[] mapPos2Char = null;
        String aLine;
        int cols = 0;

        while ((aLine = r.readLine()) != null) {
            aLine = aLine.trim();
            if (aLine.length() == 0 || aLine.startsWith("#"))
                continue;
            if (mapPos2Char == null) // must be first line listing all letters
            {
                String[] tokens = aLine.split(" ");
                cols = tokens.length;
                if (tokens.length < 20)
                    throw new IOException("Expected >=20 tokens, got: " + tokens.length + " in line: " + aLine);
                List<String> list = new LinkedList<>(Arrays.asList(tokens));
                int count = 0;
                mapPos2Char = new char[list.size()];
                for (String label : list) {
                    char c = Character.toUpperCase(label.charAt(0));
                    mapPos2Char[count++] = c;
                }
            } else // a definition line
            {
                String[] tokens = aLine.split(" ");
                if (tokens.length != cols + 1)
                    throw new IOException("Expected " + (cols + 1) + " tokens, got: " + tokens.length + " in line: " + aLine);
                char c = Character.toUpperCase(tokens[0].charAt(0));
                for (int i = 1; i < tokens.length; i++) {
                    int value = Integer.parseInt(tokens[i]);
                    char d = mapPos2Char[i - 1];
                    matrix[c][d] = value;
                }
            }
        }
    }

}
