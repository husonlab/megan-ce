/*
 * DAA2Info.java Copyright (C) 2021. Daniel H. Huson
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
 *
 */
package megan.tools;

import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import jloda.util.progress.ProgressPercentage;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Document;

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
	 * @param args
	 * @throws UsageException
	 * @throws IOException
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
		final ArgsOptions options = new ArgsOptions(args, this, "Reports taxonomy-by-function classification");
		options.setVersion(ProgramProperties.getProgramVersion());
		options.setLicense("Copyright (C) 2021 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
		options.setAuthors("Daniel H. Huson");

		options.comment("Input and Output");
		final var inputFile = options.getOptionMandatory("-i", "in", "Input file", "");
		final var outputFile = options.getOption("-o", "out", "Output file (stdout or .gz ok)", "stdout");

		options.comment("Options");
		final var firstClassificationName = options.getOption("-a", "firstClassification", "first classification name", ClassificationManager.getAllSupportedClassifications(), "Taxonomy");
		final var firstClasses = options.getOption("-ac", "firstClasses", "Class IDs in first classification?", Arrays.asList("all"));
		final var secondClassificationName = options.getOption("-b", "secondClassification", "Second classification name", ClassificationManager.getAllSupportedClassifications(), "EGGNOG");
		final var secondClasses = options.getOption("-bc", "secondClasses", "Class IDs in second classifications?", Arrays.asList("all"));

		var formats = new String[]{"name", "id"};

		final var firstFormat = options.getOption("-af", "firstFormat", "Format to report first classification class", formats, "name");
		final var secondFormat = options.getOption("-bf", "secondFormat", "Format to report second classification class", formats, firstFormat);

		final var listOption = options.getOption("-l", "list", "List counts or read names?", new String[]{"counts", "reads"}, "counts");

		var separator = options.getOption("-s", "separator", "Separator", new String[]{"tab", "comma", "semi-colon"}, "tab");

		final var includeFirstUnassigned = options.getOption("-au", "includeFirstUnassigned", "include reads unassigned in first classification", true);
		final var includeSecondUnassigned = options.getOption("-bu", "includeSecondUnassigned", "include reads unassigned second classification", true);
		options.done();

		var debuggingRun = false;

		if (firstClassificationName.equals(secondClassificationName))
			throw new UsageException("First second classifications must be different");

		switch (separator) {
			case "command":
				separator = "'";
				break;
			case "semi-colon":
				separator = ";";
				break;
			case "tab":
				separator = "\t";
				break;
		}

		Collection<Integer> firstIds = null;
		if (!(firstClasses.size() == 1 && firstClasses.get(0).equals("all"))) {
			firstIds = new HashSet<>();
			for (var token : firstClasses) {
				if (!Basic.isInteger(token))
					throw new UsageException("--firstClasses: integer expected, got: " + token);
				else
					firstIds.add(Basic.parseInt(token));
			}
		}
		Collection<Integer> secondIds = null;
		if (!(secondClasses.size() == 1 && secondClasses.get(0).equals("all"))) {
			secondIds = new HashSet<>();
			for (var token : secondClasses) {
				if (!Basic.isInteger(token))
					throw new UsageException("--secondClasses: integer expected, got: " + token);
				else
					secondIds.add(Basic.parseInt(token));
			}
		}

		final var doc = new Document();
		doc.getMeganFile().setFileFromExistingFile(inputFile, true);
		doc.loadMeganFile();

		var connector = doc.getConnector();

		if (doc.getMeganFile().isMeganSummaryFile())
			throw new UsageException("Input file must be RMA or meganized DAA file");

		var first2reads = new TreeMap<Integer, ArrayList<String>>();

		var progress = new ProgressPercentage("Processing");

		var firstClassification = (firstFormat.equals("id") ? null : ClassificationManager.get(firstClassificationName, true));
		var firstClassificationBlock = connector.getClassificationBlock(firstClassificationName);

		progress.setSubtask("First classification");
		progress.setMaximum(firstClassificationBlock.getKeySet().size());

		if (firstIds == null || firstIds.size() == 0)
			firstIds = firstClassificationBlock.getKeySet();

		for (var classId : firstIds) {
			if (includeFirstUnassigned || classId > 0) {
				var list = new ArrayList<String>();
				first2reads.put(classId, list);
				var it = connector.getReadsIterator(firstClassificationName, classId, 0, 10, false, false);
				while (it.hasNext()) {
					var readBlock = it.next();
					if (debuggingRun) {
						var ok = true;
						var which = 0;
						for (int m = 0; ok && m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
							var matchBlock = readBlock.getMatchBlock(m);
							var id = matchBlock.getId(firstClassificationName);
							if (id > 0) {
								if (which == 0)
									which = id;
								else if (id != which)
									ok = false;

							}
						}
						if (!ok)
							continue; // keep the good ones
					}
					list.add(readBlock.getReadName());
				}
			}
			progress.incrementProgress();
		}
		progress.reportTaskCompleted();

		var read2second = new HashMap<String, Integer>();

		var secondClassification = (secondFormat.equals("id") ? null : ClassificationManager.get(secondClassificationName, true));
		var secondClassificationBlock = connector.getClassificationBlock(secondClassificationName);

		progress.setSubtask("Second classification");
		progress.setProgress(0);
		progress.setMaximum(secondClassificationBlock.getKeySet().size());

		if (secondIds == null || secondIds.size() == 0)
			secondIds = secondClassificationBlock.getKeySet();

		for (var classId : secondIds) {
			if (includeSecondUnassigned || classId > 0) {
				var it = connector.getReadsIterator(secondClassificationName, classId, 0, 10, false, false);
				while (it.hasNext()) {
					var readBlock = it.next();
					if (debuggingRun) {
						var ok = true;
						var which = 0;
						for (int m = 0; ok && m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
							var matchBlock = readBlock.getMatchBlock(m);
							var id = matchBlock.getId(secondClassificationName);
							if (id > 0) {
								if (which == 0)
									which = id;
								else if (id != which)
									ok = false;

							}
						}
						if (ok)
							continue; // keep the bad ones!
					}
					read2second.put(readBlock.getReadName(), classId);
				}
			}
			progress.incrementProgress();
		}
		progress.reportTaskCompleted();

		var table = new Table<Integer, Integer, ArrayList<String>>();

		progress.setSubtask("Merging");
		progress.setProgress(0);
		progress.setMaximum(firstClassificationBlock.getKeySet().size());

		for (var classId : firstClassificationBlock.getKeySet()) {
			if (first2reads.containsKey(classId)) {
				for (var readName : first2reads.get(classId)) {
					var otherId = read2second.get(readName);
					if (otherId != null) {
						var list = table.get(classId, otherId);
						if (list == null) {
							list = new ArrayList<>();
							table.put(classId, otherId, list);
						}
						list.add(readName);
					}
				}
			}
			progress.incrementProgress();
		}
		progress.reportTaskCompleted();

		progress.setSubtask("Writing to: " + outputFile);
		progress.setProgress(0);
		progress.setMaximum(table.getNumberOfRows());

		try (Writer w = new BufferedWriter(new OutputStreamWriter(FileUtils.getOutputStreamPossiblyZIPorGZIP(outputFile)))) {
			for (var firstId : sorted(firstClassification, firstFormat, table.rowKeySet())) {
				var firstName = (firstFormat.equals("id") ? String.valueOf(firstId) : firstClassification.getName2IdMap().get(firstId));
				for (var secondId : sorted(secondClassification, secondFormat, table.columnKeySet())) {
					var secondName = (secondFormat.equals("id") ? String.valueOf(secondId) : secondClassification.getName2IdMap().get(secondId));
					if (table.contains(firstId, secondId)) {
						var values = table.get(firstId, secondId);
						if (listOption.equals("counts"))
							w.write(firstName + separator + secondName + separator + values.size() + "\n");
						else
							w.write(firstName + separator + secondName + separator + StringUtils.toString(values, ", ") + "\n");
					}
				}
				progress.incrementProgress();
			}
		}
		progress.reportTaskCompleted();
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
