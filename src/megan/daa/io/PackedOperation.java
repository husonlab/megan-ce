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
 * A  packed operation
 * Daniel Huson, 8.2015
 */
public class PackedOperation {
    private final int code;

    /**
     * constructor
     *
     * @param code
     */
    public PackedOperation(int code) {
        this.code = code;
    }

    /**
     * constructor
     *
     * @param op
     * @param count
     */
    public PackedOperation(PackedTranscript.EditOperation op, int count) {
        code = (byte) (((op.ordinal() << 6) | count) & 0xFF);
    }

    /**
     * constructor
     *
     * @param op
     * @param value
     */
    public PackedOperation(PackedTranscript.EditOperation op, byte value) {
        code = ((op.ordinal() << 6) | (int) value) & 0xFF;
    }

    /**
     * get the operation
     *
     * @return
     */
    public PackedTranscript.EditOperation getEditOperation() {
        return PackedTranscript.EditOperation.values()[code >>> 6];
    }

    public int getOpCode() {
        return code >>> 6;
    }


    /**
     * get the count
     *
     * @return
     */
    public int getCount() {
        return code & 63;
    }

    /**
     * get the letter
     *
     * @return
     */
    public byte getLetter() {
        return (byte) (code & 63);
    }


    static public PackedOperation terminator() {
        return new PackedOperation(PackedTranscript.EditOperation.op_match, 0);
    }

    public boolean equals(Object op) {
        return op instanceof PackedOperation && code == ((PackedOperation) op).code;
    }

    public String toString() {
        return "" + getEditOperation() + "," + getCount();
    }
}
