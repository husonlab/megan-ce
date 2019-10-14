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
package megan.assembly;

import jloda.graph.*;
import jloda.swing.commands.CommandManager;
import jloda.swing.commands.ICommand;
import jloda.swing.director.ProjectManager;
import jloda.swing.graphview.EdgeActionAdapter;
import jloda.swing.graphview.EdgeView;
import jloda.swing.graphview.GraphView;
import jloda.swing.graphview.NodeActionAdapter;
import jloda.swing.window.MenuBar;
import jloda.swing.window.MenuConfiguration;
import jloda.util.APoint2D;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import jloda.util.ProgressPercentage;
import megan.assembly.commands.SelectFromPreviousWindowCommand;
import megan.core.Director;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * simple viewer for overlap graph
 * Daniel Huson, 5.2015
 */
public class OverlapGraphViewer {
    final private Director dir;
    final private Graph overlapGraph;
    final private Node[][] paths;
    final private GraphView graphView;
    final private NodeArray<String> node2ReadNameMap;

    /**
     * constructor
     *
     * @param overlapGraph
     */
    public OverlapGraphViewer(Director dir, final Graph overlapGraph, final NodeArray<String> node2ReadNameMap, Node[][] paths) {
        this.dir = dir;
        this.overlapGraph = overlapGraph;
        this.node2ReadNameMap = node2ReadNameMap;
        this.paths = paths;

        graphView = new GraphView(overlapGraph);
        graphView.getScrollPane().getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        graphView.getScrollPane().setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        graphView.getScrollPane().setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        graphView.getScrollPane().addKeyListener(graphView.getGraphViewListener());

        graphView.setSize(800, 800);
        graphView.setAllowMoveNodes(true);
        graphView.setAllowRubberbandNodes(true);
        graphView.setAutoLayoutLabels(true);
        graphView.setFixedNodeSize(true);
        graphView.setMaintainEdgeLengths(false);
        graphView.setAllowEdit(false);
        graphView.setCanvasColor(Color.WHITE);

        graphView.getScrollPane().addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                final Dimension ps = graphView.trans.getPreferredSize();
                int x = Math.max(ps.width, graphView.getScrollPane().getWidth() - 20);
                int y = Math.max(ps.height, graphView.getScrollPane().getHeight() - 20);
                ps.setSize(x, y);
                graphView.setPreferredSize(ps);
                graphView.getScrollPane().getViewport().setViewSize(new Dimension(x, y));
                graphView.repaint();
            }
        });

        graphView.removeAllNodeActionListeners();
        graphView.removeAllEdgeActionListeners();

        graphView.addNodeActionListener(new NodeActionAdapter() {
            public void doClick(NodeSet nodes, int clicks) {
                ProjectManager.getPreviouslySelectedNodeLabels().clear();
                for (Node v : nodes) {
                    ProjectManager.getPreviouslySelectedNodeLabels().add(node2ReadNameMap.get(v));
                }

                if (clicks >= 2) {
                    graphView.selectedNodes.clear();
                    graphView.selectedEdges.clear();


                    final EdgeSet edgesToSelect = new EdgeSet(overlapGraph);
                    final NodeSet nodesToSelect = new NodeSet(overlapGraph);
                    final Stack<Node> stack = new Stack<>();
                    stack.addAll(nodes);
                    while (stack.size() > 0) {
                        Node v = stack.pop();
                        for (Edge e = v.getFirstAdjacentEdge(); e != null; e = v.getNextAdjacentEdge(e)) {
                            if (clicks == 3 || graphView.getLineWidth(e) == 2) {
                                edgesToSelect.add(e);
                                Node w = e.getOpposite(v);
                                if (!nodesToSelect.contains(w)) {
                                    stack.push(w);
                                    nodesToSelect.add(w);
                                }
                            }
                        }
                    }
                    graphView.selectedNodes.addAll(nodesToSelect);
                    graphView.selectedEdges.addAll(edgesToSelect);
                    graphView.repaint();
                }
            }

            public void doSelect(NodeSet nodes) {
                for (Node v : nodes) {
                    graphView.setLabel(v, node2ReadNameMap.get(v));
                }

                graphView.selectedEdges.clear();
                final EdgeSet edgesToSelect = new EdgeSet(overlapGraph);
                final NodeSet nodesToSelect = new NodeSet(overlapGraph);
                final Stack<Node> stack = new Stack<>();
                stack.addAll(nodes);
                while (stack.size() > 0) {
                    Node v = stack.pop();
                    for (Edge e = v.getFirstAdjacentEdge(); e != null; e = v.getNextAdjacentEdge(e)) {
                        if (graphView.getLineWidth(e) == 2) {
                            edgesToSelect.add(e);
                            Node w = e.getOpposite(v);
                            if (!nodesToSelect.contains(w)) {
                                stack.push(w);
                                nodesToSelect.add(w);
                            }
                        }
                    }
                }
                graphView.selectedEdges.addAll(edgesToSelect);
            }

            public void doDeselect(NodeSet nodes) {
                for (Node v : nodes) {
                    graphView.setLabel(v, null);
                }
            }
        });

        graphView.addEdgeActionListener(new EdgeActionAdapter() {
            public void doSelect(EdgeSet edges) {
                for (Edge e : edges) {
                    graphView.setLabel(e, "" + e.getInfo());
                }

                NodeSet nodes = new NodeSet(overlapGraph);
                for (Edge e : edges) {
                    nodes.add(e.getSource());
                    nodes.add(e.getTarget());
                }
                graphView.fireDoSelect(nodes);
            }

            public void doDeselect(EdgeSet edges) {
                for (Edge e : edges) {
                    graphView.setLabel(e, null);
                }
            }
        });

    }


    /**
     * build a graph view for the overlap graph
     *
     * @return graphView
     */
    public void apply(ProgressListener progress) throws CanceledException {
        progress.setSubtask("Computing graph layout");
        progress.setMaximum(-1);
        progress.setProgress(0);

        Set<Edge> pathEdges = new HashSet<>();
        if (paths != null) {
            for (Node[] path : paths) {
                for (int i = 0; i < path.length - 1; i++) {
                    Node v = path[i];
                    for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                        if (e.getTarget() == path[i + 1])
                            pathEdges.add(e);
                    }
                }
            }
        }

        // compute simple layout:
        final FruchtermanReingoldLayout fruchtermanReingoldLayout = new FruchtermanReingoldLayout(overlapGraph, null);
        NodeArray<APoint2D> coordinates = new NodeArray<>(overlapGraph);
        fruchtermanReingoldLayout.apply(1000, coordinates);
        for (Node v = overlapGraph.getFirstNode(); v != null; v = v.getNext()) {
            graphView.setLocation(v, coordinates.get(v).getX(), coordinates.get(v).getY());
            graphView.setHeight(v, 5);
            graphView.setWidth(v, 5);
        }

        for (Edge e = overlapGraph.getFirstEdge(); e != null; e = e.getNext()) {
            graphView.setDirection(e, EdgeView.DIRECTED);
            if (pathEdges.contains(e)) {
                graphView.setLineWidth(e, 2);
                graphView.setLineWidth(e.getSource(), 2);
                graphView.setLineWidth(e.getTarget(), 2);
            }
        }

        JFrame frame = new JFrame("Assembly Graph");
        graphView.setFrame(frame);
        frame.setSize(graphView.getSize());
        frame.setLocation(100, 100);
        frame.addKeyListener(graphView.getGraphViewListener());

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(graphView.getScrollPane(), BorderLayout.CENTER);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        CommandManager commandManager = new CommandManager(dir, graphView, new String[]{"megan.commands"});

        commandManager.addCommands(this, Collections.singletonList(new SelectFromPreviousWindowCommand()), true);

        MenuConfiguration menuConfig = GUIConfiguration.getMenuConfiguration();

        MenuBar menuBar = new MenuBar(this, menuConfig, commandManager);


        frame.setJMenuBar(menuBar);

        Rectangle2D bbox = graphView.getBBox();
        graphView.trans.setCoordinateRect(bbox);

        // show the frame:
        frame.setVisible(true);
        graphView.getScrollPane().revalidate();
        graphView.centerGraph();

        if (progress instanceof ProgressPercentage)
            ((ProgressPercentage) progress).reportTaskCompleted();
    }

    /**
     * get the graphview
     *
     * @return graph view
     */
    public GraphView getGraphView() {
        return graphView;
    }

    public NodeArray<String> getNode2ReadNameMap() {
        return node2ReadNameMap;
    }
}
