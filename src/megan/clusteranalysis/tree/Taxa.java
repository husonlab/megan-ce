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
package megan.clusteranalysis.tree;

import java.util.*;

/**
 * maintains the taxa associated with a tree or network
 * Daniel Huson, 6.2007
 */
public class Taxa {
    private final Map<String, Integer> name2index;
    private final Map<Integer, String> index2name;
    private final BitSet bits;
    private int ntax;

    /**
     * constructor
     */
    public Taxa() {
        name2index = new HashMap<>();
        index2name = new HashMap<>();
        bits = new BitSet();
        ntax = 0;
    }

    /**
     * get the t-th taxon (numbered 1-size)
     *
     * @param t
     * @return name of t-th taxon
     */
    public String getLabel(int t) {
        return index2name.get(t);

    }

    /**
     * index of the named taxon, or -1
     *
     * @param name
     * @return index or -1
     */
    public int indexOf(String name) {
        Integer index = name2index.get(name);
        return Objects.requireNonNullElse(index, -1);

    }

    /**
     * add the named taxon
     *
     * @param name
     * @return the index of the taxon
     */
    public int add(String name) {
        if (!name2index.containsKey(name)) {
            ntax++;
            bits.set(ntax);
            Integer index = ntax;
            index2name.put(index, name);
            name2index.put(name, index);
            return ntax;
        } else
            return name2index.get(name);
    }

    /**
     * does this taxa object contain the named taxon?
     *
     * @param name
     * @return true, if contained
     */
    public boolean contains(String name) {
        return indexOf(name) != -1;
    }

    /**
     * get the number of taxa
     *
     * @return number of taxa
     */
    public int size() {
        return bits.cardinality();
    }

    /**
     * gets the maximal defined taxon id
     *
     * @return max id
     */
    public int maxId() {
        int t = -1;
        while (true) {
            int s = bits.nextSetBit(t + 1);
            if (s == -1)
                return t;
            else
                t = s;
        }
    }

    /**
     * erase all taxa
     */
    public void clear() {
        ntax = 0;
        bits.clear();
        index2name.clear();
        name2index.clear();
    }

    /**
     * gets the complement to bit set A
     *
     * @param A
     * @return complement
     */
    public BitSet getComplement(BitSet A) {
        BitSet result = new BitSet();

        for (int t = 1; t <= ntax; t++) {
            if (!A.get(t))
                result.set(t);
        }
        return result;

    }

    /**
     * add all taxa.
     *
     * @param taxa
     * @return set of indices
     */
    public void addAll(Taxa taxa) {
        for (Iterator it = taxa.iterator(); it.hasNext(); ) {
            String name = (String) it.next();
            add(name);
        }
    }

    /**
     * gets string representation
     *
     * @return string
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Taxa (").append(size()).append("):\n");
        for (Iterator it = iterator(); it.hasNext(); ) {
            String name = (String) it.next();
            buf.append(name).append("\n");
        }
        return buf.toString();
    }

    /**
     * gets an getLetterCodeIterator over all taxon names
     *
     * @return getLetterCodeIterator
     */
    public Iterator iterator() {
        return name2index.keySet().iterator();
    }

    /**
     * gets the bits of this set
     *
     * @return bits
     */
    public BitSet getBits() {
        return bits;
    }

    /**
     * remove this taxon
     *
     * @param name
     */
    public void remove(String name) {
        Integer tt = name2index.get(name);
        if (tt != null) {
            name2index.keySet().remove(name);
            index2name.remove(tt);
            ntax--;
            bits.set(tt, false);
        }
    }
}
