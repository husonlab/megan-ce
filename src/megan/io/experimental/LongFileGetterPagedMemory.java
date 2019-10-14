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
package megan.io.experimental;

import jloda.util.Basic;
import jloda.util.CanceledException;
import megan.io.ILongGetter;
import megan.io.LongFileGetterMappedMemory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

/**
 * long file getter in paged memory
 * Daniel Huson, 5.2015
 */
public class LongFileGetterPagedMemory implements ILongGetter {
    private final File file;
    private final RandomAccessFile raf;

    private final long[][] data;
    private final long limit;
    private final int length0;

    private final int PAGE_BITS = 20; // 2^20=1048576
    // private final int PAGE_BITS=10; // 2^10=1024
    private int pages = 0;

    /**
     * long file getter in memory
     *
     * @param file
     * @throws IOException
     * @throws CanceledException
     */
    public LongFileGetterPagedMemory(File file) throws IOException {
        this.file = file;
        limit = file.length() / 8;

        System.err.println("Opening file: " + file);
        raf = new RandomAccessFile(file, "r");

        data = new long[(int) ((limit >>> PAGE_BITS)) + 1][];
        length0 = (int) (Math.min(limit, 1 << PAGE_BITS));
    }

    /**
     * gets value for given index
     *
     * @param index
     * @return value or 0
     */
    @Override
    public long get(long index) throws IOException {
        int dIndex = dataIndex(index);
        long[] array = data[dIndex];
        if (array == null) {
            synchronized (raf) {
                if (data[dIndex] == null) {
                    pages++;
                    final int length;
                    if (dIndex == data.length - 1)
                        length = (int) (limit - (data.length - 1) * length0); // is the last chunk
                    else
                        length = length0;

                    array = new long[length];
                    long toSkip = (long) dIndex * (long) length0 * 8L;
                    raf.seek(toSkip);

                    for (int pos = 0; pos < array.length; pos++) {
                        array[pos] = (((long) (raf.read()) & 0xFF) << 56) + (((long) (raf.read()) & 0xFF) << 48) + (((long) (raf.read()) & 0xFF) << 40) + (((long) (raf.read()) & 0xFF) << 32)
                                + (((long) (raf.read()) & 0xFF) << 24) + (((long) (raf.read()) & 0xFF) << 16) + (((long) (raf.read()) & 0xFF) << 8) + (((long) (raf.read()) & 0xFF));
                    }
                    data[dIndex] = array;
                } else
                    array = data[dIndex];
            }
        }
        return array[dataPos(index)];
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
        try {
            raf.close();
            System.err.println("Closing file: " + file.getName() + " (" + pages + "/" + data.length + " pages)");
        } catch (IOException e) {
            Basic.caught(e);
        }
    }

    private int dataIndex(long index) {
        return (int) ((index >>> PAGE_BITS));
    }

    private int dataPos(long index) {
        return (int) (index - (index >>> PAGE_BITS) * length0);
    }

    public static void main(String[] args) throws IOException {
        File file = new File("/Users/huson/data/ma/protein/index-new/table0.idx");

        final ILongGetter oldGetter = new LongFileGetterMappedMemory(file);
        final ILongGetter newGetter = new LongFileGetterPagedMemory(file);

        final Random random = new Random();
        System.err.println("Limit: " + oldGetter.limit());
        for (int i = 0; i < 100; i++) {
            int r = random.nextInt((int) oldGetter.limit());

            long oldValue = oldGetter.get(r);
            long newValue = newGetter.get(r);

            System.err.println(oldValue + (oldValue == newValue ? " == " : " != ") + newValue);
        }
        oldGetter.close();
        newGetter.close();
    }
}
