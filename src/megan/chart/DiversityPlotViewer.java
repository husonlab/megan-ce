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

import jloda.util.*;
import megan.alignment.AlignmentViewer;
import megan.alignment.WordCountAnalysis;
import megan.chart.data.DefaultPlot2DData;
import megan.chart.data.IPlot2DData;
import megan.chart.drawers.Plot2DDrawer;
import megan.chart.gui.ChartViewer;
import megan.core.Director;
import megan.dialogs.input.InputDialog;
import megan.main.MeganProperties;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * a diversity chart
 * Daniel Huson, 6.2012
 */
public class DiversityPlotViewer extends ChartViewer {
    private final AlignmentViewer alignmentViewer;
    private final int kmer;
    private final int step;
    private final int mindepth;

    private boolean inSync = false;

    /**
     * constructor
     *
     * @param dir
     */
    public DiversityPlotViewer(final Director dir, AlignmentViewer alignmentViewer, int kmer, int step, int mindepth) throws CanceledException {
        super(dir.getMainViewer(), dir, dir.getDocument().getSampleLabelGetter(), new DefaultPlot2DData(), ProgramProperties.isUseGUI());
        this.alignmentViewer = alignmentViewer;
        this.kmer = kmer;
        this.step = step;
        this.mindepth = mindepth;

        getToolbar().addSeparator(new Dimension(5, 10));
        getToolbar().add(getCommandManager().getButton("Sync"));

        MeganProperties.addPropertiesListListener(getJMenuBar().getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);

        chooseDrawer(Plot2DDrawer.NAME);
        IPlot2DData chartData = new DefaultPlot2DData();

        setChartTitle("Diversity plot for " + alignmentViewer.getSelectedReference());
        String name = Basic.swallowLeadingGreaterSign(alignmentViewer.getSelectedReference());
        if (name.length() > 20) {
            chartData.setDataSetName(name.substring(0, 20));
            name = name.substring(0, 20) + "...";
        } else
            chartData.setDataSetName(name);

        setWindowTitle("Diversity plot for '" + name + "'");

        getChartDrawer().setClassLabelAngle(0);

        sync();

        int[] geometry = ProgramProperties.get(MeganProperties.TAXA_CHART_WINDOW_GEOMETRY, new int[]{100, 100, 800, 600});
        setSize(geometry[2], geometry[3]);

        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                InputDialog inputDialog = InputDialog.getInstance();
                if (inputDialog != null)
                    inputDialog.setViewer(dir, DiversityPlotViewer.this);
            }
        });
    }

    /**
     * synchronize chart to reflect latest user selection in taxon chart
     */
    public void sync() throws CanceledException {
        if (!inSync) {
            inSync = true;
            String dataSetName = "Data";
            ((Plot2DDrawer) getChartDrawer()).setShowLines(dataSetName, false);
            ((Plot2DDrawer) getChartDrawer()).setShowDots(dataSetName, true);
            ((Plot2DDrawer) getChartDrawer()).setUseJitter(dataSetName, true);
            getChartColorManager().setSampleColor(dataSetName, Color.GRAY);

            setShowLegend("none");
            String graphName = "Menten Kinetics";
            ((Plot2DDrawer) getChartDrawer()).setShowLines(graphName, true);
            ((Plot2DDrawer) getChartDrawer()).setShowDots(graphName, false);


            IPlot2DData chartData = (IPlot2DData) getChartData();
            chartData.clear();

            LinkedList<Pair<Number, Number>> values = new LinkedList<>();
            SortedMap<Number, Number> rank2percentage = new TreeMap<>();
            WordCountAnalysis.apply(alignmentViewer.getAlignment(), kmer, step, mindepth, dir.getDocument().getProgressListener(), values, rank2percentage);
            Single<Integer> extrapolatedCount = new Single<>();
            LinkedList<Pair<Number, Number>> mentenKinetics = WordCountAnalysis.computeMentenKinetics(values, extrapolatedCount);

            chartData.setDataForSeries(graphName, mentenKinetics);
            getChartColorManager().setSampleColor(graphName, Color.BLUE);

            if (extrapolatedCount.get() != null && extrapolatedCount.get() > 0) {
                LinkedList<Pair<Number, Number>> exCount = new LinkedList<>();
                Pair<Number, Number> range = chartData.getRangeX();
                exCount.add(new Pair<>(range.getFirst(), extrapolatedCount.get()));
                exCount.add(new Pair<>(range.getSecond(), extrapolatedCount.get()));
                chartData.setDataForSeries("Extrapolation", exCount);
                ((Plot2DDrawer) getChartDrawer()).setShowLines("Extrapolation", true);
                ((Plot2DDrawer) getChartDrawer()).setShowDots("Extrapolation", false);
                getChartColorManager().setSampleColor("Extrapolation", ProgramProperties.SELECTION_COLOR_ADDITIONAL_TEXT);
            }

            double predicted = 0;
            if (mentenKinetics.size() > 0)
                predicted = mentenKinetics.getLast().getSecond().doubleValue();

            chartData.setDataForSeries(dataSetName, values);
            getStatusbar().setText1("Size: " + values.size());
            Pair<Float, Float> averageNandK = WordCountAnalysis.computeAverageNandK(values);
            getStatusbar().setText2(String.format("Average diversity ratio: %.1f/%.1f = %.1f. Distinct genomes: min=%d extrapolation=%d",
                    averageNandK.get2(), averageNandK.get1(),
                    averageNandK.get1() > 0 ? averageNandK.get2() / averageNandK.get1() : 0, (int) Math.round(predicted), extrapolatedCount.get()));

            getChartData().setSeriesLabel("Number of sequences (n)");
            getChartData().setCountsLabel("Number of distinct sequences (k)");
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
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "DiversityPlotViewer";
    }


}
