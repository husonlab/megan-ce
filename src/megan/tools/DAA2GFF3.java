/*
 * DAA2GFF3.java Copyright (C) 2022 Daniel H. Huson
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
import megan.core.Document;
import megan.daa.io.DAAParser;
import megan.dialogs.export.ExportAlignedReads2GFF3Format;
import megan.main.MeganProperties;

import java.io.File;
import java.io.IOException;

/**
 * computes GFF3 file from DAA file
 */
public class DAA2GFF3 {
	/**
	 * computes GFF3 file from DAA file
	 *
	 */
	public static void main(String[] args) {
		try {
			ResourceManager.insertResourceRoot(megan.resources.Resources.class);
			ProgramProperties.setProgramName("DAA2GFF3");
			ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

			PeakMemoryUsageMonitor.start();
			(new DAA2GFF3()).run(args);
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
	 */
    private void run(String[] args) throws UsageException, IOException, CanceledException {
        final ArgsOptions options = new ArgsOptions(args, this, "Extracts a GFF3 annotation file from a meganized DAA file");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2022 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and Output");
        final String daaFile = options.getOptionMandatory("-i", "in", "Input meganized DAA file", "");
        final String outputFile = options.getOption("-o", "out", "Output file (stdout or .gz ok)", "stdout");

        options.comment("Options");
		final String classificationToReport = options.getOption("-c", "classification", "Name of classification to report, or 'all'", "all");
		final boolean includeIncompatible = options.getOption("-k", "incompatible", "Include incompatible", false);
		final boolean includeDominated = options.getOption("-d", "dominated", "Include dominated", false);

		final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file",megan.main.Megan6.getDefaultPropertiesFile());
		options.done();

		MeganProperties.initializeProperties(propertiesFile);

		if (!DAAParser.isMeganizedDAAFile(daaFile, true))
			throw new IOException("Input file is not meganized DAA file");

		final var doc = new Document();
		doc.getMeganFile().setFileFromExistingFile(daaFile, true);
		doc.loadMeganFile();

		ExportAlignedReads2GFF3Format.apply(doc, new File(outputFile), classificationToReport, !includeIncompatible, !includeDominated, new ProgressPercentage());
	}
}
