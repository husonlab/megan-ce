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
package megan.rma6;

import jloda.util.*;
import megan.classification.ClassificationManager;
import megan.classification.IdParser;
import megan.parsers.blast.BlastFileFormat;
import megan.parsers.blast.BlastModeUtils;
import megan.parsers.blast.ISAMIterator;
import megan.parsers.blast.IteratorManager;
import megan.parsers.sam.SAMMatch;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * iterates over all read blocks found in a blast file
 * Daniel Huson, 4.2015
 */
public class BlastFileReadBlockIterator implements Iterator<ReadBlockRMA6>, ICloseableIterator<ReadBlockRMA6> {
    private final String readsFile;

    private final BlastMode blastMode;
    private final String[] cNames;
    private final IdParser[] parsers;

    private final ISAMIterator iterator;
    private final FileLineBytesIterator fastaIterator;
    private final boolean isFasta;
    private final byte[] queryName = new byte[100000];
    private final Single<byte[]> fastAText = new Single<>(new byte[1000]);
    private int missingReadWarnings;

    /**
     * constructor
     *
     * @param blastFile
     * @param format
     * @param blastMode
     * @param maxMatchesPerRead
     * @throws IOException
     */
    public BlastFileReadBlockIterator(String blastFile, String readsFile, BlastFileFormat format, BlastMode blastMode, String[] cNames, int maxMatchesPerRead, boolean longReads) throws IOException {
        this.readsFile = readsFile;
        this.cNames = cNames;
        parsers = new IdParser[cNames.length];
        for (int i = 0; i < cNames.length; i++) {
            parsers[i] = ClassificationManager.get(cNames[i], true).getIdMapper().createIdParser();
        }

        if (format == BlastFileFormat.Unknown) {
            format = BlastFileFormat.detectFormat(null, blastFile, false);
        }
        if (blastMode == BlastMode.Unknown) {
            blastMode = BlastModeUtils.detectMode(null, blastFile, false);
        }
        this.blastMode = blastMode;

        iterator = IteratorManager.getIterator(blastFile, format, blastMode, maxMatchesPerRead, longReads);
        if (readsFile != null) {
            fastaIterator = new FileLineBytesIterator(readsFile);
            isFasta = (fastaIterator.peekNextByte() == '>');
            if (!isFasta && (fastaIterator.peekNextByte() != '@'))
                throw new IOException("Cannot determine type of reads file (doesn't start with '>' or '@");
        } else {
            fastaIterator = null;
            isFasta = false;
        }
    }

    /**
     * close
     *
     * @throws IOException
     */
    public void close() throws IOException {
        iterator.close();
    }

    /**
     * gets the maximum progress value
     *
     * @return maximum progress value
     */
    @Override
    public long getMaximumProgress() {
        return iterator.getMaximumProgress();
    }

    /**
     * gets the current progress value
     *
     * @return current progress value
     */
    @Override
    public long getProgress() {
        return iterator.getProgress();
    }

    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {
        try {
            return iterator.hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public ReadBlockRMA6 next() {
        if (!hasNext())
            throw new NoSuchElementException();

        final int numberOfMatches = iterator.next();
        final byte[] matchesText = iterator.getMatchesText();
        final int matchesTextLength = iterator.getMatchesTextLength();

        final ReadBlockRMA6 readBlock = new ReadBlockRMA6(blastMode, false, cNames);

        final int queryNameLength = Basic.getFirstWord(matchesText, queryName);

        // write the read text
        boolean foundRead = false;
        if (fastaIterator != null) {
            if (Utilities.findQuery(queryName, queryNameLength, fastaIterator, isFasta)) {
                final int length = Utilities.getFastAText(fastaIterator, isFasta, fastAText);
                String fasta = Basic.toString(fastAText.get(), 0, length);
                int pos = fasta.indexOf('\n');
                if (pos > 0) {
                    readBlock.setReadHeader(fasta.substring(0, pos));
                    if (pos + 1 < fasta.length())
                        readBlock.setReadSequence(fasta.substring((pos + 1)));
                }
                foundRead = true;
            } else {
                if (missingReadWarnings++ < 50)
                    System.err.println("WARNING: Failed to find read '" + Basic.toString(queryName, 0, queryNameLength) + "' in file: " + readsFile);
                if (missingReadWarnings == 50)
                    System.err.println("No further missing warnings");
            }
        }
        if (!foundRead) {
            readBlock.setReadHeader(String.format(">%s\n", Basic.toString(queryName, 0, queryNameLength)));
        }
        int start = 0;
        MatchBlockRMA6[] matchBlocks = new MatchBlockRMA6[numberOfMatches];
        for (int matchCount = 0; matchCount < numberOfMatches; matchCount++) {
            int end = Utilities.nextNewLine(matchesText, start);
            final String aLine = Basic.toString(matchesText, start, end - start + 1);
            start = end + 1;
            MatchBlockRMA6 matchBlock = new MatchBlockRMA6();
            SAMMatch samMatch = new SAMMatch(blastMode);
            try {
                samMatch.parse(aLine);
            } catch (IOException e) {
                Basic.caught(e);
                return null;
            }
            matchBlock.setFromSAM(samMatch);
            for (IdParser parser : parsers) {
                try {
                    matchBlock.setId(parser.getCName(), parser.getIdFromHeaderLine(samMatch.getRefName()));
                } catch (IOException e) {
                    Basic.caught(e);
                }
            }
            matchBlocks[matchCount] = matchBlock;
        }
        readBlock.setNumberOfMatches(numberOfMatches);
        readBlock.setMatchBlocks(matchBlocks);
        return readBlock;
    }

    @Override
    public void remove() {

    }
}
