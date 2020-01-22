/*
 * BlastTabMatch.java Copyright (C) 2020. Daniel H. Huson
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
 *
 */
package megan.rma3;

import jloda.util.Basic;

import java.io.IOException;

/**
 * simple tab match
 * Created by huson on 10/16/14.
 */
public class BlastTabMatch implements IMatch {
    private int bitScore;
    private float expected;
    private int percentIdentity;
    private String queryName;
    private String refName;

    @Override
    public void clear() {

    }

    /**
     * parse a blast tab line
     * query id, ref id, percent identity, alignment length, number of mismatches, number of gap openings, query start, query end, subject start, subject end, Expect value, HSP bit score.
     * 0         1       2                 3                 4                     5                       6            7           8             9            10            11
     *
     * @param aLine
     * @throws IOException
     */
    @Override
    public void parse(String aLine) throws IOException {
        String[] tokens = aLine.split("\t");
        if (tokens.length == 1) {
            clear();
            queryName = tokens[0];
        } else {
            queryName = tokens[0];
            refName = tokens[1];
            bitScore = Basic.parseInt(tokens[11]);
            expected = Basic.parseFloat(tokens[10]);
            percentIdentity = Basic.parseInt(tokens[2]);

        }
    }

    @Override
    public int getBitScore() {
        return bitScore;
    }

    @Override
    public float getExpected() {
        return expected;
    }

    @Override
    public int getPercentIdentity() {
        return percentIdentity;
    }

    @Override
    public String getQueryName() {
        return queryName;
    }

    @Override
    public String getRefName() {
        return refName;
    }
}
