/*
 * AccessionMappingDatabaseAccess.java Copyright (C) 2019. Daniel H. Huson
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


import jloda.util.Basic;
import org.sqlite.SQLiteConfig;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/**
 * Access to accession mapping database
 * Original implementation: Syliva Siegel, 2019
 * Modified and extended by Daniel Huson, 9.2019
 */
public class AccessAccessionMappingDatabase implements Closeable {
    enum ValueType {TEXT, INT}

    private final Connection connection;

    public static IntUnaryOperator accessionFilter =(x)->(x>-1000?x:0);
    public static Function<String,Boolean> fileFilter=(x)->!x.endsWith("_UE");

    /**
     * constructor, opens and maintains connection to database
     * @param dbFile
     * @throws IOException
     * @throws SQLException
     */
    public AccessAccessionMappingDatabase(String dbFile) throws IOException, SQLException {
        if (!Basic.fileExistsAndIsNonEmpty(dbFile))
            throw new IOException("File not found or unreadable: " + dbFile);

        // setting database configurations, as suggested by suggested by takrl at
        // https://stackoverflow.com/questions/784173/what-are-the-performance-characteristics-of-sqlite-with-very-large-database-files
        final SQLiteConfig config = new SQLiteConfig();
        config.setCacheSize(10000);
         config.setReadOnly(true);

        connection = config.createConnection("jdbc:sqlite:" + dbFile);

        if(!fileFilter.apply(executeQueryString("SELECT info_string FROM info WHERE id = 'general';", 1).get(0)))
            throw new IOException("Mapping file "+Basic.getFileNameWithoutPath(dbFile)+" is intended for use with MEGAN Ultimate Edition, it is not compatible with MEGAN Community Edition");
    }

    /**
     * get the info string associated with the database. This string can be inserted when creating the DB
     *
     * @return info string associated or null if no such string is defined
     */
    public String getInfo() throws SQLException {
        final StringBuilder buf = new StringBuilder();
        try {
            buf.append(executeQueryString("SELECT info_string FROM info WHERE id = 'general';", 1).get(0));
        } catch (IndexOutOfBoundsException e) {
            throw new SQLException(e);
        }
        for (String name : getClassificationNames()) {
            buf.append("\n").append(name).append(": ").append(getInfo(name));
        }
        return buf.toString();
    }

    /**
     * gets the size of the mappings table
     *
     * @return size
     */
    public int getSize() throws SQLException {
        try {
            return executeQueryInt("SELECT size FROM info WHERE id = 'general';", 1).get(0);
        } catch (IndexOutOfBoundsException e) {
            // if the field is empty compute the size and store it in the info table
            final int size = computeDBSize();
            String insertionQuery = "UPDATE info SET size = " + size + " WHERE id = 'general';";
            try {
                executeQueryInt(insertionQuery, 1);
            } catch (SQLException ex) {
                // it does not matter whether insertion worked or not.
            }
            return size;
        }
    }

    /**
     * get the index (column) for a classification. In the methods below, we will reference classifications by their index
     *
     * @param classificationName name of the classification you want to look for
     * @return the index in the database for a given classificationName
     */
    private int getClassificationIndex(String classificationName) throws SQLException {
        final String query = "SELECT * FROM mappings LIMIT 1;";

        final ResultSetMetaData metaData = getMetaData(query);
        // Note that for the database access the index is 1-based
        // this 1-based index will be returned
        for (int i = 0; i < metaData.getColumnCount(); i++) {
            String label = metaData.getColumnLabel(i + 1);
            if (label.equals(classificationName)) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * get the type for a given classification index
     *
     * @param classificationIndex index to be considered
     * @return the ValueType of the index (either String or integer)
     */
    public ValueType getType(int classificationIndex) throws SQLException {
        final String query = "SELECT * FROM mappings LIMIT 1;";

        final ResultSetMetaData metaData = getMetaData(query);
        final String typeName = metaData.getColumnTypeName(classificationIndex);
        if (typeName.equals("TEXT")) {
            return ValueType.TEXT;
        } else if (typeName.equals("INT") || typeName.equals("NUM")) {
            return ValueType.INT;
        }
        return null;
    }

    /**
     * get the size for a given classification index
     *
     * @param classificationName
     * @return size for a given classification index or -1 if the classification was not found
     */
    public int getSize(String classificationName) throws SQLException {
        try {
            return executeQueryInt("SELECT size FROM info WHERE id = '" + classificationName + "';", 1).get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new SQLException(e);
        }
    }

    /**
     * get the info string for a given classification
     *
     * @param classificationName
     * @return info string provided when inserting the reference database or "" if no such string was given
     */
    private String getInfo(String classificationName) throws SQLException {
        try {
            final String infoString=executeQueryString("SELECT info_string FROM info WHERE id = '" + classificationName + "';", 1).get(0);
            return String.format("%s, size: %,d",infoString,getSize(classificationName));
        } catch (IndexOutOfBoundsException e) {
            throw new SQLException(e);
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                Basic.caught(e);
            }
        }
    }

    /**
     * Checks the cache/ DB for the accession and returns the whole Integer[] of all values associated with that accession
     *
     * @param accession accession String to query to database for
     * @return int[] or null
     */
    public int getValue(String classificationName,String accession) throws SQLException {
        final ResultSet rs = connection.createStatement().executeQuery("SELECT "+classificationName+" FROM mappings WHERE Accession = '" + accession + "';");
        while (rs.next()) {
            final int value=rs.getInt(classificationName);
            if(value!=0)
                return accessionFilter.applyAsInt(value);
        }
        return 0;
    }
    /**
     * alternative implementation for getValue
     * for an array of string accessions the method queries the database at once for all accessions in that array
     *
     * @param accessions
     * @return a HashMap containing the accession and a list of the corresponding classifications
     * @throws SQLException
     */
    public HashMap<String, int[]> getValues(String[] accessions, int length) throws SQLException {
        final StringBuilder buf = new StringBuilder();
        buf.append("select * from mappings where Accession in(");
        boolean first = true;
        for (int i = 0; i < length; i++) {
            if (first)
                first = false;
            else
                buf.append(", ");
            buf.append("'").append(accessions[i]).append("'");
        }
        buf.append(");");

        final ResultSet rs = connection.createStatement().executeQuery(buf.toString());
        final int columnCount = rs.getMetaData().getColumnCount();

        final HashMap<String, int[]> results = new HashMap<>();
        while (rs.next()) {
            final int[] values = new int[columnCount];
            for (int i = 2; i <= columnCount; i++) {
                // database index starts with 1; 1 is the accession everything else is result
                values[i - 2] = accessionFilter.applyAsInt(rs.getInt(i));
            }
            results.put(rs.getString(1), values);
        }

        return results;
    }

    /**
     * generic method for executing queries with results of type int/Integer
     *
     * @param query the SQL query
     * @return ArrayList containing all query results of the specified type
     * @throws SQLException if something went wrong with the database
     */
    private ArrayList<Integer> executeQueryInt(String query, int index) throws SQLException {
        final ResultSet rs = connection.createStatement().executeQuery(query);
        final ArrayList<Integer> resultlist = new ArrayList<>();
        while (rs.next()) {
            resultlist.add(rs.getInt(index));
        }
        return resultlist;
    }

    /**
     * generic method for executing queries with results of type String
     *
     * @param query the SQL query
     * @param index the index of the result of interest
     * @return ArrayList containing all query results of the specified type
     * @throws SQLException if something went wrong with the database
     */
    private ArrayList<String> executeQueryString(String query, int index) throws SQLException {
        final ResultSet rs = connection.createStatement().executeQuery(query);
        final ArrayList<String> result = new ArrayList<>();
        while (rs.next()) {
            result.add(rs.getString(index));
        }
        return result;
    }

    /**
     * computes the size of the mappings database by querying the mappings table with count(*)
     *
     * @return size of the database or 0 if an error occurred
     */
    private int computeDBSize() throws SQLException {
        return executeQueryInt("SELECT count(*) FROM mappings;", 1).get(0);
    }

    /**
     * get the list of classification names
     *
     * @return a Collection<String> containing all classification names used in the database
     */
    private Collection<String> getClassificationNames() throws SQLException {
        return executeQueryString("SELECT id FROM info WHERE id != 'general' AND id !='edition' ;", 1);
    }

    /**
     * gets the metadata for a certain query result
     *
     * @param query String containing the complete SQL query
     * @return ResultSetMetaData
     */
    private ResultSetMetaData getMetaData(String query) throws SQLException {
        return connection.createStatement().executeQuery(query).getMetaData();
    }

    /**
     * setups up the classification name to output index.
     * @param classificationNames
     * @return index, or max-int, if classification not included in database
     * @throws SQLException
     */
    public int[] setupMapClassificationId2DatabaseRank(final String[] classificationNames) throws SQLException {
        final int[] result = new int[classificationNames.length];
        for (int i = 0; i < classificationNames.length; i++) {
            final int index=getClassificationIndex(classificationNames[i]);
            result[i] = (index>=0?index - 2:Integer.MAX_VALUE);
        }
        return result;
    }


    public static Collection<String> getContainedClassificationsIfDBExists(String fileName) {
        if (Basic.fileExistsAndIsNonEmpty(fileName)) {
            try (AccessAccessionMappingDatabase accessAccessionMappingDatabase = new AccessAccessionMappingDatabase(fileName)) {
                return accessAccessionMappingDatabase.getClassificationNames();
            } catch (IOException | SQLException ex) {
                // ignore
            }
        }
        return Collections.emptySet();
    }
}
