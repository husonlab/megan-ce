/*
 * InputStreamAdapter.java Copyright (C) 2020. Daniel H. Huson
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

import java.io.IOException;
import java.io.InputStream;

/**
 * Input adapter
 * Daniel Huson, 6.2009
 */
public class InputStreamAdapter implements IInput {
    private final InputStream ins;
    private long position;

    public InputStreamAdapter(InputStream ins) {
        this.ins = ins;
    }

    public void seek(long pos) {
    }

    public boolean supportsSeek() {
        return false;
    }

    /**
     * skip some bytes
     *
     * @param bytes
     * @return number of bytes skipped
     * @throws java.io.IOException
     */
    public int skipBytes(int bytes) throws IOException {
        int skipped = (int) ins.skip(bytes);
        position += skipped;
        return skipped;
    }

    /**
     * read some bytes
     *
     * @param bytes
     * @param offset
     * @param len
     * @return number of bytes read
     * @throws IOException
     */
    public int read(byte[] bytes, int offset, int len) throws IOException {
        int count = ins.read(bytes, offset, len);
        position += count;
        return count;
    }

    public long getPosition() throws IOException {
        return position;
    }

    public long length() throws IOException {
        if (ins instanceof IInput)
            return ((IInput) ins).length();
        return Long.MAX_VALUE;
    }

    @Override
    public int read() throws IOException {
        position++;
        return ins.read();
    }

    @Override
    public void close() throws IOException {
        ins.close();

    }
}
