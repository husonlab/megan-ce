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
package megan.algorithms;

import jloda.util.Pair;
import megan.classification.IdMapper;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;

import java.util.*;

/**
 * computes the top classification assignments for a read
 * Daniel Huson, 5.2012
 */
public class TopAssignment {
    /**
     * computes the top KEGG assignments for a read
     *
     *
     * @param classificationName
     * @param readBlock
     * @return top assignments
     */
    public static String compute(String classificationName, BitSet activeMatches, IReadBlock readBlock, int ranksToReport) {

        if (activeMatches.cardinality() == 0)
            return "";


        int totalClassMatches = 0;
        Map<Integer, Integer> classId2Count = new HashMap<>();
        for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
            int classId = matchBlock.getId(classificationName);
            if (classId > 0) {
                Integer count = classId2Count.get(classId);
                classId2Count.put(classId, count == null ? 1 : count + 1);
                totalClassMatches++;
            }
        }

        if (classId2Count.size() == 0)
            return "";
        else if (classId2Count.size() == 1) {
            final Integer classId = classId2Count.keySet().iterator().next();
            final String classificationLetter = classificationName.substring(0, 1);
            return String.format(" [1] %s%05d: 100 # %d", classificationLetter, classId, classId2Count.get(classId));
        } else {
            SortedSet<Pair<Integer, Integer>> sorted = new TreeSet<>((idAndCount1, idAndCount2) -> {
                if (idAndCount1.get2() > idAndCount2.get2())
                    return -1;
                else if (idAndCount1.get2() < idAndCount2.get2())
                    return 1;
                else
                    return idAndCount1.get1().compareTo(idAndCount2.get1());
            });

            for (Map.Entry<Integer, Integer> entry : classId2Count.entrySet()) {
                sorted.add(new Pair<>(entry.getKey(), entry.getValue()));
            }
            final int top = Math.min(sorted.size(), ranksToReport);
            if (top == 0)
                return "";
            else {
                final String classificationLetter = classificationName.substring(0, 1);
                int countItems = 0;
                StringBuilder buf = new StringBuilder();
                for (Pair<Integer, Integer> idAndCount : sorted) {
                    countItems++;
                    buf.append(String.format(" [%d] %s%05d: %.1f", countItems, classificationLetter, idAndCount.getFirst(), (100.0 * idAndCount.get2()) / totalClassMatches));
                    if (countItems >= top)
                        break;
                }
                buf.append(" # ").append(totalClassMatches);
                return buf.toString();
            }
        }
    }

    /**
     * compute the class id for a read from its matches
     * matches
     *
     * @param minScore
     * @param readBlock
     * @return id or 0
     */
    public static int computeId(String cName, float minScore, float maxExpected, float minPercentIdentity, IReadBlock readBlock) {
        if (readBlock.getNumberOfMatches() == 0)
            return IdMapper.NOHITS_ID;

        for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
            IMatchBlock match = readBlock.getMatchBlock(i);
            if (match.getBitScore() >= minScore && match.getExpected() <= maxExpected && (minPercentIdentity == 0 || match.getPercentIdentity() >= minPercentIdentity)) {
                int id = match.getId(cName);
                if (id != 0)
                    return id;
            }
        }
        return IdMapper.UNASSIGNED_ID;
    }

}

