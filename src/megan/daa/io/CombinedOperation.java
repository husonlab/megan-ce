/*
 * CombinedOperation.java Copyright (C) 2023 Daniel H. Huson
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

package megan.daa.io;

/**
 * combined edit operations
 * Daniel Huson, 8.2015
 */
public class CombinedOperation {
    private PackedTranscript.EditOperation editOperation;
    private int count;
    private byte letter;

    public CombinedOperation() {
    }

    public CombinedOperation(PackedTranscript.EditOperation editOperation, int count, Byte letter) {
        this.editOperation = editOperation;
        this.count = count;
        this.letter = (letter == null ? 0 : letter);
    }

    public PackedTranscript.EditOperation getEditOperation() {
        return editOperation;
    }

    public int getOpCode() {
        return editOperation.ordinal();
    }

    public void setEditOperation(PackedTranscript.EditOperation editOperation) {
        this.editOperation = editOperation;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incrementCount(int add) {
        this.count += add;
    }

    public byte getLetter() {
        return letter;
    }

    public void setLetter(byte letter) {
        this.letter = letter;
    }
}
