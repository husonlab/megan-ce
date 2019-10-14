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

import jloda.graph.Node;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.classification.Classification;
import megan.classification.IdMapper;
import megan.core.Document;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.viewer.ClassificationViewer;
import megan.viewer.TaxonomyData;

import java.io.IOException;
import java.util.*;

/**
 * Compute a taxonomic profile by naive match-based analysis
 * Daniel Huson, 2015
 */
class NaiveMatchBasedProfile {
    /**
     * compute a taxonomic profile at a given taxonomic rank using naive projection
     *
     * @param viewer
     * @param level
     * @return mapping of each taxon to a count
     * todo: needs fixing
     */
    public static Map<Integer, float[]> compute(final ClassificationViewer viewer, final int level, final float minPercent) throws IOException, CanceledException {

        final Map<Integer, Float> rawProfile = new HashMap<>();

        // process all reads:
        final Document doc = viewer.getDocument();
        final BitSet activeMatchesForTaxa = new BitSet();
        final BitSet activeTaxa = new BitSet();

        int totalAssigned = 0;
        try (IReadBlockIterator it = doc.getConnector().getAllReadsIterator(0, 10, true, true)) {

            final ProgressListener progressListener = doc.getProgressListener();
            progressListener.setTasks("Computing profile", "Processing all reads and matches");
            progressListener.setMaximum(it.getMaximumProgress());
            progressListener.setProgress(0);

            while (it.hasNext()) {
                final IReadBlock readBlock = it.next();

                if (readBlock.getComplexity() < doc.getMinComplexity()) {
                    Float rawValue = rawProfile.get(IdMapper.LOW_COMPLEXITY_ID);
                    rawProfile.put(IdMapper.LOW_COMPLEXITY_ID, rawValue == null ? 1f : rawValue + 1f);
                } else if (readBlock.getNumberOfMatches() == 0) {
                    Float rawValue = rawProfile.get(IdMapper.NOHITS_ID);
                    rawProfile.put(IdMapper.NOHITS_ID, rawValue == null ? 1f : rawValue + 1f);
                } else {
                    ActiveMatches.compute(doc.getMinScore(), doc.getTopPercent(), doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, Classification.Taxonomy, activeMatchesForTaxa);

                    activeTaxa.clear();

                    for (int i = activeMatchesForTaxa.nextSetBit(0); i != -1; i = activeMatchesForTaxa.nextSetBit(i + 1)) {
                        Integer taxonId = readBlock.getMatchBlock(i).getTaxonId();
                        taxonId = getAncestorAtRank(level, taxonId);
                        if (taxonId > 0) {
                            activeTaxa.set(taxonId);
                        }
                    }

                    if (activeTaxa.cardinality() == 0) { // none active
                        Float rawValue = rawProfile.get(IdMapper.UNASSIGNED_ID);
                        rawProfile.put(IdMapper.UNASSIGNED_ID, rawValue == null ? 1f : rawValue + 1f);
                    } else { // have some active matches:
                        for (int taxonId = activeTaxa.nextSetBit(0); taxonId != -1; taxonId = activeTaxa.nextSetBit(taxonId + 1)) {
                            rawProfile.merge(taxonId, 1f / activeTaxa.cardinality(), Float::sum);
                        }
                        totalAssigned++;
                    }
                }
                progressListener.setProgress(it.getProgress());
            }
        }

        int minSupport = (int) (totalAssigned / 100.0 * minPercent);

        int totalReads = 0;
        final Map<Integer, float[]> profile = new HashMap<>();
        for (Integer id : rawProfile.keySet()) {
            Float rawValue = rawProfile.get(id);
            if (rawValue != null) {
                if (rawValue >= minSupport) {
                    int count = Math.round(rawValue);
                    profile.put(id, new float[]{count});
                    totalReads += count;
                }
            }
        }

        if (totalReads < doc.getNumberOfReads()) {
            float missing = doc.getNumberOfReads() - totalReads;
            Float rawValue = rawProfile.get(IdMapper.UNASSIGNED_ID);
            rawProfile.put(IdMapper.UNASSIGNED_ID, rawValue == null ? missing : rawValue + missing);
        }

        float[] total = new float[1];
        SortedMap<String, float[]> name2counts = new TreeMap<>();
        for (int id : profile.keySet()) {
            String name = TaxonomyData.getName2IdMap().get(id);
            name2counts.put(name, profile.get(id));
        }
        for (String name : name2counts.keySet()) {
            final float[] counts = name2counts.get(name);
            System.err.println(name + "\t" + Basic.toString(counts, 0, counts.length, ", ", true));
            for (int i = 0; i < 1; i++)
                total[i] += counts[i];
        }
        System.err.println("Total assigned: " + Basic.toString(total, ", "));

        return profile;
    }

    /**
     * gets the ancestor taxon id at given rank or 0
     *
     * @param targetLevel
     * @param taxonId
     * @return ancestor or 0
     */
    private static Integer getAncestorAtRank(int targetLevel, Integer taxonId) {
        if (taxonId == 0)
            return 0;

        Node v = TaxonomyData.getTree().getANode(taxonId);
        while (v != null) {
            int vLevel = TaxonomyData.getTaxonomicRank(taxonId);
            if (vLevel == targetLevel)
                return taxonId;
            if (v.getInDegree() > 0) {
                v = v.getFirstInEdge().getSource();
                taxonId = (Integer) v.getInfo();
            } else
                break;
        }
        return 0;
    }
}
