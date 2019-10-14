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

import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Document;
import megan.daa.io.DAAParser;
import megan.data.IClassificationBlock;
import megan.data.IConnector;
import megan.dialogs.export.ReadsExporter;
import megan.dialogs.export.analysis.FrameShiftCorrectedReadsExporter;
import megan.dialogs.extractor.ReadsExtractor;
import megan.main.Megan6;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * extracts reads from a DAA or RMA file, by taxa
 * Daniel Huson, 1.2019
 */
public class ReadExtractorTool {
    /**
     * ReadExtractorTool
     *
     * @param args
     * @throws UsageException
     * @throws IOException
     */
    public static void main(String[] args) {
        try {
            ResourceManager.addResourceRoot(Megan6.class, "megan.resources");
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
     *
     * @param args
     * @throws UsageException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void run(String[] args) throws UsageException, IOException, ClassNotFoundException, CanceledException {
        final ArgsOptions options = new ArgsOptions(args, this, "Extracts reads from a DAA or RMA file by classification");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2019 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and Output");
        final ArrayList<String> inputFiles = new ArrayList<>(Arrays.asList(options.getOptionMandatory("-i", "input", "Input DAA and/or RMA file(s)", new String[0])));
        final ArrayList<String> outputFiles = new ArrayList<>(Arrays.asList(options.getOption("-o", "output", "Output file(s). Use %t for class name and %i for class id. (Use - for stdout, directory ok, .gz ok)", new String[]{"-"})));

        options.comment("Commands");
        final boolean extractCorrectedReads = options.getOption("-fsc", "frameShiftCorrect", "Extract frame-shift corrected reads", false);
        final String classificationName = options.getOption("-c", "classification", "The classification to use", ClassificationManager.getAllSupportedClassifications(), "");
        final ArrayList<String> classNames = new ArrayList<>(Arrays.asList(options.getOption("-n", "classNames", "Names (or ids) of classes to extract reads from (default: extract all classes)", new String[0])));
        final boolean all = options.getOption("-a", "all", "Extract all reads (not by class)", false);

        options.comment(ArgsOptions.OTHER);
        final boolean ignoreExceptions = options.getOption("-IE", "ignoreExceptions", "Ignore exceptions and continue processing", false);
        final boolean gzOutputFiles = options.getOption("-gz", "gzipOutputFiles", "If output directory is given, gzip files written to directory", true);
        options.done();

        if (classificationName.equals("") != all) {
            throw new UsageException("Must specific either option --classification or --all");
        }


        final boolean useStdout = (outputFiles.size() == 1 && outputFiles.get(0).equals("-"));

        if (outputFiles.size() == 1 && Basic.isDirectory(outputFiles.get(0))) {
            final String directory = outputFiles.get(0);
            outputFiles.clear();
            for (String name : inputFiles) {
                if (all)
                    outputFiles.add(new File(directory, Basic.replaceFileSuffix(Basic.getFileNameWithoutPath(name), "-all.txt" + (gzOutputFiles ? ".gz" : ""))).getPath());
                else
                    outputFiles.add(new File(directory, Basic.replaceFileSuffix(Basic.getFileNameWithoutPath(name), "-%i-%t.txt" + (gzOutputFiles ? ".gz" : ""))).getPath());
            }
        } else if (inputFiles.size() != outputFiles.size()) {
            throw new UsageException("Number of input and output files must be equal, or output must be '-' or a directory");
        }

        int totalReads = 0;

        for (int i = 0; i < inputFiles.size(); i++) {
            final String inputFile = inputFiles.get(i);
            final String outputFile = (useStdout ? "-" : outputFiles.get(i));

            try {
                if (inputFile.toLowerCase().endsWith("daa") && !DAAParser.isMeganizedDAAFile(inputFile, true)) {
                    throw new IOException("Warning: non-meganized DAA file: " + inputFile);
                } else {
                    totalReads += extract(extractCorrectedReads, classificationName, classNames, all, inputFile, outputFile);
                }
            } catch (Exception ex) {
                if (ignoreExceptions)
                    System.err.println(Basic.getShortName(ex.getClass()) + ": " + ex.getMessage() + ", while processing file: " + inputFile);
                else
                    throw ex;
            }
        }
        System.err.println(String.format("Reads extracted: %,d", totalReads));
    }

    /**
     * extract all reads for each specified classes, or all classes, if none specified
     *
     * @param extractCorrectedReads
     * @param classificationName
     * @param classNames
     * @param inputFile
     * @param outputFile
     * @throws IOException
     * @throws CanceledException
     */
    private int extract(boolean extractCorrectedReads, String classificationName, Collection<String> classNames, boolean all, String inputFile, String outputFile) throws IOException, CanceledException {
        final Document doc = new Document();
        doc.getMeganFile().setFileFromExistingFile(inputFile, true);
        doc.loadMeganFile();

        final IConnector connector = doc.getConnector();

        if (extractCorrectedReads && doc.getBlastMode() != BlastMode.BlastX)
            throw new IOException("Frame-shift correction only possible when BlastModeUtils is BLASTX");

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

            final SortedSet<Integer> classIds = new TreeSet<>();
            final Classification classification = ClassificationManager.get(classificationName, true);
            final IClassificationBlock classificationBlock = connector.getClassificationBlock(classificationName);

            if (classNames.size() == 0)// no class names given, use all
            {
                for (Integer classId : classificationBlock.getKeySet()) {
                    if (classId > 0)
                        classIds.add(classId);
                }
            } else {
                int warnings = 0;

                for (String name : classNames) {
                    if (Basic.isInteger(name))
                        classIds.add(Basic.parseInt(name));
                    else {
                        int id = classification.getName2IdMap().get(name);
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

            try (ProgressPercentage progress = new ProgressPercentage("Processing file: " + inputFile)) {
                if (!extractCorrectedReads) {
                    return ReadsExtractor.extractReadsByFViewer(classificationName, progress, classIds, "", outputFile, doc, false);
                } else {
                    return FrameShiftCorrectedReadsExporter.export(classificationName, classIds, connector, outputFile, progress);
                }
            }
        }
    }
}
