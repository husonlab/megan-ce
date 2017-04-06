/*
 *  Copyright (C) 2017 Daniel H. Huson
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

import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.ResourceManager;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.core.SampleAttributeTable;
import megan.fx.NotificationsInSwing;
import megan.inspector.InspectorWindow;
import megan.main.MeganProperties;
import megan.parsers.blast.BlastFileFormat;
import megan.parsers.blast.BlastMode;
import megan.rma6.RMA6FromBlastCreator;
import megan.util.ReadMagnitudeParser;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * import blast command
 * Daniel Huson, 10.2010
 */
public class ImportBlastCommand extends CommandBase implements ICommand {
    /**
     * get command-line usage description
     *
     * @return usage
     */
    public String getSyntax() {
        return "import blastFile=<name> [,<name>...] [fastaFile=<name> [,<name>...]] meganFile=<name> [useCompression={true|false}]\n" +
                "\tformat={" + Basic.toString(BlastFileFormat.valuesExceptUnknown(), "|") + "}\n" +
                "\tmode={" + Basic.toString(BlastMode.valuesExceptUnknown(), "|") + "} [maxMatches=<num>] [minScore=<num>] [maxExpected=<num>] [minPercentIdentity=<num>]\n" +
                "\t[topPercent=<num>] [minSupportPercent=<num>] [minSupport=<num>] [weightedLCA={false|true}] [weightedLCAPercent=<num>] [minComplexity=<num>] [useIdentityFilter={false|true}]\n" +
                "\t[fNames={" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), "|") + "...} [longReads={false|true}] [paired={false|true} [pairSuffixLength={number}]]\n" +
                "\t[hasMagnitudes={false|true}] [description=<text>];";
    }


    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        final Director dir = getDir();
        final MainViewer viewer = dir.getMainViewer();
        final Document doc = dir.getDocument();

        if (!ProgramProperties.isUseGUI() || doc.neverOpenedReads) {
            doc.neverOpenedReads = false;

            np.matchIgnoreCase("import blastFile=");
            LinkedList<String> blastFiles = new LinkedList<>();
            while (true) {
                boolean comma = false;
                String name = np.getAbsoluteFileName();
                if (name.endsWith(",")) {
                    name = name.substring(0, name.length() - 1);
                    comma = true;
                }
                blastFiles.add(name);
                if (!comma && np.peekMatchIgnoreCase(",")) {
                    np.matchIgnoreCase(",");
                    comma = true;
                }
                if (!comma && np.peekMatchAnyTokenIgnoreCase("fastaFile readFile meganFile ;"))
                    break;
            }
            LinkedList<String> readsFiles = new LinkedList<>();
            if (np.peekMatchAnyTokenIgnoreCase("fastaFile readFile")) {
                np.matchAnyTokenIgnoreCase("fastaFile readFile");
                np.matchIgnoreCase("=");
                while (true) {
                    boolean comma = false;
                    String name = np.getAbsoluteFileName();
                    if (name.endsWith(",")) {
                        name = name.substring(0, name.length() - 1);
                        comma = true;
                    }
                    readsFiles.add(name);
                    if (!comma && np.peekMatchIgnoreCase(",")) {
                        np.matchIgnoreCase(",");
                        comma = true;
                    }
                    if (!comma && np.peekMatchAnyTokenIgnoreCase("meganFile ;"))
                        break;
                }
            }
            np.matchIgnoreCase("meganFile=");
            final String meganFileName = np.getAbsoluteFileName();

            boolean useCompression = true;
            if (np.peekMatchIgnoreCase("useCompression")) {
                np.matchIgnoreCase("useCompression=");
                useCompression = np.getBoolean();
            }

            np.matchIgnoreCase("format=");
            final BlastFileFormat format = BlastFileFormat.valueOfIgnoreCase(np.getWordMatchesIgnoringCase(Basic.toString(BlastFileFormat.valuesExceptUnknown(), " ")));

            np.matchIgnoreCase("mode=");
            doc.setBlastMode(BlastMode.valueOfIgnoringCase(np.getWordMatchesIgnoringCase(Basic.toString(BlastMode.valuesExceptUnknown(), " "))));

            int maxMatchesPerRead = 25;
            if (np.peekMatchIgnoreCase("maxMatches")) {
                np.matchIgnoreCase("maxMatches=");
                maxMatchesPerRead = np.getInt(0, Integer.MAX_VALUE);
            }
            if (np.peekMatchIgnoreCase("minScore")) {
                np.matchIgnoreCase("minScore=");
                doc.setMinScore((float) np.getDouble(0, Float.MAX_VALUE));
            }
            if (np.peekMatchIgnoreCase("maxExpected")) {
                np.matchIgnoreCase("maxExpected=");
                doc.setMaxExpected((float) np.getDouble(0, Float.MAX_VALUE));
            }
            if (np.peekMatchIgnoreCase("minPercentIdentity")) {
                np.matchIgnoreCase("minPercentIdentity=");
                doc.setMinPercentIdentity((float) np.getDouble(0, Float.MAX_VALUE));
            }
            if (np.peekMatchIgnoreCase("topPercent")) {
                np.matchIgnoreCase("topPercent=");
                doc.setTopPercent((float) np.getDouble(0, 100));
            }
            if (np.peekMatchIgnoreCase("minSupportPercent")) {
                np.matchIgnoreCase("minSupportPercent=");
                doc.setMinSupportPercent((float) np.getDouble(0, 100));
            } else
                doc.setMinSupportPercent(0);

            if (np.peekMatchIgnoreCase("minSupport")) {
                np.matchIgnoreCase("minSupport=");
                doc.setMinSupport(np.getInt(1, Integer.MAX_VALUE));
            } else
                doc.setMinScore(1);
            if (np.peekMatchIgnoreCase("weightedLCA")) {
                np.matchIgnoreCase("weightedLCA=");
                getDoc().setLcaAlgorithm(Document.LCAAlgorithm.Weighted);
            } else if (np.peekMatchIgnoreCase("lcaAlgorithm")) {
                np.matchIgnoreCase("lcaAlgorithm=");
                getDoc().setLcaAlgorithm(Document.LCAAlgorithm.valueOfIgnoreCase(np.getWordRespectCase()));
            }
            if (np.peekMatchIgnoreCase("weightedLCAPercent")) {
                np.matchIgnoreCase("weightedLCAPercent=");
                getDoc().setWeightedLCAPercent((float) np.getDouble(1, 100));
                ProgramProperties.put("weightedLCAPercent", doc.getWeightedLCAPercent());
            }

            if (np.peekMatchIgnoreCase("minComplexity")) {
                np.matchIgnoreCase("minComplexity=");
                doc.setMinComplexity((float) np.getDouble(-1.0, 1.0));
            }

            if (np.peekMatchIgnoreCase("useIdentityFilter")) {
                np.matchIgnoreCase("useIdentityFilter=");
                getDoc().setUseIdentityFilter(np.getBoolean());
            }
            Collection<String> known = ClassificationManager.getAllSupportedClassifications();
            if (np.peekMatchIgnoreCase("fNames=")) {
                doc.getActiveViewers().clear();
                np.matchIgnoreCase("fNames=");
                while (!np.peekMatchIgnoreCase(";")) {
                    String token = np.getWordRespectCase();
                    if (!known.contains(token)) {
                        np.pushBack();
                        break;
                    }
                    doc.getActiveViewers().add(token);
                }
                doc.getActiveViewers().add(Classification.Taxonomy);
            }

            if (np.peekMatchIgnoreCase("longReads")) {
                np.matchIgnoreCase("longReads=");
                doc.setLongReads(np.getBoolean());
                doc.setUseWeightedReadCounts(doc.isLongReads());
            }

            if (np.peekMatchIgnoreCase("paired")) {
                np.matchIgnoreCase("paired=");
                boolean paired = np.getBoolean();
                doc.setPairedReads(paired);
                if (paired) {
                    np.matchIgnoreCase("pairSuffixLength=");
                    doc.setPairedReadSuffixLength(np.getInt(0, 10));
                    System.err.println("Assuming paired-reads distinguishing suffix has length: " + doc.getPairedReadSuffixLength());
                }
            }

            boolean hasMagnitudes = false;
            if (np.peekMatchIgnoreCase("hasMagnitudes")) {
                np.matchIgnoreCase("hasMagnitudes=");
                hasMagnitudes = np.getBoolean();
                ReadMagnitudeParser.setEnabled(hasMagnitudes);
                ProgramProperties.put("allow-read-weights", hasMagnitudes);
            }

            String description = null;
            if (np.peekMatchIgnoreCase("description")) {
                np.matchIgnoreCase("description=");
                description = np.getWordFileNamePunctuation().trim();
            }

            np.matchIgnoreCase(";");

            ReadMagnitudeParser.setUnderScoreEnabled(ProgramProperties.get("allow-read-weights-underscore", false));

            if (meganFileName == null)
                throw new IOException("Must specify MEGAN file");

            if (format == null)
                throw new IOException("Failed to determine file format");

            File meganFile = new File(meganFileName);
            if (meganFile.exists()) {
                if (meganFile.delete())
                    System.err.println("Deleting existing file: " + meganFile.getPath());
            }

            final String[] blastFileNames = blastFiles.toArray(new String[blastFiles.size()]);
            final String[] readFileNames = readsFiles.toArray(new String[readsFiles.size()]);

            doc.getMeganFile().setFile(meganFileName, MeganFile.Type.RMA6_FILE);
            RMA6FromBlastCreator rma6Creator = new RMA6FromBlastCreator(ProgramProperties.getProgramName(), format, doc.getBlastMode(),
                    blastFileNames, readFileNames, doc.getMeganFile().getFileName(), useCompression, doc, maxMatchesPerRead, hasMagnitudes);

            rma6Creator.parseFiles(doc.getProgressListener());

            doc.loadMeganFile();
            if (description != null && description.length() > 0) {
                description = description.replaceAll("^ +| +$|( )+", "$1"); // replace all white spaces by a single space
                final String sampleName = Basic.replaceFileSuffix(Basic.getFileNameWithoutPath(doc.getMeganFile().getFileName()), "");
                doc.getSampleAttributeTable().put(sampleName, SampleAttributeTable.DescriptionAttribute, description);
            }

            MeganProperties.addRecentFile(meganFileName);

            if (doc.getNumberOfReads() == 0)
                NotificationsInSwing.showWarning(getViewer().getFrame(), "No reads found");

            if (dir.getViewerByClass(InspectorWindow.class) != null)
                ((InspectorWindow) dir.getViewerByClass(InspectorWindow.class)).clear();
            viewer.setCollapsedIds(TaxonomyData.getTree().getAllAtLevel(3));
            viewer.setDoReset(true);
            viewer.setDoReInduce(true);

            ProgramProperties.put(MeganProperties.DEFAULT_PROPERTIES, doc.getParameterString());
        } else                   // launch new window
        {
            final Director newDir = Director.newProject();
            newDir.getMainViewer().getFrame().setVisible(true);
            newDir.getMainViewer().setDoReInduce(true);
            newDir.getMainViewer().setDoReset(true);
            newDir.execute(np.getQuotedTokensRespectCase(null, ";") + ";", newDir.getMainViewer().getCommandManager());
        }
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
        return "Import BLAST (or RDP or Silva or SAM) and reads files to create a new MEGAN file";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/toolbarButtonGraphics/general/Import16.gif");
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
