/*
 *  Copyright (C) 2019 Daniel H. Huson
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
 */
package megan.tools;

import jloda.swing.commands.CommandManager;
import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import megan.accessiondb.AccessAccessionMappingDatabase;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.classification.IdParser;
import megan.classification.data.ClassificationCommandHelper;
import megan.main.Megan6;
import megan.main.MeganProperties;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * annotate a file of reference sequences
 * Daniel Huson, 3.2016
 */
public class ReferencesAnnotator {
    /**
     * merge RMA files
     *
     * @param args
     * @throws UsageException
     * @throws IOException
     */
    public static void main(String[] args) {
        try {
            ResourceManager.addResourceRoot(Megan6.class, "megan.resources");
            ProgramProperties.setProgramName("ReferencesAnnotator");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new ReferencesAnnotator()).run(args);
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
     * @param args
     * @throws UsageException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void run(String[] args) throws UsageException, IOException, ClassNotFoundException, CanceledException, SQLException {
        CommandManager.getGlobalCommands().addAll(ClassificationCommandHelper.getGlobalCommands());

        final ArgsOptions options = new ArgsOptions(args, this, "Annotates reference sequences");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2019 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and output");
        final String inputFile = options.getOptionMandatory("-i", "in", "Input references file (gzipped ok)", "");
        String outputFile = options.getOptionMandatory("-o", "out", "Output file", "");

        options.comment("Classification support:");

        final boolean parseTaxonNames = options.getOption("-tn", "parseTaxonNames", "Parse taxon names", true);
        final String mapDBFile = options.getOption("-mdb", "mapDB", "MEGAN mapping db (file megan-map.db)", "");
        final String acc2TaxaFile = options.getOption("-a2t", "acc2taxa", "Accession-to-Taxonomy mapping file", "");
        final String synonyms2TaxaFile = options.getOption("-s2t", "syn2taxa", "Synonyms-to-Taxonomy mapping file", "");

        final HashMap<String, String> class2AccessionFile = new HashMap<>();
        final HashMap<String, String> class2SynonymsFile = new HashMap<>();

        for (String cName : ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy()) {
            class2AccessionFile.put(cName, options.getOption("-a2" + cName.toLowerCase(), "acc2" + cName.toLowerCase(), "Accession-to-" + cName + " mapping file", ""));
            class2SynonymsFile.put(cName, options.getOption("-s2" + cName.toLowerCase(), "syn2" + cName.toLowerCase(), "Synonyms-to-" + cName + " mapping file", ""));
            final String tags = options.getOption("-t4" + cName.toLowerCase(), "tags4" + cName.toLowerCase(), "Tags for " + cName + " id parsing (must set to activate id parsing)", "").trim();
            if (tags.length() > 0)
                ProgramProperties.put(cName + "Tags", tags);
            ProgramProperties.put(cName + "ParseIds", tags.length() > 0);
            // final boolean useLCA = options.getOption("-l_" + cName.toLowerCase(), "lca" + cName.toLowerCase(), "Use LCA for assigning to '" + cName + "', alternative: best hit", ProgramProperties.get(cName + "UseLCA", cName.equals(Classification.Taxonomy)));
            // ProgramProperties.put(cName + "UseLCA", useLCA);
        }

        options.comment(ArgsOptions.OTHER);
        ProgramProperties.put(IdParser.PROPERTIES_FIRST_WORD_IS_ACCESSION, options.getOption("-fwa", "firstWordIsAccession", "First word in reference header is accession number", ProgramProperties.get(IdParser.PROPERTIES_FIRST_WORD_IS_ACCESSION, true)));
        ProgramProperties.put(IdParser.PROPERTIES_ACCESSION_TAGS, options.getOption("-atags", "accessionTags", "List of accession tags", ProgramProperties.get(IdParser.PROPERTIES_ACCESSION_TAGS, IdParser.ACCESSION_TAGS)));

        options.done();

        final String propertiesFile;
        if (ProgramProperties.isMacOS())
            propertiesFile = System.getProperty("user.home") + "/Library/Preferences/Megan.def";
        else
            propertiesFile = System.getProperty("user.home") + File.separator + ".Megan.def";
        MeganProperties.initializeProperties(propertiesFile);

        Basic.checkFileReadableNonEmpty(inputFile);

        final Collection<String> mapDBClassifications= AccessAccessionMappingDatabase.getContainedClassificationsIfDBExists(mapDBFile);
        if(mapDBClassifications.size()>0 && (Basic.hasPositiveLengthValue(class2AccessionFile)|| Basic.hasPositiveLengthValue(class2SynonymsFile)))
            throw new UsageException("Illegal to use both --mapDB and ---acc2... or --syn2... options");

        final ArrayList<String> cNames = new ArrayList<>();
        for (String cName : ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy()) {
            if (mapDBClassifications.contains(cName)  || class2AccessionFile.get(cName).length() > 0 || class2SynonymsFile.get(cName).length() > 0)
                cNames.add(cName);
        }
        if (cNames.size() > 0)
            System.err.println("Functional classifications to use: " + Basic.toString(cNames, ", "));


        final IdMapper[] idMappers = new IdMapper[cNames.size() + 1];
        final IdMapper taxonIdMapper = ClassificationManager.get(Classification.Taxonomy, true).getIdMapper();
        {

            // Load all mapping files:
            ClassificationManager.get(Classification.Taxonomy, true);
            taxonIdMapper.setUseTextParsing(parseTaxonNames);

            if (mapDBFile.length() > 0) {
                taxonIdMapper.loadMappingFile(mapDBFile, IdMapper.MapType.MeganMapDB, false, new ProgressPercentage());
            }
            if (acc2TaxaFile.length() > 0) {
                taxonIdMapper.loadMappingFile(acc2TaxaFile, IdMapper.MapType.Accession, false, new ProgressPercentage());
            }
            if (synonyms2TaxaFile.length() > 0) {
                taxonIdMapper.loadMappingFile(synonyms2TaxaFile, IdMapper.MapType.Synonyms, false, new ProgressPercentage());
            }
            idMappers[idMappers.length - 1] = taxonIdMapper;
        }
        for (int i = 0; i < cNames.size(); i++) {
            final String cName = cNames.get(i);

            idMappers[i] = ClassificationManager.get(cName, true).getIdMapper();

            if (mapDBClassifications.contains(cName))
                idMappers[i].loadMappingFile(mapDBFile, IdMapper.MapType.MeganMapDB, false, new ProgressPercentage());
            if (class2AccessionFile.get(cName).length() > 0)
                idMappers[i].loadMappingFile(class2AccessionFile.get(cName), IdMapper.MapType.Accession, false, new ProgressPercentage());
            if (class2SynonymsFile.get(cName).length() > 0)
                idMappers[i].loadMappingFile(class2SynonymsFile.get(cName), IdMapper.MapType.Synonyms, false, new ProgressPercentage());
        }

        final IdParser[] idParsers = new IdParser[idMappers.length];
        for (int i = 0; i < idMappers.length; i++) {
            idParsers[i] = new IdParser(idMappers[i]);
        }

        final int[] counts = new int[idMappers.length];

        try (FastAFileIterator it = new FastAFileIterator(inputFile);
             OutputStream outs = new BufferedOutputStream(Basic.getOutputStreamPossiblyZIPorGZIP(outputFile));
             ProgressPercentage progress = new ProgressPercentage("Reading file: " + inputFile, it.getMaximumProgress())) {
            System.err.println("Writing file: " + outputFile);

            while (it.hasNext()) {
                final Pair<String, String> pair = it.next();
                final StringBuilder header = new StringBuilder();
                final String firstWord = Basic.getFirstWord(pair.getFirst());
                header.append(firstWord);
                boolean first = true;
                for (int i = 0; i < idParsers.length; i++) {
                    int id = idParsers[i].getIdFromHeaderLine(pair.getFirst());
                    if (id != 0) {
                        if (first) {
                            if (!firstWord.endsWith("|"))
                                header.append("|");
                            first = false;
                        } else
                            header.append("|");
                        header.append(String.format("%s%d", Classification.createShortTag(idMappers[i].getCName()), id));
                        counts[i]++;
                    }
                    progress.setProgress(it.getProgress());
                }
                outs.write(header.toString().getBytes());
                outs.write('\n');
                outs.write(pair.getSecond().getBytes());
                outs.write('\n');
            }
        }
        // report classification sizes:
        for (int i = 0; i < idMappers.length; i++) {
            System.err.println(String.format("Class. %-13s%,10d", idMappers[i].getCName() + ":", counts[i]));
        }
    }
}
