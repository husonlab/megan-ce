/*
 * Copyright (C) 2020. Daniel H. Huson
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

package megan.ms.server;

/**
 * read output format
 * Daniel Huson, 10.2020
 */
public class ReadsOutputFormat {
    private final boolean readIds;
    private final boolean headers;
    private final boolean sequences;
    private final boolean matches;

    public ReadsOutputFormat(boolean readIds, boolean headers, boolean sequences, boolean matches) {
        this.readIds = readIds;
        this.headers = headers;
        this.sequences = sequences;
        this.matches = matches;
    }

    public boolean isReadIds() {
        return readIds;
    }

    public boolean isHeaders() {
        return headers;
    }

    public boolean isSequences() {
        return sequences;
    }

    public boolean isMatches() {
        return matches;
    }
}
