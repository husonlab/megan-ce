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


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * class for writing output
 * Daniel Huson, 6.2009, 8.2015
 */
public class OutputWriter implements IOutputWriter, IInputReaderOutputWriter {
    private static final int BUFFER_SIZE = 8192;
    private final BufferedOutputStream outs;
    private long position;

    private final Compressor compressor = new Compressor();
    private byte[] byteBuffer = new byte[1000];
    private boolean useCompression = true;

    /**
     * constructor
     *
     * @param file
     * @throws IOException
     */
    public OutputWriter(File file) throws IOException {
        this.outs = new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE);
        position = 0;
    }

    /**
     * constructor
     *
     * @param file
     * @param append
     * @throws IOException
     */
    public OutputWriter(File file, boolean append) throws IOException {
        this.outs = new BufferedOutputStream(new FileOutputStream(file, append), BUFFER_SIZE);
        position = file.length();
    }

    /**
     * write an int
     *
     * @param a
     * @throws IOException
     */
    public void writeInt(int a) throws IOException {
        outs.write((byte) (a >> 24));
        outs.write((byte) (a >> 16));
        outs.write((byte) (a >> 8));
        outs.write((byte) (a));
        position += 4;
    }

    /**
     * write a char
     *
     * @param a
     * @throws IOException
     */
    public void writeChar(char a) throws IOException {
        outs.write((byte) (a >> 8));
        outs.write((byte) (a));
        position += 2;
    }

    /**
     * write a long
     *
     * @param a
     * @throws IOException
     */
    public void writeLong(long a) throws IOException {
        outs.write((byte) (a >> 56));
        outs.write((byte) (a >> 48));
        outs.write((byte) (a >> 40));
        outs.write((byte) (a >> 32));
        outs.write((byte) (a >> 24));
        outs.write((byte) (a >> 16));
        outs.write((byte) (a >> 8));
        outs.write((byte) (a));
        position += 8;
    }

    /**
     * write a float
     *
     * @param a
     * @throws IOException
     */
    public void writeFloat(float a) throws IOException {
        writeInt(Float.floatToIntBits(a));
    }

    /**
     * write a byte-byte-int
     *
     * @param a
     * @throws IOException
     */
    public void writeByteByteInt(ByteByteInt a) throws IOException {
        outs.write(a.getByte1());
        outs.write(a.getByte2());
        position += 2;
        writeInt(a.getValue());
    }

    /**
     * write a string, compressed, if long enough
     *
     * @param str
     * @throws IOException
     */
    public void writeString(String str) throws IOException {
        if (str == null)
            writeInt(0);
        else {
            if (useCompression && str.length() >= Compressor.MIN_SIZE_FOR_DEFLATION) {
                byte[] bytes = compressor.deflateString2ByteArray(str);
                writeInt(-bytes.length);
                outs.write(bytes, 0, bytes.length);
                position += bytes.length;
            } else {
                byte[] bytes = str.getBytes("UTF-8");
                writeInt(bytes.length);
                outs.write(bytes, 0, bytes.length);
                position += bytes.length;

            }
        }
    }

    /**
     * write a string, compressed, if long enough
     *
     * @param str
     * @param offset
     * @param length
     * @throws IOException
     */
    public void writeString(byte[] str, int offset, int length) throws IOException {
        if (str == null)
            writeInt(0);
        else {
            if (useCompression && length >= Compressor.MIN_SIZE_FOR_DEFLATION) {
                if (byteBuffer.length < length)
                    byteBuffer = new byte[2 * length]; // surely compressed with never be longer than 2*uncompressed
                int numberOfBytes = compressor.deflateString2ByteArray(str, offset, length, byteBuffer);
                writeInt(numberOfBytes);
                outs.write(byteBuffer, 0, Math.abs(numberOfBytes));
                position += Math.abs(numberOfBytes);
            } else {
                writeInt(length);
                outs.write(str, offset, length);
                position += length;
            }
        }
    }

    /**
     * Write a string without compression
     *
     * @param str
     * @throws IOException
     */
    public void writeStringNoCompression(String str) throws IOException {
        if (str == null) {
            writeInt(0);
            //do nothing
        } else {
            writeInt(str.length());
            for (int i = 0; i < str.length(); i++)
                outs.write((byte) str.charAt(i));
            position += str.length();
        }
    }

    /**
     * compress strings?
     *
     * @return true, if strings are compressed
     */
    public boolean isUseCompression() {
        return useCompression;
    }

    /**
     * compress strings?
     *
     * @param useCompression
     */
    public void setUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }

    /**
     * get position file
     *
     * @return position
     * @throws IOException
     */
    public long getPosition() throws IOException {
        return position;
    }

    /**
     * write bytes
     *
     * @param bytes
     * @param offset
     * @param length
     * @throws IOException
     */
    public void write(byte[] bytes, int offset, int length) throws IOException {
        outs.write(bytes, offset, length);
        position += length;
    }

    /**
     * writes bytes
     *
     * @param bytes
     * @throws IOException
     */
    public void write(byte[] bytes) throws IOException {
        outs.write(bytes);
        position += bytes.length;
    }

    /**
     * write a single byte
     *
     * @param a
     * @throws IOException
     */
    public void write(int a) throws IOException {
        outs.write(a);
        position++;
    }

    /**
     * close
     *
     * @throws IOException
     */
    public void close() throws IOException {
        outs.close();
    }

    /**
     * supports seek?
     *
     * @return false
     * @throws IOException
     */
    public boolean supportsSeek() throws IOException {
        return false;
    }

    /**
     * seek to the given position
     *
     * @param pos
     * @throws IOException
     */
    public void seek(long pos) throws IOException {
        throw new IOException("seek(" + pos + "): not supported");
    }

    /**
     * get length of file
     *
     * @return length
     * @throws IOException
     */
    public long length() throws IOException {
        return position;
    }

    @Override
    public int read() throws IOException {
        throw new IOException("Not implemented");
    }

    @Override
    public int read(byte[] bytes, int offset, int len) throws IOException {
        throw new IOException("Not implemented");
    }

    @Override
    public int skipBytes(int bytes) throws IOException {
        throw new IOException("Not implemented");
    }

    @Override
    public void setLength(long length) throws IOException {
        throw new IOException("Not implemented");
    }

    @Override
    public int readInt() throws IOException {
        throw new IOException("Not implemented");
    }

    @Override
    public int readChar() throws IOException {
        throw new IOException("Not implemented");
    }

    @Override
    public long readLong() throws IOException {
        throw new IOException("Not implemented");
    }

    @Override
    public float readFloat() throws IOException {
        throw new IOException("Not implemented");
    }

    @Override
    public ByteByteInt readByteByteInt() throws IOException {
        throw new IOException("Not implemented");
    }

    @Override
    public String readString() throws IOException {
        throw new IOException("Not implemented");
    }
}
