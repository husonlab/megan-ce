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
package megan.classification.data;

import jloda.util.CanceledException;
import megan.io.IIntGetter;
import megan.io.IntFileGetterMappedMemory;
import megan.io.IntFileGetterRandomAccess;

import java.io.*;

/**
 * long to integer mapping in binary file format
 * Daniel Huson, 4.2010, 4.2015
 */
class Long2IntegerBinMap implements ILong2IntegerMap, Closeable {
    private static final int MAGIC_NUMBER = 666; // write this as first number so that we can recognize file

    private IIntGetter reader = null;

    /**
     * open a bin  file
     *
     * @param fileName
     * @throws IOException
     * @throws CanceledException
     */
    public Long2IntegerBinMap(String fileName) throws IOException, CanceledException {
        final File file = new File(fileName);
        if (!file.exists())
            throw new IOException("No such file: " + file);
        if (!file.canRead())
            throw new IOException("Can't read file: " + file);
        if (!isBinFile(fileName))
            throw new IOException("Wrong magic number: " + file);
        try {
            reader = new IntFileGetterMappedMemory(file);
        } catch (IOException ex) { // on 32-bit machine, memory mapping will fail... use Random access
            System.err.println("Opening file: " + file);
            reader = new IntFileGetterRandomAccess(file);
        }
    }

    /**
     * lookup an id from a gi
     *
     * @param key
     * @return id or 0
     */
    public int get(long key) throws IOException {
        if (key >= 0 && key < reader.limit())
            return reader.get(key);
        else
            return 0;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    /**
     * does this look like a valid bin file?
     *
     * @param fileName
     * @return true, if this looks like a valid bin file
     */
    public static boolean isBinFile(String fileName) {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(fileName))) {
            int firstInt = dis.readInt();
            return firstInt == 0 || firstInt == MAGIC_NUMBER;
        } catch (Exception e) {
            return false;
        }
    }
}
