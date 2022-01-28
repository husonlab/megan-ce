/*
 * MinSupportFilter.java Copyright (C) 2022 Daniel H. Huson
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
import jloda.util.CanceledException;
import jloda.util.progress.ProgressListener;
import jloda.util.progress.ProgressPercentage;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * apply the min-support filter and also moves assigns to disabled nodes up the tree
 * Daniel Huson, 4.2010, 3.2016
 */
public class MinSupportFilter {
    private final Map<Integer, Float> id2weight;
    private final int minSupport;
    private final ProgressListener progress;
    private final PhyloTree tree;
    private final IdMapper idMapper;

    /**
     * constructor
     *
     * @param id2weight
     * @param minSupport
     * @param progress
     */
    public MinSupportFilter(String cName, Map<Integer, Float> id2weight, int minSupport, final ProgressListener progress) {
        this.id2weight = id2weight;
        this.minSupport = minSupport;
        this.progress = progress;
        tree = ClassificationManager.get(cName, true).getFullTree();
        this.idMapper = ClassificationManager.get(cName, false).getIdMapper();
    }

    /**
     * applies the min support filter to taxon classification
     *
     * @return mapping of old taxon ids to new taxon ids
     */
    public Map<Integer, Integer> apply() throws CanceledException {
        final Map<Integer, Integer> orphan2AncestorMapping = new HashMap<>();
        if (progress != null) {
            progress.setMaximum(tree.getNumberOfNodes());
            progress.setProgress(0);
        }

        final Set<Integer> orphans = new HashSet<>();
        if (tree.getRoot() != null)
            computeOrphan2AncestorMappingRec(tree.getRoot(), orphan2AncestorMapping, orphans);
        // Any orphans that popped out of the top of the taxonomy are mapped to unassigned
        for (Integer id : orphans) {
            orphan2AncestorMapping.put(id, IdMapper.UNASSIGNED_ID);
        }
        orphans.clear();

        if (progress instanceof ProgressPercentage)
            progress.reportTaskCompleted();

        return orphan2AncestorMapping;
    }

    /**
     * recursively move all reads that land on taxa with too little support or on a disabled taxon to higher level nodes
     *
     * @param v
     * @param orphan2AncestorMapping
     * @param orphans                nodes that have too few reads
     * @return reads on or below this node
     */
    private float computeOrphan2AncestorMappingRec(Node v, Map<Integer, Integer> orphan2AncestorMapping, Set<Integer> orphans) throws CanceledException {
        if (progress != null)
            progress.incrementProgress();
        int taxId = (Integer) v.getInfo();

        if (taxId < 0)
            return 0; // ignore nohits and unassigned

        float below = 0;
        Set<Integer> orphansBelow = new HashSet<>();

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            below += computeOrphan2AncestorMappingRec(w, orphan2AncestorMapping, orphansBelow);
        }

        Float weight = id2weight.get(taxId);
        if (weight == null)
            weight = 0f;

        if (below + weight >= minSupport && !idMapper.isDisabled(taxId))  // this is a strong node, map all orphans to here
        {
            for (Integer id : orphansBelow) {
                orphan2AncestorMapping.put(id, taxId);
            }
        } else // this node is not strong enough, pass all orphans up
        {
            if (weight > 0) // this node has reads assigned to it, pass it up as an orpha
            {
                orphansBelow.add(taxId);
            }
            orphans.addAll(orphansBelow);
        }
        return below + weight;
    }
}
