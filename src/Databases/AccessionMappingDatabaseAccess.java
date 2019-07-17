package Databases;

import org.apache.commons.collections4.map.LRUMap;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AccessionMappingDatabaseAccess implements IAccessionMappingDatabaseAccess {
    private String DB_PATH = null;
    private String JDBC_DRIVER = "org.sqlite.JDBC";
    private int CACHE_SIZE = 100000;
    private LRUMap<String, int[]> cache;
    private Map<Integer, String> classificationMap;
    public int db_query_count = 0;
    private Connection con;

    /**
     * default Constructor for AccessionMappingDatabaseAccess
     * requires to open the database manually using openDatabase
     */
    public AccessionMappingDatabaseAccess() {
        cache = new LRUMap<>(CACHE_SIZE);
        classificationMap = new HashMap<>(5);
    }

    /**
     * Constructor also establishes the connection to the database. No need to call openDatabase
     * @param path to the database file
     */
    public AccessionMappingDatabaseAccess(String path) throws FileNotFoundException, ClassNotFoundException, SQLException {
        this();
        openDatabase(path);
    }

    /**
     * changes the cache size and creates a new cache with that size
     * Warning: an old cache might be discarded
     * @param size new size of the cache
     */
    public void setCacheSize(Integer size) {
        CACHE_SIZE = size;
        cache = new LRUMap<>(CACHE_SIZE);
    }

    /**
     * sets the database location and checks if a connection to that database is possible
     * @param url path to the database file
     */
    @Override
    public void openDatabase(String url) throws FileNotFoundException, ClassNotFoundException, SQLException {
        if (url == null || url.equals("")) {
            throw new FileNotFoundException("The path " + url + " is invalid. Please try again with a valid path. ");
        }
        try { // If the database is already connected return
            if (DB_PATH.equals(url)) {
                return;
            }
        } catch (NullPointerException e) {
            // happens if DB_PATH is null (-> not set yet) we want to continue
        }
        // If the file can not be found at the given url location
        if (Files.exists(Paths.get(url))) {
            DB_PATH = url;
        } else {
            throw new FileNotFoundException("The path " + url + " does not exist. Please try again with a valid path. ");
        }
        // create new cache
        setCacheSize(CACHE_SIZE);

        // Check if it is possible to establish a connection to the database
        // and set the Connection variable
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(("jdbc:sqlite:" + DB_PATH));
            // give user feetback
            System.out.println("Connected with " + DB_PATH + ", " + getSize() + " enties in the database.");
            // create new classificationMap with the correct size
            ResultSetMetaData metaData = getMetaData("SELECT * FROM mappings LIMIT 1;");
            int cols = metaData.getColumnCount();
            classificationMap = new HashMap<>(cols);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Database driver not found. ");
        } catch (SQLException e) {
            throw new SQLException("Database connection could not be established. Make sure a path to a valid .db file was entered.", e);
        }

    }

    /**
     * generic method for executing queries with results of type int/Integer
     * @param query the SQL query
     * @return ArrayList containing all query results of the specified type
     * @throws SQLException if something went wrong with the database
     */
    private ArrayList<Integer> executeQueryInt(String query, int index) throws ClassNotFoundException, SQLException {
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Database driver not found");
        }
        ResultSet rs = null;
        ArrayList<Integer> resultlist = null;
        try {
            if (con.isClosed()) {
                con = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            }
            rs = con.createStatement().executeQuery(query);
            resultlist = new ArrayList<>();
            while (rs.next()) {
                resultlist.add(rs.getInt(index));
            }
            return resultlist;
        } catch (SQLException e) {
            //e.printStackTrace();
            throw new SQLException("Error when executing query " + query + "\n" + e.getMessage());
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * generic method for executing queries with results of type String
     * @param query the SQL query
     * @param index the index of the result of interest
     * @return ArrayList containing all query results of the specified type
     * @throws SQLException if something went wrong with the database
     */
    private ArrayList<String> executeQueryString(String query, int index) throws SQLException, ClassNotFoundException {
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Database driver not found");
        }
        ResultSet rs = null;
        ArrayList<String> resultlist = null;
        try {
            if (con.isClosed()) {
                con = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            }
            rs = con.createStatement().executeQuery(query);
            resultlist = new ArrayList<>();
            while (rs.next()) {
                resultlist.add(rs.getString(index));
            }
            return resultlist;
        } catch (SQLException e) {
            //e.printStackTrace();
            throw new SQLException("Error when executing query " + query + "\n" + e.getMessage());
        } finally {
            if (rs != null) {
                rs.close();
            }
        }

    }

    /**
     * get the info string associated with the database. This string can be inserted when creating the DB
     * @return info string associated or null if no such string is defined
     */
    @Override
    public String getInfo() throws SQLException, ClassNotFoundException {
        String query = "SELECT info_string FROM info WHERE id = 'general';";
        try {
            return executeQueryString(query, 1).get(0);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("No info string given");
        }
        return null;
    }

    /**
     * gets the size of the mappings table
     * @return size of mappings table or 0 if an error occured
     */
    @Override
    public int getSize() throws SQLException, ClassNotFoundException {
        // try to read the size of the mappings table from the info table
        String query = "SELECT size FROM info WHERE id = 'general';";
        try {
            return executeQueryInt(query, 1).get(0);
        } catch (IndexOutOfBoundsException e) {
            // if the field is empty compute the size and store it in the info table
            int size = computeDBSize();
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
     * computes the size of the mappings database by querying the mappings table with count(*)
     * @return size of the mapping database or 0 if an error occured
     */
    private int computeDBSize() throws SQLException, ClassNotFoundException {
        String query = "SELECT count(*) FROM mappings;";
        return executeQueryInt(query, 1).get(0);
    }

    /**
     * get the list of classification names
     * @return a Collection<String> containing all classification names used in the database
     */
    @Override
    public Collection<String> getClassificationNames() throws ClassNotFoundException, SQLException {
        String query = "SELECT id FROM info WHERE id != 'general';";
        try {
            return executeQueryString(query, 1);
        } catch (SQLException e) {
            System.err.println("Query " + query + " could not be executed successfully.");
            throw e;
        }
    }

    /**
     * gets the metadata for a certain query result
     * @param query String containing the complete SQL query
     * @return ResultSetMetaData
     */
    private ResultSetMetaData getMetaData(String query) throws SQLException {
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException c) {
            System.err.println("Driver not found. ");
            c.printStackTrace();
        }
        if (con.isClosed()) {
            con = DriverManager.getConnection(("jdbc:sqlite:" + DB_PATH));
        }
        Statement stmd = con.createStatement();
        ResultSetMetaData rs = stmd.executeQuery(query).getMetaData();
        return rs;
    }

    /**
     * get the index (column) for a classification. In the methods below, we will reference classifications by their index
     * @param classificationName name of the classification you want to look for
     * @return the index in the database for a given classificationName (note: it starts with 1 not 0) or -1 if no match was found
     */
    @Override
    public int getClassificationIndex(String classificationName) {
        String query = "SELECT * FROM mappings LIMIT 1;";
        ResultSetMetaData metaData = null;
        try {
            metaData = getMetaData(query);
            // Note that for the database access the index is 1-based
            // this 1-based index will be returned
            for (int i = 0; i < metaData.getColumnCount(); i++) {
                String label = metaData.getColumnLabel(i + 1);
                if (label.equals(classificationName + "_id")) {
                    return i + 1;
                }
            }
        } catch (SQLException e) {
            System.err.println("An error occured while executing the query " + query);
            e.printStackTrace();
        }
        // if we get here no valid classification name was given
        System.err.println(classificationName + " is not a valid identifier");
        return -1;
    }

    /**
     * get the type for a given classification index
     * @param classificationIndex index to be considered
     * @return the ValueType of the index (either String or integer)
     */
    @Override
    public ValueType getType(int classificationIndex) throws SQLException {
        String query = "SELECT * FROM mappings LIMIT 1;";
        try {
            ResultSetMetaData metaData = getMetaData(query);
            String typeName = metaData.getColumnTypeName(classificationIndex);
            if (typeName.equals("TEXT")) {
                return ValueType.STRING;
            } else if (typeName.equals("INT") || typeName.equals("NUM")) {
                return ValueType.integers;
            }
        } catch (SQLException e) {
            System.err.println("Something went wrong with the query " + query);
            throw e;
        }
        return null;
    }

    /**
     * get the classification name for a given classification index
     * @param classificationIndex index to be considered
     * @return the name of the classification or null if an error occurred
     */
    public String getClassificationName(int classificationIndex) throws SQLException {
        if (classificationMap.containsKey(classificationIndex)) {
            return classificationMap.get(classificationIndex);
        }
        String query = "SELECT * FROM mappings LIMIT 1;";
        try {
            ResultSetMetaData metaData = getMetaData(query);
            String result = metaData.getColumnName(classificationIndex).split("_")[0];
            classificationMap.put(classificationIndex, result);
            return result;
        } catch (SQLException e) {
            System.err.println("An error occured when executing the query " + query);
            throw e;
        }
    }

    /**
     * get the size for a given classification index
     * @param classificationIndex index to be considered
     * @return size for a given classification index or -1 if the classification was not found
     */
    @Override
    public int getSize(int classificationIndex) throws ClassNotFoundException, SQLException {
        String classificationName = getClassificationName(classificationIndex);
        String query = "SELECT size FROM info WHERE id = '" + classificationName + "';";
        try {
            return executeQueryInt(query, 1).get(0);
        } catch (SQLException e) {
            System.err.println("An error occured when executing the query " + query);
            throw e;
        } catch (IndexOutOfBoundsException e) {
        }
        return -1;
    }

    /**
     * get the info string for a given classification index
     * @param classificationIndex index to be considered
     * @return info string provided when inserting the reference database or "" if no such string was given
     */
    @Override
    public String getInfo(int classificationIndex) throws ClassNotFoundException, SQLException {
        String classificationName = getClassificationName(classificationIndex);
        String query = "SELECT info_string FROM info WHERE id = '" + classificationName + "';";
        try {
            return executeQueryString(query, 1).get(0);
        } catch (SQLException e) {
            throw new SQLException("Something went wrong with the query " + query, e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Checks the cache/ DB for the accession and returns the whole Integer[] of all values associated with that accession
     * @param accession accession String to query to database for
     * @return int[] or null
     */
    private int[] getAccession(String accession) throws SQLException, ClassNotFoundException {
        // check if accession is in cache
        if (cache.containsKey(accession)) {
            return cache.get(accession);
        }
        // if not query db for accession and append to cache
        int[] values = null;
        String query = "SELECT * FROM mappings WHERE ncbi_id = '" + accession + "';";
        Statement stmd = null;
        try {
            Class.forName(JDBC_DRIVER);
            if (con.isClosed()) {
                con = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            }
            stmd = con.createStatement();
            ResultSet rs = stmd.executeQuery(query);
            int columnCount = rs.getMetaData().getColumnCount();
            values = new int[columnCount];
            while (rs.next()) {
                for (int i = 2; i <= columnCount; i++) {
                    // database index starts with 1; 1 is the ncbi_id = accession everything else is result
                    values[i - 2] = rs.getInt(i);
                }
                cache.put(accession, values);
            }
        } catch (SQLException e) {
            System.err.println("Something went wrong while executing the query " + query);
            throw e;
        } catch (ClassNotFoundException c) {
            throw new ClassNotFoundException("Database Driver not found. " + c.getMessage());
        } finally {
            try {
                if (stmd != null) {
                    stmd.close();
                }
            } catch (SQLException e) {
            }
        }
        db_query_count++;
        return values;
    }

    /**
     * get the value for a classification of type integers
     * @param classificationIndex index to be considered
     * @param accession String to query the database for
     * @return the value of type int or 0 if non is found
     */
    @Override
    public int getValueInt(int classificationIndex, String accession) throws SQLException, ClassNotFoundException {
        int[] values = getAccession(accession);
        // the classification index is database related in values we put items starting from classification index = 2
        return values[(classificationIndex - 2)];
    }

    /**
     * get the value for a classification of type STRING
     * @param classificationIndex index to be considered
     * @param accession String to query the database for
     * @return String representation of the result of getValueInt
     */
    @Override
    public String getValueString(int classificationIndex, String accession) throws SQLException, ClassNotFoundException {
        return "" + getValueInt(classificationIndex, accession);
    }

    public void closeDB() throws SQLException {
        if (con != null) {
            con.close();
        }
    }
}
