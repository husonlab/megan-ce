/*
 * ReadAssignmentCalculator.java Copyright (C) 2023 Daniel H. Huson
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

import jloda.swing.util.ProgramProperties;
import jloda.util.interval.IntervalTree;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.util.ReadMagnitudeParser;

/**
 * calcuates the values that appear is assigned reads in tree representations
 * Daniel Huson, 4.2107
 */
public class ReadAssignmentCalculator {
    private final Document.ReadAssignmentMode mode;

    public ReadAssignmentCalculator(Document.ReadAssignmentMode mode) {
        this.mode = mode;
        ReadMagnitudeParser.setUnderScoreEnabled(ProgramProperties.get("allow-read-weights-underscore", false));
    }

    /**
     * compute the assignment value for this read
     *
     * @return assignment value
     */
    public int compute(IReadBlock readBlock, IntervalTree<Object> intervals) {
        return switch (mode) {
            default /* case readCount */ -> 1;
            case readLength -> Math.max(1, readBlock.getReadLength());
            case alignedBases -> computeCoveredBases(readBlock, intervals);
            case readMagnitude -> ReadMagnitudeParser.parseMagnitude(readBlock.getReadHeader(), true);
        };
    }

    /**
     * computes the number of bases covered by any alignments
     *
     * @return covered bases
     */
    private static int computeCoveredBases(IReadBlock readBlock, IntervalTree<Object> intervals) {
        if (intervals == null)
            intervals = new IntervalTree<>();
        else
            intervals.clear();
        for (int m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
            intervals.add(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock);
        }
        return intervals.getCovered();
    }
}
