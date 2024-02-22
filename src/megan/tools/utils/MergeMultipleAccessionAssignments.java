/*
 * ReferencesAnnotator.java Copyright (C) 2024 Daniel H. Huson
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
package megan.tools.utils;

import jloda.swing.commands.CommandManager;
import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import megan.accessiondb.AccessAccessionMappingDatabase;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdParser;
import megan.classification.data.ClassificationCommandHelper;
import megan.main.Megan6;
import megan.main.MeganProperties;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * merge accession assignments
 * Daniel Huson, 9.2022, 1.2024
 */
public class MergeMultipleAccessionAssignments {
    /**
     * merge accession assignments
     */
    public static void main(String[] args) {
        try {
            ResourceManager.insertResourceRoot(megan.resources.Resources.class);
            ProgramProperties.setProgramName(MergeMultipleAccessionAssignments.class.getSimpleName());
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new MergeMultipleAccessionAssignments()).run(args);
            System.err.println("Total time:  " + PeakMemoryUsageMonitor.getSecondsSinceStartString());
            System.err.println("Peak memory: " + PeakMemoryUsageMonitor.getPeakUsageString());
            System.exit(0);
        } catch (Exception ex) {
            Basic.caught(ex);
            System.exit(1);
        }
    }

    /**
     * run
     */
    private void run(String[] args) throws UsageException, IOException, SQLException {
        CommandManager.getGlobalCommands().addAll(ClassificationCommandHelper.getGlobalCommands());

        final var options = new ArgsOptions(args, this, "Merge multiple accession assignments");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2024. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        final var inputFile = options.getOptionMandatory("-i", "in", "Input file, each line containing a cluster accession followed  member accessions (stdin, .gz ok)", "");
        var outputFile = options.getOption("-o", "out", "Output file, each line containing first accession and merged assignments (stdout or .gz ok)", "stdout");
        final var mapDBFile = options.getOptionMandatory("-mdb", "mapDB", "MEGAN mapping db (file megan-map.db)", "");

        var cNames = options.getOption("-c", "classifications", "Classifications to assign (ALL or list of names)", new String[]{"ALL"});

        final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file", Megan6.getDefaultPropertiesFile());

        options.comment("Advanced");
        final var linesPerCall = options.getOption("-lpc", "linesPerCall", "Lines to process per call", 100);
        final var accessionsPerQuery = options.getOption("-apc", "accessionsPerQuery", "Maximum number of accessions per SQLITE query", 10000);
        options.done();

        MeganProperties.initializeProperties(propertiesFile);

        FileUtils.checkFileReadableNonEmpty(inputFile);
        FileUtils.checkFileReadableNonEmpty(mapDBFile);

        FileUtils.checkAllFilesDifferent(inputFile, outputFile, mapDBFile);

        ClassificationManager.setMeganMapDBFile(mapDBFile);

        var database = new AccessAccessionMappingDatabase(mapDBFile);

        var supportedCNames = database.getClassificationNames().stream().filter(name -> ClassificationManager.getAllSupportedClassifications().contains(name)).collect(Collectors.toList());

        if (StringUtils.containsIgnoreCase(cNames, "all")) {
            if (cNames.length != 1)
                throw new UsageException("--classifications: 'ALL' must be only value");
            cNames = supportedCNames.toArray(new String[0]);
        } else {
            for (var name : cNames) {
                if (!supportedCNames.contains(name))
                    throw new UsageException("--classifications: " + name + " not supported, must be one of: " + StringUtils.toString(supportedCNames, ", "));
            }
        }
        if (cNames.length == 0) {
            throw new UsageException("--classifications: must specify at least one, or ALL");
        }
        System.err.println("Classifications: " + StringUtils.toString(cNames, ", "));

        final var idParsers = new IdParser[cNames.length];
        for (var i = 0; i < cNames.length; i++) {
            final var cName = cNames[i];
            var idMapper = ClassificationManager.get(cName, true).getIdMapper();
            idParsers[i] = idMapper.createIdParser();

            if (cNames[i].equals(Classification.Taxonomy) || cNames[i].equals("GTDB"))
                idParsers[i].setAlgorithm(IdParser.Algorithm.LCA);
            else
                idParsers[i].setAlgorithm(IdParser.Algorithm.Majority);
        }

        final var workingData = new WorkingData(accessionsPerQuery);

        try (var it = new FileLineIterator(inputFile, true);
             var w = new BufferedWriter(FileUtils.getOutputWriterPossiblyZIPorGZIP(outputFile));) {
            System.err.println("Writing file: " + outputFile);

            w.write("#Accession\t" + StringUtils.toString(cNames, "\t") + "\n");

            var rowCount = 0;

            final var accessionRows = new String[linesPerCall][];

            {
                var prevAccession = "";
                var prevVersion = 1;
                var collectedTokens = new ArrayList<String>();

                while (it.hasNext()) {
                    final var line = it.next().trim();

                    var tokens = line.split("\\s");
                    if (tokens.length > 0) {
                        var accession = getAccession(tokens[0]);
                        var version = getVersion(tokens[0]);
                        if (accession.equals(prevAccession)) { // same as previous
                            if (version != prevVersion) {
                                collectedTokens.clear(); // is other version, clear
                            }
                            for (var i = 1; i < tokens.length; i++) { // copy accessions
                                collectedTokens.add(tokens[i].replaceAll("\\.[0-9]*$", ""));
                            }
                        } else { // not same as previous
                            if (!prevAccession.isBlank()) { // if previous set, flush
                                var row = new String[collectedTokens.size() + 1];
                                row[0] = prevAccession;
                                var i = 1;
                                for (var acc : collectedTokens) {
                                    row[i++] = acc;
                                }
                                accessionRows[rowCount++] = row;

                                if (rowCount >= linesPerCall) { // time to process what we have
                                    writeOutput(w, database, idParsers, accessionRows, rowCount, cNames, workingData);
                                    rowCount = 0;
                                }
                            }
                            // copy to previous
                            prevAccession = accession;
                            prevVersion = version;
                            collectedTokens.clear();
                            for (var i = 1; i < tokens.length; i++) {
                                collectedTokens.add(tokens[i].replaceAll("\\.[0-9]*$", ""));
                            }
                        }
                    }
                }
                if (!prevAccession.isBlank()) { // if previous set, flush
                    var nextRow = new String[collectedTokens.size() + 1];
                    nextRow[0] = prevAccession;
                    var i = 1;
                    for (var acc : collectedTokens) {
                        nextRow[i++] = acc;
                    }
                    accessionRows[rowCount++] = nextRow;
                }
                if (rowCount > 0) {
                    writeOutput(w, database, idParsers, accessionRows, rowCount, cNames, workingData);
                }
            }

            if (false) {
                while (it.hasNext()) {
                    final var line = it.next().trim();


                    final var nextRow = Arrays.stream(line.split("\\s+")).map(s -> s.replaceAll("\\.[0-9]*$", "")).toArray(String[]::new);

                    if (nextRow.length > 0) {
                        accessionRows[rowCount++] = nextRow;

                        if (rowCount >= linesPerCall) { // time to process what we have
                            writeOutput(w, database, idParsers, accessionRows, rowCount, cNames, workingData);
                            rowCount = 0;
                        }
                    }
                }

                if (rowCount > 0) {
                    writeOutput(w, database, idParsers, accessionRows, rowCount, cNames, workingData);
                }
            }
        }
    }

    private static void writeOutput(final BufferedWriter w, final AccessAccessionMappingDatabase database, final IdParser[] idParsers,
                                    final String[][] accessionRows, final int rowCount, final String[] cNames, WorkingData workingData) throws SQLException, IOException {

        final int[][] accessionClassesMap;
        // compute mapping of accessions to their classes in different classifications
        {
            final var accessions = workingData.accessionsCleared();
            for (var r = 0; r < rowCount; r++) {
                Collections.addAll(accessions, accessionRows[r]);
            }

            var totalAccessions = accessions.size();

            accessionClassesMap = new int[totalAccessions][];

            for (var start = 0; start < totalAccessions; start += workingData.maxQuerySize()) {
                var end = Math.min(totalAccessions, start + workingData.maxQuerySize());
                var subAccessions = accessions.subList(start, end).toArray(new String[0]);
                var subAccessionClassesTable = database.getValues(subAccessions, subAccessions.length, cNames);
                System.arraycopy(subAccessionClassesTable, 0, accessionClassesMap, start, subAccessions.length);
            }
        }

        if (false) {
            System.err.println("Accession\t" + StringUtils.toString(cNames, "\t"));
            var count = 0;
            for (var r = 0; r < rowCount; r++) {
                var row = accessionRows[r];
                for (var accession : row) {
                    System.err.println(accession + ": " + StringUtils.toString(accessionClassesMap[count++]));
                }
            }
        }
        // for each row of accessions, compute the resulting class ids and write out 'first-accession to classes' table:
        {
            final var classIds = workingData.classIdsCleared();

            var accessionNumber = 0; // number of accession in flat list
            for (var r = 0; r < rowCount; r++) {
                final var row = accessionRows[r];
                final var firstAccessionInRow = row[0];

                w.write(firstAccessionInRow);
                for (var c = 0; c < cNames.length; c++) {
                    classIds.clear();
                    for (var posInRow = 0; posInRow < row.length; posInRow++) {
                        var id = accessionClassesMap[accessionNumber + posInRow][c];
                        if (id != 0)
                            classIds.add(id);
                        // if(id<0)  System.err.println(id);

                    }
                    var id = idParsers[c].processMultipleIds(classIds);
                    w.write("\t%s".formatted(id != 0 ? String.valueOf(id) : "null"));
                }
                w.write("\n");
                accessionNumber += row.length;
            }
        }
    }

    private static String getAccession(String accessionAndVersion) {
        var pos = accessionAndVersion.indexOf(".");
        return (pos == -1 ? accessionAndVersion : accessionAndVersion.substring(0, pos));
    }

    private static int getVersion(String accessionAndVersion) {
        var pos = accessionAndVersion.indexOf(".");
        return pos == -1 || pos == accessionAndVersion.length() - 1 ? 1 : NumberUtils.parseInt(accessionAndVersion.substring(pos + 1));

    }

    /**
     * some tmp data structures that we recycle
     */
    private record WorkingData(int maxQuerySize, ArrayList<Integer> classIds, ArrayList<String> accessions) {

        public WorkingData(int maxQuerySize) {
            this(maxQuerySize, new ArrayList<>(), new ArrayList<>());
        }

        public ArrayList<Integer> classIdsCleared() {
            classIds.clear();
            return classIds();

        }

        public ArrayList<String> accessionsCleared() {
            accessions.clear();
            return accessions();
        }
    }
}