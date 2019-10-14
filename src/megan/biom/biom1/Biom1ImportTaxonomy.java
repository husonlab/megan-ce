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

import jloda.util.Basic;
import megan.classification.IdMapper;
import megan.viewer.TaxonomyData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * imports a BIOME file of type Taxonomy table or OTU table
 * Daniel Huson, 9.2012
 */
class Biom1ImportTaxonomy {
    /**
     * gets a sample to class to value map from the data
     *
     * @return map
     */
    public static Map<String, Map<Integer, Integer>> getSample2Class2Value(Biom1Data biom1Data, boolean taxonomyIgnorePath) {
        final Map<String, Map<Integer, Integer>> sample2class2value = new HashMap<>();

        int numberOfRows = biom1Data.getRows().length;
        final Integer[] row2class = new Integer[numberOfRows];
        int rowCount = 0;
        for (Map row : biom1Data.getRows()) {
            //System.err.println("Obj: "+obj);

            Integer taxonId = null;
            Map metaData = (Map) row.get("metadata");
            if (metaData != null) {
                Object obj = metaData.get("taxonomy");
                if (obj == null)
                    obj = metaData.get("Taxonomy");
                if (obj == null)
                    obj = metaData.get("organism");
                if (obj == null)
                    obj = metaData.get("Organism");
                if (obj == null)
                    obj = metaData.get("ontology");
                if (obj instanceof ArrayList) {
                    final ArrayList<String> orig = ((ArrayList<String>) obj);
                    taxonId = QIIMETaxonParser.parseTaxon(orig.toArray(new String[0]), taxonomyIgnorePath);
                }
            }
            if (taxonId == null) {
                final String idStr = (String) row.get("id");
                if (idStr != null) {
                    if (Basic.isInteger(idStr))
                        taxonId = Basic.parseInt(idStr);
                    else {
                        int newTaxId = TaxonomyData.getName2IdMap().get(idStr);
                        if (newTaxId != 0) {
                            taxonId = newTaxId;
                        }
                    }
                }
            }

            if (taxonId != null)
                row2class[rowCount++] = taxonId;
            else {
                row2class[rowCount++] = IdMapper.UNASSIGNED_ID;
                System.err.println("Failed to determine taxon for: " + Basic.toString(row.values(), ","));
            }
        }

        int numberOfClasses = biom1Data.getColumns().length;
        String[] col2series = new String[numberOfClasses];
        int colCount = 0;
        for (Object obj : biom1Data.getColumns()) {
            //System.err.println("Obj: "+obj);

            String label = (String) ((Map) obj).get("id");
            //System.err.println("Series: " + label);
            col2series[colCount++] = label;
        }

        if (biom1Data.getMatrix_type().equalsIgnoreCase(Biom1Data.AcceptableMatrixTypes.dense.toString())) {
            int row = 0;
            for (Object obj : biom1Data.getData()) {
                final int[] array = createIntArray(obj);
                if (array == null)
                    continue;
                ;

                for (int col = 0; col < array.length; col++) {
                    int value = array[col];
                    Map<Integer, Integer> class2count = sample2class2value.computeIfAbsent(col2series[col], k -> new HashMap<>());
                    Integer previous = class2count.get(row2class[row]);
                    if (previous != null)
                        value += previous;
                    //  if (class2count.get(row2class[row]) == null) // need this to avoid reading the number for the same node  more than once
                    class2count.put(row2class[row], value);
                    // System.err.println(col2series[col] + " -> " + row2class[row] + " -> " + value);
                }
                row++;
            }
        } else if (biom1Data.getMatrix_type().equalsIgnoreCase(Biom1Data.AcceptableMatrixTypes.sparse.toString())) {
            for (Object obj : biom1Data.getData()) {
                final int[] array3 = createIntArray(obj);
                if (array3 == null)
                    continue;

                int row = array3[0];
                int col = array3[1];
                int value = array3[2];
                // System.err.println("Class: " + obj.getClass());
                // System.err.println("Row: " + Basic.toString(array3));
                Map<Integer, Integer> class2count = sample2class2value.computeIfAbsent(col2series[col], k -> new HashMap<>());
                Integer previous = class2count.get(row2class[row]);
                if (previous != null)
                    value += previous;
                // if (class2count.get(row2class[row]) == null) // need this to avoid reading the number for the same node  more than once
                class2count.put(row2class[row], value);
                // System.err.println(col2series[col] + " -> " + row2class[row] + " -> " + value);
            }
        }
        return sample2class2value;
    }

    /**
     * creates an int array from an object, if possible
     *
     * @param obj
     * @return array or null
     */
    public static int[] createIntArray(Object obj) {
        if (obj instanceof int[])
            return (int[]) obj;
        else if (obj instanceof float[]) {
            float[] that = (float[]) obj;
            int[] array = new int[that.length];
            for (int i = 0; i < that.length; i++)
                array[i] = Math.round(that[i]);
            return array;
        } else if (obj instanceof Integer[]) {
            final Integer[] that = (Integer[]) obj;
            int[] array = new int[that.length];
            for (int i = 0; i < that.length; i++)
                array[i] = Math.round(that[i]);
            return array;
        } else if (obj instanceof Float[]) {
            final Float[] that = (Float[]) obj;
            int[] array = new int[that.length];
            for (int i = 0; i < that.length; i++)
                array[i] = Math.round(that[i]);
            return array;
        } else
            return null;
    }
}
