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
import megan.data.IReadBlockGetter;

import java.io.IOException;


/**
 * Adapter to get reads from Megan5Server
 *
 * @author Hans-Joachim Ruscheweyh
 * 10:01:55 AM - Oct 28, 2014
 */
public class ReadBlockGetter implements IReadBlockGetter {

    private final String fileId;
    private final float minScore;
    private final float maxExpected;
    private final boolean wantReadText;
    private final boolean wantMatches;
    private final Megan5ServerConnector connector;

    public ReadBlockGetter(String fileId, float minScore, float maxExpected, boolean wantReadText, boolean wantMatches, Megan5ServerConnector connector) {
        this.fileId = fileId;
        this.maxExpected = maxExpected;
        this.minScore = minScore;
        this.connector = connector;
        this.wantReadText = wantReadText;
        this.wantMatches = wantMatches;
    }

    @Override
    public IReadBlock getReadBlock(long uid) throws IOException {
        return connector.getReadBlock(uid, fileId, minScore, maxExpected, wantReadText, wantMatches);
    }

    @Override
    public void close() {
    }

    /**
     * get total number of reads
     *
     * @return total number of reads
     */
    @Override
    public long getCount() {
        try {
            return connector.getNumberOfReads();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
