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

package megan.daa.io;

/**
 * translates stuff
 * Daniel Huson, 8.2015
 */
public class Translator {
    private static final byte[] reverseNucleotide = new byte[]{3, 2, 1, 0, 4};

    private static final byte[][][] lookup = {
            {{11, 2, 11, 2, 23},
                    {16, 16, 16, 16, 16},
                    {1, 15, 1, 15, 23},
                    {9, 9, 12, 9, 23},
                    {23, 23, 23, 23, 23},
            },
            {{5, 8, 5, 8, 23},
                    {14, 14, 14, 14, 14},
                    {1, 1, 1, 1, 1},
                    {10, 10, 10, 10, 10},
                    {23, 23, 23, 23, 23},
            },
            {{6, 3, 6, 3, 23},
                    {0, 0, 0, 0, 0},
                    {7, 7, 7, 7, 7},
                    {19, 19, 19, 19, 19},
                    {23, 23, 23, 23, 23},
            },
            {{23, 18, 23, 18, 23},
                    {15, 15, 15, 15, 15},
                    {23, 4, 17, 4, 23},
                    {10, 13, 10, 13, 23},
                    {23, 23, 23, 23, 23},
            },
            {{23, 23, 23, 23, 23},
                    {23, 23, 23, 23, 23},
                    {23, 23, 23, 23, 23},
                    {23, 23, 23, 23, 23},
                    {23, 23, 23, 23, 23},
            }};

    private static final byte[][][] lookupReverse = {
            {{13, 10, 13, 10, 23},
                    {4, 17, 4, 23, 23},
                    {15, 15, 15, 15, 15},
                    {18, 23, 18, 23, 23},
                    {23, 23, 23, 23, 23},
            },
            {{19, 19, 19, 19, 19},
                    {7, 7, 7, 7, 7},
                    {0, 0, 0, 0, 0},
                    {3, 6, 3, 6, 23},
                    {23, 23, 23, 23, 23},
            },
            {{10, 10, 10, 10, 10},
                    {1, 1, 1, 1, 1},
                    {14, 14, 14, 14, 14},
                    {8, 5, 8, 5, 23},
                    {23, 23, 23, 23, 23},
            },
            {{9, 12, 9, 9, 23},
                    {15, 1, 15, 1, 23},
                    {16, 16, 16, 16, 16},
                    {2, 11, 2, 11, 23},
                    {23, 23, 23, 23, 23},
            },
            {{23, 23, 23, 23, 23},
                    {23, 23, 23, 23, 23},
                    {23, 23, 23, 23, 23},
                    {23, 23, 23, 23, 23},
                    {23, 23, 23, 23, 23},
            }};

    public static final byte[] DNA_ALPHABET = "ACGTN".getBytes();
    public static final byte[] AMINO_ACID_ALPHABET = "ARNDCQEGHILKMFPSTWYVBJZX*/\\".getBytes(); // last two must be: / and then \

    public static final byte FORWARD_SHIFT_CODE = (byte) (AMINO_ACID_ALPHABET.length - 1); // last letter: backslash
    public static final byte REVERSE_SHIFT_CODE = (byte) (AMINO_ACID_ALPHABET.length - 2); // second-to-last letter: forward slash

    /**
     * get reversed-complemented DNA
     *
     * @param dna
     * @return reverse component
     */
    public static byte[] getReverseComplement(byte[] dna) {
        byte[] reverse = new byte[dna.length];

        int j = dna.length - 1;
        for (byte a : dna) {
            reverse[j--] = reverseNucleotide[dna[a]];
        }
        return reverse;
    }

    /**
     * get reversed-complemented DNA
     *
     * @param dna
     * @return reverse component
     */
    public static byte[] getReverseComplement(byte[] dna, int offset, int length) {
        byte[] reverse = new byte[length];

        int j = offset + length - 1;
        for (int i = 0; i < length; i++) {
            reverse[i] = reverseNucleotide[dna[j--]];
        }
        return reverse;
    }

    /**
     * get six frame translations
     *
     * @param dnaSequence
     * @return six frame translations
     */
    public static byte[][] getSixFrameTranslations(byte[] dnaSequence) {
        byte[][] proteins = new byte[6][];

        int length = dnaSequence.length;
        int d = length / 3;
        proteins[0] = new byte[d];
        proteins[3] = new byte[d];
        d = (length - 1) / 3;
        proteins[1] = new byte[d];
        proteins[4] = new byte[d];
        d = (length - 2) / 3;
        proteins[2] = new byte[d];
        proteins[5] = new byte[d];

        int r = length - 2;
        int pos = 0;
        int i = 0;
        while (r > 2) {
            proteins[0][i] = getAminoAcid(dnaSequence, pos++);
            proteins[3][i] = getAminoAcidReverse(dnaSequence, --r);
            proteins[1][i] = getAminoAcid(dnaSequence, pos++);
            proteins[4][i] = getAminoAcidReverse(dnaSequence, --r);
            proteins[2][i] = getAminoAcid(dnaSequence, pos++);
            proteins[5][i] = getAminoAcidReverse(dnaSequence, --r);
            ++i;
        }
        if (r > 0) {
            proteins[0][i] = getAminoAcid(dnaSequence, pos++);
            proteins[3][i] = getAminoAcidReverse(dnaSequence, --r);
        }
        if (r > 0) {
            proteins[1][i] = getAminoAcid(dnaSequence, pos);
            proteins[4][i] = getAminoAcidReverse(dnaSequence, r);
        }
        return proteins;
    }

    static public byte getAminoAcid(byte[] dnaSequence, int pos) {
        return lookup[(int) dnaSequence[pos]][(int) dnaSequence[pos + 1]][(int) dnaSequence[pos + 2]];
    }

    private static byte getAminoAcidReverse(byte[] dnaSequence, int pos) {
        return lookupReverse[(int) dnaSequence[pos + 2]][(int) dnaSequence[pos + 1]][(int) dnaSequence[pos]];
    }

    /**
     * decode sequence to nucleotides or amino acids
     *
     * @param sequence
     * @param alphabet
     * @return decoded sequence
     */
    public static byte[] translate(byte[] sequence, byte[] alphabet, int offset, int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++)
            result[i] = alphabet[sequence[i + offset]];
        return result;
    }

    /**
     * decode sequence to nucleotides or amino acids
     *
     * @param sequence
     * @param alphabet
     * @return decoded sequence
     */
    public static byte[] translate(byte[] sequence, byte[] alphabet) {
        return translate(sequence, alphabet, 0, sequence.length);
    }
}
