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
package megan.assembly;

import java.util.Comparator;

/**
 * match data
 * Daniel Huson, 5.2015
 */
public class MatchData implements Comparator<MatchData> {
    private ReadData read;
    private String refName;
    private int firstPosInRef;
    private int lastPosInRef;
    private float bitScore;
    private String text;

    public MatchData() {
    }

    public MatchData(ReadData read, String refName, int firstPosInRef, int lastPosInRef, String text, float bitScore) {
        this.read = read;
        this.refName = refName;
        this.firstPosInRef = firstPosInRef;
        this.lastPosInRef = lastPosInRef;
        this.text = text;
        this.bitScore = bitScore;
    }

    public String toString() {
        return "MatchData: refName=" + refName + " refCoordinates=" + firstPosInRef + ".." + lastPosInRef + "bitScore=" + bitScore + "\n" + text;
    }

    /**
     * sort by ascending start position and descending end position
     *
     * @param o1
     * @param o2
     * @return
     */
    public int compare(MatchData o1, MatchData o2) {
        if (o1.firstPosInRef < o2.firstPosInRef)
            return -1;
        if (o1.firstPosInRef > o2.firstPosInRef)
            return 1;
        if (o1.lastPosInRef < o2.lastPosInRef)
            return 1;
        if (o1.lastPosInRef > o2.lastPosInRef)
            return -1;
        return Integer.compare(o1.read.getId(), o2.read.getId());
    }

    public ReadData getRead() {
        return read;
    }

    public String getRefName() {
        return refName;
    }

    public int getFirstPosInRef() {
        return firstPosInRef;
    }

    public int getLastPosInRef() {
        return lastPosInRef;
    }

    public float getBitScore() {
        return bitScore;
    }

    public String getText() {
        return text;
    }
}
