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
package megan.clusteranalysis.nnet;

import java.util.BitSet;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * a weighted split
 * Daniel Huson, 6.2007
 */
public class Split implements Comparable, Comparator {
    private final BitSet A;
    private final BitSet B;
    private double weight;
    private final List weightList; // list of weights assigned to this split in different trees
    private double confidence; // confidence in this split?

    /**
     * constructor
     */
    public Split() {
        this(null, null, 1, 1);
    }

    /**
     * constructor
     *
     * @param A
     * @param B
     */
    public Split(BitSet A, BitSet B) {
        this(A, B, 1, 1);
    }

    /**
     * constructor
     *
     * @param A
     * @param B
     * @param weight
     */
    public Split(BitSet A, BitSet B, double weight) {
        this(A, B, weight, 1);
    }

    /**
     * constructor
     *
     * @param A
     * @param B
     * @param weight
     */
    public Split(BitSet A, BitSet B, double weight, double confidence) {
        this.A = (A != null ? (BitSet) A.clone() : new BitSet());
        this.B = (B != null ? (BitSet) B.clone() : new BitSet());
        this.weight = weight;
        this.weightList = new LinkedList();
        this.confidence = confidence;
    }

    /**
     * clone this split
     *
     * @return a clone of this split
     */
    public Object clone() {
        Split result = new Split();
        result.copy(this);
        return result;
    }

    /**
     * copy a split
     *
     * @param split
     */
    private void copy(Split split) {
        setA(split.getA());
        setB(split.getB());
        setWeight(split.getWeight());
        weightList.addAll(split.weightList);
    }

    /**
     * get the A part
     *
     * @return A part
     */
    public BitSet getA() {
        return A;
    }

    /**
     * get  the B part
     *
     * @return B part
     */
    public BitSet getB() {
        return B;
    }

    /**
     * does A part contain taxon?
     *
     * @param taxon
     * @return true if contains taxon
     */
    public boolean isAcontains(int taxon) {
        return A.get(taxon);
    }

    /**
     * does B part containt taxon?
     *
     * @param taxon
     * @return true if contains taxon
     */
    public boolean isBcontains(int taxon) {
        return B.get(taxon);
    }

    /**
     * does A part contain set H?
     *
     * @param H
     * @return true if contains H
     */
    public boolean isAcontains(BitSet H) {
        BitSet M = ((BitSet) A.clone());
        M.and(H);
        return M.cardinality() == H.cardinality();
    }

    /**
     * does B part contain set H?
     *
     * @param H
     * @return true if contains H
     */
    public boolean isBcontains(BitSet H) {
        BitSet M = ((BitSet) B.clone());
        M.and(H);
        return M.cardinality() == H.cardinality();
    }

    /**
     * does A part intersect set H?
     *
     * @param H
     * @return true if intersects H
     */
    public boolean isAintersects(BitSet H) {
        return A.intersects(H);
    }

    /**
     * does B part intersect set H?
     *
     * @param H
     * @return true if intersects H
     */
    public boolean isBintersects(BitSet H) {
        return B.intersects(H);
    }

    /**
     * gets the split part containing taxon
     *
     * @param taxon
     * @return split part containing taxon, or null
     */
    public BitSet getPartContainingTaxon(int taxon) {
        if (A.get(taxon))
            return A;
        else if (B.get(taxon))
            return B;
        else
            return null;
    }

    /**
     * gets  the first split part not containing taxon
     *
     * @param taxon
     * @return split part containing taxon, or null
     */
    public BitSet getPartNotContainingTaxon(int taxon) {
        if (!A.get(taxon))
            return A;
        else if (!B.get(taxon))
            return B;
        else
            return null;
    }

    /**
     * does the split split the given taxa?
     *
     * @param taxa
     * @return true, if both A and B intersects taxa
     */
    public boolean splitsTaxa(BitSet taxa) {
        return A.intersects(taxa) && B.intersects(taxa);
    }

    /**
     * returns true, if split separates taxa a and b
     *
     * @param a
     * @param b
     * @return true, if separates
     */
    public boolean separates(int a, int b) {
        return A.get(a) && B.get(b) || A.get(b) && B.get(a);
    }

    /**
     * returns true, if split separates the given set of taxa
     *
     * @param H
     * @return true, if separates
     */
    public boolean separates(BitSet H) {
        return A.intersects(H) && B.intersects(H);
    }

    /**
     * gets the weight of split, or 1, of not set
     *
     * @return weight
     */
    public double getWeight() {
        return weight;
    }

    /**
     * set the weight
     *
     * @param weight
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * sets the A part of the split
     *
     * @param A
     */
    private void setA(BitSet A) {
        this.A.clear();
        this.A.or(A);
    }

    /**
     * sets the B part of the split
     *
     * @param B
     */
    private void setB(BitSet B) {
        this.B.clear();
        this.B.or(B);
    }

    /**
     * sets the split to A | B   with weight 1
     *
     * @param A
     * @param B
     */
    public void set(BitSet A, BitSet B) {
        set(A, B, 1);
    }

    /**
     * sets the split to A |B with given weight
     *
     * @param A
     * @param B
     * @param weight
     */
    private void set(BitSet A, BitSet B, double weight) {
        this.A.clear();
        this.A.or(A);
        this.B.clear();
        this.B.or(B);
        this.weight = weight;
    }

    /**
     * are the two splits equalOverShorterOfBoth as set bipartitionings (ignoring weights)
     *
     * @param object
     * @return true, if equalOverShorterOfBoth
     */
    public boolean equals(Object object) {
        return object instanceof Split && equals((Split) object);
    }

    /**
     * are the two splits equalOverShorterOfBoth as set bipartitionings (ignoring weights)
     *
     * @param split
     * @return true, if equalOverShorterOfBoth
     */
    private boolean equals(Split split) {
        return (A.equals(split.A) && B.equals(split.B)) || (A.equals(split.B) && B.equals(split.A));
    }

    /**
     * compare to a split object
     *
     * @param o
     * @return -1, 0 or 1, depending on relation
     */
    public int compareTo(Object o) {
        Split split = (Split) o;
        BitSet P = getFirstPart();
        BitSet Q = split.getFirstPart();

        int a = P.nextSetBit(0);
        int b = Q.nextSetBit(0);
        while (a > -1 && b > -1) {
            if (a < b)
                return -1;
            else if (a > b)
                return 1;
            a = P.nextSetBit(a + 1);
            b = Q.nextSetBit(b + 1);
        }
        if (a < b)
            return -1;
        else if (a > b)
            return 1;

        P = getSecondPart();
        Q = split.getSecondPart();

        a = P.nextSetBit(0);
        b = Q.nextSetBit(0);
        while (a > -1 && b > -1) {
            if (a < b)
                return -1;
            else if (a > b)
                return 1;
            a = P.nextSetBit(a + 1);
            b = Q.nextSetBit(b + 1);
        }
        return Integer.compare(a, b);
    }

    /**
     * compares two splits
     *
     * @param o1
     * @param o2
     * @return comparison
     */
    public int compare(Object o1, Object o2) {
        Split split1 = (Split) o1;
        return split1.compareTo(o2);
    }

    /**
     * gets the lexicographic first part
     *
     * @return first part
     */
    private BitSet getFirstPart() {
        if (A.nextSetBit(0) < B.nextSetBit(0))
            return A;
        else
            return B;
    }

    /**
     * gets the lexicographic second part
     *
     * @return first part
     */
    private BitSet getSecondPart() {
        if (A.nextSetBit(0) >= B.nextSetBit(0))
            return A;
        else
            return B;
    }

    /**
     * gets the hash code
     *
     * @return hash code
     */
    public int hashCode() {
        if (A.nextSetBit(0) < B.nextSetBit(0))
            return A.hashCode() + 37 * B.hashCode();
        else
            return B.hashCode() + 37 * A.hashCode();
    }

    /**
     * gets the split size, i.e. the cardinality of the smaller split part
     *
     * @return split size
     */
    public int getSplitSize() {
        return Math.min(A.cardinality(), B.cardinality());

    }

    /**
     * gets the cardinality of the total set of both parts
     *
     * @return cardinality of union
     */
    public int getCardinality() {
        return A.cardinality() + B.cardinality();
    }


    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    /**
     * add weight to list of weights
     *
     * @param weight
     */
    public void addToWeightList(double weight) {
        weightList.add(weight);
    }

    /**
     * get the weight list
     *
     * @return weight list
     */
    public List getWeightList() {
        return weightList;
    }

    /**
     * gets a string representation
     *
     * @return string
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append(" taxa=").append(A.cardinality() + B.cardinality());
        buf.append(" weight=").append((float) weight);
        buf.append(" confidence=").append((float) confidence);

        buf.append(":");

        for (int a = A.nextSetBit(0); a != -1; a = A.nextSetBit(a + 1))
            buf.append(" ").append(a);
        buf.append(" |");
        for (int b = B.nextSetBit(0); b != -1; b = B.nextSetBit(b + 1))
            buf.append(" ").append(b);
        return buf.toString();
    }

    /**
     * is this split compatible with the given one?
     *
     * @param split
     * @return true, if compatible
     */
    public boolean isCompatible(Split split) {
        return !(getA().intersects(split.getA()) && getA().intersects(split.getB())
                && getB().intersects(split.getA()) && getB().intersects(split.getB()));
    }

    /**
     * is split trivial?
     *
     * @return true, if trivial
     */
    public boolean isTrivial() {
        return getA().size() == 1 || getB().size() == 1;
    }

    /**
     * gets all taxa mentioned in this split
     *
     * @return taxa
     */
    public BitSet getTaxa() {
        BitSet result = (BitSet) getA().clone();
        result.or(getB());
        return result;
    }

    /**
     * gets the number of taxa in this split
     *
     * @return number of taxa
     */
    public int getNumberOfTaxa() {
        return getA().cardinality() + getB().cardinality();
    }

    /**
     * gets the split induced by the given taxa set, or null, if result is not a proper split
     *
     * @param taxa
     * @return split or null
     */
    public Split getInduced(BitSet taxa) {
        Split result = (Split) clone();
        result.getA().and(taxa);
        result.getB().and(taxa);
        if (result.getA().cardinality() > 0 && result.getB().cardinality() > 0)
            return result;
        else
            return null;
    }

    /**
     * get a comparator that compares splits by decreasing weight
     *
     * @return weight-based comparator
     */
    public static Comparator createWeightComparator() {
        return (o1, o2) -> {
            Split split1 = (Split) o1;
            Split split2 = (Split) o2;
            if (split1.getWeight() > split2.getWeight())
                return -1;
            else if (split1.getWeight() < split2.getWeight())
                return 1;
            else
                return split1.compareTo(split2);
        };
    }

    /**
     * get the A side (i=0) or B side (else) of the split
     *
     * @param i
     * @return A or B side of split
     */
    public BitSet getSide(int i) {
        if (i % 2 == 0)
            return getA();
        else
            return getB();
    }

    /**
     * set the split to A vs complement of A
     *
     * @param A
     * @param ntax
     * @param weight
     */
    public void set(BitSet A, int ntax, double weight) {
        BitSet B = new BitSet();
        for (int i = 1; i <= ntax; i++)
            if (!A.get(i))
                B.set(i);
        set(A, B, weight);
    }
}
