/*
 * ProjectAssignmentsToRankCommand.java Copyright (C) 2024 Daniel H. Huson
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
package megan.commands.additional;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ProgramProperties;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.FileUtils;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;
import megan.algorithms.NaiveProjectionProfile;
import megan.classification.ClassificationManager;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.core.SampleAttributeTable;
import megan.main.MeganProperties;
import megan.util.CallBack;
import megan.util.PopupChoice;
import megan.viewer.ClassificationViewer;
import megan.viewer.TaxonomicLevels;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProjectAssignmentsToRankCommand extends CommandBase implements ICommand {

    public String getSyntax() {
		return "project rank={" + StringUtils.toString(TaxonomicLevels.getAllMajorRanks(), "|") + "} [minPercent={number}];";
    }

    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("project rank=");
        final String rank = np.getWordMatchesRespectingCase(StringUtils.toString(TaxonomicLevels.getAllNames(), " "));
        final float minPercent;
        if (np.peekMatchIgnoreCase("minPercent")) {
            np.matchIgnoreCase("minPercent=");
            minPercent = (float) np.getDouble(0, 100);
        } else
            minPercent = 0f;
        np.matchIgnoreCase(";");

        final Document doc = ((Director) getDir()).getDocument();
        final int numberOfSamples = doc.getNumberOfSamples();
		final String[] sampleNames = doc.getSampleNamesAsArray();
		final String fileName = FileUtils.replaceFileSuffix(doc.getMeganFile().getFileName(), "-" + rank + "-projection.megan");
		final SampleAttributeTable sampleAttributeTable = doc.getSampleAttributeTable().copy();

        final long numberOfReads = doc.getNumberOfReads();
        final int[] sampleSizes = new int[numberOfSamples];
        for (int s = 0; s < numberOfSamples; s++) {
            sampleSizes[s] = Math.round(doc.getDataTable().getSampleSizes()[s]);
        }

        final Director newDir;
        final Document newDocument;
        if (ProgramProperties.isUseGUI()) {
            newDir = Director.newProject(false);
            newDir.getMainViewer().setDoReInduce(true);
            newDir.getMainViewer().setDoReset(true);
            newDocument = newDir.getDocument();
            newDocument.setReadAssignmentMode(doc.getReadAssignmentMode());
        } else {
            newDir = (Director) getDir();
            newDocument = doc;
            newDocument.clearReads();
            newDocument.getSampleAttributeTable().clear();
        }
        newDocument.getMeganFile().setFile(fileName, MeganFile.Type.MEGAN_SUMMARY_FILE);
        newDocument.setNumberReads(numberOfReads);

        Map<String, Map<Integer, float[]>> classification2class2counts = new HashMap<>();

        {
            var viewers = new ArrayList<ClassificationViewer>();
            viewers.add(((Director) getDir()).getMainViewer());
            if (doc.getClassificationNames().contains("GTDB")) {
                for (var viewer : ((Director) getDir()).getViewers()) {
                    if (viewer instanceof ClassificationViewer && viewer.getClassName().equals("GTDB")) {
                        viewers.add((ClassificationViewer) viewer);
                        break;
                    }
                }
                if (viewers.size() < 2) {
                    System.err.println("Document has GTDB classification, open GTDB viewer to ensure that it gets projected, too");
                }
            }
            for (var viewer : viewers) {
                final Map<Integer, float[]> taxonMap = NaiveProjectionProfile.compute(viewer, rank, minPercent);
                final Map<Integer, float[]>[] sample2taxonMap = sortBySample(numberOfSamples, taxonMap);

                for (int s = 0; s < numberOfSamples; s++) {
                    classification2class2counts.put(viewer.getClassName(), sample2taxonMap[s]);
                    // float sampleSize = computeSize(sample2taxonMap[s]);
                    newDocument.addSample(sampleNames[s], sampleSizes[s], 0, doc.getBlastMode(), classification2class2counts);
                }
            }
        }

        System.err.printf("Number of reads: %,d%n",newDocument.getNumberOfReads());

        newDocument.processReadHits();
        newDocument.setTopPercent(100);
        newDocument.setMinScore(0);
        newDocument.setMaxExpected(10000);
        newDocument.setMinSupportPercent(0);
        newDocument.setMinSupport(1);
        newDocument.setDirty(true);
        newDocument.getDataTable().setParameters(doc.getDataTable().getParameters());
        newDocument.getActiveViewers().addAll(newDocument.getDataTable().getClassification2Class2Counts().keySet());
        newDocument.getSampleAttributeTable().addTable(sampleAttributeTable, true, true);

        NotificationsInSwing.showInformation(String.format("Computed taxonomic profile for %,d reads", newDocument.getNumberOfReads()));

        if (ProgramProperties.isUseGUI() && newDocument.getNumberOfReads() == 0) {
            newDocument.setDirty(false);
            newDir.close();
        } else {
            newDir.getMainViewer().getFrame().setVisible(true);
            if (newDocument.getNumberOfSamples() > 1) {
                newDir.getMainViewer().getNodeDrawer().setStyle(ProgramProperties.get(MeganProperties.COMPARISON_STYLE, ""), NodeDrawer.Style.PieChart);
            }
            newDir.execute("update reprocess=true reInduce=true;collapse rank=" + rank + ";", newDir.getMainViewer().getCommandManager());
        }
    }

    /**
     * compute the size of the classification
     *
     * @return size
     */
    private float computeSize(Map<Integer, float[]> integerMap) {
        float size = 0;
        for (Integer taxonId : integerMap.keySet()) {
            size += integerMap.get(taxonId)[0];
        }
        return size;
    }

    /**
     * split into single sample tables
     *
     * @return single sample tables
     */
    private Map<Integer, float[]>[] sortBySample(int numberOfSamples, Map<Integer, float[]> taxonMap) {
        Map<Integer, float[]>[] sample2TaxonMap = new HashMap[numberOfSamples];
        for (int i = 0; i < numberOfSamples; i++) {
            sample2TaxonMap[i] = new HashMap<>();
        }
        for (Integer taxId : taxonMap.keySet()) {
            float[] counts = taxonMap.get(taxId);
            for (int i = 0; i < numberOfSamples; i++) {
                sample2TaxonMap[i].put(taxId, new float[]{counts[i]});

            }
        }
        return sample2TaxonMap;
    }

    public void actionPerformed(ActionEvent event) {
        final String[] ranks = TaxonomicLevels.getAllMajorRanks().toArray(new String[0]);

        PopupChoice<String> popupChoice = new PopupChoice<>(ranks, null, new CallBack<>() {
            @Override
            public void call(String choice) {
                execute("collapse rank='" + choice + "';project rank='" + choice + "' minPercent=0;");

            }
        });
        popupChoice.showAtCurrentMouseLocation(getViewer().getFrame());

    }

    public boolean isApplicable() {
        return ClassificationManager.isTaxonomy(getViewer().getClassName()) && ((Director) getDir()).getDocument().getNumberOfReads() > 0;
    }

    public String getName() {
        return "Project Assignments To Rank...";
    }

    public String getDescription() {
        return "Projects all taxonomic assignments onto a given rank";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("sun/New16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    @Override
    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}
