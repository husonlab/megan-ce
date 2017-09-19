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
 * The biom file filter
 * Daniel Huson 9.2017
 */
public class BiomFileFilter extends FileFilterBase implements FilenameFilter {
    public enum Version {V1, V2, All}

    private final Version version;

    /**
     * default constructor. Accepts all versions
     */
    public BiomFileFilter() {
        this(Version.All);
    }

    /**
     * constructor
     * @param v version to accept
     */
    public BiomFileFilter(Version v) {
        this.version = v;
        add("biom");
        if (version == Version.V1 || version == Version.All) {
            add("biom1");
            //add("txt");
        }
        if (version == Version.V2 || version == Version.All)
            add("biom2");
    }

    /**
     * @return description of file matching the filter
     */
    public String getBriefDescription() {
        return "BIOM files";
    }

    @Override
    public boolean accept(File dir, String name) {
        if (super.accept(dir, name)) {
            final byte[] bytes = Basic.getFirstBytesFromFile(new File(dir, name), 4);
            boolean isV2 = (bytes != null && bytes[0] != '{' && Basic.toString(bytes).contains("ﾉHDF"));
            return (version == Version.All || (version == Version.V1 && !isV2) || (version == Version.V2 && isV2));
        }
        return false;
    }

    public static boolean isBiom1File(String file) {
        return new BiomFileFilter(Version.V1).accept(file);
    }

    public static boolean isBiom2File(String file) {
        return new BiomFileFilter(Version.V2).accept(file);
    }

    public static boolean isBiomFile(String file) {
        return new BiomFileFilter(Version.All).accept(file);
    }
}
