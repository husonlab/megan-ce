/*
 * TimeDatabaseAccess.java Copyright (C) 2019. Daniel H. Huson
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

package Databases;

import jloda.swing.util.ArgsOptions;
import jloda.util.*;
import megan.classification.data.IString2IntegerMap;

import java.io.IOException;

/**
 * time access to accession database
 * Daniel Huson, 7.2019
 */
public class TimeDatabaseAccess {
    public static void main(String[] args) {
        try {
            ProgramProperties.setProgramName("TimeDatabaseAccess");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new TimeDatabaseAccess()).run(args);
            System.err.println("Total time:  " + PeakMemoryUsageMonitor.getSecondsSinceStartString());
            System.err.println("Peak memory: " + PeakMemoryUsageMonitor.getPeakUsageString());
            System.exit(0);
        } catch (Exception ex) {
            Basic.caught(ex);
            System.exit(1);
        }
    }

    /**
     * run the program
     */
    public void run(String[] args) throws CanceledException, IOException, UsageException {
        final ArgsOptions options = new ArgsOptions(args, this, "Time database access");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2019 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input Output");
        final String databaseFile = options.getOptionMandatory("-d", "database", "Database file", "");
        final String inputFile = options.getOptionMandatory("-i", "input", "File containing one accession per line", "");
        options.done();

        long start = System.currentTimeMillis();
        Accession2IdAdapter accession2IdAdapter = Accession2IdAdapter.getInstance();
        accession2IdAdapter.setup(databaseFile);
        System.err.println(String.format("Open database:  %.1f sec", (System.currentTimeMillis() - start) / 1000.0));

        final String[] classifications = accession2IdAdapter.getClassificationNames().toArray(new String[0]);
        System.err.println("Classifications: " + Basic.toString(classifications, " "));

        final IString2IntegerMap[] maps = new IString2IntegerMap[classifications.length];
        for (int i = 0; i < classifications.length; i++) {
            maps[i] = accession2IdAdapter.createMap(classifications[i]);
        }

        int countQueries = 0;
        final int[] countFound = new int[maps.length];

        start = System.currentTimeMillis();
        try (FileInputIterator it = new FileInputIterator(inputFile, true)) {
            while (it.hasNext()) {
                countQueries++;
                final String accession = it.next();
                for (int i = 0; i < maps.length; i++) {
                    if (maps[i].get(accession) != 0)
                        countFound[i]++;
                }
            }
        }
        System.err.println(String.format("Query database: %.1f sec", (System.currentTimeMillis() - start) / 1000.0));
        System.err.println(String.format("Queries: %,9d", countQueries));
        for (int i = 0; i < countFound.length; i++)
            System.err.println(String.format("%s: %,9d", classifications[i], countFound[i]));

        accession2IdAdapter.close();
    }
}
