/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.samplesviewer.commands.algorithms;

import jloda.gui.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.fx.NotificationsInSwing;
import megan.main.MeganProperties;
import megan.parsers.blast.BlastMode;
import megan.samplesviewer.ComputeCoreBiome;
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
    public String getSyntax() {
        return "compute biome={total|core|shared|rare} [threshold=<percentage>] samples=<name name...>;";
    }

    public void apply(NexusStreamParser np) throws Exception {
        final Director dir = getDir();
        final Document doc = dir.getDocument();

        np.matchIgnoreCase("compute biome=");

        String what = np.getWordMatchesIgnoringCase("total core shared rare");
        float threshold = 50.0f;
        if (np.peekMatchIgnoreCase("threshold")) {
            np.matchIgnoreCase("threshold=");
            threshold = (float) np.getDouble(0, 99.99);
        }

        String legalSampleNames = "'" + Basic.toString(doc.getSampleNames(), "' '") + "'";
        Set<String> selectedSamples = new HashSet<>();
        np.matchIgnoreCase("samples=");
        while (!np.peekMatchIgnoreCase(";")) {
            selectedSamples.add(np.getWordMatchesRespectingCase(legalSampleNames));
        }
        np.matchIgnoreCase(";");

        System.err.println("Number of samples: " + selectedSamples.size());

        Director newDir = Director.newProject();
        newDir.getMainViewer().getFrame().setVisible(true);
        newDir.getMainViewer().setDoReInduce(true);
        newDir.getMainViewer().setDoReset(true);
        Document newDocument = newDir.getDocument();

        Map<String, Map<Integer, Integer[]>> classification2class2counts = new HashMap<>();
        int sampleSize = 0;
        String title = null;

        if (what.equalsIgnoreCase("core")) {
            int minSamplesToHave = (int) Math.ceil((threshold / 100.0) * selectedSamples.size());
            sampleSize = ComputeCoreBiome.apply(selectedSamples, false, minSamplesToHave, doc, classification2class2counts, doc.getProgressListener());
            title = String.format("CoreBiome-%2.1f", threshold);
        } else if (what.equalsIgnoreCase("rare")) {
            int minSamplesNotToHave = (int) Math.ceil((threshold / 100.0) * selectedSamples.size());
            sampleSize = ComputeCoreBiome.apply(selectedSamples, true, minSamplesNotToHave, doc, classification2class2counts, doc.getProgressListener());
            title = String.format("RareBiome-%2.1f", threshold);
        } else if (what.equalsIgnoreCase("total")) {
            sampleSize = ComputeCoreBiome.apply(selectedSamples, false, 0, doc, classification2class2counts, doc.getProgressListener());
            title = "TotalBiome";
        } else if (what.equalsIgnoreCase("shared")) {
            sampleSize = ComputeCoreBiome.apply(selectedSamples, false, selectedSamples.size() - 1, doc, classification2class2counts, doc.getProgressListener());
            title = "SharedBiome";
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
            newDocument.setDirty(true);
            for (String classificationName : newDocument.getDataTable().getClassification2Class2Counts().keySet()) {
                newDocument.getActiveViewers().add(classificationName);
            }

            newDocument.getSampleAttributeTable().addTable(doc.getSampleAttributeTable().mergeSamples(selectedSamples, newDocument.getSampleNames().get(0)), false, true);

            if (newDocument.getNumberOfSamples() > 1) {
                newDir.getMainViewer().getNodeDrawer().setStyle(ProgramProperties.get(MeganProperties.COMPARISON_STYLE, ""), NodeDrawer.Style.PieChart);
            }
            NotificationsInSwing.showInformation(String.format("Wrote %,d reads to file '%s'", newDocument.getNumberOfReads(), fileName));

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
        return "Compute the core or rare biome for a set of samples";
    }
}

