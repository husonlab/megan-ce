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

import java.io.IOException;

/**
 * interface for read-write access
 * Daniel Huson, 6.2009
 */
interface IInputOutput extends IBaseIO {
    int read() throws IOException;

    int read(byte[] bytes, int offset, int len) throws IOException;

    void write(int a) throws IOException;

    void write(byte[] bytes, int offset, int length) throws IOException;

    int skipBytes(int bytes) throws IOException;

    void setLength(long length) throws IOException;
}
