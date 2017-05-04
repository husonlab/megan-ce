/*
 *  Copyright (C) 2017 Daniel H. Huson
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
import megan.core.Document;
import megan.daa.Meganize;
import megan.main.MeganProperties;
import megan.util.DAAFileFilter;

import java.io.File;
import java.io.IOException;

/**
 * prepares a DAA file for use with MEGAN
 * Daniel Huson, 8.2015
 */
public class DAAMeganizer {
    /**
     * meganizes a DAA file
     *
     * @param args
     * @throws UsageException
     * @throws IOException
     */
    public static void main(String[] args) {
        try {
            ProgramProperties.setProgramName("Meganizer");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new DAAMeganizer()).run(args);
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

        final ArgsOptions options = new ArgsOptions(args, this, "Prepares ('meganizes') a DIAMOND .daa file for use with MEGAN");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2017 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Files");
        final String[] daaFiles = options.getOptionMandatory("-i", "in", "Input DAA file", new String[0]);
        final String[] metaDataFiles = options.getOption("-mdf", "metaDataFile", "Files containing metadata to be included in files", new String[0]);

        options.comment("Reads");
        final boolean pairedReads = options.getOption("-pr", "paired", "Reads are paired", false);
        final int pairedReadsSuffixLength = options.getOption("-ps", "pairedSuffixLength", "Length of name suffix used to distinguish between name of read and its mate", 0);
        options.comment("Parameters");
        boolean longReads = options.getOption("-lg", "longReads", "Parse and analyse as long reads", Document.DEFAULT_LONG_READS);

        final boolean runClassifications = options.getOption("-class", "classify", "Run classification algorithm", true);
        final float minScore = options.getOption("-ms", "minScore", "Min score", Document.DEFAULT_MINSCORE);
        final float maxExpected = options.getOption("-me", "maxExpected", "Max expected", Document.DEFAULT_MAXEXPECTED);
        final float minPercentIdentity = options.getOption("-mpi", "minPercentIdentity", "Min percent identity", Document.DEFAULT_MIN_PERCENT_IDENTITY);
        final float topPercent = options.getOption("-top", "topPercent", "Top percent", Document.DEFAULT_TOPPERCENT);
        final float minSupportPercent = options.getOption("-supp", "minSupportPercent", "Min support as percent of assigned reads (0==off)", Document.DEFAULT_MINSUPPORT_PERCENT);
        final int minSupport = options.getOption("-sup", "minSupport", "Min support", Document.DEFAULT_MINSUPPORT);
        final float minPercentReadToCover = options.getOption("-mrc", "minPercentReadCover", "Min percent of read length to be covered by alignments", Document.DEFAULT_MIN_PERCENT_READ_TO_COVER);
        final Document.LCAAlgorithm lcaAlgorithm = Document.LCAAlgorithm.valueOfIgnoreCase(options.getOption("-alg", "lcaAlgorithm", "Set the LCA algorithm to use for taxonomic assignment",
                Document.LCAAlgorithm.values(), longReads ? Document.DEFAULT_LCA_ALGORITHM_LONG_READS.toString() : Document.DEFAULT_LCA_ALGORITHM_SHORT_READS.toString()));
        final float weightedLCAPercent;
        if (options.isDoHelp() || lcaAlgorithm == Document.LCAAlgorithm.weighted || lcaAlgorithm == Document.LCAAlgorithm.longReads)
            weightedLCAPercent = (float) options.getOption("-wlp", "weightedLCAPercent", "Set the percent weight to cover", Document.DEFAULT_WEIGHTED_LCA_PERCENT);
        else
            weightedLCAPercent = -1;

        final Document.ReadAssignmentMode readAssignmentMode = Document.ReadAssignmentMode.valueOfIgnoreCase(options.getOption("-ram", "readAssignmentMode", "Set the read assignment mode",
                Document.ReadAssignmentMode.values(), longReads ? Document.DEFAULT_READ_ASSIGNMENT_MODE_LONG_READS.toString() : Document.DEFAULT_LCA_ALGORITHM_SHORT_READS.toString()));

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
        final String acc2TaxaFile = options.getOption("-a2t", "acc2taxa", "Accessopm-to-Taxonomy mapping file", "");
        final String synonyms2TaxaFile = options.getOption("-s2t", "syn2taxa", "Synonyms-to-Taxonomy mapping file", "");

        final String[] gi2FNames = new String[cNames.length];
        final String[] acc2FNames = new String[cNames.length];
        final String[] synonyms2FNames = new String[cNames.length];

        for (int f = 0; f < cNames.length; f++) {
            String cName = cNames[f];
            gi2FNames[f] = options.getOption("-g2" + cName.toLowerCase(), "gi2" + cName.toLowerCase(), "GI-to-" + cName + " mapping file", "");
            acc2FNames[f] = options.getOption("-a2" + cName.toLowerCase(), "acc2" + cName.toLowerCase(), "Accession-to-" + cName + " mapping file", "");
            synonyms2FNames[f] = options.getOption("-s2" + cName.toLowerCase(), "syn2" + cName.toLowerCase(), "Synonyms-to-" + cName + " mapping file", "");
            final String tags = options.getOption("-t4" + cName.toLowerCase(), "tags4" + cName.toLowerCase(), "Tags for " + cName + " id parsing (must set to activate id parsing)", "").trim();
            if (tags.length() > 0)
                ProgramProperties.put(cName + "Tags", tags);
            ProgramProperties.put(cName + "ParseIds", tags.length() > 0);
            // final boolean useLCA = options.getOption("-l_" + cName.toLowerCase(), "lca" + cName.toLowerCase(), "Use LCA for assigning to '" + cName + "', alternative: best hit", ProgramProperties.get(cName + "UseLCA", cName.equals(Classification.Taxonomy)));
            // ProgramProperties.put(cName + "UseLCA", useLCA);
        }

        options.comment(ArgsOptions.OTHER);
        ProgramProperties.put(IdParser.PROPERTIES_FIRST_WORD_IS_ACCESSION, options.getOption("-fwa", "firstWordIsAccession", "First word in reference header is accession number (set to 'true' for NCBI-nr downloaded Sep 2016 or later)", true));
        ProgramProperties.put(IdParser.PROPERTIES_ACCESSION_TAGS, options.getOption("-atags", "accessionTags", "List of accession tags", ProgramProperties.get(IdParser.PROPERTIES_ACCESSION_TAGS, IdParser.ACCESSION_TAGS)));
        options.done();

        final String propertiesFile;
        if (ProgramProperties.isMacOS())
            propertiesFile = System.getProperty("user.home") + "/Library/Preferences/Megan.def";
        else
            propertiesFile = System.getProperty("user.home") + File.separator + ".Megan.def";
        MeganProperties.initializeProperties(propertiesFile);

        if (Basic.getDebugMode())
            System.err.println("Java version: " + System.getProperty("java.version"));

        MeganProperties.initializeProperties(propertiesFile);

        if (minSupport > 0 && minSupportPercent > 0)
            throw new IOException("Please specify a positive value for either --minSupport or --minSupportPercent, but not for both");

        for (String fileName : daaFiles) {
            Basic.checkFileReadableNonEmpty(fileName);
            if (!DAAFileFilter.getInstance().accept(fileName))
                throw new IOException("File not in DAA format: " + fileName);
        }

        for (String fileName : metaDataFiles) {
            Basic.checkFileReadableNonEmpty(fileName);
        }

        if (metaDataFiles.length > 1 && metaDataFiles.length != daaFiles.length) {
            throw new IOException("Number of metadata files (" + metaDataFiles.length + ") doesn't match number of DAA files (" + daaFiles.length + ")");
        }

        final IdMapper taxonIdMapper = ClassificationManager.get(Classification.Taxonomy, true).getIdMapper();
        final IdMapper[] idMappers = new IdMapper[cNames.length];

        // Load all mapping files:
        if (runClassifications) {
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

            for (int i = 0; i < cNames.length; i++) {
                idMappers[i] = ClassificationManager.get(cNames[i], true).getIdMapper();

                if (gi2FNames[i].length() > 0)
                    idMappers[i].loadMappingFile(gi2FNames[i], IdMapper.MapType.GI, false, new ProgressPercentage());
                if (gi2FNames[i].length() > 0)
                    idMappers[i].loadMappingFile(gi2FNames[i], IdMapper.MapType.GI, false, new ProgressPercentage());
                if (acc2FNames[i].length() > 0)
                    idMappers[i].loadMappingFile(acc2FNames[i], IdMapper.MapType.Accession, false, new ProgressPercentage());
                if (synonyms2FNames[i].length() > 0)
                    idMappers[i].loadMappingFile(synonyms2FNames[i], IdMapper.MapType.Synonyms, false, new ProgressPercentage());
            }
        }

        /*
         * process each file
         */

        for (int i = 0; i < daaFiles.length; i++) {
            final String daaFile = daaFiles[i];
            final String metaDataFile = (metaDataFiles.length > 0 ? metaDataFiles[Math.min(i, metaDataFiles.length - 1)] : "");
            Meganize.apply(new ProgressPercentage(), daaFile, metaDataFile, cNames, minScore, maxExpected, minPercentIdentity, topPercent, minSupportPercent, minSupport, pairedReads, pairedReadsSuffixLength, lcaAlgorithm, readAssignmentMode, weightedLCAPercent, longReads, minPercentReadToCover);
        }
    }
}
