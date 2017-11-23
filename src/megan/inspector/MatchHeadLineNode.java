/*
 *  Copyright (C) 2017 Daniel H. Huson
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
package megan.inspector;

import jloda.util.MultiLineCellRenderer;

/**
 * Node representing headline for match
 * Daniel Huson, 2.2006
 */
public class MatchHeadLineNode extends NodeBase {
    private final boolean ignore;
    private final boolean isUsed;
    private final long uId;
    private final int taxId;
    private final String matchText;

    /**
     * constructor
     *
     * @param name
     * @param score
     * @param ignore
     * @param isUsed
     * @param uId
     * @param taxId
     * @param matchText
     */
    public MatchHeadLineNode(String name, float score, boolean ignore, boolean isUsed, long uId, int taxId, String matchText) {
        super(name);
        this.rank = -score;
        this.ignore = ignore;
        this.isUsed = isUsed;
        this.uId = uId;
        this.taxId=taxId;
        this.matchText = matchText;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (ignore)
            buf.append(MultiLineCellRenderer.RED);
        else if (!isUsed)
            buf.append(MultiLineCellRenderer.GRAY);
        buf.append(getName());
        if (rank != 0)
            buf.append(" score=").append(String.format("%.1f", -rank));
        return buf.toString();
    }

    public boolean getIgnore() {
        return ignore;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public long getUId() {
        return uId;
    }

    public int getTaxId() {
        return taxId;
    }

    public String getMatchText() {
        return matchText;
    }
}
