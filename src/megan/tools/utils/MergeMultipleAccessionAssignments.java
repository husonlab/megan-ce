/*
 * ReferencesAnnotator.java Copyright (C) 2022 Daniel H. Huson
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
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * merge accession assignments
 * Daniel Huson, 9.2022
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
     *
	 */
    private void run(String[] args) throws UsageException, IOException, SQLException {
        CommandManager.getGlobalCommands().addAll(ClassificationCommandHelper.getGlobalCommands());

        final var options = new ArgsOptions(args, this, "Merge multiple accession assignments");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2022 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        final var inputFile = options.getOptionMandatory("-i", "in", "Input file, each line containing space-separated accessions (stdin, .gz ok)", "");
        var outputFile = options.getOption("-o", "out", "Output file, each line containing first accession and merged assignments (stdout or .gz ok)", "stdout");
        final var mapDBFile = options.getOptionMandatory("-mdb", "mapDB", "MEGAN mapping db (file megan-map.db)", "");

        var cNames=options.getOption("-c","classifications","Classifications to assign (ALL or list of names)",new String[]{"ALL"});

        final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file", Megan6.getDefaultPropertiesFile());
        options.done();

        MeganProperties.initializeProperties(propertiesFile);

		FileUtils.checkFileReadableNonEmpty(inputFile);
        FileUtils.checkFileReadableNonEmpty(mapDBFile);

        FileUtils.checkAllFilesDifferent(inputFile,outputFile,mapDBFile);

        ClassificationManager.setMeganMapDBFile(mapDBFile);

        var database=new AccessAccessionMappingDatabase(mapDBFile);

        var supportedCNames=database.getClassificationNames().stream().filter(name->ClassificationManager.getAllSupportedClassifications().contains(name)).collect(Collectors.toList());

        if(StringUtils.containsIgnoreCase(cNames,"all")) {
            if(cNames.length!=1)
                throw new UsageException("--classifications: 'ALL' must be only value");
            cNames=supportedCNames.toArray(new String[0]);
        } else {
            for(var name:cNames) {
                if(!supportedCNames.contains(name))
                    throw new UsageException("--classifications: "+name+" not supported, must be one of: "+StringUtils.toString(supportedCNames,", "));
            }
        }
        if(cNames.length==0) {
            throw new UsageException("--classifications: must specify at least one, or ALL");
        }
        System.err.println("Classifications: "+StringUtils.toString(cNames,", "));

        final var idParsers = new IdParser[cNames.length];
        for (var i = 0; i < cNames.length; i++) {
            final var cName = cNames[i];
            var idMapper=ClassificationManager.get(cName, true).getIdMapper();
            idParsers[i] = idMapper.createIdParser();

            if(cNames[i].equals(Classification.Taxonomy) || cNames[i].equals("GTDB"))
                idParsers[i].setAlgorithm(IdParser.Algorithm.LCA);
            else
                idParsers[i].setAlgorithm(IdParser.Algorithm.Majority);
        }


        try (var it = new FileLineIterator(inputFile,true);
             var w = new BufferedWriter(FileUtils.getOutputWriterPossiblyZIPorGZIP(outputFile));) {
            System.err.println("Writing file: " + outputFile);

            w.write("#Accession\t"+StringUtils.toString(cNames,"\t")+"\n");

            var ids=new int[cNames.length];
            while (it.hasNext()) {
                var line=it.next().trim();
                var accessions= Arrays.stream(line.split("\\s+")).map(s->s.replaceAll("\\.[0-9]*$","")).toArray(String[]::new);
                if(accessions.length>0)
                    w.write(accessions[0]+"\t");
                getClassificationIds(accessions,cNames,database,idParsers,ids);
                w.write(StringUtils.toString(ids,"\t"));
                w.write("\n");
            }
        }
     }

     private final ArrayList<Integer> ids=new ArrayList<>();

    private void getClassificationIds(String[] accessions,String[] cNames,AccessAccessionMappingDatabase database,IdParser[] idParsers,int[] result) throws SQLException {
        var rows=database.getValues(accessions, cNames);
        for(var c=0;c<cNames.length;c++) {
            ids.clear();
            for(var array:rows) {
                var value=array[c];
                if(value>0)
                    ids.add(value);
            }
            result[c]=idParsers[c].processMultipleIds(ids);
        }
    }
}
