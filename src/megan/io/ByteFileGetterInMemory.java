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
package megan.io;

import jloda.util.CanceledException;
import jloda.util.ProgressPercentage;

import java.io.*;

/**
 * byte file getter in memory
 * Daniel Huson, 5.2015
 */
public class ByteFileGetterInMemory implements IByteGetter {
    private final int BITS = 30;
    private final long BIT_MASK = (1L << (long) BITS) - 1L;
    private final byte[][] data;
    private final long limit;

    /**
     * int file getter in memory
     *
     * @param file
     * @throws IOException
     * @throws CanceledException
     */
    public ByteFileGetterInMemory(File file) throws IOException, CanceledException {
        limit = file.length();

        data = new byte[(int) ((limit >>> BITS)) + 1][];
        final int length0 = (1 << BITS);
        for (int i = 0; i < data.length; i++) {
            int length = (i < data.length - 1 ? length0 : (int) (limit & BIT_MASK) + 1);
            data[i] = new byte[length];
        }

        try (InputStream ins = new BufferedInputStream(new FileInputStream(file)); ProgressPercentage progress = new ProgressPercentage("Reading file: " + file, limit)) {
            int whichArray = 0;
            int indexInArray = 0;
            for (long index = 0; index < limit; index++) {
                data[whichArray][indexInArray] = (byte) ins.read();
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
    public int get(long index) {
        return data[(int) (index >>> BITS)][(int) (index & BIT_MASK)];
    }

    /**
     * bulk get
     *
     * @param index
     * @param bytes
     * @param offset
     * @param len
     * @return
     */
    @Override
    public int get(long index, byte[] bytes, int offset, int len) {
        for (int i = 0; i < len; i++) {
            //  bytes[offset + i]=get(index++);
            bytes[offset + i] = data[(int) (index >>> BITS)][(int) (index & BIT_MASK)];
            index++;
        }
        return len;
    }

    /**
     * gets next four bytes as a single integer
     *
     * @param index
     * @return integer
     */
    @Override
    public int getInt(long index) {
        //return ((get(index++) & 0xFF) << 24) + ((get(index++) & 0xFF) << 16) + ((get(index++) & 0xFF) << 8) + ((get(index) & 0xFF));
        return (((int) (data[(int) (index >>> BITS)][(int) (index++ & BIT_MASK)]) & 0xFF) << 24)
                + (((int) (data[(int) (index >>> BITS)][(int) (index++ & BIT_MASK)]) & 0xFF) << 16)
                + (((int) (data[(int) (index >>> BITS)][(int) (index++ & BIT_MASK)]) & 0xFF) << 8)
                + (((int) (data[(int) (index >>> BITS)][(int) (index & BIT_MASK)]) & 0xFF));
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

    /**
     * get current array
     *
     * @param index
     * @return array index
     * @deprecated
     */
    private int dataIndex(long index) {
        return (int) (index >>> BITS);
    }

    /**
     * get position in current array
     *
     * @param index
     * @return pos
     * @deprecated
     */
    private int dataPos(long index) {
        return (int) (index & BIT_MASK);
    }

    /**
     * test indexing
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        ByteFileGetterInMemory byteFileGetterInMemory = new ByteFileGetterInMemory(new File("/dev/null"));
        int length0 = (1 << byteFileGetterInMemory.BITS);
        for (long i = 0; i < 10L * Integer.MAX_VALUE; i++) {
            int index = byteFileGetterInMemory.dataIndex(i);
            int pos = byteFileGetterInMemory.dataPos(i);

            long result = (long) index * (long) length0 + (long) pos;

            if (result != i)
                throw new Exception("i=" + i + " != result=" + result);
        }
    }
}
