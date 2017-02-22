/*
 *  Copyright (C) 2015 Daniel H. Huson
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
 * A last file in MAF format filter
 * Daniel Huson         4.2015
 */
public class LastMAFFileFilter extends FileFilterBase implements FilenameFilter {
    static private LastMAFFileFilter instance;

    public static LastMAFFileFilter getInstance() {
        if (instance == null) {
            instance = new LastMAFFileFilter();
            instance.setAllowGZipped(true);
            instance.setAllowZipped(true);
        }
        return instance;
    }

    private LastMAFFileFilter() {
        add("txt");
        add("lastout");
        add("last");
        add("out");
        add("maf");
    }

    /**
     * @return description of file matching the filter
     */
    public String getBriefDescription() {
        return "Last MAF files";
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
        if (fileName.startsWith("!!!")) // always allow this
            return true;
        if (!super.accept(directory, fileName))
            return false;
        String firstLine = Basic.getFirstLineFromFile(new File(directory, fileName));
        return firstLine != null && firstLine.startsWith("# LAST");
    }
}
