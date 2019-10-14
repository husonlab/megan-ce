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
package megan.parsers.blast;

import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import megan.parsers.blast.blastxml.BlastXMLParser;
import megan.parsers.blast.blastxml.MatchesText;
import megan.util.BlastXMLFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * parses a BlastXML files into SAM format
 * Daniel Huson, 4.2015
 */
public class BlastXML2SAMIterator implements ISAMIterator {
    private final ExecutorService executorService;
    private final BlastXMLParser blastXMLParser;
    private final BlockingQueue<MatchesText> queue;

    private MatchesText currentMatches;
    private MatchesText nextMatches;
    private final MatchesText sentinel;
    private boolean done = false;

    private boolean parseLongReads;

    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     */
    public BlastXML2SAMIterator(String fileName, int maxNumberOfMatchesPerRead) throws IOException {
        if (!BlastXMLFileFilter.getInstance().accept(fileName)) {
            NotificationsInSwing.showWarning("Might not be a BLAST file in XML format: " + fileName);
        }

        queue = new ArrayBlockingQueue<>(10000);
        sentinel = new MatchesText();
        currentMatches = null;
        nextMatches = null;

        blastXMLParser = new BlastXMLParser(new File(fileName), queue, maxNumberOfMatchesPerRead);

        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                blastXMLParser.apply();
            } catch (Exception e) {
                Basic.caught(e);
                NotificationsInSwing.showError(Basic.getShortName(e.getClass()) + ": " + e.getMessage());
            } finally {
                try {
                    queue.put(sentinel);
                } catch (InterruptedException e) {
                    done = true;
                    Basic.caught(e);
                }
                executorService.shutdownNow();
            }
        });
    }

    private MatchesText getNext() {
        if (!done) {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                done = true;
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * is there more data?
     *
     * @return true, if more data available
     */
    @Override
    public boolean hasNext() {
        if (done)
            return false;
        if (nextMatches == null)
            nextMatches = getNext();
        if (nextMatches == sentinel) {
            done = true;
            nextMatches = null;
        }
        return !done;

    }

    /**
     * gets the next matches
     *
     * @return number of matches
     */
    public int next() {
        if (hasNext()) {
            currentMatches = nextMatches;
            nextMatches = null;
            return currentMatches.getNumberOfMatches();
        }
        return -1;
    }

    /**
     * gets the current matches text
     *
     * @return matches text
     */
    @Override
    public byte[] getMatchesText() {
        return currentMatches.getText();
    }

    /**
     * get length of current matches text
     *
     * @return length of text
     */
    @Override
    public int getMatchesTextLength() {
        return currentMatches.getLengthOfText();
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

    @Override
    public void setParseLongReads(boolean longReads) {
        this.parseLongReads = longReads;
    }

    @Override
    public boolean isParseLongReads() {
        return parseLongReads;
    }
}
