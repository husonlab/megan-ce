/*
 * HSP.java Copyright (C) 2023 Daniel H. Huson
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
package megan.parsers.blast.blastxml;

import jloda.util.StringUtils;

/**
 * Blast HSP
 * Daniel Huson, 2.2011, 4.2015
 */
public class HSP {
    public float bitScore;
    public float score;
    public double eValue;
    public long queryFrom;
    public long queryTo;
    public long hitFrom;
    public long hitTo;
    public int queryFrame;
    public int hitFrame;
    public int identity;
    public int positive;
    public int gaps;
    public int alignLength;
    public int density;
    public String qSeq;
    public String hSeq;
    public String midLine;

    /**
     * return human readable string representation
     *
     * @return string
     */
    public String toString(String hitDef, int hitLen) {
        StringBuilder buffer = new StringBuilder();

		buffer.append(String.format(">%s\n\tLength = %d\n", StringUtils.fold(hitDef, 100), hitLen));
		buffer.append(String.format(" Score = %.1f bits (%.1f), Expect= %e\n", bitScore, score, eValue));
        buffer.append(String.format(" Identities = %d/%d (%d%%), Positives = %d/%d (%d%%), Gaps = %d/%d (%d%%)\n", identity, alignLength, Math.round(100 * identity / alignLength), positive, alignLength, 100 * positive / alignLength, gaps, alignLength, 100 * gaps / alignLength));
        if (queryFrame != 0)
            buffer.append(String.format(" Frame = %+d\n", queryFrame));
        if (qSeq != null && hSeq != null) {
            long a = (queryFrame >= 0 ? queryFrom : queryTo);
            long b = (queryFrame >= 0 ? queryTo : queryFrom);

            buffer.append(String.format("\nQuery:%9d  %s  %d\n", a, qSeq, b));
            if (midLine != null)
                buffer.append(String.format("                 %s\n", midLine));
            buffer.append(String.format("Sbjct:%9d  %s  %d\n", hitFrom, hSeq, hitTo));
        } else
            buffer.append("[No alignment given]\n");
        return buffer.toString();

    }
}
