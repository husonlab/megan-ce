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

import java.io.IOException;

/**
 * match record
 * daniel Huson, 8.2105
 */
public class DAAMatchRecord {
    private final DAAQueryRecord queryRecord;
    private final DAAParser daaParser;
    private final DAAHeader daaHeader;

    private int subjectId, totalSubjectLen, score, queryBegin, subjectBegin, frame, translatedQueryBegin, translatedQueryLen, subjectLen, len, identities, mismatches, gapOpenings;
    private int frameShiftAdjustmentForBlastXMode; // added to accommodate frame shift counts in DAA files generated from MAF files

    private byte[] subjectName;

    private final PackedTranscript transcript = new PackedTranscript();

    /**
     * constructor
     *
     * @param queryRecord
     */
    public DAAMatchRecord(DAAQueryRecord queryRecord) {
        this.queryRecord = queryRecord;
        this.daaParser = queryRecord.getDaaParser();
        this.daaHeader = daaParser.getHeader();
    }

    /**
     * parse from buffer
     *
     * @param buffer
     * @param refIns input stream to read reference sequences
     * @return new position
     */
    public void parseBuffer(ByteInputBuffer buffer, InputReaderLittleEndian refIns) throws IOException {
        subjectId = buffer.readIntLittleEndian();
        int flag = buffer.read();
        score = buffer.readPacked(flag & 3);
        queryBegin = buffer.readPacked((flag >>> 2) & 3);
        subjectBegin = buffer.readPacked((flag >>> 4) & 3);
        transcript.read(buffer);
        if (refIns != null)
            subjectName = daaHeader.getReference(subjectId, refIns);
        else
            subjectName = "unknown".getBytes();

        totalSubjectLen = daaHeader.getRefLength(subjectId);

        switch (daaHeader.getAlignMode()) {
            case blastx: {
                frame = (flag & (1 << 6)) == 0 ? queryBegin % 3 : 3 + (queryRecord.getSourceSequence().length - 1 - queryBegin) % 3;
                translatedQueryBegin = getQueryTranslatedBegin(queryBegin, frame, queryRecord.getSourceSequence().length, true);
                break;
            }
            case blastp: {
                frame = 0;
                translatedQueryBegin = queryBegin;
                break;
            }
            default:
            case blastn: {
                frame = (flag & (1 << 6)) == 0 ? 0 : 1;
                translatedQueryBegin = getQueryTranslatedBegin(queryBegin, frame, queryRecord.getSourceSequence().length);
            }

        }
        parseTranscript(transcript);
    }

    /**
     * parse the transcript.
     * Sets all these variables:
     * translatedQueryLen
     * frameShiftAdjustmentForBlastXMode
     * subjectLen
     * identities
     * mismatches
     * gapOpeningS
     *
     * @param transcript
     */
    private void parseTranscript(PackedTranscript transcript) {
        translatedQueryLen = 0;
        frameShiftAdjustmentForBlastXMode = 0;
        subjectLen = 0;
        len = 0;
        identities = 0;
        mismatches = 0;
        gapOpenings = 0;

        int d = 0;
        for (CombinedOperation op : transcript.gather()) {
            int count = op.getCount();
            len += count;

            switch (op.getEditOperation()) {
                case op_match:
                    identities += count;
                    translatedQueryLen += count;
                    subjectLen += count;
                    d = 0;
                    break;
                case op_substitution:

                    byte c = daaParser.getAlignmentAlphabet()[op.getLetter()];
                    if (c == '/') { // reverse shift
                        frameShiftAdjustmentForBlastXMode -= 4; // minus 1 for frame shift and 3 for translatedQueryLen increment

                    } else if (c == '\\') {  // forward shift
                        frameShiftAdjustmentForBlastXMode -= 2; // plus 1 for frame shift and 3 for translatedQueryLen increment
                    }
                    translatedQueryLen += count;
                    subjectLen += count;
                    mismatches += count;

                    d = 0;
                    break;
                case op_insertion:
                    translatedQueryLen += count;
                    ++gapOpenings;
                    d = 0;
                    break;
                case op_deletion:
                    subjectLen += count;
                    if (d == 0)
                        ++gapOpenings;
                    d += count;
            }
        }
    }

    private int getQueryTranslatedBegin(int query_begin, int frame, int dna_len) { // this needs testing
        if (frame == 0)
            return query_begin;
        else
            return dna_len - query_begin - 1;
    }

    private int getQueryTranslatedBegin(int query_begin, int frame, int dna_len, boolean query_translated) {
        if (!query_translated)
            return query_begin;
        int f = frame <= 2 ? frame + 1 : 2 - frame;
        if (f > 0)
            return (query_begin - (f - 1)) / 3;
        else
            return (dna_len + f - query_begin) / 3;
    }

    /**
     * gets the end of the query
     *
     * @return query end
     */
    public int getQueryEnd() {
        switch (daaHeader.getAlignMode()) {
            case blastp: {
                return queryBegin + translatedQueryLen - 1;
            }
            case blastx: {
                final int len;
                if (frame > 2) {
                    len = -(3 * translatedQueryLen + frameShiftAdjustmentForBlastXMode);
                } else
                    len = 3 * translatedQueryLen + frameShiftAdjustmentForBlastXMode;
                //System.err.println("queryEnd: "+queryEnd);
                return queryBegin + (len > 0 ? -1 : 1) + len;
            }
            case blastn: {
                int len = translatedQueryLen * (frame > 0 ? -1 : 1);
                return queryBegin + (len > 0 ? -1 : 1) + len;
            }
            default:
                return 0;
        }
    }

    public DAAQueryRecord getQueryRecord() {
        return queryRecord;
    }

    public int getTotalSubjectLen() {
        return totalSubjectLen;
    }

    public int getScore() {
        return score;
    }

    public int getQueryBegin() {
        return queryBegin;
    }

    public int getSubjectBegin() {
        return subjectBegin;
    }

    public int getFrame() {
        return frame;
    }

    public int getTranslatedQueryBegin() {
        return translatedQueryBegin;
    }

    public int getTranslatedQueryLen() {
        return translatedQueryLen;
    }

    public int getSubjectLen() {
        return subjectLen;
    }

    public int getLen() {
        return len;
    }

    public int getIdentities() {
        return identities;
    }

    public int getMismatches() {
        return mismatches;
    }

    public int getGapOpenings() {
        return gapOpenings;
    }

    public byte[] getSubjectName() {
        return subjectName;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public PackedTranscript getTranscript() {
        return transcript;
    }

    public byte[] getQuery() {
        return queryRecord.getContext()[frame];
    }

    public int getFrameShiftAdjustmentForBlastXMode() {
        return frameShiftAdjustmentForBlastXMode;
    }
}
