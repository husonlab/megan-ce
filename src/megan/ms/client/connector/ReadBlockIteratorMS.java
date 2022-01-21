/*
 * ReadBlockIteratorMS.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package megan.ms.client.connector;

import jloda.util.StringUtils;
import megan.daa.io.ByteInputStream;
import megan.daa.io.InputReaderLittleEndian;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.ms.client.ClientMS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * read blocks iterator
 * Daniel Huson, 8.2020
 */
public class ReadBlockIteratorMS implements IReadBlockIterator {
    private final ClientMS client;
    private final String[] classifications;
    private final ArrayList<IReadBlock> reads = new ArrayList<>();
    private int nextIndex = 0;
    private int count = 0;
    private long nextPageId = 0;

    /**
     * constructor
     */
    public ReadBlockIteratorMS(ClientMS client, String fileName, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
		this.client = client;
		this.classifications = StringUtils.getLinesFromString(client.getAsString("getClassificationNames?file=" + fileName), 1000).toArray(new String[0]);
		processBytes(client.getAsBytes("getReads?file=" + fileName + "&binary=true&sequences=" + wantReadSequence + "&matches=" + wantMatches + "&pageSize=" + client.getPageSize()));
    }

    /**
     * constructor
     */
    public ReadBlockIteratorMS(ClientMS client, String fileName, String classification, Collection<Integer> classIds, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
		this.client = client;
		this.classifications = StringUtils.getLinesFromString(client.getAsString("getClassificationNames?file=" + fileName), 1000).toArray(new String[0]);
		processBytes(client.getAsBytes("getReadsForClass?file=" + fileName + "&binary=true&classification=" + classification + "&classId=" + StringUtils.toString(classIds, ",") + "&sequences=" + wantReadSequence + "&matches=" + wantMatches + "&pageSize=" + client.getPageSize()));
	}

    private void processBytes(byte[] bytes) throws IOException {
        reads.clear();
        nextIndex = 0;

        try (InputReaderLittleEndian ins = new InputReaderLittleEndian(new ByteInputStream(bytes, 0, bytes.length))) {
            final int count = ins.readInt();
            for (int i = 0; i < count; i++) {
                int size = ins.readInt();
                reads.add(ReadBlockMS.readFromBytes(classifications, ins.readBytes(size)));
            }
            nextPageId = ins.readLong();
        }
    }

    @Override
    public String getStats() {
        return "Count: " + count;
    }

    @Override
    public void close() {

    }

    @Override
    public long getMaximumProgress() {
        return reads.size(); // not current, as doesn't take number of pages into account
    }

    @Override
    public long getProgress() {
        return nextIndex;
    }

    @Override
    public boolean hasNext() {
        return nextIndex < reads.size();
    }

    @Override
    public IReadBlock next() {
        if (hasNext()) {
            final IReadBlock result = reads.get(nextIndex++);
            if (nextIndex >= reads.size() && nextPageId > 0) {
                try {
                    processBytes(client.getAsBytes("getNext?pageId=" + nextPageId + "&binary=true" + "&pageSize=" + client.getPageSize()));
                    count++;
                } catch (IOException ignored) {
                }
            }
            return result;
        }
        return null;
    }
}
