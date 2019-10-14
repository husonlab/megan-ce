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

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import megan.clusteranalysis.tree.Taxa;

import java.util.*;

/**
 * a collection of splits
 * Daniel Huson, 6.2007
 */
public class SplitSystem {
    private int nsplits;

    private final Map index2split;
    private final Map split2index;

    /**
     * constructor
     */
    public SplitSystem() {
        nsplits = 0;
        index2split = new HashMap();
        split2index = new HashMap();
    }

    /**
     * constructs a set of splits from a tree
     *
     * @param allTaxa
     * @param tree
     */
    public SplitSystem(Taxa allTaxa, PhyloTree tree) {
        this();
        splitsFromTreeRec(tree.getRoot(), tree, allTaxa, allTaxa.getBits(), new NodeArray(tree), this);
    }

    /**
     * get size
     *
     * @return number of splits
     */
    public int size() {
        return nsplits;
    }

    /**
     * add a split
     *
     * @param split
     * @return index
     */
    public int addSplit(Split split) {
        nsplits++;
        index2split.put(nsplits, split);
        split2index.put(split, nsplits);

        return nsplits;
    }


    /**
     * gets a split
     *
     * @param index
     * @return split with given index
     */
    public Split getSplit(int index) {
        return (Split) index2split.get(index);
    }

    /**
     * gets the index of the split, if present, otherwise -1
     *
     * @param split
     * @return index or -1
     */
    public int indexOf(Split split) {
        Integer index = (Integer) split2index.get(split);
        return Objects.requireNonNullElse(index, -1);
    }

    /**
     * determines whether given split is already contained in the system
     *
     * @param split
     * @return true, if contained
     */
    private boolean contains(Split split) {
        return split2index.containsKey(split);
    }

    /**
     * gets a string representation
     *
     * @return string
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Splits (").append(nsplits).append("):\n");
        for (Iterator it = iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();
            buf.append(split).append("\n");
        }
        return buf.toString();
    }

    /**
     * erase
     */
    public void clear() {
        nsplits = 0;
        index2split.clear();
        split2index.clear();
    }

    /**
     * gets an getLetterCodeIterator over all splits
     *
     * @return getLetterCodeIterator
     */
    private Iterator iterator() {
        return split2index.keySet().iterator();
    }


    /**
     * given a phylogenetic tree and a set of taxa, returns all splits found in the tree.
     * Assumes the last taxon is an outgroup
     *
     * @param allTaxa
     * @param activeTaxa
     * @param tree
     * @return splits
     */
    public static SplitSystem getSplitsFromTree(Taxa allTaxa, BitSet activeTaxa, PhyloTree tree) {
        SplitSystem splits = new SplitSystem();

        splitsFromTreeRec(tree.getRoot(), tree, allTaxa, activeTaxa, new NodeArray(tree), splits);
        return splits;
    }

    /**
     * recursively extract splits from tree. Also works for cluster networks.
     *
     * @param v
     * @param tree
     * @param allTaxa
     * @param reticulateNode2Taxa
     * @param splits
     * @return taxa
     */
    private static BitSet splitsFromTreeRec(Node v, PhyloTree tree,
                                            Taxa allTaxa, BitSet activeTaxa, NodeArray reticulateNode2Taxa, SplitSystem splits) {
        BitSet e_taxa = new BitSet();

        int taxon = allTaxa.indexOf(tree.getLabel(v));
        if (taxon > -1)
            e_taxa.set(taxon);

        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            Node w = f.getTarget();
            BitSet f_taxa;
            if (!tree.isSpecial(f) || reticulateNode2Taxa.get(w) == null)
                f_taxa = splitsFromTreeRec(w, tree, allTaxa, activeTaxa, reticulateNode2Taxa, splits);
            else
                f_taxa = (BitSet) reticulateNode2Taxa.get(w);

            if (!tree.isSpecial(f)) {
                BitSet complement = (BitSet) activeTaxa.clone();
                complement.andNot(f_taxa);
                Split split = new Split(f_taxa, complement, tree.getWeight(f));
                if (!splits.contains(split))
                    splits.addSplit(split);
                else if (v == tree.getRoot() && v.getOutDegree() == 2) // is root split
                {
                    Split prevSplit = splits.get(split);
                    prevSplit.setWeight(prevSplit.getWeight() + split.getWeight());
                }
            } else
                reticulateNode2Taxa.put(w, f_taxa);

            e_taxa.or(f_taxa);
        }
        return e_taxa;
    }


    /**
     * inserts a split into a tree
     *
     * @param v
     * @param node2taxa
     * @param split
     * @param tree
     */
    private void processSplit(Node v, NodeArray node2taxa, int outGroupTaxonId, Split split, PhyloTree tree) {
        BitSet partB = split.getPartNotContainingTaxon(outGroupTaxonId);
        double weight = split.getWeight();

        boolean done = false;

        while (!done) {
            List edgesToPush = new LinkedList();
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                Node w = f.getTarget();
                BitSet nodeSet = (BitSet) node2taxa.get(w);
                if (nodeSet.intersects(partB))
                    edgesToPush.add(f);
            }
            if (edgesToPush.size() == 1) // need to move down tree
            {
                Edge f = (Edge) edgesToPush.get(0);
                v = f.getTarget();
            } else if (edgesToPush.size() > 1) { // more than one subtree contains taxa from the set, time to split
                Node u = tree.newNode();
                node2taxa.put(u, partB);
                Edge h = tree.newEdge(v, u);
                tree.setWeight(h, weight);

                for (Object anEdgesToPush1 : edgesToPush) {
                    Edge f = (Edge) anEdgesToPush1;
                    Node w = f.getTarget();
                    Edge g = tree.newEdge(u, w);
                    tree.setWeight(g, tree.getWeight(f));
                }
                for (Object anEdgesToPush : edgesToPush) {
                    Edge f = (Edge) anEdgesToPush;
                    tree.deleteEdge(f);
                }
                done = true;
            } else {
                throw new RuntimeException("0 taxa in splitsToTreeRec");
            }
        }
    }

    /**
     * returns a trivial split separating index from all other taxa, if it exists
     *
     * @param taxonIndex
     * @return trivial split, if it exists
     */
    private Split getTrivial(int taxonIndex) {
        for (Iterator it = iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();

            if (split.getA().cardinality() == 1 && split.getA().get(taxonIndex)
                    || split.getB().cardinality() == 1 && split.getB().get(taxonIndex))
                return split;
        }
        return null;
    }

    /**
     * add all given splits that are not already present (as splits, ignoring weights etc)
     *
     * @param splits
     * @return number actually added
     */
    public int addAll(SplitSystem splits) {
        int count = 0;
        for (Iterator it = splits.iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();
            if (!split2index.containsKey(split)) {
                addSplit(split);
                count++;
            }
        }
        return count;
    }

    /**
     * get all splits in a new list
     *
     * @return list of splits
     */
    public List asList() {
        List result = new LinkedList();
        for (Iterator it = iterator(); it.hasNext(); ) {
            result.add(it.next());
        }
        return result;
    }

    /**
     * returns the splits as an array
     *
     * @return array of splits
     */
    public Split[] asArray() {
        Split[] result = new Split[size()];
        int count = 0;
        for (Iterator it = iterator(); it.hasNext(); ) {
            result[count++] = (Split) it.next();
        }
        return result;
    }

    /**
     * if the given split A|B is contained in this split system, return it.
     * This is useful because A|B might have a weight etc in the split system
     *
     * @param split
     * @return split
     */
    private Split get(Split split) {
        Integer index = (Integer) split2index.get(split);
        if (index != null)
            return getSplit(index);
        else
            return null;

    }

    /**
     * determines whether these splits contains a pair with all four intersections
     *
     * @return
     */
    public boolean containsPairWithAllFourIntersections() {
        for (int s = 1; s <= size(); s++) {
            Split S = getSplit(s);
            for (int t = s + 1; t <= size(); t++) {
                Split T = getSplit(t);
                if (S.getA().intersects(T.getA()) && S.getA().intersects(T.getB())
                        && S.getB().intersects(T.getA()) && S.getB().intersects(T.getB()))
                    return true;
            }
        }
        return false;
    }

    /**
     * returns true, if all splits contain all taxa
     *
     * @param taxa
     * @return true, if full splits
     */
    public boolean isFullSplitSystem(Taxa taxa) {
        BitSet bits = taxa.getBits();

        for (Iterator it = iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();
            if (!split.getTaxa().equals(bits))
                return false;
        }
        return true;
    }

    /**
     * gets the splits as binary sequences in fastA format
     *
     * @param taxa
     * @return splits in fastA format
     */
    public String toStringAsBinarySequences(Taxa taxa) {
        StringBuilder buf = new StringBuilder();
        for (Iterator it = taxa.iterator(); it.hasNext(); ) {
            String name = (String) it.next();
            int t = taxa.indexOf(name);
            buf.append("> ").append(name).append("\n");

            for (int s = 1; s <= size(); s++) {
                Split split = getSplit(s);
                if (split.getA().get(t))
                    buf.append("1");
                else
                    buf.append("0");
            }
            buf.append("\n");
        }
        return buf.toString();
    }

    /**
     * returns the heaviest trivial split or null
     *
     * @return heaviest trivial split
     */
    public Split getHeaviestTrivialSplit() {
        Split result = null;
        for (int s = 1; s <= size(); s++) {
            Split split = getSplit(s);
            if (split.getSplitSize() == 1) {
                if (result == null)
                    result = split;
                else if (split.getWeight() > result.getWeight())
                    result = split;
            }
        }
        return result;
    }

    /**
     * delete all taxa listed
     *
     * @param labels
     * @param taxa   taxa are removed from here
     * @return split set with taxa delated
     */
    public SplitSystem deleteTaxa(List labels, Taxa taxa) {
        for (Object label1 : labels) {
            String label = (String) label1;
            taxa.remove(label);
        }
        SplitSystem result = new SplitSystem();

        for (Iterator it = iterator(); it.hasNext(); ) {
            Split split = (Split) it.next();
            Split induced = split.getInduced(taxa.getBits());
            if (result.contains(induced)) {
                Split other = result.get(induced);
                other.setWeight(other.getWeight() + induced.getWeight());
            } else if (induced.getSplitSize() > 0) // make sure that is a proper split
                result.addSplit(induced);
        }
        return result;
    }
}
