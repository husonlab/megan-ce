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

import java.io.IOException;
import java.io.Writer;

/**
 * human readable output writer for debugging
 * daniel HUson, 5.2015
 */
public class OutputWriterHumanReadable implements IOutputWriter {
    private final Writer w;
    private long pos = 0;

    /**
     * constructor
     *
     * @param w
     */
    public OutputWriterHumanReadable(Writer w) {
        this.w = w;
    }

    @Override
    public void writeInt(int a) throws IOException {
        writeString(String.format("%d", a));
    }

    @Override
    public void writeChar(char a) throws IOException {
        writeString(String.format("%c", a));

    }

    @Override
    public void write(int a) throws IOException {
        writeString(String.format("%c", (char) a));
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        writeString(Basic.toString(bytes, offset, length));
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        writeString(Basic.toString(bytes));
    }

    @Override
    public void writeLong(long a) throws IOException {
        writeString(String.format("%d", a));
    }

    @Override
    public void writeFloat(float a) throws IOException {
        writeString(String.format("%g", a));
    }

    @Override
    public void writeByteByteInt(ByteByteInt a) throws IOException {
        writeString(a.toString());
    }

    @Override
    public void writeString(String str) throws IOException {
        w.write(str + "\n");
        if (str != null)
            pos += str.length() + 1;
    }

    @Override
    public void writeString(byte[] str, int offset, int length) throws IOException {
        for (int i = 0; i < length; i++)
            w.write("" + str[i]);
        w.write("\n");
        pos += length;
    }

    @Override
    public void writeStringNoCompression(String str) throws IOException {
        writeString(str);
    }

    @Override
    public long length() throws IOException {
        return pos;
    }

    @Override
    public long getPosition() throws IOException {
        return pos;
    }

    @Override
    public boolean isUseCompression() {
        return false;
    }

    @Override
    public void setUseCompression(boolean use) {

    }

    @Override
    public void close() throws IOException {
        w.close();
    }

    public String toString() {
        return w.toString();
    }
}
