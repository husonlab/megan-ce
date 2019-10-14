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

import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import jloda.util.Basic;
import jloda.util.BlastMode;
import megan.biom.biom1.QIIMETaxonParser;
import megan.classification.Classification;
import megan.classification.IdMapper;
import megan.core.Document;
import megan.viewer.TaxonomyData;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Biom2ParserTest {
    /**
     * parse a file in biom2 format.
     *
     * @param inputFile
     * @return Document
     * @throws IOException
     */
    private static Document apply(String inputFile, boolean preservePaths) throws IOException {
        Writer dumpWriter = new BufferedWriter(new FileWriter(Basic.replaceFileSuffix(inputFile, (preservePaths ? "+p" : "-p") + "-dmp.txt")));

        final Document doc = new Document();
        doc.getDataTable().setCreator("MEGAN6 Biom2 import");
        doc.getDataTable().setCreationDate((new Date()).toString());

        System.err.println("Reading file: " + inputFile);
        int countLinesImported = 0;
        int countLinesSkipped = 0;

        try (IHDF5Reader reader = HDF5Factory.openForReading(inputFile)) {
            final TopLevelAttributes topLevelAttributes = new TopLevelAttributes(reader);
            System.err.println(topLevelAttributes.toString());

            final String[] sampleIds = reader.readStringArray("/sample/ids"); // dataset of the sample IDs

            final ArrayList<String> classifications = new ArrayList<>();
            final Map<String, MDArray<String>> classification2MDArray = new HashMap<>();

            String taxonomyNameMetadata = null;

            for (final String metaKey : reader.getGroupMembers("/observation/metadata")) {
                if (metaKey.equalsIgnoreCase("taxonomy") || metaKey.equalsIgnoreCase("organism")) {
                    taxonomyNameMetadata = metaKey;
                    classifications.add(taxonomyNameMetadata);
                    // System.err.println("Elements:  " + reader.getDataSetInformation("/observation/metadata/"+taxonomyNameMetadata).getNumberOfElements());

                    final MDArray<String> array = reader.readStringMDArray("/observation/metadata/" + taxonomyNameMetadata);
                    /*
                    int[] dimensions = array.dimensions();
                    if (dimensions.length == 2) {
                        int rows = dimensions[0];
                        int cols = dimensions[1];
                        for (int i = 0; i < rows; i++) {
                            if(false) {
                                System.err.print("row=" + i + ":");
                                for (int j = 0; j < cols; j++) {
                                    System.err.print(array.get(i, j) + ";");
                                }
                                System.err.println();
                                if (i > 100) {
                                    System.err.println("...");
                                    break;
                                }
                            }
                        }
                    }
                    */
                    classification2MDArray.put(taxonomyNameMetadata, array);
                }
            }

            final int[] indptr = reader.readIntArray("/sample/matrix/indptr"); // dataset containing the compressed column offsets
            final int[] indices = reader.readIntArray("/sample/matrix/indices"); //  dataset containing the row indices (e.g., maps into observation/ids)
            final float[] data = reader.readFloatArray("/sample/matrix/data"); // dataset containing the actual matrix data

            final Map<String, Map<Integer, float[]>> classication2class2counts = new HashMap<>();
            for (String classificationName : classifications) {
                classication2class2counts.put(classificationName, new HashMap<>());
            }

            final Map<Integer, float[]> class2counts = classication2class2counts.get("taxonomy");

            final float[] sizes = new float[sampleIds.length];
            // Loop over Samples
            for (int i = 0; i < sampleIds.length; i++) {
                long size = 0;

                // Add counts to this sample
                for (int j = indptr[i]; j < indptr[i + 1]; j++) {
                    size += data[j];

                    for (String classificationName : classifications) {
                        final MDArray<String> pathArray = classification2MDArray.get(classificationName);
                        final int[] dimensions = pathArray.dimensions();

                        final int taxonId;
                        if (dimensions.length == 1) {
                            final String[] path = new String[]{pathArray.get(indices[j])};
                            taxonId = QIIMETaxonParser.parseTaxon(path, preservePaths);
                            //System.err.println(Basic.toString(path, ";") + " -> " +taxonId+" -> "+TaxonomyData.getName2IdMap().get(taxonId)+" ->"+data[j]);
                            dumpWriter.append(Basic.toString(path, ";")).append(" -> ").append(String.valueOf(taxonId)).append(" -> ").
                                    append(TaxonomyData.getName2IdMap().get(taxonId)).append(" ->").append(String.valueOf(data[j])).append("\n");
                        } else if (dimensions.length == 2) {
                            final String[] path = getPath(pathArray, indices[j], dimensions[1]);
                            taxonId = QIIMETaxonParser.parseTaxon(path, preservePaths);
                            //System.err.println(Basic.toString(path, ";") + " -> " + data[j]);
                            dumpWriter.append(Basic.toString(path, ";")).append(" -> ").append(String.valueOf(taxonId)).append(" -> ").
                                    append(TaxonomyData.getName2IdMap().get(taxonId)).append(" ->").append(String.valueOf(data[j])).append("\n");
                        } else {
                            taxonId = IdMapper.UNASSIGNED_ID;
                            countLinesSkipped++;
                        }

                        //System.err.println(taxonId+" -> "+TaxonomyData.getName2IdMap().get(taxonId)+"- > "+data[j]);

                        float[] array = class2counts.computeIfAbsent(taxonId, k -> new float[sampleIds.length]);
                        array[i] += data[j];
                    }
                }
                sizes[i] = size;
            }

            doc.getDataTable().setSamples(sampleIds, null, sizes, new BlastMode[]{BlastMode.Classifier});
            doc.getDataTable().setTotalReads(Math.round(Basic.getSum(sizes)));

            for (Integer classId : class2counts.keySet()) {
                float[] array = class2counts.get(classId);
                for (int i = 0; i < array.length; i++)
                    doc.getDataTable().setClassification2Class2Count(Classification.Taxonomy, classId, i, array[i]);
            }

            // Loop over Metadata-Entries
            doc.getSampleAttributeTable().setSampleOrder(doc.getSampleNames());
            final Map<String, Object> sample2value = new HashMap<>();
            for (final String metaKey : reader.getGroupMembers("/sample/metadata")) {
                for (int i = 0; i < sampleIds.length; i++) {
                    String metaValue = reader.readStringArray("/sample/metadata/" + metaKey)[i];
                    sample2value.put(sampleIds[i], metaValue);
                }
                doc.getSampleAttributeTable().addAttribute(metaKey, sample2value, true, true);
                sample2value.clear();
            }
        }

        System.err.println(String.format("Lines imported:%,10d", countLinesImported));
        System.err.println(String.format("Lines skipped: %,10d", countLinesSkipped));

        dumpWriter.close();

        return doc;
    }

    private static String[] getPath(MDArray<String> array, int row, int cols) {
        final String[] path = new String[cols];
        for (int c = 0; c < cols; c++)
            path[c] = array.get(row, c);
        return path;
    }

    public static void main(String[] args) throws IOException {
        TaxonomyData.load();

        final String inputFile = "/Users/huson/data/biom2/suparna/otu_table_qiime1.9.1.biom";
        //final String inputFile="/Users/huson/data/biom2/rich_sparse_otu_table_hdf5.biom";

        boolean preservePaths = false;

        Document doc = apply(inputFile, false);
        OutputStreamWriter w = new OutputStreamWriter(System.err);
        doc.getDataTable().write(w);
        doc.getSampleAttributeTable().write(w, false, true);

        final String outputFile = Basic.replaceFileSuffix(inputFile, "-p" + ".megan");
        System.err.println("Writing file: " + outputFile);
        try (Writer writer = new FileWriter(outputFile)) {
            doc.getDataTable().write(writer);
            doc.getSampleAttributeTable().write(writer, false, true);

        }
    }
}
