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

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.swing.commands.CommandManager;
import jloda.swing.find.SearchManager;
import jloda.swing.graphview.GraphView;
import jloda.swing.graphview.PhyloTreeView;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import megan.core.Director;
import megan.core.Document;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.*;

/**
 * contains methods used both by a number of different viewers
 * Daniel Huson, 12.2009
 */
abstract public class ViewerBase extends PhyloTreeView {
    private final Set<Integer> collapsedIds = new HashSet<>(); // hide all nodes below these

    private boolean nodeLabelNames = true;
    private boolean nodeLabelIds = false;
    private boolean nodeLabelAssigned = false;
    private boolean nodeLabelSummarized = false;

    private boolean showIntermediateLabels = false;
    boolean drawLeavesOnly = false;
    String showLegend = "undefined"; // vertical or horizontal or none or undefined

    final Director dir;
    final Document doc;

    private final Set<Integer> previousNodeIdsOfInterest = new HashSet<>();

    private final Set<Integer> dirtyNodeIds = new HashSet<>();
    private final Set<Pair<Integer, Integer>> dirtyEdgeIds = new HashSet<>();

    final JPanel mainPanel;
    final LegendPanel legendPanel;
    private final JScrollPane legendScrollPane;
    final JSplitPane splitPane;
    protected CommandManager commandManager;

    public enum DiagramType {RectangularCladogram, RectangularPhylogram, RoundedCladogram, RoundedPhylogram}

    DiagramType drawerType;

    final NodeDrawer nodeDrawer;

    /*
     * constructor
     *
     * @param tree
     * @param doEmbedding
     */
    public ViewerBase(Director dir, PhyloTree tree, boolean doEmbedding) {
        super(tree, doEmbedding);
        this.dir = dir;
        this.doc = dir.getDocument();

        this.setAllowMoveInternalEdgePoints(false);
        this.setAllowMoveNodes(false);
        this.setAllowMoveInternalEdgePoints(false);

        this.setFixedNodeSize(true);
        this.setAutoLayoutLabels(true);
        this.setAllowEdit(false);
        this.canvasColor = Color.WHITE;
        nodeDrawer = new NodeDrawer(dir.getDocument(), this);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        legendPanel = new LegendPanel(this);
        legendPanel.setStyle(nodeDrawer.getStyle());
        legendScrollPane = new JScrollPane(legendPanel);
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPanel, legendScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setEnabled(true);
        splitPane.setResizeWeight(1.0);
        splitPane.setDividerLocation(1.0);
        splitPane.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent componentEvent) {
                legendPanel.updateView();
                if (getShowLegend().equals("none") || getShowLegend().equals("undefined"))
                    splitPane.setDividerLocation(1.0);
            }
        });
        this.commandManager = new CommandManager(dir, this, new String[]{"megan.commands"}, !ProgramProperties.isUseGUI());


        try {
            drawerType = DiagramType.valueOf(ProgramProperties.get("DrawerType" + getClassName(), DiagramType.RoundedCladogram.toString()));
        } catch (Exception e) {
            drawerType = DiagramType.RoundedCladogram;
        }
    }

    /**
     * determine the maximum number of reads assigned to any node
     *
     * @return max number
     */
    double[] determineMaxAssigned() {
        // determine maximum count of reads on any given node:
        double maxSingleCount = 1;
        double maxTotalCount = 1;
        for (Node v = getTree().getFirstNode(); v != null; v = v.getNext()) {
            NodeData data = (NodeData) v.getData();
            int id = (Integer) v.getInfo();
            if (data != null && (id > 0 || id < -3)) {
                maxTotalCount = Math.max(maxTotalCount, v.getOutDegree() > 0 ? data.getCountAssigned() : data.getCountSummarized());
                float[] array = (v.getOutDegree() == 0 ? data.getSummarized() : data.getAssigned());
                for (float a : array)
                    maxSingleCount = Math.max(maxSingleCount, a);

            }
        }
        return new double[]{maxSingleCount, maxTotalCount};
    }

    /**
     * gets the node data
     *
     * @param v
     * @return
     */
    public NodeData getNodeData(Node v) {
        return (NodeData) v.getData();
    }

    /**
     * This method returns a list of
     * currently selected items.
     *
     * @return list of items
     */
    public Collection<Integer> getSelectedIds() {
        Set<Integer> seen = new HashSet<>();
        LinkedList<Integer> result = new LinkedList<>();
        for (Node v = getSelectedNodes().getFirstElement(); v != null; v = getSelectedNodes().getNextElement(v)) {
            Integer id = (Integer) v.getInfo();
            if (!seen.contains(id)) {
                seen.add(id);
                result.add(id);
            }
        }
        return result;
    }

    public Set<Integer> getCollapsedIds() {
        return collapsedIds;
    }

    public void setCollapsedIds(Set<Integer> collapsedIds) {
        this.collapsedIds.clear();
        this.collapsedIds.addAll(collapsedIds);
    }

    public Set<Node> getNodes(Integer id) {
        Set<Node> result = new HashSet<>();
        for (Node v = getGraph().getFirstNode(); v != null; v = v.getNext()) {
            Integer vid = (Integer) v.getInfo();
            if (vid != null && vid.equals(id))
                result.add(v);
        }
        return result;
    }

    /**
     * get ids of all given nodes
     *
     * @return ids
     */
    Set<Integer> getAllIds(NodeSet nodes) {
        Set<Integer> result = new HashSet<>();
        for (Node v = nodes.getFirstElement(); v != null; v = nodes.getNextElement(v)) {
            result.add((Integer) v.getInfo());
        }
        return result;
    }

    /**
     * setup the key listeners
     */
    void setupKeyListener() {
        getFrame().addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent keyEvent) {
                if (!keyEvent.isAltDown() && !keyEvent.isMetaDown() && !keyEvent.isControlDown()) {
                    boolean shift = keyEvent.isShiftDown();
                    switch (keyEvent.getKeyCode()) {
                        case KeyEvent.VK_RIGHT: {
                            JScrollBar bar = getScrollPane().getHorizontalScrollBar();
                            bar.setValue(bar.getValue() + (shift ? bar.getBlockIncrement(1) : bar.getUnitIncrement(1)));
                            break;
                        }
                        case KeyEvent.VK_LEFT: {
                            JScrollBar bar = getScrollPane().getHorizontalScrollBar();
                            bar.setValue(bar.getValue() - (shift ? bar.getBlockIncrement(1) : bar.getUnitIncrement(1)));
                            break;
                        }
                        case KeyEvent.VK_DOWN: {
                            JScrollBar bar = getScrollPane().getVerticalScrollBar();
                            bar.setValue(bar.getValue() + (shift ? bar.getBlockIncrement(1) : bar.getUnitIncrement(1)));
                            break;
                        }
                        case KeyEvent.VK_UP: {
                            JScrollBar bar = getScrollPane().getVerticalScrollBar();
                            bar.setValue(bar.getValue() - (shift ? bar.getBlockIncrement(1) : bar.getUnitIncrement(1)));
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * display full names?
     *
     * @return display full names?
     */
    public boolean isNodeLabelNames() {
        return nodeLabelNames;
    }

    /**
     * display full names rather than ids?
     *
     * @param nodeLabelNames
     */
    public void setNodeLabelNames(boolean nodeLabelNames) {
        this.nodeLabelNames = nodeLabelNames;
    }

    /**
     * set all node labels visible or invisible
     *
     * @param show
     */
    public void showNodeLabels(boolean show) {
        for (Node v = getGraph().getFirstNode(); v != null; v = v.getNext()) {
            setLabelVisible(v, show);
        }
    }

    /**
     * select all nodes below any of the currently selected nodes
     */
    public void selectSubTreeNodes() {
        NodeSet seeds = getSelectedNodes();
        NodeSet selected = new NodeSet(getGraph());
        for (Node v : seeds) {
            if (!selected.contains(v))
                selectSubTreeNodesRec(v, selected);
        }
        selected.removeAll(selectedNodes);
        if (selected.size() > 0) {
            selectedNodes.addAll(selected);
            fireDoSelect(selected);
        }

    }

    /**
     * recursively does the work
     *
     * @param v
     * @param selected
     */
    private void selectSubTreeNodesRec(final Node v, final NodeSet selected) {
        selected.add(v);
        for (Edge f : v.outEdges()) {
            selectSubTreeNodesRec(f.getOpposite(v), selected);
        }
    }

    /**
     * select all leaves below any of the currently selected nodes
     */
    public void selectLeavesBelow() {
        NodeSet seeds = getSelectedNodes();
        NodeSet visited = new NodeSet(getGraph());
        NodeSet selected = new NodeSet(getGraph());
        for (Node v : seeds) {
            if (!visited.contains(v))
                selectSubTreeLeavesRec(v, visited, selected);
        }
        NodeSet oldSelected = new NodeSet(getGraph());
        for (Node v : seeds) {
            if (v.getOutDegree() > 0) {
                oldSelected.add(v);
                selectedNodes.remove(v);
                selected.remove(v);
            }
        }
        if (oldSelected.size() > 0)
            fireDoDeselect(oldSelected);
        selected.removeAll(selectedNodes);
        if (selected.size() > 0) {
            selectedNodes.addAll(selected);
            fireDoSelect(selected);
        }
    }

    /**
     * select all nodes above
     */
    public void selectNodesAbove() {
        NodeSet seeds = getSelectedNodes();
        NodeSet selected = new NodeSet(getGraph());

        for (Node v : seeds) {
            Node w = v;
            while (w != null && !selected.contains(w)) {
                selected.add(w);
                if (w.getInDegree() > 0)
                    w = w.getFirstInEdge().getSource();
            }

        }

        selected.removeAll(seeds);

        if (selected.size() > 0) {
            selectedNodes.addAll(selected);
            fireDoSelect(selected);
        }
    }

    /**
     * recursively does the work
     *
     * @param v
     * @param visited
     */
    private void selectSubTreeLeavesRec(final Node v, final NodeSet visited, final NodeSet selected) {
        visited.add(v);
        if (v.getOutDegree() == 0)
            selected.add(v);
        else {
            for (Edge f : v.outEdges()) {
                selectSubTreeLeavesRec(f.getOpposite(v), visited, selected);
            }
        }
    }


    /**
     * select all leaves
     */
    public void selectAllLeaves() {
        NodeSet selected = new NodeSet(getGraph());
        for (Node v = getGraph().getFirstNode(); v != null; v = v.getNext()) {
            if (v.getOutDegree() == 0 && !selectedNodes.contains(v) && !(v.getInfo() instanceof Integer && ((Integer) v.getInfo() < 0) && ((Integer) v.getInfo() >= -3)))
                selected.add(v);
        }
        if (selected.size() > 0) {
            selectedNodes.addAll(selected);
            fireDoSelect(selected);
        }
    }

    /**
     * select all internal nodes
     */
    public void selectAllInternal() {
        NodeSet selected = new NodeSet(getGraph());
        for (Node v = getGraph().getFirstNode(); v != null; v = v.getNext())
            if (v.getOutDegree() > 0 && !selectedNodes.contains(v))
                selected.add(v);
        if (selected.size() > 0) {
            selectedNodes.addAll(selected);
            fireDoSelect(selected);
        }
    }

    /**
     * select all nodes below any of the currently selected nodes
     */
    public void selectAllIntermediateNodes() {
        NodeSet selected = new NodeSet(getGraph());
        for (Node v = getGraph().getFirstNode(); v != null; v = v.getNext())
            if (v.getInDegree() == 1 && v.getOutDegree() == 1 & !selectedNodes.contains(v))
                selected.add(v);
        if (selected.size() > 0) {
            selectedNodes.addAll(selected);
            fireDoSelect(selected);
        }
    }

    /**
     * show intermediate labels?
     *
     * @param show
     */
    public void setShowIntermediateLabels(boolean show) {
        showIntermediateLabels = show;
        showLabels(getDegree2Nodes(), show);
    }

    public boolean isShowIntermediateLabels() {
        return showIntermediateLabels;
    }

    public int getMaxNodeRadius() {
        return nodeDrawer.getMaxNodeHeight();
    }

    public void setMaxNodeRadius(int maxNodeRadius) {
        nodeDrawer.setMaxNodeHeight(maxNodeRadius);
    }

    /**
     * select nodes by labels
     *
     * @param labels
     * @return true, if any changes made
     */
    public boolean selectNodesByLabels(Collection<String> labels, boolean state) {
        boolean changed = false;
        if (labels.size() > 0) {
            for (Node v = getGraph().getFirstNode(); v != null; v = v.getNext()) {
                if (getLabel(v) != null && getLabel(v).length() > 0) {
                    String label = getLabel(v);
                    if (labels.contains(label) && getSelected(v) != state) {
                        setSelected(v, state);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    /**
     * gets the set of selected node labels
     *
     * @param inOrder
     * @return selected node labels
     */
    public List<String> getSelectedNodeLabels(boolean inOrder) {

        List<String> selectedLabels = new LinkedList<>();
        if (inOrder) {
            Node v = getTree().getRoot();
            if (v != null) {
                Stack<Node> stack = new Stack<>();
                stack.add(v);
                if (getSelected(v))
                    selectedLabels.add(getLabel(v));
                while (stack.size() > 0) {
                    v = stack.pop();
                    for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                        Node w = e.getTarget();
                        if (getSelected(w))
                            selectedLabels.add(getLabel(w));
                        stack.add(w);

                    }
                }
            }
        } else {
            for (Node v = getSelectedNodes().getFirstElement(); v != null; v = getSelectedNodes().getNextElement(v))
                if (getLabel(v) != null)
                    selectedLabels.add(getLabel(v));
        }
        return selectedLabels;

    }

    public boolean isNodeLabelIds() {
        return nodeLabelIds;
    }

    public void setNodeLabelIds(boolean nodeLabelIds) {
        this.nodeLabelIds = nodeLabelIds;
    }

    public boolean isNodeLabelAssigned() {
        return nodeLabelAssigned;
    }

    public void setNodeLabelAssigned(boolean nodeLabelAssigned) {
        this.nodeLabelAssigned = nodeLabelAssigned;
    }

    public boolean isNodeLabelSummarized() {
        return nodeLabelSummarized;
    }

    public void setNodeLabelSummarized(boolean nodeLabelSummarized) {
        this.nodeLabelSummarized = nodeLabelSummarized;
    }

    public PhyloTree getTree() {
        return (PhyloTree) getGraph();
    }

    /**
     * get all non-root nodes of degree 2
     *
     * @return all none-root nodes of degree 2
     */
    NodeSet getDegree2Nodes() {
        NodeSet nodes = new NodeSet(getTree());

        for (Node v = getTree().getFirstNode(); v != null; v = v.getNext())
            if (v.getInDegree() == 1 && v.getOutDegree() == 1)
                nodes.add(v);

        return nodes;
    }

    /**
     * zoom to selected nodes
     */
    public void zoomToSelection() {
        Rectangle2D worldRect = null;

        for (Node v : getSelectedNodes()) {
            if (getLocation(v) != null) {
                if (worldRect == null)
                    worldRect = new Rectangle2D.Double(getLocation(v).getX(), getLocation(v).getY(), 0, 0);
                else
                    worldRect.add(getLocation(v));
            }
        }
        if (worldRect != null) {
            worldRect.setRect(worldRect.getX(), worldRect.getY(), Math.max(800, worldRect.getWidth()), Math.max(800, worldRect.getHeight()));
            //  if (getSelectedNodes().size() > 1)
            //      trans.fitToSize(worldRect, getScrollPane().getViewport().getExtentSize());
            Rectangle deviceRect = new Rectangle();
            trans.w2d(worldRect, deviceRect);
            deviceRect.y -= 40;
            deviceRect.height -= 40;
            scrollRectToVisible(deviceRect);
        }
    }

    public void scrollToNode(final Node v) {
        final Runnable runnable = () -> {
            final Rectangle rect = getNV(v).getLabelRect(trans);
            if (rect != null) {
                rect.x -= 25;
                rect.y -= 25;
                rect.width += 50;
                rect.height += 50;
                scrollRectToVisible(rect);
                repaint();
            }
        };
        if (SwingUtilities.isEventDispatchThread())
            runnable.run();
        else SwingUtilities.invokeLater(runnable);
    }

    public String getShowLegend() {
        return showLegend;
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

    public boolean isDrawLeavesOnly() {
        return drawLeavesOnly;
    }

    public void setDrawLeavesOnly(boolean drawLeavesOnly) {
        this.drawLeavesOnly = drawLeavesOnly;
    }

    /**
     * overide GraphView.centerGraph because we don't what to center graph on resize
     */
    public void centerGraph() {
    }

    /**
     * override  GraphView.fitGraphToWindow
     */
    public void fitGraphToWindow() {
        Dimension size = getScrollPane().getSize();
        trans.fitToSize(new Dimension(size.width, size.height));
    }

    /**
     * gets the current graphview
     *
     * @return graphview
     */
    public GraphView getGraphView() {
        return this;
    }

    /**
     * get set of ids of all nodes whose format have been changed
     *
     * @return ids of dirty nodes
     */
    public Set<Integer> getDirtyNodeIds() {
        return dirtyNodeIds;
    }

    /**
     * get set of ids of all dirty edges
     *
     * @return dirty edges
     */
    public Set<Pair<Integer, Integer>> getDirtyEdgeIds() {
        return dirtyEdgeIds;
    }

    abstract public JFrame getFrame();

    /**
     * recursively print a summary
     *
     * @param selectedNodes
     * @param v
     * @param indent
     */
    public void listSummaryRec(NodeSet selectedNodes, Node v, int indent, Writer outs) throws IOException {
        System.err.println("Not implemented");
    }

    Set<Integer> getPreviousNodeIdsOfInterest() {
        return previousNodeIdsOfInterest;
    }

    public void setPreviousNodeIdsOfInterest(Collection<Integer> previousNodeIdsOfInterest) {
        this.previousNodeIdsOfInterest.clear();
        if (previousNodeIdsOfInterest != null)
            this.previousNodeIdsOfInterest.addAll(previousNodeIdsOfInterest);

    }

    public Collection<Integer> getSelectedNodeIds() {
        Set<Integer> set = new HashSet<>();
        for (Node v = getSelectedNodes().getFirstElement(); v != null; v = getSelectedNodes().getNextElement(v)) {
            set.add((Integer) v.getInfo());
        }
        return set;
    }

    abstract public boolean isShowFindToolBar();

    abstract public void setShowFindToolBar(boolean showFindToolBar);

    abstract public SearchManager getSearchManager();

    public DiagramType getDrawerType() {
        return drawerType;
    }

    public void setDrawerType(String drawerName) {
        for (DiagramType aType : DiagramType.values()) {
            if (drawerName.equalsIgnoreCase(aType.toString())) {
                drawerType = aType;
                break;
            }
        }
        ProgramProperties.put("DrawerType" + getClassName(), drawerName);
    }

    public void updateTree() {
    }

    public NodeDrawer getNodeDrawer() {
        return nodeDrawer;
    }

    public Director getDir() {
        return dir;
    }

    public Document getDocument() {
        return doc;
    }

    public LegendPanel getLegendPanel() {
        return legendPanel;
    }

    public JScrollPane getLegendScrollPane() {
        return legendScrollPane;
    }

    /**
     * are there any reads on the selected (or all) nodes?
     *
     * @return true, if nodes with reads are selected
     */
    public boolean hasComparableData() {
        if (doc.getNumberOfSamples() >= 2) {
            for (Node v = getGraph().getFirstNode(); v != null; v = v.getNext()) {
                if (getSelected(v) || getSelectedNodes().size() == 0) {
                    NodeData data = (getNodeData(v));
                    if (data.getCountSummarized() > 0)
                        return true;
                }
            }
        }
        return false;
    }

    public void setupNodeLabels(boolean b) {
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public String getClassName() {
        return "ViewerBase";
    }
}
