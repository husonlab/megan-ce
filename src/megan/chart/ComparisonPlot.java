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

import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.ProgressListener;
import megan.chart.data.DefaultPlot2DData;
import megan.chart.data.IPlot2DData;
import megan.chart.drawers.ChartDrawerBase;
import megan.chart.drawers.Plot2DDrawer;
import megan.chart.gui.ChartViewer;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.input.InputDialog;
import megan.main.MeganProperties;
import megan.viewer.ClassificationViewer;
import megan.viewer.ViewerBase;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * comparison chart
 * Daniel Huson, 2.2013
 */
public class ComparisonPlot extends ChartViewer {
    private final String cName;
    private final Document doc;
    private boolean inSync = false;
    private final ClassificationViewer viewer;

    /**
     * constructor
     *
     * @param dir
     * @param parent
     * @throws jloda.util.CanceledException
     */
    public ComparisonPlot(final Director dir, ClassificationViewer parent) throws CanceledException {
        super(parent, dir, dir.getDocument().getSampleLabelGetter(), new DefaultPlot2DData(), ProgramProperties.isUseGUI());
        viewer = parent;
        this.doc = dir.getDocument();
        this.cName = parent.getClassName();

        getToolbar().addSeparator(new Dimension(5, 10));
        getToolbar().add(getCommandManager().getButton("Sync"));

        MeganProperties.addPropertiesListListener(getJMenuBar().getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);

        chooseDrawer(Plot2DDrawer.NAME);
        ((ChartDrawerBase) getChartDrawer()).setSupportedScalingTypes(ScalingType.LINEAR);

        setChartTitle(cName + " vs " + cName + " plot for " + doc.getTitle());

        setWindowTitle(cName + " vs " + cName);

        getChartDrawer().setClassLabelAngle(0);

        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                InputDialog inputDialog = InputDialog.getInstance();
                if (inputDialog != null)
                    inputDialog.setViewer(dir, ComparisonPlot.this);
            }
        });

        sync();
    }


    /**
     * synchronize chart to reflect latest user selection in taxon chart
     */
    public void sync() throws CanceledException {
        if (!inSync) {
            inSync = true;

            setChartTitle(cName + " vs " + cName + " plot for " + doc.getTitle());

            String[] sampleNames = doc.getSampleNamesAsArray();
            for (int i = 0; i < sampleNames.length; i++) {
                String name1 = sampleNames[i];
                for (int j = i + 1; j < sampleNames.length; j++) {
                    String name2 = sampleNames[j];
                    String name = name1 + " vs " + name2;
                    ((Plot2DDrawer) getChartDrawer()).setShowDots(name, true);
                    ((Plot2DDrawer) getChartDrawer()).setShowLines(name, false);
                }
            }

            //setShowLegend(true);

            Map<String, Collection<Pair<Number, Number>>> name2counts = computeCounts(doc, viewer, doc.getProgressListener());

            IPlot2DData chartData = (IPlot2DData) getChartData();

            chartData.clear();
            chartData.setDataSetName(doc.getTitle());
            for (String name : name2counts.keySet())
                chartData.setDataForSeries(name, name2counts.get(name));

            if (sampleNames.length == 2) {
                getChartData().setSeriesLabel(sampleNames[0]);
                getChartData().setCountsLabel(sampleNames[1]);
                if (name2counts.values().size() > 0) {
                    Collection<Pair<Number, Number>> pairs = name2counts.values().iterator().next();
                    double correlationCoefficent = computePearsonsCorrelation(pairs);
                    System.err.println("Number of points: " + pairs.size() + ", Pearson's correlation: " + correlationCoefficent);
                }
            }

            if (getChartDrawer() instanceof Plot2DDrawer) {
                Plot2DDrawer drawer = (Plot2DDrawer) getChartDrawer();
                for (String name : name2counts.keySet()) {
                    drawer.setUseJitter(name, true);
                }
            }

            super.sync();

            inSync = false;
        }
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() throws CanceledException {
        super.destroyView();
    }

    /**
     * compute sample vs sample data
     *
     * @param progressListener
     * @return values for 10-100 percent, for each dataset in the document
     * @throws jloda.util.CanceledException
     */
    private Map<String, Collection<Pair<Number, Number>>> computeCounts(Document doc, ViewerBase viewer, ProgressListener progressListener) throws CanceledException {
        progressListener.setTasks(cName + " vs " + cName, "Sampling from current leaves");
        progressListener.setMaximum(11 * doc.getNumberOfSamples());
        progressListener.setProgress(0);

        Map<String, Collection<Pair<Number, Number>>> plotName2Counts = new HashMap<>();

        String[] sampleNames = doc.getSampleNamesAsArray();
        for (int i = 0; i < sampleNames.length; i++) {
            String name1 = sampleNames[i];
            for (int j = i + 1; j < sampleNames.length; j++) {
                String name2 = sampleNames[j];
                String name = name1 + " vs " + name2;
                for (Node v : viewer.getSelectedNodes()) {
                    float[] counts = ((NodeData) v.getData()).getAssigned();
                    if (j < counts.length && counts[i] > 0 || counts[j] > 0) {
                        Collection<Pair<Number, Number>> pairs = plotName2Counts.computeIfAbsent(name, k -> new LinkedList<>());
                        pairs.add(new Pair<>(counts[i], counts[i + 1]));

                    }
                }
            }
        }
        return plotName2Counts;
    }

    /**
     * computes the Pearson's correlation for a list of pairs
     *
     * @param pairs
     * @return r
     */
    private static double computePearsonsCorrelation(Collection<Pair<Number, Number>> pairs) {
        double[] mean = new double[2];

        for (Pair<Number, Number> pair : pairs) {
            mean[0] += pair.get1().doubleValue();
            mean[1] += pair.get2().doubleValue();
        }
        mean[0] /= pairs.size();
        mean[1] /= pairs.size();

        double[] stddev = new double[2];
        for (Pair<Number, Number> pair : pairs) {
            stddev[0] += (pair.get1().doubleValue() - mean[0]) * (pair.get1().doubleValue() - mean[0]);
            stddev[1] += (pair.get2().doubleValue() - mean[1]) * (pair.get2().doubleValue() - mean[1]);
        }
        stddev[0] = Math.sqrt(stddev[0] / pairs.size());
        stddev[1] = Math.sqrt(stddev[1] / pairs.size());


        double cor = 0;
        for (Pair<Number, Number> pair : pairs) {
            cor += (pair.get1().doubleValue() - mean[0]) * (pair.get2().doubleValue() - mean[1]) / (stddev[0] * stddev[1]);
        }
        cor /= pairs.size();
        return cor;
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
        return cName + "vs" + cName;
    }

}
