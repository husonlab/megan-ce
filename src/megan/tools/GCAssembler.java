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
import megan.assembly.ReadAssembler;
import megan.assembly.ReadData;
import megan.assembly.ReadDataCollector;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.data.ClassificationCommandHelper;
import megan.core.Document;
import megan.data.IClassificationBlock;
import megan.data.IConnector;
import megan.data.IReadBlockIterator;
import megan.main.Megan6;
import megan.main.MeganProperties;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * performs gene-centric assemblies
 * Daniel Huson, 8/2016
 */
public class GCAssembler {
    /**
     * performs gene-centric assemblies
     *
     * @param args
     * @throws UsageException
     * @throws IOException
     */
    public static void main(String[] args) {
        try {
            ResourceManager.addResourceRoot(Megan6.class, "megan.resources");
            ProgramProperties.setProgramName("GCAssembler");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new GCAssembler()).run(args);
            System.err.println("Total time:  " + PeakMemoryUsageMonitor.getSecondsSinceStartString());
            System.err.println("Peak memory: " + PeakMemoryUsageMonitor.getPeakUsageString());
            System.exit(0);
        } catch (Exception ex) {
            Basic.caught(ex);
            System.exit(1);
        }
    }

    /**
     * parse arguments the program
     *
     * @param args
     * @throws UsageException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void run(String[] args) throws UsageException, IOException, ClassNotFoundException, CanceledException {
        CommandManager.getGlobalCommands().addAll(ClassificationCommandHelper.getGlobalCommands());

        final ArgsOptions options = new ArgsOptions(args, this, "Gene-centric assembly");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2019 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and output");
        final String inputFile = options.getOptionMandatory("-i", "input", "Input DAA or RMA6 file", "");
        final String outputFileTemplate = options.getOption("-o", "output", "Output filename template, use %d or %s to represent class id or name, respectively",
                Basic.replaceFileSuffix(inputFile.length() == 0 ? "input" : inputFile, "-%d.fasta"));

        options.comment("Classification");

        final String classificationName = options.getOptionMandatory("-fun", "function", "Name of functional classification (choices: "
                + Basic.toString(ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy(), ", ") + ", none)", "");
        final String[] selectedClassIds = options.getOptionMandatory("-id", "ids", "Names or ids of classes to assemble, or keyword ALL for all", new String[0]);

        options.comment("Options");

        final int minOverlapReads = options.getOption("-mor", "minOverlapReads", "Minimum overlap for two reads", 20);
        final int minLength = options.getOption("-len", "minLength", "Minimum contig length", 200);
        final int minReads = options.getOption("-reads", "minReads", "Minimum number of reads", 2);
        final int minAvCoverage = options.getOption("-mac", "minAvCoverage", "Minimum average coverage", 1);
        final boolean doOverlapContigs = options.getOption("-c", "overlapContigs", "Attempt to overlap contigs", true);
        final int minOverlapContigs = options.getOption("-moc", "minOverlapContigs", "Minimum overlap for two contigs", 20);
        final float minPercentIdentityContigs = (float) options.getOption("-mic", "minPercentIdentityContigs", "Mininum percent identity to merge contigs", 98.0);

        options.comment(ArgsOptions.OTHER);

        final int desiredNumberOfThreads = options.getOption("-t", "threads", "Number of worker threads", 4);
        final boolean veryVerbose = options.getOption("-vv", "veryVerbose", "Report program is very verbose detail", false);

        options.done();

        final boolean doAllClasses = selectedClassIds.length == 1 && selectedClassIds[0].equalsIgnoreCase("all");

        final String propertiesFile;
        if (ProgramProperties.isMacOS())
            propertiesFile = System.getProperty("user.home") + "/Library/Preferences/Megan.def";
        else
            propertiesFile = System.getProperty("user.home") + File.separator + ".Megan.def";
        MeganProperties.initializeProperties(propertiesFile);

        final Set<String> supportedClassifications = ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy();
        if (!supportedClassifications.contains(classificationName) && !classificationName.equalsIgnoreCase("none")) {
            throw new UsageException("--function: Must be one of: " + Basic.toString(supportedClassifications, ",") + ", none");
        }

        // todo; fun=none mode does not work

        if (classificationName.equalsIgnoreCase("none") && !(selectedClassIds.length == 1 && selectedClassIds[0].equalsIgnoreCase("all")))
            throw new UsageException("--function 'none': --ids must be 'all' ");

        if (options.isVerbose())
            System.err.println("Opening file: " + inputFile);


        final Document document = new Document();
        document.getMeganFile().setFileFromExistingFile(inputFile, true);
        if (!(document.getMeganFile().isDAAFile() || document.getMeganFile().isRMA6File()))
            throw new IOException("Input file has wrong type: must be meganized DAA file or RMA6 file");
        if (document.getMeganFile().isDAAFile() && document.getConnector() == null)
            throw new IOException("Input DAA file: Must first be meganized");

        final Classification classification;
        final List<Integer> classIdsList;
        if (classificationName.equalsIgnoreCase("none")) {
            classification = null;
            classIdsList = Collections.singletonList(0);
        } else {
            final IClassificationBlock classificationBlock;
            classification = ClassificationManager.get(classificationName, true);

            final IConnector connector = document.getConnector();
            classificationBlock = connector.getClassificationBlock(classificationName);

            if (doAllClasses) {
                classIdsList = new ArrayList<>(classificationBlock.getKeySet().size());
                for (Integer id : classificationBlock.getKeySet()) {
                    if (id > 0 && classificationBlock.getSum(id) > 0)
                        classIdsList.add(id);
                    classIdsList.sort((i, j) -> i.compareTo(j));
                }
            } else {
                classIdsList = new ArrayList<>(selectedClassIds.length);
                for (String str : selectedClassIds) {
                    if (Basic.isInteger(str))
                        classIdsList.add(Basic.parseInt(str));
                    else {
                        if (classification != null) {
                            int id = classification.getName2IdMap().get(str);
                            if (id != 0)
                                classIdsList.add(Basic.parseInt(str));
                            else
                                System.err.println("Unknown class: " + str);
                        }
                    }
                }
            }
        }
        if (options.isVerbose())
            System.err.println("Number of classes to assemble: " + classIdsList.size());

        if (classIdsList.size() == 0)
            throw new UsageException("No valid classes specified");

        final int numberOfThreads = Math.min(classIdsList.size(), desiredNumberOfThreads);
        final int remainingThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - numberOfThreads);
        final ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        final CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

        final BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(classIdsList.size() + numberOfThreads, true);
        final Integer sentinel = Integer.MAX_VALUE;

        final int[] numberOfFilesProduced = new int[numberOfThreads];
        final int[] totalContigs = new int[numberOfThreads];

        final ProgressListener totalProgress = (veryVerbose ? new ProgressSilent() : new ProgressPercentage("Progress:", classIdsList.size()));

        for (int t = 0; t < numberOfThreads; t++) {
            final int threadNumber = t;

            service.submit(() -> {
                try {
                    final ProgressListener progress = (veryVerbose ? new ProgressPercentage() : new ProgressSilent());

                    final ReadAssembler readAssembler = new ReadAssembler(veryVerbose);
                    final Document doc = new Document();
                    doc.getMeganFile().setFileFromExistingFile(inputFile, true);
                    doc.loadMeganFile();
                    final IConnector connector = doc.getConnector();

                    while (true) {
                        Integer classId = queue.take();
                        if (classId.equals(sentinel))
                            return;
                        try (final IReadBlockIterator it = getIterator(connector, classificationName, classId)) {
                            final List<ReadData> readData = ReadDataCollector.apply(it, veryVerbose ? new ProgressPercentage() : new ProgressSilent());
                            final String className = classification != null ? classification.getName2IdMap().get(classId) : "none";
                            if (veryVerbose)
                                System.err.println("++++ Assembling class " + +classId + ": " + className + ": ++++");

                            final String outputFile = createOutputFileName(outputFileTemplate, classId, className, classIdsList.size());
                            final String label = classificationName + ". Id: " + classId;

                            readAssembler.computeOverlapGraph(label, minOverlapReads, readData, progress);

                            int count = readAssembler.computeContigs(minReads, minAvCoverage, minLength, progress);

                            if (veryVerbose)
                                System.err.println(String.format("Number of contigs:%6d", count));

                            if (doOverlapContigs) {
                                count = ReadAssembler.mergeOverlappingContigs(remainingThreads, progress, minPercentIdentityContigs, minOverlapContigs, readAssembler.getContigs(), veryVerbose);
                                if (veryVerbose)
                                    System.err.println(String.format("Remaining contigs:%6d", count));
                            }

                            try (Writer w = new BufferedWriter(new FileWriter(outputFile))) {
                                readAssembler.writeContigs(w, progress);
                                if (veryVerbose) {
                                    System.err.println("Contigs written to: " + outputFile);
                                    readAssembler.reportContigStats();
                                }
                                numberOfFilesProduced[threadNumber]++;
                                totalContigs[threadNumber] += readAssembler.getContigs().size();
                            }
                        }
                        synchronized (totalProgress) {
                            totalProgress.incrementProgress();
                        }
                    }
                } catch (Exception e) {
                    Basic.caught(e);
                    if (e instanceof CanceledException) {
                        System.exit(1);
                    }
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        for (Integer classId : classIdsList) {
            try {
                queue.put(classId);
            } catch (InterruptedException e) {
                Basic.caught(e);
            }
        }

        try {
            for (int i = 0; i < numberOfThreads; i++)
                queue.put(sentinel);
        } catch (InterruptedException e) {
            Basic.caught(e);
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Basic.caught(e);
        } finally {
            service.shutdownNow();
        }

        totalProgress.close();

        if (options.isVerbose()) {
            System.err.println("Number of files produced: " + Basic.getSum(numberOfFilesProduced));
            System.err.println("Total number of contigs:  " + Basic.getSum(totalContigs));
        }
    }

    /**
     * create the output file name
     *
     * @param outputFileTemplate
     * @param classId
     * @param className
     * @param numberOfIds
     * @return output file name
     */
    private String createOutputFileName(String outputFileTemplate, int classId, String className, int numberOfIds) {
        String outputFile = null;
        if (outputFileTemplate.contains("%d"))
            outputFile = outputFileTemplate.replaceAll("%d", "" + classId);
        if (outputFileTemplate.contains("%s"))
            outputFile = (outputFile == null ? outputFileTemplate : outputFile).replaceAll("%s", Basic.toCleanName(className));
        if (outputFile == null && numberOfIds > 1)
            outputFile = Basic.replaceFileSuffix(outputFileTemplate, "-" + classId + ".fasta");
        if (outputFile == null)
            outputFile = outputFileTemplate;
        return outputFile;
    }

    /**
     * get the iterator
     *
     * @param connector
     * @param classificationName
     * @param classId
     * @return iterator
     * @throws IOException
     */
    private IReadBlockIterator getIterator(IConnector connector, String classificationName, int classId) throws IOException {
        if (classificationName.equalsIgnoreCase("none"))
            return connector.getAllReadsIterator(0, 10, true, true);
        else
            return connector.getReadsIterator(classificationName, classId, 0, 10, true, true);
    }
}