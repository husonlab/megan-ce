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
package megan.rma2;

import jloda.util.Basic;
import megan.data.IReadBlock;
import megan.data.IReadBlockGetter;
import megan.data.TextStorageReader;
import megan.io.IInputReader;

import java.io.File;
import java.io.IOException;

/**
 * RMA2 implementation of read block accessor
 * Daniel Huson, 10.2010
 */
public class ReadBlockGetterRMA2 implements IReadBlockGetter {
    private final float minScore;
    private final float maxExpected;
    private final TextStorageReader textStorageReader;
    private final IInputReader dataIndexReader;
    private final RMA2Formatter rma2Formatter;
    private final boolean wantReadText;
    private final boolean wantMatchText;
    private final boolean wantMatchData;

    private final long numberOfReads;

    /**
     * constructor
     *
     * @param file
     * @param minScore
     * @param wantReadText
     * @param wantMatchData
     * @param wantMatchText
     * @throws IOException
     */
    public ReadBlockGetterRMA2(File file, float minScore, float maxExpected, boolean wantReadText, boolean wantMatchData, boolean wantMatchText) throws IOException {
        this.minScore = minScore;
        this.maxExpected = maxExpected;
        this.wantReadText = wantReadText;
        this.wantMatchText = wantMatchText;
        this.wantMatchData = wantMatchData;

        RMA2File rma2File = new RMA2File(file);

        dataIndexReader = rma2File.getDataIndexReader();
        InfoSection infoSection = rma2File.loadInfoSection();
        rma2Formatter = infoSection.getRMA2Formatter();

        if (wantReadText || wantMatchText) {
            textStorageReader = new TextStorageReader(infoSection.getLocationManager(file));
        } else
            textStorageReader = null;

        numberOfReads = rma2File.getNumberOfReads();
    }

    /**
     * gets the read block associated with the given uid
     *
     * @param uid
     * @return read block or null
     * @throws java.io.IOException
     */
    public IReadBlock getReadBlock(long uid) throws IOException {
        return ReadBlockRMA2.read(rma2Formatter, uid, wantReadText, wantMatchData, wantMatchText, minScore, maxExpected, textStorageReader, dataIndexReader);
    }

    /**
     * closes the accessor
     *
     * @throws java.io.IOException
     */
    public void close() {
        if (textStorageReader != null)
            textStorageReader.closeAllFiles();
        if (dataIndexReader != null)
            try {
                dataIndexReader.close();
            } catch (IOException e) {
                Basic.caught(e);
            }
    }

    /**
     * get total number of reads
     *
     * @return total number of reads
     */
    @Override
    public long getCount() {
        return numberOfReads;
    }
}
