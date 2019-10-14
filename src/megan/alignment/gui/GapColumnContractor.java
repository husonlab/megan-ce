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
package megan.alignment.gui;

import jloda.util.Pair;

import java.util.*;

/**
 * manages contracted columns in an alignment
 * Daniel Huson, 9.2011
 */
public class GapColumnContractor {
    private final LinkedList<Pair<Integer, Integer>> origGapColumns = new LinkedList<>();
    private final Map<Integer, Integer> orig2jump = new HashMap<>();
    private final Map<Integer, Integer> layout2jump = new HashMap<>();
    private int jumped;
    private int originalColumns;
    private boolean enabled = true;

    public GapColumnContractor() {
        clear();
    }

    public void clear() {
        origGapColumns.clear();
        orig2jump.clear();
        originalColumns = 0;
        layout2jump.clear();
        jumped = 0;
    }

    /**
     * compute gap columns for alignment
     *
     * @param alignment
     */
    public void processAlignment(Alignment alignment) {
        clear();

        if (alignment != null) {
            LinkedList<Pair<Integer, Integer>> events = new LinkedList<>();
            for (int row = 0; row < alignment.getNumberOfSequences(); row++) {
                Lane lane = alignment.getLane(row);
                Pair<Integer, Integer> startEvent = new Pair<>(lane.getFirstNonGapPosition(), -1);
                events.add(startEvent);
                Pair<Integer, Integer> endEvent = new Pair<>(lane.getLastNonGapPosition(), 1);
                events.add(endEvent);
            }
            Pair<Integer, Integer>[] array = (Pair<Integer, Integer>[]) events.toArray(new Pair[0]);
            Arrays.sort(array);

            int lastStart = 0;
            int coverage = 0;
            for (Pair<Integer, Integer> event : array) {
                if (event.getSecond() == -1) // start
                {
                    if (coverage == 0) {
                        if (event.getFirst() - 1 >= 0 && lastStart != -1) {
                            origGapColumns.add(new Pair<>(lastStart, event.getFirst() - 1));
                            orig2jump.put(lastStart, event.getFirst() - lastStart);
                            lastStart = -1;
                        }
                    }
                    coverage++;
                } else if (event.getSecond() == 1) // end of an interval
                {
                    coverage--;
                    if (coverage == 0)
                        lastStart = event.getFirst();
                }
            }
            if (lastStart != -1 && lastStart < alignment.getLength()) {
                origGapColumns.add(new Pair<>(lastStart, alignment.getLength()));
                orig2jump.put(lastStart, alignment.getLength() - lastStart);
            }

            originalColumns = alignment.getLength();

            for (Pair<Integer, Integer> col : origGapColumns) {
                layout2jump.put(col.getFirst() - jumped, orig2jump.get(col.getFirst()));
                jumped += orig2jump.get(col.getFirst());
            }

            /*
            for (Pair<Integer, Integer> col : origGapColumns) {
                System.err.println("Original gap column: " + col);
                System.err.println("jump: " + orig2jump.get(col.getFirst()));
            }
            for (Pair<Integer, Integer> col : layoutGapColumns) {
                System.err.println("Layout gap column: " + col);
                System.err.println("jump: " + layout2jump.get(col.getFirst()));
            }
            System.err.println("Original columns: " + getLastOriginalColumn());
            System.err.println("Layout columns:    " + getLastLayoutColumn());
            */
        }
    }

    /**
     * get the first column to be draw
     *
     * @return 0
     */
    public int getFirstLayoutColumn() {
        return 0;
    }

    /**
     * get last column to be layed out in the alignment window
     *
     * @return number of columns to be drawn
     */
    public int getLastLayoutColumn() {
        if (enabled)
            return originalColumns - jumped;
        else
            return originalColumns;
    }

    /**
     * gets length of layed out alignment
     *
     * @return length
     */
    public int getLayoutLength() {
        return getLastLayoutColumn() - getFirstLayoutColumn();
    }

    /**
     * get the first original column. Usually 0
     *
     * @return first original column
     */
    public int getFirstOriginalColumn() {
        return 0;
    }

    /**
     * gets the last original column. Usually larger than the last layout column because of collapsed gaps
     *
     * @return
     */
    public int getLastOriginalColumn() {
        return originalColumns;
    }

    /**
     * given an original column (0..length), returns the number of gaps to jump, if any
     *
     * @param origColumn
     * @return number of gaps to jump, or 0
     */
    public int getJumpBeforeOriginalColumn(int origColumn) {
        if (enabled) {
            Integer result = orig2jump.get(origColumn);
            return result == null ? 0 : result;
        }
        return 0;
    }

    /**
     * given layout column, returns the number of gaps that are jumped, if any
     *
     * @param layoutCol
     * @return number of gaps jumped, or 0
     */
    public int getJumpBeforeLayoutColumn(int layoutCol) {
        if (enabled) {
            Integer result = layout2jump.get(layoutCol);
            return result == null ? 0 : result;
        }
        return 0;
    }

    /**
     * given layout column, returns the number of gaps that are jumped, if any
     *
     * @param layoutCol
     * @return number of gaps jumped, or 0
     */
    public int getTotalJumpBeforeLayoutColumn(int layoutCol) {
        if (enabled) {
            int jump = 0;
            for (Map.Entry<Integer, Integer> entry : layout2jump.entrySet()) {
                if (entry.getKey() <= layoutCol)
                    jump += entry.getValue();
            }
            return jump;
        }
        return 0;
    }

    /**
     * get all jump positions
     *
     * @return jump positions
     */
    public SortedSet<Integer> getJumpPositionsRelativeToOriginalColumns() {
        SortedSet<Integer> positions = new TreeSet<>();
        if (enabled)
            positions.addAll(orig2jump.keySet());
        return positions;

    }

    /**
     * get all jump positions
     *
     * @return jump positions
     */
    public SortedSet<Integer> getJumpPositionsRelativeToLayoutColumns() {
        SortedSet<Integer> positions = new TreeSet<>();
        if (enabled)
            positions.addAll(layout2jump.keySet());
        return positions;

    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
