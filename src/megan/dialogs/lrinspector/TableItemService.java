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

import javafx.beans.property.*;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.TableView;
import megan.core.Document;

import java.util.HashSet;
import java.util.Set;

/**
 * Service that adds items to the table
 * Created by huson on 2/21/17.
 */
public class TableItemService extends Service<Integer> {
    private Document doc;
    private String[] cNames;
    private String classificationName;
    private final Set<Integer> classIds = new HashSet<>();
    private TableView<TableItem> tableView;

    private final FloatProperty maxBitScore = new SimpleFloatProperty();
    private final FloatProperty maxNormalizedBitScore = new SimpleFloatProperty();
    private IntegerProperty maxReadLength;
    private ReadOnlyDoubleProperty layoutWidth;

    /**
     * configure the task
     *
     * @param doc
     * @param cNames
     * @param classIds
     * @param tableView
     */
    public void configure(Document doc, String[] cNames, String classificationName, Set<Integer> classIds, IntegerProperty maxReadLength, TableView<TableItem> tableView, ReadOnlyDoubleProperty panelWidth) {
        this.cNames = cNames;
        this.doc = doc;
        this.classificationName = classificationName;
        this.classIds.clear();
        this.classIds.addAll(classIds);
        this.tableView = tableView;
        this.maxReadLength = maxReadLength;
        this.layoutWidth = panelWidth;
    }

    /**
     * create a task
     *
     * @return task
     */
    @Override
    protected Task<Integer> createTask() {
        return new TableItemTask(doc, cNames, classificationName, classIds, tableView, maxBitScoreProperty(), maxNormalizedBitScoreProperty(), maxReadLengthProperty(), layoutWidth);
    }

    public float getMaxBitScore() {
        return maxBitScoreProperty().get();
    }

    private FloatProperty maxBitScoreProperty() {
        return maxBitScore;
    }

    public FloatProperty maxNormalizedBitScoreProperty() {
        return maxNormalizedBitScore;
    }

    private IntegerProperty maxReadLengthProperty() {
        if (maxReadLength == null)
            maxReadLength = new SimpleIntegerProperty();
        return maxReadLength;
    }
}
