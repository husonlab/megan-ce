/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.biom;

import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.classification.Classification;
import megan.classification.IdMapper;
import megan.core.ClassificationType;
import megan.core.DataTable;
import megan.core.Document;
import megan.fx.NotificationsInSwing;
import megan.parsers.blast.BlastMode;
import megan.viewer.MainViewer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * parses a file in biom format
 * Daniel Huson, 9.2012
 */
public class BIOMImporter {
    /**
     * apply the importer parser to the named file.
     *  @param fileName
     * @param doc
     * @param type
     */
    static public void apply(String fileName, Document doc, String type) throws IOException {
        byte[] bytes = Basic.getFirstBytesFromFile(new File(fileName), 4);
        if (bytes == null)
            throw new IOException("Failed read file: " + fileName);
        System.err.println(Basic.toString(bytes));
        if (Basic.toString(bytes).contains("ﾉHDF")) {
            throw new IOException("File is in BIOM2 format, not supported, please first convert to BIOM1 format");
        }

        System.err.println("Importing data from BIOM file");

        BiomData biomData = BiomData.fromReader(new FileReader(fileName));

        String[] names = biomData.getColumnIds();

        Map<String, Map<Integer, Integer>> series2Classes2count;

        String classificationName;
        if (type.equalsIgnoreCase("taxonomy") || biomData.isTaxonomyData() || biomData.isOTUData()) {
            series2Classes2count = BiomImportTaxonomy.getSeries2Classes2Value(biomData);
            classificationName = Classification.Taxonomy;
        } else if (type.equalsIgnoreCase("seed") || biomData.isSEEDData()) {
            series2Classes2count = BiomImportSEED.getSeries2Classes2Value(biomData);
            classificationName = "SEED";
        } else if (type.equalsIgnoreCase("kegg") || biomData.isKEGGData()) {
            series2Classes2count = BiomImportKEGG.getSeries2Classes2Value(biomData);
            classificationName = "KEGG";
        } else
            throw new IOException("Unable to import this datatype: " + biomData.getType());

        System.err.println("Classification type is: " + classificationName);

        DataTable table = doc.getDataTable();
        table.clear();
        table.setCreator(ProgramProperties.getProgramName());
        table.setCreationDate((new Date()).toString());

        doc.getActiveViewers().add(classificationName);

        final Map<Integer, Integer[]> targetClass2counts = new HashMap<>();

        int totalReads = 0;
        int numberOfSeries = series2Classes2count.keySet().size();
        final Integer[] sizes = new Integer[numberOfSeries];
        final Map<String, Integer> series2pid = new HashMap<>();
        final String[] columnIds = biomData.getColumnIds();
        for (int c = 0; c < columnIds.length; c++)
            series2pid.put(columnIds[c], c);

        for (int i = 0; i < numberOfSeries; i++) {
            sizes[i] = 0;
        }
        for (String series : series2Classes2count.keySet()) {
            int seriesId = series2pid.get(series);
            final Map<Integer, Integer> class2count = series2Classes2count.get(series);
            for (Integer classId : class2count.keySet()) {
                Integer count = class2count.get(classId);
                if (count == null)
                    count = 0;

                Integer[] counts = targetClass2counts.get(classId);
                if (counts == null) {
                    counts = new Integer[numberOfSeries];
                    targetClass2counts.put(classId, counts);
                }
                counts[seriesId] = count;
                totalReads += count;
                sizes[seriesId] += count;
            }
        }

        table.getClassification2Class2Counts().put(classificationName, targetClass2counts);

        if (!classificationName.equals(ClassificationType.Taxonomy.toString())) {
            final Map<Integer, Integer[]> class2counts = new HashMap<Integer, Integer[]>();
            class2counts.put(IdMapper.UNASSIGNED_ID, sizes);
            table.getClassification2Class2Counts().put(ClassificationType.Taxonomy.toString(), class2counts);
        }

        table.setSamples(names, null, sizes, new BlastMode[]{BlastMode.Unknown});
        table.setTotalReads(totalReads);
        doc.setNumberReads(totalReads);

        System.err.println("done (" + totalReads + " reads)");

        NotificationsInSwing.showInformation(MainViewer.getLastActiveFrame(), "Imported " + totalReads + " reads, as " + classificationName + " classification"
                + "\nGenerated by " + biomData.generated_by
                + "\nDate: " + biomData.getDate()
                + (biomData.getComment() != null ? "\nComment: " + biomData.getComment() : ""));
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
        Integer[] result = map.get(id);
        if (result == null) {
            result = newZeroedIntegerArray(size);
            map.put(id, result);
        }
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
