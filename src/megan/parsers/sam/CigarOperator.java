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
package megan.parsers.sam;

/**
 * The operators that can appear in a cigar string, and information about their disk representations.
 */
public enum CigarOperator {
    /**
     * Match or mismatch
     */
    M(true, true, 'M'),
    /**
     * Insertion vs. the reference.
     */
    I(true, false, 'I'),
    /**
     * Deletion vs. the reference.
     */
    D(false, true, 'D'),
    /**
     * Skipped region from the reference.
     */
    N(false, true, 'N'),
    /**
     * Soft clip.
     */
    S(true, false, 'S'),
    /**
     * Hard clip.
     */
    H(false, false, 'H'),
    /**
     * Padding.
     */
    P(false, false, 'P'),
    /**
     * Matches the reference.
     */
    EQ(true, true, '='),
    /**
     * Mismatches the reference.
     */
    X(true, true, 'X');

    // Representation of CigarOperator in BAM file
    private static final byte OP_M = 0;
    private static final byte OP_I = 1;
    private static final byte OP_D = 2;
    private static final byte OP_N = 3;
    private static final byte OP_S = 4;
    private static final byte OP_H = 5;
    private static final byte OP_P = 6;
    private static final byte OP_EQ = 7;
    private static final byte OP_X = 8;

    private final boolean consumesReadBases;
    private final boolean consumesReferenceBases;
    private final byte character;
    private final String string;

    // Readable synonyms of the above enums
    public static final CigarOperator MATCH_OR_MISMATCH = M;
    public static final CigarOperator INSERTION = I;
    public static final CigarOperator DELETION = D;
    public static final CigarOperator SKIPPED_REGION = N;
    public static final CigarOperator SOFT_CLIP = S;
    public static final CigarOperator HARD_CLIP = H;
    public static final CigarOperator PADDING = P;

    /**
     * Default constructor.
     */
    CigarOperator(boolean consumesReadBases, boolean consumesReferenceBases, char character) {
        this.consumesReadBases = consumesReadBases;
        this.consumesReferenceBases = consumesReferenceBases;
        this.character = (byte) character;
        this.string = new String(new char[]{character}).intern();
    }

    /**
     * If true, represents that this cigar operator "consumes" bases from the read bases.
     */
    public boolean consumesReadBases() {
        return consumesReadBases;
    }

    /**
     * If true, represents that this cigar operator "consumes" bases from the reference sequence.
     */
    public boolean consumesReferenceBases() {
        return consumesReferenceBases;
    }

    /**
     * @param b CIGAR operator in character form as appears in a text CIGAR string
     * @return CigarOperator enum value corresponding to the given character.
     */
    public static CigarOperator characterToEnum(final int b) {
        switch (b) {
            case 'M':
                return M;
            case 'I':
                return I;
            case 'D':
                return D;
            case 'N':
                return N;
            case 'S':
                return S;
            case 'H':
                return H;
            case 'P':
                return P;
            case '=':
                return EQ;
            case 'X':
                return X;
            default:
                throw new IllegalArgumentException("Unrecognized CigarOperator: " + b);
        }
    }

    /**
     * @param i CIGAR operator in binary form as appears in a BAMRecord.
     * @return CigarOperator enum value corresponding to the given int value.
     */
    public static CigarOperator binaryToEnum(final int i) {
        switch (i) {
            case OP_M:
                return M;
            case OP_I:
                return I;
            case OP_D:
                return D;
            case OP_N:
                return N;
            case OP_S:
                return S;
            case OP_H:
                return H;
            case OP_P:
                return P;
            case OP_EQ:
                return EQ;
            case OP_X:
                return X;
            default:
                throw new IllegalArgumentException("Unrecognized CigarOperator: " + i);
        }
    }

    /**
     * @param e CigarOperator enum value.
     * @return CIGAR operator corresponding to the enum value in binary form as appears in a BAMRecord.
     */
    public static int enumToBinary(final CigarOperator e) {
        switch (e) {
            case M:
                return OP_M;
            case I:
                return OP_I;
            case D:
                return OP_D;
            case N:
                return OP_N;
            case S:
                return OP_S;
            case H:
                return OP_H;
            case P:
                return OP_P;
            case EQ:
                return OP_EQ;
            case X:
                return OP_X;
            default:
                throw new IllegalArgumentException("Unrecognized CigarOperator: " + e);
        }
    }

    /**
     * Returns the character that should be used within a SAM file.
     */
    public static byte enumToCharacter(final CigarOperator e) {
        return e.character;
    }

    /**
     * Returns the cigar operator as it would be seen in a SAM file.
     */
    @Override
    public String toString() {
        return this.string;
    }
}
