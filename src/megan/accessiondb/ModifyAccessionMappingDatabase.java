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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Modify SQLITE database
 * Daniel Huson, 212.2019
 */
public class ModifyAccessionMappingDatabase {
    protected final String databaseFile;

    protected final SQLiteConfig config;

    /**
     * constructor
     *
     * @param databaseFile
     */
    public ModifyAccessionMappingDatabase(String databaseFile) throws IOException, SQLException {
        this.databaseFile = databaseFile;

        System.err.println("Database '" + databaseFile + "', current contents: ");
        try (AccessAccessionMappingDatabase accessAccessionMappingDatabase = new AccessAccessionMappingDatabase(databaseFile)) {
            System.err.println(accessAccessionMappingDatabase.getInfo());
        }

        config = new SQLiteConfig();
        config.setCacheSize(10000);
        config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
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
            statement.execute("ALTER TABLE mappings ADD COLUMN '" + classificationName + "' INTEGER;");
            connection.setAutoCommit(false);

            try (PreparedStatement insertStmd = connection.prepareStatement("UPDATE mappings SET '" + classificationName + "'=? WHERE Accession=?")) {
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
            System.err.println("Committing...");
            connection.commit();
            connection.setAutoCommit(true);
            statement.execute("INSERT INTO info VALUES ('" + classificationName + "', '" + description + "', " + count + ");");
        }
    }

}