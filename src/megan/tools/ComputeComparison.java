/*
 * ComputeComparison.java Copyright (C) 2023 Daniel H. Huson
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
import jloda.util.parse.NexusStreamParser;
import jloda.util.progress.ProgressSilent;
import megan.commands.SaveCommand;
import megan.commands.show.CompareCommand;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.dialogs.compare.Comparer;
import megan.main.MeganProperties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * compares multiple samples
 * Daniel Huson, 8.2018
 * @deprecated use CompareFiles.java instead
 */
@Deprecated
public class ComputeComparison {
    /**
     * ComputeComparison
     *
	 */
    public static void main(String[] args) {
        try {
            ResourceManager.insertResourceRoot(megan.resources.Resources.class);
            ProgramProperties.setProgramName("ComputeComparison");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new ComputeComparison()).run(args);
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
	 */
    private void run(String[] args) throws Exception {
        final ArgsOptions options = new ArgsOptions(args, this, "Computes the comparison of multiple megan, RMA or meganized DAA files");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2023 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and Output:");
        ArrayList<String> inputFiles = new ArrayList<>(Arrays.asList(options.getOptionMandatory("-i", "in", "Input RMA and/or meganized DAA files (single directory ok)", new String[0])));
        final String outputFile = options.getOption("-o", "out", "Output file", "comparison.megan");

        final String metadataFile = options.getOption("-mdf", "metaDataFile", "Metadata file", "");
        options.comment("Options:");

        final boolean normalize = options.getOption("-n", "normalize", "Normalize counts", true);
        final boolean ignoreUnassignedReads = options.getOption("-iu", "ignoreUnassignedReads", "Ignore unassigned, no-hit or contaminant reads", false);

        final Document.ReadAssignmentMode readAssignmentMode = Document.ReadAssignmentMode.valueOfIgnoreCase(options.getOption("-ram", "readAssignmentMode", "Set the desired read-assignment mode", Document.ReadAssignmentMode.readCount.toString()));
        final boolean keepOne = options.getOption("-k1", "keepOne", "In a normalized comparison, minimum non-zero count is set to 1", false);

		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file",megan.main.Megan6.getDefaultPropertiesFile());
		options.done();

		MeganProperties.initializeProperties(propertiesFile);

		if (inputFiles.size() == 1 && FileUtils.isDirectory(inputFiles.get(0))) {
			final String directory = inputFiles.get(0);
			inputFiles.clear();
			inputFiles.addAll(FileUtils.getAllFilesInDirectory(directory, true, ".daa", ".rma", ".rma6"));
		}

        for (String fileName : inputFiles) {
			if (!FileUtils.fileExistsAndIsNonEmpty(fileName))
				throw new IOException("No such file or file empty: " + fileName);
        }

        if (inputFiles.size() == 0)
            throw new UsageException("No input file");

        final Director dir = Director.newProject(false);
        final Document doc = dir.getDocument();
        doc.setProgressListener(new ProgressSilent());

        {
            CompareCommand compareCommand = new CompareCommand();
            compareCommand.setDir(dir);
			final String command = "compare mode=" + (normalize ? Comparer.COMPARISON_MODE.RELATIVE : Comparer.COMPARISON_MODE.ABSOLUTE) +
								   " readAssignmentMode=" + readAssignmentMode + " keep1=" + keepOne + " ignoreUnassigned=" + ignoreUnassignedReads +
								   " meganFile='" + StringUtils.toString(inputFiles, "', '") + "';";
			try {
				compareCommand.apply(new NexusStreamParser(new StringReader(command)));
			} catch (Exception ex) {
				Basic.caught(ex);
			}
		}

		if (StringUtils.notBlank(metadataFile)) {
			try (BufferedReader r = new BufferedReader(new InputStreamReader(FileUtils.getInputStreamPossiblyZIPorGZIP(metadataFile)))) {
				System.err.print("Processing Metadata: " + metadataFile);
				doc.getSampleAttributeTable().read(r, doc.getSampleNames(), true);
				System.err.println(", attributes: " + doc.getSampleAttributeTable().getNumberOfUnhiddenAttributes());
			}
		}

		doc.getMeganFile().setFile(outputFile, MeganFile.Type.MEGAN_SUMMARY_FILE);
		final SaveCommand saveCommand = new SaveCommand();
		saveCommand.setDir(dir);
		System.err.println("Saving to file: " + outputFile);
		saveCommand.apply(new NexusStreamParser(new StringReader("save file='" + outputFile + "';")));

    }
}
