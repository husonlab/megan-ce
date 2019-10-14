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
package megan.parsers.sam;

/**
 * Class that encapsulates a validation error message as well as a type code so that
 * errors can be aggregated by type.
 *
 * @author Doug Voet
 */
public class SAMValidationError {
    public enum Severity {
        WARNING, ERROR
    }

    public enum Type {
        /**
         * proper pair flag set for unpaired read
         */
        INVALID_FLAG_PROPER_PAIR,

        /**
         * mate unmapped flag set when mate is mapped or not set when mate is not mapped
         */
        INVALID_FLAG_MATE_UNMAPPED,

        /**
         * mate unmapped flag does not match read unmapped flag of mate
         */
        MISMATCH_FLAG_MATE_UNMAPPED,

        /**
         * mate negative strand flag set for unpaired read
         */
        INVALID_FLAG_MATE_NEG_STRAND,

        /**
         * mate negative strand flag does not match read negative strand flag of mate
         */
        MISMATCH_FLAG_MATE_NEG_STRAND,

        /**
         * first of pair flag set for unpaired read
         */
        INVALID_FLAG_FIRST_OF_PAIR,

        /**
         * second of pair flag set for unpaired read
         */
        INVALID_FLAG_SECOND_OF_PAIR,

        /**
         * pair flag set but not marked as first or second of pair
         */
        PAIRED_READ_NOT_MARKED_AS_FIRST_OR_SECOND(Severity.WARNING),

        /**
         * not primary alignment flag set for unmapped read
         */
        INVALID_FLAG_NOT_PRIM_ALIGNMENT,

        /**
         * mapped read flat not set for mapped read
         */
        INVALID_FLAG_READ_UNMAPPED,

        /**
         * inferred insert size is out of range
         */
        INVALID_INSERT_SIZE,

        /**
         * mapping quality set for unmapped read or is >= 256
         */
        INVALID_MAPPING_QUALITY,

        /**
         * CIGAR string is empty for mapped read or not empty of unmapped read, or other CIGAR badness.
         */
        INVALID_CIGAR,

        /**
         * CIGAR string contains I followed by D, or vice versa, with no intervening M
         */
        ADJACENCT_INDEL_IN_CIGAR(Severity.WARNING),

        /**
         * mate reference index (MRNM) set for unpaired read
         */
        INVALID_MATE_REF_INDEX,

        /**
         * mate reference index (MRNM) does not match reference index of mate
         */
        MISMATCH_MATE_REF_INDEX,

        /**
         * reference index not found in sequence dictionary
         */
        INVALID_REFERENCE_INDEX,

        /**
         * alignment start is can not be correct
         */
        INVALID_ALIGNMENT_START,

        /**
         * mate alignment does not match alignment start of mate
         */
        MISMATCH_MATE_ALIGNMENT_START,

        /**
         * the record's mate fields do not match the corresponding fields of the mate
         */
        MATE_FIELD_MISMATCH,

        /**
         * the NM tag (nucleotide differences) is incorrect
         */
        INVALID_TAG_NM,

        /**
         * the NM tag (nucleotide differences) is missing
         */
        MISSING_TAG_NM(Severity.WARNING),

        /**
         * the sam/bam file is missing the header
         */
        MISSING_HEADER,

        /**
         * there is no sequence dictionary in the header
         */
        MISSING_SEQUENCE_DICTIONARY,

        /**
         * the header is missing read group information
         */
        MISSING_READ_GROUP,

        /**
         * the record is out of order
         */
        RECORD_OUT_OF_ORDER,

        /**
         * A read group ID on a SAMRecord is not found in the header
         */
        READ_GROUP_NOT_FOUND,

        /**
         * Indexing bin set on SAMRecord does not agree with computed value.
         */
        INVALID_INDEXING_BIN,

        MISSING_VERSION_NUMBER,

        INVALID_VERSION_NUMBER,

        TRUNCATED_FILE,

        MISMATCH_READ_LENGTH_AND_QUALS_LENGTH,

        EMPTY_READ,

        /**
         * Bases corresponding to M operator in CIGAR are beyond the end of the reference.
         */
        CIGAR_MAPS_OFF_REFERENCE,

        /**
         * Length of E2 (secondary base calls) and U2 (secondary base quals) tag values should match read length
         */
        MISMATCH_READ_LENGTH_AND_E2_LENGTH,
        MISMATCH_READ_LENGTH_AND_U2_LENGTH,

        /**
         * Secondary base calls should not be the same as primary, unless one or the other is N
         */
        E2_BASE_EQUALS_PRIMARY_BASE(Severity.WARNING),

        /**
         * BAM appears to be healthy, but is an older file so doesn't have terminator block.
         */
        BAM_FILE_MISSING_TERMINATOR_BLOCK(Severity.WARNING),

        /**
         * Header record is not one of the standard types
         */
        UNRECOGNIZED_HEADER_TYPE,

        /**
         * Header tag does not have colon
         */
        POORLY_FORMATTED_HEADER_TAG,

        /**
         * Header tag appears more than once in header line with different value
         */
        HEADER_TAG_MULTIPLY_DEFINED,

        HEADER_RECORD_MISSING_REQUIRED_TAG,

        /**
         * Date string is not ISO-8601
         */
        INVALID_DATE_STRING(Severity.WARNING),

        /**
         * Unsigned integer tag value is deprecated in BAM.
         */
        TAG_VALUE_TOO_LARGE,

        /**
         * Invalide virtualFilePointer in index
         */
        INVALID_INDEX_FILE_POINTER,

        /**
         * PI tag value is not numeric.
         */
        INVALID_PREDICTED_MEDIAN_INSERT_SIZE,

        /**
         * Same read group id appears more than once
         */
        DUPLICATE_READ_GROUP_ID,

        /**
         * Same program group id appears more than once
         */
        DUPLICATE_PROGRAM_GROUP_ID,

        /**
         * Read is marked as paired, but its pair was not found.
         */
        MATE_NOT_FOUND;

        final Severity severity;

        Type() {
            this.severity = Severity.ERROR;
        }

        Type(final Severity severity) {
            this.severity = severity;
        }

        /**
         * @return Format for writing to histogram summary output.
         */
        public String getHistogramString() {
            return this.severity.name() + ":" + this.name();
        }
    }

    private final Type type;
    private final String message;
    private final String readName;
    private long recordNumber = -1;
    private String source;

    /**
     * Construct a SAMValidationError with unknown record number.
     *
     * @param type
     * @param message
     * @param readName May be null if readName is not known.
     */
    public SAMValidationError(final Type type, final String message, final String readName) {
        this.type = type;
        this.message = message;
        this.readName = readName;
    }

    /**
     * Construct a SAMValidationError with possibly-known record number.
     *
     * @param type
     * @param message
     * @param readName     May be null if readName is not known.
     * @param recordNumber Position of the record in the SAM file it has been read from.  -1 if not known.
     */
    public SAMValidationError(final Type type, final String message, final String readName, final long recordNumber) {
        this(type, message, readName);
        this.recordNumber = recordNumber;
    }

    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(type.severity.toString());
        builder.append(": ");
        if (source != null) {
            builder.append("File ").append(source).append(", ");
        }
        if (recordNumber > 0) {
            builder.append("Record ").append(recordNumber).append(", ");
        }
        if (readName != null) {
            builder.append("Read name ").append(readName).append(", ");
        }
        return builder.append(message).toString();
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    /**
     * may be null
     */
    public String getReadName() {
        return readName;
    }

    /**
     * 1-based.  -1 if not known.
     */
    public long getRecordNumber() {
        return recordNumber;
    }

    public void setRecordNumber(final long recordNumber) {
        this.recordNumber = recordNumber;
    }

    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }
}
