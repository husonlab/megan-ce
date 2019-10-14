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
 * Convert between String and Cigar class representations of CIGAR.
 */
public class TextCigarCodec {
    private static final byte ZERO_BYTE = "0".getBytes()[0];
    private static final byte NINE_BYTE = "9".getBytes()[0];

    private static final TextCigarCodec singleton = new TextCigarCodec();

    /**
     * It is not necessary to get the singleton but it is preferable to use the same one
     * over and over vs. creating a new object for each BAMRecord.  There is no state in this
     * class so this is thread-safe.
     *
     * @return A singleton TextCigarCodec useful for converting Cigar classes to and from strings
     */
    public static TextCigarCodec getSingleton() {
        return singleton;
    }


    /**
     * Convert from Cigar class representation to String.
     *
     * @param cigar in Cigar class format
     * @return CIGAR in String form ala SAM text file.  "*" means empty CIGAR.
     */
    public String encode(final Cigar cigar) {
        if (cigar.isEmpty()) {
            return "*";
        }
        final StringBuilder ret = new StringBuilder();
        for (final CigarElement cigarElement : cigar.getCigarElements()) {
            ret.append(cigarElement.getLength());
            ret.append(cigarElement.getOperator());
        }
        return ret.toString();
    }

    /**
     * Convert from String CIGAR representation to Cigar class representation.  Does not
     * do validation beyond the most basic CIGAR string well-formedness, i.e. each operator is
     * valid, and preceded by a decimal length.
     *
     * @param textCigar CIGAR in String form ala SAM text file.  "*" means empty CIGAR.
     * @return cigar in Cigar class format
     * @throws RuntimeException if textCigar is invalid at the most basic level.
     */
    public Cigar decode(final String textCigar) {
        if ("*".equals(textCigar)) {
            return new Cigar();
        }
        final Cigar ret = new Cigar();
        final byte[] cigarBytes = textCigar.getBytes();
        for (int i = 0; i < cigarBytes.length; ++i) {
            if (!isDigit(cigarBytes[i])) {
                throw new IllegalArgumentException("Malformed CIGAR string: " + textCigar);
            }
            int length = (cigarBytes[i] - ZERO_BYTE);
            for (++i; isDigit(cigarBytes[i]); ++i) {
                length = (length * 10) + cigarBytes[i] - ZERO_BYTE;
            }
            final CigarOperator operator = CigarOperator.characterToEnum(cigarBytes[i]);
            ret.add(new CigarElement(length, operator));
        }
        return ret;
    }

    private boolean isDigit(final byte c) {
        return c >= ZERO_BYTE && c <= NINE_BYTE;
    }
}

