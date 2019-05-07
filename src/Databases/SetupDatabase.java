package Databases;

import org.sqlite.SQLiteConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;

/**
 * setup databases for accession lookup
 * Sylvia Siegel, 5.2019
 */
public class SetupDatabase {
    private String FILE_LOCATION;
    private String JDBC_DRIVER = "org.sqlite.JDBC";
    private ArrayList<String> tables;
    // Strings defining the internal name of the created databases
    private String ID_ncbi = "ncbi";
    private String ID_mappings = "mappings";

    /**
     * creates a new database file at the specified location
     * WARNING an existing database file with the same name will be deleted and replaced by the new file
     *
     * @param fileLocation String like "path/to/database/file/databaseName.db"
     */
    public SetupDatabase(String fileLocation, String info) throws IOException, InvalidPathException, ClassNotFoundException, SQLException {
        // check if file already exists and delete it if that is the case. Opening the DB with sqlite
        // will automatically create a new database
        try {
            File f = new File(fileLocation);
            if (f.isDirectory() || !f.getParentFile().exists()) {
                throw new InvalidPathException(fileLocation, "Path is invalid");
            }
        } catch (NullPointerException e) {
            // if f has no parent directory f.getParentFile will cause NullPointerException
            // this is still a valid path so catching this exception and do nothing
        }

        try {
            Files.deleteIfExists(Paths.get(fileLocation));   // throws no error if file does not exist
        } catch (FileSystemException e) {
            throw new InvalidPathException(fileLocation, "Path is invalid");
        } catch (NullPointerException e) {
            throw new InvalidPathException("", "Path is invalid");
        }
        FILE_LOCATION = fileLocation;
        tables = new ArrayList<>();
        // configuring the database
        Connection con = null;
        Statement stmd = null;
        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(("jdbc:sqlite:" + FILE_LOCATION));
            // setting database configurations, as suggested by suggested by takrl at
            // https://stackoverflow.com/questions/784173/what-are-the-performance-characteristics-of-sqlite-with-very-large-database-files
            SQLiteConfig config = new SQLiteConfig();
            config.setPageSize(4096);
            config.setCacheSize(10000);
            config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE);
            config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
            config.setJournalMode(SQLiteConfig.JournalMode.WAL);
            config.setCacheSize(5000);
            // creating a table to hold the additional information (called info)
            stmd = con.createStatement();
            stmd.execute("CREATE TABLE info(" +
                    "id TEXT PRIMARY KEY, " +
                    "info_string TEXT, " +
                    "size NUMERIC ); ");
            stmd.execute("INSERT INTO info VALUES ('general', '" + info + "', NULL);");
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Database driver not found. ", e);
        } catch (SQLException e) {
            throw e;
        } finally {
            if (stmd != null) {
                stmd.close();
            }
            if (con != null) {
                con.close();
            }
        }
    }

    /**
     * executes a list of queries
     *
     * @param queries String[] of complete queries
     * @throws SQLException if something goes wrong with the database
     */
    private void executeQueries(String[] queries) throws SQLException, ClassNotFoundException {
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Database driver not found. ", e);
        }
        Connection con = null;
        Statement stmd = null;
        try {
            con = DriverManager.getConnection("jdbc:sqlite:" + FILE_LOCATION);
            stmd = con.createStatement();
            for (String q : queries) {
                try {
                    stmd.execute(q);
                } catch (SQLException e) {
                    System.err.println("Error when executing query " + q);
                    throw e;
                }
            }
        } finally {
            if (stmd != null) {
                stmd.close();
            }
            if (con != null) {
                con.close();
            }
        }
    }

    /**
     * inserts a new classifier into the database (separate table). Merging is done into a separate method mergeTables()
     *
     * @param classifierName      name of the classifier used in the db
     * @param pathToReferenceFile path to file
     * @param description         description string to describe the used reference
     */
    public void insertClassifier(String classifierName, String pathToReferenceFile, String description) throws ClassNotFoundException, SQLException, IOException {
        if (classifierName == null) {
            throw new NullPointerException("classifierName can not be null");
        }
        int counter = 0;
        Connection con = null;
        Statement stmd = null;
        PreparedStatement insertStmd = null;

        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(("jdbc:sqlite:" + FILE_LOCATION));
            String createTableQuery = "CREATE TABLE " + classifierName + "(" +
                    "    ncbi_id TEXT PRIMARY KEY, "
                    + classifierName + "_id NUMBER NOT NULL" +
                    "); ";
            File file;
            file = new File(pathToReferenceFile);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String inputString;
            stmd = con.createStatement();
            stmd.execute(createTableQuery);
            con.setAutoCommit(false);
            String insertionQuery = "INSERT INTO " + classifierName + " VALUES (?, ?);";
            insertStmd = con.prepareStatement(insertionQuery);
            while ((inputString = reader.readLine()) != null) {
                String[] split = inputString.split("\t");
                String refID = split[0];
                String varID = split[1];
                insertStmd.setString(1, refID);
                insertStmd.setString(2, varID);
                insertStmd.execute();
                counter++;
            }
            con.commit();
            con.setAutoCommit(true);
            // write additional information into the info table
            String appendInfoQuery = "INSERT INTO info VALUES ('" + classifierName + "', '" + description + "', "
                    + counter + ");";
            stmd.execute(appendInfoQuery);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Database driver not found. ", e);
        } finally {
            System.out.println("Added " + counter + " items to table " + classifierName);
            if (insertStmd != null) {
                insertStmd.close();
            }
            if (stmd != null) {
                stmd.close();
            }
            if (con != null) {
                con.close();
            }
        }
        tables.add(classifierName);
    }

    /**
     * final merge, merging all reference tables into one
     * takes a long time
     */
    public void mergeTables() throws SQLException, ClassNotFoundException {
        if (tables.size() < 1) {
            return;
        }
        // create a reference table containing all NCBI_ids
        createNCBIRefTable();
        // joining all tables with that reference table
        joinTables();
        // clean up after joining
        cleanAfterJoin();
    }

    /**
     * creates a new table using ID_ncbi (=ncbi) as name and depending on the activated reference databases
     * drops any preexisting table with the same name
     */
    private void createNCBIRefTable() throws SQLException, ClassNotFoundException {
        // create query string based on which reference dbs are used
        String dropTableQuery = "DROP TABLE IF EXISTS " + ID_ncbi + ";";
        String createTableQuery = "CREATE TABLE " + ID_ncbi + " AS ";
        // createTableQuery has to be dynamically extended
        for (int i = 0; i < tables.size(); i++) {
            createTableQuery = createUnionString(createTableQuery, tables.get(i), i);
        }
        // execute queries
        String[] queries = {dropTableQuery, createTableQuery};
        executeQueries(queries);
    }

    /**
     * creates a part of the create reference table query
     *
     * @param query     current status of the query
     * @param tableName name of the table to be in the Union
     * @param counter   if < tables.size() we append a UNION, since then an other table will be added in the next step
     *                  else append ; to finish the query
     * @return the extended new query.
     */
    private String createUnionString(String query, String tableName, Integer counter) {
        query = query.concat("SELECT ncbi_id FROM " + tableName);
        if (counter + 1 < tables.size()) {
            return query.concat(" UNION ");
        } else {
            return query.concat(";");
        }
    }

    /**
     * Creates the joining query variably and then performes the join. Resulting table is called ID_mappings (= mappings)
     */
    private void joinTables() throws SQLException, ClassNotFoundException {
        // when adding more classifieres after initial merging this should enable the user to do so
        String renameQuery = "DROP TABLE IF EXISTS " + ID_mappings + ";";
        if (tables.contains(ID_mappings)) {
            renameQuery = "ALTER TABLE " + ID_mappings + " RENAME TO temp;";
            tables.add("temp");
            tables.remove(ID_mappings);
        }
        // create query string based on which reference dbs are used
        String createMappingsQuery = " CREATE TABLE " + ID_mappings + " AS SELECT * FROM " + ID_ncbi + " AS n ";
        for (int i = 0; i < tables.size(); i++) {
            createMappingsQuery = createMappingsQuery.concat("LEFT OUTER JOIN " + tables.get(i) + " USING (ncbi_id) ");
        }
        createMappingsQuery = createMappingsQuery.concat(";"); // finishing the query
        // executing the queries
        String[] queries = {renameQuery, createMappingsQuery};
        executeQueries(queries);
    }

    /**
     * queries the table tableName and returns the number of rows in that table
     *
     * @param tableName name of the table
     * @return size of the table tableName or 0 if an error occured
     */
    public int calcultateSize(String tableName) throws ClassNotFoundException, SQLException {
        int counter = 0;
        String query = "SELECT count(*) AS q FROM " + tableName + ";";
        Connection con = null;
        Statement stmd = null;

        try {
            Class.forName(JDBC_DRIVER);
            con = DriverManager.getConnection(("jdbc:sqlite:" + FILE_LOCATION));
            stmd = con.createStatement();
            ResultSet rs = stmd.executeQuery(query);
            while (rs.next()) {
                counter = rs.getInt("q");
            }
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Database driver not found. ", e);
        } catch (SQLException e) {
            throw new SQLException("Error when executeing query " + query, e);
        } finally {
            if (stmd != null) {
                stmd.close();
            }
            if (con != null) {
                con.close();
            }
        }
        return counter;
    }

    /**
     * performs the cleanUp after Joining the tables into one.
     * drops the merged tables and performs a VACUUM
     */
    private void cleanAfterJoin() throws SQLException, ClassNotFoundException {
        tables.add(ID_ncbi);
        String[] queries = new String[tables.size() + 1];
        for (int i = 0; i < tables.size(); i++) {
            if (!tables.get(i).equals(ID_mappings)) {
                queries[i] = "DROP TABLE IF EXISTS " + tables.get(i) + ";";
            }
        }
        queries[tables.size()] = "VACUUM; ";
        executeQueries(queries);
        tables.clear();
        tables.add(ID_mappings);
        // finally update the size information in the info table
        int size = calcultateSize(ID_mappings);
        String q = "UPDATE info SET size = " + size + " WHERE id = 'general';";
        String[] updateInfo = {q};
        executeQueries(updateInfo);
    }

}
