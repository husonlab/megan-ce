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

import jloda.util.Basic;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.zip.DataFormatException;

/**
 * a class to access input with, memory-mapped, if possible
 * Daniel Huson, 2.2011
 */
public class InputReader implements IInputReader {
    private final boolean useAbsoluteFilePositions;
    private final Compressor compressor = new Compressor();
    private final FileChannel channel;
    private final long start;
    private final long length;
    private final IInput in;

    /**
     * constructor from input class
     *
     * @param in an input input class
     */
    public InputReader(IInput in) throws IOException {
        this.in = in;
        start = 0;
        useAbsoluteFilePositions = true;
        length = this.in.length();
        channel = null;
    }

    /**
     * constructor
     *
     * @param file
     * @throws IOException
     */
    public InputReader(File file) throws IOException {
        this(file, null, null, true);
    }

    /**
     * constructor
     *
     * @param file                     file to read
     * @param start                    start position in file or null
     * @param end                      end position in file or null
     * @param useAbsoluteFilePositions use absolute positions in file rather than relative to start
     * @throws IOException
     */
    public InputReader(File file, Long start, Long end, boolean useAbsoluteFilePositions) throws IOException {
        if (start == null)
            start = 0L;
        if (end == null) {
            // need to grab the true length from here: (not from File.length())
            RandomAccessFile fr = new RandomAccessFile(file, "r");
            end = fr.length();
            fr.close();
        }
        this.start = start;

        // System.err.println("InputReader(file=" + file.get() + ",start=" + start + ",end=" + end + ",useAbs=" + useAbsoluteFilePositions + ",mapped=" + mapped + ")");

        this.useAbsoluteFilePositions = useAbsoluteFilePositions;
        try {
            FileRandomAccessReadOnlyAdapter in = new FileRandomAccessReadOnlyAdapter(file);
            channel = in.getChannel();
            this.in = in;
            if (useAbsoluteFilePositions) {
                length = end;
                seek(start);
            } else {
                length = end - start;
                seek(0);
            }
        } catch (IOException ex) {
            Basic.caught(ex);
            throw ex;
        }
    }

    public int readInt() throws IOException {
        return ((in.read()) << 24) | ((in.read() & 0xFF) << 16) | ((in.read() & 0xFF) << 8) | ((in.read() & 0xFF));
    }

    public int readChar() throws IOException {
        return ((in.read() & 0xFF) << 8) | ((in.read() & 0xFF));
    }

    public long readLong() throws IOException {
        return (((long) in.read()) << 56) | (((long) in.read()) << 48) | (((long) in.read()) << 40) | (((long) in.read()) << 32)
                | (((long) in.read()) << 24) | (((long) in.read() & 0xFF) << 16) | (((long) in.read() & 0xFF) << 8) | (((long) in.read() & 0xFF));

    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * read bytes until next end of line
     *
     * @return
     * @throws IOException
     */
    public String readLine() throws IOException {
        StringBuilder buf = new StringBuilder();
        byte b = (byte) read();
        while (b != '\n') {
            buf.append((char) b);
            b = (byte) in.read();
            if (getPosition() >= length())
                break;
        }
        return buf.toString();
    }

    public ByteByteInt readByteByteInt() throws IOException {
        return new ByteByteInt((byte) in.read(), (byte) in.read(), readInt());
    }

    /**
     * reads an archived string
     *
     * @return string
     * @throws java.io.IOException
     * @throws java.util.zip.DataFormatException
     */
    public String readString() throws IOException {
        int size = readInt();
        if (Math.abs(size) > 100000000)
            throw new IOException("Unreasonable string length: " + Math.abs(size));
        byte[] bytes = new byte[Math.abs(size)];
        int got = in.read(bytes, 0, Math.abs(size));
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
     * reads an archived string
     *
     * @return string
     * @throws java.io.IOException
     * @throws java.util.zip.DataFormatException
     */
    public int readString(byte[] tmp, byte[] target) throws IOException {
        int size = readInt();
        if (size > 0) {
            if (size > target.length)
                throw new IOException("Unreasonable string length: " + size);
            return in.read(target, 0, size);
        } else {
            size = -size;
            if (size > tmp.length)
                throw new IOException("Unreasonable string length: " + size);
            int got = in.read(tmp, 0, size);
            if (got != size)
                throw new IOException("Bytes read: " + got + ", expected: " + size);
            try {
                return compressor.inflateByteArray(size, tmp, target);
            } catch (DataFormatException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    /**
     * skip some bytes
     *
     * @param bytes
     * @return number of bytes skipped
     * @throws java.io.IOException
     */
    public int skipBytes(int bytes) throws IOException {
        return in.skipBytes(bytes);
    }

    public boolean supportsSeek() throws IOException {
        return in.supportsSeek();
    }

    public void seek(long pos) throws IOException {
        if (useAbsoluteFilePositions)
            in.seek(pos);
        else
            in.seek(pos + start);
    }

    public long length() throws IOException {
        return length;
    }

    public int read() throws IOException {
        return in.read();
    }

    public int read(byte[] bytes, int offset, int len) throws IOException {
        return in.read(bytes, offset, len);
    }

    public long getPosition() throws IOException {
        if (useAbsoluteFilePositions)
            return in.getPosition();
        else
            return in.getPosition() - start;
    }

    public void close() throws IOException {
        in.close();
    }

    public FileChannel getChannel() {
        return channel;
    }

    public boolean isUseAbsoluteFilePositions() {
        return useAbsoluteFilePositions;
    }
}

