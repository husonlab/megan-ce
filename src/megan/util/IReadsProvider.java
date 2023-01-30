/*
 * IReadsProvider.java Copyright (C) 2023 Daniel H. Huson
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

package megan.util;

import jloda.util.Pair;

import java.util.Collection;

/**
 * A class provides reads reads
 * Created by huson on 3/13/17.
 */
public interface IReadsProvider {
    /**
     * are any reads available
     *
     * @return true, if read currently available
     */
    boolean isReadsAvailable();

    /**
     * get the provided reads
     *
     * @param maxNumber maximum number of reads to get
     * @return reads
     */
    Collection<Pair<String, String>> getReads(int maxNumber);
}
