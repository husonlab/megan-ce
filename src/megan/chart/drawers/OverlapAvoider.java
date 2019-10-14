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
package megan.chart.drawers;

import jloda.util.Pair;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;

/**
 * datastructure for avoiding overlaps of rectangles
 * Daniel Huson, 7.2012
 */
public class OverlapAvoider<T> {
    private final Vector<Pair<Rectangle2D, T>> data = new Vector<>();
    private int currentComparison = 0;

    private Rectangle2D previousHit;

    private final SortedSet<Integer> sortedByMinX;
    private final SortedSet<Integer> sortedByMaxX;
    private final SortedSet<Integer> sortedByMinY;
    private final SortedSet<Integer> sortedByMaxY;
    private final Rectangle2D bbox = new Rectangle2D.Double();

    /**
     * constructor
     */
    public OverlapAvoider() {
        sortedByMinX = new TreeSet<>((id1, id2) -> {
            Double d1 = (id1 != currentComparison ? data.get(id1).getFirst().getMinX() : data.get(id1).getFirst().getMaxX());
            Double d2 = (id2 != currentComparison ? data.get(id2).getFirst().getMinX() : data.get(id2).getFirst().getMaxX());
            if (d1 < d2)
                return -1;
            else if (d1 > d2)
                return 1;
            else if (id1 < id2)
                return -1;
            else if (id1 > id2)
                return 1;
            else
                return 0;
        });
        sortedByMaxX = new TreeSet<>((id1, id2) -> {
            Double d1 = (id1 != currentComparison ? data.get(id1).getFirst().getMaxX() : data.get(id1).getFirst().getMinX());
            Double d2 = (id2 != currentComparison ? data.get(id2).getFirst().getMaxX() : data.get(id2).getFirst().getMinX());
            if (d1 < d2)
                return -1;
            else if (d1 > d2)
                return 1;
            else if (id1 < id2)
                return -1;
            else if (id1 > id2)
                return 1;
            else
                return 0;
        });
        sortedByMinY = new TreeSet<>((id1, id2) -> {
            Double d1 = (id1 != currentComparison ? data.get(id1).getFirst().getMinY() : data.get(id1).getFirst().getMaxY());
            Double d2 = (id2 != currentComparison ? data.get(id2).getFirst().getMinY() : data.get(id2).getFirst().getMaxY());
            if (d1 < d2)
                return -1;
            else if (d1 > d2)
                return 1;
            else if (id1 < id2)
                return -1;
            else if (id1 > id2)
                return 1;
            else
                return 0;
        });
        sortedByMaxY = new TreeSet<>((id1, id2) -> {
            Double d1 = (id1 != currentComparison ? data.get(id1).getFirst().getMaxY() : data.get(id1).getFirst().getMinY());
            Double d2 = (id2 != currentComparison ? data.get(id2).getFirst().getMaxY() : data.get(id2).getFirst().getMinY());
            if (d1 < d2)
                return -1;
            else if (d1 > d2)
                return 1;
            else if (id1 < id2)
                return -1;
            else if (id1 > id2)
                return 1;
            else
                return 0;
        });
    }

    /**
     * if rectangle does not overlap any rectangle already contained, then add it
     *
     * @param pair
     * @return true if added, false if overlaps a rectangle already present
     */
    public boolean addIfDoesNotOverlap(Pair<Rectangle2D, T> pair) {
        if (previousHit != null && pair.get1().intersects(previousHit))
            return false;
        if (data.size() == data.capacity())
            data.ensureCapacity(data.size() + 1);
        int which = data.size();
        data.add(pair);

        currentComparison = which;  // need to set this global variable so sorting uses reversed interval bounds in
        // headset and tailset computations
        BitSet startingX = getAll(sortedByMinX.headSet(which));
        andAll(sortedByMaxX.tailSet(which), startingX);
        boolean ok = (startingX.cardinality() == 0);

        if (!ok) {
            andAll(sortedByMinY.headSet(which), startingX);
            ok = (startingX.cardinality() == 0);
            if (!ok) {
                andAll(sortedByMaxY.tailSet(which), startingX);
                ok = (startingX.cardinality() == 0);
            }
        }
        currentComparison = -1;

        if (!ok) {
            int id = startingX.nextSetBit(0);
            previousHit = data.get(id).get1();
            data.remove(which);
            return false;
        } else {
            sortedByMaxX.add(which);
            sortedByMinX.add(which);
            sortedByMaxY.add(which);
            sortedByMinY.add(which);
            if (data.size() == 0)
                bbox.setRect(pair.get1());
            else
                bbox.add(pair.get1());
            return true;
        }
    }

    private BitSet getAll(Set<Integer> set) {
        BitSet result = new BitSet();
        for (Integer i : set)
            result.set(i);
        return result;
    }

    private void andAll(Set<Integer> set, BitSet bitSet) {
        bitSet.and(getAll(set));
    }

    /**
     * get an iterator over all members
     *
     * @return iterator
     */
    public Iterator<Pair<Rectangle2D, T>> iterator() {
        return data.iterator();
    }

    /**
     * erase
     */
    public void clear() {
        data.clear();
        sortedByMaxX.clear();
        sortedByMinX.clear();
        sortedByMaxY.clear();
        sortedByMinY.clear();
        bbox.setRect(0, 0, 0, 0);
        previousHit = null;
    }

    /**
     * size
     *
     * @return size
     */
    public int size() {
        return data.size();
    }

    /**
     * get the bounding box
     *
     * @return bounding box
     */
    public Rectangle2D getBoundingBox() {
        return (Rectangle2D) bbox.clone();
    }

    /**
     * get the bounding box
     *
     * @param rect
     */
    public void getBoundingBox(Rectangle rect) {
        rect.setRect((int) Math.round(bbox.getX()), (int) Math.round(bbox.getY()), (int) Math.round(bbox.getWidth()), (int) Math.round(bbox.getHeight()));
    }
}
