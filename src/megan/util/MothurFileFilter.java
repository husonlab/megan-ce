/*
 * MothurFileFilter.java Copyright (C) 2024 Daniel H. Huson
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

import jloda.swing.util.FileFilterBase;
import jloda.util.FileUtils;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Mothur file filter
 * Daniel Huson         4.2015
 */
public class MothurFileFilter extends FileFilterBase implements FilenameFilter {
    static private MothurFileFilter instance;

    public static MothurFileFilter getInstance() {
        if (instance == null) {
            instance = new MothurFileFilter();
            instance.setAllowGZipped(true);
            instance.setAllowZipped(true);
        }
        return instance;
    }

    private MothurFileFilter() {
        add("txt");
        add("mothur");
    }

    /**
     * @return description of file matching the filter
     */
    public String getBriefDescription() {
        return "Mothur files";
    }

    /**
     * is file acceptable?
     *
     * @return true if acceptable
     */
    @Override
    public boolean accept(File directory, String fileName) {
        if (!super.accept(directory, fileName))
            return false;
		String line = FileUtils.getFirstLineFromFile(new File(fileName));
        return line != null && line.split("\t").length == 2 && line.endsWith(";");
    }
}
