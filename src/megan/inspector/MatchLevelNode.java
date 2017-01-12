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
 * node to represent a read-hit
 * Daniel Huson, 2.2006
 */
public class MatchLevelNode extends NodeBase {
    protected boolean ignore;
    protected boolean isUsed;
    protected long uId;
    protected String matchText;

    public MatchLevelNode(String name, float score, boolean ignore, boolean isUsed, long uId, String matchText) {
        super(name);
        this.rank = -score;
        this.ignore = ignore;
        this.isUsed = isUsed;
        this.uId = uId;
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
}
