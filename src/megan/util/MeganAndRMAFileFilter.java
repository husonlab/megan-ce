/*
 * MeganAndRMAFileFilter.java Copyright (C) 2023 Daniel H. Huson
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

import java.io.FilenameFilter;

/**
 * The megan file filter
 * Daniel Huson         2.2006
 */
public class MeganAndRMAFileFilter extends FileFilterBase implements FilenameFilter {
    public MeganAndRMAFileFilter() {
        add("meg");
        add("megan");
        add("rma");
        add("rma1");
        add("rma2");
        add("rma3");
        add("rma6");
    }

    /**
     * @return description of file matching the filter
     */
    public String getBriefDescription() {
        return "MEGAN files";
    }
}
