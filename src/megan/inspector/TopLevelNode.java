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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * node to represent top-level taxon
 * Daniel Huson, 2.2006
 */
public class TopLevelNode extends NodeBase {
    private final String classificationName;
    private final Set<Integer> classIds = new HashSet<>();

    public TopLevelNode(String name, int count, int classId, String classificationName) {
        super(name);
        this.rank = count;
        classIds.add(classId);
        this.classificationName = classificationName;
    }

    public TopLevelNode(String name, int count, Collection<Integer> classIds, String classificationName) {
        super(name);
        this.rank = count;
        this.classIds.addAll(classIds);
        this.classificationName = classificationName;
    }

    public String toString() {
        if (rank > -1)
            return String.format("%s [%,d]", getName(), (int) rank);
        else
            return getName();
    }

    public boolean isLeaf() {
        return rank == 0;
    }

    public String getClassificationName() {
        return classificationName;
    }

    public Set<Integer> getClassIds() {
        return classIds;
    }
}
