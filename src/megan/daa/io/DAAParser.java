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
import jloda.util.Pair;
import jloda.util.interval.Interval;
import jloda.util.interval.IntervalTree;
import megan.io.FileInputStreamAdapter;
import megan.io.FileRandomAccessReadOnlyAdapter;
import megan.parsers.blast.PostProcessMatches;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

/**
 * DAA file
 * Daniel Huson, 8.2015
 */
public class DAAParser {
    private final DAAHeader header;

    private final byte[] sourceAlphabet;
    private final byte[] alignmentAlphabet;

    private final jloda.util.BlastMode blastMode;

    // blocking queue sentinel:
    public final static Pair<byte[], byte[]> SENTINEL_SAM_ALIGNMENTS = new Pair<>(null, null);
    public final static Pair<DAAQueryRecord, DAAMatchRecord[]> SENTINEL_QUERY_MATCH_BLOCKS = new Pair<>();

    private final IntervalTree<DAAMatchRecord> intervalTree = new IntervalTree<>(); // used in parsing of long reads
    private final ArrayList<DAAMatchRecord> list = new ArrayList<>();

    /**
     * constructor
     */
    public DAAParser(final String fileName) throws IOException {
        this(new DAAHeader(fileName, true));
    }

    /**
     * constructor
     */
    public DAAParser(final DAAHeader header) throws IOException {
        this.header = header;

        switch (header.getAlignMode()) {
            case blastx:
                sourceAlphabet = Translator.DNA_ALPHABET;
                alignmentAlphabet = Translator.AMINO_ACID_ALPHABET;
                break;
            case blastp:
                sourceAlphabet = Translator.AMINO_ACID_ALPHABET;
                alignmentAlphabet = Translator.AMINO_ACID_ALPHABET;
                break;
            case blastn:
                sourceAlphabet = Translator.DNA_ALPHABET;
                alignmentAlphabet = Translator.DNA_ALPHABET;
                break;
            default:
                sourceAlphabet = null;
                alignmentAlphabet = null;
        }
        blastMode = AlignMode.getBlastMode(header.getModeRank());
    }

    /**
     * read the header of a DAA file and all reference names
     *
     * @throws IOException
     */
    public static boolean isMeganizedDAAFile(String fileName, boolean checkWhetherMeganized) throws IOException {
        try (InputReaderLittleEndian ins = new InputReaderLittleEndian(new FileInputStreamAdapter(fileName))) {
            long magicNumber = ins.readLong();
            if (magicNumber != DAAHeader.MAGIC_NUMBER)
                throw new IOException("Input file is not a DAA file.");
            long version = ins.readLong();
            if (version > DAAHeader.DAA_VERSION)
                throw new IOException("DAA version requires later version of MEGAN.");

            if (!checkWhetherMeganized)
                return true;
            ins.skip(76);

            int meganVersion = ins.readInt(); // reserved3
            if (meganVersion <= 0)
                return false;
            if (meganVersion > DAAHeader.MEGAN_VERSION)
                throw new IOException("DAA version requires later version of MEGAN.");
            else return true;
        }
    }

    /**
     * get the blast mode
     *
     * @return blast mode
     */
    public jloda.util.BlastMode getBlastMode() {
        return blastMode;
    }

    public static jloda.util.BlastMode getBlastMode(String fileName) {
        try {
            DAAParser daaParser = new DAAParser(fileName);
            return daaParser.getBlastMode();
        } catch (IOException e) {
            Basic.caught(e);
            return jloda.util.BlastMode.Unknown;
        }
    }

    /**
     * get all alignments in SAM format
     *
     * @param maxMatchesPerRead
     * @param outputQueue
     * @throws IOException
     */
    void getAllAlignmentsSAMFormat(int maxMatchesPerRead, BlockingQueue<Pair<byte[], byte[]>> outputQueue, boolean parseLongReads) throws IOException {
        final ByteInputBuffer inputBuffer = new ByteInputBuffer();
        final ByteOutputBuffer outputBuffer = new ByteOutputBuffer(100000);

        final float minProportionCoverToDominate;
        final float topProportionScoreToDominate;
        if (parseLongReads) {
            final PostProcessMatches postProcessMatches = new PostProcessMatches();
            postProcessMatches.setParseLongReads(true);
            minProportionCoverToDominate = postProcessMatches.getMinProportionCoverToStronglyDominate();
            topProportionScoreToDominate = postProcessMatches.getTopProportionScoreToStronglyDominate();
        } else {
            minProportionCoverToDominate = 0;
            topProportionScoreToDominate = 0;
        }

        try (InputReaderLittleEndian ins = new InputReaderLittleEndian(new FileInputStreamAdapter(header.getFileName()));
             final InputReaderLittleEndian refIns = new InputReaderLittleEndian(new FileRandomAccessReadOnlyAdapter(header.getFileName()))) {
            ins.seek(header.getLocationOfBlockInFile(header.getAlignmentsBlockIndex()));
            final DAAQueryRecord queryRecord = new DAAQueryRecord(this);
            final DAAMatchRecord matchRecord = new DAAMatchRecord(queryRecord);

            for (int a = 0; a < header.getQueryRecords(); a++) {
                inputBuffer.rewind();
                queryRecord.setLocation(ins.getPosition());
                ins.readSizePrefixedBytes(inputBuffer);
                queryRecord.parseBuffer(inputBuffer);

                if (!parseLongReads) {
                    int numberOfMatches = 0;
                    while (inputBuffer.getPosition() < inputBuffer.size()) {
                        if (++numberOfMatches > maxMatchesPerRead)
                            break;
                        matchRecord.parseBuffer(inputBuffer, refIns);
                        SAMUtilities.createSAM(this, matchRecord, outputBuffer, alignmentAlphabet);
                    }
                } else // parse long reads
                {
                    intervalTree.clear();
                    while (inputBuffer.getPosition() < inputBuffer.size()) {
                        final DAAMatchRecord aMatchRecord = new DAAMatchRecord(queryRecord);
                        aMatchRecord.parseBuffer(inputBuffer, refIns);
                        intervalTree.add(aMatchRecord.getQueryBegin(), aMatchRecord.getQueryEnd(), aMatchRecord);
                    }
                    list.clear();
                    for (Interval<DAAMatchRecord> interval : intervalTree) {
                        boolean covered = false;
                        for (Interval<DAAMatchRecord> other : intervalTree.getIntervals(interval)) {
                            if (other.overlap(interval) >= minProportionCoverToDominate * interval.length() && topProportionScoreToDominate * other.getData().getScore() > interval.getData().getScore()) {
                                covered = true;
                                break;
                            }
                        }
                        if (!covered)
                            list.add(interval.getData());
                    }
                    for (DAAMatchRecord aMatchRecord : list) {
                        SAMUtilities.createSAM(this, aMatchRecord, outputBuffer, alignmentAlphabet);
                    }
                }

                if (outputBuffer.size() > 0) {
                    outputQueue.put(new Pair<>(queryRecord.getQueryFastA(sourceAlphabet), outputBuffer.copyBytes()));
                    outputBuffer.rewind();
                }
            }

            outputQueue.put(SENTINEL_SAM_ALIGNMENTS);

            // System.err.println(String.format("Total reads:   %,15d", header.getQueryRecords()));
            // System.err.println(String.format("Alignments:    %,15d", alignmentCount));
        } catch (InterruptedException e) {
            Basic.caught(e);
        }
    }

    /**
     * get all queries with matches
     *
     * @param maxMatchesPerRead
     * @param outputQueue
     * @throws IOException
     */
    void getAllQueriesAndMatches(boolean wantMatches, int maxMatchesPerRead, BlockingQueue<Pair<DAAQueryRecord, DAAMatchRecord[]>> outputQueue, boolean longReads) throws IOException {
        final ByteInputBuffer inputBuffer = new ByteInputBuffer();

        try (InputReaderLittleEndian ins = new InputReaderLittleEndian(new FileInputStreamAdapter(header.getFileName()));
             final InputReaderLittleEndian refIns = new InputReaderLittleEndian(new FileRandomAccessReadOnlyAdapter(header.getFileName()))) {
            ins.seek(header.getLocationOfBlockInFile(header.getAlignmentsBlockIndex()));

            final DAAMatchRecord[] matchRecords = new DAAMatchRecord[maxMatchesPerRead];

            for (int a = 0; a < header.getQueryRecords(); a++) {
                final Pair<DAAQueryRecord, DAAMatchRecord[]> pair = readQueryAndMatches(ins, refIns, wantMatches, maxMatchesPerRead, inputBuffer, matchRecords, longReads);
                outputQueue.put(pair);
            }
            outputQueue.put(SENTINEL_QUERY_MATCH_BLOCKS);
        } catch (InterruptedException e) {
            Basic.caught(e);
        }
    }

    /**
     * read a query and its matches
     *
     * @param ins
     * @param maxMatchesPerRead
     * @param inputBuffer       used internally, if non null
     * @param matchRecords      used internally, if non null
     * @return query and matches
     * @throws IOException
     */
    public Pair<DAAQueryRecord, DAAMatchRecord[]> readQueryAndMatches(InputReaderLittleEndian ins, InputReaderLittleEndian refIns, boolean wantMatches, int maxMatchesPerRead, ByteInputBuffer inputBuffer, DAAMatchRecord[] matchRecords, boolean longReads) throws IOException {
        final DAAQueryRecord queryRecord = new DAAQueryRecord(this);

        if (inputBuffer == null)
            inputBuffer = new ByteInputBuffer();
        else
            inputBuffer.rewind();

        queryRecord.setLocation(ins.getPosition());
        ins.readSizePrefixedBytes(inputBuffer);

        queryRecord.parseBuffer(inputBuffer);

        int numberOfMatches = 0;
        if (wantMatches) {
            if (!longReads) {
                if (matchRecords == null)
                    matchRecords = new DAAMatchRecord[maxMatchesPerRead];

                while (inputBuffer.getPosition() < inputBuffer.size()) {
                    DAAMatchRecord matchRecord = new DAAMatchRecord(queryRecord);
                    try {
                        matchRecord.parseBuffer(inputBuffer, refIns);
                        if (numberOfMatches < maxMatchesPerRead)
                            matchRecords[numberOfMatches++] = matchRecord;
                        else
                            break;
                    }
                    catch(Exception ex) {
                        Basic.caught(ex);
                    }
                }
            } else {
                intervalTree.clear();
                final Set<Interval<DAAMatchRecord>> alive = new HashSet<>();

                while (inputBuffer.getPosition() < inputBuffer.size()) {
                    final DAAMatchRecord aMatchRecord = new DAAMatchRecord(queryRecord);
                    aMatchRecord.parseBuffer(inputBuffer, refIns);

                    final Interval<DAAMatchRecord> interval = new Interval<>(aMatchRecord.getQueryBegin(), aMatchRecord.getQueryEnd(), aMatchRecord);

                    if (interval.getStart() > 10 && interval.getEnd() < queryRecord.getQueryLength() - 10 && aMatchRecord.getSubjectLen() < 0.8 * aMatchRecord.getTotalSubjectLen())
                        continue; // skip mini alignment that are not at the beginning or end of the read

                    boolean covered = false;

                    for (Interval<DAAMatchRecord> other : intervalTree.getIntervals(interval)) {
                        if (alive.contains(other)) {
                            if (interval.overlap(other) >= 0.5 * interval.length() && interval.getData().getScore() < 0.95 * other.getData().getScore()) {
                                covered = true;
                                break;
                            } else if (interval.overlap(other) >= 0.5 * other.length() && other.getData().getScore() < 0.95 * interval.getData().getScore()) {
                                alive.remove(other);
                            }
                        }
                    }
                    if (!covered) {
                        alive.add(interval);
                        intervalTree.add(interval);
                    }
                }

                numberOfMatches = alive.size();

                if (matchRecords == null || numberOfMatches >= matchRecords.length) {
                    matchRecords = new DAAMatchRecord[numberOfMatches];
                }

                {
                    int i = 0;
                    for (Interval<DAAMatchRecord> interval : intervalTree.getAllIntervals(true)) {
                        if (alive.contains(interval))
                            matchRecords[i++] = interval.getData();
                    }
                }
            }
        }

        if (numberOfMatches > 0) {
            final DAAMatchRecord[] usedMatchRecords = new DAAMatchRecord[numberOfMatches];
            System.arraycopy(matchRecords, 0, usedMatchRecords, 0, numberOfMatches);
            return new Pair<>(queryRecord, usedMatchRecords);
        } else
            return new Pair<>(queryRecord, new DAAMatchRecord[0]);
    }

    /**
     * get the header block
     *
     * @return header
     */
    public DAAHeader getHeader() {
        return header;
    }

    public byte[] getSourceAlphabet() {
        return sourceAlphabet;
    }

    public byte[] getAlignmentAlphabet() {
        return alignmentAlphabet;
    }

    public int getRefAnnotation(String cName, int refId) {
        return header.getRefAnnotation(header.getRefAnnotationIndex(cName), refId);
    }

    /**
     * gets a block as a string of bytes
     *
     * @param header
     * @param blockType
     * @return block
     * @throws IOException
     */
    public static byte[] getBlock(DAAHeader header, BlockType blockType) throws IOException {
        int index = header.getIndexForBlockType(blockType);
        if (index == -1)
            return null;
        long location = header.getLocationOfBlockInFile(index);
        if (header.getBlockSize(index) > Integer.MAX_VALUE - 10)
            throw new IOException("Internal error: block too big");
        int size = (int) header.getBlockSize(index);
        try (RandomAccessFile raf = new RandomAccessFile(header.getFileName(), "r")) {
            raf.seek(location);
            byte[] bytes = new byte[size];
            raf.read(bytes);
            return bytes;
        }
    }
}
