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

package megan.samplesviewer;

import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import jloda.fx.control.table.MyTableView;
import jloda.swing.find.IObjectSearcher;
import jloda.util.Basic;
import jloda.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Class for finding labels in a SpreadsheetView
 * Daniel Huson, 9.2015
 */
public class TableViewSearcher implements IObjectSearcher {
    private final String name;
    private final MyTableView table;
    private final Frame frame;
    private final Pair<Integer, Integer> current = new Pair<>(-1, -1); // row, col

    private final Set<Pair<Integer, Integer>> toSelect;
    private final Set<Pair<Integer, Integer>> toDeselect;
    private static final String SEARCHER_NAME = "TableSearcher";

    private final Set<Pair<Integer, Integer>> selected;

    /**
     * constructor
     *
     * @param table
     */
    public TableViewSearcher(MyTableView table) {
        this(null, SEARCHER_NAME, table);
    }

    /**
     * constructor
     *
     * @param frame
     * @param table
     */
    public TableViewSearcher(Frame frame, MyTableView table) {
        this(frame, SEARCHER_NAME, table);
    }

    /**
     * constructor
     *
     * @param
     * @param table
     */
    public TableViewSearcher(Frame frame, String name, MyTableView table) {
        this.frame = frame;
        this.name = name;
        this.table = table;
        toSelect = new HashSet<>();
        toDeselect = new HashSet<>();
        selected = new HashSet<>();
    }

    /**
     * get the parent component
     *
     * @return parent
     */
    public Component getParent() {
        return frame;
    }

    /**
     * get the name for this type of search
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * goto the first object
     */
    public boolean gotoFirst() {
        current.set1(0);
        current.set2(0);
        return isCurrentSet();
    }

    /**
     * goto the next object
     */
    public boolean gotoNext() {
        if (isCurrentSet()) {
            current.set1(current.get1() + 1);
            if (current.get1() >= table.getRowCount()) {
                current.set1(0);
                current.set2(current.get2() + 1);
            }
        } else
            gotoFirst();
        return isCurrentSet();
    }

    /**
     * goto the last object
     */
    public boolean gotoLast() {
        current.set1(table.getRowCount() - 1);
        current.set2(table.getColCount() - 1);

        return isCurrentSet();
    }

    /**
     * goto the previous object
     */
    public boolean gotoPrevious() {
        if (isCurrentSet()) {
            if (current.get2() > 0)
                current.set2(current.get2() - 1);
            else if (current.get1() > 0) {
                current.set1(current.get1() - 1);
                current.set2(table.getColCount() - 1);
            } else {
                current.set1(-1);
                current.set2(-1);
            }
        } else
            gotoLast();
        return isCurrentSet();
    }

    /**
     * is the current object selected?
     *
     * @return true, if selected
     */
    public boolean isCurrentSelected() {
        return isCurrentSet() && selected.contains(current);
    }

    /**
     * set selection state of current object
     *
     * @param select
     */
    public void setCurrentSelected(boolean select) {
        if (select)
            toSelect.add(new Pair<>(current.get1(), current.get2()));
        else
            toDeselect.add(new Pair<>(current.get1(), current.get2()));
    }

    /**
     * set select state of all objects
     *
     * @param select
     */
    public void selectAll(boolean select) {
        if (select) {
            table.getSelectionModel().selectAll();
        } else {
            table.getSelectionModel().clearSelection();
        }
    }

    /**
     * get the label of the current object
     *
     * @return label
     */
    public String getCurrentLabel() {
        try {
            if (isCurrentSet())
                return table.getValue(current.get1(), current.get2());
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return null;
    }

    /**
     * set the label of the current object
     *
     * @param newLabel
     */
    public void setCurrentLabel(final String newLabel) {
        final Pair<Integer, Integer> cell = new Pair<>(current.getFirst(), current.getSecond());

        final Runnable runnable = () -> table.setValue(cell.getFirst(), cell.getSecond(), newLabel);
        if (Platform.isFxApplicationThread())
            runnable.run();
        else
            Platform.runLater(runnable);

    }

    /**
     * is a global find possible?
     *
     * @return true, if there is at least one object
     */
    public boolean isGlobalFindable() {
        return table.getRowCount() > 0;
    }

    /**
     * is a selection find possible
     *
     * @return true, if at least one object is selected
     */
    public boolean isSelectionFindable() {
        return selected.size() > 0;  //table.getSelectionModel().getSelectedCells().size()>0;
    }

    /**
     * is the current object set?
     *
     * @return true, if set
     */
    public boolean isCurrentSet() {
        return current.get1() >= 0 && current.get1() < table.getRowCount() && current.get2() >= 0 && current.get2() < table.getColCount();
    }

    /**
     * something has been changed or selected, update view
     */
    public void updateView() {
        Platform.runLater(() -> {
            Pair<Integer, Integer> first = null;
            final Set<Pair<Integer, Integer>> selection = new HashSet<>();
            for (Object obj : table.getSelectionModel().getSelectedCells()) {
                TablePosition pos = (TablePosition) obj;
                selection.add(new Pair<>(pos.getRow(), pos.getColumn()));
            }

            for (Pair<Integer, Integer> pair : toDeselect) {
                selection.remove(pair);
            }

            for (Pair<Integer, Integer> pair : toSelect) {
                selection.add(pair);
                if (first == null)
                    first = pair;
            }
            table.getSelectionModel().clearSelection();
            for (Pair<Integer, Integer> pair : selection) {
                final TableColumn<MyTableView.MyTableRow, ?> column = table.getCol(pair.get2());
                table.getSelectionModel().select(pair.get1(), column);
            }
            if (first != null) {
                table.scrollToRow(first.get1());
            }
            toSelect.clear();
            toDeselect.clear();
        });

    }

    /**
     * does this searcher support find all?
     *
     * @return true, if find all supported
     */
    public boolean canFindAll() {
        return true;
    }

    /**
     * how many objects are there?
     *
     * @return number of objects or -1
     */
    public int numberOfObjects() {
        return table.getRowCount() * table.getColCount();
    }

    @Override
    public Collection<AbstractButton> getAdditionalButtons() {
        return null;
    }

    public Set<Pair<Integer, Integer>> getSelected() {
        return selected;
    }
}
