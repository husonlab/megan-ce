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
package megan.biom.biom2;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.BlastMode;
import jloda.util.ProgramProperties;
import megan.classification.Classification;
import megan.classification.IdMapper;
import megan.core.DataTable;
import megan.core.Document;
import megan.core.MeganFile;
import megan.util.BiomFileFilter;
import megan.viewer.MainViewer;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * parses a file in biom format
 * Daniel Huson, 9.2012
 */
public class Biom2Importer {
    /**
     * apply the biom2 importer to the given file
     *
     * @param fileName
     * @param doc
     * @param type
     */
    static public void apply(String fileName, Document doc, String type, boolean ignorePathAbove) throws IOException {
        if (!BiomFileFilter.isBiom2File(fileName)) {
            throw new IOException("File not in BIOM2 format (or incorrect file suffix?)");
        }

        System.err.println("Importing data from BIOM2 file");
        try (IHDF5Reader reader = HDF5Factory.openForReading(fileName)) {
            final TopLevelAttributes topLevelAttributes = new TopLevelAttributes(reader);
            System.err.println(topLevelAttributes.toString());

            if (topLevelAttributes.getShape().length > 0 && topLevelAttributes.getShape()[0] > 200000)
                throw new IOException("Too many rows,shape=" + Basic.toString(topLevelAttributes.getShape(), ", "));

            final String[] sampleIds = reader.readStringArray("/sample/ids"); // dataset of the sample IDs
            final int numberOfSamples = sampleIds.length;

            final Map<String, Map<Integer, float[]>> classification2class2sample2count = new HashMap<>();

            if (ImportBiom2Taxonomy.hasTaxonomyMetadata(reader)) {
                final Map<Integer, float[]> class2sample2count = ImportBiom2Taxonomy.getClass2Samples2Counts(reader, numberOfSamples, ignorePathAbove);
                classification2class2sample2count.put(Classification.Taxonomy, class2sample2count);
            }
            // todo: add parsing of other classifications here

            final DataTable datatTable = doc.getDataTable();
            datatTable.clear();
            datatTable.setCreator(ProgramProperties.getProgramName());
            datatTable.setCreationDate((new Date()).toString());

            final float[] sizes;
            if (classification2class2sample2count.containsKey(Classification.Taxonomy))
                sizes = computeSizes(numberOfSamples, classification2class2sample2count.get(Classification.Taxonomy));
            else if (classification2class2sample2count.size() > 0)
                sizes = computeSizes(numberOfSamples, classification2class2sample2count.values().iterator().next());
            else {
                sizes = null;
                throw new IOException("Unsupported data, please report on megan.informatik.uni-tuebingen.de");
            }

            final float totalReads;
            totalReads = Basic.getSum(sizes);

            doc.getActiveViewers().addAll(classification2class2sample2count.keySet());

            doc.getMeganFile().setFileType(MeganFile.Type.MEGAN_SUMMARY_FILE);

            datatTable.getClassification2Class2Counts().putAll(classification2class2sample2count);

            if (!classification2class2sample2count.containsKey(Classification.Taxonomy)) {
                final Map<Integer, float[]> class2counts = new HashMap<>();
                class2counts.put(IdMapper.UNASSIGNED_ID, sizes);
                datatTable.getClassification2Class2Counts().put(Classification.Taxonomy, class2counts);
            }

            datatTable.setSamples(sampleIds, null, sizes, new BlastMode[]{BlastMode.Classifier});
            datatTable.setTotalReads(Math.round(totalReads));
            doc.setNumberReads(Math.round(totalReads));

            // read the meta data, if available:
            final int metaDataCount = Biom2MetaData.read(reader, sampleIds, doc.getSampleAttributeTable());

            System.err.println("done (" + totalReads + " reads)");

            final String message = "Imported " + totalReads + " reads, " + classification2class2sample2count.size() + " classifications, "
                    + metaDataCount + " attributes" + "\nGenerated by " + topLevelAttributes.getGeneratedBy()
                    + ", date: " + topLevelAttributes.getCreationDate();

            NotificationsInSwing.showInformation(MainViewer.getLastActiveFrame(), message);
        }
    }

    /**
     * determines the total sample sizes
     *
     * @param numberOfSamples
     * @param class2sample2count
     * @return sample sizes
     */
    private static float[] computeSizes(int numberOfSamples, Map<Integer, float[]> class2sample2count) {
        final float[] sizes = new float[numberOfSamples];
        for (float[] array : class2sample2count.values()) {
            for (int i = 0; i < array.length; i++) {
                sizes[i] += array[i];
            }
        }
        return sizes;
    }

    /**
     * get the entry, if it exists, otherwise create it and initialize to zeros
     *
     * @param map
     * @param id
     * @param size
     * @return entry
     */
    private static Integer[] getOrCreate(Map<Integer, Integer[]> map, Integer id, int size) {
        Integer[] result = map.computeIfAbsent(id, k -> newZeroedIntegerArray(size));
        return result;
    }

    /**
     * add all values to sum
     *
     * @param sum
     * @param add
     */
    private static void addToArray(Integer[] sum, int[] add) {
        for (int i = 0; i < add.length; i++) {
            sum[i] += add[i];
        }
    }

    /**
     * create new array with zero entries
     *
     * @param size
     * @return new array
     */
    private static Integer[] newZeroedIntegerArray(int size) {
        Integer[] result = new Integer[size];
        for (int i = 0; i < size; i++)
            result[i] = 0;
        return result;
    }
}
