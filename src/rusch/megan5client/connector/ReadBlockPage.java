/*
 * ReadBlockPage.java Copyright (C) 2020. Daniel H. Huson
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
package rusch.megan5client.connector;

import rusch.megan5client.ReadBlockServer;


/**
 * A page containing a number of {@link ReadBlockServer}s
 *
 * @author Hans-Joachim Ruscheweyh
 * 10:20:53 AM - Oct 29, 2014
 * @update Jan 30, 2015: added totalNumberOfReads and previouslySeenReads
 */
public class ReadBlockPage {
    private long totalNumberOfReads;
    private long previouslySeenReads;
    private String nextPageToken;
    private String nextPageUrl;
    private ReadBlockServer[] readblocks;

    public ReadBlockPage() {
        nextPageToken = null;
        nextPageUrl = null;
        readblocks = new ReadBlockServer[0];
        totalNumberOfReads = 0;
        previouslySeenReads = 0;
    }


    public ReadBlockPage(String nextPageToken, String nextPageUrl,
                         ReadBlockServer[] readblocks, long totalNumberOfReads, long previouslySeenReads) {
        super();
        this.nextPageToken = nextPageToken;
        this.nextPageUrl = nextPageUrl;
        this.readblocks = readblocks;
        this.totalNumberOfReads = totalNumberOfReads;
        this.previouslySeenReads = previouslySeenReads;
    }


    public long getTotalNumberOfReads() {
        return totalNumberOfReads;
    }

    public void setTotalNumberOfReads(long totalNumberOfReads) {
        this.totalNumberOfReads = totalNumberOfReads;
    }

    public long getPreviouslySeenReads() {
        return previouslySeenReads;
    }

    public void setPreviouslySeenReads(long previouslySeenReads) {
        this.previouslySeenReads = previouslySeenReads;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }

    public String getNextPageUrl() {
        return nextPageUrl;
    }

    public void setNextPageUrl(String nextPageUrl) {
        this.nextPageUrl = nextPageUrl;
    }

    public ReadBlockServer[] getReadblocks() {
        return readblocks;
    }

    public void setReadblocks(ReadBlockServer[] readblocks) {
        this.readblocks = readblocks;
    }


}
