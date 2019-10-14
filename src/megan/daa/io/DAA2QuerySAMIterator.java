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
import jloda.util.ICloseableIterator;
import jloda.util.Pair;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * iterator over all queries and sam records as text pairs
 */
public class DAA2QuerySAMIterator implements ICloseableIterator<Pair<byte[], byte[]>> {
    private final DAAParser daaParser;
    private final BlockingQueue<Pair<byte[], byte[]>> queue;
    private final ExecutorService executorService;

    private long count = 0;
    private Pair<byte[], byte[]> next = null;

    /**
     * constructor
     *
     * @param daaFile
     * @throws IOException
     */
    public DAA2QuerySAMIterator(String daaFile, final int maxMatchesPerRead, final boolean parseLongReads) throws IOException {
        this.daaParser = new DAAParser(daaFile);
        daaParser.getHeader().loadReferences(true);

        queue = new ArrayBlockingQueue<>(1000);

        // start a thread that loads queue:
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                daaParser.getAllAlignmentsSAMFormat(maxMatchesPerRead, queue, parseLongReads);
            } catch (IOException e) {
                Basic.caught(e);
            }
        });
    }

    @Override
    public void close() throws IOException {
        executorService.shutdownNow();
    }

    @Override
    public long getMaximumProgress() {
        return daaParser.getHeader().getQueryRecords();
    }

    @Override
    public long getProgress() {
        return count;
    }

    @Override
    public boolean hasNext() {
        synchronized (DAAParser.SENTINEL_SAM_ALIGNMENTS) {
            if (next == null) {
                try {
                    next = queue.take();
                } catch (InterruptedException e) {
                    Basic.caught(e);
                }
            }
            return next != DAAParser.SENTINEL_SAM_ALIGNMENTS;
        }
    }

    @Override
    public Pair<byte[], byte[]> next() {
        synchronized (DAAParser.SENTINEL_SAM_ALIGNMENTS) {
            if (next == null || next == DAAParser.SENTINEL_SAM_ALIGNMENTS)
                return null;
            count++;
            Pair<byte[], byte[]> result;
            result = next;
            try {
                next = queue.take();
            } catch (InterruptedException e) {
                Basic.caught(e);
            }
            return result;
        }
    }

    @Override
    public void remove() {
    }
}
