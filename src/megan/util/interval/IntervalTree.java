/*
 * IntervalTree.java
 * do What The F... you want to Public License
 *  Version 1.0, March 2000
 *  * Copyright (C) 2000 Banlu Kemiyatorn (]d).
 * 136 Nives 7 Jangwattana 14 Laksi Bangkok
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * Ok, the purpose of this license is simple and you just
 * DO WHAT THE F... YOU WANT TO.
 */

package megan.util.interval;

import java.util.*;

/**
 * An Interval Tree is essentially a map from intervals to objects, which
 * can be queried for all data associated with a particular interval of
 * time
 *
 * @param <T> the type of objects to associate
 * @author Kevin Dolan
 *         Modified by Daniel Huson, 2.2017
 */
public class IntervalTree<T> implements Iterable<Interval<T>> {
    private IntervalNode<T> head;
    private final List<Interval<T>> intervalList;
    private boolean inSync;
    private boolean sorted;
    private int size;

    /**
     * Instantiate a new interval tree with no intervals
     */
    public IntervalTree() {
        this.head = new IntervalNode<>();
        this.intervalList = new ArrayList<>();
        this.inSync = true;
        this.sorted = true;
        this.size = 0;
    }

    /**
     * Instantiate and build an interval tree with a preset list of intervals
     *
     * @param intervalList the list of intervals to use
     */
    public IntervalTree(List<Interval<T>> intervalList) {
        this.head = new IntervalNode<>(intervalList);
        this.intervalList = new ArrayList<>(intervalList);
        this.inSync = true;
        this.sorted = false;
        this.size = intervalList.size();
    }

    /**
     * clears the tree
     */
    public void clear() {
        this.head = new IntervalNode<>();
        this.intervalList.clear();
        this.inSync = true;
        this.sorted = true;
        this.size = 0;
    }

    /**
     * Perform a stabbing query, returning the associated data
     * Will rebuild the tree if out of sync
     *
     * @param pos the pos to stab
     * @return the data associated with all intervals that contain pos
     */
    public List<T> get(int pos) {
        final List<T> result = new ArrayList<>();
        for (Interval<T> interval : getIntervals(pos))
            result.add(interval.getData());
        return result;
    }

    /**
     * Perform a stabbing query, returning the interval objects
     * Will rebuild the tree if out of sync
     *
     * @param pos the pos to stab
     * @return all intervals that contain pos
     */
    public List<Interval<T>> getIntervals(int pos) {
        buildTree();
        return head.stab(pos);
    }

    /**
     * Perform an interval query, returning the associated data
     * Will rebuild the tree if out of sync
     *
     * @param start the start of the interval to check
     * @param end   the end of the interval to check
     * @return the data associated with all intervals that intersect target
     */
    public List<T> get(int start, int end) {
        final List<T> result = new ArrayList<>();
        for (Interval<T> interval : getIntervals(start, end))
            result.add(interval.getData());
        return result;
    }

    /**
     * Perform an interval query, returning the interval objects
     * Will rebuild the tree if out of sync
     *
     * @param target the interval to check
     * @return all intervals that intersect target
     */
    public List<Interval<T>> getIntervals(Interval<T> target) {
        buildTree();
        return head.query(target);
    }

    /**
     * Perform an interval query, returning the interval objects
     * Will rebuild the tree if out of sync
     *
     * @param start the start of the interval to check
     * @param end   the end of the interval to check
     * @return all intervals that intersect target
     */
    public List<Interval<T>> getIntervals(int start, int end) {
        return getIntervals(new Interval<T>(start, end, null));
    }

    /**
     * Add an interval object to the interval tree's list
     * Will not rebuild the tree until the next query or call to build
     *
     * @param interval the interval object to add
     */
    public void add(Interval<T> interval) {
        intervalList.add(interval);
        inSync = false;
        sorted = false;
    }

    /**
     * adds a list of intervals
     * Will not rebuild the next query or call to build
     *
     * @param intervals
     */
    public void addAll(Collection<Interval<T>> intervals) {
        intervalList.addAll(intervals);
        inSync = false;
        sorted = false;
    }

    /**
     * sets a list of intervals
     * Will not rebuild the next query or call to build
     *
     * @param intervals
     */
    public void setAll(Collection<Interval<T>> intervals) {
        clear();
        intervalList.addAll(intervals);
        inSync = false;
        sorted = false;
    }

    /**
     * remove an interval
     *
     * @param interval
     * @return true, if was contained
     */
    public boolean remove(Interval<T> interval) {
        boolean removed = intervalList.remove(interval);
        if (removed)
            inSync = false;
        return removed;
    }

    /**
     * remove a collection of intervals
     *
     * @param intervals
     * @return true, if something was contained
     */
    public boolean removeAll(Collection<Interval<T>> intervals) {
        boolean removed = intervalList.removeAll(intervals);
        if (removed)
            inSync = false;
        return removed;
    }

    /**
     * Add an interval object to the interval tree's list
     * Will not rebuild the tree until the next query or call to build
     *
     * @param begin the beginning of the interval
     * @param end   the end of the interval
     * @param data  the data to associate
     */
    public void add(int begin, int end, T data) {
        intervalList.add(new Interval<>(begin, end, data));
        inSync = false;
        sorted = false;
    }

    /**
     * Determine whether this interval tree is currently a reflection of all intervals in the interval list
     *
     * @return true if no changes have been made since the last build
     */
    public boolean inSync() {
        return inSync;
    }

    /**
     * Build the interval tree to reflect the list of intervals, if not already in sync
     */
    public void buildTree() {
        if (!inSync) {
            head = new IntervalNode<>(intervalList);
            inSync = true;
            size = intervalList.size();
        }
    }

    /**
     * gets a copy of the list of all intervals
     *
     * @return intervals
     */
    public Collection<Interval<T>> values() {
        sortList();
        return new ArrayList<>(intervalList);
    }

    /**
     * iterates over all intervals
     *
     * @return
     */
    public Iterator<Interval<T>> iterator() {
        sortList();
        return intervalList.iterator();
    }

    /**
     * sets the interval list returned by values() or iterator(), if not already sorted
     */
    private void sortList() {
        if (!sorted) {
            intervalList.sort(new Comparator<Interval<T>>() {
                @Override
                public int compare(Interval<T> a, Interval<T> b) {
                    return a.compareTo(b);
                }
            });
            sorted = true;
        }
    }


    /**
     * returns an iterator overall all members in the order they were inserted
     *
     * @return members
     */
    public Iterator<Interval<T>> insertionOrderIterator() {
        return intervalList.iterator();
    }

    /**
     * @return the number of entries in the currently built interval tree
     */
    public int currentSize() {
        return size;
    }

    /**
     * @return the number of entries in the interval list, equal to .currentSize() if inSync()
     */
    public int size() {
        return intervalList.size();
    }

    @Override
    public String toString() {
        return nodeString(head, 0);
    }

    private String nodeString(IntervalNode<T> node, int level) {
        if (node == null)
            return "";
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++)
            sb.append("\t");
        sb.append(node).append("\n");
        sb.append(nodeString(node.getLeft(), level + 1));
        sb.append(nodeString(node.getRight(), level + 1));
        return sb.toString();
    }

}