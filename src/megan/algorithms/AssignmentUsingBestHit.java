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

import megan.classification.IdMapper;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;

import java.util.BitSet;

/**
 * assignment using best hit
 * Created by huson on 1/22/16.
 */
public class AssignmentUsingBestHit implements IAssignmentAlgorithm {
    final private String cName;

    /**
     * constructor
     *
     * @param cName
     */
    public AssignmentUsingBestHit(String cName) {
        this.cName = cName;
        // System.err.println("Using 'best hit'  assignment on " + cName);
    }

    /**
     * computes the id for a read from its matches
     * matches
     *
     * @param activeMatches
     * @param readBlock
     * @return COG id or 0
     */
    public int computeId(BitSet activeMatches, IReadBlock readBlock) {
        if (activeMatches.cardinality() == 0)
            return IdMapper.NOHITS_ID;
        for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
            IMatchBlock match = readBlock.getMatchBlock(i);
            int id = match.getId(cName);
            if (id > 0)
                return id;
        }
        return IdMapper.UNASSIGNED_ID;
    }
}
