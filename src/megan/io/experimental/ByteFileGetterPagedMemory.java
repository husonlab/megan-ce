/*
 * ByteFileGetterPagedMemory.java Copyright (C) 2024 Daniel H. Huson
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
package megan.io.experimental;

import jloda.util.Basic;
import megan.io.IByteGetter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * byte file getter using paged memory
 * Daniel Huson, 5.2015
 */
public class ByteFileGetterPagedMemory implements IByteGetter {
    private final File file;
    private final RandomAccessFile raf;

    private final byte[][] data;
    private final long limit;
    private final int length0;

    private final int PAGE_BITS = 20; // 2^20=1048576
    private int pages = 0;

    /**
     * constructor
     *
	 */
    public ByteFileGetterPagedMemory(File file) throws IOException {
        this.file = file;
        limit = file.length();

        System.err.println("Opening file: " + file);
        raf = new RandomAccessFile(file, "r");

        data = new byte[(int) ((limit >>> PAGE_BITS)) + 1][];
        length0 = (int) (Math.min(limit, 1 << PAGE_BITS));
    }

    /**
     * bulk get
     *
	 */
    @Override
    public int get(long index, byte[] bytes, int offset, int len) throws IOException {
        synchronized (raf) {
            raf.seek(index);
            len = raf.read(bytes, offset, len);
        }
        return len;
    }

    /**
     * gets value for given index
     *
     * @return value or 0
     */
    @Override
    public int get(long index) throws IOException {
        int dIndex = dataIndex(index);
        byte[] array = data[dIndex];
        if (array == null) {
            synchronized (raf) {
                if (data[dIndex] == null) {
                    pages++;
                    final int length;
                    if (dIndex == data.length - 1)
                        length = (int) (limit - (data.length - 1) * length0); // is the last chunk
                    else
                        length = length0;

                    array = new byte[length];
                    long toSkip = (long) dIndex * (long) length0;
                    raf.seek(toSkip);
                    raf.read(array, 0, array.length);
                    data[dIndex] = array;
                } else
                    array = data[dIndex];
            }
        }
        return array[dataPos(index)];
    }

    /**
     * gets next four bytes as a single integer
     *
     * @return integer
     */
    @Override
    public int getInt(long index) throws IOException {
        return ((get(index++) & 0xFF) << 24) + ((get(index++) & 0xFF) << 16) + ((get(index++) & 0xFF) << 8) + ((get(index) & 0xFF));
    }

    /**
     * length of array
     *
     * @return array length
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
}
