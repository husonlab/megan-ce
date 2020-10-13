/*
 * IConnector.java Copyright (C) 2020. Daniel H. Huson
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
package megan.data;

import jloda.util.ProgressListener;
import jloda.util.Single;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * connection to RMA file, remote RMA file or database
 * Daniel Huson, 4.2010
 */
public interface IConnector {

    // classification names used below are defined in ClassificationBlock

    /**
     * set the file name of interest. Only one file can be used.
     *
     * @param filename
     */
    void setFile(String filename) throws IOException;

    /**
     * is connected document readonly?
     *
     * @return true, if read only
     * @throws IOException
     */
    boolean isReadOnly() throws IOException;

    /**
     * gets the unique identifier for the given filename.
     * This method is also used to test whether dataset exists and can be connected to
     *
     * @return unique id
     */
    long getUId() throws IOException;

    /**
     * get all reads with specified matches. If minScore=0 and topPercent=0, no filtering
     *
     * @param minScore         ignore
     * @param maxExpected      ignore
     * @param wantReadSequence
     * @param wantMatches
     * @return
     * @throws IOException
     */
    IReadBlockIterator getAllReadsIterator(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException;

    /**
     * get getLetterCodeIterator over all reads for given classification and classId. If minScore=0 and topPercent=0, no filtering
     *
     * @param classification
     * @param classId
     * @param minScore         ignore
     * @param maxExpected      ignore
     * @param wantReadSequence
     * @param wantMatches
     * @return
     * @throws IOException
     */
    IReadBlockIterator getReadsIterator(String classification, int classId, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException;

    /**
     * get getLetterCodeIterator over all reads for given classification and a collection of classids. If minScore=0 and topPercent=0, no filtering
     *
     * @param classification
     * @param classIds
     * @param minScore
     * @return getLetterCodeIterator over reads filtered by given parameters
     * @throws IOException
     */
    IReadBlockIterator getReadsIteratorForListOfClassIds(String classification, Collection<Integer> classIds, float minScore,
                                                         float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException;

    /**
     * gets a read block accessor
     *
     * @param minScore         ignored
     * @param maxExpected      ignored
     * @param wantReadSequence
     * @param wantMatches
     * @return
     * @throws IOException
     */
    IReadBlockGetter getReadBlockGetter(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException;

    /**
     * get array of all classification names associated with this file or db
     *
     * @return classifications
     */
    String[] getAllClassificationNames() throws IOException;

    /**
     * gets the number of classes in the named classification
     *
     * @param classificationName
     * @return number of classes
     * @throws java.io.IOException
     */
    int getClassificationSize(String classificationName) throws IOException;

    /**
     * gets the number of reads in a given class
     *
     * @param classificationName
     * @param classId
     * @return number of reads
     * @throws IOException
     */
    int getClassSize(String classificationName, int classId) throws IOException;

    /**
     * gets the named classification block
     *
     * @param classificationName
     * @return classification block
     * @throws IOException
     */
    IClassificationBlock getClassificationBlock(String classificationName) throws IOException;

    /**
     * updates the classId values for a collection of reads
     *
     * @param classificationNames names of classifications in the order that their values will appear in
     * @param updateItems         list of rescan items
     * @throws IOException
     */
    void updateClassifications(final String[] classificationNames, final List<UpdateItem> updateItems, ProgressListener progressListener) throws IOException;

    /**
     * get all reads that match the given expression
     *
     * @param regEx
     * @param findSelection where to search for matches
     * @param canceled
     * @return getLetterCodeIterator over reads that match
     * @throws IOException
     */
    IReadBlockIterator getFindAllReadsIterator(String regEx, FindSelection findSelection, Single<Boolean> canceled) throws IOException;

    /**
     * gets the number of reads
     *
     * @return number of reads
     * @throws IOException
     */
    int getNumberOfReads() throws IOException;

    /**
     * get the total number of matches
     *
     * @return number of matches
     */
    int getNumberOfMatches() throws IOException;

    /**
     * sets the number of reads. Note that the logical number of reads may differ from
     * the actual number of reads stored, so this needs to be stored explicitly
     *
     * @param numberOfReads
     * @throws IOException
     */
    void setNumberOfReads(int numberOfReads) throws IOException;

    /**
     * puts the MEGAN auxiliary data associated with the dataset
     *
     * @param label2data
     * @throws IOException
     */
    void putAuxiliaryData(Map<String, byte[]> label2data) throws IOException;

    /**
     * gets the MEGAN auxiliary data associated with the dataset
     * (Old style data should be returned with label USER_STATE)
     *
     * @return auxiliaryData
     * @throws IOException
     */
    Map<String, byte[]> getAuxiliaryData() throws IOException;
}
