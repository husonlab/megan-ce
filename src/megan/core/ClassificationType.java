/*
 * ClassificationType.java Copyright (C) 2023 Daniel H. Huson
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
package megan.core;

/**
 * all known classifications
 * Daniel Huson, 1.2013
 */
public enum ClassificationType {
    /**
     * standard types of classifications:
     */
    Taxonomy, SEED, KEGG, COG, REFSEQ, PFAM;

    /**
     * gets the short name used in IO
     *
     * @return short name of type
     */
    public static String getShortName(ClassificationType type) {
        if (type == Taxonomy)
            return "TAX";
        else if (type == REFSEQ)
            return "REF";
        else
            return type.toString();
    }

    /**
     * gets the short name used in IO
     *
     * @return short name of type
     */
    public static String getShortName(String fullName) {
        if (fullName.equals(Taxonomy.toString()))
            return "TAX";
        else if (fullName.equals(REFSEQ.toString()))
            return "REF";
        else
            return fullName;
    }

    /**
     * gets the full name used in IO
     *
     * @return short name of type
     */
    public static String getFullName(String shortName) {
        return switch (shortName) {
            case "TAX" -> Taxonomy.toString();
            case "REF" -> REFSEQ.toString();
            default -> shortName;
        };
    }
}
