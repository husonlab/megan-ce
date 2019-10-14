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
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Pair;
import jloda.util.interval.Interval;
import jloda.util.interval.IntervalTree;
import megan.algorithms.IntervalTree4Matches;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Document;
import megan.data.IClassificationBlock;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.fx.FXUtilities;

import java.util.*;


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
        final Set<Long> seen = new HashSet<>();
        final ArrayList<TableItem> buffer = new ArrayList<>(100);

        final Classification classification = ClassificationManager.get(classificationName, true);
        final IClassificationBlock classificationBlock = doc.getConnector().getClassificationBlock(classificationName);
        if (classificationBlock == null)
            return 0;
        classIds.retainAll(classificationBlock.getKeySet());
        updateProgress(-1, classIds.size());

        boolean warnedAboutUnknownReadLength = false;

        int count = 0;
        int classCount = 0;
        loop:
        for (Integer classId : classIds) {
            updateMessage("Setting up " + classificationName + "...");

            String className = classification.getName2IdMap().get(classId);
            if (className == null)
                className = "[" + classId + "]";

            updateMessage("Loading '" + classification.getName2IdMap().get(classId) + "'... (" + doc.getConnector().getClassSize(classificationName, classId) + " reads)");


            final Set<String> readsToUse; // todo: need to add this to GUI?
            readsToUse = null;

            try (final IReadBlockIterator it = doc.getConnector().getReadsIterator(classificationName, classId, doc.getMinScore(), doc.getMaxExpected(), true, true)) {
                while (it.hasNext()) {
                    final IReadBlock readBlock = it.next();
                    final String readName = readBlock.getReadName();

                    // make sure we have a useful read length:
                    int readLength = readBlock.getReadLength();
                    if (readLength == 0 && readBlock.getReadSequence() != null && readBlock.getReadSequence().length() > 0)
                        readLength = readBlock.getReadSequence().length();
                    if (readLength == 0) {
                        for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
                            final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                            readLength = Math.max(readLength, matchBlock.getAlignedQueryStart());
                            readLength = Math.max(readLength, matchBlock.getAlignedQueryEnd());
                        }
                        if (readLength > 0 && !warnedAboutUnknownReadLength) {
                            NotificationsInSwing.showWarning(null, "Precise read lengths unknown, using end of last alignments as lower bound", 10000);
                            warnedAboutUnknownReadLength = true;
                        }
                    }
                    if (readLength == 0)
                        readLength = 1;

                    final long uid = readBlock.getUId();
                    if (!seen.contains(uid)) {
                        if (uid != 0)
                            seen.add(uid);

                        maxReadLength.set(Math.max(maxReadLength.get(), readLength));

                        //updateMessage("Processing read " + readBlock.getReadName());

                        final IntervalTree<IMatchBlock> intervalTree = IntervalTree4Matches.computeIntervalTree(readBlock, this, null);

                        if (isCancelled())
                            break loop;

                        for (Interval<IMatchBlock> interval : intervalTree) {
                            final IMatchBlock matchBlock = interval.getData();
                            maxBitScore.set(Math.max(maxBitScore.get(), matchBlock.getBitScore()));
                            maxNormalizedBitScore.set(Math.max(maxNormalizedBitScore.get(), matchBlock.getBitScore() / (float) readLength));
                        }
                        final ReadLayoutPane pane = new ReadLayoutPane(cNames, readLength, intervalTree, maxReadLength, layoutWidth);
                        final int percentCover = Math.min(100, (int) Math.round((100.0 * intervalTree.getCovered()) / readLength));
                        final TableItem tableItem = new TableItem(readName, readLength, readBlock.getReadSequence(), className, classId, readBlock.getNumberOfAvailableMatchBlocks(), percentCover, pane);

                        tableItem.getPane().getMatchSelection().getSelectedItems().addListener(createChangeListener(tableItem, pane.previousSelectionTimeProperty()));

                        buffer.add(tableItem);
                        if (count < 100 || buffer.size() == 100)
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
    private void flushBuffer(Collection<TableItem> buffer) {
        final TableItem[] items = buffer.toArray(new TableItem[0]);
        buffer.clear();
        Platform.runLater(() -> tableView.getItems().addAll(items));
    }

    private ListChangeListener<IMatchBlock> createChangeListener(final TableItem tableItem, final LongProperty previousSelectionTime) {
        final ReadLayoutPane pane = tableItem.getPane();
        return c -> {
            if (c.next()) {

                if (!pane.getMatchSelection().isEmpty())
                    tableView.getSelectionModel().select(tableItem);
            }
            if (System.currentTimeMillis() - 200 > previousSelectionTime.get()) { // only if sufficient time has passed since last scroll...
                try {
                    final double focusCoordinate;
                    int focusIndex = pane.getMatchSelection().getFocusIndex();
                    if (focusIndex >= 0 && pane.getMatchSelection().getItems()[focusIndex] != null) {
                        final IMatchBlock focusMatch = pane.getMatchSelection().getItems()[focusIndex];
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

                        final double coordinateToShow = leadingWidth + lastWidth * (focusCoordinate / maxReadLength.get());
                        final ScrollBar hScrollBar = FXUtilities.findScrollBar(tableView, Orientation.HORIZONTAL);

                        if (hScrollBar != null) { // should never be null, but best to check...
                            final double newPos = (hScrollBar.getMax() - hScrollBar.getMin()) * ((coordinateToShow) / totalWidth);

                            Platform.runLater(() -> {
                                tableView.scrollTo(tableItem);
                                hScrollBar.setValue(newPos);
                            });
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            previousSelectionTime.set(System.currentTimeMillis());
        };
    }

    static class LongestReadsFilter {
        private final TreeSet<Pair<String, Integer>> set;
        private final int capacity;

        LongestReadsFilter(int capacity) {
            this.capacity = capacity;
            this.set = new TreeSet<>((o1, o2) -> {
                if (o1.getSecond() > o2.getSecond())
                    return -1;
                else if (o1.getSecond() < o2.getSecond())
                    return 1;
                else
                    return o1.getFirst().compareTo(o2.getFirst());
            });
        }

        void add(String readName, int readLength) {
            set.add(new Pair<>(readName, readLength));
            if (set.size() > capacity)
                set.remove(set.last());
        }

        Set<String> computeValues() {
            final HashSet<String> values = new HashSet<>();
            for (Pair<String, Integer> value : set) {
                values.add(value.getFirst());
            }
            return values;
        }

        public void clear() {
            set.clear();
        }

        public int size() {
            return set.size();
        }

    }
}
