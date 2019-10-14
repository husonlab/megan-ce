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

package megan.daa.io;


import jloda.util.Basic;
import megan.io.FileInputStreamAdapter;
import megan.io.FileOutputStreamAdapter;
import megan.io.IOutput;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * write data in little endian format
 * Daniel Huson, 8.2015
 */
public class OutputWriterLittleEndian implements Closeable {
    private final IOutput outs;
    private final byte[] bytes = new byte[8];
    private final java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.allocate(8);

    /**
     * constructor
     *
     * @param outs
     */
    public OutputWriterLittleEndian(IOutput outs) {
        this.outs = outs;
    }

    /**
     * write a byte
     *
     * @return byte
     * @throws IOException
     */
    public void write(int b) throws IOException {
        outs.write(b);
    }

    /**
     * write bytes
     *
     * @param bytes
     * @param offset
     * @param len
     * @return bytes
     * @throws IOException
     */
    public void write(byte[] bytes, int offset, int len) throws IOException {
        outs.write(bytes, offset, len);
    }

    /**
     * write char, little endian
     *
     * @return int
     * @throws IOException
     */
    public void writeChar(char a) throws IOException {
        outs.write((byte) (a));
        outs.write((byte) (a >> 8));
    }

    /**
     * write int, little endian
     *
     * @return int
     * @throws IOException
     */
    public void writeInt(int a) throws IOException {
        outs.write((byte) (a));
        outs.write((byte) (a >> 8));
        outs.write((byte) (a >> 16));
        outs.write((byte) (a >> 24));
    }

    /**
     * write long, little endian
     *
     * @return long
     * @throws IOException
     */
    public void writeLong(long a) throws IOException {
        outs.write((byte) (a));
        outs.write((byte) (a >> 8));
        outs.write((byte) (a >> 16));
        outs.write((byte) (a >> 24));
        outs.write((byte) (a >> 32));
        outs.write((byte) (a >> 40));
        outs.write((byte) (a >> 48));
        outs.write((byte) (a >> 56));
    }

    /**
     * write float, little endian
     *
     * @throws IOException
     */
    private void writeFloat(float a) throws IOException {
        byteBuffer.putFloat(0, a);
        byteBuffer.rewind();
        byteBuffer.get(bytes, 0, 4);
        swap(bytes, 4);
        outs.write(bytes, 0, 4);
    }

    /**
     * read double, little endian
     *
     * @return double
     * @throws IOException
     */
    public void writeDouble(double a) throws IOException {
        byteBuffer.putDouble(0, a);
        byteBuffer.rewind();
        byteBuffer.get(bytes, 0, 8);
        swap(bytes, 8);
        outs.write(bytes, 0, 8);
    }

    /**
     * swap order of bytes
     *
     * @param bytes
     * @param len
     */
    private void swap(byte[] bytes, int len) {
        int top = len / 2;
        int j = len - 1;
        for (int i = 0; i < top; i++, j--) {
            byte b = bytes[i];
            bytes[i] = bytes[j];
            bytes[j] = b;
        }
    }

    /**
     * write bytes as a null-terminated string
     *
     * @param bytes
     * @return length
     * @throws IOException
     */
    public void writeNullTerminatedString(byte[] bytes) throws IOException {
        int pos = 0;
        while (pos < bytes.length) {
            if (bytes[pos] == 0)
                break;
            pos++;
        }
        if (pos > 0)
            write(bytes, 0, pos);
        write((byte) 0);
    }

    /**
     * write size-prefixed bytes
     *
     * @throws IOException
     */
    public void writeSizedPrefixedBytes(byte[] bytes) throws IOException {
        writeSizedPrefixedBytes(bytes, 0, bytes.length);
    }

    /**
     * write size-prefixed bytes
     *
     * @throws IOException
     */
    private void writeSizedPrefixedBytes(byte[] bytes, int offset, int length) throws IOException {
        writeInt(length);
        write(bytes, offset, length);
    }

    /**
     * get current position
     *
     * @return position
     * @throws IOException
     */
    public long getPosition() throws IOException {
        return outs.getPosition();
    }

    /**
     * close
     *
     * @throws IOException
     */
    public void close() throws IOException {
        outs.close();
    }

    public static void main(String[] args) throws IOException {
        FileOutputStreamAdapter outs = new FileOutputStreamAdapter(new File("/tmp/xxx"));

        try (OutputWriterLittleEndian w = new OutputWriterLittleEndian(outs)) {
            w.writeInt(1234567);
            w.writeLong(123456789123456789L);
            w.writeFloat(9999999);
            w.writeDouble(4E09);
            w.writeNullTerminatedString("HELLO".getBytes());
            w.writeSizedPrefixedBytes("?AGAIN?".getBytes(), 1, 5);
        }

        FileInputStreamAdapter ins = new FileInputStreamAdapter("/tmp/xxx");
        try (InputReaderLittleEndian r = new InputReaderLittleEndian(ins)) {
            System.err.println(r.readInt());
            System.err.println(r.readLong());
            System.err.println(r.readFloat());
            System.err.println(r.readDouble());
            byte[] bytes = new byte[100];
            int len = r.readNullTerminatedBytes(bytes);
            System.err.println(Basic.toString(bytes, 0, len));
            ByteInputBuffer buffer = new ByteInputBuffer();
            r.readSizePrefixedBytes(buffer);
            System.err.println(buffer.toString());
        }
    }
}
