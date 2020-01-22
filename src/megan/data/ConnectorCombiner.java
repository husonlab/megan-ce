/*
 * ConnectorCombiner.java Copyright (C) 2020. Daniel H. Huson
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

import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import jloda.util.Single;
import megan.core.MeganFile;
import megan.rma6.ClassificationBlockRMA6;

import java.io.IOException;
import java.util.*;

/**
 * Combines multiple connectors into one
 * Daniel Huson, 5.2017
 */
public class ConnectorCombiner implements IConnector {
    private IConnector[] connectors;

    /**
     * constructor
     *
     * @param fileNames
     * @throws IOException
     */
    public ConnectorCombiner(ArrayList<String> fileNames) throws IOException {
        System.err.println("Using experimental comparison data access");
        final ArrayList<IConnector> list = new ArrayList<>();
        for (String fileName : fileNames) {
            final MeganFile meganFile = new MeganFile();
            meganFile.setFileFromExistingFile(fileName, true);
            if (meganFile.isOkToRead()) {
                final IConnector connector = meganFile.getConnector();
                if (connector != null)
                    list.add(connector);
            }
        }
        connectors = list.toArray(new IConnector[0]);
    }

    public static boolean canOpenAllConnectors(ArrayList<String> fileNames) throws IOException {
        for (String fileName : fileNames) {
            final MeganFile meganFile = new MeganFile();
            meganFile.setFileFromExistingFile(fileName, true);
            if (!meganFile.isOkToRead() || meganFile.getConnector() == null)
                return false;
        }
        return fileNames.size() > 0;
    }

    @Override
    public void setFile(String fileName) throws IOException {
        throw new IOException("Can't set file for combined document");
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public long getUId() {
        return 0;
    }

    @Override
    public IReadBlockIterator getAllReadsIterator(final float minScore, final float maxExpected, final boolean wantReadSequence, final boolean wantMatches) throws IOException {
        final IReadBlockIterator[] iterators = new IReadBlockIterator[connectors.length];
        for (int i = 0; i < connectors.length; i++) {
            iterators[i] = connectors[i].getAllReadsIterator(minScore, maxExpected, wantReadSequence, wantMatches);
        }
        return new ReadBlockIteratorCombiner(iterators);
    }

    @Override
    public IReadBlockIterator getReadsIterator(final String classification, final int classId, final float minScore, final float maxExpected, final boolean wantReadSequence, final boolean wantMatches) throws IOException {
        final IReadBlockIterator[] iterators = new IReadBlockIterator[connectors.length];
        for (int i = 0; i < connectors.length; i++) {
            iterators[i] = connectors[i].getReadsIterator(classification, classId, minScore, maxExpected, wantReadSequence, wantMatches);
        }
        return new ReadBlockIteratorCombiner(iterators);
    }

    @Override
    public IReadBlockIterator getReadsIteratorForListOfClassIds(String classification, Collection<Integer> classIds, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        final ArrayList<IReadBlockIterator> iterators = new ArrayList<>();
        for (IConnector connector : connectors) {
            if (connector != null)
                iterators.add(connector.getReadsIteratorForListOfClassIds(classification, classIds, minScore, maxExpected, wantReadSequence, wantMatches));
        }
        return new ReadBlockIteratorCombiner(iterators.toArray(new IReadBlockIterator[0]));
    }

    @Override
    public IReadBlockGetter getReadBlockGetter(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        final IReadBlockGetter[] readBlockGetters = new IReadBlockGetter[connectors.length];
        long sum = 0;
        for (int i = 0; i < connectors.length; i++) {
            readBlockGetters[i] = connectors[i].getReadBlockGetter(minScore, maxExpected, wantReadSequence, wantMatches);
            sum += readBlockGetters[i].getCount();
        }
        final long count = sum;

        return new IReadBlockGetter() {
            @Override
            public IReadBlock getReadBlock(long uid) throws IOException {
                int which = (int) ((uid >> 54) & 511); // we use the top bits of the UID to track which connector to use.
                uid = (uid & ((1L << 54) - 1));
                return readBlockGetters[which].getReadBlock(uid);
            }

            @Override
            public void close() {
                for (int i = 0; i < connectors.length; i++) {
                    readBlockGetters[i].close();
                }
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }

    @Override
    public String[] getAllClassificationNames() throws IOException {
        final Set<String> names = new TreeSet<>();
        for (IConnector connector : connectors) {
            names.addAll(Arrays.asList(connector.getAllClassificationNames()));
        }
        return names.toArray(new String[0]);
    }

    @Override
    public int getClassificationSize(String classificationName) throws IOException {
        int size = 0;
        for (IConnector connector : connectors) {
            if (Basic.getIndex(classificationName, connector.getAllClassificationNames()) != -1) {
                size += connector.getClassificationSize(classificationName);
            }
        }
        return size;
    }

    @Override
    public int getClassSize(String classificationName, int classId) throws IOException {
        int size = 0;
        for (IConnector connector : connectors) {
            if (Basic.getIndex(classificationName, connector.getAllClassificationNames()) != -1) {
                size += connector.getClassSize(classificationName, classId);
            }
        }
        return size;
    }

    @Override
    public IClassificationBlock getClassificationBlock(String classificationName) throws IOException {
        final ClassificationBlockRMA6 result = new ClassificationBlockRMA6(classificationName);
        for (IConnector connector : connectors) {
            IClassificationBlock classificationBlock = connector.getClassificationBlock(classificationName);
            for (int key : classificationBlock.getKeySet()) {
                result.setSum(key, result.getSum(key) + classificationBlock.getSum(key));
                result.setWeightedSum(key, result.getWeightedSum(key) + classificationBlock.getWeightedSum(key));
            }
        }
        return result;
    }

    @Override
    public void updateClassifications(String[] classificationNames, List<UpdateItem> updateItems, ProgressListener progressListener) throws IOException, CanceledException {
        throw new IOException("Can't updateClassifications() for combined document");
    }

    @Override
    public IReadBlockIterator getFindAllReadsIterator(String regEx, FindSelection findSelection, Single<Boolean> canceled) throws IOException {
        final IReadBlockIterator[] iterators = new IReadBlockIterator[connectors.length];
        for (int i = 0; i < connectors.length; i++) {
            iterators[i] = connectors[i].getFindAllReadsIterator(regEx, findSelection, canceled);
        }
        return new ReadBlockIteratorCombiner(iterators);
    }

    @Override
    public int getNumberOfReads() throws IOException {
        int size = 0;
        for (IConnector connector : connectors) {
            size += connector.getNumberOfReads();
        }
        return size;
    }

    @Override
    public int getNumberOfMatches() throws IOException {
        int size = 0;
        for (IConnector connector : connectors) {
            size += connector.getNumberOfMatches();
        }
        return size;
    }

    @Override
    public void setNumberOfReads(int numberOfReads) throws IOException {
        System.err.println("Can't setNumberOfReads() for combined document");
    }

    @Override
    public void putAuxiliaryData(Map<String, byte[]> label2data) throws IOException {
        System.err.println("Can't putAuxiliaryData() for combined document");
    }

    @Override
    public Map<String, byte[]> getAuxiliaryData() throws IOException {
        System.err.println("Can't getAuxiliaryData() for combined document");
        return null;
    }

}

