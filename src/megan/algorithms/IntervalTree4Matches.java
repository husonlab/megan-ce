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

package megan.algorithms;

import javafx.concurrent.Task;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.util.interval.Interval;
import megan.util.interval.IntervalTree;

/**
 * computes interval tree of all matches to keep for a read block
 * Created by huson on 3/29/17.
 */
public class IntervalTree4Matches {
    /**
     * selects the matches to keep for a given read and puts them into an interval tree
     *
     * @param readBlock
     * @param task        can be null
     * @return interval tree
     */
    public static IntervalTree<IMatchBlock> computeIntervalTree(IReadBlock readBlock, Task task) {
        final IntervalTree<IMatchBlock> intervalTree = new IntervalTree<>();

        for (int m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
            intervalTree.add(new Interval<>(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock));
            if (task != null && task.isCancelled())
                break;
        }
        return intervalTree;
    }
}
