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
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import jloda.util.Basic;
import megan.biom.biom1.QIIMETaxonParser;
import megan.classification.IdMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class ImportBiom2Taxonomy {
    private final static String[] keys = {"taxonomy", "organism", "organisms"};

    /**
     * gets the taxonomy class to samples to counts map
     *
     * @return map
     */
    public static Map<Integer, float[]> getClass2Samples2Counts(IHDF5Reader reader, int numberOfSamples, boolean ignorePathAbove) throws IOException {
        int countLinesImported = 0;
        int countLinesSkipped = 0;


        MDArray<String> pathArray = null;
        int[] dimensions = null;

        for (final String metaKey : reader.getGroupMembers("/observation/metadata")) {
            if (Basic.getIndexIgnoreCase(metaKey, keys) != -1) {
                pathArray = reader.readStringMDArray("/observation/metadata/" + metaKey);
                dimensions = pathArray.dimensions();
                if (dimensions != null && dimensions.length > 0)
                    break;
            }
        }
        if (dimensions == null)
            return null;

        final int[] indptr = reader.readIntArray("/sample/matrix/indptr"); // dataset containing the compressed column offsets
        final int[] indices = reader.readIntArray("/sample/matrix/indices"); //  dataset containing the row indices (e.g., maps into observation/ids)
        final float[] data = reader.readFloatArray("/sample/matrix/data"); // dataset containing the actual matrix data

        final Map<Integer, float[]> class2counts = new HashMap<>();

        // Loop over Samples
        for (int i = 0; i < numberOfSamples; i++) {
            // Add counts to this sample
            for (int j = indptr[i]; j < indptr[i + 1]; j++) {
                final int taxonId;
                if (dimensions.length == 1) {
                    final String[] path = new String[]{pathArray.get(indices[j])};
                    taxonId = QIIMETaxonParser.parseTaxon(path, ignorePathAbove);
                } else if (dimensions.length == 2) {
                    final String[] path = getPath(pathArray, indices[j], dimensions[1]);
                    taxonId = QIIMETaxonParser.parseTaxon(path, ignorePathAbove);
                    countLinesImported++;
                } else {
                    taxonId = IdMapper.UNASSIGNED_ID;
                    countLinesSkipped++;
                }

                float[] array = class2counts.computeIfAbsent(taxonId, k -> new float[numberOfSamples]);
                array[i] += data[j];
            }
        }

        System.err.println(String.format("Lines imported:%,10d", countLinesImported));
        if (countLinesSkipped > 0)
            System.err.println(String.format("Lines skipped: %,10d", countLinesSkipped));

        return class2counts;
    }

    /**
     * get the taxon path
     */
    private static String[] getPath(MDArray<String> array, int row, int cols) {
        final String[] path = new String[cols];
        for (int c = 0; c < cols; c++)
            path[c] = array.get(row, c);
        return path;
    }

    /**
     * determines whether taxonomy metadata is present
     *
     * @param reader
     * @return true, if present
     */
    public static boolean hasTaxonomyMetadata(IHDF5Reader reader) {
        for (final String metaKey : reader.getGroupMembers("/observation/metadata")) {
            if (Basic.getIndexIgnoreCase(metaKey, keys) != -1) {
                final MDArray<String> pathArray = reader.readStringMDArray("/observation/metadata/" + metaKey);
                final int[] dimensions = pathArray.dimensions();
                if (dimensions != null && dimensions.length > 0)
                    return true;
            }
        }
        return false;
    }
}
