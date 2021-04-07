/*
 * FileRandomAccessReadWriteAdapter.java Copyright (C) 2021. Daniel H. Huson
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * wrapper from random access read-write file
 * Daniel Huson, 6.2009
 */
public class FileRandomAccessReadWriteAdapter extends RandomAccessFile implements IInputOutput, IOutput {

    public FileRandomAccessReadWriteAdapter(String file, String mode) throws FileNotFoundException {
        super(file, mode);
    }

    public long getPosition() throws IOException {
        return getChannel().position();
    }

    public boolean supportsSeek() {
        return true;
    }


}
