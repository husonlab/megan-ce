/*
 * BlockType.java Copyright (C) 2024 Daniel H. Huson
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

package megan.daa.io;

/**
 * Block type
 * Daniel Huson, 8.2015
 */
public enum BlockType {
    empty, alignments, ref_names, ref_lengths, megan_ref_annotations, megan_classification_key_block, megan_classification_dump_block, megan_aux_data, megan_mate_pair;

    public static byte rank(BlockType type) {
        for (byte i = 0; i < values().length; i++)
            if (values()[i] == type)
                return i;
        return -1;
    }

    public static BlockType value(byte rank) {
        if (rank >= 0 && rank < values().length)
            return values()[rank];
        else
            return empty;
    }
}
