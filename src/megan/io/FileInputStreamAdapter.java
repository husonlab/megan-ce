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

import java.io.*;

/**
 * File input adapter
 * Daniel Huson, 6.2009
 */
public class FileInputStreamAdapter implements IInput {
    private final BufferedInputStream bif;
    private long position;
    private final long length;

    public FileInputStreamAdapter(String fileName) throws FileNotFoundException {
        this(new File(fileName));
    }

    public FileInputStreamAdapter(File file) throws FileNotFoundException {
        length = file.length();
        bif = new BufferedInputStream(new FileInputStream(file));
        position = 0;
    }

    public int read() throws IOException {
        position++;
        return bif.read();
    }

    public int read(byte[] bytes, int offset, int len) throws IOException {
        int count = bif.read(bytes, offset, len);
        position += count;
        return count;
    }

    public void close() throws IOException {
        bif.close();
    }

    public long getPosition() throws IOException {
        return position;
    }

    public long length() throws IOException {
        return length;
    }

    public void seek(long pos) throws IOException {
        if (pos > position) {
            long diff = (pos - position);
            while (diff > 0) {
                int moved = skipBytes((int) Math.min(Integer.MAX_VALUE >> 1, diff));
                if (moved == 0)
                    throw new IOException("Failed to seek");
                diff -= moved;
            }
        } else if (pos < position)
            throw new IOException("Can't seek backward");
    }

    public boolean supportsSeek() {
        return false;
    }

    /**
     * skip n bytes
     *
     * @param n
     * @return number of bytes skipped
     * @throws IOException
     */
    public int skipBytes(int n) throws IOException {
        int remaining = n;
        while (remaining > 0) {
            remaining -= (int) bif.skip(remaining);
        }
        position += n;
        return n;
    }
}
