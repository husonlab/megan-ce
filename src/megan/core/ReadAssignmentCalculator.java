/*
 *  Copyright (C) 2019 Daniel H. Huson
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

package megan.core;

import jloda.util.ProgramProperties;
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
     * @param readBlock
     * @return assignment value
     */
    public int compute(IReadBlock readBlock, IntervalTree<Object> intervals) {
        switch (mode) {
            default:
            case readCount: {
                return 1;
            }
            case readLength: {
                return Math.max(1, readBlock.getReadLength());
            }
            case alignedBases: {
                return computeCoveredBases(readBlock, intervals);
            }
            case readMagnitude: {
                return ReadMagnitudeParser.parseMagnitude(readBlock.getReadHeader());
            }
        }
    }

    /**
     * computes the number of bases covered by any alignments
     *
     * @param readBlock
     * @param intervals
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
