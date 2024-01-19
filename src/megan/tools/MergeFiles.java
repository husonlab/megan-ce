/*
 * CompareFiles.java Copyright (C) 2024 Daniel H. Huson
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
import megan.core.*;
import megan.data.merge.MergeConnector;
import megan.main.MeganProperties;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * computes a merged view of multiple files
 * Daniel Huson, 5.2022
 */
public class MergeFiles {
	/**
	 * merge files
	 */
	public static void main(String[] args) {
		try {
			ResourceManager.insertResourceRoot(megan.resources.Resources.class);
			ProgramProperties.setProgramName("MergeFiles");
			ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

			PeakMemoryUsageMonitor.start();
			(new MergeFiles()).run(args);
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
	 */
	public void run(String[] args) throws Exception {
		final ArgsOptions options = new ArgsOptions(args, this, "Computes the comparison of multiple megan, RMA or meganized DAA files");
		options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2024. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

		options.comment("Input and Output:");
		final ArrayList<String> inputFiles = new ArrayList<>(Arrays.asList(options.getOptionMandatory("-i", "in", "Input RMA and/or meganized DAA files (single directory ok)", new String[0])));
		final String meganFileName = options.getOption("-o", "out", "Output file", "merged.megan");
		final String metadataFile = options.getOption("-mdf", "metaDataFile", "Metadata file", "");

		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file",megan.main.Megan6.getDefaultPropertiesFile());
		options.done();

		MeganProperties.initializeProperties(propertiesFile);

		if (inputFiles.size() == 1 && FileUtils.isDirectory(inputFiles.get(0))) {
			final String directory = inputFiles.get(0);
			inputFiles.clear();
			inputFiles.addAll(FileUtils.getAllFilesInDirectory(directory, true, ".megan", ".megan.gz", ".daa", ".rma", ".rma6"));
		}

		for (var fileName : inputFiles) {
			if (!FileUtils.fileExistsAndIsNonEmpty(fileName))
				throw new IOException("No such file or file empty: " + fileName);
		}

		if (inputFiles.size() == 0)
			throw new UsageException("No input file");

		String parameters = null;

		for (var fileName : inputFiles) {
			final var file = new MeganFile();
			file.setFileFromExistingFile(fileName, false);
			if (!file.isOkToRead() || file.getConnector() == null)
				throw new IOException("Can't process file (unreadable, or not meganized-DAA or RMA6): " + fileName);
			System.err.println("Input file: " + fileName);
			System.err.printf("\t\t%,d reads%n", file.getConnector().getNumberOfReads());
			if (parameters == null) {
				var table = new DataTable();
				var label2data = file.getConnector().getAuxiliaryData();
				SyncArchiveAndDataTable.syncAux2Summary(fileName, label2data.get(SampleAttributeTable.USER_STATE), table);
				parameters = table.getParameters();
			}
		}

		var doc = new Document();
		if (parameters != null)
			doc.parseParameterString(parameters);
		var meganFile = new MeganFile();
		meganFile.setFile(meganFileName, MeganFile.Type.MEGAN_SUMMARY_FILE);

		var connector = new MergeConnector(meganFileName, inputFiles);
		SyncArchiveAndDataTable.syncArchive2Summary(null, meganFileName, connector, doc.getDataTable(), doc.getSampleAttributeTable());
		doc.getDataTable().setMergedFiles(FileUtils.getFileNameWithoutPathOrSuffix(meganFileName), inputFiles);
		meganFile.setMergedFiles(Arrays.asList(doc.getDataTable().getMergedFiles()));

		doc.getDataTable().clearCollapsed();

		try (var writer = new FileWriter(meganFileName)) {
			doc.getDataTable().write(writer);
			doc.getSampleAttributeTable().write(writer, false, true);
		}

		if (StringUtils.notBlank(metadataFile)) {
			try (var r = new BufferedReader(new InputStreamReader(FileUtils.getInputStreamPossiblyZIPorGZIP(metadataFile)))) {
				System.err.print("Processing Metadata: " + metadataFile);
				doc.getSampleAttributeTable().read(r, doc.getSampleNames(), true);
				System.err.println(", attributes: " + doc.getSampleAttributeTable().getNumberOfUnhiddenAttributes());
			}
		}

		System.err.println("Output file: " + meganFileName);
		System.err.printf("\t\t%,d reads%n", meganFile.getConnector().getNumberOfReads());
	}
}
