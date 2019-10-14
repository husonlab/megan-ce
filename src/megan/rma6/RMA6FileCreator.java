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

import jloda.util.BlastMode;
import jloda.util.ListOfLongs;
import megan.io.OutputWriter;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * class used to create a new RMA6 file
 * Daniel Huson, 6.2015
 */
public class RMA6FileCreator extends RMA6File {
    private boolean isPairedReads;
    private final boolean useCompression;

    private int numberOfClassificationNames;

    private long totalNumberOfReads;
    private long totalNumberOfMatches;

    /**
     * constructor
     *
     * @param fileName
     */
    public RMA6FileCreator(String fileName, boolean useCompression) {
        super();
        this.useCompression = useCompression;
        this.fileName = fileName;
    }

    /**
     * setup and write the header
     *
     * @param creator
     * @param blastMode
     * @param matchClassificationNames
     * @param isPairedReads
     * @throws IOException
     */
    public void writeHeader(String creator, BlastMode blastMode, String[] matchClassificationNames, boolean isPairedReads) throws IOException {
        // setup header:
        final HeaderSectionRMA6 headerSection = getHeaderSectionRMA6();
        headerSection.setCreationDate(System.currentTimeMillis());
        headerSection.setCreator(creator);
        headerSection.setBlastMode(blastMode);
        headerSection.setMatchClassNames(matchClassificationNames);
        headerSection.setIsPairedReads(isPairedReads);

        this.isPairedReads = isPairedReads;
        numberOfClassificationNames = matchClassificationNames.length;

        File file = new File(fileName);
        if (file.exists() && !file.delete())
            throw new IOException("Can't delete existing file: " + file);

        readerWriter = new OutputWriter(new File(fileName)); // need to stream output for efficiency
        readerWriter.setUseCompression(useCompression);

        getFooterSectionRMA6().setStartHeaderSection(readerWriter.getPosition());
        getHeaderSectionRMA6().write(readerWriter);
        getFooterSectionRMA6().setEndHeaderSection(readerWriter.getPosition());
    }

    /**
     * Start adding queries and writing them to a file. Assumes that the header section has been set appropriately
     *
     * @throws IOException
     */
    public void startAddingQueries() throws IOException {
        totalNumberOfReads = 0;
        totalNumberOfMatches = 0;

        getFooterSectionRMA6().setStartReadsSection(readerWriter.getPosition());
    }

    /**
     * add a query and its matches to the file
     *
     * @param queryText
     * @param queryTextLength
     * @param numberOfMatches
     * @param matchesText
     * @param matchesTextLength
     * @param match2Classification2Id
     * @return the location of the read in the file
     * @throws IOException
     */
    public long addQuery(byte[] queryText, int queryTextLength, int numberOfMatches, byte[] matchesText, int matchesTextLength, int[][] match2Classification2Id, long mateLocation) throws IOException {
        final long location = readerWriter.getPosition();

        if (isPairedReads)
            readerWriter.writeLong(mateLocation);

        readerWriter.writeString(queryText, 0, queryTextLength);

        readerWriter.writeInt(numberOfMatches);

        for (int i = 0; i < numberOfMatches; i++) {
            for (int j = 0; j < numberOfClassificationNames; j++) {
                readerWriter.writeInt(match2Classification2Id[i][j]);
            }
        }

        readerWriter.writeString(matchesText, 0, matchesTextLength);

        this.totalNumberOfReads++;
        this.totalNumberOfMatches += numberOfMatches;

        return location;
    }


    /**
     * finish creating the file. Assumes that the footer section has been set appropriately
     *
     * @throws IOException
     */
    public void endAddingQueries() throws IOException {
        getFooterSectionRMA6().setEndReadsSection(readerWriter.getPosition());

        getFooterSectionRMA6().setNumberOfReads(totalNumberOfReads);
        getFooterSectionRMA6().setNumberOfMatches(totalNumberOfMatches);
    }

    /**
     * writes the classifications
     *
     * @param cNames
     * @param fName2Location
     * @param fName2weight
     * @throws IOException
     */
    public void writeClassifications(String[] cNames, Map<Integer, ListOfLongs>[] fName2Location, Map<Integer, Integer>[] fName2weight) throws IOException {
        getFooterSectionRMA6().setStartClassificationsSection(readerWriter.getPosition());
        getFooterSectionRMA6().getAvailableClassification2Position().clear();
        if (cNames != null) {
            for (int i = 0; i < cNames.length; i++) {
                final String cName = cNames[i];
                final ClassificationBlockRMA6 classification = new ClassificationBlockRMA6(cName);
                final Map<Integer, ListOfLongs> id2locations = fName2Location[i];
                for (int id : id2locations.keySet()) {
                    final Integer weight = fName2weight[i].get(id);
                    classification.setSum(id, weight != null ? weight : 0);
                }
                getFooterSectionRMA6().getAvailableClassification2Position().put(cName, readerWriter.getPosition());
                classification.write(readerWriter, id2locations);
                System.err.println(String.format("Class. %-13s%,10d", cName + ":", id2locations.size()));
            }
        }
        getFooterSectionRMA6().setEndClassificationsSection(readerWriter.getPosition());
    }

    /**
     * finish creating the file. Assumes that the footer section has been set appropriately
     *
     * @throws IOException
     */
    public void close() throws IOException {
        getFooterSectionRMA6().setStartFooterSection(readerWriter.getPosition());
        getFooterSectionRMA6().write(readerWriter);

        readerWriter.close();
        readerWriter = null;
    }

    public long getPosition() throws IOException {
        return readerWriter.getPosition();
    }


}
