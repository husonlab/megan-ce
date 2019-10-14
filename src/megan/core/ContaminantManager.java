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

package megan.core;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.util.Basic;
import jloda.util.FileLineIterator;
import megan.data.IReadBlock;
import megan.viewer.TaxonomyData;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
 * represents taxa known to be contaminants of a sample
 * Daniel Huson, 11.2017
 */
public class ContaminantManager {
    private final Set<Integer> contaminants = new HashSet<>();
    private final Set<Integer> contaminantsAndDescendants = new HashSet<>();

    /**
     * read the internal nodes definition. Does not prepare for use
     *
     * @param file
     * @throws IOException
     */
    public void read(String file) throws IOException {
        contaminants.clear();
        contaminantsAndDescendants.clear();

        try (FileLineIterator it = new FileLineIterator(file)) {
            while (it.hasNext()) {
                final String aLine = it.next();
                final int taxonId;
                if (Basic.isInteger(aLine))
                    taxonId = Basic.parseInt(aLine);
                else
                    taxonId = TaxonomyData.getName2IdMap().get(aLine);
                if (taxonId > 0)
                    contaminants.add(taxonId);
            }
        }
        if (contaminants.size() > 0)
            setAllDescendentsRec(TaxonomyData.getTree().getRoot(), contaminants.contains(TaxonomyData.getTree().getRoot().getInfo()), contaminants, contaminantsAndDescendants);
    }

    /**
     * recursively set the all nodes set
     *
     * @param v
     * @param mustAddToAll
     * @param internalNodes
     * @param allNodes
     */
    private void setAllDescendentsRec(Node v, boolean mustAddToAll, Set<Integer> internalNodes, Set<Integer> allNodes) {
        if (!mustAddToAll && internalNodes.contains(v.getInfo()))
            mustAddToAll = true;

        if (mustAddToAll)
            allNodes.add((Integer) v.getInfo());

        for (Edge e : v.outEdges()) {
            setAllDescendentsRec(e.getTarget(), mustAddToAll, internalNodes, allNodes);
        }
    }

    public int inputSize() {
        return contaminants.size();
    }


    public int size() {
        return contaminantsAndDescendants.size();
    }

    /**
     * parse a string of ids and prepare for use
     *
     * @param taxonIdString
     */
    public void parseTaxonIdsString(String taxonIdString) {
        contaminants.clear();
        contaminantsAndDescendants.clear();
        for (String word : Basic.splitOnWhiteSpace(taxonIdString)) {
            if (Basic.isInteger(word)) {
                final int taxonId = Basic.parseInt(word);
                if (taxonId > 0)
                    contaminants.add(taxonId);
            }
        }
        if (contaminants.size() > 0)
            setAllDescendentsRec(TaxonomyData.getTree().getRoot(), contaminants.contains(TaxonomyData.getTree().getRoot().getInfo()), contaminants, contaminantsAndDescendants);
    }

    /**
     * determines whether a short read is a contaminant based on whether it has a good alignment against a contaminant
     *
     * @param readBlock
     * @param activeMatches
     * @return true if contaminant
     */
    public boolean isContaminantShortRead(IReadBlock readBlock, BitSet activeMatches) {
        for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
            if (contaminantsAndDescendants.contains(readBlock.getMatchBlock(i).getTaxonId()))
                return true;
        }
        return false;
    }

    /**
     * determines whether a long read is a contaminant based on its assigned taxon
     *
     * @param assignedTaxon
     * @return
     */
    public boolean isContaminantLongRead(int assignedTaxon) {
        return contaminantsAndDescendants.contains(assignedTaxon);
    }

    /**
     * gets all contaminants as string of taxon ids
     *
     * @return taxon ids
     */
    public String getTaxonIdsString() {
        return Basic.toString(contaminants, " ");
    }

    /**
     * get iterable over all contaminants
     *
     * @return iterable
     */
    public Iterable<Integer> getContaminants() {
        return () -> contaminants.iterator();
    }
}
