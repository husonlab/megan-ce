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
package megan.commands.algorithms;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.ClassificationType;
import megan.core.DataTable;
import megan.core.Document;
import megan.viewer.TaxonomyData;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * computes the core or rare biome for a set of samples
 * Reads summarized by a node are used to decide whether to keep node, reads assigned to node are used as counts
 * Daniel Huson, 2.2013
 */
public class ComputeCoreBiome {
    /**
     * computes core biome for a given threshold
     *
     * @param srcDoc
     * @param asUpperBound                   if true, keep rare taxa in which the sample threshold is an upper bound
     * @param samplesThreshold               number of samples that must contain a taxon so that it appears in the output, or max, if asUpperBound is true
     * @param tarClassification2class2counts
     * @param progress
     * @return sampleSize
     */
    public static int apply(Document srcDoc, Collection<String> samplesToUse, boolean asUpperBound, int samplesThreshold,
                            float taxonDetectionThresholdPercent, Map<String, Map<Integer, float[]>> tarClassification2class2counts, ProgressListener progress) throws CanceledException {
        final BitSet sampleIds = srcDoc.getDataTable().getSampleIds(samplesToUse);
        int size = 0;

        if (sampleIds.cardinality() > 0) {
            DataTable dataTable = srcDoc.getDataTable();
            for (String classificationName : dataTable.getClassification2Class2Counts().keySet()) {
                final Map<Integer, float[]> srcClass2counts = srcDoc.getDataTable().getClass2Counts(classificationName);
                final Node root;

                if (classificationName.equals(Classification.Taxonomy))
                    root = TaxonomyData.getTree().getRoot();
                else {
                    root = ClassificationManager.get(classificationName, true).getFullTree().getRoot();
                }
                final Map<Integer, float[]> tarClass2counts = new HashMap<>();
                tarClassification2class2counts.put(classificationName, tarClass2counts);

                final int[] detectionThreshold = computeDetectionThreshold(classificationName, srcDoc.getNumberOfSamples(), srcClass2counts, taxonDetectionThresholdPercent);

                computeCoreBiomeRec(sampleIds, asUpperBound, srcDoc.getNumberOfSamples(), samplesThreshold, detectionThreshold, root, srcClass2counts, tarClass2counts, progress);
                // System.err.println(classificationName + ": " + tarClassification2class2counts.size());
            }

            final Map<Integer, float[]> taxId2counts = tarClassification2class2counts.get(ClassificationType.Taxonomy.toString());
            if (taxId2counts != null) {
                for (Integer taxId : taxId2counts.keySet()) {
                    if (taxId >= 0) {
                        float[] values = taxId2counts.get(taxId);
                        size += values[0];
                    }
                }
            }
            if (size == 0) {
                for (String classificationName : dataTable.getClassification2Class2Counts().keySet()) {
                    if (!classificationName.equals(ClassificationType.Taxonomy.toString())) {
                        final Map<Integer, float[]> id2counts = tarClassification2class2counts.get(classificationName);
                        if (id2counts != null) {
                            for (Integer ids : id2counts.keySet()) {
                                final float[] values = id2counts.get(ids);
                                if (ids >= 0)
                                    size += values[0];
                            }
                            if (size > 0)
                                break;
                        }
                    }
                }
            }
        }
        return size;
    }

    /**
     * determines the number of counts necessary for a taxon to be considered detected, for each sample
     *
     * @param numberOfSamples
     * @param srcClass2counts
     * @param detectionThresholdPercent
     * @return thresholds
     */
    private static int[] computeDetectionThreshold(String classificationName, int numberOfSamples, Map<Integer, float[]> srcClass2counts, float detectionThresholdPercent) {
        final int[] array = new int[numberOfSamples];
        if (detectionThresholdPercent > 0) {
            for (Integer id : srcClass2counts.keySet()) {
                if (id > 0) {
                    final float[] counts = srcClass2counts.get(id);
                    if (counts != null) {
                        for (int i = 0; i < counts.length; i++) {
                            array[i] += counts[i];
                        }
                    }
                }
            }
            for (int i = 0; i < array.length; i++) {
                array[i] *= detectionThresholdPercent / 100.0;
            }
            System.err.println("Read detection thresholds for " + classificationName + ":");
            for (float value : array) {
                System.err.print(String.format(" %,12.0f", value));
            }
            System.err.println();
        }
        for (int i = 0; i < array.length; i++) { // need at least 1 to detect
            array[i] = Math.max(1, array[i]);
        }
        return array;
    }

    /**
     * recursively compute the core biome
     *
     * @param samplesThreshold
     * @param detectionThreshold
     * @param v
     * @param srcClass2counts
     * @param tarClass2counts
     */
    private static float[] computeCoreBiomeRec(BitSet sampleIds, boolean asUpperBound, int numberOfSamples, int samplesThreshold, int[] detectionThreshold,
                                               Node v, Map<Integer, float[]> srcClass2counts, Map<Integer, float[]> tarClass2counts, ProgressListener progress) throws CanceledException {
        final float[] summarized = new float[numberOfSamples];

        final int classId = (Integer) v.getInfo();

        if (classId == -1 || classId == -2 || classId == -3)
            return summarized;  // ignore unassigned etc

        final float[] countsV = srcClass2counts.get(classId);
        if (countsV != null) {
            for (int i = 0; i < countsV.length; i++) {
                if (sampleIds.get(i))
                    summarized[i] = countsV[i];
            }
        }

        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            final Node w = e.getTarget();
            final float[] countsBelow = computeCoreBiomeRec(sampleIds, asUpperBound, numberOfSamples, samplesThreshold, detectionThreshold, w, srcClass2counts, tarClass2counts, progress);
            for (int i = 0; i < numberOfSamples; i++) {
                if (sampleIds.get(i)) {
                    summarized[i] += countsBelow[i];
                }
            }
        }

        int numberOfSamplesWithClass = 0;
        int value = 0;
        for (int i = 0; i < numberOfSamples; i++) {
            if (sampleIds.get(i)) {
                if (summarized[i] >= detectionThreshold[i])
                    numberOfSamplesWithClass++;
                if (countsV != null && i < countsV.length && sampleIds.get(i))
                    value += countsV[i];
            }
        }
        if (countsV != null && numberOfSamplesWithClass > 0 && ((!asUpperBound && numberOfSamplesWithClass >= samplesThreshold) || (asUpperBound && numberOfSamplesWithClass <= samplesThreshold))) {
            tarClass2counts.put(classId, new float[]{value});
        }
        progress.checkForCancel();
        return summarized;
    }
}
