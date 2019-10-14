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
package megan.clusteranalysis;

import jloda.swing.find.IObjectSearcher;
import megan.clusteranalysis.gui.ITab;
import megan.clusteranalysis.gui.MatrixTab;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;


/**
 * network searcher
 * Daniel Huson, 7.2010
 */
public class ClusterAnalysisSearcher implements IObjectSearcher {
    private static final String NAME = "Network";

    private ClusterViewer clusterViewer;

    private IObjectSearcher currentSearcher;

    /**
     * empty constructor. Searcher doesn't work until setup has been called
     */
    public ClusterAnalysisSearcher() {
    }

    /**
     * Constructor
     *
     * @param clusterViewer the go viewer
     */
    public ClusterAnalysisSearcher(ClusterViewer clusterViewer) {
        setup(clusterViewer);
        updateCurrent();
    }

    /**
     * setup the  searcher
     *
     * @param clusterViewer
     */
    private void setup(ClusterViewer clusterViewer) {
        this.clusterViewer = clusterViewer;
        updateCurrent();
    }

    /**
     * rescan the matrix searcher
     */
    public void updateMatrixSearcher() {
        updateCurrent();
    }

    public boolean gotoFirst() {
        updateCurrent();
        return currentSearcher != null && currentSearcher.gotoFirst();
    }

    public boolean gotoNext() {
        updateCurrent();
        return currentSearcher != null && currentSearcher.gotoNext();
    }

    public boolean gotoLast() {
        updateCurrent();
        return currentSearcher != null && currentSearcher.gotoLast();
    }

    public boolean gotoPrevious() {
        updateCurrent();
        return currentSearcher != null && currentSearcher.gotoPrevious();
    }

    public boolean isCurrentSet() {
        updateCurrent();
        return currentSearcher != null && currentSearcher.isCurrentSet();
    }

    public boolean isCurrentSelected() {
        updateCurrent();
        return currentSearcher != null && currentSearcher.isCurrentSelected();
    }

    public void setCurrentSelected(boolean select) {
        updateCurrent();
        if (currentSearcher != null)
            currentSearcher.setCurrentSelected(select);
    }

    public String getCurrentLabel() {
        updateCurrent();
        if (currentSearcher != null)
            return currentSearcher.getCurrentLabel();
        else
            return null;
    }

    public void setCurrentLabel(String newLabel) {
        updateCurrent();
        if (currentSearcher != null)
            currentSearcher.setCurrentLabel(newLabel);
    }

    public int numberOfObjects() {
        updateCurrent();
        if (currentSearcher != null)
            return currentSearcher.numberOfObjects();
        else
            return 0;
    }

    public String getName() {
        return NAME;
    }

    public boolean isGlobalFindable() {
        updateCurrent();
        return currentSearcher != null && currentSearcher.isGlobalFindable();
    }

    public boolean isSelectionFindable() {
        updateCurrent();
        return currentSearcher != null && currentSearcher.isSelectionFindable();
    }

    public void updateView() {
        if (currentSearcher != null)
            currentSearcher.updateView();
    }

    public boolean canFindAll() {
        updateCurrent();
        return currentSearcher != null && currentSearcher.canFindAll();
    }

    public void selectAll(boolean select) {
        updateCurrent();
        if (currentSearcher != null)
            currentSearcher.selectAll(select);
    }

    public Component getParent() {
        if (currentSearcher != null)
            return currentSearcher.getParent();
        else
            return null;
    }

    private void updateCurrent() {
        if (clusterViewer.getSelectedComponent() instanceof ITab)
            currentSearcher = ((ITab) clusterViewer.getSelectedComponent()).getSearcher();
        else if (clusterViewer.getSelectedComponent() instanceof MatrixTab)
            currentSearcher = ((MatrixTab) clusterViewer.getSelectedComponent()).getSearcher();
        else
            currentSearcher = null;
    }

    @Override
    public Collection<AbstractButton> getAdditionalButtons() {
        return null;
    }
}
