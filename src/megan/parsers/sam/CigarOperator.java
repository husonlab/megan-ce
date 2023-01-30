/*
 * CigarOperator.java Copyright (C) 2023 Daniel H. Huson
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
    X(true, true, 'X'),
    /**
     * reverse frame shift
     */
    FF(true, false, '/'),
    /**
     * forward frame shift
     */
    FR(false, true, '\\');

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
        this.string = String.valueOf(character).intern();
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
        return switch (b) {
            case 'M' -> M;
            case 'I' -> I;
            case 'D' -> D;
            case 'N' -> N;
            case 'S' -> S;
            case 'H' -> H;
            case 'P' -> P;
            case '=' -> EQ;
            case 'X' -> X;
            case '/' -> FF;
            case '\\' -> FR;
            default -> throw new IllegalArgumentException("Unrecognized CigarOperator: " + (char) b);
        };
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
