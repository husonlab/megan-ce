/*
 * BlastXMLFileFilter.java Copyright (C) 2022 Daniel H. Huson
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
public class BlastXMLFileFilter extends FileFilterBase implements FilenameFilter {
    static private BlastXMLFileFilter instance;

    public static BlastXMLFileFilter getInstance() {
        if (instance == null) {
            instance = new BlastXMLFileFilter();
            instance.setAllowGZipped(true);
            instance.setAllowZipped(true);
        }
        return instance;
    }

    private BlastXMLFileFilter() {
        add("xml");
        add("blastxml");
        add("txt");
    }


    /**
     * @return description of file matching the filter
     */
    public String getBriefDescription() {
        return "BLAST XML files";
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

		String[] lines = FileUtils.getFirstLinesFromFile(new File(fileName), 2);
        return lines != null && lines[0] != null && lines[1] != null && lines[0].startsWith("<?xml") && (lines[1].startsWith("<!DOCTYPE BlastOutput") || lines[1].startsWith("<BlastOutput>"));
    }
}
