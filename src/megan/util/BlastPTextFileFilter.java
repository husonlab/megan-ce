/*
 * BlastPTextFileFilter.java Copyright (C) 2022 Daniel H. Huson
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
 * A BlastText file in text format filter
 * Daniel Huson         4.2015
 */
public class BlastPTextFileFilter extends FileFilterBase implements FilenameFilter {
    static private BlastPTextFileFilter instance;

    public static BlastPTextFileFilter getInstance() {
        if (instance == null) {
            instance = new BlastPTextFileFilter();
            instance.setAllowGZipped(true);
            instance.setAllowZipped(true);
        }
        return instance;
    }

    private BlastPTextFileFilter() {
        add("txt");
        add("blastp");
        add("blastout");
        add("blast");
        add("out");
    }

    /**
     * @return description of file matching the filter
     */
    public String getBriefDescription() {
        return "BLASTP text files";
    }

    /**
     * is file acceptable?
     *
     * @return true if acceptable
     */
    @Override
    public boolean accept(File directory, String fileName) {
        if (fileName.startsWith("!!!")) // always allow this
            return true;
        if (!super.accept(directory, fileName))
            return false;
		String firstLine = FileUtils.getFirstLineFromFile(new File(fileName));
        return firstLine != null && firstLine.startsWith("BLASTP");
    }
}
