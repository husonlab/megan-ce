/*
 * IntFilePutter.java Copyright (C) 2023 Daniel H. Huson
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
package megan.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Open a file for reading and writing
 * <p/>
 * Daniel Huson, 4.2015
 */
public class IntFilePutter extends BaseFileGetterPutter implements IIntPutter, IIntGetter {
    /**
     * constructor  to read and write values from an existing file
     *
	 */
    public IntFilePutter(File file) throws IOException {
        super(file, 0, Mode.READ_WRITE);
    }

    /**
     * constructs a int file putter using the given file and limit. (Is not in-memory)
     *
     * @param limit length of array
	 */
    public IntFilePutter(File file, long limit) throws IOException {
        this(file, limit, false);
    }

    /**
     * constructs a int file putter using the given file and limit
     *
     * @param limit    length of array
     * @param inMemory create in memory and then save on close? This uses more memory, but may be faster
	 */
    public IntFilePutter(File file, long limit, boolean inMemory) throws IOException {
        super(file, 4 * limit, inMemory ? Mode.CREATE_READ_WRITE_IN_MEMORY : Mode.CREATE_READ_WRITE);
    }

    /**
     * gets value for given index
     *
     * @return value or 0
     */
    public int get(long index) {
        if (index < limit()) {
            index <<= 2; // convert to file position
            final ByteBuffer buf = buffers[getWhichBuffer(index)];
            int indexBuffer = getIndexInBuffer(index);
            return ((buf.get(indexBuffer++)) << 24) + ((buf.get(indexBuffer++) & 0xFF) << 16) +
                    ((buf.get(indexBuffer++) & 0xFF) << 8) + ((buf.get(indexBuffer) & 0xFF));
        } else
            return 0;
    }

    /**
     * puts value for given index
     *
	 */
    @Override
    public void put(long index, int value) {
        index <<= 2; // convert to file position
        if (index < fileLength) {
            final ByteBuffer buf = buffers[getWhichBuffer(index)];
            int indexBuffer = getIndexInBuffer(index);
            buf.put(indexBuffer++, (byte) (value >> 24));
            buf.put(indexBuffer++, (byte) (value >> 16));
            buf.put(indexBuffer++, (byte) (value >> 8));
            buf.put(indexBuffer, (byte) (value));
        } else {
            throw new ArrayIndexOutOfBoundsException("" + index);
        }
    }

    /**
     * length of array (file length / 4)
     *
     * @return array length
	 */
    @Override
    public long limit() {
        return fileLength >>> 2;
    }

    /**
     * set a new limit for a file
     *
	 */
    public static void setLimit(File file, long newLimit) throws IOException {
        System.err.println("new limit: " + newLimit);

        resize(file, 4 * (newLimit + 1));
    }
}
