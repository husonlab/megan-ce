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
package megan.rma3;

import megan.io.InputReader;
import megan.rma2.RMA2File;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * RMA3 file filter
 * Created by huson on 10/3/14.
 */
public class RMA3FileFilter implements FileFilter {
    private static RMA3FileFilter instance;

    /**
     * gets an instance
     *
     * @return instance
     */
    public static RMA3FileFilter getInstance() {
        if (instance == null)
            instance = new RMA3FileFilter();
        return instance;
    }

    /**
     * Tests whether or not the specified abstract pathname should be
     * included in a pathname list.
     *
     * @param pathname The abstract pathname to be tested
     * @return <code>true</code> if and only if <code>pathname</code>
     * should be included
     */
    @Override
    public boolean accept(File pathname) {
        try {

            try (InputReader r = new InputReader(pathname, null, null, true)) {
                return r.readInt() == RMA2File.MAGIC_NUMBER && r.readInt() == 3;
            }

        } catch (IOException e) {
            return false;
        }
    }
}
