/*
 * CigarElement.java Copyright (C) 2021. Daniel H. Huson
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
 *
 */
package megan.parsers.sam;

/**
 * One component of a cigar string.  The component comprises the operator, and the number of bases to which
 * the  operator applies.
 */
public class CigarElement {
    private final int length;
    private final CigarOperator operator;

    public CigarElement(final int length, final CigarOperator operator) {
        this.length = length;
        this.operator = operator;
    }

    public int getLength() {
        return length;
    }

    public CigarOperator getOperator() {
        return operator;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CigarElement)) return false;

        final CigarElement that = (CigarElement) o;

        return length == that.length && operator == that.operator;

    }

    @Override
    public int hashCode() {
        int result = length;
        result = 31 * result + (operator != null ? operator.hashCode() : 0);
        return result;
    }
}
