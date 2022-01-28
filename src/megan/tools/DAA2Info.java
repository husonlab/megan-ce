/*
 * DAA2Info.java Copyright (C) 2022 Daniel H. Huson
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
package megan.tools;

import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import megan.classification.ClassificationManager;
import megan.core.Document;
import megan.daa.connector.DAAConnector;
import megan.daa.io.DAAHeader;
import megan.daa.io.DAAParser;
import megan.viewer.TaxonomyData;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * provides info on a DAA files
 * Daniel Huson, 11.2016
 */
public class DAA2Info {
    /**
     * DAA 2 info
     *
     * @param args
     * @throws UsageException
     * @throws IOException
     */
    public static void main(String[] args) {
        try {
            ResourceManager.insertResourceRoot(megan.resources.Resources.class);
            ProgramProperties.setProgramName("DAA2Info");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new DAA2Info()).run(args);
            PeakMemoryUsageMonitor.report();
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
     */
    private void run(String[] args) throws UsageException, IOException, CanceledException {
        final ArgsOptions options = new ArgsOptions(args, this, "Analyses a DIAMOND file");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2022 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and Output");
        final String daaFile = options.getOptionMandatory("-i", "in", "Input DAA file", "");
        final String outputFile = options.getOption("-o", "out", "Output file (stdout or .gz ok)", "stdout");

        options.comment("Commands");
		final boolean listGeneralInfo = options.getOption("-l", "list", "List general info about file", false);
		final boolean listMoreStuff = options.getOption("-m", "listMore", "List more info about file (if meganized)", false);

		final Set<String> listClass2Count = new HashSet<>(options.getOption("-c2c", "class2count", "List class to count for named classification(s) (Possible values: " + StringUtils.toString(ClassificationManager.getAllSupportedClassifications(), " ") + ")", new ArrayList<>()));
		final Set<String> listRead2Class = new HashSet<>(options.getOption("-r2c", "read2class", "List read to class assignments for named classification(s) (Possible values: " + StringUtils.toString(ClassificationManager.getAllSupportedClassifications(), " ") + ")", new ArrayList<>()));
		final boolean reportNames = options.getOption("-n", "names", "Report class names rather than class Id numbers", false);
		final boolean reportPaths = options.getOption("-p", "paths", "Report class paths rather than class Id numbers", false);
		final boolean prefixRank = options.getOption("-r", "prefixRank", "When reporting class paths for taxonomy, prefix single letter to indicate taxonomic rank", false);
		final boolean majorRanksOnly = options.getOption("-mro", "majorRanksOnly", "Only use major taxonomic ranks", false);
		final boolean bacteriaOnly = options.getOption("-bo", "bacteriaOnly", "Only report bacterial reads and counts in taxonomic report", false);
		final boolean viralOnly = options.getOption("-vo", "virusOnly", "Only report viral reads and counts in taxonomic report", false);
		final boolean ignoreUnassigned = options.getOption("-u", "ignoreUnassigned", "Don't report on reads that are unassigned", true);
		final boolean useSummary = options.getOption("-s", "sum", "Use summarized rather than assigned counts when listing class to count", false);

		final String extractSummaryFile = options.getOption("-es", "extractSummaryFile", "Output a MEGAN summary file (contains all classifications, but no reads or alignments)", "");
        options.done();

        final int taxonomyRoot;
        if (bacteriaOnly && viralOnly)
            throw new UsageException("Please specify only one of -bo and -vo");
        else if (bacteriaOnly)
            taxonomyRoot = TaxonomyData.BACTERIA_ID;
        else if (viralOnly)
            taxonomyRoot = TaxonomyData.VIRUSES_ID;
        else
            taxonomyRoot = TaxonomyData.ROOT_ID;

        final Boolean isMeganized = DAAParser.isMeganizedDAAFile(daaFile, true);

        final Document doc = new Document();
        if (isMeganized) {
            doc.getMeganFile().setFileFromExistingFile(daaFile, true);
            doc.loadMeganFile();
        }

		try (Writer outs = new BufferedWriter(new OutputStreamWriter(FileUtils.getOutputStreamPossiblyZIPorGZIP(outputFile)))) {
			if (listGeneralInfo || listMoreStuff) {
				final DAAHeader daaHeader = new DAAHeader(daaFile, true);
				outs.write(String.format("# Number of reads: %,d\n", daaHeader.getQueryRecords()));
				outs.write(String.format("# Alignment mode:  %s\n", daaHeader.getAlignMode().toString().toUpperCase()));
				outs.write(String.format("# Is meganized:    %s\n", isMeganized));

				if (isMeganized) {
					outs.write("# Classifications:");
					final DAAConnector connector = new DAAConnector(daaFile);
					for (String classificationName : connector.getAllClassificationNames()) {
                        if (ClassificationManager.getAllSupportedClassifications().contains(classificationName)) {
                            outs.write(" " + classificationName);
                        }
                    }
                    outs.write("\n");

                    if (listMoreStuff) {
                        outs.write("# Meganization summary:\n");
                        outs.write(doc.getDataTable().getSummary().replaceAll("^", "## ").replaceAll("\n", "\n## ") + "\n");
                    }
                }
            }

            if (listClass2Count.size() > 0) {
                if (isMeganized)
                    RMA2Info.reportClass2Count(doc, listGeneralInfo, listMoreStuff, reportPaths, reportNames, prefixRank, ignoreUnassigned, majorRanksOnly, listClass2Count, taxonomyRoot,useSummary, outs);
                else
                    System.err.println("Can't list class-to-count: file has not been meganized");
            }

            if (listRead2Class.size() > 0) {
                if (isMeganized)
                    RMA2Info.reportRead2Count(doc, listGeneralInfo, listMoreStuff, reportPaths, reportNames, prefixRank, ignoreUnassigned, majorRanksOnly, listRead2Class, taxonomyRoot, outs);
                else
                    System.err.println("Can't list read-to-count: file has not been meganized");
            }
        }
        if (extractSummaryFile.length() > 0) {
            try (Writer w = new FileWriter(extractSummaryFile)) {
                doc.getDataTable().write(w);
                doc.getSampleAttributeTable().write(w, false, true);
            }
        }
    }
}
