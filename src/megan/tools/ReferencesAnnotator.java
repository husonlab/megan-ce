/*
 *  Copyright (C) 2015 Daniel H. Huson
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

import jloda.gui.commands.CommandManager;
import jloda.util.*;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.classification.IdParser;
import megan.classification.data.ClassificationCommandHelper;
import megan.main.MeganProperties;
import megan.parsers.fasta.FastAFileIterator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

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
    public void run(String[] args) throws UsageException, IOException, ClassNotFoundException, CanceledException {
        CommandManager.getGlobalCommands().addAll(ClassificationCommandHelper.getGlobalCommands());

        final ArgsOptions options = new ArgsOptions(args, this, "Annotates reference sequences");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2016 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and output");
        final String inputFile = options.getOptionMandatory("-i", "in", "Input references file (gzipped ok)", "");
        String outputFile = options.getOptionMandatory("-o", "out", "Output file", "");

        final String[] availableFNames = ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy().toArray(new String[ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy().size()]);
        options.comment("Functional classification:");
        String[] cNames = options.getOption("-fun", "function", "Function assignments (any of " + Basic.toString(availableFNames, " ") + ")", new String[0]);
        for (String cName : cNames) {
            if (!ClassificationManager.getAllSupportedClassifications().contains(cName))
                throw new UsageException("--function: Unknown classification: " + cName);
            if (cName.equals(Classification.Taxonomy))
                throw new UsageException("--function: Illegal argument: 'Taxonomy'");
        }

        options.comment("Classification support:");

        if (options.isDoHelp())
            cNames = availableFNames;

        final boolean parseTaxonNames = options.getOption("-tn", "parseTaxonNames", "Parse taxon names", true);
        final String gi2TaxaFile = options.getOption("-g2t", "gi2taxa", "GI-to-Taxonomy mapping file", "");
        final String acc2TaxaFile = options.getOption("-a2t", "acc2taxa", "Accession-to-Taxonomy mapping file", "");
        final String synonyms2TaxaFile = options.getOption("-s2t", "syn2taxa", "Synonyms-to-Taxonomy mapping file", "");

        String[] gi2FNames = new String[cNames.length];
        String[] acc2FNames = new String[cNames.length];
        String[] synonyms2FNames = new String[cNames.length];

        for (int i1 = 0; i1 < cNames.length; i1++) {
            String cName = cNames[i1];
            gi2FNames[i1] = options.getOption("-g2" + cName.toLowerCase(), "gi2" + cName.toLowerCase(), "GI-to-" + cName + " mapping file", "");
            acc2FNames[i1] = options.getOption("-a2" + cName.toLowerCase(), "acc2" + cName.toLowerCase(), "Accession-to-" + cName + " mapping file", "");
            synonyms2FNames[i1] = options.getOption("-s2" + cName.toLowerCase(), "syn2" + cName.toLowerCase(), "Synonyms-to-" + cName + " mapping file", "");
            final boolean useLCA = options.getOption("-l_" + cName.toLowerCase(), "lca" + cName.toLowerCase(), "Use LCA for assigning to '" + cName + "', alternative: best hit", ProgramProperties.get(cName + "UseLCA", cName.equals(Classification.Taxonomy)));
            ProgramProperties.put(cName + "UseLCA", useLCA);
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


        final IdMapper[] idMappers = new IdMapper[cNames.length + 1];
        {
            final IdMapper taxonIdMapper = ClassificationManager.get(Classification.Taxonomy, true).getIdMapper();

            // Load all mapping files:
            ClassificationManager.get(Classification.Taxonomy, true);
            taxonIdMapper.setUseTextParsing(parseTaxonNames);

            if (gi2TaxaFile.length() > 0) {
                taxonIdMapper.loadMappingFile(gi2TaxaFile, IdMapper.MapType.GI, false, new ProgressPercentage());
            }
            if (acc2TaxaFile.length() > 0) {
                taxonIdMapper.loadMappingFile(acc2TaxaFile, IdMapper.MapType.Accession, false, new ProgressPercentage());
            }
            if (synonyms2TaxaFile.length() > 0) {
                taxonIdMapper.loadMappingFile(synonyms2TaxaFile, IdMapper.MapType.Synonyms, false, new ProgressPercentage());
            }
            idMappers[0] = taxonIdMapper;
        }

        for (int i = 0; i < cNames.length; i++) {
            idMappers[i + 1] = ClassificationManager.get(cNames[i], true).getIdMapper();

            if (gi2FNames[i].length() > 0)
                idMappers[i].loadMappingFile(gi2FNames[i], IdMapper.MapType.GI, false, new ProgressPercentage());
            if (gi2FNames[i].length() > 0)
                idMappers[i].loadMappingFile(gi2FNames[i], IdMapper.MapType.GI, false, new ProgressPercentage());
            if (acc2FNames[i].length() > 0)
                idMappers[i].loadMappingFile(acc2FNames[i], IdMapper.MapType.Accession, false, new ProgressPercentage());
            if (synonyms2FNames[i].length() > 0)
                idMappers[i].loadMappingFile(synonyms2FNames[i], IdMapper.MapType.Synonyms, false, new ProgressPercentage());
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
