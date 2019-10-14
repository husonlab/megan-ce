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

import java.util.ArrayList;

/**
 * packed alignment transcript
 * daniel huson, 8.2105
 */
public class PackedTranscript {
    public enum EditOperation {op_match, op_insertion, op_deletion, op_substitution}

    private PackedOperation[] transcript = new PackedOperation[100];
    private int size;

    /**
     * read a packed transcript from a buffer
     *
     * @param buffer
     */
    public void read(ByteInputBuffer buffer) {
        size = 0;
        for (PackedOperation op = new PackedOperation(buffer.read()); !op.equals(PackedOperation.terminator()); op = new PackedOperation(buffer.read())) {
            if (size == transcript.length - 1) {
                final PackedOperation[] tmp = new PackedOperation[2 * transcript.length];
                System.arraycopy(transcript, 0, tmp, 0, size);
                transcript = tmp;
            }
            transcript[size++] = op;
        }
    }

    private PackedOperation getPackedOperation(int i) {
        return transcript[i];
    }

    /**
     * get number of operations
     *
     * @return size
     */
    private int size() {
        return size;
    }

    /**
     * gather all edits into combined operations
     *
     * @return combined operations
     */
    public CombinedOperation[] gather() {
        final ArrayList<CombinedOperation> list = new ArrayList<>();

        for (int i = 0; i < size(); i++) {
            PackedOperation pop = getPackedOperation(i);
            final CombinedOperation cop = new CombinedOperation();

            cop.setEditOperation(pop.getEditOperation());
            if (pop.getEditOperation().equals(EditOperation.op_deletion) || pop.getEditOperation().equals(EditOperation.op_substitution)) {
                cop.setLetter(pop.getLetter());
                cop.setCount(1);
            } else {
                cop.setCount(0);
                while (true) {
                    cop.incrementCount(pop.getCount());
                    i++;
                    if (i == size())
                        break;
                    pop = getPackedOperation(i);
                    if (cop.getEditOperation() != pop.getEditOperation())
                        break;
                }
                i--;
            }
            list.add(cop);
        }
        return list.toArray(new CombinedOperation[0]);
    }
}
