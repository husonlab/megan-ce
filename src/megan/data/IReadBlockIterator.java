/*
 * IReadBlockIterator.java Copyright (C) 2023 Daniel H. Huson
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
package megan.data;

import jloda.util.ICloseableIterator;

import java.io.Closeable;

/**
 * Iterator over a set of read blocks
 * Daniel Huson, 4.2010
 */
public interface IReadBlockIterator extends ICloseableIterator<IReadBlock>, Closeable {
    /**
     * get a string reporting stats
     *
     * @return stats string
     */
    String getStats();
}
