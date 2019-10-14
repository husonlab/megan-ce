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
import jloda.util.ProgressPercentage;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Base for memory-mapped file-based getters or putters
 * Daniel Huson, 4.2015
 */
public abstract class BaseFileGetterPutter {

    protected enum Mode {READ_ONLY, READ_WRITE, CREATE_READ_WRITE, CREATE_READ_WRITE_IN_MEMORY}

    static final int BITS = 30;

    static final int BLOCK_SIZE = (1 << BITS);
    static final long BIT_MASK = (BLOCK_SIZE - 1L);
    final ByteBuffer[] buffers;
    private final FileChannel fileChannel;
    final long fileLength;
    private final File file;

    private final boolean inMemory;

    /**
     * opens the named file as READ_ONLY
     *
     * @param file
     * @throws IOException
     */
    protected BaseFileGetterPutter(File file) throws IOException {
        this(file, 0, Mode.READ_ONLY);
    }

    /**
     * constructor
     *
     * @param file
     * @param fileLength length of file to be created when mode==CREATE_READ_WRITE, otherwise ignored
     * @throws java.io.IOException
     */
    protected BaseFileGetterPutter(File file, long fileLength, Mode mode) throws IOException {
        System.err.println("Opening file: " + file);

        this.file = file;
        this.inMemory = (mode == Mode.CREATE_READ_WRITE_IN_MEMORY);

        final FileChannel.MapMode fileChannelMapMode;

        switch (mode) {
            case CREATE_READ_WRITE_IN_MEMORY:
            case CREATE_READ_WRITE: {
                // create the file and ensure that it has the given size
                final RandomAccessFile raf = new RandomAccessFile(file, "rw");
                try {
                    raf.setLength(fileLength);
                } catch (IOException ex) {
                    System.err.println("Attempted file size: " + fileLength);
                    throw ex;
                }
                raf.close();
                // fall through to READ_WRITE case....
            }
            case READ_WRITE: {
                fileChannel = (new RandomAccessFile(file, "rw")).getChannel();
                fileChannelMapMode = FileChannel.MapMode.READ_WRITE;
                break;
            }
            default:
            case READ_ONLY: {
                fileChannel = (new RandomAccessFile(file, "r")).getChannel();
                fileChannelMapMode = FileChannel.MapMode.READ_ONLY;
                break;
            }
        }
        // determine file size
        {
            if (!file.exists())
                throw new IOException("No such file: " + file);
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                this.fileLength = raf.length();
            }
            if (fileLength > 0 && this.fileLength != fileLength) {
                throw new IOException("File length expected: " + fileLength + ", got: " + this.fileLength);
            }
        }

        if (inMemory)
            System.err.println("Allocating: " + Basic.getMemorySizeString(this.fileLength));

        final List<ByteBuffer> list = new LinkedList<>();
        long blockNumber = 0;

        while (true) {
            long start = blockNumber * BLOCK_SIZE;
            long remaining = Math.min(BLOCK_SIZE, this.fileLength - start);
            if (remaining > 0) {
                if (inMemory) {
                    try {
                        ByteBuffer buffer = ByteBuffer.allocate((int) remaining);
                        list.add(buffer);
                    } catch (Exception ex) {
                        System.err.println("Memory allocation failed");
                        System.exit(1);
                    }
                } else
                    list.add(fileChannel.map(fileChannelMapMode, start, remaining));
            } else
                break;
            blockNumber++;
        }
        buffers = list.toArray(new ByteBuffer[0]);
        if (mode == Mode.CREATE_READ_WRITE)
            erase(); // clear the file
        // System.err.println("Buffers: " + buffers.length);
    }

    static int getWhichBuffer(long filePos) {
        return (int) (filePos >>> BITS);
    }

    static int getIndexInBuffer(long filePos) {
        return (int) (filePos & BIT_MASK);
    }

    /**
     * close the file
     *
     * @throws java.io.IOException
     */
    public void close() {
        try {
            if (inMemory) {
                fileChannel.close();
                final ProgressPercentage progress = new ProgressPercentage("Writing file: " + file, buffers.length);
                try (OutputStream outs = new BufferedOutputStream(new FileOutputStream(file), 1024000)) {
                    {
                        long total = 0;
                        for (ByteBuffer buffer : buffers)
                            total += buffer.limit();

                        System.err.println("Size: " + Basic.getMemorySizeString(total));
                    }

                    for (ByteBuffer buffer : buffers) {
                        // fileChannel.write(buffer); // only use this if buffer is direct because otherwise a direct copy is constructed during write
                        buffer.position(0);
                        while (true) {
                            try {
                                outs.write(buffer.get());
                            } catch (BufferUnderflowException | IndexOutOfBoundsException e) {
                                break; // buffer.get() also checks limit, so no need for us to check limit...
                            }
                        }
                        progress.incrementProgress();
                    }
                }
                progress.close();
            }
            Arrays.fill(buffers, null);
            fileChannel.close();
        } catch (IOException e) {
            Basic.caught(e);
        }
    }

    /**
     * erase file by setting all bytes to 0
     */
    private void erase() {
        byte[] bytes = null;
        for (ByteBuffer buffer : buffers) {
            if (bytes == null || bytes.length < buffer.limit())
                bytes = new byte[buffer.limit()];
            buffer.position(0);
            buffer.put(bytes, 0, buffer.limit());
            buffer.position(0);
        }
    }

    /**
     * length of array
     *
     * @return array length
     * @throws java.io.IOException
     */
    abstract public long limit();

    /**
     * resize a file and fill new bytes with zeros
     *
     * @param file
     * @param newLength
     * @throws java.io.IOException
     */
    static void resize(File file, long newLength) throws IOException {
        final long oldLength;
        {
            final RandomAccessFile raf = new RandomAccessFile(file, "r");
            oldLength = raf.length();
            raf.close();
        }

        if (newLength < oldLength) {
            final RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.setLength(newLength);
            raf.close();
        } else if (newLength > oldLength) {
            final BufferedOutputStream outs = new BufferedOutputStream(new FileOutputStream(file, true));
            long count = newLength - oldLength;
            while (--count >= 0) {
                outs.write(0);
            }
            outs.close();
            long theLength;
            {
                final RandomAccessFile raf = new RandomAccessFile(file, "r");
                theLength = raf.length();
                raf.close();
            }
            if (theLength != newLength)
                throw new IOException("Failed to resize file to length: " + newLength + ", length is: " + theLength);
        }
    }
}
