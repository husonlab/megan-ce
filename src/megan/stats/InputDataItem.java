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
package megan.stats;

import megan.core.Director;

import javax.swing.*;
import java.util.Comparator;

/**
 * combobox item
 * Daniel Huson, 2.2008
 */
public class InputDataItem extends JButton implements Comparator {
    private Director dir;

    InputDataItem() {
    }


    InputDataItem(Director dir) {
        this.dir = dir;
    }

    public String toString() {
        return "[" + getPID() + "] " + getName();
    }

    public int getPID() {
        return dir.getID();
    }

    public String getName() {
        return dir.getTitle();
    }

    public int compare(Object o, Object o1) {
        InputDataItem one = (InputDataItem) o;
        InputDataItem other = (InputDataItem) o1;
        int x = one.getName().compareTo(other.getName());
        if (x != 0)
            return x;
        return Integer.compare(one.getPID(), other.getPID());
    }
}
