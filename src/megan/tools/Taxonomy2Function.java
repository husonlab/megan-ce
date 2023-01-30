/*
 * Taxonomy2Function.java Copyright (C) 2023 Daniel H. Huson
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
import jloda.util.progress.ProgressPercentage;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.core.Document;
import megan.main.MeganProperties;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;

/**
 * Reports taxonomy-by-function classification
 * Daniel Huson, 10.2021
 */
public class Taxonomy2Function {
	/**
	 * taxonomy-by-function classification
	 *
	 */
	public static void main(String[] args) {
		try {
			ResourceManager.insertResourceRoot(megan.resources.Resources.class);
			ProgramProperties.setProgramName("Taxonomy2Function");
			ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

			PeakMemoryUsageMonitor.start();
			(new Taxonomy2Function()).run(args);
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
		final var options = new ArgsOptions(args, this, "Reports taxonomy-by-function classification");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("Copyright (C) 2023 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setAuthors("Daniel H. Huson");

		options.comment("Input and Output");
		final var inputFiles = options.getOptionMandatory("-i", "in", "Input file(s)", new String[0]);
		final var outputFile = options.getOption("-o", "out", "Output file (stdout or .gz ok)", "stdout");

		options.comment("Options");
		final var firstClassificationName = options.getOption("-a", "firstClassification", "first classification name", ClassificationManager.getAllSupportedClassifications(), "Taxonomy");
        final var firstClasses = options.getOption("-ac", "firstClasses", "Class IDs in first classification?", List.of("all"));
        final var secondClassificationName = options.getOption("-b", "secondClassification", "Second classification name", ClassificationManager.getAllSupportedClassifications(), "EGGNOG");
        final var secondClasses = options.getOption("-bc", "secondClasses", "Class IDs in second classifications?", List.of("all"));

		final var firstFormat = options.getOption("-af", "firstFormat", "Format to report first classification class", new String[]{"name", "id", "path"}, "name");
		final var secondFormat = options.getOption("-bf", "secondFormat", "Format to report second classification class", new String[]{"name", "id", "path"}, firstFormat);

		final var listOption = options.getOption("-l", "list", "List counts or read names?", new String[]{"counts", "reads"}, "counts");

		final var majorRanksOnly = options.getOption("-mro", "majorRanksOnly", "Only use major ranks for NCBI taxonomy", false);

		var separator = options.getOption("-s", "separator", "Separator", new String[]{"tab", "comma", "semi-colon"}, "tab");

		final var includeFirstUnassigned = options.getOption("-au", "includeFirstUnassigned", "include reads unassigned in first classification", true);
		final var includeSecondUnassigned = options.getOption("-bu", "includeSecondUnassigned", "include reads unassigned second classification", true);

		options.comment(ArgsOptions.OTHER);
		var showHeaderLine = options.getOption("-sh", "showHeadline", "Show a headline in the output naming classifications and files", false);
		var pathSeparator = options.getOption("-ps", "pathSeparator", "Separator used when reporting paths", new String[]{"::", "|", "tab", "comma", "semi-colon"}, "::");

		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file",megan.main.Megan6.getDefaultPropertiesFile());
		options.done();

		MeganProperties.initializeProperties(propertiesFile);

		if (firstClassificationName.equals(secondClassificationName))
			throw new UsageException("First second classifications must be different");

		for (var file : inputFiles) {
			if (!FileUtils.fileExistsAndIsNonEmpty(file))
				throw new IOException("Can't open input file: " + file);
		}

		if (inputFiles.length > 1 && listOption.equals("reads"))
			throw new UsageException("You must not specify multiple input files and use the option --list reads");

		switch (separator) {
			case "comma" -> separator = "'";
			case "semi-colon" -> separator = ";";
			case "tab" -> separator = "\t";
		}

		switch (pathSeparator) {
			case "comma" -> pathSeparator = "'";
			case "semi-colon" -> pathSeparator = ";";
			case "tab" -> pathSeparator = "\t";
		}

		var firstClassificationMajorRanksOnly=majorRanksOnly && firstClassificationName.equals(Classification.Taxonomy);
		var secondClassificationMajorRanksOnly=majorRanksOnly && secondClassificationName.equals(Classification.Taxonomy);

		if(firstClassificationMajorRanksOnly || secondClassificationMajorRanksOnly){
			ClassificationManager.get(Classification.Taxonomy, true);
		}

		Collection<Integer> firstIds = null;
		if (!(firstClasses.size() == 1 && firstClasses.get(0).equals("all"))) {
			firstIds = new HashSet<>();
			for (var token : firstClasses) {
				if (!NumberUtils.isInteger(token))
					throw new UsageException("--firstClasses: integer expected, got: " + token);
				else
					firstIds.add(NumberUtils.parseInt(token));
			}
		}
		Collection<Integer> secondIds = null;
		if (!(secondClasses.size() == 1 && secondClasses.get(0).equals("all"))) {
			secondIds = new HashSet<>();
			for (var token : secondClasses) {
				if (!NumberUtils.isInteger(token))
					throw new UsageException("--secondClasses: integer expected, got: " + token);
				else
					secondIds.add(NumberUtils.parseInt(token));
			}
		}

		var useReadsTable = listOption.equals("reads");
		var readsTable = new Table<Integer, Integer, ArrayList<String>>();
		var countsTable = new Table<Integer, Integer, int[]>();

		for (var f = 0; f < inputFiles.length; f++) {
			var inputFile = inputFiles[f];
			var progress = new ProgressPercentage("Processing file:", inputFile);

			final var doc = new Document();
			doc.getMeganFile().setFileFromExistingFile(inputFile, true);
			doc.loadMeganFile();

			var connector = doc.getConnector();

			if (doc.getMeganFile().isMeganSummaryFile())
				throw new UsageException("Input file '" + inputFile + "': must be RMA or meganized DAA file");

			var first2reads = new TreeMap<Integer, ArrayList<String>>();

			var firstClassificationBlock = connector.getClassificationBlock(firstClassificationName);

			progress.setTasks("Processing:","First classification");
			progress.setMaximum(firstClassificationBlock.getKeySet().size());

			if (firstIds == null || firstIds.size() == 0)
				firstIds = firstClassificationBlock.getKeySet();

			for (var classId : firstIds) {
				if (includeFirstUnassigned || classId > 0) {
					var mappedClassId=classId;
					if(firstClassificationMajorRanksOnly) {
						mappedClassId= TaxonomyData.getLowestAncestorWithMajorRank(classId);
					}
					var list = first2reads.computeIfAbsent(mappedClassId,k->new ArrayList<String>());
					var it = connector.getReadsIterator(firstClassificationName, classId, 0, 10, false, false);
					while (it.hasNext()) {
						var readBlock = it.next();
						list.add(readBlock.getReadName());
					}
				}
				progress.incrementProgress();
			}
			progress.reportTaskCompleted();

			var read2second = new HashMap<String, Integer>();

			var secondClassificationBlock = connector.getClassificationBlock(secondClassificationName);

			progress.setTasks("Processing:","Second classification");
			progress.setProgress(0);
			progress.setMaximum(secondClassificationBlock.getKeySet().size());

			if (secondIds == null || secondIds.size() == 0)
				secondIds = secondClassificationBlock.getKeySet();

			for (var classId : secondIds) {
				var mappedClassId=classId;
				if(secondClassificationMajorRanksOnly) {
					mappedClassId = TaxonomyData.getLowestAncestorWithMajorRank(classId);
				}
				if (includeSecondUnassigned || classId > 0) {
					var it = connector.getReadsIterator(secondClassificationName, classId, 0, 10, false, false);
					while (it.hasNext()) {
						var readBlock = it.next();
						read2second.put(readBlock.getReadName(),mappedClassId);
					}
				}
				progress.incrementProgress();
			}
			progress.reportTaskCompleted();

			progress.setSubtask("Merging");
			progress.setProgress(0);
			progress.setMaximum(firstClassificationBlock.getKeySet().size());

			for (var classId : firstClassificationBlock.getKeySet()) {
				if (first2reads.containsKey(classId)) {
					for (var readName : first2reads.get(classId)) {
						var otherId = read2second.get(readName);
						if (otherId != null) {
							if (useReadsTable) {
								var list = readsTable.get(classId, otherId);
								if (list == null) {
									list = new ArrayList<>();
									readsTable.put(classId, otherId, list);
								}
								list.add(readName);
							} else {
								var counts = countsTable.get(classId, otherId);
								if (counts == null) {
									counts = new int[inputFiles.length];
									countsTable.put(classId, otherId, counts);
								}
								counts[f]++;
							}
						}
					}
				}
				progress.incrementProgress();
			}
			progress.reportTaskCompleted();
			doc.closeConnector();
		}

		{
			var firstClassification = (firstFormat.equals("id") ? null : ClassificationManager.get(firstClassificationName, true));
			var secondClassification = (secondFormat.equals("id") ? null : ClassificationManager.get(secondClassificationName, true));

			var progress = new ProgressPercentage("Writing", outputFile);
			progress.setProgress(0);

			var rowSet = (useReadsTable ? readsTable.rowKeySet() : countsTable.rowKeySet());
			var colSet = (useReadsTable ? readsTable.columnKeySet() : countsTable.columnKeySet());
			var numberOfReads = (useReadsTable ? readsTable.getNumberOfRows() : countsTable.getNumberOfRows());

			progress.setMaximum(numberOfReads);

			try (var w = new BufferedWriter(new OutputStreamWriter(FileUtils.getOutputStreamPossiblyZIPorGZIP(outputFile)))) {
				if (showHeaderLine)
					w.write(firstClassificationName + separator + secondClassificationName + separator + StringUtils.toString(inputFiles, separator) + "\n");
				for (var firstId : sorted(firstClassification, firstFormat, rowSet)) {
					var firstName =
							switch (firstFormat) {
								default -> String.valueOf(firstId);
								case "id" -> String.valueOf(firstId);
								case "name" -> firstClassification.getName2IdMap().get(firstId);
								case "path" -> firstClassification.getPath(firstId, pathSeparator);
							};
					for (var secondId : sorted(secondClassification, secondFormat, colSet)) {
						var secondName =
								switch (secondFormat) {
									default -> String.valueOf(secondId);
									case "id" -> String.valueOf(secondId);
									case "name" -> secondClassification.getName2IdMap().get(secondId);
									case "path" -> secondClassification.getPath(secondId, pathSeparator);
								};
						if (useReadsTable) {
							if (readsTable.contains(firstId, secondId)) {
								var values = readsTable.get(firstId, secondId);
								w.write(firstName + separator + secondName + separator + StringUtils.toString(values, ", ") + "\n");
							}
						} else {
							if (countsTable.contains(firstId, secondId)) {
								var values = countsTable.get(firstId, secondId);
								w.write(firstName + separator + secondName + separator + StringUtils.toString(values, separator) + "\n");

							}
						}
					}
					progress.incrementProgress();
				}
			}
			progress.reportTaskCompleted();
		}
	}

	private Collection<Integer> sorted(Classification classification, String format, Collection<Integer> values) {
		if (format.equals("name")) {
			var map = new TreeMap<String, Integer>();
			for (var value : values) {
				map.put(classification.getName2IdMap().get(value), value);
			}
			return map.values();
		} else {
			var list = new ArrayList<>(values);
			list.sort(Integer::compareTo);
			return list;
		}
	}
}
