/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.rma6.ReadBlockRMA6;

import java.io.IOException;
import java.util.BitSet;

/**
 * determines the set of matches that are active for a given read, that is, which pass
 * all filter criteria
 * Daniel Huson, 1.2009
 */
public class ActiveMatches {
    /**
     * get the set of matches active for the given read
     *  @param minScore
     * @param topPercent
     * @param maxExpected
     * @param readBlock
     * @param activeMatchesForClassification
     * @throws IOException
     */
    public static void compute(double minScore, double topPercent, double maxExpected, float minPercentIdentity, IReadBlock readBlock, String classificationName, BitSet activeMatchesForClassification) throws IOException {
        activeMatchesForClassification.clear();
        // the set of matches that we will consider:
        for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
            if (!matchBlock.isIgnore() && matchBlock.getBitScore() >= minScore && matchBlock.getExpected() <= maxExpected && matchBlock.getPercentIdentity() >= minPercentIdentity) {
                if (matchBlock.getId(classificationName) > 0)
                    activeMatchesForClassification.set(i);
            }
        }

        // determine best score:
        float bestScore = 0;
        for (int i = activeMatchesForClassification.nextSetBit(0); i != -1; i = activeMatchesForClassification.nextSetBit(i + 1)) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
            float score = matchBlock.getBitScore();
            if (score > bestScore)
                bestScore = score;
        }

        // For taxonomy-relevant matches: keep only hits within percentage of top one
        applyTopPercentFilter(topPercent, bestScore, minPercentIdentity, readBlock, activeMatchesForClassification);
    }

    /**
     * applies the top percent filter to a set of active matches
     *
     * @param topPercent
     * @param bestScore     if 0, which compute this from data
     * @param readBlock     current read block
     * @param activeMatches current set of active matches
     */
    public static void applyTopPercentFilter(double topPercent, double bestScore, float minPercentIdentity, IReadBlock readBlock, BitSet activeMatches) {
        if (topPercent > 0 && topPercent < 100) {
            if (bestScore == 0) {
                for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
                    final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                    if (matchBlock.getPercentIdentity() >= minPercentIdentity) {
                        bestScore = Math.max(bestScore, matchBlock.getBitScore());
                    }
                }
            }
            // keep only hits within percentage of top one
            final double threshold = (1 - topPercent / 100.0) * bestScore;

            for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
                final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                if (matchBlock.getBitScore() < threshold && matchBlock.getPercentIdentity() >= minPercentIdentity)
                    activeMatches.set(i, false);
            }
        }
    }

    /**
     * given matches for both read block and its mate, set the sets of active matches to those that share taxa
     * This is used to focus paired reads onto intersection of their taxa, unless they have none in common, in which case
     * the sets are left untouched
     *
     * @param readBlock
     * @param activeMatchesForTaxa
     * @param mateReadBlock
     * @param activeMatches
     */
    public static void restrictActiveMatchesToSameIds(IReadBlock readBlock, BitSet activeMatchesForTaxa, ReadBlockRMA6 mateReadBlock, String classificationName, BitSet activeMatches) {
        if (activeMatchesForTaxa.cardinality() == 0 || activeMatches.cardinality() == 0)
            return; // one set is empty, can't intersect

        final BitSet setA = new BitSet();
        for (int i = activeMatchesForTaxa.nextSetBit(0); i != -1; i = activeMatchesForTaxa.nextSetBit(i + 1)) {
            setA.set(readBlock.getMatchBlock(i).getId(classificationName));
        }
        final BitSet setB = new BitSet();
        for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
            setB.set(mateReadBlock.getMatchBlock(i).getId(classificationName));
        }

        setA.and(setB); // compute intersection
        if (setA.cardinality() == 0)
            return; // have no taxa in common, do not intersect...

        setB.clear(); // now use setB to store which matches to keep
        for (int i = activeMatchesForTaxa.nextSetBit(0); i != -1; i = activeMatchesForTaxa.nextSetBit(i + 1)) {
            if (setA.get(readBlock.getMatchBlock(i).getId(classificationName)))
                setB.set(i);
        }
        activeMatchesForTaxa.and(setB);

        setB.clear();
        for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
            if (setA.get(mateReadBlock.getMatchBlock(i).getId(classificationName)))
                setB.set(i);
        }
        activeMatches.and(setB);
    }
}
