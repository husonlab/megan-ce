/*
 * ReadExtractorTool.java Copyright (C) 2024 Daniel H. Huson
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

import jloda.seq.BlastMode;
import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import jloda.util.progress.ProgressPercentage;
import megan.classification.ClassificationManager;
import megan.core.Document;
import megan.daa.io.DAAParser;
import megan.dialogs.export.ReadsExporter;
import megan.dialogs.export.analysis.FrameShiftCorrectedReadsExporter;
import megan.dialogs.extractor.ReadsExtractor;
import megan.main.MeganProperties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

/**
 * extracts reads from a DAA or RMA file, by taxa
 * Daniel Huson, 1.2019
 */
public class ReadExtractorTool {
    /**
     * ReadExtractorTool
      */
    public static void main(String[] args) {
        try {
            ResourceManager.insertResourceRoot(megan.resources.Resources.class);
            ProgramProperties.setProgramName("ReadExtractorTool");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new ReadExtractorTool()).run(args);
            PeakMemoryUsageMonitor.report();
            System.exit(0);
        } catch (Exception ex) {
            Basic.caught(ex);
            System.exit(1);
        }
    }

    /**
     * run
     */
    private void run(String[] args) throws UsageException, IOException {
        final var options = new ArgsOptions(args, this, "Extracts reads from a DAA or RMA file by classification");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2024. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and Output");
        final var inputFiles = new ArrayList<>(Arrays.asList(options.getOptionMandatory("-i", "input", "Input DAA and/or RMA file(s)", new String[0])));
        final var outputFiles = new ArrayList<>(Arrays.asList(options.getOption("-o", "output", "Output file(s). Use %f for input file name, %t for class name and %i for class id. (Directory, stdout, .gz ok)", new String[]{"stdout"})));

        options.comment("Options");
        final var extractCorrectedReads = options.getOption("-fsc", "frameShiftCorrect", "Extract frame-shift corrected reads", false);
        final var classificationName = options.getOption("-c", "classification", "The classification to use", ClassificationManager.getAllSupportedClassifications(), "");
        final var classNames = new ArrayList<>(Arrays.asList(options.getOption("-n", "classNames", "Names (or ids) of classes to extract reads from (default: extract all classes)", new String[0])));
        final var allBelow = options.getOption("-b", "allBelow", "Report all reads assigned to or below a named class", false);

        final var all = options.getOption("-a", "all", "Extract all reads (not by class)", false);

        options.comment(ArgsOptions.OTHER);
        final var ignoreExceptions = options.getOption("-IE", "ignoreExceptions", "Ignore exceptions and continue processing", false);
        final var gzOutputFiles = options.getOption("-gz", "gzipOutputFiles", "If output directory is given, gzip files written to directory", true);

        final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file",megan.main.Megan6.getDefaultPropertiesFile());
        options.done();

        MeganProperties.initializeProperties(propertiesFile);

        if (classificationName.equals("") != all)
            throw new UsageException("Must specific either option --classification or --all, but not both");

        if(allBelow && all)
            throw new UsageException("Must specific either option --allBelow or --all, but not both");

        if(allBelow && classNames.size()==0)
            throw new UsageException("When using --allBelow, must specify --classNames");

        if(allBelow && extractCorrectedReads)
            throw new UsageException("Option --allBelow is not implemented for --extractCorrectedReads");

		if (outputFiles.size() == 1 && outputFiles.get(0).equals("stdout")) {
			outputFiles.clear();
			for (var i = 0; i < inputFiles.size(); i++)
				outputFiles.add("stdout");
		} else if (outputFiles.size() == 1 && FileUtils.isDirectory(outputFiles.get(0))) {
			final var directory = outputFiles.get(0);
			outputFiles.clear();
			for (var name : inputFiles) {
				if (all)
					outputFiles.add(new File(directory, FileUtils.replaceFileSuffix(FileUtils.getFileNameWithoutPath(name), "-all.txt" + (gzOutputFiles ? ".gz" : ""))).getPath());
				else
					outputFiles.add(new File(directory, FileUtils.replaceFileSuffix(FileUtils.getFileNameWithoutPath(name), "-%i-%t.txt" + (gzOutputFiles ? ".gz" : ""))).getPath());
			}
		} else if (inputFiles.size() != outputFiles.size()) {
			throw new UsageException("Number of input and output files must be equal, or output must be 'stdout' or a directory");
        }

        int totalReads = 0;

        for (int i = 0; i < inputFiles.size(); i++) {
            final var inputFile = inputFiles.get(i);
            final var outputFile = outputFiles.get(i);

            try {
                if (inputFile.toLowerCase().endsWith("daa") && !DAAParser.isMeganizedDAAFile(inputFile, true)) {
                    throw new IOException("Warning: non-meganized DAA file: " + inputFile);
                } else {
                    totalReads += extract(extractCorrectedReads, classificationName, classNames, all,allBelow, inputFile, outputFile);
                }
            } catch (Exception ex) {
                if (ignoreExceptions)
                    System.err.println(Basic.getShortName(ex.getClass()) + ": " + ex.getMessage() + ", while processing file: " + inputFile);
                else
                    throw ex;
            }
        }
        System.err.printf("Reads extracted: %,d%n", totalReads);
    }

    /**
     * extract all reads for each specified classes, or all classes, if none specified
     */
    private long extract(boolean extractCorrectedReads, String classificationName, Collection<String> classNames,
                        boolean all, boolean allBelow, String inputFile, String outputFile) throws IOException {
        final var doc = new Document();
        doc.getMeganFile().setFileFromExistingFile(inputFile, true);
        doc.loadMeganFile();

        final var connector = doc.getConnector();

        if (extractCorrectedReads && doc.getBlastMode() != BlastMode.BlastX)
            throw new IOException("Frame-shift correction only possible when BlastMode is BLASTX");

        if (all) {
            try (ProgressPercentage progress = new ProgressPercentage("Processing file: " + inputFile)) {
                if (extractCorrectedReads) {
                    return FrameShiftCorrectedReadsExporter.exportAll(connector, outputFile, progress);
                } else {
                    return ReadsExporter.exportAll(connector, outputFile, progress);
                }
            }
        } else {
            if (!Arrays.asList(connector.getAllClassificationNames()).contains(classificationName)) {
                throw new IOException("Input file does not contain the requested classification '" + classificationName + "'");
            }

            final var classIds = new TreeSet<Integer>();
            final var classification = ClassificationManager.get(classificationName, true);
            final var classificationBlock = connector.getClassificationBlock(classificationName);

            if (classNames.size() == 0)// no class names given, use all
            {
                for (Integer classId : classificationBlock.getKeySet()) {
                    if (classId > 0)
                        classIds.add(classId);
                }
            } else {
                var warnings = 0;

                for (var name : classNames) {
                    if (NumberUtils.isInteger(name))
                        classIds.add(NumberUtils.parseInt(name));
                    else {
                        var id = classification.getName2IdMap().get(name);
                        if (id > 0)
                            classIds.add(id);
                        else {
                            if (warnings++ < 5) {
                                System.err.println("Warning: unknown class: '" + name + "'");
                                if (warnings == 5)
                                    System.err.println("No further warnings");
                            }
                        }
                    }
                }
            }

            try (var progress = new ProgressPercentage("Processing file: " + inputFile)) {
                if (!extractCorrectedReads) {
                    return ReadsExtractor.extractReadsByFViewer(classificationName, progress, classIds, "", outputFile, doc, allBelow);
                } else {
                    return FrameShiftCorrectedReadsExporter.export(classificationName, classIds, connector, outputFile, progress);
                }
            }
        }
    }
}
