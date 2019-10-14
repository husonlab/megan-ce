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

/**
 * read data object
 * Daniel Huson, 5.2015
 */
public class ReadData {
    private final int id;
    private final String name;
    private String segment;
    private MatchData[] matches;

    public ReadData(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("ReadData: name='").append(name).append("' seg='").append(segment).append("'\n");
        for (MatchData match : matches) {
            buf.append("\t").append(match.toString()).append("\n");
        }
        return buf.toString();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public MatchData[] getMatches() {
        return matches;
    }

    public void setMatches(MatchData[] matches) {
        this.matches = matches;
    }
}
