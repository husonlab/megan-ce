/*
 * CreateAccessionMappingDatabase.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package megan.accessiondb;

import jloda.util.*;
import org.sqlite.SQLiteConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import static megan.accessiondb.AccessAccessionMappingDatabase.SQLiteTempStoreDirectoryProgramProperty;
import static megan.accessiondb.AccessAccessionMappingDatabase.SQLiteTempStoreInMemoryProgramProperty;

/**
 * setup databases for accession lookup
 * Original implementation: Syliva Siegel, 2019
 * Modified and extended by Daniel Huson, 9.2019
 */
public class CreateAccessionMappingDatabase {
    protected final String databaseFile;
    protected final ArrayList<String> tables;

    protected final SQLiteConfig config;

    private static final Single<Boolean> tempStoreInMemory=new Single<>(false);
    private static final Single<String> tempStoreDirectory=new Single<>("");

    /**
     * creates a new database file at the specified location
     */
    public CreateAccessionMappingDatabase(String databaseFile, String info, boolean overwrite) throws IOException, SQLException {
        this.databaseFile = databaseFile;
        this.tables = new ArrayList<>();

        // setting database configurations, as suggested by suggested by takrl at
        // https://stackoverflow.com/questions/784173/what-are-the-performance-characteristics-of-sqlite-with-very-large-database-files
        config = new SQLiteConfig();
        config.setCacheSize(10000);
        config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);

        tempStoreInMemory.set(ProgramProperties.get(SQLiteTempStoreInMemoryProgramProperty,tempStoreInMemory.get()));
        tempStoreDirectory.set(ProgramProperties.get(SQLiteTempStoreDirectoryProgramProperty,tempStoreDirectory.get()));

        if(tempStoreInMemory.get()) {
            config.setTempStore(SQLiteConfig.TempStore.MEMORY);
        }
        else if(!tempStoreDirectory.get().isBlank()){
            final File directory=new File(tempStoreDirectory.get());
            if(directory.isDirectory() && directory.canWrite()) {
                config.setTempStoreDirectory(tempStoreDirectory.get());
            }
        }

        //config.setJournalMode(SQLiteConfig.JournalMode.WAL);

        if (overwrite) {
            // check if file already exists and delete it if that is the case. Opening the DB with sqlite
            // will automatically create a new database
            try {
                final File f = new File(databaseFile);
                if (f.isDirectory() || !f.getParentFile().exists()) {
                    throw new IOException("Invalid file specification");
                }
            } catch (NullPointerException e) {
                // if f has no parent directory f.getParentFile will cause NullPointerException
                // this is still a valid path so catching this exception and do nothing
            }

            try {
                Files.deleteIfExists(Paths.get(databaseFile));   // throws no error if file does not exist
            } catch (FileSystemException | NullPointerException e) {
                throw new IOException("Failed to delete existing file");
            }

            execute("CREATE TABLE info(id TEXT PRIMARY KEY, info_string TEXT, size NUMERIC );");
        }

        if (info != null) {
            if (executeQueryString("SELECT info_string FROM info WHERE id = 'general';", 1).size() == 0)
                execute("INSERT INTO info VALUES ('general', '" + info + "', NULL);");
            else
                execute("UPDATE info SET info_string='" + info + "' WHERE id='general';");
        }
    }

    /**
     * inserts a new classifier into the database (separate table). Merging is done in mergeTables()
     *
     * @param classificationName name of the classifier used in the db
     * @param inputFile          path to file
     * @param description        description string to describe the used reference
     */
    public void insertClassification(String classificationName, String inputFile, String description) throws SQLException, IOException {
        insertClassification(classificationName, inputFile, 0, 1, description);
    }


    /**
     * inserts a new classifier into the database (separate table). Merging is done in mergeTables()
     *
     * @param classificationName name of the classifier used in the db
     * @param inputFile          path to file
     * @param description        description string to describe the used reference
     * @param accessionColumn  accession column in input file (0-based)
     * @param classColumn class column in input file (0-based)
     */
    public void insertClassification(String classificationName, String inputFile, int accessionColumn, int classColumn, String description) throws SQLException, IOException {
        if (classificationName == null) {
            throw new NullPointerException("classificationName");
        }
        int count = 0;

        try (var connection = config.createConnection("jdbc:sqlite:" + this.databaseFile);
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE " + classificationName + "(Accession TEXT PRIMARY KEY, " + classificationName + "_id NUMBER NOT NULL); ");
            connection.setAutoCommit(false);

            try (var insertStmd = connection.prepareStatement("INSERT INTO " + classificationName + " VALUES (?, ?);")) {
                try (var it = new FileLineIterator(inputFile, true)) {
                    while (it.hasNext()) {
                        final var tokens = it.next().split("\t");
                        if (accessionColumn < tokens.length && classColumn < tokens.length && NumberUtils.isInteger(tokens[classColumn])) {
                            var accession = tokens[accessionColumn];
                            var value = NumberUtils.parseInt(tokens[classColumn]);
                            if (value != 0) {
                                insertStmd.setString(1, accession);
                                insertStmd.setInt(2, value);
                                insertStmd.execute();
                                count++;
                            }
                        }
                    }
                }
            }
            connection.commit();
            connection.setAutoCommit(true);
            // write additional information into the info table
            statement.execute("INSERT INTO info VALUES ('" + classificationName + "', '" + description + "', " + count + ");");
        }
        System.err.printf("Table %s: added %,d items%n", classificationName, count);
        tables.add(classificationName);
    }

    /**
     * final merge, merging all reference tables into one
     * takes a long time
     */
    public void mergeTables() throws SQLException {
        if (tables.size() < 1) {
            return;
        }
        createNCBIRefTable();
        joinTables();
        cleanAfterJoin();
    }

    /**
     * creates a new table using ID_ncbi (=ncbi) as name and depending on the activated reference databases
     * drops any pre-existing table with the same name
     */
    private void createNCBIRefTable() throws SQLException {
        System.err.println("Creating accession table...");

        var createTableCommand = new StringBuilder("CREATE TABLE Accession AS ");
        for (var i = 0; i < tables.size(); i++) {
            if (i > 0)
                createTableCommand.append(" UNION ");
            createTableCommand.append("SELECT Accession FROM ").append(tables.get(i));
        }
        createTableCommand.append(";");

        execute("DROP TABLE IF EXISTS Accession;", createTableCommand.toString());
    }

    /**
     * Creates the joining query variably and then performs the join. Resulting table is called ID_mappings (= mappings)
     */
    private void joinTables() throws SQLException {
        System.err.println("Joining tables...");

        // when adding more classifications after initial merging this should enable the user to do so
        String renameQuery;
        if (tables.contains("mappings")) {
            renameQuery = "ALTER TABLE mappings RENAME TO temp;";
            tables.add("temp");
            tables.remove("mappings");
        } else
            renameQuery = "DROP TABLE IF EXISTS mappings;";

        // create query string based on which reference dbs are used
        var createMappingsCommand = new StringBuilder("CREATE TABLE mappings (Accession PRIMARY KEY ");
        var fillMappingCommand = new StringBuilder(" INSERT INTO mappings SELECT * FROM Accession AS n ");

        for (var table : tables) {
            if (table.equals("temp")) {
                // add all rows of temp (except accession)
                createMappingsCommand.append(getTablesAlreadyIncluded());
            } else {
                createMappingsCommand.append(", ").append(table).append(" INT");
            }
            fillMappingCommand.append("LEFT OUTER JOIN ").append(table).append(" USING (").append("Accession").append(") ");
        }
        // turn row ids column off
        createMappingsCommand.append(") WITHOUT ROWID;");
        fillMappingCommand.append(";"); // finishing the query
        // executing the queries
        execute(renameQuery, createMappingsCommand.toString(), fillMappingCommand.toString());
    }

    /**
     * get all tables already included in the mappings table (at this point renamed to temp)
     *
     * @return tables
     * @throws SQLException
     */
    private String getTablesAlreadyIncluded() throws SQLException {
        var buf = new StringBuilder();
        try (var connection = createConnection();
             var rs = connection.createStatement().executeQuery("SELECT id FROM info WHERE id != 'general';")) {
            while (rs.next()) {
                var s = rs.getString("id");
                if (!tables.contains(s)) {
                    buf.append(", ").append(s).append(" INT");
                }
            }
        }
        return buf.toString();
    }

    /**
     * performs the cleanUp after Joining the tables into one.
     * drops the merged tables and performs a VACUUM
     */
    private void cleanAfterJoin() throws SQLException {
        System.err.println("Cleaning up...");

        tables.add("Accession");

        var commands = new String[tables.size() + 1];
        for (var i = 0; i < tables.size(); i++) {
            if (!tables.get(i).equals("mappings")) {
                commands[i] = "DROP TABLE IF EXISTS " + tables.get(i) + ";";
            }
        }
        commands[tables.size()] = "VACUUM;";
        execute(commands);

        tables.clear();
        tables.add("mappings");
        // finally update the size information in the info table
        var size = computeSize("mappings");
        execute("UPDATE info SET size = " + size + " WHERE id = 'general';");
    }

    /**
     * queries the table tableName and returns the number of rows in that table
     *
     * @param tableName name of the table
     * @return size of the table tableName
     */
    private int computeSize(String tableName) throws SQLException {
        var count = 0;

        try (var connection = createConnection(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT count(*) AS q FROM " + tableName + ";")) {
            while (rs.next()) {
                count = rs.getInt("q"); // todo: is this correct?
            }
        }
        return count;
    }


    /**
     * adds a new column
     *
     * @param classificationName
     * @param inputFile
     * @param description
     * @throws SQLException
     * @throws IOException
     */
    public void addNewColumn(String classificationName, String inputFile, String description) throws SQLException, IOException {
        if (classificationName == null) {
            throw new NullPointerException("classificationName");
        }
        var count = 0;

        try (var connection = createConnection(); var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE mappings ADD COLUMN " + classificationName + " INTEGER;");
            connection.setAutoCommit(false);

            try (var insert = connection.prepareStatement("UPDATE mappings SET " + classificationName + "=? WHERE Accession=?")) {
                try (var it = new FileLineIterator(inputFile, true)) {
                    while (it.hasNext()) {
                        var tokens = it.next().split("\t");
                        var accession = tokens[0];
                        var value = NumberUtils.parseInt(tokens[1]);
                        if (value != 0) {
                            insert.setString(2, accession);
                            insert.setInt(1, value);
                            insert.execute();
                            count++;
                        }
                    }
                }
            }
            connection.commit();
            connection.setAutoCommit(true);
            statement.execute("INSERT INTO info VALUES ('" + classificationName + "', '" + description + "', " + count + ");");
        }
    }

    public Connection createConnection() throws SQLException {
        return config.createConnection("jdbc:sqlite:" + this.databaseFile);
    }

    /**
     * executes a list of commands
     *
     * @param commands String[] of complete queries
     * @throws SQLException if something goes wrong with the database
     */
    public void execute(String... commands) throws SQLException {
        try (var connection = createConnection()) {
            execute(connection, commands);
        }
    }

    /**
     * executes a list of commands
     */
    public static void execute(Connection connection, String... commands) throws SQLException {
        if (false)
			System.err.println("execute:\n" + StringUtils.toString(commands, "\n"));
        var statement = connection.createStatement();
        {
            for (var q : commands) {
                statement.execute(q);
            }
        }
    }

    /**
     * generic method for executing queries with results of type String
     */
    public ArrayList<String> executeQueryString(String query, int index) throws SQLException {
        try (var connection = createConnection()) {
            return executeQueryString(connection, query, index);
        }
    }


    /**
     * generic method for executing queries with results of type String
     */
    public static ArrayList<String> executeQueryString(Connection connection, String query, int index) throws SQLException {
        var rs = connection.createStatement().executeQuery(query);
        var result = new ArrayList<String>();
        while (rs.next()) {
            result.add(rs.getString(index));
        }
        return result;
    }
}