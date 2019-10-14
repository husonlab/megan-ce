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

import jloda.util.Basic;
import megan.classification.Classification;
import megan.io.FileInputStreamAdapter;
import megan.io.FileRandomAccessReadOnlyAdapter;
import megan.io.FileRandomAccessReadWriteAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAA header block
 * Daniel Huson, 8.2015
 */
public class DAAHeader {
    public final static long MAGIC_NUMBER = 4327487858190246763L;
    public final static long DAA_VERSION = 1L; // changed from 0 to 1 on Jan-25, 2018
    public final static int MEGAN_VERSION = 6;

    private final String fileName;

    // header1
    private long magicNumber;
    private long version;

    // header2:
    private long diamondBuild;
    private long dbSeqs;
    private long dbSeqsUsed;
    private long dbLetters;
    private long flags;
    private long queryRecords;
    private int modeRank;
    private int gapOpen;
    private int gapExtend;
    private int reward;
    private int penalty;
    private int reserved1;
    private int reserved2;
    private int reserved3;

    private double k;
    private double lambda;
    private double reserved4;
    private double reserved5;
    private final byte[] scoreMatrix = new byte[16];
    private final long[] blockSize = new long[256];
    private final byte[] blockTypeRank = new byte[256];

    // references:
    private byte[][] references;
    private int[] refLengths;

    private final int referenceLocationChunkBits = 6; // 6 bits = 64 chunk size
    private final int referenceLocationChunkSize = 1 << referenceLocationChunkBits;
    private long[] referenceLocations; // location of every 2^referenceLocationChunkBits reference

    // ref annotations:
    private int numberOfRefAnnotations;
    private final int[][] refAnnotations = new int[256][];
    private final String[] refAnnotationNames = new String[256];
    private int refAnnotationIndexForTaxonomy = -1;

    // helper variables:
    private String scoreMatrixName;

    private long headerSize;

    private int refNamesBlockIndex = -1;
    private int refLengthsBlockIndex = -1;
    private int alignmentsBlockIndex = -1;

    private double lnK;

    /**
     * constructor
     *
     * @param fileName
     */
    public DAAHeader(String fileName) {
        this.fileName = fileName;
    }

    /**
     * constructor
     *
     * @param fileName
     */
    public DAAHeader(String fileName, boolean load) throws IOException {
        this.fileName = fileName;
        if (load)
            load();
    }

    /**
     * read the header of a DAA file and all reference names
     *
     * @throws IOException
     */
    public void load() throws IOException {
        if (magicNumber == 0) {
            //System.err.println("Loading DAA header...");
            try (InputReaderLittleEndian ins = new InputReaderLittleEndian(new FileInputStreamAdapter(fileName))) {
                magicNumber = ins.readLong();
                if (magicNumber != MAGIC_NUMBER)
                    throw new IOException("Input file is not a DAA file.");
                version = ins.readLong();
                if (version > DAA_VERSION)
                    throw new IOException("DAA version not supported by this version of MEGAN.");

                diamondBuild = ins.readLong();
                dbSeqs = ins.readLong();
                dbSeqsUsed = ins.readLong();
                dbLetters = ins.readLong();
                flags = ins.readLong();
                queryRecords = ins.readLong();

                modeRank = ins.readInt();
                gapOpen = ins.readInt();
                gapExtend = ins.readInt();
                reward = ins.readInt();
                penalty = ins.readInt();
                reserved1 = ins.readInt();
                reserved2 = ins.readInt();
                reserved3 = ins.readInt();

                k = ins.readDouble();
                lambda = ins.readDouble();
                reserved4 = ins.readDouble();
                reserved5 = ins.readDouble();

                for (int i = 0; i < scoreMatrix.length; i++) {
                    scoreMatrix[i] = (byte) ins.read();
                }
                scoreMatrixName = Basic.toString(scoreMatrix);

                for (int i = 0; i < blockSize.length; i++)
                    blockSize[i] = ins.readLong();

                if (blockSize[0] == 0)
                    throw new IOException("Invalid DAA file. DIAMOND run probably has not completed successfully.");
                for (int i = 0; i < blockTypeRank.length; i++) {
                    blockTypeRank[i] = (byte) ins.read();
                    switch (BlockType.value(blockTypeRank[i])) {
                        case ref_names:
                            if (refNamesBlockIndex != -1)
                                throw new IOException("DAA file contains multiple ref_names blocks, not implemented.");
                            refNamesBlockIndex = i;
                            break;
                        case ref_lengths:
                            if (refLengthsBlockIndex != -1)
                                throw new IOException("DAA file contains multiple ref_lengths blocks, not implemented.");
                            refLengthsBlockIndex = i;
                            break;
                        case alignments:
                            if (alignmentsBlockIndex != -1)
                                throw new IOException("DAA file contains multiple alignments blocks, not implemented.");
                            alignmentsBlockIndex = i;
                            break;
                    }
                }
                if (refNamesBlockIndex == -1)
                    throw new IOException("DAA file contains 0 ref_names blocks, not implemented.");
                if (refLengthsBlockIndex == -1)
                    throw new IOException("DAA file contains 0 ref_lengths blocks, not implemented.");
                if (alignmentsBlockIndex == -1)
                    throw new IOException("DAA file contains 0 alignments blocks, not implemented.");
                if (refLengthsBlockIndex < refNamesBlockIndex)
                    throw new IOException("DAA file contains ref_lengths block before ref_names block, not implemented.");

                headerSize = ins.getPosition();

                lnK = Math.log(k);
            }
        }
    }

    /**
     * load all references from file (if not already loaded)
     *
     * @throws IOException
     */
    public void loadReferences(boolean loadOnDemand) throws IOException {
        if (references == null) {
            //System.err.println("Loading DAA references...");

            try (InputReaderLittleEndian ins = new InputReaderLittleEndian(new FileInputStreamAdapter(fileName))) {
                ins.skip(getLocationOfBlockInFile(getRefNamesBlockIndex()));
                initializeReferences((int) getDbSeqsUsed(), loadOnDemand);

                if (loadOnDemand) { // load on demand
                    for (int r = 0; r < getDbSeqsUsed(); r++) {
                        if ((r & (referenceLocationChunkSize - 1)) == 0) {
                            referenceLocations[r >>> referenceLocationChunkBits] = ins.getPosition();
                        }
                        ins.skipNullTerminatedBytes();
                    }
                } else { // load all now
                    for (int r = 0; r < getDbSeqsUsed(); r++) {
                        setReference(r, ins.readNullTerminatedBytes().getBytes());
                    }
                }
                initializeRefLengths((int) getDbSeqsUsed());
                for (int i = 0; i < getDbSeqsUsed(); i++) {
                    setRefLength(i, ins.readInt());
                }
            }
        }
    }

    private void initializeReferences(int numberOfReferences, boolean loadOnDemand) {
        this.references = new byte[numberOfReferences][];
        if (loadOnDemand)
            this.referenceLocations = new long[1 + (numberOfReferences >>> referenceLocationChunkBits)];
    }

    /**
     * get a reference header
     *
     * @param i
     * @param ins
     * @return reference header
     * @throws IOException
     */
    public byte[] getReference(final int i, final InputReaderLittleEndian ins) throws IOException {
        if (references[i] != null)
            return references[i];
        if (ins == null)
            throw new IOException("getReference(i,ins==null)");
        // we need to load:
        int iChunk = (i >>> referenceLocationChunkBits);
        final long savePosition = ins.getPosition();
        ins.seek(referenceLocations[iChunk]);

        int start = iChunk * referenceLocationChunkSize; // the smallest multiple of 64 that is <= i
        // System.err.println("i "+i+" start "+start+" (i-start) "+(i-start));
        int stop = Math.min((int) getDbSeqsUsed(), start + referenceLocationChunkSize);

        for (int r = start; r < stop; r++) {
            setReference(r, ins.readNullTerminatedBytes().getBytes());
        }
        ins.seek(savePosition); // restore current position

        // System.err.println("got: "+((stop-start)+" more entries"));
        return references[i];
    }

    /**
     * load all reference annotations from file
     *
     * @throws IOException
     */
    public void loadRefAnnotations() throws IOException {
        numberOfRefAnnotations = 0;
        refAnnotationIndexForTaxonomy = -1;

        try (InputReaderLittleEndian ins = new InputReaderLittleEndian(new FileInputStreamAdapter(fileName))) {
            for (int b = 0; b < blockTypeRank.length; b++) {
                if (getBlockType(b) == BlockType.megan_ref_annotations) {
                    ins.seek(getLocationOfBlockInFile(b));
                    refAnnotationNames[numberOfRefAnnotations] = ins.readNullTerminatedBytes();
                    if (refAnnotationNames[numberOfRefAnnotations].equals(Classification.Taxonomy))
                        refAnnotationIndexForTaxonomy = numberOfRefAnnotations;
                    int[] annotations = refAnnotations[numberOfRefAnnotations] = new int[getNumberOfReferences()];
                    for (int i = 0; i < getNumberOfReferences(); i++) {
                        annotations[i] = ins.readInt();
                    }
                    numberOfRefAnnotations++;
                }
            }
        }
    }

    public String getFileName() {
        return fileName;
    }

    public long getMagicNumber() {
        return magicNumber;
    }

    public long getVersion() {
        return version;
    }

    public long getDiamondBuild() {
        return diamondBuild;
    }

    public long getDbSeqs() {
        return dbSeqs;
    }

    public long getDbSeqsUsed() {
        return dbSeqsUsed;
    }

    private long getDbLetters() {
        return dbLetters;
    }

    public long getFlags() {
        return flags;
    }

    public long getQueryRecords() {
        return queryRecords;
    }

    public int getModeRank() {
        return modeRank;
    }

    public int getGapOpen() {
        return gapOpen;
    }

    public int getGapExtend() {
        return gapExtend;
    }

    public int getReward() {
        return reward;
    }

    public int getPenalty() {
        return penalty;
    }

    public int getReserved1() {
        return reserved1;
    }

    public int getReserved2() {
        return reserved2;
    }

    public int getReserved3() {
        return reserved3;
    }

    public void setReserved3(int reserved3) {
        this.reserved3 = reserved3;
    }

    public double getK() {
        return k;
    }

    public double getLambda() {
        return lambda;
    }

    public double getReserved4() {
        return reserved4;
    }

    public double getReserved5() {
        return reserved5;
    }

    public byte[] getScoreMatrix() {
        return scoreMatrix;
    }

    public long getBlockSize(int i) {
        return blockSize[i];
    }

    public void setBlockSize(int i, long size) {
        blockSize[i] = size;
    }

    public int getBlockTypeRankArrayLength() {
        return blockSize.length;
    }

    public byte getBlockTypeRank(int i) {
        return blockTypeRank[i];
    }

    public BlockType getBlockType(int i) {
        return BlockType.value(blockTypeRank[i]);
    }

    public void setBlockTypeRank(int i, byte rank) {
        blockTypeRank[i] = rank;
    }

    public int getNumberOfReferences() {
        return references == null ? 0 : references.length;
    }

    private void setReference(int i, byte[] reference) {
        references[i] = reference;
    }

    public int getRefLength(int i) {
        return refLengths[i];
    }

    private void setRefLength(int i, int length) {
        refLengths[i] = length;
    }


    private void initializeRefLengths(int numberOfReferences) {
        this.refLengths = new int[numberOfReferences];
    }

    public String getScoreMatrixName() {
        return scoreMatrixName;
    }

    public long getHeaderSize() {
        return headerSize;
    }

    private int getRefNamesBlockIndex() {
        return refNamesBlockIndex;
    }

    public int getRefLengthsBlockIndex() {
        return refLengthsBlockIndex;
    }

    public int getAlignmentsBlockIndex() {
        return alignmentsBlockIndex;
    }

    public double getLnK() {
        return lnK;
    }


    /**
     * gets the first available block index. This is the first index of a block that is empty and is not followed by any non-empty block
     *
     * @return first available block index or -1, if available
     */
    public int getFirstAvailableBlockIndex() {
        int index = getLastDefinedBlockIndex() + 1;
        if (index < getBlockTypeRankArrayLength())
            return index;
        else
            return -1;
    }

    /**
     * get the first index for the given blockType, or -1, if not found
     *
     * @param blockType
     * @return index or -1
     */
    public int getIndexForBlockType(BlockType blockType) {
        for (int i = 0; i < getBlockTypeRankArrayLength(); i++) {
            if (getBlockType(i) == blockType)
                return i;
        }
        return -1;
    }

    /**
     * gets the index of the last defined block
     *
     * @return highest index of any defined block
     */
    public int getLastDefinedBlockIndex() {
        final int emptyRank = BlockType.rank(BlockType.empty);
        for (int i = blockTypeRank.length - 1; i >= 0; i--) {
            if (blockTypeRank[i] != emptyRank)
                return i;
        }
        return -1;
    }

    public long getLocationOfBlockInFile(int blockIndex) {
        long location = getHeaderSize();
        for (int i = 0; i < blockIndex; i++)
            location += getBlockSize(i);
        return location;
    }

    public AlignMode getAlignMode() {
        return AlignMode.value(modeRank);
    }

    public long computeBlockStart(int index) {
        long start = headerSize;
        for (int k = 0; k < index; k++)
            start += blockSize[k];
        return start;
    }

    /**
     * gets the index assigned to a named reference annotation
     *
     * @param cName
     * @return index
     */
    public int getRefAnnotationIndex(String cName) {
        for (int i = 0; i < numberOfRefAnnotations; i++) {
            if (refAnnotationNames[i] != null && refAnnotationNames[i].equals(cName))
                return i;
        }
        return -1;
    }

    public int getRefAnnotationIndexForTaxonomy() {
        return refAnnotationIndexForTaxonomy;
    }

    /**
     * returns the reference annotation for a given reference annotation index and a given refNumber
     *
     * @param refAnnotationIndex
     * @param refNumber
     * @return reference annotation
     */
    public int getRefAnnotation(int refAnnotationIndex, int refNumber) {
        if (refAnnotationIndex < 0 || refAnnotationIndex >= refAnnotations.length)
            return 0;
        return refAnnotations[refAnnotationIndex][refNumber];
    }

    public String getRefAnnotationName(int i) {
        return refAnnotationNames[i];
    }

    public int getNumberOfRefAnnotations() {
        return numberOfRefAnnotations;
    }

    public String[] getRefAnnotationNames() {
        final List<String> list = new ArrayList<>();
        try (InputReaderLittleEndian reader = new InputReaderLittleEndian(new FileRandomAccessReadOnlyAdapter(fileName))) {
            for (int i = 0; i < getBlockTypeRankArrayLength(); i++) {
                if (getBlockType(i) == BlockType.megan_ref_annotations) {
                    final long pos = getLocationOfBlockInFile(i);
                    reader.seek(pos);
                    final String name = reader.readNullTerminatedBytes();
                    list.add(name);
                }
            }
        } catch (IOException e) {
            Basic.caught(e);
        }
        return list.toArray(new String[0]);
    }

    /**
     * read the header of a DAA file and all reference names
     *
     * @throws IOException
     */
    public void save() throws IOException {
        try (OutputWriterLittleEndian outs = new OutputWriterLittleEndian(new FileRandomAccessReadWriteAdapter(fileName, "rw"))) {
            outs.writeLong(magicNumber);
            outs.writeLong(version);

            outs.writeLong(diamondBuild);
            outs.writeLong(dbSeqs);
            outs.writeLong(dbSeqsUsed);
            outs.writeLong(dbLetters);
            outs.writeLong(flags);
            outs.writeLong(queryRecords);

            outs.writeInt(modeRank);
            outs.writeInt(gapOpen);
            outs.writeInt(gapExtend);
            outs.writeInt(reward);
            outs.writeInt(penalty);
            outs.writeInt(reserved1);
            outs.writeInt(reserved2);
            outs.writeInt(reserved3);

            outs.writeDouble(k);
            outs.writeDouble(lambda);
            outs.writeDouble(reserved4);
            outs.writeDouble(reserved5);

            for (byte a : scoreMatrix) {
                outs.write(a);
            }
            scoreMatrixName = Basic.toString(scoreMatrix);

            for (long a : blockSize) {
                outs.writeLong(a);
            }

            for (byte a : blockTypeRank) {
                outs.write(a);
            }
        }
    }

    private static final double LN_2 = 0.69314718055994530941723212145818;

    /**
     * compute the bit score from a raw score
     *
     * @param rawScore
     * @return bitscore
     */
    public float computeAlignmentBitScore(int rawScore) {
        return (float) ((lambda * rawScore - lnK) / LN_2);
    }

    /**
     * compute expected value
     *
     * @param queryLength
     * @param rawScore
     * @return expected
     */
    public float computeAlignmentExpected(int queryLength, int rawScore) {
        double bitScore = (float) ((lambda * rawScore - lnK) / LN_2);
        return (float) (((double) getDbLetters() * queryLength * Math.pow(2, -bitScore)));
    }
}
