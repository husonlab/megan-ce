/*
 * RMA2Connector.java Copyright (C) 2023 Daniel H. Huson
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
package megan.rma2;

import jloda.util.Single;
import jloda.util.progress.ProgressListener;
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
	 */
    public RMA2Connector(String fileName) throws IOException {
        setFile(fileName);
    }

    @Override
    public String getFilename() {
        return file.getPath();
    }

    /**
     * set the file name of interest. Only one file can be used.
     *
	 */
    public void setFile(String filename) {
        file = new File(filename);
    }

    /**
     * is connected document readonly?
     *
     * @return true, if read only
     */
    public boolean isReadOnly() {
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
     * @param wantReadSequence @param wantMatches specifies what data to return  in ReadBlock
     * @return getLetterCodeIterator over all reads
	 */
    public IReadBlockIterator getAllReadsIterator(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return new AllReadsIterator(getReadBlockGetter(minScore, maxExpected, wantReadSequence, wantMatches));
    }

    /**
     * get getLetterCodeIterator over all reads for given classification and classId.
     *
     * @param wantReadSequence @param wantMatches  specifies what data to return  in ReadBlock
     * @return getLetterCodeIterator over reads in class
	 */
    public IReadBlockIterator getReadsIterator(String classification, int classId, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return getReadsIteratorForListOfClassIds(classification, Collections.singletonList(classId), minScore, maxExpected, wantReadSequence, wantMatches);
    }

    /**
     * get getLetterCodeIterator over all reads for given classification and a collection of classids. If minScore=0  no filtering
     *
     * @param wantReadSequence @param wantMatches
     * @return getLetterCodeIterator over reads filtered by given parameters
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
     * @return number of classes
	 */
    public int getClassificationSize(String classificationName) throws IOException {
        return (new RMA2File(file)).getClassificationSize(classificationName);
    }

    /**
     * gets the number of reads in a given class
     *
     * @return number of reads
	 */
    public int getClassSize(String classificationName, int classId) throws IOException {
        ClassificationBlockRMA2 classificationBlock = (new RMA2File(file)).getClassificationBlock(classificationName);
        // todo: this is very wasteful, better to read in the block once and then keep it...
        return classificationBlock.getSum(classId);
    }

    /**
     * gets the named classification block
     *
     * @return classification block
	 */
    public IClassificationBlock getClassificationBlock(String classificationName) throws IOException {
        return (new RMA2File(file)).getClassificationBlock(classificationName);
    }

    /**
     * updates the classId values for a collection of reads
     *
     * @param names          names of classifications in the order that their values will appear in
     * @param updateItemList list of rescan items
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
     * @param findSelection where to search for matches
     * @return getLetterCodeIterator over reads that match
	 */
    public IReadBlockIterator getFindAllReadsIterator(String regEx, FindSelection findSelection, Single<Boolean> canceled) throws IOException {
        return new FindAllReadsIterator(regEx, findSelection, getAllReadsIterator(0, 10, true, true), canceled);
    }

    /**
     * gets the number of reads
     *
     * @return number of reads
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
	 */
    public void setNumberOfReads(int numberOfReads) throws IOException {
        (new RMA2File(file)).setNumberOfReads(numberOfReads);
    }

    /**
     * puts the MEGAN auxiliary data associated with the dataset
     *
	 */
    public void putAuxiliaryData(Map<String, byte[]> label2data) throws IOException {
        (new RMA2File(file)).replaceAuxiliaryData(label2data);
    }

    /**
     * gets the MEGAN auxiliary data associated with the dataset
     *
     * @return auxiliaryData
	 */
    public Map<String, byte[]> getAuxiliaryData() throws IOException {
        return (new RMA2File(file)).getAuxiliaryData();
    }
}
