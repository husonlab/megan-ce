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
import jloda.graph.NodeSet;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import megan.chart.data.DefaultChartData;
import megan.chart.data.IChartData;
import megan.chart.drawers.BarChartDrawer;
import megan.chart.gui.ChartViewer;
import megan.commands.OpenNCBIWebPageCommand;
import megan.commands.OpenWebPageCommand;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.input.InputDialog;
import megan.main.MeganProperties;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;

/**
 * a taxa chart
 * Daniel Huson, 6.2012
 */
public class TaxaChart extends ChartViewer {
    private NodeSet syncedNodes;
    private boolean inSync = false;
    private String colorByRank = null;
    private final MainViewer mainViewer;

    /**
     * constructor
     *
     * @param dir
     */
    public TaxaChart(final Director dir) {
        super(dir.getMainViewer(), dir, dir.getDocument().getSampleLabelGetter(), new DefaultChartData(), ProgramProperties.isUseGUI());
        this.mainViewer = dir.getMainViewer();
        MeganProperties.addPropertiesListListener(getJMenuBar().getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);

        getToolbar().addSeparator(new Dimension(5, 10));
        getToolbar().add(getCommandManager().getButton("Sync"));
        chooseDrawer(BarChartDrawer.NAME);

        setChartTitle("Taxonomy profile for " + dir.getDocument().getTitle());
        IChartData chartData = (IChartData) getChartData();
        String name = dir.getDocument().getTitle();
        if (name.length() > 20)
            chartData.setDataSetName(name.substring(0, 20));
        else
            getChartData().setDataSetName(name);
        setWindowTitle("Taxonomy Chart");
        chartData.setSeriesLabel("Samples");
        chartData.setClassesLabel("Taxa");
        chartData.setCountsLabel(dir.getDocument().getReadAssignmentMode().getDisplayLabel());
        setClassLabelAngle(Math.PI / 4);
        //setShowLegend(true);

        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                InputDialog inputDialog = InputDialog.getInstance();
                if (inputDialog != null)
                    inputDialog.setViewer(dir, TaxaChart.this);
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
                                command.apply("show webPage classification='Taxonomy' id='" + label + "';");
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

        {
            final OpenNCBIWebPageCommand command = new OpenNCBIWebPageCommand();
            command.setViewer(this);
            Action action = new AbstractAction(OpenNCBIWebPageCommand.NAME) {
                public void actionPerformed(ActionEvent actionEvent) {
                    java.util.Collection<String> selectedIds = getClassesList().getSelectedLabels();
                    if (selectedIds.size() > 0) {
                        if (selectedIds.size() >= 5 && JOptionPane.showConfirmDialog(getFrame(), "Do you really want to open " + selectedIds.size() +
                                        " windows in your browser?", "Confirmation - MEGAN", JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon()) != JOptionPane.YES_OPTION)
                            return;
                        for (String label : selectedIds) {
                            try {
                                command.apply("show webPage taxon='" + label + "';");
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

        int[] geometry = ProgramProperties.get(MeganProperties.TAXA_CHART_WINDOW_GEOMETRY, new int[]{100, 100, 800, 600});
        setSize(geometry[2], geometry[3]);

        getFrame().addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                ProgramProperties.put(MeganProperties.TAXA_CHART_WINDOW_GEOMETRY, new int[]{
                        getLocation().x, getLocation().y, getSize().width, getSize().height});
            }
        });
        setVisible(true);

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
            setChartTitle("Taxonomy profile for " + doc.getTitle());

            int numberOfSamples = doc.getNumberOfSamples();
            if (numberOfSamples > 0) {
                final MainViewer mainViewer = dir.getMainViewer();

                if (mainViewer.getSelectedNodes().size() == 0) {
                    mainViewer.selectAllLeaves();
                    if (mainViewer.getPOWEREDBY() != null)
                        setChartTitle(getChartTitle() + " (rank=" + mainViewer.getPOWEREDBY() + ")");
                }

                chartData.setAllSeries(doc.getSampleNames());

                final String[] names = doc.getSampleNames().toArray(new String[0]);
                final float[] totalSizes = new float[doc.getSampleNames().size()];

                syncedNodes = mainViewer.getSelectedNodes();
                final LinkedList<String> taxonNames = new LinkedList<>();

                for (Node v = mainViewer.getGraph().getFirstNode(); v != null; v = v.getNext()) {
                    if (syncedNodes.contains(v)) {
                        final String taxonName = TaxonomyData.getName2IdMap().get((Integer) v.getInfo());

                        taxonNames.add(taxonName);
                        if (numberOfSamples == 1) {
                            if (v.getOutDegree() == 0)
                                chartData.putValue(names[0], taxonName, ((NodeData) v.getData()).getCountSummarized());
                            else
                                chartData.putValue(names[0], taxonName, ((NodeData) v.getData()).getCountAssigned());

                        } else {
                            float[] values;
                            if (v.getOutDegree() == 0)
                                values = ((NodeData) v.getData()).getSummarized();
                            else
                                values = ((NodeData) v.getData()).getAssigned();
                            for (int i = 0; i < names.length; i++) {
                                chartData.putValue(names[i], taxonName, values[i]);
                            }
                        }
                    }
                    float[] values;
                    if (v.getOutDegree() == 0)
                        values = ((NodeData) v.getData()).getSummarized();
                    else
                        values = ((NodeData) v.getData()).getAssigned();
                    for (int i = 0; i < names.length; i++) {
                        totalSizes[i] += values[i];
                    }
                }
                chartData.setAllSeriesTotalSizes(totalSizes);
                chartData.setClassNames(taxonNames);
            }
            updateColorByRank();
            chartData.setTree(mainViewer.getInducedTree(TaxonomyData.getName2IdMap().getId2Name(), mainViewer.getSelectedNodes()));
            super.sync();
            updateView(Director.ALL);
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

    public String getColorByRank() {
        return colorByRank;
    }

    public void setColorByRank(String colorByRank) {
        this.colorByRank = colorByRank;
    }

    public void updateColorByRank() {
        String rank = getColorByRank();
        getClass2HigherClassMapper().clear();

        if (rank == null)
            return;

        int rankId = TaxonomicLevels.getId(rank);

        if (rank.equals("None") || rankId == 0) {
            getStatusbar().setText2("");
        } else {
            getStatusbar().setText2("Colored by rank=" + rank);

            if (syncedNodes != null) {
                for (Node v : syncedNodes) {
                    Node w = v;
                    String taxName = TaxonomyData.getName2IdMap().get((Integer) v.getInfo());
                    boolean ok = false;
                    while (true) {
                        Integer taxId = (Integer) w.getInfo();
                        int taxLevel = TaxonomyData.getTaxonomicRank(taxId);
                        if (taxLevel == rankId) {
                            getClass2HigherClassMapper().put(taxName, TaxonomyData.getName2IdMap().get(taxId));
                            ok = true;
                            break;
                        }
                        if (w.getInDegree() > 0)
                            w = w.getFirstInEdge().getSource();
                        else
                            break;
                    }
                    if (!ok)
                        getClass2HigherClassMapper().put(TaxonomyData.getName2IdMap().get((Integer) v.getInfo()), "GRAY"); // will be shown in gray

                }
            }
        }
    }

    public NodeSet getSyncedNodes() {
        return syncedNodes;
    }

    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "TaxaChart";
    }
}
