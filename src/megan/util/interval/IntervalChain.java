/*
 *  Copyright (C) 2015 Daniel H. Huson
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

package megan.util.interval;

/**
 * chain of disjoint intervals
 * Daniel Huson, 3.2018
 */
public class IntervalChain {
    private Interval head;

    public void clear() {
        head = null;
    }

    /**
     * add an interval to the chain
     *
     * @param a start, 1-based, inclusive
     * @param b end, 1-based, inclusive
     */
    public void add(int a, int b) {
        if (a > b) { // swap if wrong way around
            int tmp = a;
            a = b;
            b = tmp;
        }

        if (head == null) {
            head = new Interval(a, b, null);
            return;
        }

        Interval lastBefore;
        if (head.getA() <= a) {
            lastBefore = head;
            while (lastBefore.getNext() != null && lastBefore.getNext().getA() <= a)
                lastBefore = lastBefore.getNext();
            if (lastBefore.getB() >= b)
                return; // contained...
        } else
            lastBefore = null;

        Interval firstAfter = (lastBefore != null ? lastBefore : head);
        while (firstAfter != null && firstAfter.getB() < b)
            firstAfter = firstAfter.getNext();

        if (lastBefore == null && firstAfter == null) {
            head = new Interval(a, b, null); // this when all existing intervals are contained in [a,b]
        }

        if (lastBefore == null && firstAfter != null) {
            if (overlaps(firstAfter, a, b)) {
                firstAfter.setA(a);
            } else {
                head = new Interval(a, b, head);
            }
        }
        if (lastBefore != null && firstAfter == null) {
            if (overlaps(lastBefore, a, b)) {
                lastBefore.setB(b);
            } else {
                lastBefore.setNext(new Interval(a, b, null));
            }
        }
        if (lastBefore != null && firstAfter != null && lastBefore != firstAfter) {
            if (overlaps(firstAfter, a, b)) {
                if (overlaps(lastBefore, a, b)) {
                    lastBefore.setB(firstAfter.getB());
                    lastBefore.setNext(firstAfter.getNext());
                } else {
                    firstAfter.setA(a);
                }
            } else { // doesn't overlap lastBefore
                if (overlaps(lastBefore, a, b)) {
                    firstAfter.setA(a);
                } else { // doesn't overlap any
                    lastBefore.setNext(new Interval(a, b, firstAfter));
                }
            }
        }
    }

    /**
     * does interval overlap with [a,b]?
     *
     * @param interval
     * @param a
     * @param b
     * @return true, if overlaps
     */
    private boolean overlaps(Interval interval, int a, int b) {
        return interval.a <= b && interval.b >= a;
    }

    /**
     * report the length of the intervals
     *
     * @return length
     */
    public int getLength() {
        int count = 0;
        Interval interval = head;
        while (interval != null) {
            count += (interval.getB() - interval.getA() + 1);
            interval = interval.getNext();
        }
        return count;
    }

    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("Intervals: ");

        Interval interval = head;
        while (interval != null) {
            buf.append(String.format("[%d,%d] ", interval.getA(), interval.getB()));
            interval = interval.getNext();
        }
        buf.append(" length: ").append(getLength());
        return buf.toString();
    }

    /**
     * single-linked interval node
     */
    class Interval {
        private int a;
        private int b;
        private Interval next;

        public Interval(int a, int b, Interval next) {
            this.a = a;
            this.b = b;
            this.next = next;
        }


        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        public int getB() {
            return b;
        }

        public void setB(int b) {
            this.b = b;
        }

        public Interval getNext() {
            return next;
        }

        public void setNext(Interval next) {
            this.next = next;
        }
    }

    public static void main(String[] args) {

        int[] pairs = {11, 20, 71, 80, 31, 41, 50, 60, 36, 55, 21, 25, 68, 72, 15, 18};

        final IntervalChain intervalChain = new IntervalChain();

        for (int i = 0; i < pairs.length; i += 2) {
            System.err.println("Adding: " + pairs[i] + "," + pairs[i + 1]);
            intervalChain.add(pairs[i], pairs[i + 1]);
            System.err.println(intervalChain);
        }
    }

}
