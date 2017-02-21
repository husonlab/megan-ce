/*
 * IntervalNode.java
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
import java.util.Map.Entry;

/**
 * The Node class contains the interval tree information for one single node
 *
 * @author Kevin Dolan
 *         Modified by Daniel Huson, 2.2017
 */
public class IntervalNode<Type> {
    private SortedMap<Interval<Type>, List<Interval<Type>>> intervals;
    private int center;
    private IntervalNode<Type> leftNode;
    private IntervalNode<Type> rightNode;

    IntervalNode() {
        intervals = new TreeMap<>();
        center = 0;
        leftNode = null;
        rightNode = null;
    }

    IntervalNode(List<Interval<Type>> intervalList) {
        intervals = new TreeMap<>();

        SortedSet<Integer> endpoints = new TreeSet<>();

        for (Interval<Type> interval : intervalList) {
            endpoints.add(interval.getStart());
            endpoints.add(interval.getEnd());
        }

        int median = getMedian(endpoints);
        center = median;

        final List<Interval<Type>> left = new ArrayList<>();
        final List<Interval<Type>> right = new ArrayList<>();

        for (final Interval<Type> interval : intervalList) {
            if (interval.getEnd() < median)
                left.add(interval);
            else if (interval.getStart() > median)
                right.add(interval);
            else {
                List<Interval<Type>> posting = intervals.get(interval);
                if (posting == null) {
                    posting = new ArrayList<>();
                    intervals.put(interval, posting);
                }
                posting.add(interval);
            }
        }

        if (left.size() > 0)
            leftNode = new IntervalNode<>(left);
        if (right.size() > 0)
            rightNode = new IntervalNode<>(right);
    }

    /**
     * Perform a stabbing query on the node
     *
     * @param pos the pos to query at
     * @return all intervals containing pos
     */
    List<Interval<Type>> stab(int pos) {
        final List<Interval<Type>> result = new ArrayList<>();

        for (Entry<Interval<Type>, List<Interval<Type>>> entry : intervals.entrySet()) {
            if (entry.getKey().contains(pos))
                for (Interval<Type> interval : entry.getValue())
                    result.add(interval);
            else if (entry.getKey().getStart() > pos)
                break;
        }

        if (pos < center && leftNode != null)
            result.addAll(leftNode.stab(pos));
        else if (pos > center && rightNode != null)
            result.addAll(rightNode.stab(pos));
        return result;
    }

    /**
     * Perform an interval intersection query on the node
     *
     * @param target the interval to intersect
     * @return all intervals containing time
     */
    List<Interval<Type>> query(Interval<?> target) {
        final List<Interval<Type>> result = new ArrayList<>();

        for (Entry<Interval<Type>, List<Interval<Type>>> entry : intervals.entrySet()) {
            if (entry.getKey().intersects(target))
                for (Interval<Type> interval : entry.getValue())
                    result.add(interval);
            else if (entry.getKey().getStart() > target.getEnd())
                break;
        }

        if (target.getStart() < center && leftNode != null)
            result.addAll(leftNode.query(target));
        if (target.getEnd() > center && rightNode != null)
            result.addAll(rightNode.query(target));
        return result;
    }

    int getCenter() {
        return center;
    }

    void setCenter(int center) {
        this.center = center;
    }

    IntervalNode<Type> getLeft() {
        return leftNode;
    }

    void setLeft(IntervalNode<Type> left) {
        this.leftNode = left;
    }

    IntervalNode<Type> getRight() {
        return rightNode;
    }

    void setRight(IntervalNode<Type> right) {
        this.rightNode = right;
    }

    /**
     * @param set the set to look on
     * @return the median of the set, not interpolated
     */
    private int getMedian(SortedSet<Integer> set) {
        int i = 0;
        int middle = set.size() / 2;
        for (int point : set) {
            if (i == middle)
                return point;
            i++;
        }
        return 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(center).append(": ");
        for (Entry<Interval<Type>, List<Interval<Type>>> entry : intervals.entrySet()) {
            sb.append("[").append(entry.getKey().getStart()).append(",").append(entry.getKey().getEnd()).append("]:{");
            for (Interval<Type> interval : entry.getValue()) {
                sb.append("(").append(interval.getStart()).append(",").append(interval.getEnd()).append(",").append(interval.getData()).append(")");
            }
            sb.append("} ");
        }
        return sb.toString();
    }

}