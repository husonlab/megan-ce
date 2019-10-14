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

package megan.daa.io;

import static megan.daa.io.Translator.FORWARD_SHIFT_CODE;
import static megan.daa.io.Translator.REVERSE_SHIFT_CODE;

/**
 * generates SAM lines
 * Daniel Huson, 8.2015
 */
public class SAMUtilities {
    private static final String FILE_HEADER_BLASTN_TEMPLATE = "@HD\tVN:1.5\tSO:unsorted\tGO:query\n@PG\tID:1\tPN:MEGAN\tCL:%s\tDS:BlastN\n@RG\tID:1\tPL:unknown\tSM:unknown\n@CO\tBlastN-like alignments\n" +
            "@CO\tReporting AS: bitScore, ZR: rawScore, ZE: expected, ZI: percent identity, ZL: reference length\n";
    private static final String FILE_HEADER_BLASTP_TEMPLATE = "@HD\tVN:1.5\tSO:unsorted\tGO:query\n@PG\tID:1\tPN:MEGAN\tCL:%s\tDS:BlastP\n@RG\tID:1\tPL:unknown\tSM:unknown\n@CO\tBlastP-like alignments\n" +
            "@CO\tReporting AS: bitScore, ZR: rawScore, ZE: expected, ZI: percent identity, ZL: reference length\n";
    private static final String FILE_HEADER_BLASTX_TEMPLATE = "@HD\tVN:1.5\tSO:unsorted\tGO:query\n@PG\tID:1\tPN:MEGAN\tCL:%s\tDS:BlastX\n@RG\tID:1\tPL:unknown\tSM:unknown\n@CO\tBlastX-like alignments\n" +
            "@CO\tReporting AS: bitScore, ZR: rawScore, ZE: expected, ZI: percent identity, ZL: reference length, ZF: frame, ZS: query start DNA coordinate\n";

    private static final int[] mapDaaOpCode2CigarOpCode = {0, 1, 2, 0}; // op_match -> M, op_insertion-> I, op_deletion-> D, op_substitution -> M
    private static final char[] daaOpCode2CigarLetter = {'M', 'I', 'D'};

    private static final CombinedOperation insertOperation = new CombinedOperation(PackedTranscript.EditOperation.op_insertion, 1, null);

    /**
     * create a sam line
     *
     * @param matchRecord
     * @param buffer
     */
    public static void createSAM(DAAParser daaParser, DAAMatchRecord matchRecord, ByteOutputBuffer buffer, byte[] queryAlphabet) {
        buffer.write(matchRecord.getQueryRecord().getQueryName());
        buffer.writeString("\t0\t");
        buffer.write(matchRecord.getSubjectName());
        buffer.writeString(String.format("\t%d\t255\t", matchRecord.getSubjectBegin() + 1));

        final byte[][] cigarAndAlignedQueryAndMD;
        if (matchRecord.getFrameShiftAdjustmentForBlastXMode() != 0) {
            final int queryBegin = matchRecord.getQueryBegin();
            final int queryEnd = matchRecord.getQueryEnd(); // query end position, in the case of BlastX, not taking frame shifts into account
            int start;
            byte[] querySequence;
            if (queryBegin < queryEnd) { // positive frame
                querySequence = matchRecord.getQueryRecord().getSourceSequence();
                start = queryBegin;
            } else { // negative frame
                start = 0;
                int length = queryBegin - queryEnd + 1;
                querySequence = Translator.getReverseComplement(matchRecord.getQueryRecord().getSourceSequence(), queryEnd, length);
            }
            cigarAndAlignedQueryAndMD = computeCigarAndAlignedQueryAndMD(querySequence, start, queryAlphabet, matchRecord.getTranscript());
        } else
            cigarAndAlignedQueryAndMD = null;

        if (cigarAndAlignedQueryAndMD == null)
            writeCigar(matchRecord, buffer);
        else
            buffer.write(cigarAndAlignedQueryAndMD[0]);

        buffer.writeString("\t*\t0\t0\t");
        if (cigarAndAlignedQueryAndMD == null) {
            buffer.write(Translator.translate(matchRecord.getQuery(), queryAlphabet, matchRecord.getTranslatedQueryBegin(), matchRecord.getTranslatedQueryLen()));
        } else {
            buffer.write(cigarAndAlignedQueryAndMD[1]);
        }
        buffer.writeString("\t*\t");

        float bitScore = daaParser.getHeader().computeAlignmentBitScore(matchRecord.getScore());
        float evalue = daaParser.getHeader().computeAlignmentExpected(matchRecord.getQuery().length, matchRecord.getScore());
        int percentIdentity = Utilities.computePercentIdentity(matchRecord);
        int blastFrame = computeBlastFrame(matchRecord.getFrame());
        buffer.writeString(
                String.format("AS:i:%d\tNM:i:%d\tZL:i:%d\tZR:i:%d\tZE:f:%.1e\tZI:i:%d\tZF:i:%d\tZS:i:%d\tMD:Z:",
                        (int) bitScore, matchRecord.getLen() - matchRecord.getIdentities(), matchRecord.getTotalSubjectLen(), matchRecord.getScore(),
                        evalue, percentIdentity, blastFrame, matchRecord.getQueryBegin() + (matchRecord.getQueryBegin() < matchRecord.getQueryEnd() ? 1 : 1)));
        if (cigarAndAlignedQueryAndMD == null)
            writeMD(matchRecord, buffer, queryAlphabet);
        else
            buffer.write(cigarAndAlignedQueryAndMD[2]);
        buffer.write((byte) '\n');
    }


    /**
     * compute the aligned query string, cigar and MD string
     *
     * @param queryDNA
     * @param queryAlphabet
     * @param editTranscript
     * @return alignment, cigar and MD string
     */
    private static byte[][] computeCigarAndAlignedQueryAndMD(byte[] queryDNA, int start, byte[] queryAlphabet, PackedTranscript editTranscript) {
        final ByteOutputBuffer alignedQueryBuf = new ByteOutputBuffer();
        //final ByteOutputBuffer alignedReferenceBuf=new ByteOutputBuffer();
        final ByteOutputBuffer cigarBuf = new ByteOutputBuffer();
        final ByteOutputBuffer mdBuf = new ByteOutputBuffer();

        // used in computation of cigar:
        int previousCount = 0, previousOp = 0;

        // used in computation of MD string:
        int matches = 0, del = 0;

        // current query position
        int queryPosition = start;

        for (CombinedOperation editOp : editTranscript.gather()) {
            final CombinedOperation editOpCorrected;
            // compute sequence:
            switch (editOp.getEditOperation()) {
                case op_match: {
                    for (int i = 0; i < editOp.getCount(); i++) {
                        byte aa = queryAlphabet[Translator.getAminoAcid(queryDNA, queryPosition)];
                        alignedQueryBuf.write(aa);
                        //alignedReferenceBuf.write(aa);
                        queryPosition += 3;
                    }
                    editOpCorrected = editOp;
                    break;
                }
                case op_insertion: {
                    for (int i = 0; i < editOp.getCount(); i++) {
                        byte aa = queryAlphabet[Translator.getAminoAcid(queryDNA, queryPosition)];
                        alignedQueryBuf.write(aa);
                        //alignedReferenceBuf.write((byte)'-');
                        queryPosition += 3;
                    }
                    editOpCorrected = editOp;
                    break;
                }
                case op_deletion: {
                    byte c = queryAlphabet[editOp.getLetter()];
                    //alignedQueryBuf.write((byte)'-');
                    //alignedReferenceBuf.write(c);
                    editOpCorrected = editOp;
                    break;
                }
                // Key idea here: Although a frame shift in the query is really an insertion in the query,
                // in a DAA file it is represented as a substitution by a / or \ in the reference, due to limitations due to the bit packed transcript encoding
                case op_substitution: {
                    byte c = queryAlphabet[editOp.getLetter()];
                    if (c == '/') { // reverse shift
                        alignedQueryBuf.write(c);
                        //alignedReferenceBuf.write((byte)'-');
                        queryPosition -= 1;
                        editOpCorrected = insertOperation;
                    } else if (c == '\\') {  // forward shift
                        alignedQueryBuf.write(c);
                        //alignedReferenceBuf.write((byte)'-');
                        queryPosition += 1;
                        editOpCorrected = insertOperation;
                    } else {
                        byte aa = queryAlphabet[Translator.getAminoAcid(queryDNA, queryPosition)];
                        alignedQueryBuf.write(aa);
                        //alignedReferenceBuf.write(c);
                        queryPosition += 3;
                        editOpCorrected = editOp;
                    }
                    break;
                }
                default:
                    throw new RuntimeException("this should't happen");
            }
            // compute cigar:
            if (mapDaaOpCode2CigarOpCode[editOpCorrected.getOpCode()] == previousOp)
                previousCount += editOpCorrected.getCount();
            else {
                if (previousCount > 0)
                    cigarBuf.writeString(String.format("%d", previousCount));
                cigarBuf.write((byte) daaOpCode2CigarLetter[previousOp]);
                previousCount = editOpCorrected.getCount();
                previousOp = mapDaaOpCode2CigarOpCode[editOpCorrected.getOpCode()];
            }
            // compute md:
            switch (editOpCorrected.getEditOperation()) {
                case op_match:
                    del = 0;
                    matches += editOpCorrected.getCount();
                    break;
                case op_insertion:
                    break;
                case op_substitution:
                    if (matches > 0) {
                        mdBuf.writeString(String.format("%d", matches));
                        matches = 0;
                    } else if (del > 0) {
                        mdBuf.write((byte) '0');
                        del = 0;
                    }
                    mdBuf.write(queryAlphabet[editOpCorrected.getLetter()]);
                    break;
                case op_deletion:
                    if (matches > 0) {
                        mdBuf.writeString(String.format("%d", matches));
                        matches = 0;
                    }
                    if (del == 0)
                        mdBuf.write((byte) '^');
                    mdBuf.write(queryAlphabet[editOpCorrected.getLetter()]);
                    ++del;
            }
        }
        if (previousCount > 0) { // finish cigar
            cigarBuf.writeString(String.format("%d", previousCount));
            cigarBuf.write((byte) daaOpCode2CigarLetter[previousOp]);
        }
        if (matches > 0) // finish md
            mdBuf.writeString(String.format("%d", matches));
        return new byte[][]{cigarBuf.copyBytes(), alignedQueryBuf.copyBytes(), mdBuf.copyBytes()};
    }

    /**
     * compute the aligned query length correction required to accommodate frame shifts
     *
     * @param transcript
     * @return frame shift correction
     */
    private static int computeFrameShiftCorrection(PackedTranscript transcript) {
        int frameShiftCorrection = 0;

        for (CombinedOperation editOp : transcript.gather()) {
            // compute sequence:
            if (editOp.getEditOperation() == PackedTranscript.EditOperation.op_substitution) {
                if (editOp.getLetter() == REVERSE_SHIFT_CODE) {
                    frameShiftCorrection += 4;
                } else if (editOp.getLetter() == FORWARD_SHIFT_CODE) {
                    frameShiftCorrection += 2;
                }
            }
        }
        return frameShiftCorrection;
    }

    /**
     * write the cigar string
     *
     * @param match
     * @param buffer
     */
    private static void writeCigar(DAAMatchRecord match, ByteOutputBuffer buffer) {
        int previousCount = 0, previousOp = 0;

        for (final CombinedOperation cop : match.getTranscript().gather()) {
            int opCode = cop.getOpCode();

            if (mapDaaOpCode2CigarOpCode[opCode] == previousOp)
                previousCount += cop.getCount();
            else {
                if (previousCount > 0)
                    buffer.writeString(String.format("%d", previousCount));
                buffer.write((byte) daaOpCode2CigarLetter[previousOp]);
                previousCount = cop.getCount();
                previousOp = mapDaaOpCode2CigarOpCode[opCode];
            }
        }
        if (previousCount > 0) {
            buffer.writeString(String.format("%d", previousCount));
            buffer.write((byte) daaOpCode2CigarLetter[previousOp]);
        }
    }

    /**
     * write the MD string
     *
     * @param match
     * @param buffer
     * @param queryAlphabet
     */
    private static void writeMD(DAAMatchRecord match, ByteOutputBuffer buffer, byte[] queryAlphabet) {
        {
            int matches = 0, del = 0;
            for (CombinedOperation editOp : match.getTranscript().gather()) {
                switch (editOp.getEditOperation()) {
                    case op_match:
                        del = 0;
                        matches += editOp.getCount();
                        break;
                    case op_insertion:
                        break;
                    case op_substitution:
                        if (matches > 0) {
                            buffer.writeString(String.format("%d", matches));
                            matches = 0;
                        } else if (del > 0) {
                            buffer.write((byte) '0');
                            del = 0;
                        }
                        buffer.write(queryAlphabet[editOp.getLetter()]);
                        break;
                    case op_deletion:
                        if (matches > 0) {
                            buffer.writeString(String.format("%d", matches));
                            matches = 0;
                        }
                        if (del == 0)
                            buffer.write((byte) '^');
                        buffer.write(queryAlphabet[editOp.getLetter()]);
                        ++del;
                }
            }
            if (matches > 0)
                buffer.writeString(String.format("%d", matches));
        }
    }

    /**
     * compute the BLAST frame
     *
     * @param frame (in range 0-5)
     * @return BLAST frame (in range -2 to 2)
     */
    private static int computeBlastFrame(int frame) {
        return frame <= 2 ? frame + 1 : 2 - frame;
    }
}
