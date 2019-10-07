/*
 * Accession2IdAdapter2.java Copyright (C) 2019. Daniel H. Huson
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
 *
 */

package megan.accessiondb;

import megan.classification.data.IString2IntegerMap;

import java.io.IOException;
import java.sql.SQLException;

/**
 * adapts database accession mapping
 * Daniel Huson, 9.2019
 */
public class AccessAccessionAdapter implements IString2IntegerMap {
    private final AccessAccessionMappingDatabase accessAccessionMappingDatabase;
    private final String classificationName;
    private final int size;

    private final String mappingDBFile;

    /**
     * constructor
     * @param mappingDBFile
     * @param classificationName
     * @throws IOException
     * @throws SQLException
     */
    public AccessAccessionAdapter(final String mappingDBFile, final String classificationName) throws IOException, SQLException {
        this.mappingDBFile=mappingDBFile;
        accessAccessionMappingDatabase = new AccessAccessionMappingDatabase(mappingDBFile);
       this.classificationName=classificationName;
        size = accessAccessionMappingDatabase.getSize(classificationName);
    }

    @Override
    public int get(String accession) {
        try {
            return accessAccessionMappingDatabase.getValue(classificationName, accession);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void close() {
        accessAccessionMappingDatabase.close();
    }

    public String getMappingDBFile() {
        return mappingDBFile;
    }

    public AccessAccessionMappingDatabase getAccessAccessionMappingDatabase() {
        return accessAccessionMappingDatabase;
    }
}
