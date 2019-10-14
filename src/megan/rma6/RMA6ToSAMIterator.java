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
package megan.rma6;

import jloda.util.Basic;
import jloda.util.ListOfLongs;
import megan.io.IInputReader;
import megan.parsers.blast.ISAMIterator;

import java.io.IOException;
import java.util.Collection;

/**
 * iterator over an RMA6 file that returns reads and matches in SAM format
 * Daniel Huson, 4.2015
 */
public class RMA6ToSAMIterator implements ISAMIterator {
    private final RMA6File rma6File;
    private final IInputReader reader;

    private final ListOfLongs list;
    private final int positionInList = 0;

    private final String[] cNames;
    private final boolean pairedReads;

    private String readText;
    private byte[] matchesText;
    private int matchesTextLength;

    private boolean parseLongReads;

    /**
     * constructor
     *
     * @param classificationName
     * @param classIds
     * @param fileName
     * @throws IOException
     */
    public RMA6ToSAMIterator(String classificationName, Collection<Integer> classIds, String fileName) throws IOException {
        rma6File = new RMA6File(fileName, "r");
        reader = rma6File.getReader();
        pairedReads = rma6File.getHeaderSectionRMA6().isPairedReads();
        cNames = rma6File.getHeaderSectionRMA6().getMatchClassNames();

        final ClassificationBlockRMA6 block = new ClassificationBlockRMA6(classificationName);
        long start = rma6File.getFooterSectionRMA6().getStartClassification(classificationName);
        block.read(start, rma6File.getReader());
        list = new ListOfLongs();
        for (Integer classId : classIds) {
            if (block.getSum(classId) > 0) {
                block.readLocations(start, rma6File.getReader(), classId, list);
            }
        }
    }

    /**
     * gets the next matches
     *
     * @return number of matches
     */
    @Override
    public int next() {
        try {
            if (pairedReads)
                reader.skipBytes(8); // skip paired read info

            readText = reader.readString(); // read the read text

            final int numberOfMatches = reader.readInt(); // number of matches
            reader.skipBytes(numberOfMatches * cNames.length * 4); // skip taxon and classification ids
            matchesText = reader.readString().getBytes(); // todo: implement reading this directly into byte[]
            matchesTextLength = matchesText.length;
            return numberOfMatches;
        } catch (IOException ex) {
            Basic.caught(ex);
            return -1;
        }
    }

    /**
     * is there more data?
     *
     * @return true, if more data available
     */
    @Override
    public boolean hasNext() throws IOException {
        return positionInList < list.size();
    }

    /**
     * gets the matches text
     *
     * @return matches text
     */
    @Override
    public byte[] getMatchesText() {
        return matchesText;
    }

    /**
     * length of matches text
     *
     * @return length of text
     */
    @Override
    public int getMatchesTextLength() {
        return matchesTextLength;
    }

    @Override
    public long getMaximumProgress() {
        return list.size();
    }

    @Override
    public long getProgress() {
        return positionInList;
    }

    @Override
    public void close() throws IOException {
        rma6File.close();
    }

    /**
     * gets the read text
     *
     * @return read text
     */
    public String getReadText() {
        return readText;
    }

    @Override
    public byte[] getQueryText() {
        return readText.getBytes();
    }

    @Override
    public void setParseLongReads(boolean longReads) {
        parseLongReads = longReads;
    }

    @Override
    public boolean isParseLongReads() {
        return parseLongReads;
    }
}
