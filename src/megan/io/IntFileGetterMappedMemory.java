/*
 * IntFileGetterMappedMemory.java Copyright (C) 2020. Daniel H. Huson
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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Open and read file of integers. File can be arbitrarily large, uses memory mapping.
 * <p/>
 * Daniel Huson, 4.2015
 */
public class IntFileGetterMappedMemory extends BaseFileGetterPutter implements IIntGetter {
    /**
     * constructor
     *
     * @param file
     * @throws java.io.IOException
     */
    public IntFileGetterMappedMemory(File file) throws IOException {
        super(file);
    }

    /**
     * gets value for given index
     *
     * @return integer
     * @throws IOException
     */
    public int get(long index) {
        if (index < limit()) {
            index <<= 2; // convert to file position
            // the following works because we buffers overlap by 4 bytes
            int indexBuffer = getIndexInBuffer(index);
            final ByteBuffer buf = buffers[getWhichBuffer(index)];
            return ((buf.get(indexBuffer++)) << 24) + ((buf.get(indexBuffer++) & 0xFF) << 16) + ((buf.get(indexBuffer++) & 0xFF) << 8) + ((buf.get(indexBuffer) & 0xFF));
        } else
            return 0;
    }

    /**
     * length of array (file length/4)
     *
     * @return array length
     * @throws java.io.IOException
     */
    @Override
    public long limit() {
        return fileLength >>> 2;
    }
}
