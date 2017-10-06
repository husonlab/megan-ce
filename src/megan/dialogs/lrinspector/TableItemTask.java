/*
 *  Copyright (C) 2017 Daniel H. Huson
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
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import megan.algorithms.IntervalTree4Matches;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Document;
import megan.data.IClassificationBlock;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.fx.FXUtilities;
import megan.util.interval.Interval;
import megan.util.interval.IntervalTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


/**
 * task
 * Created by huson on 3/7/17.
 */
public class TableItemTask extends Task<Integer> {
    private final Document doc;
    private final String[] cNames;
    private final String classificationName;
    private final Set<Integer> classIds;
    private final TableView<TableItem> tableView;
    private long previousSelectionTime = 0;

    private final FloatProperty maxBitScore;
    private final FloatProperty maxNormalizedBitScore;
    private final IntegerProperty maxReadLength;
    private final ReadOnlyDoubleProperty layoutWidth;


    /**
     * constructor
     *
     * @param doc
     * @param cNames
     * @param classificationName
     * @param tableView
     */
    public TableItemTask(Document doc, String[] cNames, String classificationName, Set<Integer> classIds, TableView<TableItem> tableView, FloatProperty maxBitScore, FloatProperty maxNormalizedBitScore, IntegerProperty maxReadLength, ReadOnlyDoubleProperty layoutWidth) {
        this.doc = doc;
        this.cNames = cNames;
        this.classificationName = classificationName;
        this.classIds = classIds;
        this.tableView = tableView;
        this.maxBitScore = maxBitScore;
        this.maxNormalizedBitScore = maxNormalizedBitScore;
        this.maxReadLength = maxReadLength;
        this.layoutWidth = layoutWidth;
    }

    /**
     * compute all table items for configured classification name and class ids
     */
    @Override
    protected Integer call() throws Exception {
        final Set<String> seen = new HashSet<>();
        final ArrayList<TableItem> buffer = new ArrayList<>(50);

        final Classification classification = ClassificationManager.get(classificationName, true);
        final IClassificationBlock classificationBlock = doc.getConnector().getClassificationBlock(classificationName);
        classIds.retainAll(classificationBlock.getKeySet());
        updateProgress(-1, classIds.size());

        int count = 0;
        int classCount = 0;
        loop:
        for (Integer classId : classIds) {
            updateMessage("Setting up " + classificationName + "...");

            String className = classification.getName2IdMap().get(classId);
            if (className == null)
                className = "[" + classId + "]";

            updateMessage("Loading '" + classification.getName2IdMap().get(classId) + "'... (" + doc.getConnector().getClassSize(classificationName, classId) + " reads)");

            try (final IReadBlockIterator it = doc.getConnector().getReadsIterator(classificationName, classId, doc.getMinScore(), doc.getMaxExpected(), true, true)) {
                while (it.hasNext()) {
                    final IReadBlock readBlock = it.next();
                    final String readName = readBlock.getReadName();
                    if (readBlock.getReadLength() == 0) {
                        int readLength = 0;
                        for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
                            final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                            readLength = Math.max(readLength, matchBlock.getAlignedQueryStart());
                            readLength = Math.max(readLength, matchBlock.getAlignedQueryEnd());
                        }
                        readBlock.setReadLength(readLength);
                    }

                    if (!seen.contains(readName)) {
                        seen.add(readName);

                        maxReadLength.set(Math.max(maxReadLength.get(), readBlock.getReadLength()));

                        //updateMessage("Processing read " + readBlock.getReadName());

                        final IntervalTree<IMatchBlock> intervalTree = IntervalTree4Matches.computeIntervalTree(readBlock, this);

                        if (isCancelled())
                            break loop;

                        for (Interval<IMatchBlock> interval : intervalTree) {
                            final IMatchBlock matchBlock = interval.getData();
                            maxBitScore.set(Math.max(maxBitScore.get(), matchBlock.getBitScore()));
                            maxNormalizedBitScore.set(Math.max(maxNormalizedBitScore.get(), matchBlock.getBitScore() / (float) readBlock.getReadLength()));
                        }
                        final ReadLayoutPane pane = new ReadLayoutPane(cNames, readBlock.getReadLength(), intervalTree, maxReadLength, layoutWidth);
                        final Utilities.Values values = Utilities.analyze(intervalTree);
                        final int percentCover = Math.min(100, (int) Math.round((100.0 * values.coverage) / readBlock.getReadLength()));
                        final TableItem tableItem = new TableItem(readName, readBlock.getReadSequence(), className, classId, values.disjointScore, values.maxScore, values.hits, percentCover, pane);

                        tableItem.getPane().getMatchSelection().getSelectedItems().addListener(new ListChangeListener<IMatchBlock>() {
                            @Override
                            public void onChanged(Change<? extends IMatchBlock> c) {
                                if (c.next()) {
                                    if (!tableItem.getPane().getMatchSelection().isEmpty())
                                        tableView.getSelectionModel().select(tableItem);
                                }
                                if (true) { // scroll to selected item
                                    if (System.currentTimeMillis() - 100 > previousSelectionTime) {
                                        final double focusCoordinate;
                                        int focusIndex = tableItem.getPane().getMatchSelection().getFocusIndex();
                                        if (focusIndex >= 0) {
                                            IMatchBlock focusMatch = tableItem.getPane().getMatchSelection().getItems()[focusIndex];
                                            focusCoordinate = 0.5 * (focusMatch.getAlignedQueryStart() + focusMatch.getAlignedQueryEnd());
                                            double leadingWidth = 0;
                                            double lastWidth = 0;
                                            double totalWidth = 0;
                                            {
                                                int numberOfColumns = tableView.getColumns().size();
                                                int columns = 0;
                                                for (TableColumn col : tableView.getColumns()) {
                                                    if (col.isVisible()) {
                                                        if (columns < numberOfColumns - 1)
                                                            leadingWidth += col.getWidth();
                                                        else
                                                            lastWidth = col.getWidth();
                                                        totalWidth += col.getWidth();
                                                    }
                                                    columns++;
                                                }
                                            }

                                            double coordinateToShow = leadingWidth + lastWidth * (focusCoordinate / maxReadLength.get());
                                            final ScrollBar hScrollBar = FXUtilities.findScrollBar(tableView, Orientation.HORIZONTAL);

                                            if (hScrollBar != null) {
                                                final double newPos = (hScrollBar.getMax() - hScrollBar.getMin()) * ((coordinateToShow) / totalWidth);

                                                Platform.runLater(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        tableView.scrollTo(tableItem);
                                                        hScrollBar.setValue(newPos);
                                                    }
                                                });
                                            }
                                        }
                                    }

                                    // find left most and move there
                                    previousSelectionTime = System.currentTimeMillis();
                                }
                            }
                        });

                        buffer.add(tableItem);
                        if (count < 100 || buffer.size() == 50)
                            flushBuffer(buffer);
                        updateProgress(classCount + (double) it.getProgress() / (double) it.getMaximumProgress(), classIds.size());
                        updateValue(count++);

                        if (isCancelled())
                            break loop;
                    }
                }
            }
            classCount++;
        }
        flushBuffer(buffer);
        return count;
    }


    /**
     * flush the TableItem buffer by adding all the buffered TableItems to the table
     *
     * @param buffer
     */
    private void flushBuffer(ArrayList<TableItem> buffer) {
        final TableItem[] items = buffer.toArray(new TableItem[buffer.size()]);
        buffer.clear();
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                tableView.getItems().addAll(items);
            }
        });
    }
}
