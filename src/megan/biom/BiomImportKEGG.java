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
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * extracts classification from a BIOME file containing a kegg classification
 * Daniel Huson, 9.2012
 */
public class BiomImportKEGG {
    /**
     * gets a series 2 classes to value map from the data
     *
     * @return map
     */
    public static Map<String, Map<Integer, Integer>> getSeries2Classes2Value(BiomData biomData) {
        final Classification classification = ClassificationManager.get("KEGG", true);

        final Map<String, Map<Integer, Integer>> series2Classes2count = new HashMap<>();

        int numberOfRows = biomData.getRows().length;
        final Integer[] row2class = new Integer[numberOfRows];
        int rowCount = 0;
        for (Map row : biomData.getRows()) {
            //System.err.println("Obj: "+obj);

            Integer bestId = null;
            final String idStr = (String) row.get("id");
            if (idStr != null && Basic.isInteger(idStr))
                bestId = Basic.parseInt(idStr);
            else if (idStr != null && idStr.startsWith("K"))
                bestId = Basic.parseInt(idStr.substring(1));
            else {
                Map metaData = (Map) row.get("metadata");

                if (metaData != null) {
                    Object obj = metaData.get("taxonomy");
                    if (obj == null)
                        obj = metaData.get("ontology");
                    if (obj != null && obj instanceof ArrayList) {
                        List<String> names = Basic.reverseList((ArrayList) obj);
                        for (String name : names) {
                            int keggId = classification.getName2IdMap().get(name);
                            if (keggId > 0) {
                                bestId = keggId;
                                break;
                            }
                        }

                    }
                }
            }

            // System.err.println("Class: " + label);
            if (bestId != null)
                row2class[rowCount++] = bestId;
            else {
                row2class[rowCount++] = IdMapper.UNASSIGNED_ID;
                System.err.println("Failed to determine KEGG for: " + Basic.toString(row.values(), ","));
            }
        }

        int numberOfClasses = biomData.getColumns().length;
        final String[] col2series = new String[numberOfClasses];
        int colCount = 0;
        for (Object obj : biomData.getColumns()) {
            //System.err.println("Obj: "+obj);

            String label = (String) ((Map) obj).get("id");
            //System.err.println("Series: " + label);
            col2series[colCount++] = label;
        }

        if (biomData.getMatrix_type().equalsIgnoreCase(BiomData.AcceptableMatrixTypes.dense.toString())) {
            int row = 0;
            for (Object obj : biomData.getData()) {
                int[] array = (int[]) obj;
                for (int col = 0; col < array.length; col++) {
                    int value = array[col];
                    Map<Integer, Integer> class2count = series2Classes2count.get(col2series[col]);
                    if (class2count == null) {
                        class2count = new HashMap<>();
                        series2Classes2count.put(col2series[col], class2count);
                    }
                    Integer previous = class2count.get(row2class[row]);
                    if (previous != null)
                        value += previous;
                    // if (class2count.get(row2class[row]) == null) // need this to avoid reading the number for the same node  more than once
                        class2count.put(row2class[row], value);
                    // System.err.println(col2series[col] + " -> " + row2class[row] + " -> " + value);
                }
                row++;
            }
        } else if (biomData.getMatrix_type().equalsIgnoreCase(BiomData.AcceptableMatrixTypes.sparse.toString())) {
            for (Object obj : biomData.getData()) {
                int[] array3 = (int[]) obj;
                int row = array3[0];
                int col = array3[1];
                int value = array3[2];
                //System.err.println("Class: " + obj.getClass());
                //System.err.println("Row: " + Basic.toString(array3));
                Map<Integer, Integer> class2count = series2Classes2count.get(col2series[col]);
                if (class2count == null) {
                    class2count = new HashMap<>();
                    series2Classes2count.put(col2series[col], class2count);
                }
                Integer previous = class2count.get(row2class[row]);
                if (previous != null)
                    value += previous;
                // if (class2count.get(row2class[row]) == null) // need this to avoid reading the number for the same node  more than once
                    class2count.put(row2class[row], value);
                // System.err.println(col2series[col] + " -> " + row2class[row] + " -> " + value);
            }
        }
        return series2Classes2count;
    }

}
