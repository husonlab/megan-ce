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

import jloda.fx.util.ProgramExecutorService;
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
import megan.core.Document;
import megan.daa.Meganize;
import megan.main.Megan6;
import megan.main.MeganProperties;
import megan.util.DAAFileFilter;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

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
            ResourceManager.addResourceRoot(Megan6.class, "megan.resources");
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
    private void run(String[] args) throws UsageException, IOException, ClassNotFoundException, CanceledException, SQLException {
        CommandManager.getGlobalCommands().addAll(ClassificationCommandHelper.getGlobalCommands());

        final ArgsOptions options = new ArgsOptions(args, this, "Prepares ('meganizes') a DIAMOND .daa file for use with MEGAN");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2019 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Files");
        final String[] daaFiles = options.getOptionMandatory("-i", "in", "Input DAA file(s). Each is meganized separately", new String[0]);
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
        final int minSupport;
        final float minSupportPercent;
        {
            final float minSupportPercent0 = options.getOption("-supp", "minSupportPercent", "Min support as percent of assigned reads (0==off)", Document.DEFAULT_MINSUPPORT_PERCENT);
            final int minSupport0 = options.getOption("-sup", "minSupport", "Min support (0==off)", Document.DEFAULT_MINSUPPORT);
            if (minSupportPercent0 != Document.DEFAULT_MINSUPPORT_PERCENT && minSupport0 == Document.DEFAULT_MINSUPPORT) {
                minSupportPercent = minSupportPercent0;
                minSupport = 0;
            } else if (minSupportPercent0 == Document.DEFAULT_MINSUPPORT_PERCENT && minSupport0 != Document.DEFAULT_MINSUPPORT) {
                minSupportPercent = 0;
                minSupport = minSupport0;
            } else if (minSupportPercent0 != Document.DEFAULT_MINSUPPORT_PERCENT) {
                throw new IOException("Please specify a value for either --minSupport or --minSupportPercent, but not for both");
            } else {
                minSupportPercent = minSupportPercent0;
                minSupport = minSupport0;
            }
        }
        final float minPercentReadToCover = options.getOption("-mrc", "minPercentReadCover", "Min percent of read length to be covered by alignments", Document.DEFAULT_MIN_PERCENT_READ_TO_COVER);
        final float minPercentReferenceToCover = options.getOption("-mrefc", "minPercentReferenceCover", "Min percent of reference length to be covered by alignments", Document.DEFAULT_MIN_PERCENT_REFERENCE_TO_COVER);

        final Document.LCAAlgorithm lcaAlgorithm = Document.LCAAlgorithm.valueOfIgnoreCase(options.getOption("-alg", "lcaAlgorithm", "Set the LCA algorithm to use for taxonomic assignment",
                Document.LCAAlgorithm.values(), longReads ? Document.DEFAULT_LCA_ALGORITHM_LONG_READS.toString() : Document.DEFAULT_LCA_ALGORITHM_SHORT_READS.toString()));
        final float lcaCoveragePercent = options.getOption("-lcp", "lcaCoveragePercent", "Set the percent for the LCA to cover", longReads ? 80 : Document.DEFAULT_LCA_COVERAGE_PERCENT);

        final Document.ReadAssignmentMode readAssignmentMode = Document.ReadAssignmentMode.valueOfIgnoreCase(options.getOption("-ram", "readAssignmentMode", "Set the read assignment mode",
                Document.ReadAssignmentMode.values(), longReads ? Document.DEFAULT_READ_ASSIGNMENT_MODE_LONG_READS.toString() : Document.DEFAULT_READ_ASSIGNMENT_MODE_SHORT_READS.toString()));

        final String contaminantsFile = options.getOption("-cf", "conFile", "File of contaminant taxa (one Id or name per line)", "");

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
        ProgramExecutorService.setNumberOfCoresToUse(options.getOption("-p","threads","Number of threads",8));
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

        for (String fileName : daaFiles) {
            Basic.checkFileReadableNonEmpty(fileName);
            if (!DAAFileFilter.getInstance().accept(fileName))
                throw new IOException("File not in DAA format (or incorrect file suffix?): " + fileName);
        }

        for (String fileName : metaDataFiles) {
            Basic.checkFileReadableNonEmpty(fileName);
        }

        if (metaDataFiles.length > 1 && metaDataFiles.length != daaFiles.length) {
            throw new IOException("Number of metadata files (" + metaDataFiles.length + ") doesn't match number of DAA files (" + daaFiles.length + ")");
        }


        final Collection<String> mapDBClassifications= AccessAccessionMappingDatabase.getContainedClassificationsIfDBExists(mapDBFile);
        if(mapDBClassifications.size()>0 && (Basic.hasPositiveLengthValue(class2AccessionFile)|| Basic.hasPositiveLengthValue(class2SynonymsFile)))
            throw new UsageException("Illegal to use both --mapDB and ---acc2... or --syn2... options");

        if(mapDBClassifications.size()>0)
            ClassificationManager.setMeganMapDBFile(mapDBFile);

        final ArrayList<String> cNames = new ArrayList<>();
        for (String cName : ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy()) {
            if (mapDBClassifications.contains(cName) ||  class2AccessionFile.get(cName).length() > 0 || class2SynonymsFile.get(cName).length() > 0)
                cNames.add(cName);
        }
        if (cNames.size() > 0)
            System.err.println("Functional classifications to use: " + Basic.toString(cNames, ", "));

        final IdMapper taxonIdMapper = ClassificationManager.get(Classification.Taxonomy, true).getIdMapper();
        final IdMapper[] idMappers = new IdMapper[cNames.size()];

        // Load all mapping files:
        if (runClassifications) {
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
        }

        /*
         * process each file
         */

        for (int i = 0; i < daaFiles.length; i++) {
            final String daaFile = daaFiles[i];
            System.err.println("Meganizing: " + daaFile);
            final String metaDataFile = (metaDataFiles.length > 0 ? metaDataFiles[Math.min(i, metaDataFiles.length - 1)] : "");
            Meganize.apply(new ProgressPercentage(), daaFile, metaDataFile, cNames, minScore, maxExpected, minPercentIdentity,
                    topPercent, minSupportPercent, minSupport, pairedReads, pairedReadsSuffixLength, lcaAlgorithm, readAssignmentMode, lcaCoveragePercent, longReads,
                    minPercentReadToCover, minPercentReferenceToCover, contaminantsFile);
        }
    }
}
