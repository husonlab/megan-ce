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

import jloda.util.CanceledException;
import jloda.util.ListOfLongs;
import jloda.util.ProgressListener;
import jloda.util.Single;
import megan.data.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * RMA6 connector
 * Created by huson on 2.2015
 */
public class RMA6Connector implements IConnector {
    private String fileName;

    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     */
    public RMA6Connector(String fileName) throws IOException {
        setFile(fileName);
    }

    @Override
    public void setFile(String file) throws IOException {
        this.fileName = file;
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return fileName != null && ((new File(fileName)).canWrite());
    }

    @Override
    public long getUId() throws IOException {
        try (RMA6File rma6File = new RMA6File(fileName, RMA6File.READ_ONLY)) {
            return rma6File.getHeaderSectionRMA6().getCreationDate();
        }
    }

    @Override
    public IReadBlockIterator getAllReadsIterator(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        final RMA6File rma6File = new RMA6File(fileName, RMA6File.READ_ONLY);
        return new AllReadsIteratorRMA6(wantReadSequence, wantMatches, rma6File, minScore, maxExpected);
    }

    @Override
    public IReadBlockIterator getReadsIterator(String classification, int classId, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return getReadsIteratorForListOfClassIds(classification, Collections.singletonList(classId), minScore, maxExpected, wantReadSequence, wantMatches);
    }

    @Override
    public IReadBlockIterator getReadsIteratorForListOfClassIds(String classification, Collection<Integer> classIds, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        try (final RMA6File rma6File = new RMA6File(fileName, RMA6File.READ_ONLY)) {
            final ClassificationBlockRMA6 block = new ClassificationBlockRMA6(classification);
            final long start = rma6File.getFooterSectionRMA6().getStartClassification(classification);
            block.read(start, rma6File.getReader());
            final ListOfLongs list = new ListOfLongs();
            for (Integer classId : classIds) {
                if (block.getSum(classId) > 0) {
                    block.readLocations(start, rma6File.getReader(), classId, list);
                }
            }
            return new ReadBlockIterator(list, getReadBlockGetter(minScore, maxExpected, wantReadSequence, wantMatches));
        }
    }

    @Override
    public IReadBlockGetter getReadBlockGetter(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        final RMA6File rma6File = new RMA6File(fileName, RMA6File.READ_ONLY);
        return new ReadBlockGetterRMA6(rma6File, wantReadSequence, wantMatches, minScore, maxExpected, false, true);
    }

    @Override
    public String[] getAllClassificationNames() throws IOException {
        try (RMA6File rma6File = new RMA6File(fileName, RMA6File.READ_ONLY)) {
            return rma6File.getHeaderSectionRMA6().getMatchClassNames();
        }
    }

    @Override
    public int getClassificationSize(String classificationName) throws IOException {
        IClassificationBlock classificationBlock = getClassificationBlock(classificationName);
        return classificationBlock.getKeySet().size();
    }

    @Override
    public int getClassSize(String classificationName, int classId) throws IOException {
        IClassificationBlock classificationBlock = getClassificationBlock(classificationName);
        return classificationBlock.getSum(classId);
    }

    @Override
    public IClassificationBlock getClassificationBlock(String classificationName) throws IOException {
        try (RMA6File rma6File = new RMA6File(fileName, RMA6File.READ_ONLY)) {
            final Long location = rma6File.getFooterSectionRMA6().getStartClassification(classificationName);
            if (location != null) {
                ClassificationBlockRMA6 classificationBlockRMA6 = new ClassificationBlockRMA6(classificationName);
                classificationBlockRMA6.read(location, rma6File.getReader());
                return classificationBlockRMA6;
            }
        }
        return null;
    }

    /**
     * rescan classifications after running the data processor
     *
     * @param cNames
     * @param updateItemList
     * @param progressListener
     * @throws IOException
     * @throws CanceledException
     */
    @Override
    public void updateClassifications(String[] cNames, List<UpdateItem> updateItemList, ProgressListener progressListener) throws IOException, CanceledException {
        final UpdateItemList updateItems = (UpdateItemList) updateItemList;

        long maxProgress = 0;
        for (int i = 0; i < cNames.length; i++) {
            maxProgress += updateItems.getClassIds(i).size();
        }
        progressListener.setMaximum(maxProgress);

        final Map<Integer, ListOfLongs>[] fName2ClassId2Location = new HashMap[cNames.length];
        final Map<Integer, Float>[] fName2ClassId2Weight = new HashMap[cNames.length];
        final Map<Integer, Integer>[] fName2ClassId2Count = new HashMap[cNames.length];
        for (int i = 0; i < cNames.length; i++) {
            fName2ClassId2Location[i] = new HashMap<>(10000);
            fName2ClassId2Weight[i] = new HashMap<>(10000);
            fName2ClassId2Weight[i] = new HashMap<>(10000);
        }

        for (int i = 0; i < cNames.length; i++) {
            final Map<Integer, ListOfLongs> classId2Location = fName2ClassId2Location[i];
            final Map<Integer, Float> classId2weight = fName2ClassId2Weight[i];

            for (Integer classId : updateItems.getClassIds(i)) {
                float weight = updateItems.getWeight(i, classId);
                classId2weight.put(classId, weight);
                final ListOfLongs positions = new ListOfLongs();
                classId2Location.put(classId, positions);
                if (updateItems.getWeight(i, classId) > 0) {
                    for (UpdateItem item = updateItems.getFirst(i, classId); item != null; item = item.getNextInClassification(i)) {
                        positions.add(item.getReadUId());
                    }
                }
                progressListener.incrementProgress();
            }
        }
        try (RMA6FileModifier rma6Modifier = new RMA6FileModifier(fileName)) {
            rma6Modifier.updateClassifications(cNames, fName2ClassId2Location, fName2ClassId2Weight);
        }
    }

    @Override
    public IReadBlockIterator getFindAllReadsIterator(String regEx, FindSelection findSelection, Single<Boolean> canceled) throws IOException {
        return new FindAllReadsIterator(regEx, findSelection, getAllReadsIterator(0, 10, true, true), canceled);
    }

    @Override
    public int getNumberOfReads() throws IOException {
        try (RMA6File rma6File = new RMA6File(fileName, RMA6File.READ_ONLY)) {
            return (int) Math.min(Integer.MAX_VALUE, rma6File.getFooterSectionRMA6().getNumberOfReads());
        }
    }

    @Override
    public int getNumberOfMatches() throws IOException {
        try (RMA6File rma6File = new RMA6File(fileName, RMA6File.READ_ONLY)) {
            return (int) Math.min(Integer.MAX_VALUE, rma6File.getFooterSectionRMA6().getNumberOfMatches());
        }
    }

    @Override
    public void setNumberOfReads(int numberOfReads) throws IOException {
    }

    @Override
    public void putAuxiliaryData(Map<String, byte[]> label2data) throws IOException {
        try (RMA6FileModifier rma6Modifier = new RMA6FileModifier(fileName)) {
            rma6Modifier.saveAuxData(label2data);
        }
    }

    @Override
    public Map<String, byte[]> getAuxiliaryData() throws IOException {
        final Map<String, byte[]> label2data;
        try (RMA6File rma6File = new RMA6File(fileName, RMA6File.READ_ONLY)) {
            label2data = new HashMap<>(rma6File.readAuxBlocks());
        }
        return label2data;
    }

}
