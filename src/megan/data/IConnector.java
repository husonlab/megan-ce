/*
 * IConnector.java Copyright (C) 2022 Daniel H. Huson
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
package megan.data;

import jloda.util.Single;
import jloda.util.progress.ProgressListener;

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
	 */
    void setFile(String filename) throws IOException;

    /**
     * is connected document readonly?
     *
     * @return true, if read only
     */
    boolean isReadOnly();

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
	 */
    IReadBlockIterator getAllReadsIterator(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException;

    /**
     * get getLetterCodeIterator over all reads for given classification and classId. If minScore=0 and topPercent=0, no filtering
     *
     * @param minScore         ignore
     * @param maxExpected      ignore
	 */
    IReadBlockIterator getReadsIterator(String classification, int classId, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException;

    /**
     * get getLetterCodeIterator over all reads for given classification and a collection of classids. If minScore=0 and topPercent=0, no filtering
     *
     * @return getLetterCodeIterator over reads filtered by given parameters
	 */
    IReadBlockIterator getReadsIteratorForListOfClassIds(String classification, Collection<Integer> classIds, float minScore,
                                                         float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException;

    /**
     * gets a read block accessor
     *
     * @param minScore         ignored
     * @param maxExpected      ignored
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
     * @return number of classes
	 */
    int getClassificationSize(String classificationName) throws IOException;

    /**
     * gets the number of reads in a given class
     *
     * @return number of reads
	 */
    int getClassSize(String classificationName, int classId) throws IOException;

    /**
     * gets the named classification block
     *
     * @return classification block
	 */
    IClassificationBlock getClassificationBlock(String classificationName) throws IOException;

    /**
     * updates the classId values for a collection of reads
     *
     * @param classificationNames names of classifications in the order that their values will appear in
     * @param updateItems         list of rescan items
	 */
    void updateClassifications(final String[] classificationNames, final List<UpdateItem> updateItems, ProgressListener progressListener) throws IOException;

    /**
     * get all reads that match the given expression
     *
     * @param findSelection where to search for matches
     * @return getLetterCodeIterator over reads that match
	 */
    IReadBlockIterator getFindAllReadsIterator(String regEx, FindSelection findSelection, Single<Boolean> canceled) throws IOException;

    /**
     * gets the number of reads
     *
     * @return number of reads
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
	 */
    void setNumberOfReads(int numberOfReads) throws IOException;

    /**
     * puts the MEGAN auxiliary data associated with the dataset
     *
	 */
    void putAuxiliaryData(Map<String, byte[]> label2data) throws IOException;

    /**
     * gets the MEGAN auxiliary data associated with the dataset
     * (Old style data should be returned with label USER_STATE)
     *
     * @return auxiliaryData
	 */
    Map<String, byte[]> getAuxiliaryData() throws IOException;
}
