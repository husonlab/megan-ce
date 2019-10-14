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
package megan.clusteranalysis;

import jloda.util.Triplet;
import megan.clusteranalysis.tree.Distances;
import megan.clusteranalysis.tree.Taxa;
import megan.core.SampleAttributeTable;

import java.util.*;

/**
 * performs the triangulation test
 */
public class TriangulationTest {
    /**
     * performs the triangulation test described in Huson et al, A statistical test for detecting taxonomic inhomogeneity in replicated metagenomic samples
     *
     * @param clusterViewer
     * @param attributeThatDefinesBiologicalSamples
     * @return true, if the H0 "all samples from same taxon distribution" is rejected, false else
     * @throws IllegalArgumentException if sample doesn't have attribute value
     */
    public boolean apply(ClusterViewer clusterViewer, String attributeThatDefinesBiologicalSamples) {

        final SampleAttributeTable sampleAttributeTable = clusterViewer.getDocument().getSampleAttributeTable();
        final ArrayList<Triplet<String, String, String>> triangles = computeTriangluation(sampleAttributeTable, attributeThatDefinesBiologicalSamples); // first two of triplet always in same biological sample, third is from other
        System.err.println("Triangulation (" + triangles.size() + "):");
        for (Triplet<String, String, String> triangle : triangles) {
            System.err.println(triangle.toString());
        }

        final int minNumberOfNonConflictedTrianglesRequired = computeMinNumberOfNonConflictedTrianglesRequired(triangles.size());
        System.err.println("Minimum number of non-conflicted triangles required to reject H0: " + minNumberOfNonConflictedTrianglesRequired);

        final Taxa taxa = clusterViewer.getTaxa();
        final Distances distances = clusterViewer.getDistances();

        int numberOfNonconflictedTriangles = 0;
        for (Triplet<String, String, String> triangle : triangles) {
            double ab = distances.get(taxa.indexOf(triangle.get1()), taxa.indexOf(triangle.get2()));
            double ac = distances.get(taxa.indexOf(triangle.get1()), taxa.indexOf(triangle.get3()));
            double bc = distances.get(taxa.indexOf(triangle.get2()), taxa.indexOf(triangle.get3()));
            if (ac < ab || bc < ab) {
                System.err.println("Conflicted triangle a=" + triangle.get1() + ",b=" + triangle.get2() + " vs c=" + triangle.get3() + ", distances: "
                        + String.format("ab=%.4f, ac=%.4f, bc=%.4f", ab, ac, bc));
            } else
                numberOfNonconflictedTriangles++;
        }
        if (numberOfNonconflictedTriangles < minNumberOfNonConflictedTrianglesRequired)
            System.err.println("Insufficient number of non-conflicted triangles: " + numberOfNonconflictedTriangles + ", null hypothesis not rejected");
        return numberOfNonconflictedTriangles >= minNumberOfNonConflictedTrianglesRequired;
    }

    /**
     * compute a triangulation
     *
     * @return triangulation
     */
    private ArrayList<Triplet<String, String, String>> computeTriangluation(SampleAttributeTable sampleAttributeTable, String attributeThatDefinesBiologicalSamples) {
        final ArrayList<Triplet<String, String, String>> triangles = new ArrayList<>(); // first two of triplet always in same biological sample, third is from other
        for (int i = 0; i < 50; i++) { // try 50 times to compute a perfect triangulation
            triangles.clear();
            final Map<Object, LinkedList<String>> key2set = new HashMap<>();
            final Map<Object, Integer> key2pairs = new HashMap<>();

            for (String sample : sampleAttributeTable.getSampleOrder()) {
                final Object key = sampleAttributeTable.get(sample, attributeThatDefinesBiologicalSamples);
                if (key == null)
                    throw new IllegalArgumentException("No value for sample=" + sample + " and attribute=" + attributeThatDefinesBiologicalSamples);
                LinkedList<String> set = key2set.get(key);
                if (set == null) {
                    set = new LinkedList<>();
                    key2set.put(key, set);
                    key2pairs.put(key, 0);
                }
                set.add(sample);
            }

            for (LinkedList<String> set : key2set.values()) {
                if (set.size() < 2)
                    throw new IllegalArgumentException("Too few samples for attribute=" + attributeThatDefinesBiologicalSamples + ": " + set.size());
            }

            final List<Object> allKeys = new LinkedList<>(key2set.keySet());
            final List<Object> allKeysForDoubletOrMoreSets = new LinkedList<>(key2set.keySet());
            final List<Object> allKeysForSingletonSets = new LinkedList<>();

            final Random random = new Random();

            while (allKeysForDoubletOrMoreSets.size() > 0 && (2 * allKeysForDoubletOrMoreSets.size() + allKeysForSingletonSets.size()) > 2) {
                Object key1 = allKeys.get(random.nextInt(allKeys.size()));

                if (allKeysForSingletonSets.contains(key1)) {
                    final Object key2 = allKeysForDoubletOrMoreSets.get(random.nextInt(allKeysForDoubletOrMoreSets.size())); // won't be key1
                    key2pairs.put(key2, key2pairs.get(key2) + 1);
                    final String sample1 = key2set.get(key2).remove(random.nextInt(key2set.get(key2).size()));
                    final String sample2 = key2set.get(key2).remove(random.nextInt(key2set.get(key2).size()));
                    final String sample3 = key2set.get(key1).remove(0); // last one left in set
                    triangles.add(new Triplet<>(sample1, sample2, sample3));

                    allKeysForSingletonSets.remove(key1);
                    allKeys.remove(key1);

                    if (key2set.get(key2).size() < 2) {
                        allKeysForDoubletOrMoreSets.remove(key2);
                        if (key2set.get(key2).size() == 1)
                            allKeysForSingletonSets.add(key2);
                        else
                            allKeys.remove(key2);
                    }
                } else // key1 points to set with at least two replicates available
                {
                    Object key2;
                    do {
                        key2 = allKeys.get(random.nextInt(allKeys.size()));
                    }
                    while (key2 == key1); // there are at least two keys available, so this will end
                    final boolean pair1 = (allKeysForSingletonSets.contains(key2) || key2pairs.get(key1) < key2pairs.get(key2) || (key2pairs.get(key1).equals(key2pairs.get(key2)) && random.nextBoolean()));

                    if (!pair1) { // swap roles
                        Object tmp = key1;
                        key1 = key2;
                        key2 = tmp;
                    }

                    final String sample1 = key2set.get(key1).remove(random.nextInt(key2set.get(key1).size()));
                    final String sample2 = key2set.get(key1).remove(random.nextInt(key2set.get(key1).size()));
                    final String sample3 = key2set.get(key2).remove(random.nextInt(key2set.get(key2).size()));
                    triangles.add(new Triplet<>(sample1, sample2, sample3));
                    key2pairs.put(key1, key2pairs.get(key1) + 1);

                    if (key2set.get(key1).size() < 2) {
                        allKeysForDoubletOrMoreSets.remove(key1);
                        if (key2set.get(key1).size() == 1 && !allKeysForSingletonSets.contains(key1))
                            allKeysForSingletonSets.add(key1);
                        if (key2set.get(key1).size() == 0) {
                            allKeysForSingletonSets.remove(key1);
                            allKeys.remove(key1);
                        }
                    }
                    if (key2set.get(key2).size() < 2) {
                        allKeysForDoubletOrMoreSets.remove(key2);
                        if (key2set.get(key2).size() == 1 && !allKeysForSingletonSets.contains(key2))
                            allKeysForSingletonSets.add(key2);
                        if (key2set.get(key2).size() == 0) {
                            allKeysForSingletonSets.remove(key2);
                            allKeys.remove(key2);
                        }
                    }
                }
            }
            if ((allKeysForDoubletOrMoreSets.size() == 0 || key2set.get(allKeysForDoubletOrMoreSets.get(0)).size() < 3) && allKeysForSingletonSets.size() < 3)
                break; //  no excess of remaining samples
        }
        return triangles;
    }

    /**
     * computes the minimum number of non-conflicted triangles required to reject H0 with alpha=0.05
     *
     * @param totalTriangles
     * @return max triangles allowed to be in conflict
     */
    private int computeMinNumberOfNonConflictedTrianglesRequired(int totalTriangles) {
        double sum = 0;
        for (int k = totalTriangles; k >= 0; k--) {
            sum += binomial(totalTriangles, k) * Math.pow(1.0 / 3.0, k) * Math.pow(2.0 / 3.0, totalTriangles - k);
            if (sum > 0.05)
                return k + 1;
        }
        return 0;
    }

    /**
     * binomial
     *
     * @param n
     * @param k
     * @return n choose k
     */
    private static long binomial(int n, int k) {
        if (k > n - k)
            k = n - k;
        long b = 1;
        for (int i = 1, m = n; i <= k; i++, m--)
            b = b * m / i;
        return b;
    }

    /**
     * test whether there are at least two samples for each attribute value
     *
     * @param sampleAttributeTable
     * @param attribute
     * @return true, if ok
     */
    public static boolean isSuitableAttribute(SampleAttributeTable sampleAttributeTable, String attribute) {
        if (sampleAttributeTable.isSecretAttribute(attribute) || sampleAttributeTable.isHiddenAttribute(attribute))
            return false;

        boolean ok = true;
        Map<Object, Integer> value2count = new HashMap<>();
        for (String sample : sampleAttributeTable.getSampleOrder()) {
            Object value = sampleAttributeTable.get(sample, attribute);
            if (value == null) {
                ok = false;
                break;
            } else {
                value2count.merge(value, 1, Integer::sum);
            }
        }
        if (ok) {
            for (Integer count : value2count.values()) {
                if (count < 2) {
                    ok = false;
                    break;
                }
            }
        }
        return ok;
    }
}
