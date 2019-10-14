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
package megan.clusteranalysis.gui;

import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.phylo.PhyloTree;
import jloda.swing.director.IDirector;
import jloda.swing.find.IObjectSearcher;
import jloda.swing.find.NodeLabelSearcher;
import jloda.swing.find.SearchManager;
import jloda.swing.graphview.ITransformChangeListener;
import jloda.swing.graphview.NodeActionAdapter;
import jloda.swing.graphview.NodeView;
import jloda.swing.graphview.Transform;
import jloda.util.Basic;
import megan.clusteranalysis.ClusterViewer;
import megan.viewer.ViewerBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

/**
 * tab that shows a plot of the data
 * Daniel Huson, 5.2010
 */
public class TreeTabBase extends JPanel {
    private final ViewerBase graphView;
    final ClusterViewer clusterViewer;
    private final IObjectSearcher searcher;


    /**
     * constructor
     *
     * @param clusterViewer
     */
    public TreeTabBase(final ClusterViewer clusterViewer) {
        this.clusterViewer = clusterViewer;

        graphView = new ViewerBase(clusterViewer.getDir(), new PhyloTree(), false) {
            public JFrame getFrame() {
                return null;
            }

            public void fitGraphToWindow() {
                Dimension size = getScrollPane().getSize();
                if (size.getWidth() == 0 || size.getHeight() == 0) {
                    try {

                        Runnable runnable = () -> {
                            TreeTabBase.this.validate();
                            getScrollPane().validate();
                        };
                        if (SwingUtilities.isEventDispatchThread())
                            runnable.run(); // already in the swing thread, just run
                        else
                            SwingUtilities.invokeAndWait(runnable);
                    } catch (InterruptedException | InvocationTargetException e) {
                        Basic.caught(e);
                    }
                    size = getScrollPane().getSize();
                }
                if (getGraph().getNumberOfNodes() > 0) {
                    // need more width than in GraphView
                    trans.fitToSize(new Dimension(Math.max(100, size.width - 300), Math.max(50, size.height - 200)));
                } else
                    trans.fitToSize(new Dimension(0, 0));
                centerGraph();
            }

            public void centerGraph() {
                JScrollBar hScrollBar = getScrollPane().getHorizontalScrollBar();
                hScrollBar.setValue((hScrollBar.getMaximum() - hScrollBar.getModel().getExtent() + hScrollBar.getMinimum()) / 2);
                JScrollBar vScrollBar = getScrollPane().getVerticalScrollBar();
                vScrollBar.setValue((vScrollBar.getMaximum() - vScrollBar.getModel().getExtent() + vScrollBar.getMinimum()) / 2);
            }

            public boolean isShowFindToolBar() {
                return clusterViewer.isShowFindToolBar();
            }

            public void setShowFindToolBar(boolean showFindToolBar) {
                clusterViewer.setShowFindToolBar(showFindToolBar);
            }

            public java.util.Collection<Integer> getSelectedIds() {
                return graphView.getSelectedIds();
            }

            public SearchManager getSearchManager() {
                return clusterViewer.getSearchManager();
            }

            public void resetViews() {
            }
        };
        graphView.setCanvasColor(Color.WHITE);

        graphView.trans.setLockXYScale(false);
        graphView.trans.setTopMargin(80);
        graphView.trans.setBottomMargin(80);
        graphView.trans.setLeftMargin(200);
        graphView.trans.setRightMargin(300);

        graphView.setDrawScaleBar(true);

        graphView.setFixedNodeSize(true);
        graphView.setAllowMoveInternalEdgePoints(false);
        graphView.setAllowMoveNodes(false);
        graphView.setAllowMoveInternalEdgePoints(false);

        graphView.setFixedNodeSize(true);
        graphView.setAutoLayoutLabels(false);
        graphView.setAllowEdit(false);

        setLayout(new BorderLayout());
        add(graphView.getScrollPane(), BorderLayout.CENTER);

        this.setPreferredSize(new Dimension(0, 0));

        graphView.getScrollPane().getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        graphView.getScrollPane().setWheelScrollingEnabled(true);
        graphView.getScrollPane().setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        graphView.trans.removeAllChangeListeners();
        graphView.trans.addChangeListener(trans -> graphView.recomputeMargins());

        graphView.getScrollPane().addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent event) {
                {
                    if (graphView.getScrollPane().getSize().getHeight() > 400 && graphView.getScrollPane().getSize().getWidth() > 400)
                        graphView.fitGraphToWindow();
                    else
                        graphView.trans.fireHasChanged();
                }
            }
        });


        graphView.addNodeActionListener(new NodeActionAdapter() {
            public void doMoveLabel(NodeSet nodes) {
                for (Node v = nodes.getFirstElement(); v != null; v = nodes.getNextElement(v)) {
                    graphView.setLabelLayout(v, NodeView.USER);
                }
            }

            public void doSelect(NodeSet nodes) {
                Set<String> samples = new HashSet<>();
                for (Node v : nodes) {
                    String label = graphView.getTree().getLabel(v);
                    if (label != null)
                        samples.add(label);
                }
                graphView.getDocument().getSampleSelection().setSelected(samples, true);
                clusterViewer.updateView(IDirector.ENABLE_STATE);
            }

            public void doDeselect(NodeSet nodes) {
                Set<String> samples = new HashSet<>();
                for (Node v : nodes) {
                    String label = graphView.getTree().getLabel(v);
                    if (label != null)
                        samples.add(label);
                }
                graphView.getDocument().getSampleSelection().setSelected(samples, false);
                clusterViewer.updateView(IDirector.ENABLE_STATE);
            }
        });

        searcher = new NodeLabelSearcher(clusterViewer.getFrame(), "PCoA", graphView);
    }

    /**
     * get the associated graph view
     *
     * @return graph view
     */
    public ViewerBase getGraphView() {
        return graphView;
    }

    public void clear() {
        graphView.getGraph().deleteAllNodes();
    }

    /**
     * this tab has been selected
     */
    public void activate() {
    }

    /**
     * this tab has been deselected
     */
    public void deactivate() {

    }

    public void updateView(String what) {
    }


    /**
     * zoom to fit
     */
    public void zoomToFit() {
        graphView.fitGraphToWindow();
    }

    /**
     * zoom to selection
     */
    public void zoomToSelection() {
        graphView.zoomToSelection();
    }

    /**
     * gets the searcher associated with this tab
     *
     * @return searcher
     */
    public IObjectSearcher getSearcher() {
        return searcher;
    }

    public boolean isApplicable() {
        return true;
    }

    public boolean needsUpdate() {
        return graphView.getGraph().getNumberOfNodes() == 0;
    }
}
