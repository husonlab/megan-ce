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
package megan.algorithms;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.classification.IdMapper;
import megan.viewer.TaxonomyData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * apply the minsupport algorithm
 * Daniel Huson, 7.2014
 */
public class MinSupportAlgorithm {
    private final Map<Integer, Integer> taxId2count;
    private final int minSupport;
    private final ProgressListener progressListener;
    private final PhyloTree tree;

    /**
     * applies the min-support algorithm to the given taxonomic analysis
     *
     * @param tax2count
     * @param minSupport
     * @param progressListener
     */
    public static void apply(Map<Integer, Integer> tax2count, int minSupport, final ProgressListener progressListener) {
        MinSupportAlgorithm algorithm = new MinSupportAlgorithm(tax2count, minSupport, progressListener);
        try {
            Map<Integer, Integer> lowSupportTaxa2HighSupportTaxa = algorithm.apply();
            for (Integer lowTaxon : lowSupportTaxa2HighSupportTaxa.keySet()) {
                Integer highTaxon = lowSupportTaxa2HighSupportTaxa.get(lowTaxon);
                Integer count = tax2count.get(highTaxon);
                if (count == null)
                    tax2count.put(highTaxon, tax2count.get(lowTaxon));
                else
                    tax2count.put(highTaxon, count + tax2count.get(lowTaxon));
            }
            tax2count.keySet().removeAll(lowSupportTaxa2HighSupportTaxa.keySet());
        } catch (CanceledException e) {
            Basic.caught(e);
        }
    }

    /**
     * constructor
     *
     * @param taxId2count
     * @param minSupport
     * @param progressListener
     */
    public MinSupportAlgorithm(Map<Integer, Integer> taxId2count, int minSupport, final ProgressListener progressListener) {
        this.taxId2count = taxId2count;
        this.minSupport = minSupport;
        this.progressListener = progressListener;
        tree = TaxonomyData.getTree();
    }

    /**
     * applies the min support filter to taxon classification
     *
     * @return mapping of old taxon ids to new taxon ids
     */
    private Map<Integer, Integer> apply() throws CanceledException {
        Map<Integer, Integer> orphan2AncestorMapping = new HashMap<>();
        progressListener.setMaximum(tree.getNumberOfNodes());
        progressListener.setProgress(0);

        Set<Integer> orphans = new HashSet<>();
        computeOrphan2AncestorMappingRec(tree.getRoot(), orphan2AncestorMapping, orphans);
        // Any orphans that popped out of the top of the taxonomy are mapped to unassigned
        for (Integer id : orphans) {
            orphan2AncestorMapping.put(id, IdMapper.UNASSIGNED_ID);
        }
        orphans.clear();

        return orphan2AncestorMapping;
    }

    /**
     * recursively move all reads that land on taxa with too little support to higher level nodes
     *
     * @param v
     * @param orphan2AncestorMapping
     * @param orphans                nodes that have too few reads
     * @return reads on or below this node
     */
    private int computeOrphan2AncestorMappingRec(Node v, Map<Integer, Integer> orphan2AncestorMapping, Set<Integer> orphans) throws CanceledException {
        progressListener.incrementProgress();
        int taxId = (Integer) v.getInfo();

        if (taxId < 0)
            return 0; // ignore nohits and unassigned

        int below = 0;
        Set<Integer> orphansBelow = new HashSet<>();

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            below += computeOrphan2AncestorMappingRec(w, orphan2AncestorMapping, orphansBelow);
        }

        Integer count = taxId2count.get(taxId);
        if (count == null)
            count = 0;

        if (below + count >= minSupport)  // this is a strong node, map all orphans to here
        {
            for (Integer id : orphansBelow) {
                orphan2AncestorMapping.put(id, taxId);
            }
        } else // this node is not strong enough, pass all orphans up
        {
            if (count > 0) // this node has reads assigned to it, pass it up as an orpha
            {
                orphansBelow.add(taxId);
            }
            orphans.addAll(orphansBelow);
        }
        return below + count;
    }
}
