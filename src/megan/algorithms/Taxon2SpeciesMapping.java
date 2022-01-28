/*
 * Taxon2SpeciesMapping.java Copyright (C) 2022 Daniel H. Huson
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
import jloda.util.CanceledException;
import jloda.util.progress.ProgressListener;
import jloda.util.progress.ProgressPercentage;
import megan.classification.ClassificationManager;
import megan.classification.data.ClassificationFullTree;
import megan.classification.data.IntIntMap;
import megan.classification.data.Name2IdMap;
import megan.viewer.TaxonomicLevels;

/**
 * computes and maintains a taxon to species mapping
 * Daniel Huson, May 2017
 */
public class Taxon2SpeciesMapping {

    private final IntIntMap taxId2SpeciesId;

    public Taxon2SpeciesMapping(final String cName, final ProgressListener progress) throws CanceledException {
        ClassificationFullTree fullTree = ClassificationManager.get(cName, true).getFullTree();
        final Name2IdMap name2IdMap = ClassificationManager.get(cName, true).getName2IdMap();
        taxId2SpeciesId = new IntIntMap(fullTree.getNumberOfNodes(), 0.999f);

        progress.setSubtask("Computing taxon-to-species map for '" + cName + "'");
        progress.setMaximum(fullTree.getNumberOfNodes());
        progress.setProgress(0);
        computeTax2SpeciesMapRec(fullTree.getRoot(), 0, taxId2SpeciesId, name2IdMap, progress);
        if (progress instanceof ProgressPercentage)
            progress.reportTaskCompleted();
    }

    /**
     * recursively compute the taxon-id to species-id map
     *
     * @param v
     * @param taxId2SpeciesId
     * @return taxa below species
     */
    private void computeTax2SpeciesMapRec(final Node v, int speciesId, final IntIntMap taxId2SpeciesId, Name2IdMap name2IdMap, final ProgressListener progress) throws CanceledException {
        final int taxId = (Integer) v.getInfo();

        if (speciesId == 0) {
            // todo: prepare for taxonomies that use other ids
            if (name2IdMap.getRank(taxId) == TaxonomicLevels.getSpeciesId()) {
                speciesId = taxId;
                taxId2SpeciesId.put(taxId, speciesId);
            }
        } else
            taxId2SpeciesId.put(taxId, speciesId);

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e))
            computeTax2SpeciesMapRec(e.getTarget(), speciesId, taxId2SpeciesId, name2IdMap, progress);
        progress.incrementProgress();
    }

    /**
     * gets the species, if defined, or 0
     *
     * @param taxonId
     * @return species id or 0
     */
    public int getSpecies(int taxonId) {
        return taxId2SpeciesId.get(taxonId);
    }

    /**
     * gets the species id, if defined, or returns taxonId
     *
     * @param taxonId
     * @return species id or taxonId
     */
    public int getSpeciesOrReturnTaxonId(int taxonId) {
        int id = taxId2SpeciesId.get(taxonId);
        return id > 0 ? id : taxonId;
    }
}
