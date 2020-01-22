/*
 * StampFileFilter.java Copyright (C) 2020. Daniel H. Huson
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

import java.io.FilenameFilter;

/**
 * The STAMP file filter
 * Daniel Huson 1.2016
 */
public class StampFileFilter extends FileFilterBase implements FilenameFilter {
    /**
     * constructor
     */
    public StampFileFilter() {
        add("spf");
        add("txt");

    }

    /**
     * @return description of file matching the filter
     */
    public String getBriefDescription() {
        return "STAMP profile files";
    }
}
