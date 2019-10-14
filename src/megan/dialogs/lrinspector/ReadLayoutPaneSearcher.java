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

package megan.dialogs.lrinspector;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import jloda.fx.control.AMultipleSelectionModel;
import jloda.swing.find.IObjectSearcher;
import megan.data.IMatchBlock;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * read layout pane searcher
 * Created by huson on 2/24/17.
 */
public class ReadLayoutPaneSearcher implements IObjectSearcher {
    private final Component parent;
    private final ReadLayoutPane readLayoutPane;
    private final AMultipleSelectionModel<IMatchBlock> matchBlockSelectionModel;
    private final ArrayList<Label> labels = new ArrayList<>();
    private int currentIndex = -1;

    /**
     * constructor
     *
     * @param readLayoutPane
     */
    public ReadLayoutPaneSearcher(Component parent, ReadLayoutPane readLayoutPane, AMultipleSelectionModel<IMatchBlock> matchBlockSelectionModel) {
        this.parent = parent;
        this.readLayoutPane = readLayoutPane;
        this.matchBlockSelectionModel = matchBlockSelectionModel;
    }


    @Override
    public String getName() {
        return "Alignments";
    }

    @Override
    public boolean gotoFirst() {
        if (labels.size() > 0) {
            currentIndex = 0;
            return true;
        } else
            return false;
    }

    @Override
    public boolean gotoNext() {
        if (currentIndex + 1 < labels.size()) {
            currentIndex++;
            return true;
        } else
            return false;
    }


    @Override
    public boolean isGlobalFindable() {
        return true;
    }

    @Override
    public boolean gotoLast() {
        if (labels.size() > 0) {
            currentIndex = labels.size() - 1;
            return true;
        } else
            return false;
    }

    @Override
    public boolean isSelectionFindable() {
        return false;
    }

    @Override
    public boolean gotoPrevious() {
        if (currentIndex > 0 && currentIndex <= labels.size()) {
            currentIndex--;
            return true;
        } else
            return false;
    }

    @Override
    public void updateView() {
    }

    @Override
    public boolean isCurrentSet() {
        return currentIndex >= 0 && currentIndex < labels.size();
    }

    @Override
    public boolean canFindAll() {
        return true;
    }

    @Override
    public boolean isCurrentSelected() {
        if (isCurrentSet()) {
            final Label label = labels.get(currentIndex);
            if (label.getUserData() instanceof IMatchBlock[]) {
                for (IMatchBlock matchBlock : (IMatchBlock[]) label.getUserData()) {
                    if (matchBlockSelectionModel.getSelectedItems().contains(matchBlock)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void selectAll(final boolean select) {
        Runnable runnable = () -> {
            if (isCurrentSet()) {
                if (select)
                    matchBlockSelectionModel.selectAll();
                else
                    matchBlockSelectionModel.clearSelection();
            }
        };
        if (Platform.isFxApplicationThread())
            runnable.run();
        else
            Platform.runLater(runnable);
    }

    @Override
    public void setCurrentSelected(final boolean select) {
        final int index = currentIndex;
        Runnable runnable = () -> {
            if (isCurrentSet()) {
                final Label label = labels.get(index);
                if (label.getUserData() instanceof IMatchBlock[]) {
                    for (IMatchBlock matchBlock : (IMatchBlock[]) label.getUserData()) {
                        if (select)
                            matchBlockSelectionModel.select(matchBlock);
                        else
                            matchBlockSelectionModel.clearSelection(matchBlock);
                    }
                }
            }
        };
        if (Platform.isFxApplicationThread())
            runnable.run();
        else
            Platform.runLater(runnable);
    }

    @Override
    public Component getParent() {
        return parent;
    }

    @Override
    public String getCurrentLabel() {
        if (isCurrentSet()) {
            Label label = labels.get(currentIndex);
            if (label.getTooltip() != null)
                return label.getTooltip().getText();
            else
                return label.getText();
        } else
            return "";
    }

    @Override
    public Collection<AbstractButton> getAdditionalButtons() {
        return null;
    }

    @Override
    public void setCurrentLabel(String newLabel) {
    }

    @Override
    public int numberOfObjects() {
        return labels.size();
    }

    /**
     * update lists, must be called after list of visible labels has changed
     */
    public ReadLayoutPaneSearcher updateLists() {
        labels.clear();
        for (Group group : readLayoutPane.getVisibleGroups()) {
            for (Node node : group.getChildren()) {
                if (node instanceof Label && node.getUserData() instanceof IMatchBlock[]) {
                    labels.add((Label) node);
                }
            }
        }
        labels.sort((a, b) -> {
            Integer startA = ((IMatchBlock[]) a.getUserData())[0].getAlignedQueryStart();
            return startA.compareTo(((IMatchBlock[]) b.getUserData())[0].getAlignedQueryStart());
        });
        return this;
    }

    public double getCurrentLocation() {
        if (isCurrentSet())
            return labels.get(currentIndex).getLayoutX();
        else
            return 0;
    }
}
