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


import java.io.File;
import java.io.IOException;
import java.util.zip.DataFormatException;

/**
 * read input and write output
 * Daniel Huson, 6.2009
 */
public class InputOutputReaderWriter implements IInputReaderOutputWriter {
    private final Compressor compressor = new Compressor();
    private boolean useCompression = true;

    private final IInputOutput io;
    private byte[] byteBuffer = new byte[1000];

    private long offset = 0;

    /**
     * constructor.
     *
     * @param io an input-output class
     */
    public InputOutputReaderWriter(IInputOutput io) {
        this.io = io;
    }

    public InputOutputReaderWriter(String file) throws IOException {
        this(file, "r");
        //this(new FileRandomAccessReadWriteAdapter(file));
    }

    public InputOutputReaderWriter(File file, String mode) throws IOException {
        this(file.getPath(), mode);
    }

    public InputOutputReaderWriter(String file, String mode) throws IOException {
        this.io = new FileRandomAccessReadWriteAdapter(file, mode);
    }

    public int readInt() throws IOException {
        return ((io.read()) << 24) + ((io.read() & 0xFF) << 16) + ((io.read() & 0xFF) << 8) + ((io.read() & 0xFF));
    }

    public int readChar() throws IOException {
        return ((io.read() & 0xFF) << 8) + ((io.read() & 0xFF));
    }

    public long readLong() throws IOException {
        return (((long) io.read()) << 56) + (((long) io.read()) << 48) + (((long) io.read()) << 40) + (((long) io.read()) << 32)
                + (((long) io.read()) << 24) + (((long) io.read() & 0xFF) << 16) + (((long) io.read() & 0xFF) << 8) + (((long) io.read() & 0xFF));

    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public ByteByteInt readByteByteInt() throws IOException {
        return new ByteByteInt((byte) io.read(), (byte) io.read(), readInt());
    }

    /**
     * reads an archived string
     *
     * @return string
     * @throws IOException
     * @throws java.util.zip.DataFormatException
     */
    public String readString() throws IOException {
        int size = readInt();
        if (Math.abs(size) > 100000000)
            throw new IOException("Unreasonable string length: " + Math.abs(size));
        byte[] bytes = new byte[Math.abs(size)];
        int got = io.read(bytes, 0, Math.abs(size));
        if (got != Math.abs(size))
            throw new IOException("Bytes read: " + got + ", expected: " + Math.abs(size));

        if (size < 0) // is zip compressed
        {
            try {
                return compressor.inflateByteArray2String(-size, bytes);
            } catch (DataFormatException e) {
                throw new IOException(e.getMessage());
            }
        } else {
            return Compressor.convertUncompressedByteArray2String(size, bytes);
        }
    }

    /**
     * skip some bytes
     *
     * @param bytes
     * @return number of bytes skipped
     * @throws IOException
     */
    public int skipBytes(int bytes) throws IOException {
        return io.skipBytes(bytes);
    }

    public int read() throws IOException {
        return io.read();
    }

    public int read(byte[] bytes, int offset, int len) throws IOException {
        return io.read(bytes, offset, len);
    }

    /**
     * write an int
     *
     * @param a
     * @throws IOException
     */
    public void writeInt(int a) throws IOException {
        io.write((byte) (a >> 24));
        io.write((byte) (a >> 16));
        io.write((byte) (a >> 8));
        io.write((byte) (a));
    }

    /**
     * write a char
     *
     * @param a
     * @throws IOException
     */
    public void writeChar(char a) throws IOException {
        io.write((byte) (a >> 8));
        io.write((byte) (a));
    }

    /**
     * write a long
     *
     * @param a
     * @throws IOException
     */
    public void writeLong(long a) throws IOException {
        io.write((byte) (a >> 56));
        io.write((byte) (a >> 48));
        io.write((byte) (a >> 40));
        io.write((byte) (a >> 32));
        io.write((byte) (a >> 24));
        io.write((byte) (a >> 16));
        io.write((byte) (a >> 8));
        io.write((byte) (a));
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
        io.write(a.getByte1());
        io.write(a.getByte2());
        writeInt(a.getValue());
    }

    /**
     * write a string
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
                io.write(bytes, 0, bytes.length);
            } else {
                byte[] bytes = str.getBytes("UTF-8");
                writeInt(bytes.length);
                io.write(bytes, 0, bytes.length);
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
    @Override
    public void writeString(byte[] str, int offset, int length) throws IOException {
        if (str == null)
            writeInt(0);
        else {
            if (useCompression && length >= Compressor.MIN_SIZE_FOR_DEFLATION) {
                if (byteBuffer.length < length)
                    byteBuffer = new byte[2 * length]; // surely compressed with never be longer than 2*uncompressed
                int numberOfBytes = compressor.deflateString2ByteArray(str, offset, length, byteBuffer);
                writeInt(numberOfBytes);
                io.write(byteBuffer, 0, Math.abs(numberOfBytes));
            } else {
                writeInt(length);
                io.write(str, offset, length);
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
                io.write((byte) str.charAt(i));
        }
    }

    public long getPosition() throws IOException {
        return io.getPosition() - offset;
    }

    public void write(byte[] bytes, int offset, int length) throws IOException {
        io.write(bytes, offset, length);
    }

    public void write(byte[] bytes) throws IOException {
        io.write(bytes, 0, bytes.length);
    }

    public void write(int a) throws IOException {
        io.write(a);
    }

    public void close() throws IOException {
        io.close();
    }

    public long length() throws IOException {
        return io.length() + offset;
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

    public void setLength(long length) throws IOException {
        io.setLength(length);
    }

    /**
     * get the offset that is applied to all seek() calls
     *
     * @return offest
     */
    public long getOffset() {
        return offset;
    }

    /**
     * set offset that is applied to all seek() calls
     *
     * @param offset
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }

    public void seekToEnd() throws IOException {
        io.seek(io.length());
    }

    public boolean supportsSeek() throws IOException {
        return io.supportsSeek();
    }

    public void seek(long pos) throws IOException {
        io.seek(pos + offset);
    }
}
