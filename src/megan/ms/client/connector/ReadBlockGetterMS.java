/*
 * Copyright (C) 2021. Daniel H. Huson
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

package megan.ms.client.connector;

import jloda.util.Basic;
import megan.data.IReadBlock;
import megan.data.IReadBlockGetter;
import megan.data.IReadBlockIterator;
import megan.ms.client.ClientMS;

import java.io.IOException;

/**
 * read block getter
 * Daniel Huson, 8.2020
 */
public class ReadBlockGetterMS implements IReadBlockGetter {
    private final ClientMS client;
    private final String fileName;
    private final int numberOfReads;
    private IReadBlockIterator iterator;
    private boolean iteratorHasBeenSet = false;
    private final boolean wantSequences;
    private final boolean wantMatches;
    private final String[] classifications;
    private static final String commandTemplate = "getRead?file=%s&readId=%d&binary=true&sequence=%b&matches=%b";

    /**
     * constructor
     */
    public ReadBlockGetterMS(ClientMS client, String fileName, boolean wantSequences, boolean wantMatches) throws IOException {
        this.client = client;
        this.fileName = fileName;
        this.wantSequences = wantSequences;
        this.wantMatches = wantMatches;
        this.numberOfReads = client.getAsInt("getNumberOfReads?file=" + fileName);
        this.classifications = Basic.getLinesFromString(client.getAsString("getClassificationNames?file=" + fileName), 1000).toArray(new String[0]);
    }

    @Override
    public IReadBlock getReadBlock(long uid) { // uid -1: stream
        if (uid == -1) // use next
        {
            if (!iteratorHasBeenSet) {
                iteratorHasBeenSet = true;
                try {
                    iterator = new ReadBlockIteratorMS(client, fileName, 0, 10, wantSequences, wantMatches);
                } catch (IOException e) {
                    Basic.caught(e);
                }
            }
            if (iterator != null && iterator.hasNext()) {
                return iterator.next();
            } else
                return null;
        }
        try {
            if (false) // todo: for debugging
            {
                final String commandTemplate = "getRead?file=%s&readId=%d&binary=false&sequence=%b&matches=%b";
                System.err.println(client.getAsString(String.format(commandTemplate, fileName, uid, wantSequences, wantMatches)));
            }

            final byte[] bytes = client.getAsBytes(String.format(commandTemplate, fileName, uid, wantSequences, wantMatches));
            return ReadBlockMS.readFromBytes(classifications, bytes);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void close() {
    }

    @Override
    public long getCount() {
        return numberOfReads;
    }
}
