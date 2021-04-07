/*
 * LongFileGetterInMemory.java Copyright (C) 2021. Daniel H. Huson
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
package megan.io;

import jloda.util.CanceledException;
import jloda.util.ProgressPercentage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * long file getter in memory
 * Daniel Huson, 5.2015
 */
public class LongFileGetterInMemory implements ILongGetter {
    private final int BITS = 30;
    private final long BIT_MASK = (1L << (long) BITS) - 1L;
    private final long[][] data;
    private final long limit;

    /**
     * long file getter in memory
     *
     * @param file
     * @throws IOException
     * @throws CanceledException
     */
    public LongFileGetterInMemory(File file) throws IOException {
        limit = file.length() / 8;

        data = new long[(int) ((limit >>> BITS)) + 1][];
        final int length0 = (1 << BITS);
        for (int i = 0; i < data.length; i++) {
            int length = (i < data.length - 1 ? length0 : (int) (limit & BIT_MASK) + 1);
            data[i] = new long[length];
        }

        try (BufferedInputStream ins = new BufferedInputStream(new FileInputStream(file)); ProgressPercentage progress = new ProgressPercentage("Reading file: " + file, limit)) {
            int whichArray = 0;
            int indexInArray = 0;

            for (long index = 0; index < limit; index++) {
                data[whichArray][indexInArray] = (((long) ins.read()) << 56) | (((long) ins.read()) << 48) | (((long) ins.read()) << 40) | (((long) ins.read()) << 32)
                        | (((long) ins.read()) << 24) | (((long) ins.read() & 0xFF) << 16) | (((long) ins.read() & 0xFF) << 8) | (((long) ins.read() & 0xFF));
                if (++indexInArray == length0) {
                    whichArray++;
                    indexInArray = 0;
                }
                progress.setProgress(index);
            }
        }
    }

    /**
     * gets value for given index
     *
     * @param index
     * @return value or 0
     */
    @Override
    public long get(long index) {
        return data[(int) (index >>> BITS)][(int) (index & BIT_MASK)];
    }

    /**
     * length of array
     *
     * @return array length
     * @throws IOException
     */
    @Override
    public long limit() {
        return limit;
    }

    /**
     * close the array
     */
    @Override
    public void close() {
    }
}
