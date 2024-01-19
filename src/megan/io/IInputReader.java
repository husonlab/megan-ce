/*
 * IInputReader.java Copyright (C) 2024 Daniel H. Huson
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
package megan.io;

import java.io.Closeable;
import java.io.IOException;

/**
 * common interface for both InputReader and InputOutputReaderWriter
 * Daniel Huson, 8.2008
 */
public interface IInputReader extends Closeable {

    int readInt() throws IOException;

    int readChar() throws IOException;

    int read() throws IOException;

    int read(byte[] bytes, int offset, int len) throws IOException;

    long readLong() throws IOException;

    float readFloat() throws IOException;

    ByteByteInt readByteByteInt() throws IOException;

    String readString() throws IOException;

    int skipBytes(int bytes) throws IOException;

    long length() throws IOException;

    long getPosition() throws IOException;

    boolean supportsSeek() throws IOException;

    void seek(long pos) throws IOException;
}
