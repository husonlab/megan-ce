/*
 *  Copyright (C) 2017 Daniel H. Huson
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
import megan.io.IByteGetter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * byte file getter input stream
 * Daniel Huson, 2.2016
 */
public class ByteFileStreamGetter implements IByteGetter {
    private final File file;
    private final BufferedInputStream raf;

    private final long limit;
    private long currentIndex = 0;

    /**
     * constructor
     *
     * @param file
     * @throws IOException
     * @throws CanceledException
     */
    public ByteFileStreamGetter(File file) throws IOException {
        this.file = file;
        limit = file.length();

        System.err.println("Opening file: " + file);
        raf = new BufferedInputStream(new FileInputStream(file));
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
    public int get(long index, byte[] bytes, int offset, int len) throws IOException {
        synchronized (raf) {
            if (index != currentIndex)
                throw new IOException("seek(): not supported");
            else {
                len = raf.read(bytes, offset, len);
                index += len;
                return len;
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
    public int get(long index) throws IOException {
        synchronized (raf) {
            if (index != currentIndex)
                throw new IOException("seek(): not supported");
            currentIndex++;
            return raf.read();
        }
    }

    /**
     * gets next four bytes as a single integer
     *
     * @param index
     * @return integer
     */
    @Override
    public int getInt(long index) throws IOException {
        synchronized (raf) {
            if (index != currentIndex)
                throw new IOException("seek(): not supported");
            currentIndex += 4;
            return ((raf.read() & 0xFF) << 24) + ((raf.read() & 0xFF) << 16) + ((raf.read() & 0xFF) << 8) + (raf.read() & 0xFF);
        }
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
            System.err.println("Closing file: " + file.getName());
        } catch (IOException e) {
            Basic.caught(e);
        }
    }

    public static void main(String[] args) throws IOException {
        File file = new File("/Users/huson/data/ma/protein/index/table0.idx");

        final IByteGetter getter = new ByteFileStreamGetter(file);

        System.err.println("Limit: " + getter.limit());
        for (int i = 0; i < 100; i++) {
            int value = getter.get(i);

            System.err.println(i + " -> " + value);
        }
        getter.close();
    }
}
