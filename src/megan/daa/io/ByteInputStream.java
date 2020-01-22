/*
 * ByteInputStream.java Copyright (C) 2020. Daniel H. Huson
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

package megan.daa.io;

import megan.io.IInput;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Byte input stream
 * 8.2015
 */
public class ByteInputStream extends ByteArrayInputStream implements IInput {
    private static final byte[] EMPTY_ARRAY = new byte[0];

    public ByteInputStream() {
        this(EMPTY_ARRAY, 0);
    }

    public ByteInputStream(byte[] buf, int length) {
        super(buf, 0, length);
    }

    public ByteInputStream(byte[] buf, int offset, int length) {
        super(buf, offset, length);
    }

    public byte[] getBytes() {
        return this.buf;
    }

    public int getCount() {
        return this.count;
    }

    public void close() throws IOException {
        this.reset();
    }

    public void setBuf(byte[] buf) {
        this.buf = buf;
        this.pos = 0;
        this.count = buf.length;
    }

    @Override
    public int skipBytes(int bytes) throws IOException {
        return (int) this.skip(bytes);
    }

    @Override
    public long getPosition() throws IOException {
        return this.count;
    }

    @Override
    public long length() throws IOException {
        return this.count;
    }

    @Override
    public boolean supportsSeek() throws IOException {
        return false;
    }

    @Override
    public void seek(long pos) throws IOException {
    }
}
