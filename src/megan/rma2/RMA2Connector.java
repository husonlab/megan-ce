/*
 * RMA2Connector.java Copyright (C) 2021. Daniel H. Huson
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
package megan.rma2;

import jloda.util.progress.ProgressListener;
import jloda.util.Single;
import megan.data.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * connector for an RMA2 file
 * Daniel Huson, 9.2010
 */
public class RMA2Connector implements IConnector {
    private File file;

    /**
     * constructor
     *
     * @param fileName
     * @throws java.io.IOException
     */
    public RMA2Connector(String fileName) throws IOException {
        setFile(fileName);
    }

    /**
     * set the file name of interest. Only one file can be used.
     *
     * @param filename
     */
    public void setFile(String filename) throws IOException {
        file = new File(filename);
    }

    /**
     * is connected document readonly?
     *
     * @return true, if read only
     * @throws java.io.IOException
     */
    public boolean isReadOnly() throws IOException {
        return file == null || !file.canWrite();
    }

    /**
     * gets the unique identifier for the given filename.
     * This method is also used to test whether dataset exists and can be connected to
     *
     * @return unique id
     */
    public long getUId() throws IOException {
        return (new RMA2File(file)).getCreationDate();
    }

    /**
     * get all reads with specified matches.
     *
     * @param minScore
     * @param wantReadSequence @param wantMatches specifies what data to return  in ReadBlock
     * @return getLetterCodeIterator over all reads
     * @throws java.io.IOException
     */
    public IReadBlockIterator getAllReadsIterator(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return new AllReadsIterator(getReadBlockGetter(minScore, maxExpected, wantReadSequence, wantMatches));
    }

    /**
     * get getLetterCodeIterator over all reads for given classification and classId.
     *
     * @param classification
     * @param classId
     * @param minScore
     * @param wantReadSequence @param wantMatches  specifies what data to return  in ReadBlock
     * @return getLetterCodeIterator over reads in class
     * @throws java.io.IOException
     */
    public IReadBlockIterator getReadsIterator(String classification, int classId, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return getReadsIteratorForListOfClassIds(classification, Collections.singletonList(classId), minScore, maxExpected, wantReadSequence, wantMatches);
    }

    /**
     * get getLetterCodeIterator over all reads for given classification and a collection of classids. If minScore=0  no filtering
     *
     * @param classification
     * @param classIds
     * @param minScore
     * @param wantReadSequence @param wantMatches
     * @return getLetterCodeIterator over reads filtered by given parameters
     * @throws IOException
     */

    public IReadBlockIterator getReadsIteratorForListOfClassIds(String classification, Collection<Integer> classIds, float minScore,
                                                                float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return new ReadBlockIteratorRMA2(classification, classIds, wantReadSequence, wantMatches, wantMatches, minScore, maxExpected, file);
    }

    /**
     * gets a read block accessor
     *
     * @param wantReadSequence @param wantMatches
     * @return read block accessor
     * @throws java.io.IOException
     */
    public IReadBlockGetter getReadBlockGetter(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return new ReadBlockGetterRMA2(file, minScore, maxExpected, wantReadSequence, wantMatches, wantMatches);
    }

    /**
     * get array of all classification names associated with this file or db
     *
     * @return classifications
     */
    public String[] getAllClassificationNames() throws IOException {
        return (new RMA2File(file)).getClassificationNames();
    }

    /**
     * gets the number of classes in the named classification
     *
     * @param classificationName
     * @return number of classes
     * @throws java.io.IOException
     */
    public int getClassificationSize(String classificationName) throws IOException {
        return (new RMA2File(file)).getClassificationSize(classificationName);
    }

    /**
     * gets the number of reads in a given class
     *
     * @param classificationName
     * @param classId
     * @return number of reads
     * @throws java.io.IOException
     */
    public int getClassSize(String classificationName, int classId) throws IOException {
        ClassificationBlockRMA2 classificationBlock = (new RMA2File(file)).getClassificationBlock(classificationName);
        // todo: this is very wasteful, better to read in the block once and then keep it...
        return classificationBlock.getSum(classId);
    }

    /**
     * gets the named classification block
     *
     * @param classificationName
     * @return classification block
     * @throws java.io.IOException
     */
    public IClassificationBlock getClassificationBlock(String classificationName) throws IOException {
        return (new RMA2File(file)).getClassificationBlock(classificationName);
    }

    /**
     * updates the classId values for a collection of reads
     *
     * @param names          names of classifications in the order that their values will appear in
     * @param updateItemList list of rescan items
     * @throws java.io.IOException
     */
    public void updateClassifications(String[] names, List<UpdateItem> updateItemList, ProgressListener progressListener) throws IOException {
        UpdateItemList updateItems = (UpdateItemList) updateItemList;

        final int numClassifications = names.length;

        long maxProgress = 0;
        for (int i = 0; i < numClassifications; i++) {
            maxProgress += updateItems.getClassIds(i).size();
        }
        progressListener.setMaximum(maxProgress);

        final RMA2Modifier rma2Modifier = new RMA2Modifier(file);
        for (int i = 0; i < numClassifications; i++) {
            rma2Modifier.startClassificationSection(names[i]);
            try {
                for (Integer classId : updateItems.getClassIds(i)) {
                    float weight = updateItems.getWeight(i, classId);
                    final List<Long> positions = new ArrayList<>();
                    if (updateItems.getWeight(i, classId) > 0) {
                        for (UpdateItem item = updateItems.getFirst(i, classId); item != null; item = item.getNextInClassification(i)) {
                            positions.add(item.getReadUId());
                        }
                    }
                    rma2Modifier.addToClassification(classId, weight, positions);
                    progressListener.incrementProgress();
                }
            } finally {
                rma2Modifier.finishClassificationSection();
            }
        }
        rma2Modifier.close();
    }

    /**
     * get all reads that match the given expression
     *
     * @param regEx
     * @param findSelection where to search for matches
     * @param canceled
     * @return getLetterCodeIterator over reads that match
     * @throws java.io.IOException
     */
    public IReadBlockIterator getFindAllReadsIterator(String regEx, FindSelection findSelection, Single<Boolean> canceled) throws IOException {
        return new FindAllReadsIterator(regEx, findSelection, getAllReadsIterator(0, 10, true, true), canceled);
    }

    /**
     * gets the number of reads
     *
     * @return number of reads
     * @throws java.io.IOException
     */
    public int getNumberOfReads() throws IOException {
        return (new RMA2File(file)).getNumberOfReads();
    }

    /**
     * get the total number of matches
     *
     * @return number of matches
     */
    public int getNumberOfMatches() throws IOException {
        return (new RMA2File(file)).getNumberOfMatches();
    }

    /**
     * sets the number of reads. Note that the logical number of reads may differ from
     * the actual number of reads stored, so this needs to be stored explicitly
     *
     * @param numberOfReads
     * @throws java.io.IOException
     */
    public void setNumberOfReads(int numberOfReads) throws IOException {
        (new RMA2File(file)).setNumberOfReads(numberOfReads);
    }

    /**
     * puts the MEGAN auxiliary data associated with the dataset
     *
     * @param label2data
     * @throws java.io.IOException
     */
    public void putAuxiliaryData(Map<String, byte[]> label2data) throws IOException {
        (new RMA2File(file)).replaceAuxiliaryData(label2data);
    }

    /**
     * gets the MEGAN auxiliary data associated with the dataset
     *
     * @return auxiliaryData
     * @throws java.io.IOException
     */
    public Map<String, byte[]> getAuxiliaryData() throws IOException {
        return (new RMA2File(file)).getAuxiliaryData();
    }
}
