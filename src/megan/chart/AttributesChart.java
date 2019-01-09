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

import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import megan.chart.data.DefaultChartData;
import megan.chart.data.IChartData;
import megan.chart.drawers.BarChartDrawer;
import megan.chart.gui.ChartViewer;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.attributes.MicrobialAttributes;
import megan.dialogs.input.InputDialog;
import megan.main.MeganProperties;
import megan.viewer.MainViewer;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * a taxa chart
 * Daniel Huson, 6.2012
 */
public class AttributesChart extends ChartViewer {
    private boolean inSync = false;

    /**
     * constructor
     *
     * @param dir
     */
    public AttributesChart(final Director dir) {
        super(dir.getMainViewer(), dir, dir.getDocument().getSampleLabelGetter(), new DefaultChartData(), ProgramProperties.isUseGUI());
        MeganProperties.addPropertiesListListener(getJMenuBar().getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);

        getToolbar().addSeparator(new Dimension(5, 10));
        getToolbar().add(getCommandManager().getButton("Sync"));
        chooseDrawer(BarChartDrawer.NAME);

        setChartTitle("Microbial Attributes for " + dir.getDocument().getTitle());
        IChartData chartData = (IChartData) getChartData();
        String name = dir.getDocument().getTitle();
        if (name.length() > 20)
            chartData.setDataSetName(name.substring(0, 20));
        else
            getChartData().setDataSetName(name);
        setWindowTitle("Microbial Attributes Chart");
        chartData.setSeriesLabel("Samples");
        chartData.setClassesLabel("Microbial Attributes");
        chartData.setCountsLabel(dir.getDocument().getReadAssignmentMode().getDisplayLabel());
        setClassLabelAngle(Math.PI / 4);
        //setShowLegend(true);

        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                InputDialog inputDialog = InputDialog.getInstance();
                if (inputDialog != null)
                    inputDialog.setViewer(dir, AttributesChart.this);
            }
        });


        try {
            sync();
        } catch (CanceledException e) {
            Basic.caught(e);
        }

        setVisible(true);
    }

    /**
     * synchronize chart to reflect latest user selection in taxon chart
     */
    public void sync() throws CanceledException {
        if (!inSync) {
            inSync = true;

            IChartData chartData = (IChartData) getChartData();
            chartData.clear();

            try {
                MicrobialAttributes attributes = MicrobialAttributes.getInstance();

                Document doc = dir.getDocument();
                MainViewer mainViewer = dir.getMainViewer();
                int numberOfDatasets = doc.getNumberOfSamples();

                if (numberOfDatasets > 0) {
                    final Map<String, Map<String, Integer>> dataset2AttributeState2Value = attributes.getDataSet2AttributeState2Value(mainViewer);

                    chartData.setAllSeries(doc.getSampleNames());
                    SortedSet<String> classNames = new TreeSet<>();
                    for (String series : dataset2AttributeState2Value.keySet()) {
                        Map<String, Integer> attributeState2value = dataset2AttributeState2Value.get(series);
                        for (String attributeState : attributeState2value.keySet()) {
                            classNames.add(attributeState);
                            Integer value = attributeState2value.get(attributeState);
                            if (value == null)
                                value = 0;
                            chartData.putValue(series, attributeState, value);
                        }
                    }
                    chartData.setClassNames(classNames);
                }
            } catch (IOException e) {
                Basic.caught(e);
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
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "MicrobialAttributesChart";
    }


}
