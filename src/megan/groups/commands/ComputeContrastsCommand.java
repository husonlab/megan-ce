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
package megan.groups.commands;

import contrasts.Contrasts;
import jloda.graph.Graph;
import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Director;
import megan.core.Document;
import megan.data.IName2IdMap;
import megan.groups.GroupsViewer;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

/**
 * * compute contrasts command
 * * Daniel Huson, 8.2014
 */
public class ComputeContrastsCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "compute contrasts data={" + Basic.toString(ClassificationManager.getAllSupportedClassifications(), ",") + "}";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("compute contrasts data=");
        final String viewerName = np.getWordMatchesIgnoringCase(Basic.toString(ClassificationManager.getAllSupportedClassifications(), " "));
        np.matchIgnoreCase(";");

        if (getViewer() instanceof GroupsViewer) {
            final GroupsViewer viewer = (GroupsViewer) getViewer();
            final Document doc = viewer.getDir().getDocument();
            final Pair<String, String> twoGroupIds = viewer.getGroupsPanel().getTwoSelectedGroups();
            if (twoGroupIds != null) {
                ArrayList<String> first = new ArrayList<>();
                for (String sample : doc.getSampleNames()) {
                    if (doc.getSampleAttributeTable().getGroupId(sample).equals(twoGroupIds.getFirst()))
                        first.add(sample);
                }
                ArrayList<String> second = new ArrayList<>();
                for (String sample : doc.getSampleNames()) {
                    if (doc.getSampleAttributeTable().getGroupId(sample).equals(twoGroupIds.getSecond()))
                        second.add(sample);
                }

                final String[] samples = doc.getSampleNamesAsArray();

                if (first.size() > 0 && second.size() > 0) {
                    // compute data map for contrasts:
                    Map<String, Map<String, Double>> data = new HashMap<>();
                    int numberOfNodes = 0;


                    final IName2IdMap name2IdMap;

                    final ClassificationViewer classificationViewer = (ClassificationViewer) ((Director) getDir()).getViewerByClassName(viewerName);
                    final Graph graph = classificationViewer.getGraph();
                    name2IdMap = classificationViewer.getClassification().getName2IdMap();

                    for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
                        int classId = (Integer) v.getInfo();
                        if (classId > 0) {
                            NodeData nd = (NodeData) v.getData();
                            for (int t = 0; t < samples.length; t++) {
                                String sample = samples[t];
                                Map<String, Double> classId2Counts = data.computeIfAbsent(sample, k -> new HashMap<>());
                                classId2Counts.put(name2IdMap.get(classId), (double) (v.getOutDegree() > 0 ? nd.getAssigned()[t] : nd.getSummarized()[t]));
                            }
                            numberOfNodes++;
                        }
                    }

                    final Contrasts contrasts = new Contrasts();

                    try {
                        Basic.hideSystemErr();
                        contrasts.apply(data);
                    } finally {
                        Basic.restoreSystemErr();
                    }
                    executeImmediately("show window=message;");
                    System.out.println("\nContrasts for " + viewerName + " assignments on " + numberOfNodes + " nodes:");

                    System.out.println("Group " + twoGroupIds.getFirst() + ": " + Basic.toString(first, ","));
                    System.out.println("Group " + twoGroupIds.getSecond() + ": " + Basic.toString(second, ","));

                    System.out.println("Results for group " + twoGroupIds.getFirst() + " vs group " + twoGroupIds.getSecond() + ":");
                    Map<String, Double> results = contrasts.getSplitScores(first.toArray(new String[0]), second.toArray(new String[0]));

                    SortedSet<Pair<Double, String>> sorted = new TreeSet<>();
                    System.out.println(String.format("%-20s\tScore", viewerName));
                    for (String taxon : results.keySet()) {
                        double value = results.get(taxon); // clamp to range -1 1
                        if (value < -1)
                            value = -1;
                        else if (value > 1)
                            value = 1;
                        sorted.add(new Pair<>(-value, taxon)); // negative sign to sort from high to low...
                    }
                    System.out.println("Number of results: " + sorted.size());
                    System.out.println("--------------------------------------");
                    for (Pair<Double, String> pair : sorted) {
                        if ((-pair.getFirst()) >= 0)
                            System.out.println(String.format("%-20s\t %1.4f", pair.getSecond(), (-pair.getFirst())));
                        else
                            System.out.println(String.format("%-20s\t%1.4f", pair.getSecond(), (-pair.getFirst())));
                    }
                    System.out.println("--------------------------------------");
                }
            }
            viewer.setIgnoreNextUpdateAll(true); // this prevents loss of selection
        }
    }

    public void actionPerformed(ActionEvent event) {
        List<String> openViewers = new ArrayList<>();
        for (String name : ClassificationManager.getAllSupportedClassifications()) {
            if (((Director) getDir()).getViewerByClassName(name) != null)
                openViewers.add(name);
        }
        if (!openViewers.contains(Classification.Taxonomy))
            openViewers.add(Classification.Taxonomy);

        if (openViewers.size() == 1) {
            execute("compute contrasts data=" + openViewers.get(0) + ";");
        } else if (openViewers.size() > 1) {
            String[] choices = openViewers.toArray(new String[0]);
            String result = (String) JOptionPane.showInputDialog(getViewer().getFrame(), "Choose viewer", "Choose viewer", JOptionPane.PLAIN_MESSAGE, ProgramProperties.getProgramIcon(), choices, choices[0]);
            if (result != null)
                execute("compute contrasts data=" + result + ";");
        }
    }

    public boolean isApplicable() {
        return (getViewer() instanceof GroupsViewer) && (((GroupsViewer) getViewer()).getGroupsPanel().getTwoSelectedGroups() != null);
    }

    public String getName() {
        return "Compute Contrasts...";
    }

    public String getDescription() {
        return "Computes contrasts on two selected groups";
    }

    public ImageIcon getIcon() {
        return null;
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }
}
