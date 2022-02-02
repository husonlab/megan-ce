/*
 * RMA2Formatter.java Copyright (C) 2022 Daniel H. Huson
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
package megan.rma2;

/**
 * reads and writes the read block fixed part
 * Daniel Huson, 3.2011
 */
public class RMA2Formatter {
    private final ReadBlockRMA2Formatter readBlockFormatter;
    private final MatchBlockRMA2Formatter matchBlockFormatter;
    private boolean wantLocationData = false; // in the special case that we are parsing an RMA file for submission to a database, we want the ReadBlock.read method to return a readblock with location data

    /**
     * Constructor
     *
	 */
    public RMA2Formatter(String readBlockFormatString, String matchBlockFormatString) {
        readBlockFormatter = new ReadBlockRMA2Formatter(readBlockFormatString);
        matchBlockFormatter = new MatchBlockRMA2Formatter(matchBlockFormatString);
    }

    /**
     * get the read block formatter
     *
     * @return readBlockFormatter
     */
    public ReadBlockRMA2Formatter getReadBlockRMA2Formatter() {
        return readBlockFormatter;
    }

    public MatchBlockRMA2Formatter getMatchBlockRMA2Formatter() {
        return matchBlockFormatter;
    }

    public void setWantLocationData(boolean wantLocationData) {
        this.wantLocationData = wantLocationData;
    }

    public boolean isWantLocationData() {
        return wantLocationData;
    }
}
