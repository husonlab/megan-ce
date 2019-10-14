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
 * node to represent the data (i.e. text) of a read
 * Daniel Huson, 2.2006
 */
public class ReadDataTextNode extends NodeBase {
    private String text;

    public ReadDataTextNode(String text) {
        setText(text);
    }

    private void setText(String text) {
        if (text.endsWith("\n"))
            this.text = text;
        else
            this.text = text + "\n";
    }

    public String getText() {
        return text;
    }

    public boolean isLeaf() {
        return true;
    }

    public String toString() {
        return text;
    }
}
