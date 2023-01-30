/*
 * LCAAddressing.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
	 */
    public static void computeAddresses(PhyloTree tree, Map<Integer, String> id2address, Map<String, Integer> address2id) {
        var root = tree.getRoot();
        if (root != null)
            buildId2AddressRec(root, "", id2address, address2id);
    }

    /**
     * computes the id to address mapping
     *
	 */
    private static void buildId2AddressRec(Node v, String path, Map<Integer, String> id2address, Map<String, Integer> address2id) {
        var id = (Integer) v.getInfo();
        id2address.put(id, path);
        address2id.put(path, id);
        if (v.getOutDegree() < Character.MAX_VALUE) {
            char count = 1;
            for (var f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
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
     * @param ignoreAncestors ignore ancestors, i.e. ignore prefixes of longer addresses
     * @return prefix
     */
    public static String getCommonPrefix(final Collection<String> addresses, boolean ignoreAncestors) {
        if (addresses.size() == 0)
            return "";
        else if (addresses.size() == 1)
            return addresses.iterator().next();


        String reference = null;
        for (String other : addresses) {
            if (other != null && other.length() > 0) {
                if (reference == null) {
                    reference = other;
                } else {
                    // if most specific requested, use longest sequence as reference, else use shortest
                    if (ignoreAncestors && other.length() > reference.length() || !ignoreAncestors && other.length() < reference.length()) {
                        reference = other;
                    }
                }
            }
        }
        if (reference == null)
            return "";

        for (int pos = 0; pos < reference.length(); pos++) {
            final char charAtPos = reference.charAt(pos);
            for (String other : addresses) {
                if (other != null && pos < other.length() && other.charAt(pos) != charAtPos)
                    return reference.substring(0, pos);
            }
        }
        return reference;
    }

    /**
     * given an array of addresses, returns the common prefix
     *
     * @param ignorePrefixes ignore prefixes of longer addresses
     * @return prefix
     */
    public static String getCommonPrefix(final String[] addresses, final int numberOfAddresses, boolean ignorePrefixes) {
        if (numberOfAddresses == 0)
            return "";
        else if (numberOfAddresses == 1)
            return addresses[0];

        String reference = null;
        for (int i = 0; i < numberOfAddresses; i++) {
            final String other = addresses[i];
            if (other != null && other.length() > 0) {
                if (reference == null)
                    reference = other;
                // if ignore prefixes, use longest sequence as reference, else use shortest
                if (ignorePrefixes && other.length() > reference.length() || !ignorePrefixes && other.length() < reference.length()) {
                    reference = other;
                }
            }
        }
        if (reference == null)
            return "";

        for (int pos = 0; pos < reference.length(); pos++) {
            final char charAtPos = reference.charAt(pos);
            for (int i = 0; i < numberOfAddresses; i++) {
                final String other = addresses[i];
                if (other != null && pos < other.length() && other.charAt(pos) != charAtPos)
                    return reference.substring(0, pos);
            }
        }
        return reference;
    }
}
