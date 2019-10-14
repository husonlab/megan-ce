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
import megan.commands.OpenWebPageCommand;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.input.InputDialog;
import megan.main.MeganProperties;
import megan.viewer.ClassificationViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;

/**
 * a Viewer chart
 * Daniel Huson, 6.2012, 4.2015
 */
public class FViewerChart extends ChartViewer {
    private boolean inSync = false;
    private final String cName;
    private final ClassificationViewer viewer;

    /**
     * constructor
     *
     * @param dir
     */
    public FViewerChart(final Director dir, ClassificationViewer parent) {
        super(parent, dir, dir.getDocument().getSampleLabelGetter(), new DefaultChartData(), ProgramProperties.isUseGUI());
        viewer = parent;
        cName = parent.getClassName();

        MeganProperties.addPropertiesListListener(getJMenuBar().getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);

        getToolbar().addSeparator(new Dimension(5, 10));
        getToolbar().add(getCommandManager().getButton("Sync"));
        chooseDrawer(BarChartDrawer.NAME);

        setChartTitle(parent.getClassName() + " profile for " + dir.getDocument().getTitle());
        IChartData chartData = (IChartData) getChartData();
        String name = dir.getDocument().getTitle();
        if (name.length() > 20) {
            chartData.setDataSetName(name.substring(0, 20));
            name = name.substring(0, 20) + "...";
        }
        getChartData().setDataSetName(name);
        setWindowTitle(cName + " Chart");
        chartData.setSeriesLabel("Samples");
        chartData.setClassesLabel(cName);
        chartData.setCountsLabel("Number of reads");
        setClassLabelAngle(Math.PI / 4);

        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                InputDialog inputDialog = InputDialog.getInstance();
                if (inputDialog != null)
                    inputDialog.setViewer(dir, FViewerChart.this);
            }
        });

        getClassesList().getPopupMenu().addSeparator();
        {
            final OpenWebPageCommand command = new OpenWebPageCommand();
            command.setViewer(this);
            Action action = new AbstractAction(OpenWebPageCommand.NAME) {
                public void actionPerformed(ActionEvent actionEvent) {
                    java.util.Collection<String> selectedIds = getClassesList().getSelectedLabels();
                    if (selectedIds.size() > 0) {
                        if (selectedIds.size() >= 5 && JOptionPane.showConfirmDialog(getFrame(), "Do you really want to open " + selectedIds.size() +
                                        " windows in your browser?", "Confirmation - MEGAN", JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon()) != JOptionPane.YES_OPTION)
                            return;
                        for (String label : selectedIds) {
                            try {
                                command.apply("show webPage classification=" + cName + " id='" + label + "';");
                            } catch (Exception e) {
                                Basic.caught(e);
                            }
                        }
                    }
                }
            };
            action.putValue(AbstractAction.SMALL_ICON, command.getIcon());
            getClassesList().getPopupMenu().add(action);
        }


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

            IChartData chartData = (IChartData) getChartData();
            chartData.clear();

            Document doc = dir.getDocument();
            setChartTitle(cName + " profile for " + doc.getTitle());
            int numberOfSamples = doc.getNumberOfSamples();
            if (numberOfSamples > 0) {
                if (viewer.getSelectedNodes().size() == 0) {
                    viewer.selectAllLeaves();
                }
                chartData.setAllSeries(doc.getSampleNames());
                String[] sampleNames = doc.getSampleNames().toArray(new String[0]);

                java.util.Collection<Integer> ids = viewer.getSelectedIds();
                LinkedList<String> classNames = new LinkedList<>();
                for (Integer id : ids) {
                    String className = viewer.getClassification().getName2IdMap().get(id);
                    classNames.add(className);
                    float[] summarized = viewer.getSummarized(id);

                    for (int i = 0; i < sampleNames.length; i++) {
                        chartData.putValue(sampleNames[i], className, summarized[i]);
                    }
                }
                chartData.setClassNames(classNames);
            }
            chartData.setTree(viewer.getInducedTree(viewer.getClassification().getName2IdMap().getId2Name(), viewer.getSelectedNodes()));
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

    public static String getClassName(String cName) {
        return cName + "Chart";
    }
}
