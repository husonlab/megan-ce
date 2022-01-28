/*
 * TextStoragePolicy.java Copyright (C) 2022 Daniel H. Huson
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
package megan.data;

import jloda.util.NumberUtils;

/**
 * Constants for the different storage policies
 * Daniel Huson, 10.2010
 */
public enum TextStoragePolicy {
    Embed, Reference, InRMAZ;

    public static String getDescription(TextStoragePolicy policy) {
        return switch (policy) {
            case Embed -> "Embed matches and reads in MEGAN file";
            case Reference -> "Save references to original files for lookup of matches and reads";
            case InRMAZ -> "Save matches and reads in an auxiliary .rmaz file";
            default -> "Unknown";
        };
    }

    /**
     * get value of label ignoring case
     *
     * @param label
     * @return value
     */
    public static TextStoragePolicy valueOfIgnoreCase(String label) {
		for (TextStoragePolicy policy : values()) {
			if (label.equalsIgnoreCase(policy.toString()))
				return policy;
		}
		if (NumberUtils.isInteger(label))
			return fromId(NumberUtils.parseInt(label));
		else
			return null;
	}

    /**
     * gets an id
     *
     * @param policy
     * @return
     */
    public static int getId(TextStoragePolicy policy) {
        return switch (policy) {
            case Embed -> 0;
            case Reference -> 1;
            case InRMAZ -> 2;
            default -> -1;
        };
    }

    public static TextStoragePolicy fromId(int id) {
        return switch (id) {
            case 0 -> Embed;
            case 1 -> Reference;
            case 2 -> InRMAZ;
            default -> null;
        };
    }
}
