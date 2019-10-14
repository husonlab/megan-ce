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
package megan.rma3;

import jloda.util.Basic;
import megan.data.IReadBlock;
import megan.data.IReadBlockGetter;
import megan.data.MatchBlockFromBlast;
import megan.data.ReadBlockFromBlast;
import megan.io.IInputReader;
import megan.io.InputReader;
import megan.parsers.sam.SAMMatch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * accesses a read block
 * <p/>
 * Created by huson on 5/21/14.
 */
public class ReadBlockGetterRMA3 implements IReadBlockGetter {
    private final RMA3File rma3File;

    private final ReadLineRMA3 readLine;
    private final MatchLineRMA3 matchLine;

    private final float minScore;
    private final float maxExpected;

    private final boolean wantReadText;
    private final boolean wantMatches;

    private final IInputReader reader;
    private final InputReader samReader;
    private final InputReader fastaReader;

    private final SAMMatch samMatch;

    private final long startMatches;
    private final long endMatches;

    private boolean inStreaming = false;

    /**
     * constructor
     *
     * @param rma3File
     * @param minScore
     * @param maxExpected
     * @throws IOException
     */
    public ReadBlockGetterRMA3(RMA3File rma3File, float minScore, float maxExpected, boolean wantReadText, boolean wantMatches) throws IOException {
        this.rma3File = rma3File;
        this.wantReadText = wantReadText;
        this.wantMatches = wantMatches;

        readLine = new ReadLineRMA3(rma3File.getMatchFooter().getReadFormatDef());
        matchLine = new MatchLineRMA3(rma3File.getMatchFooter().getMatchFormatDef());

        samMatch = new SAMMatch(rma3File.getBlastMode());

        startMatches = rma3File.getStartMatches();
        endMatches = rma3File.getEndMatches();

        this.minScore = minScore;
        this.maxExpected = maxExpected;

        final FileManagerRMA3 rma3FileManager = FileManagerRMA3.getInstance();


        if (wantReadText && !readLine.isEmbedText()) {
            final File fastAFile = rma3FileManager.getFASTAFile(rma3File.getFileName());
            if (fastAFile != null)
                fastaReader = new InputReader(fastAFile, null, null, true);
            else
                fastaReader = null;
        } else
            fastaReader = null;

        if (wantMatches && !matchLine.isEmbedText()) {
            final File samFile = rma3FileManager.getSAMFile(rma3File.getFileName());
            if (samFile != null)
                samReader = new InputReader(samFile, null, null, true);
            else
                samReader = null;
        } else
            samReader = null;

        reader = rma3File.getReader();
        reader.seek(startMatches);
    }

    /**
     * grabs the read block with the given UID
     *
     * @param uid
     * @return read block
     * @throws IOException
     */
    @Override
    public IReadBlock getReadBlock(long uid) throws IOException {
        if (uid == -1 && !inStreaming) {
            inStreaming = true;
        }

        if (uid >= 0) {
            if (inStreaming)
                throw new IOException("getReadBlock(uid=" + uid + ") failed: streamOnly");
            reader.seek(uid);
        } else
            uid = reader.getPosition();
        readLine.read(reader);
        if (readLine.getReadUid() != uid)
            throw new IOException("getReadUid(): doesn't match expected: " + uid);

        final ReadBlockFromBlast readBlock = new ReadBlockFromBlast();

        readBlock.setUId(uid);
        readBlock.setReadWeight(readLine.getReadWeight());

        if (readLine.isEmbedText()) {
            String readText = readLine.getText();
            readBlock.setReadHeader(Utilities.getFastAHeader(readText));
            readBlock.setReadSequence(Utilities.getFastASequence(readText));
            readBlock.setNumberOfMatches(readLine.getNumberOfMatches());
        } else if (fastaReader != null) {
            try {
                String readText = Utilities.getFastAText(fastaReader, readLine.getFileOffset());
                readBlock.setReadHeader(Utilities.getFastAHeader(readText));
                readBlock.setReadSequence(Utilities.getFastASequence(readText));
            } catch (Exception ex) {
                Basic.caught(ex);
            }
        }

        final ArrayList<MatchBlockFromBlast> matches = new ArrayList<>(readLine.getNumberOfMatches());
        String firstSAMLineForCurrentRead = null;

        for (int i = 0; i < readLine.getNumberOfMatches(); i++) {
            if (reader.getPosition() >= endMatches)
                throw new IOException("Overrun matches section");

            matchLine.read(reader);

            if (wantMatches && matchLine.getBitScore() >= minScore && matchLine.getExpected() <= maxExpected) {
                MatchBlockFromBlast matchBlock = new MatchBlockFromBlast();
                matchBlock.setUId(matchLine.getFileOffset());
                matchBlock.setExpected(matchLine.getExpected());
                matchBlock.setBitScore(matchLine.getBitScore());
                matchBlock.setPercentIdentity(matchLine.getPercentId());
                matchBlock.setTaxonId(matchLine.getTaxId());
                if (matchLine.isDoKegg())
                    matchBlock.setId("KEGG", matchLine.getKeggId());
                if (matchLine.isDoSeed())
                    matchBlock.setId("SEED", matchLine.getSeedId());
                if (matchLine.isDoCog())
                    matchBlock.setId("EGGNOG", matchLine.getCogId());

                if (matchLine.isEmbedText()) {
                    samMatch.parse(SAMCompress.inflate(firstSAMLineForCurrentRead, matchLine.getText()));
                    matchBlock.setText(samMatch.getBlastAlignmentText());
                    if (readBlock.getReadHeader() == null)
                        readBlock.setReadHeader(samMatch.getQueryName());
                } else if (samReader != null) {
                    try {
                        samReader.seek(matchLine.getFileOffset());
                        samMatch.parse(samReader.readLine());
                        matchBlock.setText(samMatch.getBlastAlignmentText());
                        if (readBlock.getReadHeader() == null)
                            readBlock.setReadHeader(samMatch.getQueryName());
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    }
                }
                matches.add(matchBlock);
            }
            if (firstSAMLineForCurrentRead == null) { // need to grab first line
                if (matchLine.isEmbedText()) {
                    firstSAMLineForCurrentRead = matchLine.getText();
                } else if (wantReadText && (readBlock.getReadHeader() == null || readBlock.getReadHeader().length() == 0) && samReader != null) { // if we don't yet have the read header then we need it now
                    try {
                        samReader.seek(matchLine.getFileOffset());
                        firstSAMLineForCurrentRead = samReader.readLine();
                    } catch (Exception ex) {
                        Basic.caught(ex);
                    }
                }
            }
        }
        if (wantReadText && (readBlock.getReadHeader() == null || readBlock.getReadHeader().length() == 0) && firstSAMLineForCurrentRead != null)
            readBlock.setReadHeader(Basic.getFirstWord(firstSAMLineForCurrentRead));

        readBlock.setMatchBlocks(matches.toArray(new MatchBlockFromBlast[0]));

        return readBlock;
    }

    @Override
    public void close() {
        try {
            rma3File.close();
            if (samReader != null)
                samReader.close();
            if (fastaReader != null)
                fastaReader.close();
        } catch (Exception e) {
            Basic.caught(e);
        }
    }

    public long getPosition() {
        try {
            return reader.getPosition();
        } catch (IOException e) {
            return -1;
        }
    }

    public long getStartMatches() {
        return startMatches;
    }

    public long getEndMatches() {
        return endMatches;
    }

    /**
     * get total number of reads
     *
     * @return total number of reads
     */
    @Override
    public long getCount() {
        return rma3File.getMatchFooter().getNumberOfReads();
    }
}
