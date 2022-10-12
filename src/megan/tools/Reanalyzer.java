/*
 * Reanalyzer.java Copyright (C) 2022 Daniel H. Huson
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
import jloda.swing.util.ProgramProperties;
import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.StringUtils;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Director;
import megan.core.Document;
import megan.main.MeganProperties;

import java.io.IOException;

/**
 * Reanalyze DAA and RMA files
 * Daniel Huson, 12.2019
 */
public class Reanalyzer {
    /**
     * Reanalyze DAA and RMA files
     *
	 */
    public static void main(String[] args) {
        try {
            ResourceManager.insertResourceRoot(megan.resources.Resources.class);
            ProgramProperties.setProgramName("Reanalyzer");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            long start = System.currentTimeMillis();
            (new Reanalyzer()).run(args);
            System.err.println("Time: " + ((System.currentTimeMillis() - start) / 1000) + "s");
            System.exit(0);
        } catch (Exception ex) {
            Basic.caught(ex);
            System.exit(1);
        }
    }

    /**
     * run the program
     *
	 */
    private void run(String[] args) throws Exception {
        final ArgsOptions options = new ArgsOptions(args, this, "Reanalyze DAA and RMA files");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2022 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        final String[] inputFiles = options.getOptionMandatory("-i", "input", "Input  file (stdin ok)", new String[0]);

        options.comment("Parameters");
        final boolean longReads = options.getOption("-lg", "longReads", "Parse and analyse as long reads", Document.DEFAULT_LONG_READS);
        final boolean longReadsSet = options.optionWasExplicitlySet();

        final boolean runClassifications = options.getOption("-class", "classify", "Run classification algorithm", true);
        final float minScore = options.getOption("-ms", "minScore", "Min score (-1: no change)", -1f);
        final float maxExpected = options.getOption("-me", "maxExpected", "Max expected (-1: no change)", -1f);
        final float minPercentIdentity = options.getOption("-mpi", "minPercentIdentity", "Min percent identity (-1: no change)", -1f);
        final float topPercent = options.getOption("-top", "topPercent", "Top percent (-1: no change)", -1f);
        final int minSupport;
        final float minSupportPercent;
        {
            final float minSupportPercent0 = options.getOption("-supp", "minSupportPercent", "Min support as percent of assigned reads (0: off, -1: no change)", -1f);
            final int minSupport0 = options.getOption("-sup", "minSupport", "Min support (0: off, -1; no change)", -1);
            if (minSupportPercent0 != -1 && minSupport0 == -1) {
                minSupportPercent = minSupportPercent0;
                minSupport = 0;
            } else if (minSupportPercent0 == -1 && minSupport0 != -1) {
                minSupportPercent = 0;
                minSupport = minSupport0;
            } else if (minSupportPercent0 != -1) {
                throw new IOException("Please specify a value for either --minSupport or --minSupportPercent, but not for both");
            } else {
                minSupportPercent = minSupportPercent0;
                minSupport = minSupport0;
            }
        }
        final float minPercentReadToCover = options.getOption("-mrc", "minPercentReadCover", "Min percent of read length to be covered by alignments (-1: no change)", -1f);
        final float minPercentReferenceToCover = options.getOption("-mrefc", "minPercentReferenceCover", "Min percent of reference length to be covered by alignments (-1: no change)", -1f);

        final Document.LCAAlgorithm lcaAlgorithm = Document.LCAAlgorithm.valueOfIgnoreCase(options.getOption("-alg", "lcaAlgorithm", "Set the LCA algorithm to use for taxonomic assignment",
                Document.LCAAlgorithm.values(), longReads ? Document.DEFAULT_LCA_ALGORITHM_LONG_READS.toString() : Document.DEFAULT_LCA_ALGORITHM_SHORT_READS.toString()));
        final boolean lcaAlgorithmWasSet = options.optionWasExplicitlySet();

        final float lcaCoveragePercent = options.getOption("-lcp", "lcaCoveragePercent", "Set the percent for the LCA to cover (-1: no change)", -1f);

        final String readAssignmentModeDefaultValue;
        if (options.isDoHelp()) {
            readAssignmentModeDefaultValue = (Document.DEFAULT_READ_ASSIGNMENT_MODE_LONG_READS + " in long read mode, " + Document.DEFAULT_READ_ASSIGNMENT_MODE_SHORT_READS + " else");
        } else if (longReads)
            readAssignmentModeDefaultValue = Document.DEFAULT_READ_ASSIGNMENT_MODE_LONG_READS.toString();
        else
            readAssignmentModeDefaultValue = Document.DEFAULT_READ_ASSIGNMENT_MODE_SHORT_READS.toString();
        final Document.ReadAssignmentMode readAssignmentMode = Document.ReadAssignmentMode.valueOfIgnoreCase(options.getOption("-ram", "readAssignmentMode", "Set the read assignment mode", readAssignmentModeDefaultValue));
        final boolean readAssignmentModeSet = options.optionWasExplicitlySet();

        final String contaminantsFile = options.getOption("-cf", "conFile", "File of contaminant taxa (one Id or name per line)", "");
        final boolean useContaminantFilter = (contaminantsFile.length() > 0);

        final boolean pairedReads = options.getOption("-pr", "paired", "Reads are paired", false);
        final boolean pairedReadsSet = options.optionWasExplicitlySet();

        final var propertiesFile = options.getOption("-P", "propertiesFile", "Properties file",megan.main.Megan6.getDefaultPropertiesFile());
        options.done();

        MeganProperties.initializeProperties(propertiesFile);

        ClassificationManager.ensureTreeIsLoaded(Classification.Taxonomy);

        if (runClassifications) {
            final StringBuilder buf = new StringBuilder();
			buf.append("reanalyzeFiles file='").append(StringUtils.toString(inputFiles, "', '")).append("'");
            if (minSupportPercent != -1f)
                buf.append(" minSupportPercent = ").append(minSupportPercent);
            if (minSupport != -1f)
                buf.append(" minSupport = ").append(minSupport);
            if (minScore != -1f)
                buf.append(" minScore = ").append(minScore);
            if (maxExpected != -1f)
                buf.append(" maxExpected = ").append(maxExpected);
            if (minPercentIdentity != -1f)
                buf.append(" minPercentIdentity = ").append(minPercentIdentity);
            if (topPercent != -1f)
                buf.append(" topPercent = ").append(topPercent);
            if (lcaAlgorithmWasSet)
                buf.append(" lcaAlgorithm = ").append(lcaAlgorithm);
            if (lcaCoveragePercent != -1f)
                buf.append(" lcaCoveragePercent = ").append(lcaCoveragePercent);
            if (minPercentReadToCover != -1f)
                buf.append(" minPercentReadToCover = ").append(minPercentReadToCover);
            if (minPercentReferenceToCover != -1f)
                buf.append(" minPercentReferenceToCover = ").append(minPercentReferenceToCover);
            //" minComplexity = ");minComplexity);
            if (longReadsSet)
                buf.append(" longReads = ").append(longReads);
            if (pairedReadsSet)
                buf.append(" pairedReads = ").append(pairedReads);
            // " useIdentityFilter =");useIdentityFilter);
            if (useContaminantFilter) {
                buf.append(" useContaminantFilter = ").append(useContaminantFilter);
                buf.append(" loadContaminantFile = '").append(contaminantsFile).append("'");
            }
            if (readAssignmentModeSet)
                buf.append(" readAssignmentMode = ").append(readAssignmentMode);
            buf.append(" fNames=*;");

            final Director director = Director.newProject(false, true);

            director.executeImmediately(buf.toString(), director.getMainViewer().getCommandManager());
        }
    }
}
