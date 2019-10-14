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
package megan.biom.biom1;

import jloda.swing.window.NotificationsInSwing;
import jloda.util.BlastMode;
import jloda.util.ProgramProperties;
import megan.classification.Classification;
import megan.classification.IdMapper;
import megan.core.ClassificationType;
import megan.core.DataTable;
import megan.core.Document;
import megan.core.MeganFile;
import megan.util.BiomFileFilter;
import megan.viewer.MainViewer;

import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * import a file in biom1 format
 * Daniel Huson, 9.2012
 */
public class BIOM1Importer {
    /**
     * apply the importer parser to the named file.
     *
     * @param fileName
     * @param doc
     * @param type
     */
    static public void apply(String fileName, Document doc, String type, boolean taxonomyIgnorePath) throws IOException {
        doc.getMeganFile().setFileType(MeganFile.Type.MEGAN_SUMMARY_FILE);

        if (!BiomFileFilter.isBiom1File(fileName)) {
            throw new IOException("File not in BIOM1 format (or incorrect file suffix?)");
        }

        System.err.println("Importing data from BIOM1 file");

        Biom1Data biom1Data = Biom1Data.fromReader(new FileReader(fileName));

        String[] names = biom1Data.getColumnIds();

        Map<String, Map<Integer, Integer>> series2Classes2count;

        String classificationName;
        if (type.equalsIgnoreCase("taxonomy") || biom1Data.isTaxonomyData() || biom1Data.isOTUData()) {
            series2Classes2count = Biom1ImportTaxonomy.getSample2Class2Value(biom1Data, taxonomyIgnorePath);
            classificationName = Classification.Taxonomy;
        } else if (type.equalsIgnoreCase("seed") || biom1Data.isSEEDData()) {
            series2Classes2count = Biom1ImportSEED.getSeries2Classes2Value(biom1Data);
            classificationName = "SEED";
        } else if (type.equalsIgnoreCase("kegg") || biom1Data.isKEGGData()) {
            series2Classes2count = Biom1ImportKEGG.getSeries2Classes2Value(biom1Data);
            classificationName = "KEGG";
        } else
            throw new IOException("Unable to import this datatype: " + biom1Data.getType());

        System.err.println("Classification type is: " + classificationName);

        DataTable table = doc.getDataTable();
        table.clear();
        table.setCreator(ProgramProperties.getProgramName());
        table.setCreationDate((new Date()).toString());

        doc.getActiveViewers().add(classificationName);

        final Map<Integer, float[]> targetClass2counts = new HashMap<>();

        int totalReads = 0;
        int numberOfSeries = series2Classes2count.keySet().size();
        final float[] sizes = new float[numberOfSeries];

        final Map<String, Integer> series2pid = new HashMap<>();
        final String[] columnIds = biom1Data.getColumnIds();
        for (int c = 0; c < columnIds.length; c++)
            series2pid.put(columnIds[c], c);

        for (String series : series2Classes2count.keySet()) {
            int seriesId = series2pid.get(series);
            final Map<Integer, Integer> class2count = series2Classes2count.get(series);
            for (Integer classId : class2count.keySet()) {
                Integer count = class2count.get(classId);
                if (count == null)
                    count = 0;

                float[] counts = targetClass2counts.computeIfAbsent(classId, k -> new float[numberOfSeries]);
                counts[seriesId] = count;
                totalReads += count;
                sizes[seriesId] += count;
            }
        }

        table.getClassification2Class2Counts().put(classificationName, targetClass2counts);

        if (!classificationName.equals(ClassificationType.Taxonomy.toString())) {
            final Map<Integer, float[]> class2counts = new HashMap<>();
            class2counts.put(IdMapper.UNASSIGNED_ID, sizes);
            table.getClassification2Class2Counts().put(ClassificationType.Taxonomy.toString(), class2counts);
        }

        table.setSamples(names, null, sizes, new BlastMode[]{BlastMode.Classifier});
        table.setTotalReads(totalReads);
        doc.setNumberReads(totalReads);

        System.err.println("done (" + totalReads + " reads)");

        NotificationsInSwing.showInformation(MainViewer.getLastActiveFrame(), "Imported " + totalReads + " reads, as " + classificationName + " classification"
                + "\nGenerated by " + biom1Data.generated_by
                + "\nDate: " + biom1Data.getDate()
                + (biom1Data.getComment() != null ? "\nComment: " + biom1Data.getComment() : ""));
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
