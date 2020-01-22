/*
 * IntFileGetterRandomAccess.java Copyright (C) 2020. Daniel H. Huson
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

import jloda.util.Basic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * adapter from random-access read-only file
 * Daniel Huson, 6.2009
 */
public class IntFileGetterRandomAccess extends RandomAccessFile implements IIntGetter {
    /**
     * constructor
     *
     * @param file
     * @throws FileNotFoundException
     */
    public IntFileGetterRandomAccess(File file) throws FileNotFoundException {
        super(file, "r");
    }

    /**
     * get an int from the given location
     *
     * @param index
     * @return int
     */
    @Override
    public int get(long index) {
        try {
            index <<= 2; // convert to file pos
            if (index < length()) {
                // synchronized (syncObject)
                {
                    seek(index);
                    return readInt();
                }
            }

        } catch (IOException e) {
            Basic.caught(e);
        }
        return 0;
    }

    /**
     * length of array
     *
     * @return array length
     * @throws java.io.IOException
     */
    @Override
    public long limit() {
        try {
            return length() >>> 2;
        } catch (IOException e) {
            Basic.caught(e);
            return 0;
        }
    }

    public void close() {
        try {
            super.close();
        } catch (IOException e) {
            Basic.caught(e);
        }
    }
}
