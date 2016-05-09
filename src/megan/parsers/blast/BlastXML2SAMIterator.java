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
package megan.parsers.blast;

import jloda.util.Basic;
import megan.parsers.blast.blastxml.BlastXMLParser;
import megan.parsers.blast.blastxml.BlockingQueue;
import megan.parsers.blast.blastxml.MatchesText;
import megan.util.BlastXMLFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * parses a BlastXML files into SAM format
 * Daniel Huson, 4.2015
 */
public class BlastXML2SAMIterator implements ISAMIterator {
    private final ExecutorService executorService;
    private final BlastXMLParser blastXMLParser;
    private final BlockingQueue<MatchesText> blockingQueue;

    private MatchesText matchesText;

    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     */
    public BlastXML2SAMIterator(String fileName, int maxNumberOfMatchesPerRead) throws IOException {
        if (!BlastXMLFileFilter.getInstance().accept(fileName)) {
            close();
            throw new IOException("File not a BLAST file in XML format: " + fileName);
        }

        blockingQueue = new BlockingQueue<>();

        blastXMLParser = new BlastXMLParser(new File(fileName), blockingQueue, maxNumberOfMatchesPerRead);

        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            public void run() {
                try {
                    blastXMLParser.apply();
                } catch (Exception e) {
                    Basic.caught(e);
                } finally {
                    executorService.shutdown();
                }
            }
        });

    }

    /**
     * is there more data?
     *
     * @return true, if more data available
     */
    @Override
    public boolean hasNext() {
        return !blockingQueue.isOutputDone() || blockingQueue.size() > 1;
    }

    /**
     * gets the next matches
     *
     * @return number of matches
     */
    public int next() {
        if (hasNext()) {
            matchesText = blockingQueue.take();
            return matchesText.getNumberOfMatches();
        } else
            return -1;
    }

    /**
     * gets the matches text
     *
     * @return matches text
     */
    @Override
    public byte[] getMatchesText() {
        return matchesText.getText();
    }

    /**
     * length of matches text
     *
     * @return length of text
     */
    @Override
    public int getMatchesTextLength() {
        return matchesText.getLengthOfText();
    }

    @Override
    public long getMaximumProgress() {
        return blastXMLParser.getMaximumProgress();
    }

    @Override
    public long getProgress() {
        return blastXMLParser.getProgress();
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public byte[] getQueryText() {
        return null;
    }
}
