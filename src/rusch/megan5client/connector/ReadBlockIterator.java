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
package rusch.megan5client.connector;

import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import rusch.megan5client.ReadBlock;


/**
 * Paginated ReadBlockIterator for Megan5Server
 *
 * @author Hans-Joachim Ruscheweyh
 * 3:36:46 PM - Oct 28, 2014
 */
public class ReadBlockIterator implements IReadBlockIterator {
    private ReadBlockPage currentPage;
    private final Megan5ServerConnector connector;
    private int posInPage = 0;
    private final long totalNumberOfReads;
    private long progress = 0;

    public ReadBlockIterator(Megan5ServerConnector connector, ReadBlockPage page) {
        this.connector = connector;
        this.currentPage = page;
        this.totalNumberOfReads = page.getTotalNumberOfReads();
    }

    @Override
    public void close() {

    }

    @Override
    public long getMaximumProgress() {
        return totalNumberOfReads;
    }

    @Override
    public long getProgress() {
        return progress;
    }

    @Override
    public boolean hasNext() {
        if (posInPage == currentPage.getReadblocks().length) {
            //time to load new page
            if (currentPage.getNextPageToken() == null) {
                //well there is no next page
                return false;
            } else {
                currentPage = connector.retrieveReadBlockPage(currentPage.getNextPageToken());
                posInPage = 0;
                return hasNext();
            }
        }
        return posInPage < currentPage.getReadblocks().length;
    }

    @Override
    public IReadBlock next() {
        if (hasNext()) {
            progress++;
            posInPage++;
            return new ReadBlock(currentPage.getReadblocks()[posInPage - 1]);
        } else {
            return null;
        }
    }

    @Override
    public void remove() {
    }

    @Override
    public String getStats() {
        return "Reads: " + progress;
    }

}
