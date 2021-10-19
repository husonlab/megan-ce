/*
 * SAMFileFilter.java Copyright (C) 2021. Daniel H. Huson
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
package megan.util;

import jloda.swing.util.FileFilterBase;
import jloda.util.FileUtils;

import java.io.File;
import java.io.FilenameFilter;

/**
 * A sam file filter
 * Daniel Huson         10.2014
 */
public class SAMFileFilter extends FileFilterBase implements FilenameFilter {
    static private SAMFileFilter instance;

    public static SAMFileFilter getInstance() {
        if (instance == null) {
            instance = new SAMFileFilter();
            instance.setAllowGZipped(true);
            instance.setAllowZipped(true);
        }
        return instance;
    }

    private SAMFileFilter() {
        add("sam");
    }

    /**
     * @return description of file matching the filter
     */
    public String getBriefDescription() {
        return "SAM files";
    }

    /**
     * is file acceptable?
     *
     * @param directory
     * @param fileName
     * @return true if acceptable
     */
    @Override
    public boolean accept(File directory, String fileName) {
        if (!super.accept(directory, fileName))
            return false;
		String firstLine = FileUtils.getFirstLineFromFile(new File(fileName));
        return firstLine != null && (firstLine.startsWith("@HD") || firstLine.startsWith("@PG") || firstLine.startsWith("@SQ"));
    }
}
