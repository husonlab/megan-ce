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

package megan.chart.cluster;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.swing.graphview.PhyloTreeView;
import jloda.util.Correlation;
import jloda.util.ProgramProperties;
import jloda.util.Table;
import megan.chart.gui.ChartSelection;
import megan.chart.gui.SelectionGraphics;
import megan.clusteranalysis.tree.Distances;
import megan.clusteranalysis.tree.Taxa;
import megan.clusteranalysis.tree.UPGMA;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * do clustering of series or class names
 * Created by huson on 3/9/16.
 */
public class ClusteringTree {

    public enum TYPE {SERIES, CLASSES, ATTRIBUTES}

    public enum SIDE {LEFT, RIGHT, BOTTOM, TOP}

    private final ArrayList<String> labelOrder;
    private final PhyloTree tree;
    private final PhyloTreeView treeView;
    private Rectangle previousRectangle;

    private TYPE type;
    private SIDE rootSide;

    private ChartSelection chartSelection;

    private final SelectionGraphics<Edge> selectionGraphics;

    private boolean inUpdate;

    /**
     * constructor
     */
    public ClusteringTree(TYPE type, SIDE rootSide) {
        this.type = type;
        this.rootSide = rootSide;

        labelOrder = new ArrayList<>();
        tree = new PhyloTree();
        treeView = new PhyloTreeView(tree);

        selectionGraphics = new SelectionGraphics<>(null);
    }

    public void clear() {
        tree.deleteAllNodes();
        labelOrder.clear();
    }

    public SIDE getRootSide() {
        return rootSide;
    }

    public void setRootSide(SIDE rootSide) {
        this.rootSide = rootSide;
    }

    /**
     * update clustering
     *
     * @param seriesAndClass2Value
     */
    public void updateClustering(Table<String, String, Double> seriesAndClass2Value) {
        if (!inUpdate) {
            try {
                inUpdate = true;

                labelOrder.clear();
                previousRectangle = null;

                final Taxa taxa;
                final Distances distances;
                switch (type) {
                    case SERIES: {
                        final String[] series = seriesAndClass2Value.rowKeySet().toArray(new String[0]);

                        taxa = new Taxa();
                        for (String seriesName : series)
                            taxa.add(seriesName);
                        distances = new Distances(taxa.size());

                        for (int i = 0; i < series.length; i++) {
                            for (int j = 0; j < series.length; j++) {
                                distances.set(i + 1, j + 1, computeCorrelationDistanceBetweenSeries(series[i], series[j], seriesAndClass2Value));
                            }
                        }
                        break;
                    }
                    case CLASSES: {
                        final String[] classes = seriesAndClass2Value.columnKeySet().toArray(new String[0]);

                        taxa = new Taxa();
                        for (String className : classes)
                            taxa.add(className);
                        distances = new Distances(taxa.size());

                        for (int i = 0; i < classes.length; i++) {
                            for (int j = 0; j < classes.length; j++) {
                                distances.set(i + 1, j + 1, computeCorrelationDistanceBetweenClasses(classes[i], classes[j], seriesAndClass2Value));
                            }
                        }
                        break;
                    }
                    default:
                        throw new RuntimeException("Invalid case: " + type.toString());
                }

                treeView.getGraph().clear();
                UPGMA.apply(taxa, distances, treeView);
                flipCoordinates(treeView, rootSide);
                labelOrder.addAll(getLabelOrder(treeView));
                // if(rootSide==SIDE.RIGHT)
                //   Basic.reverseList(labelOrder);
            } finally {
                inUpdate = false;
            }
        }
    }

    /**
     * update clustering
     *
     * @param labels
     * @param matrix
     */
    public void updateClustering(String[] labels, Table<String, String, Float> matrix) {
        if (!inUpdate) {
            try {
                inUpdate = true;
                labelOrder.clear();
                treeView.getGraph().clear();
                previousRectangle = null;

                if (labels.length > 0) {
                    final Taxa taxa = new Taxa();
                    for (String label : labels)
                        taxa.add(label);

                    if (labels.length == 1) {
                        final Node root = treeView.getPhyloTree().newNode();
                        treeView.getPhyloTree().setRoot(root);
                        treeView.setLabel(root, labels[0]);
                        labelOrder.addAll(getLabelOrder(treeView));
                    } else {
                        final Distances distances = new Distances(taxa.size());

                        for (int i = 0; i < labels.length; i++) {
                            final float[] iValues = getValuesRow(labels[i], matrix);
                            for (int j = i + 1; j < labels.length; j++) {
                                final float[] jValues = getValuesRow(labels[j], matrix);
                                distances.set(i + 1, j + 1, computeCorrelationDistances(iValues.length, iValues, jValues));
                            }
                        }

                        UPGMA.apply(taxa, distances, treeView);
                        // treeView.topologicallySortTreeLexicographically();
                        // UPGMA.embedTree(treeView);
                        flipCoordinates(treeView, rootSide);
                        labelOrder.addAll(getLabelOrder(treeView));
                    }
                    // if(rootSide==SIDE.RIGHT)
                    //   Basic.reverseList(labelOrder);

                }
            } finally {
                inUpdate = false;
            }
        }
    }

    private float[] getValuesRow(String rowKey, Table<String, String, Float> matrix) {
        final float[] array = new float[matrix.columnKeySet().size()];
        final Map<String, Float> row = matrix.row(rowKey);
        int i = 0;
        for (String colKey : row.keySet()) {
            array[i++] = matrix.get(rowKey, colKey);
        }
        return array;

    }

    /**
     * compute order of labels
     *
     * @param treeView
     * @return labels as ordered in tree
     */
    private static ArrayList<String> getLabelOrder(PhyloTreeView treeView) {
        final ArrayList<String> order = new ArrayList<>();

        final PhyloTree tree = treeView.getPhyloTree();
        if (tree.getRoot() != null) {
            final Stack<Node> stack = new Stack<>();
            stack.add(tree.getRoot());
            while (stack.size() > 0) {
                Node v = stack.pop();
                if (v.getOutDegree() == 0)
                    order.add(treeView.getLabel(v));
                else
                    for (Edge e = v.getLastOutEdge(); e != null; e = v.getPrevOutEdge(e)) {
                        stack.push(e.getTarget());
                    }
            }
        }
        return order;
    }

    /**
     * compute correlation distance between two series
     *
     * @param seriesA
     * @param seriesB
     * @param seriesAndClass2Value
     * @return distance
     */
    private static double computeCorrelationDistanceBetweenSeries(String seriesA, String seriesB, Table<String, String, Double> seriesAndClass2Value) {
        final Set<String> classes = seriesAndClass2Value.columnKeySet();
        final ArrayList<Double> xValues = new ArrayList<>(classes.size());
        final ArrayList<Double> yValues = new ArrayList<>(classes.size());

        for (String className : classes) {
            xValues.add(seriesAndClass2Value.get(seriesA, className));
            yValues.add(seriesAndClass2Value.get(seriesB, className));
        }
        return 1 - Correlation.computePersonsCorrelationCoefficent(classes.size(), xValues, yValues);
    }

    /**
     * compute correlation distance between two series
     *
     * @param seriesA
     * @param seriesB
     * @return distance
     */
    private static double computeCorrelationDistances(int n, float[] seriesA, float[] seriesB) {
        return 1 - Correlation.computePersonsCorrelationCoefficent(n, seriesA, seriesB);
    }

    /**
     * compute correlation distance between two classes
     *
     * @param classA
     * @param classB
     * @param seriesAndClass2Value
     * @return distance
     */
    private static double computeCorrelationDistanceBetweenClasses(String classA, String classB, Table<String, String, Double> seriesAndClass2Value) {
        final Set<String> series = seriesAndClass2Value.rowKeySet();
        final ArrayList<Double> xValues = new ArrayList<>(series.size());
        final ArrayList<Double> yValues = new ArrayList<>(series.size());

        for (String seriesName : series) {
            xValues.add(seriesAndClass2Value.get(seriesName, classA));
            yValues.add(seriesAndClass2Value.get(seriesName, classB));
        }
        return 1 - Correlation.computePersonsCorrelationCoefficent(series.size(), xValues, yValues);
    }


    public ArrayList<String> getLabelOrder() {
        return (ArrayList) labelOrder.clone();
    }

    public PhyloTreeView getPanel() {
        return treeView;
    }

    /**
     * paint the tree
     *
     * @param gc
     * @param rect
     */
    public void paint(Graphics2D gc, Rectangle rect) {
        try {
            if (gc instanceof SelectionGraphics) {
                final SelectionGraphics sgc = (SelectionGraphics) gc;
                select(rect, sgc.getSelectionRectangle(), sgc.getMouseClicks());
            } else if (!inUpdate) {
                doPaint(gc, rect);
            }
        } catch (Exception ex) {
            // Basic.caught(ex);
        }
    }

    /**
     * paint the tree
     *
     * @param gc
     * @param rect
     */
    private void doPaint(Graphics2D gc, Rectangle rect) {
        if (!(gc instanceof SelectionGraphics))
            selectEdgesAbove();

        gc.setStroke(new BasicStroke(1));

        if (!rect.equals(previousRectangle)) {
            previousRectangle = rect;
            fitToRectangle(treeView, rect);
        }
        for (Edge e = tree.getFirstEdge(); e != null; e = tree.getNextEdge(e)) {
            try {
                if (inUpdate)
                    break;
                Point2D a = treeView.getLocation(e.getSource());
                Point2D b = treeView.getLocation(e.getTarget());

                if (treeView.getSelected(e))
                    gc.setColor(ProgramProperties.SELECTION_COLOR_DARKER);
                else
                    gc.setColor(Color.BLACK);

                if (gc instanceof SelectionGraphics)
                    ((SelectionGraphics) gc).setCurrentItem(e);

                final int ax = (int) Math.round(a.getX());
                final int ay = (int) Math.round(a.getY());
                gc.fillOval(ax - 1, ay - 1, 3, 3);

                switch (rootSide) {
                    case BOTTOM:
                    case TOP:
                        gc.drawLine(ax, ay, (int) Math.round(b.getX()), ay);
                        gc.drawLine((int) Math.round(b.getX()), ay, (int) Math.round(b.getX()), (int) Math.round(b.getY()));
                        break;
                    default:
                    case RIGHT:
                    case LEFT:
                        gc.drawLine(ax, ay, (int) Math.round(a.getX()), (int) Math.round(b.getY()));
                        gc.drawLine(ax, (int) Math.round(b.getY()), (int) Math.round(b.getX()), (int) Math.round(b.getY()));
                        break;
                }
            /*
            if (e.getTarget().getOutDegree() == 0)
                gc.drawString(treeView.getLabel(e.getTarget()), (int) b.getX(), (int) b.getY());
                */
            } finally {
                if (gc instanceof SelectionGraphics)
                    ((SelectionGraphics) gc).clearCurrentItem();
            }
        }
    }

    /**
     * select series or classes
     *
     * @param rect
     */
    private void select(Rectangle rect, Rectangle selectionRect, int mouseClicks) {
        if (selectionRect == null || chartSelection == null || mouseClicks != 1)
            return;

        selectionGraphics.setSelectionRectangle(selectionRect);
        selectionGraphics.getSelectedItems().clear();
        doPaint(selectionGraphics, rect);
        final Collection<Edge> hitEdges = selectionGraphics.getSelectedItems();

        if (hitEdges.size() > 0) {
            final Set<Node> seen = new HashSet<>();
            final Stack<Node> stack = new Stack<>();
            for (Edge e : hitEdges) {
                stack.add(e.getTarget());
            }
            while (stack.size() > 0) {
                final Node v = stack.pop();
                if (v.getOutDegree() == 0) {
                    if (type == TYPE.SERIES)
                        chartSelection.setSelectedSeries(treeView.getLabel(v), true);
                    else if (type == TYPE.CLASSES)
                        chartSelection.setSelectedClass(treeView.getLabel(v), true);
                    else if (type == TYPE.ATTRIBUTES)
                        chartSelection.setSelectedAttribute(treeView.getLabel(v), true);
                } else {
                    for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                        if (!seen.contains(e.getTarget())) {
                            stack.push(e.getTarget());
                            seen.add(e.getTarget());
                        }
                    }
                }
            }
        }
    }

    private void selectEdgesAbove() {
        treeView.getSelectedEdges().clear();
        treeView.getSelectedNodes().clear();

        if (tree.getRoot() != null) {
            final Map<Node, Integer> below = new HashMap<>();
            final int countBelow = countSelectedBelowRec(tree.getRoot(), below);
            if (countBelow > 0)
                selectBelowRec(tree.getRoot(), below, countBelow);
        }
    }

    private int countSelectedBelowRec(Node v, Map<Node, Integer> below) {
        if (v.getOutDegree() == 0) {
            if (type == TYPE.SERIES)
                below.put(v, chartSelection.isSelectedSeries(treeView.getLabel(v)) ? 1 : 0);
            else if (type == TYPE.CLASSES)
                below.put(v, chartSelection.isSelectedClass(treeView.getLabel(v)) ? 1 : 0);
            else
                below.put(v, chartSelection.isSelectedAttribute(treeView.getLabel(v)) ? 1 : 0);
        } else {
            int count = 0;
            for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                count += countSelectedBelowRec(e.getTarget(), below);
            }
            below.put(v, count);
        }
        return below.get(v);
    }

    private void selectBelowRec(Node v, Map<Node, Integer> below, final int count) {
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            final Node w = e.getTarget();
            final Integer countW = below.get(w);
            if (countW != null && countW > 0) {
                selectBelowRec(w, below, count);
                if (countW == count)
                    return; // have seen all below
                // System.err.println("Select "+e);
                treeView.setSelected(e, true);
                treeView.setSelected(v, true);
                treeView.setSelected(w, true);
            }
        }
    }

    /**
     * flip coordinates so as to fit the specified root side
     *
     * @param treeView
     * @param rootSide
     */
    private static void flipCoordinates(final PhyloTreeView treeView, final SIDE rootSide) {
        final PhyloTree tree = treeView.getPhyloTree();
        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            final Point2D loc = treeView.getLocation(v);
            switch (rootSide) {
                case TOP:
                    treeView.setLocation(v, loc.getY(), loc.getX());
                    break;
                case BOTTOM:
                    treeView.setLocation(v, loc.getY(), -loc.getX());
                    break;
                case RIGHT:
                    treeView.setLocation(v, -loc.getX(), loc.getY());
                    break;
                default:
                case LEFT:
                    break;
            }
        }
    }

    /**
     * fit coordinates into rect
     *
     * @param treeView
     * @param rect
     */
    private static void fitToRectangle(final PhyloTreeView treeView, final Rectangle rect) {
        final PhyloTree tree = treeView.getPhyloTree();

        double minX = Integer.MAX_VALUE;
        double minY = Integer.MAX_VALUE;
        double maxX = Integer.MIN_VALUE;
        double maxY = Integer.MIN_VALUE;

        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            final Point2D loc = treeView.getLocation(v);
            minX = Math.min(minX, loc.getX());
            minY = Math.min(minY, loc.getY());
            maxX = Math.max(maxX, loc.getX());
            maxY = Math.max(maxY, loc.getY());
        }

        final double mX = ((maxX - minX) != 0 ? rect.getWidth() / (maxX - minX) : 0);
        final double mY = ((maxY - minY) != 0 ? rect.getHeight() / (maxY - minY) : 0);

        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            final Point2D loc = treeView.getLocation(v);
            treeView.setLocation(v, rect.getX() + mX * (loc.getX() - minX), rect.getY() + mY * (loc.getY() - minY));
        }
    }

    public ChartSelection getChartSelection() {
        return chartSelection;
    }

    public void setChartSelection(ChartSelection chartSelection) {
        this.chartSelection = chartSelection;
    }

    /**
     * does the tree have exactly one selected subtree?
     *
     * @return true, if a complete non-empty subtree is selected
     */
    public boolean hasSelectedSubTree() {
        boolean foundASelectedRoot = false;
        if (treeView.getNumberSelectedNodes() > 1) {
            for (Node v : treeView.getSelectedNodes()) {
                if (v.getInDegree() == 0 || !treeView.getSelected(v.getFirstInEdge().getSource())) {
                    if (foundASelectedRoot)
                        return false; // has more than one selected root
                    else
                        foundASelectedRoot = true;
                }
            }
        }
        return foundASelectedRoot;
    }

    /**
     * rotate all currently selected subtrees
     */
    public void rotateSelectedSubTree() {
        boolean changed = false;
        if (treeView.getNumberSelectedNodes() > 1) {
            for (Node v : treeView.getSelectedNodes()) {
                if (v.getInDegree() == 0 || !treeView.getSelected(v.getFirstInEdge().getSource())) {
                    Stack<Node> stack = new Stack<>();
                    stack.push(v);
                    while (stack.size() > 0) {
                        Node w = stack.pop();
                        if (w.getOutDegree() > 1) {
                            w.reverseOrderAdjacentEdges();
                            changed = true;
                        }
                        if (w.getOutDegree() > 0) {
                            for (Edge e = w.getFirstOutEdge(); e != null; e = w.getNextOutEdge(e)) {
                                stack.push(e.getTarget());
                            }
                        }
                    }
                }
            }
        }
        if (changed) {
            labelOrder.clear();
            labelOrder.addAll(getLabelOrder(treeView));
            UPGMA.embedTree(treeView);
            flipCoordinates(treeView, rootSide);
            previousRectangle = null;
        }
    }

    public void setType(TYPE type) {
        this.type = type;
    }
}
