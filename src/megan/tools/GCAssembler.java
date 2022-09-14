/*
 * GCAssembler.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.swing.commands.CommandManager;
import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import jloda.util.progress.ProgressListener;
import jloda.util.progress.ProgressPercentage;
import jloda.util.progress.ProgressSilent;
import megan.assembly.ReadAssembler;
import megan.assembly.ReadDataCollector;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.data.ClassificationCommandHelper;
import megan.core.Document;
import megan.data.IConnector;
import megan.data.IReadBlockIterator;
import megan.main.MeganProperties;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * performs gene-centric assemblies
 * Daniel Huson, 8/2016
 */
public class GCAssembler {
	/**
	 * performs gene-centric assemblies
	 *
	 */
	public static void main(String[] args) {
		try {
			ResourceManager.insertResourceRoot(megan.resources.Resources.class);
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
	 */
	private void run(String[] args) throws UsageException, IOException {
		CommandManager.getGlobalCommands().addAll(ClassificationCommandHelper.getGlobalCommands());

		final var options = new ArgsOptions(args, this, "Gene-centric assembly");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("Copyright (C) 2022 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setAuthors("Daniel H. Huson");

		options.comment("Input and output");
		final var inputFile = options.getOptionMandatory("-i", "input", "Input DAA or RMA6 file", "");
		final var outputFileTemplate = options.getOption("-o", "output", "Output filename template, use %d or %s to represent class id or name, respectively",
				FileUtils.replaceFileSuffix(inputFile.length() == 0 ? "input" : inputFile, "-%d.fasta"));

		options.comment("Classification");

		final var classificationName = options.getOptionMandatory("-fun", "function", "Name of functional classification (choices: "
																					  + StringUtils.toString(ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy(), ", ") + ", none)", "");
		final var selectedClassIds = options.getOptionMandatory("-id", "ids", "Names or ids of classes to assemble, or keyword ALL for all", new String[0]);

		options.comment("Options");

		final var minOverlapReads = options.getOption("-mor", "minOverlapReads", "Minimum overlap for two reads", 20);
		final var minLength = options.getOption("-len", "minLength", "Minimum contig length", 200);
		final var minReads = options.getOption("-reads", "minReads", "Minimum number of reads", 2);
		final var minAvCoverage = options.getOption("-mac", "minAvCoverage", "Minimum average coverage", 1);
		final var doOverlapContigs = options.getOption("-c", "overlapContigs", "Attempt to overlap contigs", true);
		final var minOverlapContigs = options.getOption("-moc", "minOverlapContigs", "Minimum overlap for two contigs", 20);
		final var minPercentIdentityContigs = (float) options.getOption("-mic", "minPercentIdentityContigs", "Mininum percent identity to merge contigs", 98.0);

		options.comment(ArgsOptions.OTHER);

		final var desiredNumberOfThreads = options.getOption("-t", "threads", "Number of worker threads", 8);
		final var veryVerbose = options.getOption("-vv", "veryVerbose", "Report program is very verbose detail", false);

		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file",megan.main.Megan6.getDefaultPropertiesFile());
		options.done();

		MeganProperties.initializeProperties(propertiesFile);

		final var doAllClasses = selectedClassIds.length == 1 && selectedClassIds[0].equalsIgnoreCase("all");

		final var supportedClassifications = ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy();
		if (!supportedClassifications.contains(classificationName) && !classificationName.equalsIgnoreCase("none")) {
			throw new UsageException("--function: Must be one of: " + StringUtils.toString(supportedClassifications, ",") + ", none");
		}

		// todo; fun=none mode does not work

		if (classificationName.equalsIgnoreCase("none") && !(selectedClassIds.length == 1 && selectedClassIds[0].equalsIgnoreCase("all")))
			throw new UsageException("--function 'none': --ids must be 'all' ");

		if (options.isVerbose())
			System.err.println("Opening file: " + inputFile);


		final var document = new Document();
		document.getMeganFile().setFileFromExistingFile(inputFile, true);
		if (!(document.getMeganFile().isDAAFile() || document.getMeganFile().isRMA6File()))
			throw new IOException("Input file has wrong type: must be meganized DAA file or RMA6 file");
		if (document.getMeganFile().isDAAFile() && document.getConnector() == null)
			throw new IOException("Input DAA file: Must first be meganized");

		final Classification classification;
		final List<Integer> classIdsList;
		if (classificationName.equalsIgnoreCase("none")) {
			classification = null; // all reads!
			classIdsList = Collections.singletonList(0); // all reads!
		} else {
			classification = ClassificationManager.get(classificationName, true);

			final var connector = document.getConnector();
			final var classificationBlock = connector.getClassificationBlock(classificationName);

			if (doAllClasses) {
				classIdsList = new ArrayList<>(classificationBlock.getKeySet().size());
				for (Integer id : classificationBlock.getKeySet()) {
					if (id > 0 && classificationBlock.getSum(id) > 0)
						classIdsList.add(id);
					classIdsList.sort(Integer::compareTo);
				}
			} else {
				classIdsList = new ArrayList<>(selectedClassIds.length);
				for (String str : selectedClassIds) {
					if (NumberUtils.isInteger(str))
						classIdsList.add(NumberUtils.parseInt(str));
					else {
						if (classification != null) {
							int id = classification.getName2IdMap().get(str);
							if (id != 0)
								classIdsList.add(NumberUtils.parseInt(str));
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

		final var numberOfThreads = Math.min(classIdsList.size(), desiredNumberOfThreads);

		var numberOfFilesProduced = new LongAdder();
		var totalContigs = new LongAdder();

		final var executorService = Executors.newFixedThreadPool(Math.max(1,doOverlapContigs?numberOfThreads/2:numberOfThreads));

		try (ProgressListener totalProgress = (veryVerbose ? new ProgressSilent() : new ProgressPercentage("Progress:", classIdsList.size()))) {
			var exception = new Single<Exception>();
			final var doc = new Document();
			doc.getMeganFile().setFileFromExistingFile(inputFile, true);
			doc.loadMeganFile();
			final var connector = doc.getConnector();
			for (var classId : classIdsList) {
				if (exception.isNull()) {
					try (final var it = getIterator(connector, classificationName, classId)) {
						final var readAssembler = new ReadAssembler(veryVerbose);
						final var readData = ReadDataCollector.apply(it, veryVerbose ? new ProgressPercentage() : new ProgressSilent());

						executorService.submit(() -> {
							try {
								final var progress = (veryVerbose ? new ProgressPercentage() : new ProgressSilent());
								final var className = classification != null ? classification.getName2IdMap().get(classId) : "none";
								if (veryVerbose)
									System.err.println("++++ Assembling class " + classId + ": " + className + ": ++++");

								final var outputFile = createOutputFileName(outputFileTemplate, classId, className, classIdsList.size());
								final var label = classificationName + ". Id: " + classId;

								readAssembler.computeOverlapGraph(label, minOverlapReads, readData, progress);

								var count = readAssembler.computeContigs(minReads, minAvCoverage, minLength, progress);

								if (veryVerbose)
									System.err.printf("Number of contigs:%6d%n", count);

								if (doOverlapContigs) {
									count = ReadAssembler.mergeOverlappingContigs(4, progress, minPercentIdentityContigs, minOverlapContigs, readAssembler.getContigs(), veryVerbose);
									if (veryVerbose)
										System.err.printf("Remaining contigs:%6d%n", count);
								}

								try (var w = new BufferedWriter(new FileWriter(outputFile))) {
									readAssembler.writeContigs(w, progress);
									if (veryVerbose) {
										System.err.println("Contigs written to: " + outputFile);
										readAssembler.reportContigStats();
									}
									numberOfFilesProduced.increment();
									totalContigs.add(readAssembler.getContigs().size());
								}
								synchronized (totalProgress) {
									totalProgress.incrementProgress();
								}
							} catch (Exception ex) {
								exception.setIfCurrentValueIsNull(ex);
							}
						});
					}
				}
			}
			executorService.shutdown();
			try {
				executorService.awaitTermination(1000, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				exception.set(e);
			}
			if (exception.isNotNull())
				throw new IOException(exception.get());

		} finally {
			executorService.shutdownNow();
		}

		if (options.isVerbose()) {
			System.err.println("Number of files produced: " + numberOfFilesProduced.intValue());
			System.err.println("Total number of contigs:  " + totalContigs.intValue());
		}
	}

	/**
	 * create the output file name
	 *
	 * @return output file name
	 */
	private String createOutputFileName(String outputFileTemplate, int classId, String className, int numberOfIds) {
		String outputFile = null;
		if (outputFileTemplate.contains("%d"))
			outputFile = outputFileTemplate.replaceAll("%d", "" + classId);
		if (outputFileTemplate.contains("%s"))
			outputFile = (outputFile == null ? outputFileTemplate : outputFile).replaceAll("%s", StringUtils.toCleanName(className));
		if (outputFile == null && numberOfIds > 1)
			outputFile = FileUtils.replaceFileSuffix(outputFileTemplate, "-" + classId + ".fasta");
		if (outputFile == null)
			outputFile = outputFileTemplate;
		return outputFile;
	}

	/**
	 * get the iterator. It will be an interator over all reads in a given class, if classificationName and classId given, otherwise, over all reads
	 *
	 * @return iterator
	 */
	private IReadBlockIterator getIterator(IConnector connector, String classificationName, int classId) throws IOException {
		if (classificationName.equalsIgnoreCase("none"))
			return connector.getAllReadsIterator(0, 10, true, true);
		else
			return connector.getReadsIterator(classificationName, classId, 0, 10, true, true);
	}
}