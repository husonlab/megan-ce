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
package megan.inspector;

/**
 * leaf node that represents the text of an alignment
 * Daniel Huson, 2.2006
 */
public class MatchTextNode extends NodeBase {
    private final String text;

    public MatchTextNode(String text) {
        if (text == null)
            text = "Null";
        if (text.endsWith("\n"))
            this.text = text;
        else
            this.text = text + "\n";
    }

    public boolean isLeaf() {
        return true;
    }

    public String toString() {
        return text;
    }

    public String getText() {
        return text;
    }
}
