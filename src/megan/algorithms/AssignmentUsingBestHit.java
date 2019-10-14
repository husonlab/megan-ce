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

import jloda.util.Basic;
import jloda.util.FileLineIterator;
import megan.classification.IdMapper;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * assignment using best hit
 * Created by huson on 1/22/16.
 */
public class AssignmentUsingBestHit implements IAssignmentAlgorithm {
    private final String cName;

    private final Map<String, Integer> externalName2IdMap;

    /**
     * constructor
     *
     * @param cName
     */
    public AssignmentUsingBestHit(String cName, String fileName) {
        this.cName = cName;

        externalName2IdMap = loadAssignmentFiles(cName, fileName);

        // System.err.println("Using 'best hit'  assignment on " + cName);
    }

    /**
     * computes the id for a read from its matches
     * matches
     *
     * @param activeMatches
     * @param readBlock
     * @return id or 0
     */
    public int computeId(BitSet activeMatches, IReadBlock readBlock) {
        if (externalName2IdMap != null) {
            final String name = readBlock.getReadName();
            final Integer id = externalName2IdMap.get(name);
            if (id != null && id > 0)
                return id;
        }

        if (readBlock.getNumberOfMatches() == 0)
            return IdMapper.NOHITS_ID;
        if (activeMatches.cardinality() == 0)
            return IdMapper.UNASSIGNED_ID;

        for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
            IMatchBlock match = readBlock.getMatchBlock(i);
            int id = match.getId(cName);
            if (id > 0)
                return id;
        }
        return IdMapper.UNASSIGNED_ID;
    }

    /**
     * get the LCA of two ids
     *
     * @param id1
     * @param id2
     * @return LCA of id1 and id2
     */
    @Override
    public int getLCA(int id1, int id2) {
        throw new RuntimeException("getLCA() called for assignment using best hit");
    }


    /**
     * load all assignment files
     *
     * @param cName
     * @param fileName
     * @return all read to id assignments
     */
    private Map<String, Integer> loadAssignmentFiles(String cName, String fileName) {
        final File file = new File(Basic.replaceFileSuffix(fileName, "." + cName.toLowerCase()));
        if (file.exists() && file.canRead()) {
            System.err.println("External assignment file for " + cName + " detected: " + file);
            final Map<String, Integer> map = new HashMap<>();
            try (final FileLineIterator it = new FileLineIterator(file, true)) {
                while (it.hasNext()) {
                    final String[] tokens = Basic.split(it.next(), '\t');
                    if (tokens.length == 2 && Basic.isInteger(tokens[1])) {
                        int id = Basic.parseInt(tokens[1]);
                        map.put(tokens[0], id);
                    }
                }
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
            System.err.println("Count: " + map.size());
            if (map.size() > 0)
                return map;
        }
        return null;
    }
}
