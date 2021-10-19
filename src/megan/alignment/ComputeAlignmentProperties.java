/*
 * ComputeAlignmentProperties.java Copyright (C) 2021. Daniel H. Huson
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
 *
 */
package megan.alignment;

import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.progress.ProgressListener;
import megan.alignment.gui.Alignment;
import megan.alignment.gui.Lane;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * compute different properties of an alignment
 * Daniel Huson, 9.2011
 */
public class ComputeAlignmentProperties {
    /**
     * computes the k-mer based diversity index
     *
     * @param alignment
     * @param kmer
     * @param step
     * @param minDepth
     * @return k/n
     */
    public static Pair<Double, Double> computeSequenceDiversityRatio(Alignment alignment, int kmer, int step, int minDepth, ProgressListener progressListener) throws CanceledException {
        progressListener.setTasks("Computing diversity ratio k/n", "");
        progressListener.setMaximum(alignment.getNumberOfSequences());
        progressListener.setProgress(0);
        int firstCol = Integer.MAX_VALUE;
        int lastCol = 0;
        for (int row = 0; row < alignment.getNumberOfSequences(); row++) {
            Lane lane = alignment.getLane(row);
            firstCol = Math.min(firstCol, lane.getFirstNonGapPosition());
            lastCol = Math.max(firstCol, lane.getLastNonGapPosition());
        }

        Set<String> words = new HashSet<>();
        List<Pair<Integer, Integer>> listKN = new LinkedList<>();
        for (int col = firstCol; col <= lastCol; col += step) {
            words.clear();
            int n = 0;
            for (int row = 0; row < alignment.getNumberOfSequences(); row++) {
                Lane lane = alignment.getLane(row);
                if (col >= lane.getFirstNonGapPosition() && col + kmer <= lane.getLastNonGapPosition()) {
                    int start = col - lane.getFirstNonGapPosition();
                    String word = lane.getBlock().substring(start, start + kmer);
                    if (!(word.contains("-") || word.contains("N"))) {
                        words.add(word);
                        n++;
                    }
                }
                if (n >= minDepth) {
                    listKN.add(new Pair<>(words.size(), n));
                }
                progressListener.incrementProgress();
            }
        }
        if (listKN.size() == 0)
            return new Pair<>(0.0, 0.0);

        double averageK = 0;
        double averageN = 0;
        //System.err.println("k, n:");
        for (Pair<Integer, Integer> kn : listKN) {
            averageK += kn.getFirst();
            averageN += kn.getSecond();
            // System.err.println(kn.getFirst()+", "+kn.getSecond());
        }
        averageK /= listKN.size();
        averageN /= listKN.size();

        // System.err.println("Average k,n="+averageK+" "+averageN);

        return new Pair<>(averageK, averageN);
    }

    /**
     * computes the CG content and coverage
     *
     * @param alignment
     * @return CG content (in percent) and coverage
     */
    public static Pair<Double, Double> computeCGContentAndCoverage(Alignment alignment, ProgressListener progressListener) throws CanceledException {
        if (progressListener != null) {
            progressListener.setTasks("Computing CG content and coverage", "");
            progressListener.setMaximum(alignment.getNumberOfSequences());
            progressListener.setProgress(0);
        }

        int cgCount = 0;
        int atCount = 0;
        int otherLetterCount = 0;
        int firstCol = Integer.MAX_VALUE;
        int lastCol = 0;

        for (int row = 0; row < alignment.getNumberOfSequences(); row++) {
            Lane lane = alignment.getLane(row);
            firstCol = Math.min(firstCol, lane.getFirstNonGapPosition());
            lastCol = Math.max(lastCol, lane.getLastNonGapPosition());
            String block = lane.getBlock();
            for (int i = 0; i < block.length(); i++) {
                int ch = Character.toUpperCase(block.charAt(i));
                if (ch == 'C' || ch == 'G') {
                    cgCount++;
                } else if (ch == 'A' || ch == 'T' || ch == 'U') {
                    atCount++;
                } else if (Character.isLetter(ch))
                    otherLetterCount++;
            }
            if (progressListener != null)
                progressListener.incrementProgress();
        }
        double totalCount = cgCount + atCount + otherLetterCount;
        double cgContent = 100 * (totalCount > 0 ? (double) cgCount / totalCount : 0);
        double totalLength = lastCol - firstCol + 1;
        double coverage = (firstCol <= lastCol ? totalCount / totalLength : 0);

        return new Pair<>(cgContent, coverage);
    }
}
