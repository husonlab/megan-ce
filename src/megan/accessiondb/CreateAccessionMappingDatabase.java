/*
 * SetupDatabase.java Copyright (C) 2019. Daniel H. Huson
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
import jloda.util.FileLineIterator;
import org.sqlite.SQLiteConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;

/**
 * setup databases for accession lookup
 * Original implementation: Syliva Siegel, 2019
 * Modified and extended by Daniel Huson, 9.2019
 */
public class CreateAccessionMappingDatabase {
    protected final String databaseFile;
    protected final ArrayList<String> tables;

    protected final SQLiteConfig config;

    /**
     * creates a new database file at the specified location
     * WARNING an existing database file with the same name will be deleted and replaced by the new file
     *
     * @param databaseFile String like "path/to/database/file/databaseName.db"
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
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);

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
        execute("INSERT INTO info VALUES ('general', '" + info + "', NULL);");
    }

    /**
     * executes a list of commands
     *
     * @param commands String[] of complete queries
     * @throws SQLException if something goes wrong with the database
     */
    private void execute(String... commands) throws SQLException {
        try (Connection connection = config.createConnection("jdbc:sqlite:" + this.databaseFile);
             Statement statement = connection.createStatement()) {
            for (String q : commands) {
                statement.execute(q);
            }
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
        if (classificationName == null) {
            throw new NullPointerException("classificationName");
        }
        int count = 0;

        try (Connection connection = config.createConnection("jdbc:sqlite:" + this.databaseFile);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE " + classificationName + "(Accession TEXT PRIMARY KEY, " + classificationName + "_id NUMBER NOT NULL); ");
            connection.setAutoCommit(false);

            try (PreparedStatement insertStmd = connection.prepareStatement("INSERT INTO " + classificationName + " VALUES (?, ?);")) {
                try (FileLineIterator it = new FileLineIterator(inputFile, true)) {
                    while (it.hasNext()) {
                        final String[] tokens = it.next().split("\t");
                        final String accession = tokens[0];
                        final int value = Basic.parseInt(tokens[1]);
                        if (value != 0) {
                            insertStmd.setString(1, accession);
                            insertStmd.setInt(2, value);
                            insertStmd.execute();
                            count++;
                        }
                    }
                }
            }
            connection.commit();
            connection.setAutoCommit(true);
            // write additional information into the info table
            statement.execute("INSERT INTO info VALUES ('" + classificationName + "', '" + description + "', " + count + ");");
        }
        System.err.println(String.format("Table %s: added %,d items", classificationName, count));
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

        final StringBuilder createTableCommand = new StringBuilder("CREATE TABLE Accession AS ");
        for (int i = 0; i < tables.size(); i++) {
            if (i > 0)
                createTableCommand.append(" UNION ");
            createTableCommand.append("SELECT Accession FROM ").append(tables.get(i));
        }
        createTableCommand.append(";");

        execute("DROP TABLE IF EXISTS Accession;", createTableCommand.toString());
    }

    /**
     * Creates the joining query variably and then performes the join. Resulting table is called ID_mappings (= mappings)
     */
    private void joinTables() throws SQLException {
        System.err.println("Joining tables...");

        // when adding more classifieres after initial merging this should enable the user to do so
        final String renameQuery;
        if (tables.contains("mappings")) {
            renameQuery = "ALTER TABLE mappings RENAME TO temp;";
            tables.add("temp");
            tables.remove("mappings");
        } else
            renameQuery = "DROP TABLE IF EXISTS mappings;";

        // create query string based on which reference dbs are used
        final StringBuilder createMappingsCommand = new StringBuilder("CREATE TABLE mappings (Accession PRIMARY KEY ");
        final StringBuilder fillMappingCommand = new StringBuilder(" INSERT INTO mappings SELECT * FROM Accession AS n ");

        for (String table : tables) {
            if (table.equals("temp")) {
                // add all rows of temp (except accession)
                createMappingsCommand.append(getTablesAlreadyIncluded());
            } else {
                createMappingsCommand.append(", ").append(table).append(" INT");
            }
            fillMappingCommand.append("LEFT OUTER JOIN ").append(table).append(" USING (").append("Accession").append(") ");
        }
        // turn rowid column off
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
        final StringBuilder buf = new StringBuilder();
        try (Connection connection = config.createConnection("jdbc:sqlite:" + this.databaseFile);
             ResultSet rs = connection.createStatement().executeQuery("SELECT id FROM info WHERE id != 'general';")) {
            while (rs.next()) {
                final String s = rs.getString("id");
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

        final String[] commands = new String[tables.size() + 1];
        for (int i = 0; i < tables.size(); i++) {
            if (!tables.get(i).equals("mappings")) {
                commands[i] = "DROP TABLE IF EXISTS " + tables.get(i) + ";";
            }
        }
        commands[tables.size()] = "VACUUM;";
        execute(commands);

        tables.clear();
        tables.add("mappings");
        // finally update the size information in the info table
        final int size = computeSize("mappings");
        execute("UPDATE info SET size = " + size + " WHERE id = 'general';");

        final SQLiteConfig config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.DELETE);
    }

    /**
     * queries the table tableName and returns the number of rows in that table
     *
     * @param tableName name of the table
     * @return size of the table tableName
     */
    private int computeSize(String tableName) throws SQLException {
        int counter = 0;

        try (Connection connection = config.createConnection("jdbc:sqlite:" + this.databaseFile); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT count(*) AS q FROM " + tableName + ";")) {
            while (rs.next()) {
                counter = rs.getInt("q"); // todo: is this correct?
            }
        }
        return counter;
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
        int count = 0;

        try (Connection connection = config.createConnection("jdbc:sqlite:" + this.databaseFile); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE mappings ADD COLUMN " + classificationName + " INTEGER;");
            connection.setAutoCommit(false);

            try (PreparedStatement insertStmd = connection.prepareStatement("UPDATE mappings SET " + classificationName + "=? WHERE Accession=?")) {
                try (FileLineIterator it = new FileLineIterator(inputFile, true)) {
                    while (it.hasNext()) {
                        final String[] tokens = it.next().split("\t");
                        final String accession = tokens[0];
                        final int value = Basic.parseInt(tokens[1]);
                        if (value != 0) {
                            insertStmd.setString(2, accession);
                            insertStmd.setInt(1, value);
                            insertStmd.execute();
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

}