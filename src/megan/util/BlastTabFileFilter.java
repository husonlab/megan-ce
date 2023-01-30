/*
 * BlastTabFileFilter.java Copyright (C) 2023 Daniel H. Huson
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
public class BlastTabFileFilter extends FileFilterBase implements FilenameFilter {
    static private BlastTabFileFilter instance;

    public static BlastTabFileFilter getInstance() {
        if (instance == null) {
            instance = new BlastTabFileFilter();
            instance.setAllowGZipped(true);
            instance.setAllowZipped(true);
        }
        return instance;
    }

    private BlastTabFileFilter() {
        add("txt");
        add("blastn");
        add("blastx");
        add("blastp");
        add("blastout");
        add("blast");
        add("blasttab");
        add("out");
        add("tab");
        add("tabout");
        add("m8");
    }

    /**
     * @return description of file matching the filter
     */
    public String getBriefDescription() {
        return "BLAST tab files";
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
		String firstLine = FileUtils.getFirstLineFromFile(new File(fileName), "#", 1000);
        return firstLine != null && (firstLine.split("\t").length == 12 || firstLine.split("\t").length == 13);
    }
}
