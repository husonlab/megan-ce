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
package megan.core;

import java.util.*;

/**
 * maintains selection state of labels
 * Daniel Huson, 1.2013
 */
public class SelectionSet {
    private boolean selectionIsChanging = false;

    private final Set<String> selectedLabels = new HashSet<>();
    private final List<SelectionListener> selectionListenerList = new LinkedList<>();
    private final SelectionListener listener = (labels, selected) -> {
    };

    /**
     * set the selection state of the label
     *
     * @param label
     * @param selected
     */
    public void setSelected(String label, boolean selected) {
        if (!selectionIsChanging) {
            selectionIsChanging = true;
            try {
                int currentSize = size();
                if (selected)
                    this.selectedLabels.add(label);
                else
                    this.selectedLabels.remove(label);
                if (currentSize != size()) {
                    fireChanged(Collections.singletonList(label), selected);
                }
            } finally {
                selectionIsChanging = false;
            }
        }
    }

    /**
     * sets the selection state of a collection of labels
     *
     * @param labels
     * @param selected
     */
    public void setSelected(java.util.Collection<String> labels, boolean selected) {
        if (!selectionIsChanging) {
            selectionIsChanging = true;
            try {
                int currentSize = size();
                if (selected)
                    this.selectedLabels.addAll(labels);
                else
                    this.selectedLabels.removeAll(labels);
                if (currentSize != size())
                    fireChanged(labels, selected);
            } finally {
                selectionIsChanging = false;
            }
        }
    }

    /**
     * is label selected?
     *
     * @param label
     * @return true, if selected
     */
    public boolean isSelected(String label) {
        return selectedLabels.contains(label);
    }

    public Set<String> getAll() {
        return selectedLabels;
    }

    public void clear() {
        if (!selectionIsChanging) {
            selectionIsChanging = true;
            try {
                Set<String> selected = new HashSet<>(this.selectedLabels);
                this.selectedLabels.clear();
                fireChanged(selected, false);
            } finally {
                selectionIsChanging = false;
            }
        }
    }

    public int size() {
        return selectedLabels.size();
    }

    private void fireChanged(java.util.Collection<String> labels, boolean select) {
        for (SelectionListener selectionListener : selectionListenerList) {
            selectionListener.changed(labels, select);
        }
    }

    public void addSampleSelectionListener(SelectionListener selectionListener) {
        selectionListenerList.add(selectionListener);
    }

    public void removeSampleSelectionListener(SelectionListener selectionListener) {
        selectionListenerList.remove(selectionListener);
    }

    public void removeAllSampleSelectionListeners() {
        selectionListenerList.clear();
    }

    public interface SelectionListener {
        void changed(java.util.Collection<String> labels, boolean selected);
    }
}
