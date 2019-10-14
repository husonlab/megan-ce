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
package megan.viewer;

import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.swing.commands.CommandManager;
import jloda.swing.director.*;
import jloda.swing.export.ExportManager;
import jloda.swing.find.FindToolBar;
import jloda.swing.find.SearchManager;
import jloda.swing.format.Formatter;
import jloda.swing.graphview.*;
import jloda.swing.util.PopupMenu;
import jloda.swing.util.*;
import jloda.swing.window.MenuBar;
import jloda.util.*;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.data.SyncDataTableAndClassificationViewer;
import megan.core.Director;
import megan.core.Document;
import megan.dialogs.compare.Comparer;
import megan.dialogs.input.InputDialog;
import megan.main.MeganProperties;
import megan.viewer.gui.NodeDrawer;
import megan.viewer.gui.ViewerJTable;
import megan.viewer.gui.ViewerJTree;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.*;

/**
 * Classification viewer
 *
 * @author Daniel Huson, 4.2015
 */
public class ClassificationViewer extends ViewerBase implements IDirectableViewer, IViewerWithFindToolBar, IViewerWithLegend, IUsesHeatMapColors {
    static public final int XSTEP = 50;
    static public final int YSTEP = 50;
    private static final int HLEAFBOX = 200; // horizontal length of leaf box, needed for picking

    private final JFrame frame;
    private final StatusBar statusBar;

    private final Map<Integer, NodeData> id2NodeData = new HashMap<>();
    private final Map<Integer, Set<Node>> id2Nodes = new HashMap<>();
    private final Map<Integer, Integer> id2rank;
    private final Map<Integer, String> id2toolTip;

    private final JSplitPane mainSplitPane;

    private final ViewerJTable viewerJTable;
    private final ViewerJTree viewerJTree;

    private boolean showFindToolBar = false;
    private final SearchManager searchManager;

    private boolean uptodate = true;
    private long lastRecomputeTimeFromDocument = -1;

    private int totalAssignedReads = 0;

    private final NodeSet nodesWithMovedLabels; // track node labels that have been moved
    private final NodeArray<Rectangle> node2BoundingBox;

    // some one-time calculations
    private boolean doScrollToRight = false;

    private boolean drawOnScreen = true;
    private boolean mustDrawTwice = false;

    private final MenuBar menuBar;

    private boolean avoidSelectionBounce = false;

    Classification classification;

    private boolean useReadWeights = false;

    /**
     * Classification viewer
     *
     * @param dir     the director
     * @param visible
     * @throws Exception
     */
    public ClassificationViewer(final Director dir, final Classification classification, boolean visible) throws Exception {
        super(dir, new PhyloTree(), false);
        this.classification = classification;

        setDefaultEdgeDirection(EdgeView.UNDIRECTED);
        setDefaultEdgeColor(Color.BLACK);
        setDefaultNodeColor(Color.BLACK);
        setDefaultNodeBackgroundColor(Color.WHITE);

        try {
            drawerType = DiagramType.valueOf(ProgramProperties.get("DrawerType" + getClassName(), DiagramType.RoundedCladogram.toString()));
        } catch (Exception e) {
            drawerType = DiagramType.RoundedCladogram;
        }

        id2rank = classification.getId2Rank();
        id2toolTip = classification.getId2ToolTip();

        setGraphDrawer(new DefaultGraphDrawer(this));
        getGraphDrawer().setNodeDrawer(nodeDrawer);
        getNodeDrawer().setStyle(doc.getDataTable().getNodeStyle(getClassName()), this instanceof MainViewer ? NodeDrawer.Style.Circle : dir.getMainViewer().getNodeDrawer().getStyle());
        getNodeDrawer().setScaleBy(getClassName().equals(Classification.Taxonomy) || ProgramProperties.get(getClassName() + "UseLCA", false) ? NodeDrawer.ScaleBy.Assigned : NodeDrawer.ScaleBy.Summarized);

        this.commandManager = new CommandManager(dir, this, new String[]{"megan.commands", "megan.viewer.commands"}, !ProgramProperties.isUseGUI());

        node2BoundingBox = new NodeArray<>(getTree());
        nodesWithMovedLabels = new NodeSet(getGraph());

        setCanvasColor(Color.WHITE);
        setAllowMoveNodes(false);
        setAllowMoveInternalEdgePoints(false);
        setFixedNodeSize(false);
        setAutoLayoutLabels(true);
        setAllowEdit(false);
        trans.setLockXYScale(false);
        trans.setTopMargin(100);
        trans.setLeftMargin(150);
        trans.setRightMargin(250);
        setAutoLayoutLabels(false);

        setPreferredSize(new Dimension(0, 0));

        super.getScrollPane().getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        super.getScrollPane().setWheelScrollingEnabled(true);
        super.getScrollPane().setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        setGraphViewListener(new MyGraphViewListener(this, node2BoundingBox, nodesWithMovedLabels));
        setPOWEREDBY(null);

        trans.removeAllChangeListeners();

        trans.addChangeListener(trans -> {
            final Dimension ps = trans.getPreferredSize();
            int x = Math.max(ps.width, ClassificationViewer.super.getScrollPane().getWidth() - 20);
            int y = Math.max(ps.height, ClassificationViewer.super.getScrollPane().getHeight() - 20);
            ps.setSize(x, y);
            setPreferredSize(ps);
            ClassificationViewer.super.getScrollPane().getViewport().setViewSize(new Dimension(x, y));
            repaint();
        });

        statusBar = new StatusBar();

        frame = new JFrame();
        frame.setIconImages(ProgramProperties.getProgramIconImages());

        this.menuBar = new MenuBar(this, GUIConfiguration.getMenuConfiguration(), getCommandManager());
        getFrame().setJMenuBar(menuBar);
        MeganProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);
        if (!(this instanceof MainViewer))
            ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());

        setWindowTitle();
        viewerJTable = new ViewerJTable(this);
        viewerJTree = new ViewerJTree(this);
        mainPanel.add(getScrollPane());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Overview", new JScrollPane(viewerJTree));
        tabbedPane.add("Heatmap", new JScrollPane(viewerJTable));

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabbedPane, splitPane);
        mainSplitPane.setDividerLocation(250);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setEnabled(true);

        getFrame().getContentPane().setLayout(new BorderLayout());

        JToolBar toolBar = new ToolBar(this, GUIConfiguration.getToolBarConfiguration(), getCommandManager());
        getFrame().getContentPane().add(toolBar, BorderLayout.NORTH);
        getFrame().getContentPane().add(mainSplitPane, BorderLayout.CENTER);
        getFrame().getContentPane().add(statusBar, BorderLayout.SOUTH);

        searchManager = new SearchManager(dir, this, new ClassificationViewerSearcher(getFrame(), getClassName(), this), false, true);

        getFrame().pack();
        getFrame().setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        final int[] geometry = ProgramProperties.get(ClassificationManager.getWindowGeometryKey(getClassName()), new int[]{100, 100, 800, 600});
        if (getClassName().equals(Classification.Taxonomy))
            getFrame().setLocation(geometry[0] + (ProjectManager.getNumberOfProjects() > 0 ? 20 : 0), geometry[1] + (ProjectManager.getNumberOfProjects() > 0 ? 20 : 0));
        else
            getFrame().setLocationRelativeTo(MainViewer.getLastActiveFrame());
        getFrame().setSize(geometry[2], geometry[3]);

        // add window listeners
        addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
                componentResized(e);
            }

            public void componentResized(ComponentEvent event) {
                if ((event.getID() == ComponentEvent.COMPONENT_RESIZED || event.getID() == ComponentEvent.COMPONENT_MOVED) &&
                        (getFrame().getExtendedState() & JFrame.MAXIMIZED_HORIZ) == 0
                        && (getFrame().getExtendedState() & JFrame.MAXIMIZED_VERT) == 0) {
                    ProgramProperties.put(ClassificationManager.getWindowGeometryKey(getClassName()), new int[]{getFrame().getLocation().x, getFrame().getLocation().y, getFrame().getSize().width,
                            getFrame().getSize().height}
                    );
                }
            }
        });

        getFrame().addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent event) {
                //System.err.println(getTitle()+" activiated");
                //Basic.caught(new Exception());

                MainViewer.setLastActiveFrame(getFrame());
                if (Formatter.getInstance() != null) {
                    Formatter.getInstance().setViewer(dir, ClassificationViewer.this);
                }

                InputDialog inputDialog = InputDialog.getInstance();
                if (inputDialog != null)
                    inputDialog.setViewer(dir, ClassificationViewer.this);
                //ClassificationViewer.this.requestFocusInWindow();
            }

            public void windowDeactivated(WindowEvent event) {
                Collection<String> selectedLabels = getSelectedNodeLabels(false);
                if (selectedLabels.size() != 0) {
                    ProjectManager.getPreviouslySelectedNodeLabels().clear();
                    ProjectManager.getPreviouslySelectedNodeLabels().addAll(selectedLabels);
                }
            }

            public void windowClosing(WindowEvent e) {
                if (dir.getDocument().getProgressListener() != null)
                    dir.getDocument().getProgressListener().setUserCancelled(true);
                if (MainViewer.getLastActiveFrame() == getFrame())
                    MainViewer.setLastActiveFrame(null);
            }
        });

        this.addNodeActionListener(new NodeActionAdapter() {
            public void doSelect(NodeSet nodes) {
                if (!avoidSelectionBounce) {
                    avoidSelectionBounce = true;
                    Set<Integer> ids = getAllIds(nodes);
                    if (ids.size() > 0) {
                        viewerJTree.setSelected(ids, true);
                        viewerJTable.setSelected(ids, true);
                    }
                    if (!ClassificationViewer.this.isLocked())
                        getCommandManager().updateEnableState();

                    updateStatusBarTooltip();
                    avoidSelectionBounce = false;
                }
            }

            public void doDeselect(NodeSet nodes) {
                if (!avoidSelectionBounce) {
                    avoidSelectionBounce = true;
                    Set<Integer> ids = getAllIds(nodes);
                    if (ids.size() > 0) {
                        viewerJTree.setSelected(ids, false);
                        viewerJTable.setSelected(ids, false);
                    }
                    if (!ClassificationViewer.this.isLocked())
                        getCommandManager().updateEnableState();
                    updateStatusBarTooltip();
                    avoidSelectionBounce = false;
                }
            }

            // double click: select subtree
            public void doClick(NodeSet nodes, int clicks) {
                if (clicks == 2) {
                    selectedNodes.addAll(nodes);
                    selectSubTreeNodes();
                    fireDoSelect(selectedNodes);
                    ClassificationViewer.this.repaint();
                }
            }

            // double click label: inspect node
            public void doClickLabel(NodeSet nodes, int clicks) {
            }
        });

        super.getScrollPane().addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                trans.fireHasChanged();
            }
        });

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                getScrollPane().requestFocusInWindow();
            }
        });

        // at start up, collapse to subsystems:
        for (Edge e = classification.getFullTree().getRoot().getFirstOutEdge(); e != null; e = classification.getFullTree().getRoot().getNextOutEdge(e)) {
            getCollapsedIds().add((Integer) e.getTarget().getInfo());
        }

        setPopupListener(new GraphViewPopupListener(this, GUIConfiguration.getNodePopupConfiguration(),
                GUIConfiguration.getEdgePopupConfiguration(),
                GUIConfiguration.getPanelPopupConfiguration(), commandManager));

        SyncDataTableAndClassificationViewer.syncCollapsedFromSummary2Viewer(doc.getDataTable(), this);

        legendPanel.setPopupMenu(new PopupMenu(this, megan.chart.gui.GUIConfiguration.getLegendPanelPopupConfiguration(), commandManager));

        setupKeyListener();
        splitPane.setDividerLocation(1.0);
        getFrame().setVisible(visible);
    }

    private Thread thread = null;

    /**
     * display number of selected nodes etc as tooltip on status bar.
     * Waits a while before doing so, because there may be multiple updates of the selection state
     */
    private void updateStatusBarTooltip() {
        SwingUtilities.invokeLater(() -> {
            if (getSelectedNodes().size() > 0) {
                double numberOfAssigned = 0;
                double numberOfSummarized = 0;
                for (Node v : getSelectedNodes()) {
                    if (v.getData() instanceof NodeData) {
                        NodeData nodeData = (NodeData) v.getData();
                        numberOfAssigned += nodeData.getCountAssigned();
                        numberOfSummarized += nodeData.getCountSummarized();
                    }
                }

                final String line = (String.format("Selected nodes: %,d, total assigned: %,d, total summarized: %,d", getSelectedNodes().size(),
                        Math.round(numberOfAssigned), Math.round(numberOfSummarized)));
                //System.err.println(line);
                statusBar.setToolTipText(line);
            } else
                statusBar.setToolTipText(null);
            thread = null;
        });
    }

    /**
     * gets the summarized array for a fid
     *
     * @param fId
     * @return summarized
     */
    public float[] getSummarized(int fId) {
        Node v = getANode(fId);
        return getNodeData(v).getSummarized();
    }

    /**
     * ask view to rescan itself. This is method is wrapped into a runnable
     * object and put in the swing event queue to avoid concurrent
     * modifications.
     *
     * @param what what should be updated? Possible values: Director.ALL or
     *             Director.TITLE
     */
    public void updateView(final String what) {
        if (what.equals(Director.ALL)) {
            try {
                // rescan colors
                final String[] sampleNames = doc.getSampleNamesAsArray();
                for (int i = 0; i < sampleNames.length; i++) {
                    doc.getColorsArray()[i] = doc.getChartColorManager().getSampleColor(sampleNames[i]);
                }

                setFont(ProgramProperties.get(ProgramProperties.DEFAULT_FONT, getFont()));

                if (lastRecomputeTimeFromDocument != dir.getDocument().getLastRecomputeTime()) {
                    lastRecomputeTimeFromDocument = dir.getDocument().getLastRecomputeTime();
                    // System.err.println("RECOMPUTING at "+
                    // lastRecomputeTimeFromDocument);
                    updateData();
                    updateTree();
                }
                if (hasSyncedFormatFromSummaryToViewer)
                    SyncDataTableAndClassificationViewer.syncFormattingFromViewer2Summary(this, doc.getDataTable());
            } catch (IOException e) {
                Basic.caught(e);
            }

            // scroll back to previous nodes of interest
            if (getPreviousNodeIdsOfInterest() != null) {
                for (Integer id : getPreviousNodeIdsOfInterest()) {
                    Node v = getANode(id);
                    if (v != null) {
                        setSelected(v, true);
                    }
                }
                // trans.setScaleY(1);
                zoomToSelection();
                setPreviousNodeIdsOfInterest(null);
            }
            setupNodeLabels(true);
            repaint();
        }

        final FindToolBar findToolBar = searchManager.getFindDialogAsToolBar();
        if (findToolBar.isClosing()) {
            showFindToolBar = false;
            findToolBar.setClosing(false);
        }
        if (!findToolBar.isEnabled() && showFindToolBar) {
            mainPanel.add(findToolBar, BorderLayout.NORTH);
            findToolBar.setEnabled(true);
            getFrame().getContentPane().validate();
        } else if (findToolBar.isEnabled() && !showFindToolBar) {
            mainPanel.remove(findToolBar);
            findToolBar.setEnabled(false);
            getFrame().getContentPane().validate();
        }
        getCommandManager().updateEnableState();
        if (findToolBar.isEnabled())
            findToolBar.clearMessage();

        if (showLegend.equals("undefined") && doc.getNumberOfSamples() > 0 && doc.getNumberOfSamples() < 100) {
            setShowLegend(doc.getNumberOfSamples() <= 1 ? "none" : "horizontal");
        }
        legendPanel.setStyle(getNodeDrawer().getStyle());
        legendPanel.updateView();
        if (doc.getNumberOfSamples() <= 1)
            splitPane.setDividerLocation(1.0);
        legendPanel.repaint();
        setWindowTitle();
        //requestFocusInWindow();
    }

    /**
     * rescan the data in the FViewer
     *
     * @throws java.io.IOException
     */
    private void updateData() throws IOException {
        ProgressListener progress = doc.getProgressListener();
        boolean saveCancelable = false;

        if (progress != null) {
            saveCancelable = progress.isCancelable();
            progress.setSubtask("updating viewer");
            progress.setCancelable(false);
            try {
                progress.setProgress(-1);
            } catch (CanceledException ignored) {
            }
        }

        totalAssignedReads = 0;

        classification.getFullTree().computeId2Data(doc.getNumberOfSamples(), doc.getDataTable().getClass2Counts(getClassName()), id2NodeData);
        for (Integer fId : id2NodeData.keySet()) {
            if (fId > 0) {
                totalAssignedReads += id2NodeData.get(fId).getCountAssigned();
                //System.err.println(classification.getName2IdMap().get(fId)+": "+id2NodeData.get(fId).getCountAssigned());
            }
        }

        if (progress != null)
            progress.setCancelable(saveCancelable);
        getCommandManager().updateEnableState();
    }

    private boolean hasSyncedFormatFromSummaryToViewer = false;

    /**
     * Constructs and updates the tree
     */
    public void updateTree() {
        final PhyloTree tree = getTree();

        if (hasSyncedFormatFromSummaryToViewer)
            SyncDataTableAndClassificationViewer.syncFormattingFromViewer2Summary(this, doc.getDataTable());

        classification.getFullTree().extractInducedTree(id2NodeData, getCollapsedIds(), tree, id2Nodes);
        nodeDrawer.setCounts(determineMaxAssigned());

        if (tree.getRoot() != null) {
            embedTree(tree.getRoot());
        }
        nodesWithMovedLabels.clear();

        trans.setCoordinateRect(getBBox());
        Rectangle rect = new Rectangle();
        trans.w2d(getBBox(), rect);
        setPreferredSize(rect.getSize());

        if (tree.getRoot() != null) {
            for (Edge e = tree.getRoot().getFirstOutEdge(); e != null; e = tree.getRoot().getNextOutEdge(e)) {
                Node v = e.getTarget();
                int id = (Integer) v.getInfo();
                if (id == -1 || id == -2 || id == -3 || id == -6) {
                    setColor(e, Color.LIGHT_GRAY);
                    setColor(v, Color.LIGHT_GRAY);
                    setLabelColor(v, Color.LIGHT_GRAY);
                }
            }
            /*
            for (Node v = tree.getFirstNode(); v != null; v = v.getNext()) {
                setLabel(v, tree.getLabel(v));
                // make sure that there aren't any black nodes!
                if (nodeDrawer.getStyle() == NodeDrawer.Style.Circle && getBackgroundColor(v).equals(Color.BLACK))
                    setBackgroundColor(v, Color.WHITE);
                if ((Integer) v.getInfo() >= -3 && (Integer) v.getInfo() <= -1) {
                    setColor(v, Color.LIGHT_GRAY);
                    setLabelColor(v, Color.LIGHT_GRAY);
                    if (v.getInDegree() > 0) {
                        setColor(v.getFirstInEdge(), Color.LIGHT_GRAY);
                    }
                } else if (v.getInDegree() > 0)
                    setColor(v.getFirstInEdge(), Color.BLACK);
            }
            */

            setColor(getTree().getRoot(), Color.LIGHT_GRAY);
            setLabelColor(getTree().getRoot(), Color.LIGHT_GRAY);

            for (Edge e = tree.getRoot().getFirstOutEdge(); e != null; e = tree.getRoot().getNextOutEdge(e))
                setColor(e, Color.LIGHT_GRAY);
            if (getNumberOfDatasets() > 1 && nodeDrawer.getStyle() == NodeDrawer.Style.Circle) {
                legendPanel.setStyle(getNodeDrawer().getStyle());
            }
            setupNodeLabels(false);
        }

        fitGraphToWindow();

        if (!hasSyncedFormatFromSummaryToViewer) {
            SyncDataTableAndClassificationViewer.syncFormattingFromSummary2Viewer(doc.getDataTable(), this);
            hasSyncedFormatFromSummaryToViewer = true;
        }
        repaint();

        updateStatusBar();
        setPOWEREDBY(getMajorityRankOfLeaves());

        viewerJTable.update();
        viewerJTree.update();
    }


    /**
     * get the majority rank of leaves or null
     *
     * @return name of majority rank or null
     */
    private String getMajorityRankOfLeaves() {
        if (classification.getId2Rank().size() > 0) {
            int[] rank2count = new int[Byte.MAX_VALUE + 1];
            int count = 0;
            for (Node v = getTree().getFirstNode(); v != null; v = v.getNext()) {
                if (v.getOutDegree() == 0) {
                    Integer rank = classification.getId2Rank().get(v.getInfo());
                    if (rank != null && rank > 0)
                        rank2count[rank]++;
                    count++;
                }
            }

            for (int i = 0; i <= Byte.MAX_VALUE; i++) {
                if (rank2count[(byte) i] > count / 2) {
                    return TaxonomicLevels.getName((byte) i);
                }
            }
        }
        return null;
    }

    /**
     * sets the title of the window
     */
    private void setWindowTitle() {
        String newTitle;
        if (getClassName().equals(Classification.Taxonomy))
            newTitle = dir.getDocument().getTitle();
        else
            newTitle = getClassName() + " Viewer - " + dir.getDocument().getTitle();

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

        if (!getFrame().getTitle().equals(newTitle)) {
            getFrame().setTitle(newTitle);
            ProjectManager.updateWindowMenus();
        }
    }

    /**
     * gets the getFrame()
     *
     * @return
     */
    public JFrame getFrame() {
        // Basic.caught(new Exception());
        return frame;
    }

    /*
     * (non-Javadoc)
     *
     * @see jloda.director.IDirectableViewer#isUptoDate()
     */

    public boolean isUptoDate() {
        return uptodate;
    }

    /*
     * (non-Javadoc)
     *
     * @see jloda.director.IDirectorListener#destroyView()
     */

    public void destroyView() throws CanceledException {
        ProgramProperties.put(ClassificationManager.getWindowGeometryKey(getClassName()), new int[]{
                getFrame().getLocation().x, getFrame().getLocation().y, getFrame().getSize().width, getFrame().getSize().height});
        SyncDataTableAndClassificationViewer.syncFormattingFromViewer2Summary(this, doc.getDataTable());

        searchManager.getFindDialogAsToolBar().close();

        getFrame().setVisible(false);
        MeganProperties.removePropertiesListListener(menuBar.getRecentFilesListener());
        if (MainViewer.getLastActiveFrame() == this.getFrame())
            MainViewer.setLastActiveFrame(null);
        dir.removeViewer(this);
        getFrame().dispose();
    }


    /**
     * gets the window menu
     *
     * @return window menu
     */
    public JMenu getWindowMenu() {
        return menuBar.getWindowMenu();
    }

    /*
     * (non-Javadoc)
     *
     * @see jloda.director.IDirectorListener#lockUserInput()
     */

    public void lockUserInput() {
        locked = true;
        statusBar.setText1("");
        statusBar.setText2("Busy...");
        getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getScrollPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getLegendPanel().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        getCommandManager().setEnableCritical(false);
        searchManager.getFindDialogAsToolBar().setEnableCritical(false);
        menuBar.setEnableRecentFileMenuItems(false);
    }

    /*
     * (non-Javadoc)
     *
     * @see jloda.director.IDirectorListener#setUptoDate(boolean)
     */

    public void setUptoDate(final boolean flag) {
        uptodate = flag;
    }

    /*
     * (non-Javadoc)
     *
     * @see jloda.director.IDirectorListener#unlockUserInput()
     */

    public void unlockUserInput() {
        locked = false;
        getCommandManager().setEnableCritical(true);
        searchManager.getFindDialogAsToolBar().setEnableCritical(true);
        getFrame().setCursor(Cursor.getDefaultCursor());
        getScrollPane().setCursor(Cursor.getDefaultCursor());
        getLegendPanel().setCursor(Cursor.getDefaultCursor());
        menuBar.setEnableRecentFileMenuItems(true);
        updateStatusBar();
    }

    /**
     * rescan the status bar
     */
    void updateStatusBar() {
        statusBar.setText1("Terms=" + getTree().getNumberOfNodes());
        final long totalReads = doc.getNumberOfReads();
        final StringBuilder buf2 = new StringBuilder();

        if (doc.getNumberOfSamples() > 1) {
            Comparer.COMPARISON_MODE mode = Comparer.parseMode(doc.getDataTable().getParameters());
            if (mode.equals(Comparer.COMPARISON_MODE.RELATIVE)) {
                buf2.append(String.format("Relative comparison, Assigned=%,d (normalized to %,d per sample)", totalReads, Comparer.parseNormalizedTo(doc.getDataTable().getParameters())));
            } else
                buf2.append(String.format("Absolute comparison, Reads=%,d Assigned=%,d", totalReads, totalAssignedReads));
        } else if (totalReads > 0) {
            buf2.append(String.format("Reads=%,d Assigned=%,d", totalReads, totalAssignedReads));
            if (doc.getReadAssignmentMode() == Document.ReadAssignmentMode.readMagnitude)
                buf2.append(String.format(" (%s)", doc.getReadAssignmentMode().toString()));

            if (doc.getBlastMode() != BlastMode.Unknown)
                buf2.append(" mode=").append(doc.getBlastMode().toString());
        } else {
            if (getTree().getNumberOfNodes() > 0)
                buf2.append(String.format(" total terms=%,d", getTree().getNumberOfNodes()));
        }
        if (Document.getVersionInfo().get(getClassName() + " tree") != null)
            buf2.append("     ").append(Basic.skipFirstLine(Document.getVersionInfo().get(getClassName() + " tree")).replaceAll("\\s+", " "));
        statusBar.setText2(buf2.toString());
    }

    /**
     * gets the nodes associated with the given f id
     *
     * @param fId
     * @return nodes or null
     * todo: modify to use all nodes associated with this id
     */
    public Set<Node> getNodes(int fId) {
        return id2Nodes.get(fId);
    }

    public Set<Integer> getIds() {
        return id2Nodes.keySet();
    }

    /**
     * get a node associated with the given fId
     *
     * @param fId
     * @return a node or null
     */
    public Node getANode(int fId) {
        Set<Node> nodes = getNodes(fId);
        if (nodes != null && nodes.size() > 0)
            return nodes.iterator().next();
        else
            return null;
    }

    /**
     * is node with this f id selected?
     *
     * @param fId
     * @return selected?
     */
    public boolean isSelected(int fId) {
        Node v = getANode(fId);
        return v != null && getSelected(v);
    }

    public String getTitle() {
        return getFrame().getTitle();
    }

    /**
     * embeds a tree is a phylogram from the given root, using the given h- and v-spacing
     *
     * @param root
     */
    private void embedTree(Node root) {
        removeAllInternalPoints();
        node2BoundingBox.clear();


        for (Node v = getTree().getFirstNode(); v != null; v = v.getNext()) {
            setLocation(v, null);
            getNV(v).setFixedSize(true);
        }
        taxonLevel = 0; // number of leaves placed


        if (getDrawerType() == DiagramType.RectangularPhylogram || getDrawerType() == DiagramType.RoundedPhylogram) {
            embedPhylogramRec(root, null, 0);
            if (getDrawerType() == DiagramType.RoundedPhylogram) {
                for (Edge e = getTree().getFirstEdge(); e != null; e = getTree().getNextEdge(e))
                    getEV(e).setShape(EdgeView.ROUNDED_EDGE);
            }
        } else //  getDrawerType().equals(DiagramType.RectangularCladogram
        {
            embedCladogramRec(root, null);
            if (getDrawerType() == DiagramType.RoundedCladogram) {
                for (Edge e = getTree().getFirstEdge(); e != null; e = getTree().getNextEdge(e))
                    getEV(e).setShape(EdgeView.ROUNDED_EDGE);
            }
        }

        trans.setCoordinateRect(getBBox()); // tell the transform that this has changed

        if (!isShowIntermediateLabels())
            showLabels(getDegree2Nodes(), false);
    }

    /**
     * get all non-root nodes of degree 2
     *
     * @return all none-root nodes of degree 2
     */
    public NodeSet getDegree2Nodes() {
        NodeSet nodes = new NodeSet(getTree());

        for (Node v = getTree().getFirstNode(); v != null; v = v.getNext())
            if (v != getTree().getRoot() && v.getDegree() == 2)
                nodes.add(v);

        return nodes;
    }

    private int taxonLevel;

    /**
     * recursively does the work
     *
     * @param v
     * @param e
     * @return bbounding box of subtree
     */
    private Rectangle embedCladogramRec(Node v, Edge e) {
        Rectangle bbox = null;

        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            Rectangle subBox = embedCladogramRec(f.getOpposite(v), f);
            if (bbox == null)
                bbox = subBox;
            else
                bbox.add(subBox);
        }

        Point location;
        if (bbox == null) // no dependent subtree, make new box
        {
            location = new Point(0, XSTEP * taxonLevel++);
            bbox = new Rectangle(location.x, location.y, HLEAFBOX, YSTEP);
            setLocation(v, location);
        } else {
            location = new Point(bbox.x - XSTEP, bbox.y + (bbox.height - YSTEP) / 2);
            bbox.add(location);
            setLocation(v, location);
        }

        final float num;
        final NodeData nodeData = super.getNodeData(v);
        if (nodeDrawer.getScaleBy() == NodeDrawer.ScaleBy.Summarized || (nodeDrawer.getScaleBy() == NodeDrawer.ScaleBy.Assigned && v.getOutDegree() == 0))
            num = (nodeData == null ? 0 : nodeData.getCountSummarized());
        else if (nodeDrawer.getScaleBy() == NodeDrawer.ScaleBy.Assigned)
            num = (nodeData == null ? 0 : nodeData.getCountAssigned());
        else  // nodeDrawer.getScaleBy() == MeganNodeDrawer.ScaleBy.None
            num = 0;

        if (num > 0) {
            int radius = (int) Math.max(1.0, nodeDrawer.getScaledSize(num));
            this.setHeight(v, 2 * radius);
            this.setWidth(v, 2 * radius);
        } else {
            this.setWidth(v, 1);
            this.setHeight(v, 1);
        }

        // add bends to edges:
        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            Node w = f.getOpposite(v);
            if (getLocation(w) != null && getLocation(v).getY() != getLocation(w).getY()) {
                List<Point2D> list = new LinkedList<>();
                list.add(new Point2D.Double(getLocation(v).getX(), getLocation(w).getY()));
                setInternalPoints(f, list);
            }
        }

        node2BoundingBox.put(v, (Rectangle) bbox.clone());
        return bbox;
    }

    /**
     * recursively does the work
     *
     * @param v
     * @param e
     * @param level
     * @return bbounding box of subtree
     */
    private Rectangle embedPhylogramRec(Node v, Edge e, int level) {
        Rectangle bbox = null;

        for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
            if (f != e) {
                Rectangle subBox = embedPhylogramRec(f.getOpposite(v), f, level + 1);
                if (bbox == null)
                    bbox = subBox;
                else
                    bbox.add(subBox);
            }
        }

        Point location;
        if (bbox == null) // no dependent subtree, make new box
        {
            location = new Point(XSTEP * level, YSTEP * taxonLevel++);
            bbox = new Rectangle(location.x, location.y, HLEAFBOX, YSTEP);
            setLocation(v, location);
        } else {
            location = new Point(XSTEP * level, bbox.y + (bbox.height - YSTEP) / 2);
            bbox.add(location);
            setLocation(v, location);
        }

        final float num;
        if (nodeDrawer.getScaleBy() == NodeDrawer.ScaleBy.Summarized || (nodeDrawer.getScaleBy() == NodeDrawer.ScaleBy.Assigned && v.getOutDegree() == 0))
            num = (super.getNodeData(v)).getCountSummarized();
        else if (nodeDrawer.getScaleBy() == NodeDrawer.ScaleBy.Assigned)
            num = (super.getNodeData(v)).getCountAssigned();
        else  // nodeDrawer.getScaleBy() == MeganNodeDrawer.ScaleBy.None
            num = 0;

        if (num > 0) {
            int radius = (int) Math.max(1.0, nodeDrawer.getScaledSize(num));
            this.setHeight(v, 2 * radius);
            this.setWidth(v, 2 * radius);
        } else {
            this.setWidth(v, 1);
            this.setHeight(v, 1);
        }

        // add bends to edges:
        for (Edge f = v.getFirstAdjacentEdge(); f != null; f = v.getNextAdjacentEdge(f)) {
            if (f != e) {
                Node w = f.getOpposite(v);
                if (getLocation(w) != null && getLocation(v).getY() != getLocation(w).getY()) {
                    List<Point2D> list = new LinkedList<>();
                    list.add(new Point2D.Double(getLocation(v).getX(), getLocation(w).getY()));
                    setInternalPoints(f, list);
                }
            }
        }

        node2BoundingBox.put(v, (Rectangle) bbox.clone());
        return bbox;
    }

    /**
     * Paint.
     *
     * @param gc0 the Graphics
     */
    public void paint(Graphics gc0) {
        drawOnScreen = (!ExportManager.inWriteToFileOrGetData() && !inPrint);
        Graphics2D gc = (Graphics2D) gc0;
        nodeDrawer.setup(this, gc);
        setBackground(canvasColor);

        gc.setColor(drawOnScreen ? canvasColor : Color.WHITE);
        Rectangle totalRect;
        Rectangle frameRect;
        frameRect = new Rectangle(super.getScrollPane().getHorizontalScrollBar().getValue(),
                super.getScrollPane().getVerticalScrollBar().getValue(),
                super.getScrollPane().getHorizontalScrollBar().getVisibleAmount(),
                super.getScrollPane().getVerticalScrollBar().getVisibleAmount());

        if (drawOnScreen)
            totalRect = frameRect;
        else
            totalRect = trans.getPreferredRect();

        if (!inPrint)
            gc.fill(totalRect);

        gc.setColor(Color.BLACK);
        gc.setFont(getFont());

        BasicStroke stroke = new BasicStroke(1);
        gc.setStroke(stroke);

        if (drawOnScreen && trans.getMagnifier().isActive())
            trans.getMagnifier().draw(gc);

        try {
            final List<Node> drawableNodeLabels = new LinkedList<>();
            final Node root = getTree().getRoot();
            if (root != null)
                paintRec(gc, root, drawableNodeLabels, true);

            // this is where we draw the node labels
            drawNodeLabels(gc, drawableNodeLabels);
        } catch (Exception ex) {
            Basic.caught(ex);
        }

        if (getFoundNode() != null) {
            if (getFoundNode().getOwner() == null || !getSelected(getFoundNode()))
                setFoundNode(null);
            else {
                Node v = getFoundNode();
                NodeView nv = getNV(v);

                if (nv.getLabel() != null)
                    nv.setLabelSize(BasicSwing.getStringSize(gc, getLabel(v), getFont(v)));

                gc.setColor(ProgramProperties.SELECTION_COLOR.brighter());

                final Rectangle rect = nv.getBox(trans);
                if (rect != null) {
                    rect.grow(2, 2);
                    gc.fill(rect);
                }

                final Shape shape = nv.getLabelShape(trans);
                if (nv.isLabelVisible())
                    gc.fill(shape);
                gc.setColor(ProgramProperties.SELECTION_COLOR_DARKER);
                gc.draw(shape);
                nodeDrawer.drawNodeAndLabel(v, false);
            }
        }

        drawPoweredBy(gc, frameRect);

        if (doScrollToRight) {
            doScrollToRight = false;
            JScrollBar sbar = super.getScrollPane().getHorizontalScrollBar();
            sbar.setValue(sbar.getMaximum() - trans.getRightMargin() - 200);
            super.getScrollPane().getViewport().setViewPosition(new Point(
                    sbar.getMaximum() - trans.getRightMargin() - 200,
                    super.getScrollPane().getVerticalScrollBar().getValue()));
        }

        if (mustDrawTwice) {
            mustDrawTwice = false;
            repaint();
        }
    }

    /**
     * draws the powered by logo
     *
     * @param gc
     */
    protected void drawPoweredBy(Graphics2D gc, Rectangle rect) {
        if (getPOWEREDBY() != null && getPOWEREDBY().length() > 2) {
            gc.setColor(Color.gray);
            gc.setStroke(new BasicStroke(1));
            gc.setFont(poweredByFont);
            int width = (int) BasicSwing.getStringSize(gc, getPOWEREDBY(), gc.getFont()).getWidth();
            int x = rect.x + rect.width - width - 2;
            int y = rect.y + rect.height - 2;
            gc.drawString(getPOWEREDBY(), x, y);
        }
    }

    /**
     * recursively draw the tree
     *
     * @param gc
     * @param v
     * @param enabled
     */
    private void paintRec(Graphics2D gc, Node v, List<Node> drawableNodeLabels, boolean enabled) {
        Rectangle bbox = node2BoundingBox.get(v);
        if (bbox == null)
            bbox = new Rectangle();

        //bbox.width += HLEAFBOX;
        final Rectangle bboxDeviceCoordinates = new Rectangle();
        trans.w2d(bbox, bboxDeviceCoordinates);

        /*
          {
            gc.setColor(Color.GREEN);
            gc.draw(bboxDeviceCoordinates);
            Rectangle r = (Rectangle) getVisibleRect().clone();
            r.x += 5;
            r.width -= 10;
            r.y += 5;
            r.height -= 10;
            gc.draw(r);
        }
        */

        if (drawOnScreen && !getVisibleRect().intersects(bboxDeviceCoordinates)) // && getTree().getDegree(v) > 1)
            return; // nothing visible, return

        final NodeView nv = getNV(v);
        if (nv.getLabel() != null) {
            nv.setLabelSize(gc); // ensure label rect is set
        }

        /*
        if (getSelected(v)) {
            gc.setColor(Color.BLUE);
            gc.draw(bboxDeviceCoordinates);
        }
        */

        nv.setEnabled(enabled);

        // if height of bbox is so small that we can't see the edges, just draw a gray box:
        final boolean isSmall = v.getOutDegree() > 0 && (bboxDeviceCoordinates.getHeight() / (getTree().getDegree(v) - 1) < 4);

        if (isSmall) {
            if (1 != ((BasicStroke) gc.getStroke()).getLineWidth())
                gc.setStroke(new BasicStroke(1));
            gc.setColor(Color.DARK_GRAY);
            gc.fill(bboxDeviceCoordinates);
        } else // draw the edges in detail
        {
            // use edges back-to-front so that grey edges get drawn first!
            for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
                Node w = v.getOpposite(f);


                Point2D nextToV = getNV(w).getLocation();
                Point2D nextToW = getNV(v).getLocation();
                if (nextToV == null || nextToW == null)
                    continue;
                if (getInternalPoints(f) != null) {
                    if (getInternalPoints(f).size() != 0) {
                        nextToV = getInternalPoints(f).get(0);
                        nextToW = getInternalPoints(f).get(getInternalPoints(f).size() - 1);
                    }
                }
                final Point pv = (!drawLeavesOnly || v.getOutDegree() == 0 ? getNV(v).computeConnectPoint(nextToV, trans) :
                        trans.w2d(getNV(v).getLocation()));
                final Point pw = (!drawLeavesOnly || w.getOutDegree() == 0 ? getNV(w).computeConnectPoint(nextToW, trans) :
                        trans.w2d(getNV(w).getLocation()));

                final EdgeView ev = getEV(f);
                ev.setEnabled(enabled);
                if (ev.getLineWidth() != ((BasicStroke) gc.getStroke()).getLineWidth()) {
                    gc.setStroke(new BasicStroke(ev.getLineWidth()));
                }

                ev.draw(gc, pv, pw, trans, getSelected(f));

                ev.setLabelReferenceLocation(nextToV, nextToW, trans);

                if (ev.getLabel() != null && ev.isLabelVisible()) {
                    ev.setLabelReferenceLocation(pv, pw, trans);
                    ev.setLabelSize(gc);
                    ev.drawLabel(gc, trans, false);
                    if (getSelected(f))
                        ev.drawLabel(gc, trans, true);
                }

                paintRec(gc, w, drawableNodeLabels, enabled);
            }
        }
        if (getNV(v).getLineWidth() != ((BasicStroke) gc.getStroke()).getLineWidth()) {
            gc.setStroke(new BasicStroke(getNV(v).getLineWidth()));
        }

        if (!drawLeavesOnly || v.getOutDegree() == 0)
            nodeDrawer.draw(v, selectedNodes.contains(v));

        if (getLabel(v) != null && getLabel(v).length() > 0 && !isSmall)
            drawableNodeLabels.add(v);
    }

    private double oldXScale = 0;
    private double oldYScale = 0;

    /**
     * we first collect all node labels to be drawn and then draw them here.
     *
     * @param gc
     * @param drawableNodeLabels
     */
    private void drawNodeLabels(Graphics2D gc, List<Node> drawableNodeLabels) {

        //    if (drawableNodeLabels.size() > 350)
        //        setAutoLayoutLabels(false);
        // if (getAutoLayoutLabels())
        {
            boolean scaleHasChanged = (oldXScale != trans.getScaleX() || oldYScale != trans.getScaleY());
            if (scaleHasChanged) {        // centerAndScale has changed, allow new layout of all labels
                oldXScale = trans.getScaleX();
                oldYScale = trans.getScaleY();
                for (Node v : drawableNodeLabels) {
                    if (v == getTree().getRoot())
                        setLabelLayout(v, ViewBase.WEST);
                    else if (v.getOutDegree() == 0)      // leaf
                    {
                        setLabelLayout(v, ViewBase.EAST);
                    } else if (v.getInDegree() == 1 && v.getOutDegree() == 1) // internal path node
                    {
                        setLabelLayout(v, ViewBase.NORTH);
                    } else    // internal node
                    {
                        setLabelLayout(v, ViewBase.NORTHWEST);
                    }
                }
            }
            List<Pair<Node, Node>> pairs = new LinkedList<>();
            for (Node v : drawableNodeLabels) {
                if (getNV(v) == null || !getNV(v).isLabelVisible())
                    continue;
                final Rectangle box = getNV(v).getLabelRect(trans);
                if (box != null) {
                    /*
                    {
                        gc.setColor(Color.GREEN);
                        gc.draw(box);
                    }
                    */
                    // find pairs of adjacent labels:
                    for (Node w : drawableNodeLabels) {
                        if (w == v)
                            break;
                        try {
                            if (getGraph().getOutDegree(v) > 0 || getGraph().getOutDegree(w) > 0) {
                                final Rectangle rect = getNV(w).getLabelRect(trans);
                                if (rect != null && box.intersects(rect))
                                    pairs.add(new Pair<>(v, w));
                            }
                        } catch (Exception ex) {
                            // silently ignore...
                        }
                    }
                }
            }

            final Random random = new Random(26660);
            for (int run = 0; run < 20; run++) {
                boolean changed = false;
                for (Iterator it = Basic.randomize(pairs.iterator(), random); it.hasNext(); ) {
                    Pair pair = (Pair) it.next();
                    Node v = (Node) pair.getFirst();
                    Node w = (Node) pair.getSecond();
                    Rectangle rv = getNV(v).getLabelRect(trans);
                    Rectangle rw = getNV(w).getLabelRect(trans);
                    if (rv != null && rw != null && rv.intersects(rw)) {
                        Point pv = getNV(v).getLabelPosition(trans);
                        Point pw = getNV(w).getLabelPosition(trans);

                        if (pv == null || pw == null)
                            continue;

                        if (rv.x <= rw.x) {
                            pv.x -= Math.max(1, rv.height / 3);
                            pw.x += Math.max(1, rw.height / 3);
                        } else {
                            pv.x += Math.max(1, rv.height / 3);
                            pw.x -= Math.max(1, rw.height / 3);

                        }
                        if (rv.y <= rw.y) {
                            pv.y -= Math.max(1, rv.height / 3);
                            pw.y += Math.max(1, rw.height / 3);
                        } else {
                            pv.y += Math.max(1, rv.height / 3);
                            pw.y -= Math.max(1, rw.height / 3);
                        }

                        if (getGraph().getOutDegree(v) > 0) {
                            getNV(v).setLabelPosition(pv.x, pv.y, trans);
                            changed = true;
                        }
                        if (getGraph().getOutDegree(w) > 0) {
                            getNV(w).setLabelPosition(pw.x, pw.y, trans);
                            changed = true;
                        }
                    }
                }
                if (!changed)
                    break;
            }
        }

        // draw all labels
        for (Node v : drawableNodeLabels) {
            if (getSelected(v)) {
                //getNV(v).hiliteLabel(gc, trans, getFont());
                nodeDrawer.drawLabel(v, true);
            } else
                getNV(v).drawLabel(gc, trans, getFont());
        }
    }


    /**
     * uncollapse all selected nodes
     *
     * @param wholeSubtree if true, uncollapse the whole subtrees below any selected nodes
     */
    public void uncollapseSelectedNodes(boolean wholeSubtree) {
        final Set<Integer> ids = new HashSet<>();

        for (Node v = getSelectedNodes().getFirstElement(); v != null; v = getSelectedNodes().getNextElement(v)) {
            if (v.getOutDegree() == 0) {
                Integer vid = (Integer) v.getInfo();
                ids.add(vid);
                if (!wholeSubtree)    // collapse all dependant nodes
                    setSelected(v, false);
            }
        }

        final Set<Integer> seen = new HashSet<>();

        for (int id : ids) {
            getCollapsedIds().remove(id);
            Node vFull = classification.getFullTree().getANode(id);
            if (!wholeSubtree)    // collapse all dependent nodes
            {
                for (Edge eFull : vFull.outEdges()) {
                    Node wFull = eFull.getOpposite(vFull);
                    Integer wid = (Integer) wFull.getInfo();
                    getCollapsedIds().add(wid);
                }
            } else
                uncollapseSelectedNodesRec(vFull, ids, seen);
        }
        updateTree();
    }

    /**
     * recursively does the work
     */
    private void uncollapseSelectedNodesRec(Node v, Set<Integer> ids, Set<Integer> seen) {
        Integer id = (Integer) v.getInfo();
        if (!seen.contains(id)) {
            if (ids.contains(id))
                seen.add(id);
            if (id != null)
                getCollapsedIds().remove(id);
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                uncollapseSelectedNodesRec(e.getOpposite(v), ids, seen);
            }
        }
    }


    /**
     * completely uncollapse all nodes
     */
    public void uncollapseAll() {
        getCollapsedIds().clear();
        updateTree();
    }

    /**
     * collapse  all selected  nodes
     */
    public void collapseSelectedNodes() {
        for (Node v = getSelectedNodes().getFirstElement(); v != null; v = getSelectedNodes().getNextElement(v)) {
            getCollapsedIds().add((Integer) v.getInfo());
        }
        updateTree();
    }

    /**
     * collapse all  nodes at subsystem level
     */
    public void collapseToTop() {
        getCollapsedIds().clear();
        if (getTree().getRoot() != null) {
            for (Edge e = getTree().getRoot().getFirstOutEdge(); e != null; e = getTree().getRoot().getNextOutEdge(e)) {
                getCollapsedIds().add((Integer) e.getTarget().getInfo());
            }
            updateTree();
        }
    }

    /**
     * setup the node labels
     *
     * @param resetLabelPositions
     */
    public void setupNodeLabels(boolean resetLabelPositions) {
        for (Node v = getTree().getFirstNode(); v != null; v = v.getNext()) {
            if (getNumberSelectedNodes() == 0 || getSelected(v)) {
                StringBuilder buf = new StringBuilder();
                int fId = (Integer) v.getInfo();

                if (isNodeLabelNames())
                    buf.append(classification.getName2IdMap().get(fId));
                if (isNodeLabelIds()) {
                    if (buf.length() > 0)
                        buf.append(" ");
                    buf.append(fId);
                }
                NodeData nodeData = super.getNodeData(v);
                if (isShowIntermediateLabels() || !(v.getInDegree() == 1 && v.getOutDegree() == 1)) {
                    if (isNodeLabelAssigned()) {
                        if (v.getOutDegree() > 0) {
                            if (nodeData.getAssigned().length == 1) {
                                if (buf.length() > 0)
                                    buf.append("; ");
                                buf.append(Math.round(nodeData.getCountAssigned()));
                            } else if (nodeData.getAssigned().length > 1) {
                                if (buf.length() > 0)
                                    buf.append("; ");
                                buf.append(Basic.toString(nodeData.getAssigned(), 0, nodeData.getAssigned().length, " ", true));
                            }
                        } else {
                            if (nodeData.getSummarized().length == 1) {
                                if (buf.length() > 0)
                                    buf.append("; ");
                                buf.append(Math.round(nodeData.getCountSummarized()));
                            } else if (nodeData.getAssigned().length > 1) {
                                if (buf.length() > 0)
                                    buf.append("; ");
                                buf.append(Basic.toString(nodeData.getSummarized(), 0, nodeData.getSummarized().length, " ", true));
                            }
                        }
                        if (buf.length() > 0 && !getLabelVisible(v))
                            setLabelVisible(v, true); // explicitly set, make visible
                    }
                    if (isNodeLabelSummarized() && (!isNodeLabelAssigned() || v.getOutDegree() > 0)) {
                        if (nodeData.getSummarized().length == 1) {
                            if (buf.length() > 0)
                                buf.append("; ");
                            buf.append(Math.round(nodeData.getCountSummarized()));
                        } else if (nodeData.getAssigned().length > 1) {
                            if (buf.length() > 0)
                                buf.append("; ");
                            buf.append(Basic.toString(nodeData.getSummarized(), 0, nodeData.getSummarized().length, " ", true));
                        }
                        if (buf.length() > 0 && !getLabelVisible(v))
                            setLabelVisible(v, true); // explicitly set, make visible
                    }
                }

                this.setLabel(v, buf.toString());

                if (resetLabelPositions) {
                    if (v == getTree().getRoot())
                        setLabelLayout(v, ViewBase.WEST);
                    else if (v.getOutDegree() == 0)      // leaf
                    {
                        setLabelLayout(v, ViewBase.EAST);
                    } else if (v.getInDegree() == 1 && v.getOutDegree() == 1) // internal path node
                    {
                        setLabelLayout(v, ViewBase.NORTH);
                    } else    // internal node
                    {
                        setLabelLayout(v, ViewBase.NORTHWEST);

                    }
                }
            }
        }
    }

    /**
     * gets the number of datasets
     *
     * @return number of datasets
     */
    public int getNumberOfDatasets() {
        return doc.getNumberOfSamples();
    }

    /**
     * select all nodes associated with the given f ids
     *
     * @param fIds
     * @param state
     */
    public void setSelectedIds(Collection<Integer> fIds, boolean state) {
        NodeSet nodesToSelect = new NodeSet(getTree());
        for (Node v = getTree().getFirstNode(); v != null; v = v.getNext()) {
            if (fIds.contains(v.getInfo())) {
                nodesToSelect.add(v);
            }
        }
        setSelected(nodesToSelect, state);
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    /**
     * set the tool tip to the given node
     *
     * @param v
     */
    public void setToolTipText(Node v) {
        final StringWriter writer = new StringWriter();
        writer.write("<html><i>");
        final Integer id = (Integer) v.getInfo();
        String text = id2toolTip.get(id);
        if (text == null || text.length() == 0)
            text = getLabel(v);
        else
            text = Basic.fold(text, 80, "<br>");
        writer.write(text);
        writer.write("</i><p>");
        {
            List<String> list = new ArrayList<>();
            for (Node w : getNodes(id)) {
                while (w.getInDegree() > 0) {
                    w = w.getFirstInEdge().getSource();
                    list.add(getLabel(w));
                }
                String str = Basic.toString(Basic.reverse(list), ";");
                list.clear();
                writer.write(str);
                writer.write("<br>");
                //System.err.println(str);
            }
        }

        {
            Integer level = id2rank.get(v.getInfo());
            if (level != null) {
                String name = TaxonomicLevels.getName(level);
                if (name != null) {
                    writer.write("<i>(" + name + ")</i><p>");
                }
            }
        }

        NodeData data = (super.getNodeData(v));
        if (data.getCountAssigned() > 0) {
            writer.write("Assigned: ");
            if (data.getAssigned().length < 50) {
                boolean first = true;
                for (float value : data.getAssigned()) {
                    if (first)
                        first = false;
                    else
                        writer.write(", ");
                    writer.write("<b>" + Math.round(value) + "</b>");
                }
                writer.write("<p>");
            } else {
                Statistics statistics = new Statistics(data.getAssigned());
                writer.write(String.format("<b>%,.0f - %,.0f</b> (mean: %,.0f sd: %,.0f)", statistics.getMin(), statistics.getMax(), statistics.getMean(), statistics.getStdDev()));
            }
            writer.write("<p>");
        }
        if (data.getCountSummarized() > data.getCountAssigned()) {
            writer.write("Summed:    ");
            if (data.getSummarized().length < 50) {
                boolean first = true;
                for (float value : data.getSummarized()) {
                    if (first)
                        first = false;
                    else
                        writer.write(", ");
                    writer.write("<b>" + Math.round(value) + "</b>");
                }
                writer.write("<p>");
            } else {
                Statistics statistics = new Statistics(data.getSummarized());
                writer.write(String.format("<b>%,.0f - %,.0f</b> (mean: %,.0f sd: %,.0f)", statistics.getMin(), statistics.getMax(), statistics.getMean(), statistics.getStdDev()));
            }
            writer.write("<p>");
        }
        setToolTipText(writer.toString());
    }

    /**
     * recursively print a summary
     *
     * @param selectedNodes
     * @param v
     * @param indent
     */
    @Override
    public void listSummaryRec(NodeSet selectedNodes, Node v, int indent, Writer outs) throws IOException {
        int id = (Integer) v.getInfo();
        final String name = classification.getName2IdMap().get(id);

        NodeData data = (super.getNodeData(v));

        if ((selectedNodes == null || selectedNodes.contains(v))) {
            if (data.getCountSummarized() > 0) {
                for (int i = 0; i < indent; i++)
                    outs.write(" ");
                outs.write(name + ": " + data.getCountSummarized() + "\n");
            }
        }
        if (getCollapsedIds().contains(id)) {
            return;
        }
        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            listSummaryRec(selectedNodes, f.getOpposite(v), indent + 2, outs);
        }
    }

    public boolean isShowFindToolBar() {
        return showFindToolBar;
    }

    public void setShowFindToolBar(boolean showFindToolBar) {
        if (this.showFindToolBar && !showFindToolBar)
            requestFocusInWindow();
        this.showFindToolBar = showFindToolBar;
    }

    public SearchManager getSearchManager() {
        return searchManager;
    }

    /**
     * does this viewer currently have any URLs for selected nodes?
     *
     * @return true, if has URLs for selected nodes
     */
    public boolean hasURLsForSelection() {
        return false;
    }

    /**
     * gets list of URLs associated with selected nodes
     *
     * @return URLs
     */
    public List<String> getURLsForSelection() {
        return new ArrayList<>();
    }

    public Classification getClassification() {
        return classification;
    }

    public ViewerJTable getViewerJTable() {
        return viewerJTable;
    }

    public ViewerJTree getViewerJTree() {
        return viewerJTree;
    }

    public String getClassName() {
        if (classification != null)
            return getClassName(classification.getName());
        else return "?";
    }

    public static String getClassName(String cName) {
        return cName;
    }

    public void setMustDrawTwice() {
        mustDrawTwice = true;
    }

    public List<Integer> computeDisplayedIdsInSearchOrder() {
        final List<Integer> list = new ArrayList<>(getTree().getNumberOfNodes());
        final Stack<Node> stack = new Stack<>();
        stack.add(getTree().getRoot());
        while (stack.size() > 0) {
            final Node v = stack.pop();
            Integer id = (Integer) v.getInfo();
            list.add(id);

            for (Edge e = v.getLastOutEdge(); e != null; e = v.getPrevOutEdge(e))
                stack.push(e.getTarget());
        }
        return list;
    }

    public List<Integer> computeAllIdsInSearchOrder() {
        final PhyloTree tree = new PhyloTree();
        computeInduceTreeWithNoCollapsedNodes(tree, new HashMap<>());
        final List<Integer> list = new ArrayList<>(tree.getNumberOfNodes());
        final Stack<Node> stack = new Stack<>();
        stack.add(tree.getRoot());
        while (stack.size() > 0) {
            final Node v = stack.pop();
            list.add((Integer) v.getInfo());
            for (Edge e = v.getLastOutEdge(); e != null; e = v.getPrevOutEdge(e))
                stack.push(e.getTarget());
        }
        return list;
    }

    public void computeInduceTreeWithNoCollapsedNodes(PhyloTree tree, Map<Integer, Set<Node>> id2nodes) {
        tree.clear();
        classification.getFullTree().extractInducedTree(id2NodeData, new HashSet<>(), tree, id2nodes);
    }

    public int getTotalAssignedReads() {
        return totalAssignedReads;
    }

    JSplitPane getMainSplitPane() {
        return mainSplitPane;
    }

    StatusBar getStatusBar() {
        return statusBar;
    }

    @Override
    public boolean useHeatMapColors() {
        return getNodeDrawer().getStyle() == NodeDrawer.Style.HeatMap;
    }

    public void selectNodesPositiveAssigned() {
        for (Object o : getGraph().nodes()) {
            final Node v = (Node) o;
            if (!getSelected(v) && getNodeData(v).getCountAssigned() > 0) {
                setSelected(v, true);
            }
        }
    }
}


