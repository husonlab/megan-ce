/*
 * InputReaderLittleEndian.java Copyright (C) 2022 Daniel H. Huson
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

package megan.daa.io;


import jloda.util.ByteInputBuffer;
import megan.io.ByteByteInt;
import megan.io.IInput;
import megan.io.IInputReader;

import java.io.Closeable;
import java.io.IOException;

/**
 * read data in little endian format as produced by C++ programs
 * Daniel Huson, 8.2015
 */
public class InputReaderLittleEndian implements Closeable, IInputReader {
    private final IInput ins;
    private final byte[] bytes = new byte[8];
    private final java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.allocate(8);

    /**
     * constructor
     *
     * @param ins
     */
    public InputReaderLittleEndian(IInput ins) {
        this.ins = ins;
    }

    /**
     * read byte
     *
     * @return byte
     * @throws IOException
     */
    public int read() throws IOException {
        return ins.read();
    }

    /**
     * read bytes
     */
    public int read(byte[] bytes, int offset, int len) throws IOException {
        if (ins.read(bytes, offset, len) < len)
            throw new IOException("buffer underflow");
        return len;
    }

    /**
     * read bytes
     */
    public int read_available(byte[] bytes, int offset, int len) throws IOException {
        return ins.read(bytes, offset, len);
    }

    /**
     * read bytes
     */
    public byte[] readBytes(int count) throws IOException {
        final byte[] bytes = new byte[count];
        read(bytes, 0, count);
        return bytes;
    }


    /**
     * read int little endian
     *
     * @return int
     * @throws IOException
     */
    public int readInt() throws IOException {
        if (ins.read(bytes, 0, 4) < 4)
            throw new IOException("buffer underflow at file pos: " + ins.getPosition());
        return (((int) bytes[0] & 0xFF)) | (((int) bytes[1] & 0xFF) << 8) | (((int) bytes[2] & 0xFF) << 16) | (((int) bytes[3]) << 24);
    }

    /**
     * read long, little endian
     *
     * @return long
     * @throws IOException
     */
    public long readLong() throws IOException {
        if (ins.read(bytes, 0, 8) < 8)
            throw new IOException("buffer underflow");
        return (((long) bytes[0] & 0xFF))
                | (((long) bytes[1] & 0xFF) << 8)
                | (((long) bytes[2] & 0xFF) << 16)
                | (((long) bytes[3] & 0xFF) << 24)
                | (((long) bytes[4] & 0xFF) << 32)
                | (((long) bytes[5] & 0xFF) << 40)
                | (((long) bytes[6] & 0xFF) << 48)
                | (((long) bytes[7] & 0xFF) << 56);
    }

    /**
     * read float, little endian
     *
     * @return float
     * @throws IOException
     */
    public float readFloat() throws IOException {
        read(bytes, 0, 4);
        for (int i = 0; i < 4; i++)
            byteBuffer.put(i, bytes[4 - i - 1]);
        return byteBuffer.getFloat(0);
    }

    /**
     * read double, little endian
     *
     * @return double
     * @throws IOException
     */
    public double readDouble() throws IOException {
        read(bytes, 0, 8);
        for (int i = 0; i < 8; i++)
            byteBuffer.put(i, bytes[8 - i - 1]);
        return byteBuffer.getDouble(0);
    }

    /**
     * reads a null-terminated string
     *
     * @param bytes
     * @return length
     * @throws IOException
     */
    public int readNullTerminatedBytes(byte[] bytes) throws IOException {
        int i = 0;
        while (true) {
            byte letter = (byte) ins.read();
            if (letter == 0)
                return i;
            else
                bytes[i++] = letter;
        }
    }

    /**
     * reads a null-terminated string
     *
     * @return length
     * @throws IOException
     */
    public String readNullTerminatedBytes() throws IOException {
        StringBuilder buf = new StringBuilder();
        while (true) {
            byte letter = (byte) ins.read();
            if (letter == -1)
                throw new IOException("readNullTerminatedBytes(): failed (EOF)");
            if (letter == 0)
                break;
            else
                buf.append((char) letter);
        }
        return buf.toString();
    }

    /**
     * skip a null-terminated string
     *
     * @throws IOException
     */
    public void skipNullTerminatedBytes() throws IOException {
        int letter = 1;
        while (letter != 0) {
            letter = ins.read();
            if (letter == -1)
                throw new IOException("skipNullTerminatedBytes(): failed (EOF)");
        }
    }

    /**
     * reads size-prefixed bytes
     *
     * @param buffer
     * @throws IOException
     */
    public void readSizePrefixedBytes(ByteInputBuffer buffer) throws IOException {
        buffer.setSize(readInt());
        read(buffer.getBytes(), 0, buffer.size());
        buffer.rewind();
    }

    /**
     * skip n bytes
     *
     * @param n
     * @throws IOException
     */
    public void skip(long n) throws IOException {
        seek(getPosition() + n);
    }

    /**
     * get current position
     *
     * @return position
     * @throws IOException
     */
    public long getPosition() throws IOException {
        return ins.getPosition();
    }

    /**
     * close
     *
     * @throws IOException
     */
    public void close() throws IOException {
        ins.close();
    }

    @Override
    public int readChar() throws IOException {
        return 0;
    }

    @Override
    public ByteByteInt readByteByteInt() throws IOException {
        return null;
    }

    @Override
    public String readString() throws IOException {
        ByteInputBuffer buffer = new ByteInputBuffer();
        readSizePrefixedBytes(buffer);
        return buffer.toString();
    }

    @Override
    public int skipBytes(int bytes) throws IOException {
        return ins.skipBytes(bytes);
    }

    @Override
    public long length() throws IOException {
        return ins.length();
    }

    @Override
    public boolean supportsSeek() throws IOException {
        return ins.supportsSeek();
    }

    @Override
    public void seek(long pos) throws IOException {
        ins.seek(pos);
    }
}
