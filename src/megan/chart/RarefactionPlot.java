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
package megan.chart;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.graph.NodeIntegerArray;
import jloda.phylo.PhyloTree;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.ProgressListener;
import megan.chart.data.DefaultPlot2DData;
import megan.chart.data.IPlot2DData;
import megan.chart.drawers.Plot2DDrawer;
import megan.chart.gui.ChartViewer;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.input.InputDialog;
import megan.main.MeganProperties;
import megan.viewer.ClassificationViewer;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;

/**
 * Rarefaction plot
 * Daniel Huson, 7.2012, 4.2015
 */
public class RarefactionPlot extends ChartViewer {
    private final Document doc;
    private final String cName;
    private final ClassificationViewer viewer;

    private boolean inSync = false;

    /**
     * constructor
     *
     * @param dir
     * @param parent
     * @throws jloda.util.CanceledException
     */
    public RarefactionPlot(final Director dir, ClassificationViewer parent) throws CanceledException {
        super(parent, dir, dir.getDocument().getSampleLabelGetter(), new DefaultPlot2DData(), ProgramProperties.isUseGUI());
        viewer = parent;
        this.cName = parent.getClassName();
        doc = dir.getDocument();

        getToolbar().addSeparator(new Dimension(5, 10));
        getToolbar().add(getCommandManager().getButton("Sync"));

        MeganProperties.addPropertiesListListener(getJMenuBar().getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);

        chooseDrawer(Plot2DDrawer.NAME);

        setChartTitle(cName + " rarefaction plot for " + doc.getTitle());
        setWindowTitle(cName + " rarefaction");

        getChartDrawer().setClassLabelAngle(0);

        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                InputDialog inputDialog = InputDialog.getInstance();
                if (inputDialog != null)
                    inputDialog.setViewer(dir, RarefactionPlot.this);
            }
        });

        sync();

        int[] geometry = ProgramProperties.get(cName + "RareFactionChartGeometry", new int[]{100, 100, 800, 600});
        setSize(geometry[2], geometry[3]);

        getFrame().addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                ProgramProperties.put(cName + "RareFactionChartGeometry", new int[]{
                        getLocation().x, getLocation().y, getSize().width, getSize().height});
            }
        });
    }


    /**
     * synchronize chart to reflect latest user selection in taxon chart
     */
    public void sync() throws CanceledException {
        if (!inSync) {
            inSync = true;

            setChartTitle(cName + " rarefaction plot for " + doc.getTitle());

            for (String name : doc.getSampleNames()) {
                ((Plot2DDrawer) getChartDrawer()).setShowLines(name, true);
                ((Plot2DDrawer) getChartDrawer()).setShowDots(name, true);
            }

            final Map<String, Collection<Pair<Number, Number>>> name2counts = computeCounts(doc, 1, viewer, doc.getProgressListener());

            final IPlot2DData chartData = (IPlot2DData) getChartData();

            chartData.clear();
            chartData.setDataSetName(doc.getTitle());
            for (String name : doc.getSampleNames())
                chartData.setDataForSeries(name, name2counts.get(name));

            getChartData().setSeriesLabel("Number of reads sampled from leaves");
            getChartData().setCountsLabel("Number of leaves in " + cName + " tree");

            super.sync();
            inSync = false;
        }
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() throws CanceledException {
        MeganProperties.removePropertiesListListener(getJMenuBar().getRecentFilesListener());
        super.destroyView();
    }

    /**
     * do rarefaction analysis for parent
     *
     * @param threshold
     * @param viewer
     * @param progressListener
     * @return values for 10-100 percent, for each dataset in the document
     * @throws jloda.util.CanceledException
     */
    private static Map<String, Collection<Pair<Number, Number>>> computeCounts(Document doc, int threshold, ClassificationViewer viewer, ProgressListener progressListener) throws CanceledException {
        final String cName = viewer.getClassName();
        final int numberOfPoints = ProgramProperties.get("NumberRareFactionDataPoints", 20);
        final int numberOfReplicates = ProgramProperties.get("NumberRareFactionReplicates", 10);
        progressListener.setTasks(cName + " rarefaction analysis", "Sampling from current leaves");
        progressListener.setMaximum(numberOfPoints * numberOfReplicates * doc.getNumberOfSamples());
        progressListener.setProgress(0);

        Random rand = new Random(666);
        final PhyloTree tree = viewer.getTree();

        Map<String, Collection<Pair<Number, Number>>> name2counts = new HashMap<>();

        for (int pid = 0; pid < doc.getNumberOfSamples(); pid++) {
            NodeIntegerArray numbering = new NodeIntegerArray(tree);

            int numberOfReads = computeCountRec(pid, tree.getRoot(), viewer, 0, numbering);
            progressListener.incrementProgress();

            Vector<Float> counts = new Vector<>();
            counts.add(0f);

            NodeIntegerArray[] node2count = new NodeIntegerArray[numberOfReplicates];
            for (int r = 0; r < numberOfReplicates; r++)
                node2count[r] = new NodeIntegerArray(tree);

            Set<Node> nodes = new HashSet<>();
            int batchSize = numberOfReads / numberOfPoints;
            for (int p = 1; p <= numberOfPoints; p++) {
                for (int r = 0; r < numberOfReplicates; r++) {
                    for (int i = 1; i <= batchSize; i++) {
                        int which = rand.nextInt(numberOfReads);
                        Node v = getIdRec(tree.getRoot(), which, numbering);
                        nodes.add(v);
                        node2count[r].set(v, node2count[r].getValue(v) + 1);
                    }
                    progressListener.incrementProgress();
                }
                int count = 0;
                for (int r = 0; r < numberOfReplicates; r++) {
                    for (Node v : nodes) {
                        if (node2count[r].getValue(v) >= threshold)
                            count++;
                    }
                }
                counts.add((float) count / (float) numberOfReplicates);
            }

            ArrayList<Pair<Number, Number>> list = new ArrayList<>(counts.size());
            int sampleSize = 0;
            for (int p = 0; p <= numberOfPoints; p++) {
                list.add(new Pair<>(sampleSize, counts.get(p)));
                sampleSize += batchSize;
            }
            name2counts.put(doc.getSampleNames().get(pid), list);
        }
        return name2counts;
    }

    /**
     * each node is numbered by the count of reads that come before it
     *
     * @param v
     * @param top
     * @param numbering
     * @return count for node
     */
    private static int computeCountRec(int pid, Node v, ClassificationViewer viewer, int top, NodeIntegerArray numbering) {
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            top = computeCountRec(pid, e.getTarget(), viewer, top, numbering);
        }
        if (v.getOutDegree() == 0) // sample from leaves only
        {
            NodeData data = viewer.getNodeData(v);
            if (data != null && data.getSummarized() != null)
                top += data.getSummarized()[pid];
        }
        numbering.set(v, top);
        return top;
    }

    /**
     * gets the node hit
     *
     * @param v
     * @param which
     * @param counts
     * @return
     */
    private static Node getIdRec(Node v, int which, NodeIntegerArray counts) {
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            Node w = e.getTarget();
            if (which <= counts.getValue(w)) {
                return getIdRec(w, which, counts);
            }
        }
        return v;
    }

    /**
     * get name for this type of parent
     *
     * @return name
     */
    public String getClassName() {
        return getClassName(cName);
    }


    public static String getClassName(String cName) {
        return cName + "RarefactionPlot";
    }
}
