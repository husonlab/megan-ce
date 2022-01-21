/*
 * SelectionGroup.java Copyright (C) 2022 Daniel H. Huson
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

package megan.dialogs.lrinspector;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Toggle;

import java.util.ArrayList;

/**
 * a group of toggles
 * Created by huson on 3/9/17.
 */
public class SelectionGroup {
    private final ObservableList<Toggle> items = FXCollections.observableArrayList();

    public SelectionGroup() {
    }

    public ObservableList<Toggle> getItems() {
        return items;
    }

    public boolean isSelected() {
        for (Toggle item : items) {
            if (item.isSelected())
                return true;
        }
        return false;
    }

    public boolean selectAll(boolean select) {
        boolean changed = false;
        for (Toggle item : items) {
            if (item.isSelected() != select) {
                item.setSelected(select);
                changed = true;
            }
        }
        return changed;
    }

    public ArrayList<Toggle> getSelectedItems() {
        final ArrayList<Toggle> selectedItems = new ArrayList<>();
        for (Toggle item : items) {
            if (item.isSelected())
                selectedItems.add(item);
        }
        return selectedItems;
    }
}
