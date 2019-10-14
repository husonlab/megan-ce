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

package megan.assembly.align;

import jloda.util.BlastMode;
import jloda.util.Pair;

/**
 * all options required by an aligner
 * Daniel Huson, 8.2014
 */
public class AlignerOptions {
    public enum AlignmentMode {Local, SemiGlobal}

    private AlignmentMode alignmentType = AlignmentMode.Local;

    private int minSeedIdentities = 0;
    private int ungappedXDrop = 0;
    private int ungappedMinRawScore = 0;

    private int gapOpenPenalty = 7;
    private int gapExtensionPenalty = 3;
    private int matchScore = 2;
    private int mismatchScore = -3;
    private int band = 4;

    private boolean referenceIsDNA = true;

    // two values for computing blast statistics:
    private double lambda = 0.625;
    private double lnK = -0.89159811928378356416921953633132;

    private IScoringMatrix scoringMatrix;

    private long referenceDatabaseLength = 100000;

    private boolean samSoftClipping = false;


    public AlignmentMode getAlignmentType() {
        return alignmentType;
    }

    public void setAlignmentType(AlignmentMode alignmentType) {
        this.alignmentType = alignmentType;
    }

    public void setAlignmentType(String alignmentType) {
        setAlignmentType(AlignmentMode.valueOf(alignmentType));
    }

    public int getGapOpenPenalty() {
        return gapOpenPenalty;
    }

    public void setGapOpenPenalty(int gapOpenPenalty) {
        this.gapOpenPenalty = gapOpenPenalty;
    }

    public int getGapExtensionPenalty() {
        return gapExtensionPenalty;
    }

    public void setGapExtensionPenalty(int gapExtensionPenalty) {
        this.gapExtensionPenalty = gapExtensionPenalty;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public int getMismatchScore() {
        return mismatchScore;
    }

    public void setMismatchScore(int mismatchScore) {
        this.mismatchScore = mismatchScore;
    }

    public int getBand() {
        return band;
    }

    public void setBand(int band) {
        this.band = band;
    }

    public long getReferenceDatabaseLength() {
        return referenceDatabaseLength;
    }

    public void setReferenceDatabaseLength(long referenceDatabaseLength) {
        this.referenceDatabaseLength = referenceDatabaseLength;
    }

    public IScoringMatrix getScoringMatrix() {
        return scoringMatrix;
    }

    public void setScoringMatrix(IScoringMatrix scoringMatrix) {
        this.scoringMatrix = scoringMatrix;
    }

    public void setLambdaAndK(Pair<Double, Double> lambdaAndK) {
        System.err.println("BLAST statistics parameters: lambda=" + lambdaAndK.get1() + " k=" + lambdaAndK.get2());
        lambda = lambdaAndK.get1();
        lnK = Math.log(lambdaAndK.get2());
    }

    public void setK(double K) {
        this.lnK = Math.log(K);
    }

    public double getK() {
        return Math.exp(lnK);
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    public double getLambda() {
        return lambda;
    }

    public double getLnK() {
        return lnK;
    }

    public boolean isReferenceIsDNA() {
        return referenceIsDNA;
    }

    public void setReferenceIsDNA(boolean referenceIsDNA) {
        this.referenceIsDNA = referenceIsDNA;
    }

    public int getMinSeedIdentities(final BlastMode mode) {
        if (minSeedIdentities == 0) {
            switch (mode) {
                case BlastP:
                case BlastX:
                    return 10;
                case BlastN:
                    return 0; // no need to set this, because BlastN seeds are always completely identical
            }
        }
        return minSeedIdentities;
    }

    public void setMinSeedIdentities(int minSeedIdentities) {
        this.minSeedIdentities = minSeedIdentities;
    }

    public int getUngappedXDrop(final BlastMode mode) {
        if (ungappedXDrop == 0) {
            switch (mode) {
                case BlastP:
                case BlastX:
                    return 20;
                case BlastN:
                    return 8; // todo: need to figure out best default
            }
        }
        return ungappedXDrop;
    }

    public void setUngappedXDrop(int ungappedXDrop) {
        this.ungappedXDrop = ungappedXDrop;
    }

    public int getUngappedMinRawScore(final BlastMode mode) {
        if (ungappedMinRawScore == 0) {
            switch (mode) {
                case BlastP:
                case BlastX:
                    return 60;
                case BlastN:
                    return 60;  // todo: need to figure out best default
            }
        }
        return ungappedMinRawScore;
    }

    public void setUngappedMinRawScore(int ungappedMinRawScore) {
        this.ungappedMinRawScore = ungappedMinRawScore;
    }

    public boolean isSamSoftClipping() {
        return samSoftClipping;
    }

    public void setSamSoftClipping(boolean samSoftClipping) {
        this.samSoftClipping = samSoftClipping;
    }
}
