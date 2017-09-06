/*
 *  Copyright (C) 2015 Daniel H. Huson
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
import ch.systemsx.cisd.hdf5.IHDF5SimpleReader;

import java.io.IOException;
import java.util.ArrayList;

public class Biom2Parser {
    /**
     * parse a file in biom2 format.
     * Based on code in CORNETTO
     *
     * @param filepath
     * @return list of samples
     * @throws IOException
     */
    public static ArrayList<Biom2Sample> parse(String filepath) throws IOException {
        final ArrayList<Biom2Sample> sampleList = new ArrayList<>();

        try (IHDF5SimpleReader reader = HDF5Factory.openForReading(filepath)) {
            final String[] sampleIds = reader.readStringArray("/sample/ids");
            final String[] observationIds = reader.readStringArray("/observation/ids");
            final int[] indptr = reader.readIntArray("/sample/matrix/indptr");
            final int[] indices = reader.readIntArray("/sample/matrix/indices");
            final float[] data = reader.readFloatArray("/sample/matrix/data");

            // Loop over Samples
            for (int i = 0; i < sampleIds.length; i++) {
                // Create new Biom2Sample
                final Biom2Sample newSample = new Biom2Sample();
                newSample.setName(sampleIds[i]);

                // Add counts to this sample
                for (int j = indptr[i]; j < indptr[i + 1]; j++) {
                    final int classId = Integer.parseInt(observationIds[indices[j]]);
                    newSample.getClassId2Count().put(classId, data[j]);
                }

                // Loop over Metadata-Entries
                for (final String metaKey : reader.getGroupMembers("/sample/metadata")) {
                    System.out.println(metaKey);
                    String metaValue = reader.readStringArray("/sample/metadata/" + metaKey)[i];
                    newSample.getMetaDataMap().put(metaKey, metaValue);
                }

                sampleList.add(newSample);
            }
        }
        return sampleList;
    }

    public static void main(String[] args) throws IOException {
        ArrayList<Biom2Sample> samples = parse("/Users/huson/exampleV2.biom");

        for (Biom2Sample sample : samples) {
            System.err.println("Sample: " + sample.getName());
            System.err.println("Counts:");
            for (Integer id : sample.getClassId2Count().keySet()) {
                System.err.println("\t" + id + " -> " + sample.getClassId2Count().get(id));
            }
            System.err.println("Metadata:");
            for (String key : sample.getMetaDataMap().keySet()) {
                System.err.println("\t" + key + " -> " + sample.getMetaDataMap().get(key));

            }

        }
    }
}
