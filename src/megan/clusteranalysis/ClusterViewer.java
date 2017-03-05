/*
 *  Copyright (C) 2017 Daniel H. Huson
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
package megan.clusteranalysis;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.graphview.EdgeView;
import jloda.graphview.GraphView;
import jloda.graphview.NodeShape;
import jloda.graphview.NodeView;
import jloda.gui.MenuBar;
import jloda.gui.StatusBar;
import jloda.gui.ToolBar;
import jloda.gui.commands.CommandManager;
import jloda.gui.director.*;
import jloda.gui.find.FindToolBar;
import jloda.gui.find.SearchManager;
import jloda.gui.format.Formatter;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.clusteranalysis.gui.*;
import megan.clusteranalysis.indices.CalculateEcologicalIndices;
import megan.clusteranalysis.indices.JensenShannonDivergence;
import megan.clusteranalysis.indices.PearsonDistance;
import megan.clusteranalysis.indices.UniFrac;
import megan.clusteranalysis.tree.Distances;
import megan.clusteranalysis.tree.Taxa;
import megan.core.Director;
import megan.core.Document;
import megan.core.SelectionSet;
import megan.dialogs.input.InputDialog;
import megan.fx.NotificationsInSwing;
import megan.main.MeganProperties;
import megan.util.Appliable;
import megan.viewer.MainViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.util.*;

/**
 * viewer for distance-based comparison of metagenome datasets
 * Daniel Huson, 5.2010
 */
public class ClusterViewer extends JFrame implements IDirectableViewer, IViewerWithFindToolBar, IViewerWithLegend, Printable {
    public static int PCoA_TAB_INDEX = 0;
    public static int UPGMA_TAB_INDEX = 1;
    public static int NJ_TAB_INDEX = 2;
    public static int NNET_TAB_INDEX = 3;
    public static int MATRIX_TAB_INDEX = 4;

    private int currentTab = 0;

    private final Director dir;
    private final Document doc;
    private final JFrame frame;
    private final JPanel mainPanel;
    private final jloda.gui.StatusBar statusBar;
    private final JTabbedPane tabbedPane;

    private final MatrixTab matrixTab;
    private final UPGMATab upgmaTab;
    private final NJTab njTab;
    private final NNetTab nnetTab;
    private final PCoATab pcoaTab;

    private final LegendPanel legendPanel;
    private final JScrollPane legendScrollPane;
    private String showLegend = "none";
    private final JSplitPane splitPane;

    private final MenuBar menuBar;
    public final CommandManager commandManager;
    final SelectionSet.SelectionListener selectionListener;

    private int numberOfNodesUsed = 0;

    private String dataType;
    private String ecologicalIndex = CalculateEcologicalIndices.BRAYCURTIS;

    private boolean locked = false;
    private boolean uptodate = true;

    private boolean showFindToolBar = false;
    private final SearchManager searchManager;
    private final ClusterAnalysisSearcher clusterAnalysisSearcher;

    private boolean useColors = true;
    private boolean showLabels = true;
    private int nodeRadius = 12; // default node radius

    private final HashMap<String, NodeShape> label2shape = new HashMap<>();

    private final Map<String, LinkedList<Node>> group2Nodes = new HashMap<>(); // used in calc of convex hulls

    public boolean updateConvexHulls = false;

    private Taxa taxa;
    private Distances distances;

    private ViewerBase parentViewer;

    public static Appliable clusterViewerAddOn;


    /**
     * creates a new network viewer
     *
     * @param dir
     */
    public ClusterViewer(final Director dir, ViewerBase parentViewer, String dataType) {
        this.dataType = dataType;
        this.parentViewer = parentViewer;

        this.dir = dir;
        this.doc = dir.getDocument();

        frame = new JFrame();
        frame.getContentPane().setLayout(new BorderLayout());
        frame.setSize(600, 600);
        frame.setLocationRelativeTo(parentViewer.getFrame());
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        commandManager = new CommandManager(dir, this, new String[]{"megan.commands", "megan.clusteranalysis.commands"}, !ProgramProperties.isUseGUI());

        menuBar = new MenuBar(this, GUIConfiguration.getMenuConfiguration(), getCommandManager());

        frame.setJMenuBar(menuBar);
        MeganProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());

        frame.add(new ToolBar(GUIConfiguration.getToolBarConfiguration(), commandManager), BorderLayout.NORTH);
        frame.setIconImage(ProgramProperties.getProgramIcon().getImage());
        statusBar = new jloda.gui.StatusBar();
        frame.add(statusBar, BorderLayout.SOUTH);

        tabbedPane = new JTabbedPane();

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        legendPanel = new LegendPanel(this);
        legendScrollPane = new JScrollPane(legendPanel);
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPanel, legendScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setEnabled(true);
        splitPane.setResizeWeight(1.0);
        splitPane.setDividerLocation(1.0);
        splitPane.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent componentEvent) {
                if (getShowLegend().equals("none"))
                    splitPane.setDividerLocation(1.0);
                legendPanel.updateView();
            }
        });

        frame.getContentPane().add(splitPane, BorderLayout.CENTER);

        upgmaTab = new UPGMATab(this);
        njTab = new NJTab(this);
        njTab.getGraphView().trans.setLockXYScale(true);
        nnetTab = new NNetTab(this);
        nnetTab.getGraphView().trans.setLockXYScale(true);
        pcoaTab = new PCoATab(this);
        matrixTab = new MatrixTab(this.getFrame());

        tabbedPane.add(pcoaTab.getLabel(), pcoaTab);
        tabbedPane.add(upgmaTab.getLabel(), upgmaTab);
        tabbedPane.add(njTab.getLabel(), njTab);
        tabbedPane.add(nnetTab.getLabel(), nnetTab);
        tabbedPane.add(matrixTab.getLabel(), matrixTab);

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                int tabId = tabbedPane.getSelectedIndex();
                if (tabId != currentTab) {
                    if (tabbedPane.getComponentAt(currentTab) instanceof ITab) {
                        ((ITab) tabbedPane.getComponentAt(currentTab)).deactivate();
                    }
                    if (tabbedPane.getSelectedComponent() instanceof ITab && ((ITab) tabbedPane.getSelectedComponent()).needsUpdate()) {
                        final ITab tab = ((ITab) tabbedPane.getSelectedComponent());
                        tab.activate();
                        dir.execute("sync;", getCommandManager());
                    } else {
                        final GraphView prevView = getGraphViewForTabId(currentTab);
                        final GraphView nextView = getGraphViewForTabId(tabId);
                        if (prevView != null && nextView != null && prevView != nextView) // copy attributes from one labeled node to next
                        {
                            for (Node v = prevView.getGraph().getFirstNode(); v != null; v = v.getNext()) {
                                if (prevView.getLabel(v) != null) {
                                    for (Node w = nextView.getGraph().getFirstNode(); w != null; w = w.getNext()) {
                                        if (nextView.getLabel(w) != null && prevView.getLabel(v).equals(nextView.getLabel(w))) {
                                            NodeView nw = nextView.getNV(w);
                                            NodeView nv = prevView.getNV(v);
                                            nw.setColor(nv.getColor());
                                            nw.setFont(nv.getFont());
                                            nw.setLabelColor(nv.getLabelColor());
                                            nw.setBackgroundColor(nv.getBackgroundColor());
                                            nw.setLabelBackgroundColor(nv.getLabelBackgroundColor());
                                            nw.setHeight(nv.getHeight());
                                            nw.setWidth(nv.getWidth());
                                            nw.setShape(nv.getShape());
                                            nw.setLineWidth((byte) nv.getLineWidth());
                                            nextView.setSelected(w, prevView.getSelected(v));
                                        }
                                    }
                                }
                            }
                        }
                        if (nextView != null)
                            nextView.fitGraphToWindow();
                    }
                    currentTab = tabId;
                }

                if (frame.isActive() && Formatter.getInstance() != null) {
                    Formatter.getInstance().setViewer(dir, getGraphView());
                }
                // updateView(IDirector.ALL);
            }
        });

        this.setPreferredSize(new Dimension(0, 0));

        clusterAnalysisSearcher = new ClusterAnalysisSearcher(this);
        searchManager = new SearchManager(dir, this, clusterAnalysisSearcher, false, true);

        frame.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                MainViewer.setLastActiveFrame(frame);
                updateView(IDirector.ENABLE_STATE);
                if (Formatter.getInstance() != null) {
                    Formatter.getInstance().setViewer(dir, getGraphView());
                }
                InputDialog inputDialog = InputDialog.getInstance();
                if (inputDialog != null)
                    inputDialog.setViewer(dir, ClusterViewer.this);
            }

            public void windowDeactivated(WindowEvent event) {
                if (tabbedPane.getComponentAt(currentTab) instanceof ITab) {
                    ((ITab) tabbedPane.getComponentAt(currentTab)).deactivate();
                }
                if (getGraphView() instanceof ViewerBase) {
                    Collection<String> selectedLabels = ((ViewerBase) getGraphView()).getSelectedNodeLabels(false);
                    if (selectedLabels.size() != 0) {
                        ProjectManager.getPreviouslySelectedNodeLabels().clear();
                        ProjectManager.getPreviouslySelectedNodeLabels().addAll(selectedLabels);
                    }
                }
            }

            public void windowClosing(WindowEvent e) {
                if (dir.getDocument().getProgressListener() != null)
                    dir.getDocument().getProgressListener().setUserCancelled(true);
                if (MainViewer.getLastActiveFrame() == frame)
                    MainViewer.setLastActiveFrame(null);
            }
        });


        setTitle();

        selectionListener = new SelectionSet.SelectionListener() {
            public void changed(Collection<String> labels, boolean selected) {
                selectByLabel(getTabbedIndex(), labels, selected);
            }
        };
        doc.getSampleSelection().addSampleSelectionListener(selectionListener);

        legendPanel.setPopupMenu(new jloda.gui.PopupMenu(this, megan.chart.gui.GUIConfiguration.getLegendPanelPopupConfiguration(), commandManager));

        frame.setVisible(true);
        splitPane.setDividerLocation(1.0);

        dir.execute("sync;", commandManager);
    }

    /**
     * is viewer uptodate?
     *
     * @return uptodate
     */
    public boolean isUptoDate() {
        return uptodate;
    }

    /**
     * return the frame associated with the viewer
     *
     * @return frame
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     * gets the title
     *
     * @return title
     */
    public String getTitle() {
        return frame.getTitle();
    }

    public void setTitle() {
        String newTitle = dataType + " Cluster Analysis - " + doc.getTitle();

        if (doc.getMeganFile().isMeganServerFile())
            newTitle += " (remote file)";
        if (doc.getMeganFile().isReadOnly())
            newTitle += " (read-only)";
        else if (doc.isDirty())
            newTitle += "*";

        if (dir.getID() == 1)
            newTitle += " - " + ProgramProperties.getProgramVersion();
        else
            newTitle += " - [" + dir.getID() + "] - " + ProgramProperties.getProgramVersion();


        if (!frame.getTitle().equals(newTitle)) {
            frame.setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }

    /**
     * ask view to rescan itself. This method is wrapped into a runnable object
     * and put in the swing event queue to avoid concurrent modifications.
     *
     * @param what what should be updated? Possible values: Director.ALL or Director.TITLE
     */
    public void updateView(final String what) {
        for (String sample : doc.getSampleNames()) {
            String label = doc.getSampleAttributeTable().getSampleShape(sample);
            NodeShape shape;
            if (label == null || label.equalsIgnoreCase("circle"))
                shape = NodeShape.Oval;
            else
                shape = NodeShape.valueOfIgnoreCase(label);
            label2shape.put(sample, shape);
        }
        setFont(ProgramProperties.get(ProgramProperties.DEFAULT_FONT, getFont()));

        final GraphView graphView = getGraphViewForTabId(tabbedPane.getSelectedIndex());

        setStatusLine(ClusterViewer.this);
        getCommandManager().updateEnableState();
        setTitle();

        if (what.equals(IDirector.ALL)) {
            if (graphView != null) {
                final PhyloTree graph = ((PhyloTree) graphView.getGraph());

                group2Nodes.clear();
                if (isPCoATab()) { // setup group 2 nodes in order that samples appear in table
                    Map<String, Node> sample2node = new HashMap<>();
                    for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
                        sample2node.put(graphView.getNV(v).getLabel(), v);
                    }
                    for (String sample : getDir().getDocument().getSampleAttributeTable().getSampleOrder()) {
                        String groupId = getDir().getDocument().getSampleAttributeTable().getGroupId(sample);
                        if (groupId != null) {
                            LinkedList<Node> nodes = group2Nodes.get(groupId);
                            if (nodes == null) {
                                nodes = new LinkedList<>();
                                group2Nodes.put(groupId, nodes);
                                }
                            nodes.add(sample2node.get(sample));
                            }
                        }
                    }

                if (frame.isActive())
                    graphView.requestFocusInWindow();
                final Set<String> selectedLabels = doc.getSampleSelection().getAll();
                final NodeSet toSelect = new NodeSet(graphView.getGraph());
                for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
                    final NodeView nv = graphView.getNV(v);
                    if (nv.getLabel() != null) {
                        nv.setLabelVisible(showLabels);
                        if (nv.getHeight() <= 3)
                            nv.setHeight(nodeRadius);
                        if (nv.getWidth() <= 3)
                            nv.setWidth(nodeRadius);
                        nv.setFixedSize(true);
                        if (useColors) {
                            String sample = graph.getLabel(v);
                            Color color = dir.getDocument().getChartColorManager().getSampleColor(sample);
                            if (nodeRadius > 1 || !showLabels) {
                                nv.setBackgroundColor(color);
                                nv.setLabelBackgroundColor(null);
                            } else
                                nv.setLabelBackgroundColor(color);
                        } else
                            nv.setBackgroundColor(null);

                        if (selectedLabels.contains(nv.getLabel()))
                            toSelect.add(v);
                        }
                    }
                addFormatting(upgmaTab.getGraphView());
                addFormatting(njTab.getGraphView());
                addFormatting(nnetTab.getGraphView());
                if (pcoaTab.isShowGroupsAsEllipses() || pcoaTab.isShowGroupsAsConvexHulls())
                    pcoaTab.computeConvexHullsAndEllipsesForGroups(group2Nodes);
                addFormatting(pcoaTab.getGraphView());
                graphView.setSelected(toSelect, true);
            }
        }

        if (graphView != null)
            graphView.repaint();

        final FindToolBar findToolBar = searchManager.getFindDialogAsToolBar();
        if (findToolBar.isClosing()) {
            showFindToolBar = false;
            findToolBar.setClosing(false);
        }
        if (!findToolBar.isEnabled() && showFindToolBar) {
            mainPanel.add(findToolBar, BorderLayout.NORTH);
            findToolBar.setEnabled(true);
            frame.getContentPane().validate();
            getCommandManager().updateEnableState();
        } else if (findToolBar.isEnabled() && !showFindToolBar) {
            mainPanel.remove(findToolBar);
            findToolBar.setEnabled(false);
            frame.getContentPane().validate();
            getCommandManager().updateEnableState();
        }

        if (tabbedPane.getSelectedComponent() instanceof ITab) {
            try {
                ((ITab) tabbedPane.getSelectedComponent()).updateView(what);
            } catch (Exception e) {
                Basic.caught(e);
                }
            }

        legendPanel.updateView();
        if (doc.getNumberOfSamples() <= 1)
            splitPane.setDividerLocation(1.0);
        legendPanel.repaint();

        // enable applicable tabs
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getComponentAt(i) instanceof ITab) {
                ITab tab = (ITab) tabbedPane.getComponentAt(i);
                tabbedPane.setEnabledAt(i, tab.isApplicable());

                }
            }
    }

    /**
     * update distances
     */
    public void updateDistances() throws Exception {
        taxa = new Taxa();
        java.util.List<String> pids = doc.getSampleNames();
        for (String name : pids) {
            taxa.add(name);
        }

        if (taxa.size() < 4)
            throw new IOException("Too few samples: " + taxa.size());

        distances = new Distances(taxa.size());

        if (ecologicalIndex.equalsIgnoreCase(UniFrac.TOPOLOGICAL_UNIFRAC))
            numberOfNodesUsed = UniFrac.apply(getParentViewer(), UniFrac.TOPOLOGICAL_UNIFRAC, 1, distances);
        else if (ecologicalIndex.equalsIgnoreCase(JensenShannonDivergence.SqrtJensenShannonDivergence))
            numberOfNodesUsed = JensenShannonDivergence.apply(getParentViewer(), JensenShannonDivergence.SqrtJensenShannonDivergence, distances);
        else if (ecologicalIndex.equalsIgnoreCase(PearsonDistance.PEARSON_DISTANCE))
            numberOfNodesUsed = PearsonDistance.apply(doc, getParentViewer(), ecologicalIndex, distances);
        else
            numberOfNodesUsed = CalculateEcologicalIndices.apply(doc, getParentViewer(), ecologicalIndex, distances, !getEcologicalIndex().contains("Goodall"));

        if (distances.replaceNaNByZero()) {
            NotificationsInSwing.showWarning(getFrame(), "Undefined distances detected, replaced by 0");
        }

        getPcoaTab().clear();
        getUpgmaTab().clear();
        getNnetTab().clear();
        getNJTab().clear();
        matrixTab.setData(taxa, distances);
    }

    /**
     * update the graph
     */
    public void updateGraph() throws Exception {
        if (tabbedPane.getSelectedComponent() instanceof ITab) {
            final ITab iTab = (ITab) tabbedPane.getSelectedComponent();
            iTab.compute(taxa, distances);
            clusterAnalysisSearcher.updateMatrixSearcher();
        }
    }

    /**
     * sets the status line
     */
    public void setStatusLine(ClusterViewer clusterViewer) {
        statusBar.setText1("Samples=" + clusterViewer.getDir().getDocument().getNumberOfSamples());

        String text2 = "Data=" + clusterViewer.getDataType();

        text2 += " Matrix=" + clusterViewer.getEcologicalIndex();
        Component pane = getSelectedComponent();
        if (pane instanceof ITab) {
            text2 += " Method=" + ((ITab) pane).getMethod();
        }
        statusBar.setText2(text2);
    }


    /**
     * get the graph viewer for the given id
     *
     * @param tabId
     * @return GraphView or null
     */
    public GraphView getGraphViewForTabId(int tabId) {
        if (tabbedPane.getComponentAt(tabId) instanceof ITab) {
            return ((ITab) tabbedPane.getComponentAt(tabId)).getGraphView();
        } else {
            return null;
        }
    }

    /**
     * ask view to prevent user input
     */
    public void lockUserInput() {
        locked = true;
        tabbedPane.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        statusBar.setText2("Busy...");
        getCommandManager().setEnableCritical(false);
    }

    /**
     * ask view to allow user input
     */
    public void unlockUserInput() {
        locked = false;
        tabbedPane.setEnabled(true);
        setCursor(Cursor.getDefaultCursor());
        getCommandManager().setEnableCritical(true);
        getCommandManager().updateEnableState();
    }

    public boolean isLocked() {
        return locked;
    }

    /**
     * set the cursor for all tabs
     *
     * @param cursor
     */
    public void setCursor(Cursor cursor) {
        super.setCursor(cursor);
        if (getPcoaTab() != null)
            getPcoaTab().getGraphView().setCursor(cursor);
        if (getNJTab() != null)
            getNJTab().getGraphView().setCursor(cursor);
        if (getNnetTab() != null)
            getNnetTab().getGraphView().setCursor(cursor);
        if (getUpgmaTab() != null)
            getUpgmaTab().getGraphView().setCursor(cursor);
        if (getMatrixTab() != null)
            getMatrixTab().setCursor(cursor);
    }

    /**
     * ask view to destroy itself
     */
    public void destroyView() throws CanceledException {
        searchManager.getFindDialogAsToolBar().close();
        frame.setVisible(false);
        doc.getSampleSelection().removeSampleSelectionListener(selectionListener);
        MeganProperties.removePropertiesListListener(menuBar.getRecentFilesListener());
        dir.removeViewer(this);
        frame.dispose();
    }

    /**
     * set uptodate state
     *
     * @param flag
     */
    public void setUptoDate(boolean flag) {
        uptodate = flag;
    }

    public Director getDir() {
        return dir;
    }

    public String getDataType() {
        return dataType;
    }

    public String getEcologicalIndex() {
        return ecologicalIndex;
    }

    /**
     * add node and edge formatting
     *
     * @param graphView
     */
    public void addFormatting(GraphView graphView) {
        try {
            final PhyloTree graph = ((PhyloTree) graphView.getGraph());
            for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
                final String sample = graph.getLabel(v);
                final NodeView nv = graphView.getNV(v);
                boolean showThisLabel = showLabels;
                if (!showThisLabel && !pcoaTab.isSampleNode(v))
                    showThisLabel = true;

                if (sample != null) {
                    nv.setLabelVisible(showThisLabel);
                    if (nv.getHeight() <= 3)
                        nv.setHeight(nodeRadius);
                    if (nv.getWidth() <= 3)
                        nv.setWidth(nodeRadius);
                    nv.setFixedSize(true);

                    NodeShape shape = label2shape.get(sample);
                    if (shape != null) {
                        nv.setNodeShape(shape);
                    }
                    graphView.setLabel(v, doc.getSampleLabelGetter().getLabel(sample));
                    if (useColors) {
                        Color color = dir.getDocument().getChartColorManager().getSampleColor(sample);
                        if (nodeRadius > 1 || !showThisLabel) {
                            nv.setBackgroundColor(color);
                            nv.setLabelBackgroundColor(null);
                        } else
                            nv.setLabelBackgroundColor(color);
                    } else
                        nv.setBackgroundColor(null);

                } else {
                    nv.setNodeShape(NodeShape.None);
                }

            }
            for (Edge e = graph.getFirstEdge(); e != null; e = e.getNext()) {
                if (graph.getInfo(e) != null && (Byte) graph.getInfo(e) == EdgeView.DIRECTED)
                    graphView.setDirection(e, EdgeView.DIRECTED);
                else
                    graphView.setDirection(e, EdgeView.UNDIRECTED);
            }
        } catch (Exception e) {
            Basic.caught(e);
        }
    }

    public void setEcologicalIndex(String ecologicalIndex) {
        this.ecologicalIndex = ecologicalIndex;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    /**
     * gets the index of the showing tab
     *
     * @return index
     */
    public int getTabbedIndex() {
        return tabbedPane.getSelectedIndex();
    }

    public boolean isPCoATab() {
        return tabbedPane.getSelectedComponent() instanceof ITab && ((ITab) tabbedPane.getSelectedComponent()).getLabel().contains("PCoA");
    }

    public void selectAll(boolean select) {
        GraphView graphView = getGraphViewForTabId(getTabbedIndex());
        if (graphView != null) {
            graphView.selectAllNodes(select);
            graphView.selectAllEdges(select);
            graphView.repaint();

        } else {
            if (select)
                matrixTab.getTable().selectAll();
            else
                matrixTab.getTable().clearSelection();
        }
    }

    public void selectByLabel(int tabbedIndex, Collection<String> labels, boolean select) {
        GraphView graphView = getGraphViewForTabId(tabbedIndex);
        if (graphView != null) {
            PhyloTree graph = ((PhyloTree) graphView.getGraph());
            NodeSet nodes = new NodeSet(graph);
            for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
                String label = graph.getLabel(v);
                if (label != null && labels.contains(label) && graphView.getSelected(v) != select)
                    nodes.add(v);
            }
            graphView.setSelected(nodes, select);
        } else if (tabbedIndex == MATRIX_TAB_INDEX) {
            matrixTab.getTable().selectAll();
        }
    }

    public void selectInverted() {
        GraphView graphView = getGraphViewForTabId(getTabbedIndex());
        if (graphView != null) {
            graphView.invertNodeSelection();
            if (graphView.getSelectedEdges().size() > 0)
                graphView.invertEdgeSelection();

        } else {
            matrixTab.getTable().selectAll();
        }
    }

    public PCoATab getPcoaTab() {
        return pcoaTab;
    }

    public TreeTabBase getUpgmaTab() {
        return upgmaTab;
    }

    public TreeTabBase getNJTab() {
        return njTab;
    }

    public TreeTabBase getNnetTab() {
        return nnetTab;
    }

    public MatrixTab getMatrixTab() {
        return matrixTab;
    }

    public Taxa getTaxa() {
        return taxa;
    }

    public Distances getDistances() {
        return distances;
    }

    public int getNumberOfNodesUsed() {
        return numberOfNodesUsed;
    }

    public Component getSelectedComponent() {
        if (tabbedPane != null)
            return tabbedPane.getSelectedComponent();
        else
            return null;
    }

    public void selectComponent(Component component) {
        tabbedPane.setSelectedComponent(component);
    }

    /**
     * gets the current graphview
     *
     * @return graphview
     */
    public GraphView getGraphView() {
        if (tabbedPane.getSelectedComponent() instanceof ITab)
            return ((ITab) tabbedPane.getSelectedComponent()).getGraphView();
        else
            return null;
    }

    /**
     * gets the current data viewer (MainViewer, SeedViewer, KeggViewer)
     *
     * @return graphview
     */
    public ViewerBase getParentViewer() {
        return parentViewer;
    }

    /**
     * Print the graph associated with this viewer.
     *
     * @param gc0        the graphics context.
     * @param format     page format
     * @param pagenumber page index
     */
    public int print(Graphics gc0, PageFormat format, int pagenumber) throws PrinterException {
        JPanel panel = getPanel();

        if (panel != null && pagenumber == 0) {
            if (panel instanceof GraphView) {
                return ((GraphView) panel).print(gc0, format, pagenumber);
            } else {
                Graphics2D gc = ((Graphics2D) gc0);
                Dimension dim = panel.getSize();
                int image_w = dim.width;
                int image_h = dim.height;

                double paper_x = format.getImageableX() + 1;
                double paper_y = format.getImageableY() + 1;
                double paper_w = format.getImageableWidth() - 2;
                double paper_h = format.getImageableHeight() - 2;

                double scale_x = paper_w / image_w;
                double scale_y = paper_h / image_h;
                double scale = (scale_x <= scale_y) ? scale_x : scale_y;

                double shift_x = paper_x + (paper_w - scale * image_w) / 2.0;
                double shift_y = paper_y + (paper_h - scale * image_h) / 2.0;

                gc.translate(shift_x, shift_y);
                gc.scale(scale, scale);

                panel.print(gc0);
                return Printable.PAGE_EXISTS;
            }
        }
        return Printable.NO_SUCH_PAGE;
    }

    public JScrollPane getSelectedScrollPane() {
        Component tab = tabbedPane.getSelectedComponent();
        if (tab instanceof TreeTabBase) {
            return ((TreeTabBase) tab).getGraphView().getScrollPane();
        }
        if (tab instanceof PCoATab) {
            return ((PCoATab) tab).getGraphView().getScrollPane();
        }
        if (tab instanceof MatrixTab) {
            return ((MatrixTab) tab).getScrollPane();
        }
        return null;
    }

    public JPanel getPanel() {
        Component tab = tabbedPane.getSelectedComponent();
        if (tab instanceof TreeTabBase) {
            return ((TreeTabBase) tab).getGraphView();
        }
        if (tab instanceof PCoATab) {
            return ((PCoATab) tab).getGraphView();
        }
        if (tab instanceof MatrixTab) {
            return ((MatrixTab) tab);
        }
        return (JPanel) tab;
    }

    public boolean isShowFindToolBar() {
        return showFindToolBar;
    }

    public void setShowFindToolBar(boolean showFindToolBar) {
        this.showFindToolBar = showFindToolBar;
    }

    public SearchManager getSearchManager() {
        return searchManager;
    }


    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return dataType.toUpperCase() + "ClusterViewer";
    }

    public boolean isUseColors() {
        return useColors;
    }

    public void setUseColors(boolean useColors) {
        this.useColors = useColors;
    }

    public boolean isShowLabels() {
        return showLabels;
    }

    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    public int getNodeRadius() {
        return nodeRadius;
    }

    public void setNodeRadius(int nodeRadius) {
        this.nodeRadius = nodeRadius;
    }

    public StatusBar getStatusBar() {
        return statusBar;
    }

    /**
     * show the legend horizontal, vertical or none
     *
     * @param showLegend
     */
    public void setShowLegend(String showLegend) {
        this.showLegend = showLegend;
        if (showLegend.equalsIgnoreCase("horizontal")) {
            splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            Dimension size = new Dimension();
            splitPane.validate();
            legendPanel.setSize(splitPane.getWidth(), 50);
            legendPanel.draw((Graphics2D) legendPanel.getGraphics(), size);
            int height = (int) size.getHeight() + 10;
            legendPanel.setPreferredSize(new Dimension(splitPane.getWidth(), height));
            legendPanel.validate();
            splitPane.setDividerLocation(splitPane.getSize().height - splitPane.getInsets().right - splitPane.getDividerSize() - height);
        } else if (showLegend.equalsIgnoreCase("vertical")) {
            splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
            Dimension size = new Dimension();
            splitPane.validate();
            legendPanel.setSize(20, splitPane.getHeight());
            legendPanel.draw((Graphics2D) legendPanel.getGraphics(), size);
            int width = (int) size.getWidth() + 5;
            legendPanel.setPreferredSize(new Dimension(width, splitPane.getHeight()));
            legendPanel.validate();
            splitPane.setDividerLocation(splitPane.getSize().width - splitPane.getInsets().right - splitPane.getDividerSize() - width);
        } else {
            splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            splitPane.setDividerLocation(1.0);
        }
    }

    public Document getDocument() {
        return dir.getDocument();
    }

    public String getShowLegend() {
        return showLegend;
    }

    public LegendPanel getLegendPanel() {
        return legendPanel;
    }

    public JScrollPane getLegendScrollPane() {
        return legendScrollPane;
    }

    public Map<String, LinkedList<Node>> getGroup2Nodes() {
        return group2Nodes;
    }

    /**
     * add a tab at the indicated position
     *
     * @param index
     * @param tab   must be instance of JPanel
     */
    public void addTab(int index, ITab tab) {
        JPanel panel = (JPanel) tab;
        tabbedPane.insertTab(tab.getLabel(), null, panel, tab.getLabel(), index);
        if (PCoA_TAB_INDEX >= index)
            PCoA_TAB_INDEX++;
        if (NJ_TAB_INDEX >= index)
            NJ_TAB_INDEX++;
        if (NNET_TAB_INDEX >= index)
            NNET_TAB_INDEX++;
        if (UPGMA_TAB_INDEX >= index)
            UPGMA_TAB_INDEX++;
        if (MATRIX_TAB_INDEX >= index)
            MATRIX_TAB_INDEX++;
    }

    /**
     * gets the number of selected taxon or similar ids
     *
     * @return set of selected ids
     */
    public Set<Integer> getSelectedClassIds() {
        Set<Integer> set = new HashSet<>();

        Classification classification = ClassificationManager.get(parentViewer.getClassName(), false);

        if (getTabbedIndex() == PCoA_TAB_INDEX) {
            PCoATab tab = getPcoaTab();
            GraphView graphView = getGraphView();
            for (Node v : graphView.getSelectedNodes()) {
                if (tab.isBiplotNode(v)) {
                    Integer id = classification.getName2IdMap().get(graphView.getLabel(v));
                    if (id != 0)
                        set.add(id);
                }
            }
        }
        return set;
    }

    /**
     * is the currently selected tab a Swing panel? Needed for export image dialog
     *
     * @return true. if swing panel
     */
    public boolean isSwingPanel() {
        return getSelectedComponent() == getPcoaTab() || getSelectedComponent() == getPcoaTab() || getSelectedComponent() == getNJTab() ||
                getSelectedComponent() == getNnetTab() || getSelectedComponent() == getUpgmaTab();
    }
}
