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

package megan.assembly.align;

import jloda.util.BlastMode;
import jloda.util.BoyerMoore;
import jloda.util.Single;

import java.util.Iterator;

/**
 * convenience class for aligning a DNA query into a DNA reference
 * Created by huson on 2/9/16.
 */
public class SimpleAligner4DNA {
    public enum OverlapType {QuerySuffix2RefPrefix, QueryContainedInRef, QueryPrefix2RefSuffix, None} // what is query?

    private final AlignerOptions alignerOptions;
    private final BandedAligner bandedAligner;
    private int minRawScore = 1;
    private float minPercentIdentity = 0;

    public SimpleAligner4DNA() {
        alignerOptions = new AlignerOptions();
        alignerOptions.setAlignmentType(AlignerOptions.AlignmentMode.SemiGlobal);
        alignerOptions.setScoringMatrix(new DNAScoringMatrix(alignerOptions.getMatchScore(), alignerOptions.getMismatchScore()));
        bandedAligner = new BandedAligner(alignerOptions, BlastMode.BlastN);
    }

    /**
     * compute a semi-global alignment between the query and the reference
     *
     * @param query
     * @param reference
     * @param queryPos
     * @param refPos
     * @param seedLength
     * @return true, if alignment found
     */
    private boolean computeAlignment(byte[] query, byte[] reference, int queryPos, int refPos, int seedLength) {
        bandedAligner.computeAlignment(query, query.length, reference, reference.length, queryPos, refPos, seedLength);
        return bandedAligner.getRawScore() >= minRawScore && (minPercentIdentity == 0 || bandedAligner.getPercentIdentity() >= minPercentIdentity);
    }

    /**
     * set the parameters
     *
     * @param matchScore
     * @param mismatchScore
     * @param gapOpenPenality
     * @param gapExtensionPenality
     */
    public void setAlignmentParameters(int matchScore, int mismatchScore, int gapOpenPenality, int gapExtensionPenality) {
        alignerOptions.setScoringMatrix(new DNAScoringMatrix(matchScore, mismatchScore));
        alignerOptions.setGapOpenPenalty(gapOpenPenality);
        alignerOptions.setGapExtensionPenalty(gapExtensionPenality);
    }

    /**
     * get the min score to be attained
     *
     * @return
     */
    public int getMinRawScore() {
        return minRawScore;
    }

    /**
     * set the min raw score
     *
     * @param minRawScore
     */
    public void setMinRawScore(int minRawScore) {
        this.minRawScore = minRawScore;
    }

    /**
     * get the min percent identity
     *
     * @return
     */
    private float getMinPercentIdentity() {
        return minPercentIdentity;
    }

    /**
     * set the min identity
     *
     * @param minPercentIdentity
     */
    public void setMinPercentIdentity(float minPercentIdentity) {
        this.minPercentIdentity = minPercentIdentity;
    }

    /**
     * gets a position of the query in the reference, or reference.length if not contained
     *
     * @param query
     * @param reference
     * @param queryMustBeContained
     * @return pos or reference.length
     */
    private int getPositionInReference(byte[] query, byte[] reference, boolean queryMustBeContained) {
        if (queryMustBeContained && getMinPercentIdentity() >= 100) {
            return (new BoyerMoore(query, 0, query.length, 127)).search(reference);
        }

        int bestQueryPos = 0;
        int bestRefPos = 0;
        int bestScore = 0;

        final int k = Math.max(10, (int) (100.0 / (100.0 - minPercentIdentity + 1))); // determine smallest exact match that must be present
        for (int queryPos = 0; queryPos < query.length - k + 1; queryPos += k) {
            BoyerMoore boyerMoore = new BoyerMoore(query, queryPos, k, 127);
            for (Iterator<Integer> it = boyerMoore.iterator(reference); it.hasNext(); ) {
                int refPos = it.next();
                if ((!queryMustBeContained && computeAlignment(query, reference, queryPos, refPos, k))
                        || (queryMustBeContained && refPos <= reference.length - query.length && computeAlignment(query, reference, queryPos, refPos, k) && bandedAligner.getAlignmentLength() >= query.length)) {
                    {
                        if (bandedAligner.getRawScore() > bestScore) {
                            bestScore = bandedAligner.getRawScore();
                            bestQueryPos = queryPos;
                            bestRefPos = refPos;
                        }
                    }
                }
            }
        }
        if (bestScore > 0) {
            computeAlignment(query, reference, bestQueryPos, bestRefPos, k);
            return bestRefPos;
        }
        return reference.length;
    }

    /**
     * gets the overlap type of the query in the reference
     *
     * @param query
     * @param reference
     * @param overlap   length
     * @return type
     */
    public OverlapType getOverlap(byte[] query, byte[] reference, Single<Integer> overlap) {
        if (getPositionInReference(query, reference, false) != reference.length) {
            if (bandedAligner.getStartQuery() > 0 && bandedAligner.getStartReference() == 0 && bandedAligner.getAlignmentLength() < reference.length) {
                overlap.set(query.length - bandedAligner.getStartQuery());
                return OverlapType.QuerySuffix2RefPrefix;
            } else if (bandedAligner.getStartQuery() == 0 && bandedAligner.getStartReference() > 0 && bandedAligner.getAlignmentLength() < query.length) {
                overlap.set(bandedAligner.getEndQuery());
                return OverlapType.QueryPrefix2RefSuffix;
            } else if (bandedAligner.getStartQuery() == 0 && bandedAligner.getEndQuery() == query.length) {
                overlap.set(query.length);
                return OverlapType.QueryContainedInRef;
            }
        }
        overlap.set(0);
        return OverlapType.None;
    }

    /**
     * get the percent identity of the last alignment
     *
     * @return percent identity
     */
    public float getPercentIdentity() {
        return bandedAligner.getPercentIdentity();
    }
}
