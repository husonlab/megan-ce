/**
 * Copyright (C) 2016 Daniel H. Huson
 * Author Hans-Joachim Ruscheweyh
 * <p/>
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package rusch.megan5client;

import java.util.Map;


/**
 * An adapter to transport RMA Metadata via MEGAN and MEGAN5Server
 *
 * @author Hans-Joachim Ruscheweyh
 *         3:08:11 PM - Oct 27, 2014
 */
public class RMADataset {


    private String datasetName;
    private long datasetUid;
    private Map<String, String> metadata;
    private String description;


    public RMADataset() {
        // Needed for Jackson
    }

    public RMADataset(Integer datasetUid, String datasetName) {
        this.datasetName = datasetName;
        this.datasetUid = datasetUid;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public long getDatasetUid() {
        return datasetUid;
    }

    public void setDatasetUid(long datasetUid) {
        this.datasetUid = datasetUid;
    }

    public String toString() {
        return datasetName + "," + datasetUid;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


}
