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
package megan.core;

import jloda.util.Basic;
import jloda.util.BlastMode;
import jloda.util.ProgramProperties;
import megan.classification.IdMapper;
import megan.data.IClassificationBlock;
import megan.data.IConnector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * synchronize between archive and data table
 * Daniel Huson, 6.2010
 */
public class SyncArchiveAndDataTable {

    /**
     * synchronizes recomputed data from an archive to a summary and also parameters
     *
     * @param dataSetName
     * @param algorithmName
     * @param parameters
     * @param connector
     * @param table
     * @throws IOException
     */
    static public void syncRecomputedArchive2Summary(Document.ReadAssignmentMode readAssignmentMode, String dataSetName, String algorithmName, BlastMode blastMode, String parameters, IConnector connector, DataTable table, int additionalReads) throws IOException {
        final String[] classifications = connector.getAllClassificationNames();
        table.clear();
        table.setCreator(ProgramProperties.getProgramName());
        table.setCreationDate((new Date()).toString());
        table.setAlgorithm(ClassificationType.Taxonomy.toString(), algorithmName);
        table.setParameters(parameters);
        table.setTotalReads(connector.getNumberOfReads());
        table.setAdditionalReads(additionalReads);

        table.setSamples(new String[]{dataSetName}, new Long[]{connector.getUId()}, new float[]{connector.getNumberOfReads()}, new BlastMode[]{blastMode});
        for (String classification : classifications) {
            IClassificationBlock classificationBlock = connector.getClassificationBlock(classification);
            if (classificationBlock != null)
                syncClassificationBlock2Summary(readAssignmentMode, 0, 1, classificationBlock, table);
        }
    }

    /**
     * sync the content of an archive to the Megan4Summary. Formatting is obtained from the aux block, while
     * classifications are obtained from the classification blocks
     *
     * @param readAssignmentMode if null, first determines this from user state table
     * @param connector
     * @param table
     */
    public static void syncArchive2Summary(Document.ReadAssignmentMode readAssignmentMode, String fileName, IConnector connector, DataTable table, SampleAttributeTable sampleAttributeTable) throws IOException {
        table.clear();
        Map<String, byte[]> label2data = connector.getAuxiliaryData();
        if (label2data.containsKey(SampleAttributeTable.USER_STATE)) {
            syncAux2Summary(fileName, label2data.get(SampleAttributeTable.USER_STATE), table);
        }

        if (readAssignmentMode == null && table.getParameters() != null) {
            Document doc = new Document();
            doc.parseParameterString(table.getParameters());
            readAssignmentMode = doc.getReadAssignmentMode();
        }

        if (label2data.containsKey(SampleAttributeTable.SAMPLE_ATTRIBUTES)) {
            sampleAttributeTable.read(new StringReader(new String(label2data.get(SampleAttributeTable.SAMPLE_ATTRIBUTES))), null, true);
            if (sampleAttributeTable.getSampleSet().size() > 0) {
                String sampleName = sampleAttributeTable.getSampleSet().iterator().next();
                String name = Basic.replaceFileSuffix(Basic.getFileNameWithoutPath(fileName), "");
                if (!sampleName.equals(name))
                    sampleAttributeTable.renameSample(sampleName, name, false);
            }
        } else {
            String name = Basic.replaceFileSuffix(Basic.getFileNameWithoutPath(fileName), "");
            sampleAttributeTable.addSample(name, new HashMap<>(), true, true);
        }

        // fix some broken files that contain two lines of metadata...
        if (sampleAttributeTable.getSampleSet().size() > 1) {
            String sampleName = Basic.getFileNameWithoutPath(fileName);
            if (sampleAttributeTable.getSampleSet().contains(sampleName))
                sampleAttributeTable.removeSample(sampleName);
        }

        String[] classifications = connector.getAllClassificationNames();
        for (String classification : classifications) {
            final IClassificationBlock classificationBlock = connector.getClassificationBlock(classification);
            if (classificationBlock != null)
                syncClassificationBlock2Summary(readAssignmentMode, 0, 1, classificationBlock, table);
        }
    }

    /**
     * sync classification block to the summary
     *
     * @param dataSetId
     * @param classificationBlock
     * @param table
     */
    private static void syncClassificationBlock2Summary(Document.ReadAssignmentMode readAssignmentMode, int dataSetId, int totalDataSets, IClassificationBlock classificationBlock, DataTable table) {
        boolean useWeights = (readAssignmentMode != Document.ReadAssignmentMode.readCount);

        final Map<Integer, float[]> classId2count = new HashMap<>();
        table.setClass2Counts(classificationBlock.getName(), classId2count);

        for (Integer classId : classificationBlock.getKeySet()) {
            float sum = (useWeights ? classificationBlock.getWeightedSum(classId) : classificationBlock.getSum(classId));
            if (sum > 0) {
                classId2count.computeIfAbsent(classId, k -> new float[totalDataSets]);
                classId2count.get(classId)[dataSetId] += sum;
            }
        }
        if (table.getAdditionalReads() > 0) {
            classId2count.computeIfAbsent(IdMapper.NOHITS_ID, k -> new float[totalDataSets]);
            classId2count.get(IdMapper.NOHITS_ID)[dataSetId] += (int) table.getAdditionalReads();
        }
    }

    /**
     * sync bytes from aux block to summary
     *
     * @param bytes
     * @param table
     * @throws IOException
     */
    static private void syncAux2Summary(String fileName, byte[] bytes, DataTable table) throws IOException {
        if (bytes != null) {
            String string = Basic.toString(bytes);
            if (string.startsWith(DataTable.MEGAN6_SUMMARY_TAG_NOT_USED_ANYMORE) || string.startsWith(DataTable.MEGAN4_SUMMARY_TAG) || string.startsWith("!MEGAN4")) {
                BufferedReader r = new BufferedReader(new StringReader(string));
                table.read(r, true);
                r.close();
            } else if (string.startsWith("!MEGAN")) // is MEGAN3 summary
            {
                System.err.println("Archive is in an old format, upgrading to MEGAN6");
                BufferedReader r = new BufferedReader(new StringReader(string));
                table.importMEGAN3SummaryFile(fileName, r, false);
                r.close();
            }
        }
    }
}
