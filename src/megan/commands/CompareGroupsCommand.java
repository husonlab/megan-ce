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

import jloda.swing.commands.ICommand;
import jloda.swing.director.IDirector;
import jloda.swing.director.ProjectManager;
import jloda.util.Basic;
import jloda.util.BlastMode;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.algorithms.ComputeCoreBiome;
import megan.core.Director;
import megan.core.Document;
import megan.core.MeganFile;
import megan.dialogs.compare.Comparer;
import megan.main.MeganProperties;
import megan.viewer.MainViewer;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * compute comparison of groups
 * Daniel Huson, 7.2015
 */
public class CompareGroupsCommand extends jloda.swing.commands.CommandBase implements ICommand {

    public void apply(NexusStreamParser np) throws Exception {
        final Document doc = ((Director) getDir()).getDocument();

        final List<Pair<String, List<String>>> groups = new ArrayList<>();
        final Set<String> nameSet = new HashSet<>();
        final Set<String> sampleSet = new HashSet<>();

        // parse command:
        String mode = Comparer.COMPARISON_MODE.ABSOLUTE.toString();
        boolean ignoreUnassigned = false;
        String title = "Untitled";
        {
            np.matchIgnoreCase("compare");
            while (!np.peekMatchIgnoreCase(";")) {
                if (np.peekMatchIgnoreCase("title")) {
                    np.matchIgnoreCase("title=");
                    title = np.getWordRespectCase();
                }
                np.matchIgnoreCase("name=");
                final Pair<String, List<String>> group = new Pair<>();
                final String name = np.getWordRespectCase();
                if (nameSet.contains(name))
                    throw new IOException("Name used multiple times: " + name);
                else
                    nameSet.add(name);
                group.set1(name);

                final ArrayList<String> samples = new ArrayList<>();
                group.set2(samples);
                if (np.peekMatchIgnoreCase("samples")) {
                    np.matchIgnoreCase("samples=");
                    while (!np.peekMatchAnyTokenIgnoreCase("name comparisonMode ignoreUnassigned;")) {
                        final String sample = np.getWordRespectCase();
                        if (sampleSet.contains(sample))
                            throw new IOException("Sample used multiple times: " + sample);
                        else
                            sampleSet.add(sample);
                        if (!doc.getSampleNames().contains(sample))
                            throw new IOException("Unknown sample: " + sample);

                        if (sample.equalsIgnoreCase("samples") || sample.equals("="))
                            throw new IOException("Illegal sample name: '" + sample + "'");
                        samples.add(sample);
                    }
                }
                if (group.get2().size() > 0)
                    groups.add(group);
                else
                    System.err.println("Ignored empty group: " + group.get1());
            }
            if (np.peekMatchIgnoreCase("mode")) {
                np.matchIgnoreCase("mode=");
                mode = np.getWordMatchesRespectingCase(Basic.toString(Comparer.COMPARISON_MODE.values(), " "));
            }
            if (np.peekMatchIgnoreCase("ignoreUnassigned")) {
                np.matchIgnoreCase("ignoreUnassigned=");
                ignoreUnassigned = np.getBoolean();
            }
            np.matchIgnoreCase(";");
        }

        if (groups.size() == 0) {
            System.err.println("No groups to compare");
            return;
        }

        System.err.println("Number of groups to compare:  " + groups.size());
        System.err.println("Number of samples to compare: " + sampleSet.size());

        final Comparer comparer = new Comparer();
        comparer.setMode(mode);
        comparer.setIgnoreUnassigned(ignoreUnassigned);

        // build document for each sample:
        {
            for (Pair<String, List<String>> group : groups) {
                final Map<String, Map<Integer, float[]>> classification2class2counts = new HashMap<>();
                int sampleSize = ComputeCoreBiome.apply(doc, group.get2(), false, 0, 0, classification2class2counts, doc.getProgressListener());

                final Director tmpDir = Director.newProject(false);
                final Document tmpDocument = tmpDir.getDocument();
                final MainViewer tmpViewer = tmpDir.getMainViewer();

                if (classification2class2counts.size() > 0) {
                    tmpDocument.addSample(group.get1(), sampleSize, 0, BlastMode.Unknown, classification2class2counts);

                    tmpDocument.setNumberReads(tmpDocument.getDataTable().getTotalReads());
                    String fileName = group.get1() + ".megan";
                    tmpDocument.getMeganFile().setFile(fileName, MeganFile.Type.MEGAN_SUMMARY_FILE);
                    System.err.println("Number of reads: " + tmpDocument.getNumberOfReads());
                    tmpDocument.processReadHits();
                    tmpDocument.setTopPercent(100);
                    tmpDocument.setMinScore(0);
                    tmpDocument.setMaxExpected(10000);
                    tmpDocument.setMinSupport(1);
                    tmpDocument.setDirty(true);
                    for (String classificationName : tmpDocument.getDataTable().getClassification2Class2Counts().keySet()) {
                        tmpDocument.getActiveViewers().add(classificationName);
                    }

                    tmpDocument.getSampleAttributeTable().addTable(doc.getSampleAttributeTable().mergeSamples(group.get2(), tmpDocument.getSampleNames().get(0)), false, true);

                    tmpDocument.processReadHits();
                    tmpViewer.setDoReset(true);
                    tmpViewer.setDoReInduce(true);
                    tmpDocument.setLastRecomputeTime(System.currentTimeMillis());
                    tmpViewer.updateView(IDirector.ALL);
                }
                comparer.addDirector(tmpDir);
            }
        }

        // make comparer:
        {
            final Director newDir = Director.newProject();
            final MainViewer newViewer = newDir.getMainViewer();
            newViewer.getFrame().setVisible(true);
            newViewer.setDoReInduce(true);
            newViewer.setDoReset(true);
            final Document newDocument = newDir.getDocument();
            final String fileName = Basic.replaceFileSuffix(new File(new File(doc.getMeganFile().getFileName()).getParent(), title).getPath(), ".megan");
            newDocument.getMeganFile().setFile(fileName, MeganFile.Type.MEGAN_SUMMARY_FILE);

            newDocument.setReadAssignmentMode(doc.getReadAssignmentMode());
            comparer.computeComparison(newDocument.getSampleAttributeTable(), newDocument.getDataTable(), doc.getProgressListener());
            newDocument.processReadHits();
            newDocument.setTopPercent(100);
            newDocument.setMinScore(0);
            newDocument.setMinSupportPercent(0);
            newDocument.setMinSupport(1);
            newDocument.setMaxExpected(10000);
            newDocument.getActiveViewers().addAll(newDocument.getDataTable().getClassification2Class2Counts().keySet());
            newDocument.setDirty(true);
            if (newDocument.getNumberOfSamples() > 1) {
                newViewer.getNodeDrawer().setStyle(ProgramProperties.get(MeganProperties.COMPARISON_STYLE, ""), NodeDrawer.Style.BarChart);
                newViewer.setShowLegend("horizontal");
            }
            newDir.execute("update reprocess=true reinduce=true;", newDir.getMainViewer().getCommandManager());
        }

        // remove temporary directors and documents:
        for (Director tmpDir : comparer.getDirs()) {
            ProjectManager.removeProject(tmpDir);
        }

    }


    public boolean isApplicable() {
        return ((Director) getDir()).getDocument().getNumberOfSamples() > 1;
    }

    public boolean isCritical() {
        return true;
    }

    public String getSyntax() {
        return "compare title=<string> name=<string> samples=<string ...> [name=<string> samples=<string ...>] ... [comparisonMode={" + Basic.toString(Comparer.COMPARISON_MODE.values(), "|") + "}] [ignoreUnassigned={false|true}] ;";
    }

    public void actionPerformed(ActionEvent event) {

    }

    public String getName() {
        return "Compare Groups";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public String getDescription() {
        return "Compare some groups of samples";
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    @Override
    public KeyStroke getAcceleratorKey() {
        return null;
    }
}
