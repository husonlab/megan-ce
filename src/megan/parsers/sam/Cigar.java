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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A list of CigarElements, which describes how a read aligns with the reference.
 * E.g. the Cigar string 10M1D25M means
 * * match or mismatch for 10 bases
 * * deletion of 1 base
 * * match or mismatch for 25 bases
 * <p/>
 * c.f. http://samtools.sourceforge.net/SAM1.pdf for complete CIGAR specification.
 */
public class Cigar {
    private final List<CigarElement> cigarElements = new ArrayList<>();

    public Cigar() {
    }

    public Cigar(final List<CigarElement> cigarElements) {
        this.cigarElements.addAll(cigarElements);
    }

    public List<CigarElement> getCigarElements() {
        return Collections.unmodifiableList(cigarElements);
    }

    public CigarElement getCigarElement(final int i) {
        return cigarElements.get(i);
    }

    public void add(final CigarElement cigarElement) {
        cigarElements.add(cigarElement);
    }

    public int numCigarElements() {
        return cigarElements.size();
    }

    public boolean isEmpty() {
        return cigarElements.isEmpty();
    }

    /**
     * @return The number of reference bases that the read covers, excluding padding.
     */
    public int getReferenceLength() {
        int length = 0;
        for (final CigarElement element : cigarElements) {
            switch (element.getOperator()) {
                case M:
                case D:
                case N:
                case EQ:
                case X:
                    length += element.getLength();
            }
        }
        return length;
    }

    /**
     * @return The number of reference bases that the read covers, including padding.
     */
    public int getPaddedReferenceLength() {
        int length = 0;
        for (final CigarElement element : cigarElements) {
            switch (element.getOperator()) {
                case M:
                case D:
                case N:
                case EQ:
                case X:
                case P:
                    length += element.getLength();
            }
        }
        return length;
    }

    /**
     * @return The number of read bases that the read covers.
     */
    public int getReadLength() {
        return getReadLength(cigarElements);
    }

    /**
     * @return The number of read bases that the read covers.
     */
    private static int getReadLength(final List<CigarElement> cigarElements) {
        int length = 0;
        for (final CigarElement element : cigarElements) {
            if (element.getOperator().consumesReadBases()) {
                length += element.getLength();
            }
        }
        return length;
    }

    /**
     * Exhaustive validation of CIGAR.
     * Note that this method deliberately returns null rather than Collections.emptyList() if there
     * are no validation errors, because callers tend to assume that if a non-null list is returned, it is modifiable.
     *
     * @param readName     For error reporting only.  May be null if not known.
     * @param recordNumber For error reporting only.  May be -1 if not known.
     * @return List of validation errors, or null if no errors.
     */
    public List<SAMValidationError> isValid(final String readName, final long recordNumber) {
        if (this.isEmpty()) {
            return null;
        }
        List<SAMValidationError> ret = null;
        boolean seenRealOperator = false;
        for (int i = 0; i < cigarElements.size(); ++i) {
            final CigarElement element = cigarElements.get(i);
            // clipping operator can only be at start or end of CIGAR
            final CigarOperator op = element.getOperator();
            if (isClippingOperator(op)) {
                if (op == CigarOperator.H) {
                    if (i != 0 && i != cigarElements.size() - 1) {
                        if (ret == null) ret = new ArrayList<>();
                        ret.add(new SAMValidationError(SAMValidationError.Type.INVALID_CIGAR,
                                "Hard clipping operator not at start or end of CIGAR", readName, recordNumber));
                    }
                } else {
                    if (op != CigarOperator.S) throw new IllegalStateException("Should never happen: " + op.name());
                    if (i == 0 || i == cigarElements.size() - 1) {
                        // Soft clip at either end is fine
                    } else if (i == 1) {
                        if (cigarElements.size() == 3 && cigarElements.get(2).getOperator() == CigarOperator.H) {
                            // Handle funky special case in which S operator is both one from the beginning and one
                            // from the end.
                        } else if (cigarElements.get(0).getOperator() != CigarOperator.H) {
                            if (ret == null) ret = new ArrayList<>();
                            ret.add(new SAMValidationError(SAMValidationError.Type.INVALID_CIGAR,
                                    "Soft clipping CIGAR operator can only be inside of hard clipping operator",
                                    readName, recordNumber));
                        }
                    } else if (i == cigarElements.size() - 2) {
                        if (cigarElements.get(cigarElements.size() - 1).getOperator() != CigarOperator.H) {
                            if (ret == null) ret = new ArrayList<>();
                            ret.add(new SAMValidationError(SAMValidationError.Type.INVALID_CIGAR,
                                    "Soft clipping CIGAR operator can only be inside of hard clipping operator",
                                    readName, recordNumber));
                        }
                    } else {
                        if (ret == null) ret = new ArrayList<>();
                        ret.add(new SAMValidationError(SAMValidationError.Type.INVALID_CIGAR,
                                "Soft clipping CIGAR operator can at start or end of read, or be inside of hard clipping operator",
                                readName, recordNumber));
                    }

                }
            } else if (isRealOperator(op)) {
                // Must be at least one real operator (MIDN)
                seenRealOperator = true;
                // There should be an M or P operator between any pair of IDN operators
                if (isInDelOperator(op)) {
                    for (int j = i + 1; j < cigarElements.size(); ++j) {
                        final CigarOperator nextOperator = cigarElements.get(j).getOperator();
                        // Allow
                        if ((isRealOperator(nextOperator) && !isInDelOperator(nextOperator)) || isPaddingOperator(nextOperator)) {
                            break;
                        }
                        if (isInDelOperator(nextOperator) && op == nextOperator) {
                            if (ret == null) ret = new ArrayList<>();
                            ret.add(new SAMValidationError(SAMValidationError.Type.INVALID_CIGAR,
                                    "No M or N operator between pair of " + op.name() + " operators in CIGAR", readName, recordNumber));
                        }
                    }
                }
            } else if (isPaddingOperator(op)) {
                if (i == 0 || i == cigarElements.size() - 1) {
                    if (ret == null) ret = new ArrayList<>();
                    ret.add(new SAMValidationError(SAMValidationError.Type.INVALID_CIGAR,
                            "Padding operator not valid at start or end of CIGAR", readName, recordNumber));
                } else if (!isRealOperator(cigarElements.get(i - 1).getOperator()) ||
                        !isRealOperator(cigarElements.get(i + 1).getOperator())) {
                    if (ret == null) ret = new ArrayList<>();
                    ret.add(new SAMValidationError(SAMValidationError.Type.INVALID_CIGAR,
                            "Padding operator not between real operators in CIGAR", readName, recordNumber));
                }
            }
        }
        if (!seenRealOperator) {
            if (ret == null) ret = new ArrayList<>();
            ret.add(new SAMValidationError(SAMValidationError.Type.INVALID_CIGAR,
                    "No real operator (M|I|D|N) in CIGAR", readName, recordNumber));
        }
        return ret;
    }

    private static boolean isRealOperator(final CigarOperator op) {
        return op == CigarOperator.M || op == CigarOperator.EQ || op == CigarOperator.X ||
                op == CigarOperator.I || op == CigarOperator.D || op == CigarOperator.N;
    }

    private static boolean isInDelOperator(final CigarOperator op) {
        return op == CigarOperator.I || op == CigarOperator.D;
    }

    private static boolean isClippingOperator(final CigarOperator op) {
        return op == CigarOperator.S || op == CigarOperator.H;
    }

    private static boolean isPaddingOperator(final CigarOperator op) {
        return op == CigarOperator.P;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Cigar)) return false;

        final Cigar cigar = (Cigar) o;

        return !!cigarElements.equals(cigar.cigarElements);

    }

    @Override
    public int hashCode() {
        return cigarElements.hashCode();
    }

    public String toString() {
        return TextCigarCodec.getSingleton().encode(this);
    }

    public void clear() {
        cigarElements.clear();
    }
}
