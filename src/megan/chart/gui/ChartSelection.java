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
package megan.chart.gui;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * selection manager
 * Daniel Huson, 7.2012
 */
public class ChartSelection {
    private final Set<String> selectedSeries = new HashSet<>();
    private final Set<String> selectedClasses = new HashSet<>();
    private final Set<String> selectedAttributes = new HashSet<>();
    private final LinkedList<IChartSelectionListener> seriesSelectionListeners = new LinkedList<>();
    private final LinkedList<IChartSelectionListener> classesSelectionListeners = new LinkedList<>();
    private final LinkedList<IChartSelectionListener> attributesSelectionListeners = new LinkedList<>();

    private boolean isSelectedBasedOnSeries = true;

    /**
     * is series selected and  isSelectedBasedOnSeries==true or className selected and isSelectedBasedOnSeries==false?
     * Logic is flipped if transposed is set
     *
     * @param series
     * @param className
     * @return true, if either condition holds
     */
    public boolean isSelected(String series, String className) {
        if (isSelectedBasedOnSeries)
            return selectedSeries.contains(series);
        else
            return selectedClasses.contains(className);
    }

    public boolean isSelectedBasedOnSeries() {
        return isSelectedBasedOnSeries;
    }

    public void setSelectedBasedOnSeries(boolean isSelectedBasedOnSeries) {
        this.isSelectedBasedOnSeries = isSelectedBasedOnSeries;
    }

    public Set<String> getSelectedSeries() {
        return selectedSeries;
    }

    public boolean isSelectedSeries(String series) {
        return selectedSeries.contains(series);
    }

    public void setSelectedSeries(String series, boolean select) {
        if (select)
            selectedSeries.add(series);
        else
            selectedSeries.remove(series);
        fireSeriesSelectionListeners();
    }

    public void setSelectedSeries(java.util.Collection<String> series, boolean select) {
        if (select)
            selectedSeries.addAll(series);
        else
            selectedSeries.removeAll(series);
        fireSeriesSelectionListeners();
    }

    public void clearSelectionSeries() {
        selectedSeries.clear();
        fireSeriesSelectionListeners();
    }

    public void toggleSelectedSeries(java.util.Collection<String> series) {
        Collection<String> toSelect = new HashSet<>(series);
        toSelect.removeAll(selectedSeries);
        selectedSeries.removeAll(series);
        selectedSeries.addAll(toSelect);
        fireSeriesSelectionListeners();
    }

    public Set<String> getSelectedClasses() {
        return selectedClasses;
    }

    public boolean isSelectedClass(String className) {
        return selectedClasses.contains(className);
    }

    public void setSelectedClass(String className, boolean select) {
        if (select)
            selectedClasses.add(className);
        else
            selectedClasses.remove(className);
        fireClassesSelectionListeners();
    }

    public void toggleSelectedClasses(java.util.Collection<String> classes) {
        Collection<String> toSelect = new HashSet<>(classes);
        toSelect.removeAll(selectedClasses);
        selectedClasses.removeAll(classes);
        selectedClasses.addAll(toSelect);
        fireClassesSelectionListeners();
    }

    public void setSelectedClass(java.util.Collection<String> classes, boolean select) {
        if (select)
            selectedClasses.addAll(classes);
        else
            selectedClasses.removeAll(classes);
        fireClassesSelectionListeners();
    }

    public void clearSelectionClasses() {
        selectedClasses.clear();
        fireClassesSelectionListeners();
    }

    public Set<String> getSelectedAttributes() {
        return selectedAttributes;
    }

    public boolean isSelectedAttribute(String className) {
        return selectedAttributes.contains(className);
    }

    public void setSelectedAttribute(String name, boolean select) {
        if (select)
            selectedAttributes.add(name);
        else
            selectedAttributes.remove(name);
        fireAttributesSelectionListeners();
    }

    public void toggleSelectedAttributes(java.util.Collection<String> attributes) {
        Collection<String> toSelect = new HashSet<>(attributes);
        toSelect.removeAll(selectedAttributes);
        selectedAttributes.removeAll(attributes);
        selectedAttributes.addAll(toSelect);
        fireAttributesSelectionListeners();
    }

    public void setSelectedAttribute(java.util.Collection<String> attributes, boolean select) {
        if (select)
            selectedAttributes.addAll(attributes);
        else
            selectedAttributes.removeAll(attributes);
        fireAttributesSelectionListeners();
    }

    public void clearSelectionAttributes() {
        selectedAttributes.clear();
        fireAttributesSelectionListeners();
    }

    public void addSeriesSelectionListener(IChartSelectionListener listener) {
        seriesSelectionListeners.add(listener);
    }

    public void removeSeriesSelectionListener(IChartSelectionListener listener) {
        seriesSelectionListeners.remove(listener);
    }

    private void fireSeriesSelectionListeners() {
        for (IChartSelectionListener selectionListener : seriesSelectionListeners) {
            selectionListener.selectionChanged(this);
        }
    }

    public void addClassesSelectionListener(IChartSelectionListener listener) {
        classesSelectionListeners.add(listener);
    }

    public void removeClassesSelectionListener(IChartSelectionListener listener) {
        classesSelectionListeners.remove(listener);
    }

    private void fireClassesSelectionListeners() {
        for (IChartSelectionListener selectionListener : classesSelectionListeners) {
            selectionListener.selectionChanged(this);
        }
    }

    public void addAttributesSelectionListener(IChartSelectionListener listener) {
        attributesSelectionListeners.add(listener);
    }

    public void removeAttributesSelectionListener(IChartSelectionListener listener) {
        attributesSelectionListeners.remove(listener);
    }

    private void fireAttributesSelectionListeners() {
        for (IChartSelectionListener selectionListener : attributesSelectionListeners) {
            selectionListener.selectionChanged(this);
        }
    }

    /**
     * set selected
     *
     * @param target series, classes or attributes
     * @param list
     * @param select
     */
    public void setSelected(String target, java.util.Collection<String> list, boolean select) {
        switch (target.toLowerCase()) {
            case "series":
                setSelectedSeries(list, select);
                break;
            case "classes":
                setSelectedClass(list, select);
                break;
            case "attributes":
                setSelectedAttribute(list, select);
                break;
        }
    }

    /**
     * clear selection
     *
     * @param target series, classes or attributes
     */
    public void clearSelection(String target) {
        switch (target.toLowerCase()) {
            case "series":
                clearSelectionSeries();
                break;
            case "classes":
                clearSelectionClasses();
                break;
            case "attributes":
                clearSelectionAttributes();
                break;
        }
    }

}


