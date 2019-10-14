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

package megan.parsers.blast;

import java.util.Comparator;

/**
 * use this to sort matches
 * Daniel Huson, 2016
 */
public class Match implements Comparator<Match> {
    float bitScore;
    int id;
    String samLine;

    @Override
    public int compare(Match a, Match b) {
        if (a.bitScore > b.bitScore)
            return -1;
        else if (a.bitScore < b.bitScore)
            return 1;
        else return Integer.compare(a.id, b.id);
    }

    public float getBitScore() {
        return bitScore;
    }

    public void setBitScore(float bitScore) {
        this.bitScore = bitScore;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSamLine() {
        return samLine;
    }

    public void setSamLine(String samLine) {
        this.samLine = samLine;
    }
}
