/*
 * Copyright (C) 2020. Daniel H. Huson
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
 *
 */

package megan.ms.server;

import megan.daa.connector.DAAConnector;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.rma6.RMA6Connector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * this implements pagination of read iterators
 * Daniel Huson, 8.2020
 */
public class ReadIteratorPagination {
    private static int timeoutSeconds = 10000;
    private static final AtomicLong mostRecentPageId = new AtomicLong(0);

    private final static Map<Long, PagingJob> pageId2Job = new TreeMap<>();

    public static Long createPagination(IReadBlockIterator iterator, String[] cNames, ReadsOutputFormat format, int pageSize) {
        // we keep references to read blocks, so must not reuse readblocks:
        RMA6Connector.reuseReadBlockInGetter=false;
        DAAConnector.reuseReadBlockInGetter=false;

        final long pageId = mostRecentPageId.addAndGet(1);
        final PagingJob pagingJob = new PagingJob(pageId, cNames, format, iterator, pageSize);
        synchronized (pageId2Job) {
            pageId2Job.put(pageId, pagingJob);
        }
        return pageId;
    }

    public static Page getNextPage(long pageId, int pageSize) {
        final PagingJob pagingJob = pageId2Job.get(pageId);
        if (pagingJob != null && pagingJob.hasNext()) {
            ArrayList<IReadBlock> data = pagingJob.nextPage(pageSize > 0 ? pageSize : pagingJob.getPageSize());
            final long newPageId;
            if (pagingJob.hasNext()) {
                newPageId = mostRecentPageId.addAndGet(1);
                synchronized (pageId2Job) {
                    pageId2Job.remove(pageId);
                    pageId2Job.put(newPageId, pagingJob);
                }
            } else
                newPageId = 0; // no further pages
            return new Page(data, pagingJob.getCNames(), pagingJob.getFormat(), newPageId);
        } else
            return Page.createEmptyPage();
    }

    private static void purgeStale() {
        synchronized (pageId2Job) {
            final long time = System.currentTimeMillis();
            final ArrayList<Long> toDelete = new ArrayList<>();
            for (PagingJob job : pageId2Job.values()) {
                if (!job.iterator.hasNext() || time - job.getLastAccess() > 1000 * timeoutSeconds) {
                    try {
                        job.iterator.close();
                    } catch (IOException ignored) {
                    }
                    toDelete.add(job.getJobId());
                }
            }
            pageId2Job.keySet().removeAll(toDelete);
        }
    }

    public static int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public static void setTimeoutSeconds(int timeoutSeconds) {
        ReadIteratorPagination.timeoutSeconds = timeoutSeconds;
    }

    private static class PagingJob {
        private final long jobId;
        private final String[] cNames;
        private final ReadsOutputFormat format;
        private final IReadBlockIterator iterator;
        private long lastAccess;
        private final int pageSize;

        public PagingJob(long jobId, String[] cNames, ReadsOutputFormat format, IReadBlockIterator iterator, int pageSize) {
            this.jobId = jobId;
            this.cNames = cNames;
            this.format = format;
            this.iterator = iterator;
            this.lastAccess = System.currentTimeMillis();
            this.pageSize = pageSize;
        }

        public long getJobId() {
            return jobId;
        }

        public String[] getCNames() {
            return cNames;
        }

        public ReadsOutputFormat getFormat() {
            return format;
        }

        public IReadBlockIterator getIterator() {
            return iterator;
        }

        public long getLastAccess() {
            return lastAccess;
        }

        public void setLastAccess(long lastAccess) {
            this.lastAccess = lastAccess;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public int getPageSize() {
            return pageSize;
        }

        public ArrayList<IReadBlock> nextPage(int pageSize) {
            setLastAccess(System.currentTimeMillis());
            final ArrayList<IReadBlock> list = new ArrayList<>(pageSize);
            while (iterator.hasNext() && list.size() < pageSize) {
                list.add(iterator.next());
            }
            setLastAccess(System.currentTimeMillis());
            purgeStale();
            return list;
        }
    }

    public static class Page {
        private final ArrayList<IReadBlock> reads;
        private final String[] cNames;
        private final ReadsOutputFormat format;
        private final long nextPage;

        public Page(ArrayList<IReadBlock> reads, String[] cNames, ReadsOutputFormat format, long nextPage) {
            this.reads = reads;
            this.cNames = cNames;
            this.format = format;
            this.nextPage = nextPage;
        }

        public static Page createEmptyPage() {
            return new Page(new ArrayList<>(),new String[0],new ReadsOutputFormat(false,false,false,false),-1);
        }

        public ArrayList<IReadBlock> getReads() {
            return reads;
        }

        public String[] getCNames() {
            return cNames;
        }

        public ReadsOutputFormat getFormat() {
            return format;
        }

        public long getNextPage() {
            return nextPage;
        }
    }
}
