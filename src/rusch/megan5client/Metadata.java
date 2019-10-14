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
package rusch.megan5client;

import java.util.HashMap;
import java.util.Map;


/**
 * Translates Metadata to a user friendly format
 *
 * @author Hans-Joachim Ruscheweyh
 * 2:17:16 PM - Dec 23, 2014
 */
public class Metadata {

    /**
     * A simple method to translate the way how Metadata is stored in RMA to a more user friendly {@link Map<String, String>} format.
     * <p/>
     * TODO Ask Daniel if he needs it mapped back to the RMA format for his Sampleattribute Table
     *
     * @param metadata
     * @return
     */
    public static Map<String, String> transformMetadataString(String metadata) {
        try {
            String[] splits = metadata.split("\n");
            String[] metadatanames = splits[0].split("\t");
            metadatanames[0] = metadatanames[0].replace("#", "");
            String[] metadatavalues = splits[1].split("\t");
            Map<String, String> values = new HashMap<>();
            for (int i = 0; i < metadatanames.length; i++) {
                values.put(metadatanames[i], metadatavalues[i]);
            }
            return values;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }


}
