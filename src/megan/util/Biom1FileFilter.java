/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.util;

import jloda.util.Basic;
import jloda.util.FileFilterBase;

import java.io.File;
import java.io.FilenameFilter;

/**
 * The biome file filter
 * Daniel Huson         9.2012
 */
public class Biom1FileFilter extends FileFilterBase implements FilenameFilter {
    /**
     * constructor
     */
    public Biom1FileFilter() {
        add("biom");
        add("biom1");
        add("txt");

    }

    /**
     * @return description of file matching the filter
     */
    public String getBriefDescription() {
        return "BIOM files";
    }

    @Override
    public boolean accept(File dir, String name) {
        if (super.accept(dir, name)) { // ensure that file is not biom2 format...
            final byte[] bytes = Basic.getFirstBytesFromFile(new File(dir, name), 4);
            return bytes != null && bytes[0] == '{' && !Basic.toString(bytes).contains("ﾉHDF");
        }
        return false;
    }
}
