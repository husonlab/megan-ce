/*
 * AlignMode.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.seq.BlastMode;

/**
 * alignment mode enum
 * Daniel Huson, 8.2015
 */
public enum AlignMode {
	blastp, blastx, blastn;

	public static byte rank(AlignMode mode) {
        return switch (mode) {
            case blastp -> 2;
            case blastx -> 3;
            case blastn -> 4;
            default -> -1;
        };
    }

    public static AlignMode value(int rank) {
        return switch (rank) {
            case 2 -> blastp;
            case 3 -> blastx;
            case 4 -> blastn;
        };
    }

	public static BlastMode getBlastMode(int rank) {
        return switch (rank) {
            case 2 -> BlastMode.BlastP;
            case 3 -> BlastMode.BlastX;
            case 4 -> BlastMode.BlastN;
        };

    }
}
