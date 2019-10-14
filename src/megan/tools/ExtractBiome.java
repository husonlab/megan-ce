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

import jloda.swing.director.ProjectManager;
import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import jloda.util.parse.NexusStreamParser;
import megan.commands.SaveCommand;
import megan.commands.algorithms.ComputeBiomeCommand;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.main.Megan6;
import megan.viewer.TaxonomyData;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * extracts a biome from a comparison file
 * Daniel Huson, 8.2018
 */
public class ExtractBiome {
    public enum Mode {total, core, rare}

    /**
     * extracts a biome from a comparison file
     *
     * @param args
     * @throws UsageException
     * @throws IOException
     */
    public static void main(String[] args) {
        try {
            ResourceManager.addResourceRoot(Megan6.class, "megan.resources");
            ProgramProperties.setProgramName("ExtractBiome");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new ExtractBiome()).run(args);
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
     * @param args
     * @throws UsageException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void run(String[] args) throws Exception {
        final ArgsOptions options = new ArgsOptions(args, this, "Extracts the total, core or rare biome from a MEGAN comparison file");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2019 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        options.comment("Input and Output:");
        final String inputFile = options.getOptionMandatory("-i", "in", "Input MEGAN comparison file (.megan file)", "");
        final String outputFile = options.getOption("-o", "out", "Output file", "biome.megan");

        options.comment("Options:");
        final Mode mode = Basic.valueOfIgnoreCase(Mode.class, options.getOption("-b", "biome", "Biome type to compute", Mode.values(), Mode.total.toString()));

        final String[] samplesToUseOption = options.getOption("-s", "samples", "Samples to use or 'ALL'", new String[]{"ALL"});

        final float sampleThreshold = (float) options.getOption("-stp", "sampleThresholdPercent", "Min or max percent of samples that class must be present in to be included in core or rare biome, resp.", 50.0);
        final float classThreshold = (float) options.getOption("-ctp", "classThresholdPercent", "Min percent of sample that reads assigned to class must achieve for class to be considered present in sample", 0.1);

        options.done();

        if (mode == null)
            throw new UsageException("Unknown compare mode");

        if ((new File(inputFile)).equals(new File(outputFile)))
            throw new UsageException("Input file equals output file");

        TaxonomyData.load();

        final Document doc = new Document();
        final Director dir = new Director(doc);
        ProjectManager.addProject(dir, null);
        doc.setProgressListener(new ProgressSilent());

        doc.getMeganFile().setFile(inputFile, MeganFile.Type.MEGAN_SUMMARY_FILE);
        doc.loadMeganFile();
        {
            if (doc.getDataTable().getTotalReads() > 0) {
                doc.setNumberReads(doc.getDataTable().getTotalReads());
            } else {
                throw new IOException("File is either empty or format is too old: " + inputFile);
            }
        }
        //dir.updateView(Director.ALL);

        final ArrayList<String> selectedSamples = new ArrayList<>();
        if (samplesToUseOption.length == 1 && samplesToUseOption[0].equalsIgnoreCase("ALL"))
            selectedSamples.addAll(doc.getSampleNames());
        else
            selectedSamples.addAll(Arrays.asList(samplesToUseOption));

        if (selectedSamples.size() == 0)
            throw new UsageException("No valid samples-to-use specified");

        final ComputeBiomeCommand computeBiomeCommand = new ComputeBiomeCommand();
        computeBiomeCommand.setDir(dir);

        // compute the biome:
        final String command = "compute biome=" + mode + " classThreshold=" + classThreshold + " sampleThreshold=" + sampleThreshold + " samples='"
                + Basic.toString(selectedSamples, "' '") + "';";
        computeBiomeCommand.apply(new NexusStreamParser(new StringReader(command)));

        // save to new file:
        final Director newDir = computeBiomeCommand.getNewDir();
        final Document newDoc = newDir.getDocument();

        newDoc.getMeganFile().setFile(outputFile, MeganFile.Type.MEGAN_SUMMARY_FILE);
        final SaveCommand saveCommand = new SaveCommand();
        saveCommand.setDir(newDir);
        System.err.println("Saving to file: " + outputFile);
        saveCommand.apply(new NexusStreamParser(new StringReader("save file='" + outputFile + "';")));
    }
}
