/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.util.Collection;
import java.util.Map;

/**
 * Computes addresses used in LCA algorithm
 * Daniel Huson, 4.2015
 */
public class LCAAddressing {
    /**
     * compute node addresses used to compute LCA
     *
     * @param tree
     * @param id2address
     * @param address2id
     */
    public static void computeAddresses(PhyloTree tree, Map<Integer, String> id2address, Map<String, Integer> address2id) {
        Node root = tree.getRoot();
        if (root != null)
            buildId2AddressRec(root, "", id2address, address2id);
    }

    /**
     * computes the id to address mapping
     *
     * @param v
     * @param path
     */
    private static void buildId2AddressRec(Node v, String path, Map<Integer, String> id2address, Map<String, Integer> address2id) {
        int id = (Integer) v.getInfo();
        id2address.put(id, path);
        address2id.put(path, id);
        if (v.getOutDegree() < Character.MAX_VALUE) {
            char count = 1;
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                buildId2AddressRec(f.getOpposite(v), path + count, id2address, address2id);
                count++;
            }
        } else { // use two characters if outdegree is too big
            char count1 = 1;
            char count2 = 1;
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                if (count1 == Character.MAX_VALUE) {
                    count2++;
                    count1 = 1;
                }
                buildId2AddressRec(f.getOpposite(v), (path + count1) + count2, id2address, address2id);
                count1++;
            }
        }
    }

    /**
     * given a set of addresses, returns the common prefix.
     *
     * @param addresses
     * @return prefix
     */
    public static String getCommonPrefix(final Collection<String> addresses) {
        if (addresses.size() == 0)
            return "";
        final String firstAddress = addresses.iterator().next();

        for (int pos = 0; pos < firstAddress.length(); pos++) {
            final char charAtPos = firstAddress.charAt(pos);
            boolean anotherAddressAlive = false;
            boolean first = true;
            for (String address : addresses) {
                if (first) {
                    first = false; // don't compare the first address with itself...
                } else {
                    if (pos < address.length()) {
                        if (address.charAt(pos) != charAtPos)
                            return firstAddress.substring(0, pos);
                        else
                            anotherAddressAlive = true;
                    }
                }
            }
            if (!anotherAddressAlive)
                break; // no need to increment pos any further
        }
        return firstAddress;
    }

    /**
     * given an array of addresses, returns the common prefix. Array may contain null entries
     *
     * @param addresses
     * @return prefix
     */
    public static String getCommonPrefix(final String[] addresses, final int numberOfAddresses) {
        if (numberOfAddresses == 0)
            return "";

        final String first = addresses[0];

        for (int pos = 0; pos < first.length(); pos++) {
            final char charAtPos = first.charAt(pos);
            boolean anotherAddressAlive = false;
            for (int i = 1; i < numberOfAddresses; i++) { // start at 1, because don't compare first address with itself
                final String address = addresses[i];
                if (pos < address.length()) {
                    if (address.charAt(pos) != charAtPos)
                        return first.substring(0, pos);
                    else
                        anotherAddressAlive = true;
                }
            }
            if (!anotherAddressAlive)
                break; // no need to increment pos any further
        }
        return first;
    }
}
