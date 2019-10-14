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
package megan.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.ClassificationManager;
import megan.core.Director;
import megan.core.Document;
import megan.core.SampleAttributeTable;
import megan.daa.Meganize;
import megan.main.MeganProperties;
import megan.util.ReadMagnitudeParser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * meganize DAA files
 * Daniel Huson, 3.2016
 */
public class MeganizeDAACommand extends CommandBase implements ICommand {

    /**
     * get command-line usage description
     *
     * @return usage
     */
    public String getSyntax() {
        return "meganize daaFile=<name> [,<name>...] [minScore=<num>] [maxExpected=<num>] [minPercentIdentity=<num>]\n" +
                "\t[topPercent=<num>] [minSupportPercent=<num>] [minSupport=<num>] [weightedLCA={false|true}] [lcaCoveragePercent=<num>]  [minPercentReadToCover=<num>] [minComplexity=<num>] [useIdentityFilter={false|true}]\n" +
                "\t[fNames={" + Basic.toString(ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy(), "|") + "...} [longReads={false|true}] [paired={false|true} [pairSuffixLength={number}]]\n" +
                "\t[contaminantsFile=<filename>] [description=<text>];";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("meganize daaFile=");

        final ArrayList<String> daaFiles = new ArrayList<>();
        daaFiles.add(np.getWordFileNamePunctuation());

        while (np.peekMatchAnyTokenIgnoreCase(",")) {
            np.matchIgnoreCase(",");
            daaFiles.add(np.getWordFileNamePunctuation());
        }

        float minScore = Document.DEFAULT_MINSCORE;
        if (np.peekMatchIgnoreCase("minScore")) {
            np.matchIgnoreCase("minScore=");
            minScore = (float) np.getDouble(0, Float.MAX_VALUE);
        }
        float maxExpected = Document.DEFAULT_MAXEXPECTED;
        if (np.peekMatchIgnoreCase("maxExpected")) {
            np.matchIgnoreCase("maxExpected=");
            maxExpected = (float) np.getDouble(0, Float.MAX_VALUE);
        }
        float minPercentIdentity = Document.DEFAULT_MIN_PERCENT_IDENTITY;
        if (np.peekMatchIgnoreCase("minPercentIdentity")) {
            np.matchIgnoreCase("minPercentIdentity=");
            minPercentIdentity = (float) np.getDouble(0, Float.MAX_VALUE);
        }
        float topPercent = Document.DEFAULT_TOPPERCENT;
        if (np.peekMatchIgnoreCase("topPercent")) {
            np.matchIgnoreCase("topPercent=");
            topPercent = (float) np.getDouble(0, 100);
        }
        float minSupportPercent = Document.DEFAULT_MINSUPPORT_PERCENT;
        if (np.peekMatchIgnoreCase("minSupportPercent")) {
            np.matchIgnoreCase("minSupportPercent=");
            minSupportPercent = (float) np.getDouble(0, 100);
        }

        int minSupport = Document.DEFAULT_MINSUPPORT;
        if (np.peekMatchIgnoreCase("minSupport")) {
            np.matchIgnoreCase("minSupport=");
            minSupport = np.getInt(0, Integer.MAX_VALUE);
            if (minSupport > 0)
                minSupportPercent = 0;
        }

        Document.LCAAlgorithm lcaAlgorithm = Document.DEFAULT_LCA_ALGORITHM_SHORT_READS;
        if (np.peekMatchIgnoreCase("weightedLCA")) {
            np.matchIgnoreCase("weightedLCA=");
            if (np.getBoolean())
                lcaAlgorithm = Document.LCAAlgorithm.weighted;
            else
                lcaAlgorithm = Document.LCAAlgorithm.naive;
            ProgramProperties.put("lcaAlgorithm", lcaAlgorithm.toString());
        } else if (np.peekMatchIgnoreCase("lcaAlgorithm")) {
            np.matchIgnoreCase("lcaAlgorithm=");
            lcaAlgorithm = Document.LCAAlgorithm.valueOfIgnoreCase(np.getWordRespectCase());
        }
        float lcaCoveragePercent = Document.DEFAULT_LCA_COVERAGE_PERCENT;
        if (np.peekMatchAnyTokenIgnoreCase("lcaCoveragePercent weightedLCAPercent")) {
            np.matchAnyTokenIgnoreCase("lcaCoveragePercent weightedLCAPercent");
            np.matchIgnoreCase("=");
            lcaCoveragePercent = (float) np.getDouble(1, 100);
            ProgramProperties.put("lcaCoveragePercent", lcaCoveragePercent);
        }

        float minPercentReadToCover = Document.DEFAULT_MIN_PERCENT_READ_TO_COVER;
        if (np.peekMatchIgnoreCase("minPercentReadToCover")) {
            np.matchIgnoreCase("minPercentReadToCover=");
            minPercentReadToCover = (float) np.getDouble(0, 100);
            ProgramProperties.put("minPercentReadToCover", minPercentReadToCover);
        }

        float minPercentReferenceToCover = Document.DEFAULT_MIN_PERCENT_REFERENCE_TO_COVER;
        if (np.peekMatchIgnoreCase("minPercentReferenceToCover")) {
            np.matchIgnoreCase("minPercentReferenceToCover=");
            minPercentReferenceToCover = (float) np.getDouble(0, 100);
            ProgramProperties.put("minPercentReferenceToCover", minPercentReferenceToCover);
        }

        float minComplexity = 0;
        if (np.peekMatchIgnoreCase("minComplexity")) {
            np.matchIgnoreCase("minComplexity=");
            minComplexity = (float) np.getDouble(-1.0, 1.0);
            if (minComplexity > 0)
                System.err.println("IGNORED setting: minComplexity=" + minComplexity);
        }

        boolean useIdentityFilter = Document.DEFAULT_USE_IDENTITY;
        if (np.peekMatchIgnoreCase("useIdentityFilter")) {
            np.matchIgnoreCase("useIdentityFilter=");
            useIdentityFilter = np.getBoolean();
            if (useIdentityFilter)
                System.err.println("IGNORED setting: useIdentityFilter=" + true);
        }

        Document.ReadAssignmentMode readAssignmentMode = Document.DEFAULT_READ_ASSIGNMENT_MODE_SHORT_READS;
        if (np.peekMatchIgnoreCase("readAssignmentMode")) {
            np.matchIgnoreCase("readAssignmentMode=");
            readAssignmentMode = Document.ReadAssignmentMode.valueOfIgnoreCase(np.getWordMatchesIgnoringCase(Basic.toString(Document.ReadAssignmentMode.values(), " ")));
        }

        final Collection<String> known = ClassificationManager.getAllSupportedClassificationsExcludingNCBITaxonomy();
        final ArrayList<String> cNames = new ArrayList<>();
        if (np.peekMatchIgnoreCase("fNames=")) {
            np.matchIgnoreCase("fNames=");
            while (!np.peekMatchIgnoreCase(";")) {
                String token = np.getWordRespectCase();
                if (!known.contains(token)) {
                    np.pushBack();
                    break;
                }
                cNames.add(token);
            }
        }

        final boolean longReads;
        if (np.peekMatchIgnoreCase("longReads")) {
            np.matchIgnoreCase("longReads=");
            longReads = np.getBoolean();
        } else
            longReads = false;

        boolean pairedReads = false;
        int pairSuffixLength = 0;
        if (np.peekMatchIgnoreCase("paired")) {
            np.matchIgnoreCase("paired=");
            pairedReads = np.getBoolean();
            if (pairedReads) {
                np.matchIgnoreCase("pairSuffixLength=");
                pairSuffixLength = np.getInt(0, 10);
                System.err.println("Assuming paired-reads distinguishing suffix has length: " + pairSuffixLength);
            }
        }

        final String contaminantsFile;
        if (np.peekMatchIgnoreCase("contaminantsFile")) {
            np.matchIgnoreCase("contaminantsFile=");
            contaminantsFile = np.getWordFileNamePunctuation().trim();
        } else
            contaminantsFile = "";

        String description = null;
        if (np.peekMatchIgnoreCase("description")) {
            np.matchIgnoreCase("description=");
            description = np.getWordFileNamePunctuation().trim();
        }

        np.matchIgnoreCase(";");

        ReadMagnitudeParser.setUnderScoreEnabled(ProgramProperties.get("allow-read-weights-underscore", false));

        final Director dir = (Director) getDir();
        final Document doc = dir.getDocument();

        int countFailed = 0;

        for (String daaFile : daaFiles) {
            try {
                System.err.println("Meganizing file: " + daaFile);

                final Director openDir = findOpenDirector(daaFile);
                if (openDir != null) {
                    throw new IOException("File is currently open, cannot meganize open files");
                }

                Meganize.apply(((Director) getDir()).getDocument().getProgressListener(), daaFile, "", cNames, minScore, maxExpected, minPercentIdentity,
                        topPercent, minSupportPercent, minSupport, pairedReads, pairSuffixLength, lcaAlgorithm, readAssignmentMode, lcaCoveragePercent, longReads,
                        minPercentReadToCover, minPercentReferenceToCover, contaminantsFile);

                // todo: save the description

                {
                    final Document tmpDoc = new Document();
                    tmpDoc.getMeganFile().setFileFromExistingFile(daaFile, false);
                    tmpDoc.loadMeganFile();
                    ProgramProperties.put(MeganProperties.DEFAULT_PROPERTIES, tmpDoc.getParameterString());

                    if (description != null && description.length() > 0) {
                        description = description.replaceAll("^ +| +$|( )+", "$1"); // replace all white spaces by a single space
                        final String sampleName = Basic.replaceFileSuffix(Basic.getFileNameWithoutPath(tmpDoc.getMeganFile().getFileName()), "");
                        tmpDoc.getSampleAttributeTable().put(sampleName, SampleAttributeTable.DescriptionAttribute, description);
                    }
                    if (tmpDoc.getNumberOfReads() == 0)
                        NotificationsInSwing.showWarning(getViewer().getFrame(), "No reads found");
                }

                MeganProperties.addRecentFile(daaFile);

                if (ProgramProperties.isUseGUI() && daaFiles.size() == 1) {
                    int result = JOptionPane.showConfirmDialog(getViewer().getFrame(), "Open meganized file?", "Open?", JOptionPane.YES_NO_OPTION);

                    if (result == JOptionPane.YES_OPTION) {
                        if (daaFiles.size() == 1 && doc.neverOpenedReads) {
                            dir.getDocument().clearReads();
                            dir.getMainViewer().setDoReInduce(true);
                            dir.getMainViewer().setDoReset(true);
                            dir.executeImmediately("open file='" + daaFile + "';", dir.getMainViewer().getCommandManager());
                        } else {
                            final Director newDir = Director.newProject();
                            newDir.getMainViewer().setDoReInduce(true);
                            newDir.getMainViewer().setDoReset(true);
                            newDir.execute("open file='" + daaFile + "';", newDir.getMainViewer().getCommandManager());
                        }
                    }
                }
            } catch (CanceledException ex) {
                throw ex;
            } catch (Exception ex) {
                NotificationsInSwing.showError(ex.getMessage());
                System.err.println(ex.getMessage());
                countFailed++;
            }
        }
        NotificationsInSwing.showInformation("Finished meganizing " + daaFiles.size() + " files" + (countFailed > 0 ? ". ERRORS: " + countFailed : "."));
    }

    /**
     * get directory if this file is currently open
     *
     * @param daaFile
     * @return directory
     */
    private Director findOpenDirector(String daaFile) {
        final File file = new File(daaFile);
        if (file.isFile()) {
            for (IDirector dir : ProjectManager.getProjects()) {
                File aFile = new File(((Director) dir).getDocument().getMeganFile().getFileName());
                if (aFile.isFile() && aFile.equals(file))
                    return (Director) dir;
            }
        }
        return null;
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
    }

    /**
     * get the name to be used as a menu label
     *
     * @return name
     */
    public String getName() {
        return null;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Meganize DAA File";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/Import16.gif");
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        return true;
    }
}
