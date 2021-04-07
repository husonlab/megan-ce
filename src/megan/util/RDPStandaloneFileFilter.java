/*
 * RDPStandaloneFileFilter.java Copyright (C) 2021. Daniel H. Huson
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
import jloda.util.Basic;

import java.io.File;
import java.io.FilenameFilter;

/**
 * A RDP standalone file in text format filter
 * Daniel Huson         4.2015
 */
public class RDPStandaloneFileFilter extends FileFilterBase implements FilenameFilter {
    static private RDPStandaloneFileFilter instance;

    public static RDPStandaloneFileFilter getInstance() {
        if (instance == null) {
            instance = new RDPStandaloneFileFilter();
            instance.setAllowGZipped(true);
            instance.setAllowZipped(true);
        }
        return instance;
    }

    private RDPStandaloneFileFilter() {
        add("txt");
        add("rdp");
    }

    /**
     * @return description of file matching the filter
     */
    public String getBriefDescription() {
        return "RDP standalone files";
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
        final String firstLine = Basic.getFirstLineFromFile(new File(fileName));
        if (firstLine != null) { // need to have at least 5 tokens and the 5-th must be a float <=1
            final String[] tokens = firstLine.split("\t");
            return tokens.length >= 5 && Basic.isFloat(tokens[4]) && Basic.parseFloat(tokens[4]) <= 1f;
        }
        return false;
    }
}
