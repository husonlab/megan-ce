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
 * BandedAligner.java Copyright (C) 2019 Daniel H. Huson
 * <p>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.assembly.align;

import jloda.util.Basic;
import jloda.util.BlastMode;
import jloda.util.ReusableByteBuffer;

/**
 * banded DNA aligner. Does both local and semiGlobal alignment
 * Daniel Huson, 8.2014
 */
class BandedAligner {
    private double lambda = 0.625;
    private double lnK = -0.89159811928378356416921953633132;
    private final static double LN_2 = 0.69314718055994530941723212145818;
    private final static int MINUS_INFINITY = -100000000;

    public static int ALIGNMENT_SEGMENT_LENGTH = 60; // length of alignment segment in text format output
    private final static byte[] MID_TRACK_LEADING_SPACES = "                 ".getBytes(); // spaces used in text format output

    private long referenceDatabaseLength = 10000000;

    private byte[] query;
    private int queryLength;
    private byte[] reference;
    private int referenceLength;

    private final int[][] scoringMatrix;
    private final int gapOpenPenalty;
    private final int gapExtensionPenalty;
    private final int band;

    private int rawScore;
    private float bitScore = 0;
    private double expected = 0;

    private final boolean isDNAAlignment;

    private int identities;
    private int mismatches;
    private int gaps;
    private int gapOpens;
    private int alignmentLength;

    private final BlastMode mode;
    private final boolean doSemiGlobal;

    private int refOffset; // needed convert from row to position in reference

    private int startQuery; // first alignment position of query
    private int endQuery = -1;   // last alignment position of query +1
    private int startReference;
    private int endReference;

    private int[][] matrixM;
    private int[][] matrixIRef;
    private int[][] matrixIQuery;

    private byte[][] traceBackM;
    private byte[][] traceBackIRef;
    private byte[][] traceBackIQuery;

    private static final byte DONE = 9;
    private static final byte M_FROM_M = 1;
    private static final byte M_FROM_IRef = 2;
    private static final byte M_FROM_IQuery = 3;
    private static final byte IRef_FROM_M = 4;
    private static final byte IRef_FROM_IRef = 5;
    private static final byte IQuery_FROM_M = 6;
    private static final byte IQuery_FROM_IQuery = 7;

    // buffers:
    private byte[] queryTrack = new byte[1000];
    private byte[] midTrack = new byte[1000];
    private byte[] referenceTrack = new byte[1000];

    private ReusableByteBuffer alignmentBuffer = new ReusableByteBuffer(10000);

    private int queryPos;
    private int refPos;

    // new stuff:

    private byte[][] alignment; // last computed alignment
    private int seedLength;
    // number of rows depends only on band width
    private final int rows;
    private final int lastRowToFill;
    private final int middleRow;

    /**
     * constructor
     *
     * @param alignerOptions
     */
    public BandedAligner(final AlignerOptions alignerOptions, final BlastMode mode) {
        this.scoringMatrix = alignerOptions.getScoringMatrix().getMatrix();
        this.isDNAAlignment = (mode == BlastMode.BlastN);
        this.doSemiGlobal = alignerOptions.getAlignmentType() == AlignerOptions.AlignmentMode.SemiGlobal;

        this.lambda = alignerOptions.getLambda();
        this.lnK = alignerOptions.getLnK();

        this.mode = mode;

        band = alignerOptions.getBand();
        gapOpenPenalty = alignerOptions.getGapOpenPenalty();
        gapExtensionPenalty = alignerOptions.getGapExtensionPenalty();
        referenceDatabaseLength = alignerOptions.getReferenceDatabaseLength();

        rows = 2 * band + 3;
        lastRowToFill = rows - 2;
        middleRow = rows / 2; // half

        matrixM = new int[0][0]; // don't init here, need to initialize properly
        matrixIRef = new int[0][0];
        matrixIQuery = new int[0][0];
        traceBackM = new byte[0][0];
        traceBackIRef = new byte[0][0];
        traceBackIQuery = new byte[0][0];
        // todo: only use one traceback matrix

        boolean samSoftClipping = alignerOptions.isSamSoftClipping();
    }

    /**
     * Computes a banded local or semiGlobal alignment.
     * The raw score is computed.
     *
     * @param query
     * @param queryLength
     * @param reference
     * @param referenceLength
     * @param queryPos
     * @param refPos
     */
    public void computeAlignment(byte[] query, int queryLength, byte[] reference, int referenceLength, int queryPos, int refPos, int seedLength) {
        this.query = query;
        this.queryLength = queryLength;
        this.reference = reference;
        this.referenceLength = referenceLength;
        this.queryPos = queryPos;
        this.refPos = refPos;
        this.seedLength = seedLength;

        startQuery = startReference = endQuery = endReference = -1;

        if (doSemiGlobal)
            computeSemiGlobalAlignment();
        else
            computeLocalAlignment();
    }

    /**
     * Performs a banded local alignment and return the raw score.
     */
    private void computeLocalAlignment() {
        alignment = null; // will need to call alignmentByTraceBack to compute this

        refOffset = refPos - queryPos - band - 2; // need this to compute index in reference sequence

        final int cols = queryLength + 2; // query plus one col before and one after

        final int firstSeedCol = queryPos + 1; // +1 because col=pos+1
        final int lastSeedCol = queryPos + seedLength; // +1 because col=pos+1, but then -1 because want to be last in seed (not first after seed)

        //if (lastSeedCol > queryLength)
        //     return; // too long

        // ------- compute score that comes from seed (without first and last member)
        rawScore = 0;
        {
            for (int col = firstSeedCol + 1; col < lastSeedCol; col++) {
                final int refIndex = middleRow + col + refOffset;
                rawScore += scoringMatrix[query[col - 1]][reference[refIndex]];
            }
            if (rawScore <= 0) {
                rawScore = 0;
                return;
            }
        }

        // ------- resize matrices if necessary:
        if (cols >= matrixM.length) {  // all values will be 0
            // resize:
            matrixM = new int[cols][rows];
            matrixIRef = new int[cols][rows];
            matrixIQuery = new int[cols][rows];
            traceBackM = new byte[cols][rows];
            traceBackIRef = new byte[cols][rows];
            traceBackIQuery = new byte[cols][rows];

            // initialize first column:
            for (int r = 1; r < rows; r++) {
                // matrixM[0][r] = matrixIRef[0][r] = matrixIQuery[0][r] = 0;
                traceBackM[0][r] = traceBackIRef[0][r] = traceBackIQuery[0][r] = DONE;
            }
            // initialize the first and last row:
            for (int c = 0; c < cols; c++) {
                // matrixM[c][0] = matrixIRef[c][0] = matrixIQuery[c][0] = matrixM[c][rows - 1] = matrixIRef[c][rows - 1] = matrixIQuery[c][rows - 1] = 0;
                traceBackM[c][0] = traceBackIRef[c][0] = traceBackIQuery[c][0] = traceBackM[c][rows - 1] = traceBackIRef[0][rows - 1] = traceBackIQuery[0][rows - 1] = DONE;
            }
        }


        // ------- fill dynamic programming matrix from 0 to first column of seed:
        {
            final int firstCol = Math.max(1, -refOffset - 2 * band - 1); // the column for which refIndex(firstCol,bottom-to-last row)==0
            if (firstCol > 1) {
                final int prevCol = firstCol - 1;
                final int secondToLastRow = rows - 2;
                traceBackM[prevCol][secondToLastRow] = traceBackIRef[prevCol][secondToLastRow] = traceBackIQuery[prevCol][secondToLastRow] = DONE; // set previous column to done
                matrixM[prevCol][secondToLastRow] = matrixIRef[prevCol][secondToLastRow] = matrixIQuery[prevCol][secondToLastRow] = 0;
            }

            // note that query pos is c-1, because c==0 is before start of query

            for (int col = firstCol; col <= firstSeedCol; col++) {   // we never modify the first column or the first or last row
                for (int row = 1; row <= lastRowToFill; row++) {
                    final int refIndex = row + col + refOffset;

                    if (refIndex == -1) { // in column before reference starts, init
                        traceBackM[col][row] = traceBackIRef[col][row] = traceBackIQuery[col][row] = DONE;
                        matrixM[col][row] = matrixIRef[col][row] = matrixIQuery[col][row] = 0;
                    } else if (refIndex >= 0) //do the actual alignment:
                    {
                        int bestMScore = 0;
                        // match or mismatch
                        {
                            final int s = scoringMatrix[query[col - 1]][reference[refIndex]];

                            int score = matrixM[col - 1][row] + s;
                            if (score > 0) {
                                traceBackM[col][row] = M_FROM_M;
                                bestMScore = score;
                            }
                            score = matrixIRef[col - 1][row] + s;
                            if (score > bestMScore) {
                                traceBackM[col][row] = M_FROM_IRef;
                                bestMScore = score;
                            }
                            score = matrixIQuery[col - 1][row] + s;
                            if (score > bestMScore) {
                                traceBackM[col][row] = M_FROM_IQuery;
                                bestMScore = score;
                            }
                            if (bestMScore == 0) {
                                traceBackM[col][row] = DONE;
                            }
                            matrixM[col][row] = bestMScore;
                        }

                        // insertion in reference:
                        int bestIRefScore = 0;
                        {
                            int score = matrixM[col][row - 1] - gapOpenPenalty;

                            if (score > bestIRefScore) {
                                traceBackIRef[col][row] = IRef_FROM_M;
                                bestIRefScore = score;
                            }

                            score = matrixIRef[col][row - 1] - gapExtensionPenalty;
                            if (score > bestIRefScore) {
                                bestIRefScore = score;
                                traceBackIRef[col][row] = IRef_FROM_IRef;
                            }
                            if (bestIRefScore == 0) {
                                traceBackIRef[col][row] = DONE;
                            }
                            matrixIRef[col][row] = bestIRefScore;

                        }

                        // insertion in query:
                        int bestIQueryScore = 0;
                        {
                            int score = matrixM[col - 1][row + 1] - gapOpenPenalty;

                            if (score > bestIQueryScore) {
                                bestIQueryScore = score;
                                traceBackIQuery[col][row] = IQuery_FROM_M;
                            }

                            score = matrixIQuery[col - 1][row + 1] - gapExtensionPenalty;
                            if (score > bestIQueryScore) {
                                bestIQueryScore = score;
                                traceBackIQuery[col][row] = IQuery_FROM_IQuery;
                            }
                            if (bestIQueryScore == 0) {
                                traceBackIQuery[col][row] = DONE;
                            }
                            matrixIQuery[col][row] = bestIQueryScore;
                        }

                    }
                    // else refIndex < -1

                }
            }
        }

        // ------- fill dynamic programming matrix from end of query to last column of seed:
        {
            final int lastCol = Math.min(queryLength + 1, queryPos + referenceLength - refPos + 1); // last column, fill upto lastCol-1

            // initial last column:

            for (int row = 1; row < rows; row++) {
                matrixM[lastCol][row] = matrixIRef[lastCol][row] = matrixIQuery[lastCol][row] = 0;
                traceBackM[lastCol][row] = traceBackIRef[lastCol][row] = traceBackIQuery[lastCol][row] = DONE;
            }

            // note that col=pos-1, or pos=col+1, because c==0 is before start of query

            /*
            System.err.println("lastSeedCol: " + lastSeedCol);
            System.err.println("lastCol: " + lastCol);
            System.err.println("lastRowToFill: " + lastRowToFill);
*/

            for (int col = lastCol - 1; col >= lastSeedCol; col--) {   // we never modify the first column or the first or last row
                for (int row = lastRowToFill; row >= 1; row--) {
                    final int refIndex = row + col + refOffset;

                    if (refIndex >= referenceLength) { // out of range of the alignment
                        traceBackM[col][row] = traceBackIRef[col][row] = traceBackIQuery[col][row] = DONE;
                        matrixM[col][row] = matrixIRef[col][row] = matrixIQuery[col][row] = 0;
                    } else if (refIndex >= 0) { // do the actual alignment:
                        int bestMScore = 0;
                        // match or mismatch
                        {
                            final int s = scoringMatrix[query[col - 1]][reference[refIndex]]; // pos in query=col-1

                            int score = matrixM[col + 1][row] + s;
                            if (score > 0) {
                                traceBackM[col][row] = M_FROM_M;
                                bestMScore = score;
                            }
                            score = matrixIRef[col + 1][row] + s;
                            if (score > bestMScore) {
                                traceBackM[col][row] = M_FROM_IRef;
                                bestMScore = score;
                            }
                            score = matrixIQuery[col + 1][row] + s;
                            if (score > bestMScore) {
                                traceBackM[col][row] = M_FROM_IQuery;
                                bestMScore = score;
                            }
                            if (bestMScore == 0) {
                                traceBackM[col][row] = DONE;
                            }
                            matrixM[col][row] = bestMScore;
                        }

                        // insertion in ref
                        int bestIRefScore = 0;
                        {
                            int score = matrixM[col][row + 1] - gapOpenPenalty;

                            if (score > bestIRefScore) {
                                traceBackIRef[col][row] = IRef_FROM_M;
                                bestIRefScore = score;
                            }

                            score = matrixIRef[col][row + 1] - gapExtensionPenalty;
                            if (score > bestIRefScore) {
                                bestIRefScore = score;
                                traceBackIRef[col][row] = IRef_FROM_IRef;
                            }
                            if (bestIRefScore == 0) {
                                traceBackIRef[col][row] = DONE;
                            }
                            matrixIRef[col][row] = bestIRefScore;

                        }

                        // insertion in query:
                        int bestIQueryScore = 0;
                        {
                            int score = matrixM[col + 1][row - 1] - gapOpenPenalty;

                            if (score > bestIQueryScore) {
                                bestIQueryScore = score;
                                traceBackIQuery[col][row] = IQuery_FROM_M;
                            }

                            score = matrixIQuery[col + 1][row - 1] - gapExtensionPenalty;
                            if (score > bestIQueryScore) {
                                bestIQueryScore = score;
                                traceBackIQuery[col][row] = IQuery_FROM_IQuery;
                            }
                            if (bestIQueryScore == 0) {
                                traceBackIQuery[col][row] = DONE;
                            }
                            matrixIQuery[col][row] = bestIQueryScore;
                        }

                    }
                    // else  refIndex >referenceLength
                }
            }
        }

        rawScore += Math.max(Math.max(matrixIQuery[firstSeedCol][middleRow], matrixIRef[firstSeedCol][middleRow]), matrixM[firstSeedCol][middleRow]);
        rawScore += Math.max(Math.max(matrixIQuery[lastSeedCol][middleRow], matrixIRef[lastSeedCol][middleRow]), matrixM[lastSeedCol][middleRow]);
    }

    /**
     * Performs a banded semi-global alignment.
     */
    private void computeSemiGlobalAlignment() {
        alignment = null; // will need to call alignmentByTraceBack to compute this

        refOffset = refPos - queryPos - band - 2; // need this to compute index in reference sequence

        final int cols = queryLength + 2; // query plus one col before and one after

        final int firstSeedCol = queryPos + 1; // +1 because col=pos+1
        final int lastSeedCol = queryPos + seedLength; // +1 because col=pos+1, but then -1 because want to be last in seed (not first after seed)

        //if (lastSeedCol > queryLength)
        //    return; // too long

        // ------- compute score that comes from seed (without first and last member)
        rawScore = 0;
        {
            for (int col = firstSeedCol + 1; col < lastSeedCol; col++) {
                final int refIndex = middleRow + col + refOffset;
                rawScore += scoringMatrix[query[col - 1]][reference[refIndex]];
            }
            if (rawScore <= 0) {
                rawScore = 0;
                return;
            }
        }

        // ------- resize matrices if necessary:
        if (cols >= matrixM.length) {  // all values will be 0
            // resize:
            matrixM = new int[cols][rows];
            matrixIRef = new int[cols][rows];
            matrixIQuery = new int[cols][rows];
            traceBackM = new byte[cols][rows];
            traceBackIRef = new byte[cols][rows];
            traceBackIQuery = new byte[cols][rows];

            // initialize first column:
            for (int r = 1; r < rows; r++) {
                traceBackM[0][r] = traceBackIRef[0][r] = traceBackIQuery[0][r] = DONE;
                matrixIQuery[0][r] = -gapOpenPenalty;
            }
            // initialize the first and last row:
            for (int c = 0; c < cols; c++) {
                matrixM[c][0] = matrixIRef[c][0] = matrixIQuery[c][0]
                        = matrixM[c][rows - 1] = matrixIRef[c][rows - 1] = matrixIQuery[c][rows - 1]
                        = MINUS_INFINITY; // must never go outside band
            }
        }

        // ------- fill dynamic programming matrix from 0 to first column of seed:
        {
            final int firstCol = Math.max(1, -refOffset - 2 * band - 1); // the column for which refIndex(firstCol,bottom-to-last row)==0
            if (firstCol > 1) {
                final int prevCol = firstCol - 1;
                final int secondToLastRow = rows - 2;
                traceBackM[prevCol][secondToLastRow] = traceBackIRef[prevCol][secondToLastRow] = traceBackIQuery[prevCol][secondToLastRow] = DONE; // set previous column to done
                matrixM[prevCol][secondToLastRow] = matrixIRef[prevCol][secondToLastRow] = matrixIQuery[prevCol][secondToLastRow] = 0;
            }

            // note that query pos is c-1, because c==0 is before start of query

            for (int col = firstCol; col <= firstSeedCol; col++) {   // we never modify the first column or the first or last row
                for (int row = 1; row <= lastRowToFill; row++) {
                    final int refIndex = row + col + refOffset;
                    if (refIndex >= reference.length)
                        continue; // todo: debug this, sometimes happens, but shouldn't

                    if (refIndex == -1) { // in column before reference starts, init
                        traceBackM[col][row] = traceBackIRef[col][row] = traceBackIQuery[col][row] = DONE;
                        matrixM[col][row] = 0;
                        matrixIRef[col][row] = matrixIQuery[col][row] = -gapOpenPenalty;
                    } else if (refIndex >= 0) //do the actual alignment:
                    {
                        int bestMScore = Integer.MIN_VALUE;
                        // match or mismatch
                        {
                            final int s = scoringMatrix[query[col - 1]][reference[refIndex]];

                            int score = matrixM[col - 1][row] + s;
                            if (score > bestMScore) {
                                traceBackM[col][row] = M_FROM_M;
                                bestMScore = score;
                            }
                            score = matrixIRef[col - 1][row] + s;
                            if (score > bestMScore) {
                                traceBackM[col][row] = M_FROM_IRef;
                                bestMScore = score;
                            }
                            score = matrixIQuery[col - 1][row] + s;
                            if (score > bestMScore) {
                                traceBackM[col][row] = M_FROM_IQuery;
                                bestMScore = score;
                            }
                            matrixM[col][row] = bestMScore;
                        }

                        // insertion in reference:
                        int bestIRefScore = Integer.MIN_VALUE;
                        {
                            int score = matrixM[col][row - 1] - gapOpenPenalty;

                            if (score > bestIRefScore) {
                                traceBackIRef[col][row] = IRef_FROM_M;
                                bestIRefScore = score;
                            }

                            score = matrixIRef[col][row - 1] - gapExtensionPenalty;
                            if (score > bestIRefScore) {
                                bestIRefScore = score;
                                traceBackIRef[col][row] = IRef_FROM_IRef;
                            }
                            matrixIRef[col][row] = bestIRefScore;
                        }

                        // insertion in query:
                        int bestIQueryScore = Integer.MIN_VALUE;
                        {
                            int score = matrixM[col - 1][row + 1] - gapOpenPenalty;

                            if (score > bestIQueryScore) {
                                bestIQueryScore = score;
                                traceBackIQuery[col][row] = IQuery_FROM_M;
                            }

                            score = matrixIQuery[col - 1][row + 1] - gapExtensionPenalty;
                            if (score > bestIQueryScore) {
                                bestIQueryScore = score;
                                traceBackIQuery[col][row] = IQuery_FROM_IQuery;
                            }
                            matrixIQuery[col][row] = bestIQueryScore;
                        }
                    }
                    // else refIndex < -1
                }
            }
        }

        // ------- fill dynamic programming matrix from end of query to last column of seed:
        {
            final int lastCol = Math.min(queryLength + 1, queryPos + referenceLength - refPos + 1); // last column, fill upto lastCol-1

            // initial last column:

            for (int row = 1; row < rows - 1; row++) { // no need to init first or last row...
                matrixM[lastCol][row] = 0;
                matrixIRef[lastCol][row] = matrixIQuery[lastCol][row] = -gapOpenPenalty;
                traceBackM[lastCol][row] = traceBackIRef[lastCol][row] = traceBackIQuery[lastCol][row] = DONE;
            }

            // note that col=pos-1, or pos=col+1, because c==0 is before start of query

            /*
            System.err.println("lastSeedCol: " + lastSeedCol);
            System.err.println("lastCol: " + lastCol);
            System.err.println("lastRowToFill: " + lastRowToFill);
            */

            for (int col = lastCol - 1; col >= lastSeedCol; col--) {   // we never modify the first column or the first or last row
                for (int row = lastRowToFill; row >= 1; row--) {
                    final int refIndex = row + col + refOffset;

                    if (refIndex >= referenceLength) { // out of range of the alignment
                        traceBackM[col][row] = traceBackIRef[col][row] = traceBackIQuery[col][row] = DONE;
                        matrixM[col][row] = matrixIRef[col][row] = matrixIQuery[col][row] = -gapOpenPenalty;
                    } else if (refIndex >= 0) { // do the actual alignment:
                        int bestMScore = Integer.MIN_VALUE;
                        // match or mismatch
                        {
                            final int s = scoringMatrix[query[col - 1]][reference[refIndex]]; // pos in query=col-1

                            int score = matrixM[col + 1][row] + s;
                            if (score > bestMScore) {
                                traceBackM[col][row] = M_FROM_M;
                                bestMScore = score;
                            }
                            score = matrixIRef[col + 1][row] + s;
                            if (score > bestMScore) {
                                traceBackM[col][row] = M_FROM_IRef;
                                bestMScore = score;
                            }
                            score = matrixIQuery[col + 1][row] + s;
                            if (score > bestMScore) {
                                traceBackM[col][row] = M_FROM_IQuery;
                                bestMScore = score;
                            }
                            matrixM[col][row] = bestMScore;
                        }

                        // insertion in ref
                        int bestIRefScore = Integer.MIN_VALUE;
                        {
                            int score = matrixM[col][row + 1] - gapOpenPenalty;

                            if (score > bestIRefScore) {
                                traceBackIRef[col][row] = IRef_FROM_M;
                                bestIRefScore = score;
                            }

                            score = matrixIRef[col][row + 1] - gapExtensionPenalty;
                            if (score > bestIRefScore) {
                                bestIRefScore = score;
                                traceBackIRef[col][row] = IRef_FROM_IRef;
                            }
                            matrixIRef[col][row] = bestIRefScore;
                        }

                        // insertion in query:
                        int bestIQueryScore = Integer.MIN_VALUE;
                        {
                            int score = matrixM[col + 1][row - 1] - gapOpenPenalty;

                            if (score > bestIQueryScore) {
                                bestIQueryScore = score;
                                traceBackIQuery[col][row] = IQuery_FROM_M;
                            }

                            score = matrixIQuery[col + 1][row - 1] - gapExtensionPenalty;
                            if (score > bestIQueryScore) {
                                bestIQueryScore = score;
                                traceBackIQuery[col][row] = IQuery_FROM_IQuery;
                            }
                            matrixIQuery[col][row] = bestIQueryScore;
                        }
                    }
                    // else  refIndex >referenceLength
                }
            }
        }

        rawScore += Math.max(Math.max(matrixIQuery[firstSeedCol][middleRow], matrixIRef[firstSeedCol][middleRow]), matrixM[firstSeedCol][middleRow]);
        rawScore += Math.max(Math.max(matrixIQuery[lastSeedCol][middleRow], matrixIRef[lastSeedCol][middleRow]), matrixM[lastSeedCol][middleRow]);
    }

    /**
     * compute the bit score and expected score from the raw score
     */
    public void computeBitScoreAndExpected() {
        if (rawScore > 0) {
            bitScore = (float) ((lambda * rawScore - lnK) / LN_2);
            expected = referenceDatabaseLength * queryLength * Math.pow(2, -bitScore);
        } else {
            bitScore = 0;
            expected = Double.MAX_VALUE;
        }
    }

    /**
     * gets the alignment. Also sets the number of matches, mismatches and gaps
     *
     * @return alignment
     */
    private void computeAlignmentByTraceBack() {
        if (rawScore <= 0) {
            alignment = null;
            return;
        }

        gaps = 0;
        gapOpens = 0;
        identities = 0;
        mismatches = 0;

        // get first part of alignment:
        int length = 0;
        {
            int r = middleRow;
            int c = queryPos + 1;

            byte[][] traceBack;
            traceBack = traceBackM;
            if (matrixIRef[c][r] > matrixM[c][r]) {
                traceBack = traceBackIRef;
                if (matrixIQuery[c][r] > matrixIRef[c][r])
                    traceBack = traceBackIQuery;
            } else if (matrixIQuery[c][r] > matrixM[c][r])
                traceBack = traceBackIQuery;

            loop:
            while (true) {
                int refIndex = r + c + refOffset;

                switch (traceBack[c][r]) {
                    case DONE:
                        startQuery = c;
                        startReference = r + c + refOffset + 1;
                        break loop;
                    case M_FROM_M:
                        queryTrack[length] = query[c - 1];
                        referenceTrack[length] = reference[refIndex];
                        if (queryTrack[length] == referenceTrack[length]) {
                            if (isDNAAlignment)
                                midTrack[length] = '|';
                            else
                                midTrack[length] = queryTrack[length];
                            identities++;
                        } else {
                            if (isDNAAlignment || scoringMatrix[queryTrack[length]][referenceTrack[length]] <= 0)
                                midTrack[length] = ' ';
                            else
                                midTrack[length] = '+';
                            mismatches++;
                        }
                        c--;
                        traceBack = traceBackM;
                        break;
                    case M_FROM_IRef:
                        queryTrack[length] = query[c - 1];
                        referenceTrack[length] = reference[refIndex];
                        if (queryTrack[length] == referenceTrack[length]) {
                            if (isDNAAlignment)
                                midTrack[length] = '|';
                            else
                                midTrack[length] = queryTrack[length];
                            identities++;
                        } else {
                            if (isDNAAlignment || scoringMatrix[queryTrack[length]][referenceTrack[length]] <= 0)
                                midTrack[length] = ' ';
                            else
                                midTrack[length] = '+';
                        }
                        c--;
                        traceBack = traceBackIRef;
                        break;
                    case M_FROM_IQuery:
                        queryTrack[length] = query[c - 1];
                        referenceTrack[length] = reference[refIndex];
                        if (queryTrack[length] == referenceTrack[length]) {
                            if (isDNAAlignment)
                                midTrack[length] = '|';
                            else
                                midTrack[length] = queryTrack[length];
                            identities++;
                        } else {
                            if (isDNAAlignment || scoringMatrix[queryTrack[length]][referenceTrack[length]] <= 0)
                                midTrack[length] = ' ';
                            else
                                midTrack[length] = '+';
                        }
                        c--;
                        traceBack = traceBackIQuery;
                        break;
                    case IRef_FROM_M:
                        queryTrack[length] = '-';
                        referenceTrack[length] = reference[refIndex];
                        midTrack[length] = ' ';
                        r--;
                        traceBack = traceBackM;
                        gaps++;
                        gapOpens++;
                        break;
                    case IRef_FROM_IRef:
                        queryTrack[length] = '-';
                        referenceTrack[length] = reference[refIndex];
                        midTrack[length] = ' ';
                        r--;
                        traceBack = traceBackIRef;
                        gaps++;
                        break;
                    case IQuery_FROM_M:
                        queryTrack[length] = query[c - 1];
                        referenceTrack[length] = '-';
                        midTrack[length] = ' ';
                        c--;
                        r++;
                        traceBack = traceBackM;
                        gaps++;
                        gapOpens++;
                        break;
                    case IQuery_FROM_IQuery:
                        queryTrack[length] = query[c - 1];
                        referenceTrack[length] = '-';
                        midTrack[length] = ' ';
                        c--;
                        r++;
                        traceBack = traceBackIQuery;
                        gaps++;
                        break;
                    default:
                        throw new RuntimeException("Undefined trace-back state: " + traceBack[c][r]);
                }
                if (queryTrack[length] == '-' && referenceTrack[length] == '-')
                    System.err.println("gap-gap at: " + length);

                if (++length >= queryTrack.length) {
                    queryTrack = grow(queryTrack);
                    midTrack = grow(midTrack);
                    referenceTrack = grow(referenceTrack);
                }
            } // end of loop

            reverseInPlace(queryTrack, length);
            reverseInPlace(midTrack, length);
            reverseInPlace(referenceTrack, length);
        }

        // get second part of alignment:
        {
            for (int i = 1; i < seedLength - 1; i++) {
                queryTrack[length] = query[queryPos + i];
                referenceTrack[length] = reference[refPos + i];
                if (queryTrack[length] == referenceTrack[length]) {
                    if (isDNAAlignment)
                        midTrack[length] = '|';
                    else
                        midTrack[length] = queryTrack[length];
                    identities++;
                } else {
                    if (isDNAAlignment || scoringMatrix[queryTrack[length]][referenceTrack[length]] <= 0)
                        midTrack[length] = ' ';
                    else
                        midTrack[length] = '+';
                    mismatches++;
                }
                if (++length >= queryTrack.length) {
                    queryTrack = grow(queryTrack);
                    midTrack = grow(midTrack);
                    referenceTrack = grow(referenceTrack);
                }
            }
        }


        // get third part of alignment:
        {
            int r = middleRow;
            int c = queryPos + seedLength; // +1 because col=pos+1, but -1 because want to be in last position of seed

            byte[][] traceBack;
            traceBack = traceBackM;
            if (matrixIRef[c][r] > matrixM[c][r]) {
                traceBack = traceBackIRef;
                if (matrixIQuery[c][r] > matrixIRef[c][r])
                    traceBack = traceBackIQuery;
            } else if (matrixIQuery[c][r] > matrixM[c][r])
                traceBack = traceBackIQuery;

            loop:
            while (true) {
                int refIndex = r + c + refOffset;

                switch (traceBack[c][r]) {
                    case DONE:
                        endQuery = c - 1;
                        endReference = r + c + refOffset + 1;
                        break loop;
                    case M_FROM_M:
                        queryTrack[length] = query[c - 1];
                        referenceTrack[length] = reference[refIndex];
                        if (queryTrack[length] == referenceTrack[length]) {
                            if (isDNAAlignment)
                                midTrack[length] = '|';
                            else
                                midTrack[length] = queryTrack[length];
                            identities++;
                        } else {
                            if (isDNAAlignment || scoringMatrix[queryTrack[length]][referenceTrack[length]] <= 0)
                                midTrack[length] = ' ';
                            else
                                midTrack[length] = '+';
                            mismatches++;
                        }
                        c++;
                        traceBack = traceBackM;
                        break;
                    case M_FROM_IRef:
                        queryTrack[length] = query[c - 1];
                        referenceTrack[length] = reference[refIndex];
                        if (queryTrack[length] == referenceTrack[length]) {
                            if (isDNAAlignment)
                                midTrack[length] = '|';
                            else
                                midTrack[length] = queryTrack[length];
                            identities++;
                        } else {
                            if (isDNAAlignment || scoringMatrix[queryTrack[length]][referenceTrack[length]] <= 0)
                                midTrack[length] = ' ';
                            else
                                midTrack[length] = '+';
                        }
                        c++;
                        traceBack = traceBackIRef;
                        break;
                    case M_FROM_IQuery:
                        queryTrack[length] = query[c - 1];
                        referenceTrack[length] = reference[refIndex];
                        if (queryTrack[length] == referenceTrack[length]) {
                            if (isDNAAlignment)
                                midTrack[length] = '|';
                            else
                                midTrack[length] = queryTrack[length];
                            identities++;
                        } else {
                            if (isDNAAlignment || scoringMatrix[queryTrack[length]][referenceTrack[length]] <= 0)
                                midTrack[length] = ' ';
                            else
                                midTrack[length] = '+';
                        }
                        c++;
                        traceBack = traceBackIQuery;
                        break;
                    case IRef_FROM_M:
                        queryTrack[length] = '-';
                        referenceTrack[length] = reference[refIndex];
                        midTrack[length] = ' ';
                        r++;
                        traceBack = traceBackM;
                        gaps++;
                        gapOpens++;
                        break;
                    case IRef_FROM_IRef:
                        queryTrack[length] = '-';
                        referenceTrack[length] = reference[refIndex];
                        midTrack[length] = ' ';
                        r++;
                        traceBack = traceBackIRef;
                        gaps++;
                        break;
                    case IQuery_FROM_M:
                        queryTrack[length] = query[c - 1];
                        referenceTrack[length] = '-';
                        midTrack[length] = ' ';
                        c++;
                        r--;
                        traceBack = traceBackM;
                        gaps++;
                        gapOpens++;
                        break;
                    case IQuery_FROM_IQuery:
                        queryTrack[length] = query[c - 1];
                        referenceTrack[length] = '-';
                        midTrack[length] = ' ';
                        c++;
                        r--;
                        traceBack = traceBackIQuery;
                        gaps++;
                        break;
                    default: {
                        throw new RuntimeException("Undefined trace-back state: " + traceBack[c][r]);
                    }
                }
                if (queryTrack[length] == '-' && referenceTrack[length] == '-')
                    System.err.println("gap-gap at: " + length);

                if (++length >= queryTrack.length) {
                    queryTrack = grow(queryTrack);
                    midTrack = grow(midTrack);
                    referenceTrack = grow(referenceTrack);
                }
            } // end of loop
        }

        alignmentLength = length;
        alignment = new byte[][]{copy(queryTrack, length), copy(midTrack, length), copy(referenceTrack, length)};
    }

    public int getStartQuery() {
        return startQuery;
    }

    public int getEndQuery() {
        return endQuery;
    }

    public int getStartReference() {
        return startReference;
    }

    public int getEndReference() {
        return endReference;
    }

    public int getGaps() {
        return gaps;
    }

    public int getGapOpens() {
        return gapOpens;
    }

    private int getIdentities() {
        return identities;
    }

    public float getPercentIdentity() {
        if (alignment == null)
            computeAlignmentByTraceBack();
        return getAlignmentLength() == 0 ? 0 : (float) (100 * getIdentities()) / (float) getAlignmentLength();
    }

    public int getMismatches() {
        return mismatches;
    }

    public int getRawScore() {
        return rawScore;
    }

    public float getBitScore() {
        return bitScore;
    }

    public double getExpected() {
        return expected;
    }

    public int getAlignmentLength() {
        return alignmentLength;
    }

    public long getReferenceDatabaseLength() {
        return referenceDatabaseLength;
    }

    public void setReferenceDatabaseLength(long referenceDatabaseLength) {
        this.referenceDatabaseLength = referenceDatabaseLength;
    }

    /**
     * reverse bytes
     *
     * @param array
     * @return reversed bytes
     */
    private void reverseInPlace(byte[] array, int length) {
        int top = length / 2;
        for (int i = 0; i < top; i++) {
            byte tmp = array[i];
            int j = length - i - 1;
            array[i] = array[j];
            array[j] = tmp;
        }
    }

    /**
     * grow an array
     *
     * @param a
     * @return larger array containing values
     */
    private byte[] grow(byte[] a) {
        byte[] result = new byte[Math.max(2, 2 * a.length)];
        System.arraycopy(a, 0, result, 0, a.length);
        return result;
    }

    /**
     * return a copy
     *
     * @param array
     * @param length
     * @return copy
     */
    private byte[] copy(byte[] array, int length) {
        byte[] result = new byte[length];
        System.arraycopy(array, 0, result, 0, length);
        return result;
    }

    /**
     * return a reverse copy
     *
     * @param array
     * @param length
     * @return copy
     */
    public byte[] copyReverse(byte[] array, int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++)
            result[i] = array[length - 1 - i];
        return result;
    }

    /**
     * to string
     *
     * @param colRowMatrix
     * @return
     */
    private String toString(int[][] colRowMatrix, int firstCol, int cols, byte[] query) {
        StringBuilder buf = new StringBuilder();

        buf.append("   |");
        for (int i = firstCol; i < cols; i++) {
            buf.append(String.format(" %3d", i));
        }
        buf.append("\n");
        buf.append("   |    ");
        for (int i = firstCol + 1; i < cols; i++) {
            buf.append("   ").append((char) query[i - 1]);
        }
        buf.append("\n");
        buf.append("---+");
        buf.append("----".repeat(Math.max(0, cols - firstCol)));
        buf.append("\n");


        int r = 0;
        boolean hasRow = true;
        while (hasRow) {
            hasRow = false;
            for (int i = firstCol; i < cols; i++) {
                int[] aColRowMatrix = colRowMatrix[i];
                if (aColRowMatrix.length > r) {
                    if (!hasRow) {
                        hasRow = true;
                        buf.append(String.format("%2d |", r));
                    }
                    int value = aColRowMatrix[r];
                    if (value <= MINUS_INFINITY)
                        buf.append(" -oo");
                    else
                        buf.append(String.format(" %3d", value));
                }
            }
            buf.append("\n");
            r++;
        }
        return buf.toString();
    }

    /**
     * maps a bit score to a raw score
     *
     * @param bitScore
     * @return raw score
     */
    public int getRawScoreForBitScore(double bitScore) {
        return (int) Math.floor((LN_2 * bitScore + lnK) / lambda);
    }

    private static final int minNumberOfExactMatches = 10;
    private static final int windowForMinNumberOfExactMatches = 30;

    /**
     * heuristically check whether there is going to be a good alignment
     *
     * @param query
     * @param reference
     * @param queryPos
     * @param refPos
     * @return true, if good alignment is likely
     */
    public boolean quickCheck(final byte[] query, final int queryLength, final byte[] reference, final int referenceLength, final int queryPos, final int refPos) {
        if (mode == BlastMode.BlastN)
            return true;

        if (queryPos + minNumberOfExactMatches >= queryLength || refPos + minNumberOfExactMatches >= referenceLength)
            return false;

        int count = 0;
        final int maxSteps = Math.min(windowForMinNumberOfExactMatches, Math.min(queryLength - queryPos, referenceLength - refPos));
        for (int i = 0; i < maxSteps; i++) {
            if (query[queryPos + i] == reference[refPos + i]) {
                count++;
                if (count == minNumberOfExactMatches)
                    return true;
            }
        }
        return false;
    }
}
