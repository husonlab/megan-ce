/*
 * LongFilePutter.java Copyright (C) 2024 Daniel H. Huson
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
public class LongFilePutter extends BaseFileGetterPutter implements ILongPutter, ILongGetter {

    /**
     * constructor  to read and write values from an existing file
     *
	 */
    public LongFilePutter(File file) throws IOException {
        super(file, 0, Mode.READ_WRITE);
    }

    /**
     * constructs a long file putter using the given file and limit
     *
     * @param limit length of array
	 */
    public LongFilePutter(File file, long limit) throws IOException {
        this(file, limit, false);
    }

    /**
     * constructs a long file putter using the given file and limit
     *
     * @param limit length of array
	 */
    public LongFilePutter(File file, long limit, boolean inMemory) throws IOException {
        super(file, 8 * limit, (inMemory ? Mode.CREATE_READ_WRITE_IN_MEMORY : Mode.CREATE_READ_WRITE));
    }

    /**
     * gets value for given index
     *
     * @return value or 0
     */
    public long get(long index) {
        if (index < limit()) {
            index <<= 3; // convert to file position
            final ByteBuffer buf = buffers[getWhichBuffer(index)];
            int indexBuffer = getIndexInBuffer(index);
            return (((long) buf.get(indexBuffer++)) << 56) + ((long) (buf.get(indexBuffer++) & 0xFF) << 48) + ((long) (buf.get(indexBuffer++) & 0xFF) << 40) +
                    ((long) (buf.get(indexBuffer++) & 0xFF) << 32) + ((long) (buf.get(indexBuffer++) & 0xFF) << 24) + ((long) (buf.get(indexBuffer++) & 0xFF) << 16) +
                    ((long) (buf.get(indexBuffer++) & 0xFF) << 8) + (((long) buf.get(indexBuffer) & 0xFF));
        } else
            return 0;
    }

    /**
     * puts value for given index
     *
	 */
    @Override
    public ILongPutter put(long index, long value) {
        if (index < limit()) {
            index <<= 3; // convert to file position
            final ByteBuffer buf = buffers[getWhichBuffer(index)];
            int indexBuffer = getIndexInBuffer(index);

            buf.put(indexBuffer++, (byte) (value >> 56));
            buf.put(indexBuffer++, (byte) (value >> 48));
            buf.put(indexBuffer++, (byte) (value >> 40));
            buf.put(indexBuffer++, (byte) (value >> 32));
            buf.put(indexBuffer++, (byte) (value >> 24));
            buf.put(indexBuffer++, (byte) (value >> 16));
            buf.put(indexBuffer++, (byte) (value >> 8));
            buf.put(indexBuffer, (byte) (value));
        } else
            throw new ArrayIndexOutOfBoundsException("" + index);
        return this;
    }

    /**
     * length of array (file length / 8)
     *
     * @return array length
	 */
    @Override
    public long limit() {
        return fileLength >>> 3;
    }

    /**
     * set a new limit for a file
     *
	 */
    public static void setLimit(File file, long newLimit) throws IOException {
        resize(file, 8 * (newLimit + 1));
    }
}
