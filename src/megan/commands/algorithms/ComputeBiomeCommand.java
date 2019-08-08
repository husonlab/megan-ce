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
package megan.commands.algorithms;

import jloda.swing.commands.ICommand;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.BlastMode;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.main.MeganProperties;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * compute biome
 * Daniel Huson, 2.2013
 */
public class ComputeBiomeCommand extends CommandBase implements ICommand {
    private Director newDir;

    public String getSyntax() {
        return "compute biome={core|total|rare} [classThreshold=<percentage>] [sampleThreshold=<percentage>] samples=<name name...>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        final Director dir = getDir();
        final Document doc = dir.getDocument();

        np.matchIgnoreCase("compute biome=");
        final String what = np.getWordMatchesIgnoringCase("core total rare");

        final float classThreshold;
        if (np.peekMatchIgnoreCase("classThreshold")) {
            np.matchIgnoreCase("classThreshold=");
            classThreshold = (float) np.getDouble(0, 100);
        } else
            classThreshold = 1;

        final float samplesThreshold;
        if (np.peekMatchIgnoreCase("sampleThreshold")) {
            np.matchIgnoreCase("sampleThreshold=");
            samplesThreshold = (float) np.getDouble(0, 100);
        } else
            samplesThreshold = 50;

        final String legalSampleNames = "'" + Basic.toString(doc.getSampleNames(), "' '") + "' ALL";
        final Set<String> selectedSamples = new HashSet<>();
        np.matchIgnoreCase("samples=");
        while (!np.peekMatchIgnoreCase(";")) {
            selectedSamples.add(np.getWordMatchesRespectingCase(legalSampleNames));
        }
        np.matchIgnoreCase(";");

        if (selectedSamples.contains("ALL")) {
            selectedSamples.addAll(doc.getSampleNames());
        }

        System.err.println("Number of samples: " + selectedSamples.size());

        newDir = Director.newProject(false);
        if (dir.getMainViewer() != null) {
            newDir.getMainViewer().getFrame().setVisible(true);
            newDir.getMainViewer().setDoReInduce(true);
            newDir.getMainViewer().setDoReset(true);
        }
        final Document newDocument = newDir.getDocument();

        final Map<String, Map<Integer, float[]>> classification2class2counts = new HashMap<>();
        int sampleSize = 0;
        String title = null;

        if (what.equalsIgnoreCase("core")) {
            int minSamplesToHave = (int) Math.ceil((samplesThreshold / 100.0) * selectedSamples.size());
            sampleSize = ComputeCoreBiome.apply(doc, selectedSamples, false, minSamplesToHave, classThreshold, classification2class2counts, doc.getProgressListener());
            title = String.format("CoreBiome-%2.1f-%2.1f", samplesThreshold, classThreshold);
        } else if (what.equalsIgnoreCase("rare")) {
            int minSamplesNotToHave = (int) Math.ceil((samplesThreshold / 100.0) * selectedSamples.size());
            sampleSize = ComputeCoreBiome.apply(doc, selectedSamples, true, minSamplesNotToHave, classThreshold, classification2class2counts, doc.getProgressListener());
            title = String.format("RareBiome-%2.1f-%2.1f", samplesThreshold, classThreshold);
        } else if (what.equalsIgnoreCase("total")) {
            sampleSize = ComputeCoreBiome.apply(doc, selectedSamples, false, 0, classThreshold, classification2class2counts, doc.getProgressListener());
            title = "TotalBiome";
        }

        if (classification2class2counts.size() > 0) {
            newDocument.addSample(title, sampleSize, 0, BlastMode.Unknown, classification2class2counts);

            newDocument.setNumberReads(newDocument.getDataTable().getTotalReads());
            String fileName = Basic.replaceFileSuffix(doc.getMeganFile().getFileName(), "-" + title + ".megan");
            newDocument.getMeganFile().setFile(fileName, MeganFile.Type.MEGAN_SUMMARY_FILE);
            System.err.println("Number of reads: " + newDocument.getNumberOfReads());
            newDocument.processReadHits();
            newDocument.setTopPercent(100);
            newDocument.setMinScore(0);
            newDocument.setMaxExpected(10000);
            newDocument.setMinSupport(1);
            newDocument.setMinSupportPercent(0);
            newDocument.setDirty(true);
            for (String classificationName : newDocument.getDataTable().getClassification2Class2Counts().keySet()) {
                newDocument.getActiveViewers().add(classificationName);
            }

            newDocument.getSampleAttributeTable().addTable(doc.getSampleAttributeTable().mergeSamples(selectedSamples, newDocument.getSampleNames().get(0)), false, true);

            if (newDocument.getNumberOfSamples() > 1) {
                newDir.getMainViewer().getNodeDrawer().setStyle(ProgramProperties.get(MeganProperties.COMPARISON_STYLE, ""), NodeDrawer.Style.PieChart);
            }
            NotificationsInSwing.showInformation(String.format(Basic.capitalizeFirstLetter(what) + " biome has %,d reads", newDocument.getNumberOfReads()));

            newDir.execute("update reprocess=true reinduce=true;", newDir.getMainViewer().getCommandManager());
        }
    }

    public boolean isApplicable() {
        return false;
    }

    public boolean isCritical() {
        return true;
    }

    public void actionPerformed(ActionEvent event) {
    }

    public String getName() {
        return "Compute Core Biome...";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getDescription() {
        return "Compute the core biome for a set of samples";
    }

    /**
     * gets the new director created by running this command
     *
     * @return new dir
     */
    public Director getNewDir() {
        return newDir;
    }
}

