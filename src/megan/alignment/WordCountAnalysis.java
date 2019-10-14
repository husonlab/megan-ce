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
package megan.alignment;

import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.ProgressListener;
import jloda.util.Single;
import megan.alignment.gui.Alignment;
import megan.alignment.gui.Lane;

import java.util.*;

/**
 * computes the word-count analysis on an alignment
 * Daniel Huson, 5.2012
 */
public class WordCountAnalysis {
    /**
     * runs the word count analysis
     *
     * @param alignment
     * @param wordSize
     * @param step
     * @return mapping of alignment-depth (n) to number of different sequences in alignment (k)
     */
    public static void apply(Alignment alignment, int wordSize, int step, int minDepth,
                             ProgressListener progressListener, Collection<Pair<Number, Number>> depthVsDifferences, SortedMap<Number, Number> rank2percentage) throws CanceledException {
        if (!alignment.getSequenceType().equalsIgnoreCase(Alignment.DNA)
                && !alignment.getSequenceType().equalsIgnoreCase(Alignment.cDNA))
            return;

        if (wordSize < 1 || step < 1)
            return;

        int firstCol = Integer.MAX_VALUE;
        int lastCol = 0;
        for (int row = 0; row < alignment.getNumberOfSequences(); row++) {
            Lane lane = alignment.getLane(row);
            firstCol = Math.min(firstCol, lane.getFirstNonGapPosition());
            lastCol = Math.max(firstCol, lane.getLastNonGapPosition());
        }

        if (progressListener != null) {
            progressListener.setTasks("Computing diversity plot", "");
            progressListener.setMaximum(lastCol / step);
            progressListener.setProgress(0);
        }

        Set<String> words = new HashSet<>();
        Map<String, Integer> word2count = new HashMap<>();
        Map<Integer, Pair<Integer, Integer>> rank2countAndTotal = new HashMap<>();

        for (int col = firstCol; col <= lastCol; col += step) {
            words.clear();
            int n = 0;
            for (int row = 0; row < alignment.getNumberOfSequences(); row++) {
                Lane lane = alignment.getLane(row);
                if (col >= lane.getFirstNonGapPosition() && col + wordSize <= lane.getLastNonGapPosition()) {
                    int start = col - lane.getFirstNonGapPosition();
                    String word = lane.getBlock().substring(start, start + wordSize).toUpperCase();
                    words.add(word);
                    n++;
                    Integer previousCount = word2count.get(word);
                    word2count.put(word, previousCount == null ? 1 : previousCount + 1);
                }
            }
            if (n >= minDepth) {
                depthVsDifferences.add(new Pair<>(n, words.size()));

                SortedSet<Pair<Integer, String>> ranker = new TreeSet<>((pair1, pair2) -> {
                    if (pair1.get1() > pair2.get1())
                        return -1;
                    else if (pair1.get1() < pair2.get1())
                        return 1;
                    else
                        return pair1.get2().compareTo(pair2.get2());
                });
                for (Map.Entry<String, Integer> entry : word2count.entrySet()) {
                    ranker.add(new Pair<>(entry.getValue(), entry.getKey()));
                }
                int rank = 1;
                for (Pair<Integer, String> pair : ranker) {
                    rank2countAndTotal.put(rank, new Pair<>(pair.get1(), n));
                }
            }
            if (progressListener != null)
                progressListener.incrementProgress();
        }
        // compute average coverage for each rank:
        for (Map.Entry<Integer, Pair<Integer, Integer>> entry : rank2countAndTotal.entrySet()) {
            Pair<Integer, Integer> pair = entry.getValue();
            rank2percentage.put(entry.getKey(), (float) ((100.0 * pair.get1()) / pair.get2()));
        }
    }

    /**
     * compute the Menten kinetics for a list of values, see http://en.wikipedia.org/wiki/Michaelis-Menten_kinetics
     *
     * @param depthVsDifferences
     * @return points to be plotted
     */
    public static LinkedList<Pair<Number, Number>> computeMentenKinetics(Collection<Pair<Number, Number>> depthVsDifferences, Single<Integer> extrapolatedCount) {
        if (depthVsDifferences.size() == 0)
            return new LinkedList<>();

        Pair<Number, Number>[] array = (Pair<Number, Number>[]) depthVsDifferences.toArray(new Pair[0]);

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int vMax = 0;
        for (Pair<Number, Number> pair : array) {
            minX = Math.min(minX, pair.get1().intValue());
            maxX = Math.max(maxX, pair.get1().intValue());
            vMax = Math.max(vMax, pair.get2().intValue());
        }

        // sort by increasing first value
        Arrays.sort(array);

        // smooth the values:
        vMax = 0;
        double[] smoothedValues = new double[array.length];
        double sum = 0;
        int count = 0;
        for (int i = 0; i < array.length; i++) {
            Pair<Number, Number> pair = array[i];
            sum += pair.getSecond().intValue();
            if (count < 10)
                count++;
            else
                sum -= array[i - 10].getSecond().intValue();
            if (i > 5)
                smoothedValues[i - 5] = sum / count;
        }
        for (int i = 0; i < Math.min(5, smoothedValues.length); i++) {
            smoothedValues[i] = smoothedValues[Math.min(5, smoothedValues.length) - 1];
        }
        for (int i = 0; i < array.length; i++) {
            smoothedValues[i] = smoothedValues[Math.max(0, array.length - 6)];
        }

        for (int i = 0; i < array.length; i++) {
            Pair<Number, Number> pair = array[i];
            array[i] = new Pair<>(pair.get1(), smoothedValues[i]);
            // pair.set2(smoothedValues[i]);       // uncomment this line to plot the smoothed values
            vMax = Math.max(vMax, (int) smoothedValues[i]);
        }

        int first = 0;
        while (first < array.length - 1 && array[first + 1].get2().intValue() < vMax / 2)
            first++;

        int kM = array[first].get1().intValue();

        float stepX = Math.max(0.1f, (maxX - minX) / 100.0f);

        System.err.println("vMax: " + vMax + " kM: " + kM);
        if (extrapolatedCount != null) {
            int x = Math.max(100000, 1000 * maxX);
            extrapolatedCount.set(Math.max(1, (int) Math.round((vMax * x) / (double) (kM + x))));
            System.err.println("Extrapolated count: " + extrapolatedCount.get());
        }

        LinkedList<Pair<Number, Number>> result = new LinkedList<>();
        for (float x = minX; x <= maxX; x += stepX) {
            float y = (vMax * x) / (kM + x);
            result.add(new Pair<>(x, y));
        }
        return result;
    }

    /**
     * compute the average k and N values
     *
     * @param values
     * @return average N and K values
     */
    public static Pair<Float, Float> computeAverageNandK(LinkedList<Pair<Number, Number>> values) {
        if (values.size() == 0)
            return new Pair<>(0f, 0f);

        double n = 0;
        double k = 0;
        for (Pair<Number, Number> pair : values) {
            n += pair.get1().doubleValue();
            k += pair.get2().doubleValue();
        }
        return new Pair<>((float) (n / values.size()), (float) (k / values.size()));
    }
}
