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

package megan.daa.io;

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

    private static final int map[] = {0, 1, 2, 0};
    private static final char letter[] = {'M', 'I', 'D'};

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

        writeCigar(matchRecord, buffer);

        buffer.writeString("\t*\t0\t0\t");
        buffer.write(Translator.translate(matchRecord.getQuery(), queryAlphabet, matchRecord.getTranslatedQueryBegin(), matchRecord.getTranslatedQueryLen()));
        buffer.writeString("\t*\t");

        float bitScore = daaParser.getHeader().computeAlignmentBitScore(matchRecord.getScore());
        float evalue = daaParser.getHeader().computeAlignmentExpected(matchRecord.getQuery().length, matchRecord.getScore());
        int percentIdentity = Utilities.computePercentIdentity(matchRecord);
        int blastFrame = computeBlastFrame(matchRecord.getFrame());
        buffer.writeString(
                String.format("AS:i:%d\tNM:i:%d\tZL:i:%d\tZR:i:%d\tZE:f:%.1e\tZI:i:%d\tZF:i:%d\tZS:i:%d\tMD:Z:",
                        (int) bitScore, matchRecord.getLen() - matchRecord.getIdentities(), matchRecord.getTotalSubjectLen(), matchRecord.getScore(),
                        evalue, percentIdentity, blastFrame, matchRecord.getQueryBegin() + 1));
        writeMD(matchRecord, buffer, queryAlphabet);
        buffer.write((byte) '\n');
    }

    /**
     * write the cigar string
     *
     * @param match
     * @param buffer
     */
    private static void writeCigar(DAAMatchRecord match, ByteOutputBuffer buffer) {
        int n = 0, op = 0;

        for (CombinedOperation cop : match.getTranscript().gather()) {
            if (map[cop.getOpCode()] == op)
                n += cop.getCount();
            else {
                if (n > 0)
                    buffer.writeString(String.format("%d", n));
                buffer.write((byte) letter[op]);
                n = cop.getCount();
                op = map[cop.getOpCode()];
            }
        }
        if (n > 0) {
            buffer.writeString(String.format("%d", n));
            buffer.write((byte) letter[op]);
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
            for (CombinedOperation cop : match.getTranscript().gather()) {
                switch (cop.getEditOperation()) {
                    case op_match:
                        del = 0;
                        matches += cop.getCount();
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
                        buffer.write(queryAlphabet[cop.getLetter()]);
                        break;
                    case op_deletion:
                        if (matches > 0) {
                            buffer.writeString(String.format("%d", matches));
                            matches = 0;
                        }
                        if (del == 0)
                            buffer.write((byte) '^');
                        buffer.write(queryAlphabet[cop.getLetter()]);
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
