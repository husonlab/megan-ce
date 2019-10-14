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

package megan.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

/**
 * list of intervals
 * Daniel Huson, 2017
 */
class IntervalList {
    private final ArrayList<IntPair> list = new ArrayList<>();
    private int covered = 0;
    private boolean isSorted = false;

    public IntervalList() {
    }

    private void updateSort() {
        covered = -1;
        // sort all the intervals:
        list.sort((p, q) -> {
            if (p.getA() < q.getA())
                return -1;
            else if (p.getA() > q.getA())
                return 1;
            else return Integer.compare(p.getB(), q.getB());
        });

        // make the intervals disjoint:
        final ArrayList<IntPair> orig = new ArrayList<>(list);
        list.clear();
        IntPair prev = null;
        for (IntPair pair : orig) {
            if (prev == null)
                prev = pair;
            else if (pair.getA() > prev.getB()) {
                list.add(prev);
                prev = new IntPair(pair.getA(), pair.getB());
            } else {
                prev.setB(Math.max(prev.getB(), pair.getB()));
            }
        }
        if (prev != null)
            list.add(prev);
        isSorted = true;
    }

    private void updateCover() {
        // recompute the amount covered:
        covered = 0;
        int lastStart = -1;
        int lastFinish = -1;

        for (IntPair pair : list) {
            if (lastStart == -1) {
                lastStart = pair.getA();
                lastFinish = pair.getB();
            } else {
                if (pair.getA() < lastFinish)
                    lastFinish = pair.getB();
                else {
                    covered += (lastFinish - lastStart + 1);
                    lastStart = pair.getA();
                    lastFinish = pair.getB();
                }
            }
        }
        if (lastStart <= lastFinish)
            covered += (lastFinish - lastStart + 1);
    }

    public int getCovered() {
        if (covered == -1) {
            if (!isSorted) {
                updateSort();
            }
            updateCover();
        }
        return covered;
    }

    public void add(int a, int b) {
        add(new IntPair(a, b));
    }

    private void add(IntPair pair) {
        list.add(pair);
        isSorted = false;
        covered = -1;
    }

    public void addAll(Collection<IntPair> pairs) {
        list.addAll(pairs);
        isSorted = false;
        covered = -1;
    }

    public void setIsSorted(boolean value) {
        isSorted = value;
        if (!isSorted)
            covered = -1;
    }

    public Iterator<IntPair> iterator() {
        return list.iterator();
    }

    public Collection<IntPair> getAll() {
        return list;
    }

    public int size() {
        return list.size();
    }

    public int computeMin() {
        int min = Integer.MAX_VALUE;
        for (IntPair pair : list) {
            min = Math.min(min, pair.a);

        }
        return min;
    }

    public int computeMax() {
        int max = Integer.MIN_VALUE;
        for (IntPair pair : list) {
            max = Math.max(max, pair.b);

        }
        return max;
    }

    private static class IntPair {
        private final int a;
        private int b;

        IntPair(int a, int b) {
            this.a = Math.min(a, b);
            this.b = Math.max(a, b);
        }

        void setB(int b) {
            this.b = b;
        }

        final int getA() {
            return a;
        }

        final int getB() {
            return b;
        }
    }
}