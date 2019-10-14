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

import jloda.util.Basic;

/**
 * node representing head line for read
 * Daniel Huson, 2.2006
 */
public class ReadDataHeadLineNode extends NodeBase {
    private String text;
    private String data;

    public ReadDataHeadLineNode(String text, int readLength, float complexity, int weight, String data) {
        final StringBuilder builder = new StringBuilder();
        builder.append(text);

        boolean first = true;
        if (readLength > 0) {
            builder.append("[");
            first = false;
            builder.append(String.format("length=%,d", readLength));
        }
        if (weight > 1) {
            if (first) {
                builder.append("[");
                first = false;
            } else
                builder.append(", ");
            builder.append(String.format("weight=%,d", weight));
        }

        if (complexity > 0) {
            if (first) {
                builder.append("[");
                first = false;
            } else
                builder.append(", ");
            builder.append(String.format("complexity=%1.2f", complexity));
        }
        if (!first)
            builder.append("]");
        setText(builder.toString());

        this.data = (data != null && data.startsWith(">") ? data : ">" + data);
        this.data = Basic.foldHard(this.data, 140);
    }

    private void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public boolean isLeaf() {
        return data == null || data.length() == 0;
    }

    public String toString() {
        return text;
    }

    public String getData() {
        return data;
    }
}
