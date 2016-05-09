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

import jloda.util.FileFilterBase;
import megan.daa.io.DAAParser;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * A DAA file filter
 * Daniel Huson         18.2015
 */
public class DAAFileFilter extends FileFilterBase implements FilenameFilter {
    static private DAAFileFilter instance;

    public static DAAFileFilter getInstance() {
        if (instance == null) {
            instance = new DAAFileFilter();
            instance.setAllowGZipped(true);
            instance.setAllowZipped(true);
        }
        return instance;
    }

    private DAAFileFilter() {
        add("daa");
    }

    /**
     * @return description of file matching the filter
     */
    public String getBriefDescription() {
        return "DAA files";
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
        try {
            return DAAParser.isMeganizedDAAFile((new File(directory, fileName)).getPath(), false);
        } catch (IOException e) {
            return false;
        }
    }
}
