/*
 * DAA2Info.java Copyright (C) 2023 Daniel H. Huson
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
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Document;
import megan.daa.connector.DAAConnector;
import megan.daa.io.DAAHeader;
import megan.daa.io.DAAParser;
import megan.dialogs.export.CSVExporter;
import megan.main.MeganProperties;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * import CSV files to MEGAN files
 * Daniel Huson,1.2023
 */
public class CSV2Megan {
    /**
     * import CSV files to MEGAN files
	 */
    public static void main(String[] args) {
        try {
            ResourceManager.insertResourceRoot(megan.resources.Resources.class);
            ProgramProperties.setProgramName("CSV2Megan");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new CSV2Megan()).run(args);
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
        final var options = new ArgsOptions(args, this, "Imports CSV files to Megan summary format");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2023. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and Output");
        final var inputFiles = options.getOptionMandatory("-i", "in", "Input file(s)", new String[0]);
        final var outputFiles = options.getOption("-o", "out", "Output file(s) (directory or .gz ok)", new String[0]);

		options.comment("Import specification");

		final var importType=options.getOption("-t","type","Type of data contained in lines",new String[]{"reads","summary}"},"summary");
    }
}
