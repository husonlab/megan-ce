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
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import megan.chart.data.DefaultChartData;
import megan.chart.data.IChartData;
import megan.chart.drawers.BarChartDrawer;
import megan.chart.gui.ChartViewer;
import megan.core.Director;
import megan.core.Document;
import megan.core.SampleAttributeTable;
import megan.dialogs.input.InputDialog;
import megan.main.MeganProperties;
import megan.viewer.MainViewer;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.*;

/**
 * a metadata attribute chart
 * Daniel Huson, 10.2017
 */
public class MetadataChart extends ChartViewer {
    private boolean inSync = false;
    private final String cName;

    /**
     * constructor
     *
     * @param dir
     */
    public MetadataChart(final Director dir, MainViewer parent) {
        super(parent, dir, dir.getDocument().getSampleLabelGetter(), new DefaultChartData(), ProgramProperties.isUseGUI());
        cName = "MetaData";

        MeganProperties.addPropertiesListListener(getJMenuBar().getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);

        getToolbar().addSeparator(new Dimension(5, 10));
        getToolbar().add(getCommandManager().getButton("Sync"));
        chooseDrawer(BarChartDrawer.NAME);

        setChartTitle("Meta Data Chart for " + dir.getDocument().getTitle());
        final IChartData chartData = (IChartData) getChartData();
        String name = dir.getDocument().getTitle();
        if (name.length() > 20) {
            chartData.setDataSetName(name.substring(0, 20));
            name = name.substring(0, 20) + "...";
        }
        getChartData().setDataSetName(name);
        setWindowTitle(cName + " Chart");
        chartData.setSeriesLabel("Samples");
        chartData.setClassesLabel(cName);
        chartData.setCountsLabel("Counts");
        setClassLabelAngle(Math.PI / 4);

        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                final InputDialog inputDialog = InputDialog.getInstance();
                if (inputDialog != null)
                    inputDialog.setViewer(dir, MetadataChart.this);
            }
        });


        try {
            sync();
        } catch (CanceledException e) {
            Basic.caught(e);
        }

        int[] geometry = ProgramProperties.get(cName + "ChartGeometry", new int[]{100, 100, 800, 600});
        setSize(geometry[2], geometry[3]);
        setVisible(true);

        getFrame().addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                ProgramProperties.put(cName + "ChartGeometry", new int[]{
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

            final IChartData chartData = (IChartData) getChartData();
            chartData.clear();

            final Document doc = dir.getDocument();
            final SampleAttributeTable attributeTable = doc.getSampleAttributeTable();

            setChartTitle("Meta Data Chart for " + dir.getDocument().getTitle());
            final int numberOfSamples = doc.getNumberOfSamples();

            if (numberOfSamples > 0) {
                final ArrayList<String> sampleNames = new ArrayList<>(doc.getSampleNames());

                chartData.setAllSeries(sampleNames);

                final Map<String, float[]> attribute2sample2value = attributeTable.getNumericalAttributes(sampleNames, false);

                final Set<String> classNames = new TreeSet<>();
                for (String attribute : attributeTable.getAttributeOrder()) {
                    if (!attributeTable.isSecretAttribute(attribute) && !attributeTable.isHiddenAttribute(attribute)) {
                        final float[] values = attribute2sample2value.get(attribute);
                        if (values != null) { // is a numerical attribute
                            int t = 0;
                            for (String sample : attributeTable.getSampleOrder()) {
                                chartData.putValue(sample, attribute, values[t++]);
                                classNames.add(attribute);
                            }
                        } else { // no numerical attribute
                            final Map<String, Integer> attributeValue2count = new HashMap<>();
                            long total = 0;
                            for (String sample : attributeTable.getSampleOrder()) {
                                final Object value = attributeTable.get(sample, attribute);
                                final String key = attribute + ":" + value;
                                attributeValue2count.merge(key, 1, Integer::sum);
                                total++;

                            }
                            final Set<String> topAttributeValues = getTopFive(attributeValue2count);
                            long covered = 0;
                            for (String top : topAttributeValues) {
                                covered += attributeValue2count.get(top);
                            }
                            if (covered > 0.5 * total) {
                                for (String sample : attributeTable.getSampleOrder()) {
                                    final Object value = attributeTable.get(sample, attribute);
                                    final String key = attribute + ":" + value;
                                    if (topAttributeValues.contains(key)) {
                                        chartData.putValue(sample, key, 1);
                                        classNames.add(key);
                                    } else {
                                        chartData.putValue(sample, attribute + ":other", 1);
                                        classNames.add(attribute + ":other");
                                    }
                                }
                            }
                        }
                    }
                }
                chartData.setClassNames(classNames);

            }
            super.sync();
            inSync = false;
        }
    }

    /**
     * get the top five attributes
     *
     * @param attributeValue2count
     * @return top five
     */
    private Set<String> getTopFive(Map<String, Integer> attributeValue2count) {
        final List<Pair<Integer, String>> count2attribute = new ArrayList<>(attributeValue2count.size());
        for (String key : attributeValue2count.keySet()) {
            count2attribute.add(new Pair<>(-attributeValue2count.get(key), key));
        }

        count2attribute.sort(new Pair<>());

        final Set<String> result = new HashSet<>();
        for (int i = 0; i < Math.min(10, count2attribute.size()); i++)
            result.add(count2attribute.get(i).getSecond());
        return result;
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() throws CanceledException {
        MeganProperties.removePropertiesListListener(getJMenuBar().getRecentFilesListener());
        super.destroyView();
    }

    /**
     * get name for this type of parent
     *
     * @return name
     */
    public String getClassName() {
        return getClassName(cName);
    }

    public String getCName() {
        return cName;
    }

    private static String getClassName(String cName) {
        return cName + "Chart";
    }
}
