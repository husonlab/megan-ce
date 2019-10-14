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

import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ch.systemsx.cisd.hdf5.IHDF5Writer;
import megan.core.SampleAttributeTable;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class Biom2MetaData {
    /**
     * read metadata from a BIOM2 file
     *
     * @param reader
     * @param sampleIds
     * @param table
     * @throws IOException
     */
    public static int read(IHDF5Reader reader, String[] sampleIds, SampleAttributeTable table) throws IOException {
        table.setSampleOrder(Arrays.asList(sampleIds));
        final Map<String, Object> sample2value = new HashMap<>();
        for (final String metaKey : reader.getGroupMembers("/sample/metadata")) {
            for (int i = 0; i < sampleIds.length; i++) {
                String metaValue = reader.readStringArray("/sample/metadata/" + metaKey)[i];
                sample2value.put(sampleIds[i], metaValue);
            }
            table.addAttribute(metaKey, sample2value, true, true);
            sample2value.clear();
        }
        return reader.getGroupMembers("/sample/metadata").size();
    }

    /**
     * write metadata to a BIOM2 file
     *
     * @param writer
     * @param sampleIds
     * @param table
     * @throws IOException
     */
    public static void write(IHDF5Writer writer, String[] sampleIds, SampleAttributeTable table) throws IOException {
        throw new IOException("Not implemented");
    }
}
