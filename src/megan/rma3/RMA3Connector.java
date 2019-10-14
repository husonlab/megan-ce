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
package megan.rma3;

import jloda.util.*;
import megan.core.ClassificationType;
import megan.data.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * RMA3 connector
 * Created by huson on 5/16/14.
 */
public class RMA3Connector implements IConnector {
    private String fileName;

    /**
     * constructor
     *
     * @param fileName
     * @throws java.io.IOException
     */
    public RMA3Connector(String fileName) throws IOException {
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
        try (RMA3File rma3File = new RMA3File(fileName, RMA3File.READ_ONLY)) {
            return rma3File.getFileHeader().getCreationDate();
        }
    }

    @Override
    public IReadBlockIterator getAllReadsIterator(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return new AllReadsIterator(getReadBlockGetter(minScore, maxExpected, wantReadSequence, wantMatches));
    }

    @Override
    public IReadBlockIterator getReadsIterator(String classification, int classId, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return getReadsIteratorForListOfClassIds(classification, Collections.singletonList(classId), minScore, maxExpected, wantReadSequence, wantMatches);
    }

    @Override
    public IReadBlockIterator getReadsIteratorForListOfClassIds(String classification, Collection<Integer> classIds, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        try (RMA3File rma3File = new RMA3File(fileName, RMA3File.READ_ONLY)) {
            final ClassificationBlockRMA3 block = new ClassificationBlockRMA3(ClassificationType.valueOf(classification));
            block.read(rma3File.getClassificationsFooter(), rma3File.getReader());
            ListOfLongs list = new ListOfLongs();
            for (Integer classId : classIds) {
                if (block.getSum(classId) > 0) {
                    block.readLocations(rma3File.getClassificationsFooter(), rma3File.getReader(), classId, list);
                }
            }
            return new ReadBlockIterator(list, getReadBlockGetter(minScore, maxExpected, wantReadSequence, wantMatches));
        }
    }

    @Override
    public IReadBlockGetter getReadBlockGetter(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        final RMA3File rma3File = new RMA3File(fileName, RMA3File.READ_ONLY);
        return new ReadBlockGetterRMA3(rma3File, minScore, maxExpected, wantReadSequence, wantMatches);
    }

    @Override
    public String[] getAllClassificationNames() throws IOException {
        try (RMA3File rma3File = new RMA3File(fileName, RMA3File.READ_ONLY)) {
            List<String> names = rma3File.getClassificationsFooter().getAllNames();
            return names.toArray(new String[names.size()]);
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
        try (RMA3File rma3File = new RMA3File(fileName, RMA3File.READ_ONLY)) {
            ClassificationBlockRMA3 classificationBlock = new ClassificationBlockRMA3(ClassificationType.valueOf(classificationName));
            classificationBlock.read(rma3File.getClassificationsFooter(), rma3File.getReader());
            return classificationBlock;
        }
    }

    /**
     * rescan classifications after running the data processor
     *
     * @param names
     * @param updateItemList
     * @param progressListener
     * @throws IOException
     * @throws CanceledException
     */
    @Override
    public void updateClassifications(String[] names, List<UpdateItem> updateItemList, ProgressListener progressListener) throws IOException, CanceledException {
        final UpdateItemList updateItems = (UpdateItemList) updateItemList;

        final int numClassifications = names.length;

        long maxProgress = 0;
        for (int i = 0; i < numClassifications; i++) {
            maxProgress += updateItems.getClassIds(i).size();
        }
        progressListener.setMaximum(maxProgress);

        RMA3FileModifier rma3FileModifier = new RMA3FileModifier(fileName);
        rma3FileModifier.startModification();

        for (int i = 0; i < numClassifications; i++) {
            if (Basic.toString(ClassificationType.values(), " ").contains(names[i])) {
                ClassificationType classificationType = ClassificationType.valueOf(names[i]);

                final Map<Integer, ListOfLongs> classId2Locations = new HashMap<>();
                for (Integer classId : updateItems.getClassIds(i)) {
                    float weight = updateItems.getWeight(i, classId);
                    final ListOfLongs positions = new ListOfLongs();
                    classId2Locations.put(classId, positions);
                    if (updateItems.getWeight(i, classId) > 0) {
                        for (UpdateItem item = updateItems.getFirst(i, classId); item != null; item = item.getNextInClassification(i)) {
                            positions.add(item.getReadUId());
                        }
                    }
                    progressListener.incrementProgress();
                }
                rma3FileModifier.updateClassification(classificationType, classId2Locations);
            } else
                System.err.println("Unsupported classification type: " + names[i]);
        }
        rma3FileModifier.finishModification();
    }

    @Override
    public IReadBlockIterator getFindAllReadsIterator(String regEx, FindSelection findSelection, Single<Boolean> canceled) throws IOException {
        return new FindAllReadsIterator(regEx, findSelection, getAllReadsIterator(0, 10, true, true), canceled);
    }

    @Override
    public int getNumberOfReads() throws IOException {
        try (RMA3File rma3File = new RMA3File(fileName, RMA3File.READ_ONLY)) {
            return (int) Math.min(Integer.MAX_VALUE, rma3File.getMatchFooter().getNumberOfReads());
        }
    }

    @Override
    public int getNumberOfMatches() throws IOException {
        try (RMA3File rma3File = new RMA3File(fileName, RMA3File.READ_ONLY)) {
            return (int) Math.min(Integer.MAX_VALUE, rma3File.getMatchFooter().getNumberOfMatches());
        }
    }

    @Override
    public void setNumberOfReads(int numberOfReads) throws IOException {
    }

    @Override
    public void putAuxiliaryData(Map<String, byte[]> label2data) throws IOException {
        try (RMA3FileModifier rma3FileModifier = new RMA3FileModifier(fileName)) {
            rma3FileModifier.saveAuxData(label2data);
        }
    }

    @Override
    public Map<String, byte[]> getAuxiliaryData() throws IOException {
        final RMA3File rma3File = new RMA3File(fileName, RMA3File.READ_ONLY);
        final Map<String, byte[]> label2data = new HashMap<>();
        try {
            rma3File.getAuxBlocksFooter().readAuxBlocks(rma3File.getFileFooter(), rma3File.getReader(), label2data);
        } finally {
            rma3File.close();
        }
        return label2data;
    }
}
