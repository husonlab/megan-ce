/*
 * DAAConnector.java Copyright (C) 2020. Daniel H. Huson
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
package megan.daa.connector;

import jloda.util.CanceledException;
import jloda.util.ListOfLongs;
import jloda.util.ProgressListener;
import jloda.util.Single;
import megan.daa.io.*;
import megan.data.*;
import megan.io.InputStreamAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * DAA connector
 * Daniel Huson, 8.2015
 */
public class DAAConnector implements IConnector {
    private String fileName;
    private DAAHeader daaHeader;

    public static boolean openDAAFileOnlyIfMeganized = true; // only allow DAA files that have been Meganized
    public static final Object syncObject = new Object(); // use for changing openDAAFileOnlyIfMeganized

    private boolean longReads = false;

    public static boolean reuseReadBlockInGetter=true;


    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     */
    public DAAConnector(String fileName) throws IOException {
        setFile(fileName);
        if (openDAAFileOnlyIfMeganized && !isMeganized())
            throw new IOException("DAA file has not been meganized: " + fileName);
    }

    @Override
    public void setFile(String file) throws IOException {
        this.fileName = file;
        this.daaHeader = new DAAHeader(fileName);
        daaHeader.load();
    }

    @Override
    public boolean isReadOnly() {
        return fileName != null && ((new File(fileName)).canWrite());
    }

    @Override
    public long getUId() throws IOException {
        return Files.readAttributes(Paths.get(fileName), BasicFileAttributes.class).creationTime().toMillis();
    }

    @Override
    public IReadBlockIterator getAllReadsIterator(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return new AllReadsIterator(new ReadBlockGetterDAA(daaHeader, wantReadSequence, wantMatches, minScore, maxExpected, true, false, longReads));
    }

    @Override
    public IReadBlockIterator getReadsIterator(String classification, int classId, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return getReadsIteratorForListOfClassIds(classification, Collections.singletonList(classId), minScore, maxExpected, wantReadSequence, wantMatches);
    }

    @Override
    public IReadBlockIterator getReadsIteratorForListOfClassIds(String classification, Collection<Integer> classIds, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        ListOfLongs list = AccessClassificationsDAA.loadQueryLocations(daaHeader, classification, classIds);
        if (list == null)
            list = new ListOfLongs();
        return new ReadBlockIterator(list.iterator(), list.size(), getReadBlockGetter(minScore, maxExpected, wantReadSequence, wantMatches));
    }

    @Override
    public IReadBlockIterator getFindAllReadsIterator(String regEx, FindSelection findSelection, Single<Boolean> canceled) throws IOException {
        return new FindAllReadsIterator(regEx, findSelection, getAllReadsIterator(0, 10, true, true), canceled);
    }

    @Override
    public IReadBlockGetter getReadBlockGetter(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return new ReadBlockGetterDAA(daaHeader, wantReadSequence, wantMatches, minScore, maxExpected, false, reuseReadBlockInGetter, longReads);
    }

    @Override
    public String[] getAllClassificationNames() {
        return daaHeader.getRefAnnotationNames();
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
        return AccessClassificationsDAA.loadClassification(daaHeader, classificationName);
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
        for (int i = 0; i < cNames.length; i++) {
            fName2ClassId2Location[i] = new HashMap<>(10000);
            fName2ClassId2Weight[i] = new HashMap<>(10000);
        }

        for (int i = 0; i < cNames.length; i++) {
            final Map<Integer, ListOfLongs> classId2Location = fName2ClassId2Location[i];
            final Map<Integer, Float> classId2weight = fName2ClassId2Weight[i];

            for (Integer classId : updateItems.getClassIds(i)) {
                classId2weight.put(classId, updateItems.getWeight(i, classId));
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
        ModifyClassificationsDAA.saveClassifications(daaHeader, cNames, fName2ClassId2Location, fName2ClassId2Weight);
    }

    @Override
    public int getNumberOfReads() throws IOException {
        DAAHeader daaHeader = new DAAHeader(fileName);
        daaHeader.load();
        return (int) daaHeader.getQueryRecords();
    }

    @Override
    public int getNumberOfMatches() {
        return 0; // todo: fix
        /*
        DAAHeader daaHeader=new DAAHeader(fileName);
        daaHeader.load();

        return (int)daaHeader.getNumberOfAlignments();
        */
    }

    @Override
    public void setNumberOfReads(int numberOfReads) {
        // todo: allow user to change number of reads
        //System.err.println("Not implemented");
    }

    @Override
    public void putAuxiliaryData(Map<String, byte[]> label2data) throws IOException {
        final ByteOutputStream outs = new ByteOutputStream(100000);
        final OutputWriterLittleEndian w = new OutputWriterLittleEndian(outs);

        w.writeInt(label2data.size());
        for (String label : label2data.keySet()) {
            w.writeNullTerminatedString(label.getBytes());
            byte[] bytes = label2data.get(label);
            w.writeInt(bytes.length);
            w.write(bytes, 0, bytes.length);
        }
        DAAModifier.replaceBlock(daaHeader, BlockType.megan_aux_data, outs.getBytes(), outs.size());
    }

    @Override
    public Map<String, byte[]> getAuxiliaryData() throws IOException {
        final Map<String, byte[]> label2data = new HashMap<>();

        final byte[] block = DAAParser.getBlock(daaHeader, BlockType.megan_aux_data);
        if (block != null) {
            try (final InputReaderLittleEndian ins = new InputReaderLittleEndian(new InputStreamAdapter(new ByteInputStream(block, block.length)))) {
                final int numberOfLabels = ins.readInt();
                for (int i = 0; i < numberOfLabels; i++) {
                    final String label = ins.readNullTerminatedBytes();
                    final int size = ins.readInt();
                    final byte[] bytes = new byte[size];
                    final int length = ins.read_available(bytes, 0, size);
                    if (length < size) {
                        final byte[] tmp = new byte[length];
                        System.arraycopy(bytes, 0, tmp, 0, length);
                        label2data.put(label, tmp);
                        throw new IOException("buffer underflow");
                    }
                    label2data.put(label, bytes);
                }
            } catch (IOException ex) {
                System.err.println("Incomplete aux block detected, will try to recover...");
                // ignore any problems, megan should be able to recover from this
            }
        }
        return label2data;
    }

    public DAAHeader getDAAHeader() {
        return daaHeader;
    }

    public boolean isLongReads() {
        return longReads;
    }

    public void setLongReads(boolean longReads) {
        this.longReads = longReads;
    }

    public boolean isMeganized() {
        try {
            return DAAParser.isMeganizedDAAFile(fileName, true);
        } catch (Exception e) {
            return false;
        }
    }
}
