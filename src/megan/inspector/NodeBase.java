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

import javax.swing.tree.DefaultMutableTreeNode;


/**
 * a node in the inspector2 data tree
 * Daniel Huson, 2.2006
 */
public class NodeBase extends DefaultMutableTreeNode implements Comparable<NodeBase> {
    private String name;
    float rank;
    private static long maxId = 0;
    private final long id;
    private boolean completed = true;

    public NodeBase() {
        this("Untitled");
    }

    public NodeBase(String name) {
        this.name = name;
        rank = 0;
        id = (++maxId);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isLeaf() {
        return false;
    }

    public boolean getAllowsChildren() {
        return !isLeaf();
    }

    public String toString() {
        return name;
    }

    public float getRank() {
        return rank;
    }

    public void setRank(float rank) {
        this.rank = rank;
    }

    public long getId() {
        return id;
    }


    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isCompleted() {
        return completed;
    }

    public int compareTo(NodeBase v) {
        if (rank < v.rank)
            return -1;
        else if (rank > v.rank)
            return 1;

        if (name != null && v.name == null)
            return -1;
        else if (name == null && v.name != null)
            return 1;
        if (name != null) {
            int value = name.compareTo(v.name);
            if (value != 0)
                return value;
        }

        return Long.compare(id, v.id);
    }
}
