/*
 * AccessClassificationsDAA.java Copyright (C) 2022 Daniel H. Huson
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

package megan.daa.io;

import jloda.util.ListOfLongs;
import megan.daa.connector.ClassificationBlockDAA;
import megan.data.IClassificationBlock;
import megan.io.FileRandomAccessReadOnlyAdapter;

import java.io.IOException;
import java.util.Collection;

/**
 * access classifications in a DAA file
 * Daniel Huson, 8.2015
 */
public class AccessClassificationsDAA {

    /**
     * load all query locations for a given classification and class ids
     *
     * @param daaHeader
     * @param classificationName
     * @throws IOException
     */
    public static ListOfLongs loadQueryLocations(DAAHeader daaHeader, String classificationName, Collection<Integer> classIds) throws IOException {
        for (int i = 0; i < daaHeader.getBlockTypeRankArrayLength() - 1; i++) {
            final int j = i + 1;
            if (daaHeader.getBlockType(i) == BlockType.megan_classification_key_block && daaHeader.getBlockType(j) == BlockType.megan_classification_dump_block) {
                try (InputReaderLittleEndian insKey = new InputReaderLittleEndian(new FileRandomAccessReadOnlyAdapter(daaHeader.getFileName()))) {
                    final long keyBase = daaHeader.computeBlockStart(i);
                    insKey.seek(keyBase);
                    final String cName = insKey.readNullTerminatedBytes();

                    if (cName.equals(classificationName)) {
                        final int numberOfClasses = insKey.readInt();
                        final ListOfLongs list = new ListOfLongs(100000);

                        try (InputReaderLittleEndian insDump = new InputReaderLittleEndian(new FileRandomAccessReadOnlyAdapter(daaHeader.getFileName()))) {
                            final long dumpBase = daaHeader.computeBlockStart(j);
                            insDump.seek(dumpBase);
                            if (!insDump.readNullTerminatedBytes().equals(classificationName))
                                throw new IOException("Internal error: key-dump mismatch");

                            for (int c = 0; c < numberOfClasses; c++) {
                                int classId = insKey.readInt();
                                insKey.skip(4); //  weight
                                int size = insKey.readInt();
                                final long offset = insKey.readLong();
                                if (classIds.contains(classId)) {
                                    insDump.seek(dumpBase + offset);
                                    for (int n = 0; n < size; n++) {
                                        list.add(insDump.readLong());
                                    }
                                }
                            }
                        }
                        return list;
                    }
                }
            }
        }
        return null;
    }

    /**
     * load a named classification block
     *
     * @param daaHeader
     * @param classificationName
     * @return classification
     * @throws IOException
     */
    public static IClassificationBlock loadClassification(DAAHeader daaHeader, String classificationName) throws IOException {
        for (int i = 0; i < daaHeader.getBlockTypeRankArrayLength() - 1; i++) {
            if (daaHeader.getBlockType(i) == BlockType.megan_classification_key_block) {
                long keyBase = daaHeader.computeBlockStart(i);

                try (InputReaderLittleEndian ins = new InputReaderLittleEndian(new FileRandomAccessReadOnlyAdapter(daaHeader.getFileName()))) {
                    ins.seek(keyBase);
                    String cName = ins.readNullTerminatedBytes();
                    if (cName.equalsIgnoreCase(classificationName)) {
                        final ClassificationBlockDAA classificationBlock = new ClassificationBlockDAA(classificationName);
                        int numberOfClasses = ins.readInt();

                        for (int c = 0; c < numberOfClasses; c++) {
                            int classId = ins.readInt();
                            classificationBlock.setWeightedSum(classId, ins.readInt());
                            classificationBlock.setSum(classId, ins.readInt());
                            ins.skipBytes(8); // skip offset
                        }
                        return classificationBlock;
                    }
                }
            }
        }
        return null;
    }
}
