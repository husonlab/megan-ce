/*
 *  Copyright (C) 2018 Daniel H. Huson
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
package megan.chart.drawers;

import jloda.graph.*;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.chart.IChartDrawer;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * draws a co-occurrence graph
 * Daniel Huson, 6.2012
 */
public class CoOccurrenceDrawer extends BarChartDrawer implements IChartDrawer {
    public static final String NAME = "CoOccurrencePlot";

    private int maxRadius = ProgramProperties.get("COMaxRadius", 40);
    private final Graph graph;
    private final EdgeArray<Float> edgeValue;
    private final Color PositiveColor = new Color(0x3F861C);
    private final Color NegativeColor = new Color(0xFB6542);
    private final Rectangle2D boundingBox = new Rectangle2D.Double();

    private double minThreshold = ProgramProperties.get("COMinThreshold", 0.01d); // min percentage for class
    private int minProbability = ProgramProperties.get("COMinProbability", 70); // min co-occurrence probability in percent
    private int minPrevalence = ProgramProperties.get("COMinPrevalence", 10); // minimum prevalence in percent
    private int maxPrevalence = ProgramProperties.get("COMaxPrevalence", 90); // maximum prevalence in percent
    private boolean showAntiOccurring = ProgramProperties.get("COShowAntiOccurring", true);
    private boolean showCoOccurring = ProgramProperties.get("COShowCoOccurring", true);

    /**
     * constructor
     */
    public CoOccurrenceDrawer() {
        graph = new Graph();
        edgeValue = new EdgeArray<>(graph);
        setSupportedScalingTypes(ChartViewer.ScalingType.LINEAR);
    }

    /**
     * draw heat map with colors representing classes
     *
     * @param gc
     */
    public void drawChart(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);

        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;

        int x0 = leftMargin;
        int x1 = getWidth() - rightMargin;
        if (x0 >= x1)
            return;
        int leftRightLabelOverhang = (getWidth() > 200 ? 100 : 0);

        double drawWidth = (getWidth() - 2 * leftRightLabelOverhang);
        double drawHeight = (getHeight() - topMargin - 20 - 20); // minus extra 20 for bottom toolbar

        double factorX = drawWidth / boundingBox.getWidth();
        double factorY = drawHeight / boundingBox.getHeight();

        double dx = leftRightLabelOverhang - factorX * boundingBox.getMinX() + (drawWidth - factorX * boundingBox.getWidth()) / 2;
        double dy = topMargin + 20 - factorY * boundingBox.getMinY() + (drawHeight - factorY * boundingBox.getHeight()) / 2;

        Line2D line = new Line2D.Double();

        /*
        gc.setColor(Color.BLUE);

        gc.drawRect((int) (factorX * boundingBox.getX() + dx) + 3, (int) (factorY * boundingBox.getY() + dy) + 3,
                (int) (factorX * boundingBox.getWidth()) - 6, (int) (factorY * boundingBox.getHeight()) - 6);
        */

        gc.setColor(Color.BLACK);

        for (Edge e = graph.getFirstEdge(); e != null; e = graph.getNextEdge(e)) {
            Node v = e.getSource();
            Point2D pv = ((NodeData) v.getData()).getLocation();
            Node w = e.getTarget();
            Point2D pw = ((NodeData) w.getData()).getLocation();
            try {
                line.setLine(factorX * pv.getX() + dx, factorY * pv.getY() + dy, factorX * pw.getX() + dx, factorY * pw.getY() + dy);
                gc.setColor(edgeValue.get(e) > 0 ? PositiveColor : NegativeColor);
                gc.draw(line);
            } catch (Exception ex) {
                Basic.caught(ex);
            }
        }

        double maxPrevalence = 1;
        for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
            Integer prevalence = ((NodeData) v.getData()).getPrevalence();
            if (prevalence > maxPrevalence)
                maxPrevalence = prevalence;
        }

        // draw all nodes in white to mask edges
        for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
            Point2D pv = ((NodeData) v.getData()).getLocation();
            Integer prevalence = ((NodeData) v.getData()).getPrevalence();
            double value = 0;
            if (scalingType == ChartViewer.ScalingType.PERCENT) {
                value = prevalence / maxPrevalence;
            } else if (scalingType == ChartViewer.ScalingType.LOG) {
                value = Math.log(prevalence + 1) / Math.log(maxPrevalence + 1);
            } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                value = Math.sqrt(prevalence) / Math.sqrt(maxPrevalence);
            } else
                value = prevalence / maxPrevalence;

            double size = Math.max(1, value * (double) maxRadius);

            int[] oval = {(int) (factorX * pv.getX() + dx - size), (int) (factorY * pv.getY() + dy - size), (int) (2 * size), (int) (2 * size)};

            gc.setColor(Color.WHITE); // don't want to see the edges behind the nodes
            gc.fillOval(oval[0], oval[1], oval[2], oval[3]);
        }

        // draw all nodes in color:
        for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
            String className = ((NodeData) v.getData()).getLabel();
            Point2D pv = ((NodeData) v.getData()).getLocation();
            Integer prevalence = ((NodeData) v.getData()).getPrevalence();
            double value = 0;
            if (scalingType == ChartViewer.ScalingType.PERCENT) {
                value = prevalence / maxPrevalence;
            } else if (scalingType == ChartViewer.ScalingType.LOG) {
                value = Math.log(prevalence + 1) / Math.log(maxPrevalence + 1);
            } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                value = Math.sqrt(prevalence) / Math.sqrt(maxPrevalence);
            } else
                value = prevalence / maxPrevalence;

            double size = Math.max(1, value * (double) maxRadius);

            int[] oval = {(int) (factorX * pv.getX() + dx - size), (int) (factorY * pv.getY() + dy - size), (int) (2 * size), (int) (2 * size)};

            Color color = getChartColors().getClassColor(class2HigherClassMapper.get(className), 150);
            gc.setColor(color);
            if (sgc != null)
                sgc.setCurrentItem(new String[]{null, className});
            gc.fillOval(oval[0], oval[1], oval[2], oval[3]);
            if (sgc != null)
                sgc.clearCurrentItem();

            boolean isSelected = getChartData().getChartSelection().isSelected(null, className);
            if (isSelected) {
                gc.setColor(ProgramProperties.SELECTION_COLOR);
                if (oval[2] <= 1) {
                    oval[0] -= 1;
                    oval[1] -= 1;
                    oval[2] += 2;
                    oval[3] += 2;
                }
                gc.setStroke(HEAVY_STROKE);
                gc.drawOval(oval[0], oval[1], oval[2], oval[3]);
                gc.setStroke(NORMAL_STROKE);
            } else {
                gc.setColor(color.darker());
                gc.drawOval(oval[0], oval[1], oval[2], oval[3]);
            }
            if ((showValues && value > 0) || isSelected) {
                String label = "" + prevalence;
                valuesList.add(new DrawableValue(label, oval[0] + oval[2] + 2, oval[1] + oval[3] / 2, isSelected));
            }
        }

        // show labels if requested
        if (isShowXAxis()) {
            gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));
            for (Node v = graph.getFirstNode(); v != null; v = graph.getNextNode(v)) {
                String className = ((NodeData) v.getData()).getLabel();
                Point2D pv = ((NodeData) v.getData()).getLocation();
                Integer prevalence = ((NodeData) v.getData()).getPrevalence();
                double value = 0;
                if (scalingType == ChartViewer.ScalingType.PERCENT) {
                    value = prevalence / maxPrevalence;
                } else if (scalingType == ChartViewer.ScalingType.LOG) {
                    value = Math.log(prevalence + 1) / Math.log(maxPrevalence + 1);
                } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                    value = Math.sqrt(prevalence) / Math.sqrt(maxPrevalence);
                } else
                    value = prevalence / maxPrevalence;
                double size = Math.max(1, value * (double) maxRadius);

                int[] oval = {(int) (factorX * pv.getX() + dx - size), (int) (factorY * pv.getY() + dy - size), (int) (2 * size), (int) (2 * size)};
                Dimension labelSize = Basic.getStringSize(gc, className, gc.getFont()).getSize();
                int x = (int) Math.round(oval[0] + oval[2] / 2 - labelSize.getWidth() / 2);
                int y = oval[1] - 2;

                if (getChartData().getChartSelection().isSelected(null, className)) {
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    fillAndDrawRect(gc, x, y, labelSize.width, labelSize.height, 0, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                }
                gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.BLACK));
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{null, className});
                gc.drawString(className, x, y);
                if (sgc != null)
                    sgc.clearCurrentItem();
            }
        }

        if (valuesList.size() > 0) {
            gc.setFont(getFont(ChartViewer.FontKeys.YAxisFont.toString()));
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            DrawableValue.drawValues(gc, valuesList, false, true);
            valuesList.clear();
        }
    }

    /**
     * draw heat map with colors representing series
     *
     * @param gc
     */
    public void drawChartTransposed(Graphics2D gc) {
        gc.setFont(getFont(ChartViewer.FontKeys.XAxisFont.toString()));
    }

    /**
     * update the view
     */
    public void updateView() {
        // todo: may need to this in a separate thread
        updateGraph();
        embedGraph();
    }

    /**
     * computes the co-occurrences graph
     */
    private void updateGraph() {
        graph.clear();

        Map<String, Node> className2Node = new HashMap<>();

        // setup nodes
        for (String className1 : getChartData().getClassNames()) {
            int prevalenceOfClass1 = 0;
            for (String series : getChartData().getSeriesNames()) {
                double percentage = 100.0 * getChartData().getValue(series, className1).doubleValue() / getChartData().getTotalForSeries(series);
                if (percentage >= getMinThreshold())
                    prevalenceOfClass1++;
            }
            double prevalencePercentage = 100.0 * prevalenceOfClass1 / (double) getChartData().getNumberOfSeries();
            if (prevalencePercentage >= getMinPrevalence() && prevalencePercentage <= getMaxPrevalence()) {
                Node v = graph.newNode();
                NodeData nodeData = new NodeData();
                nodeData.setLabel(className1);
                v.setData(nodeData);
                className2Node.put(className1, v);
                nodeData.setPrevalence(prevalenceOfClass1);
            }
        }

        // setup edges
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            String className1 = ((NodeData) v.getData()).getLabel();
            for (Node w = v.getNext(); w != null; w = w.getNext()) {
                String className2 = ((NodeData) w.getData()).getLabel();
                Set<String> intersection = new HashSet<>();
                Set<String> union = new HashSet<>();
                for (String series : getChartData().getSeriesNames()) {
                    double total = getChartData().getTotalForSeries(series);
                    double percentage1 = 100.0 * getChartData().getValue(series, className1).doubleValue() / total;
                    double percentage2 = 100.0 * getChartData().getValue(series, className2).doubleValue() / total;
                    if (percentage1 >= getMinThreshold() || percentage2 >= getMinThreshold()) {
                        union.add(series);
                    }
                    if (percentage1 > getMinThreshold() && percentage2 >= getMinThreshold()) {
                        intersection.add(series);
                    }
                }
                if (union.size() > 0) {
                    boolean positive;
                    if (isShowCoOccurring() && !isShowAntiOccurring())
                        positive = true;
                    else if (!isShowCoOccurring() && isShowAntiOccurring())
                        positive = false;
                    else
                        positive = (intersection.size() >= 0.5 * union.size());
                    final float probabilityPercent;
                    if (positive)
                        probabilityPercent = (100.0f * (float) intersection.size() / (float) union.size());
                    else
                        probabilityPercent = (100.0f * (float) (union.size() - intersection.size()) / (float) union.size());

                    if (probabilityPercent >= getMinProbability()) {
                        Edge e = graph.newEdge(className2Node.get(className1), className2Node.get(className2));
                        graph.setInfo(e, probabilityPercent);
                        edgeValue.set(e, positive ? probabilityPercent : -probabilityPercent); // negative value indicates anticorrelated
                    }
                }
            }
        }
    }

    /**
     * do embedding of graph
     */
    private void embedGraph() {
        final FruchtermanReingoldLayout fruchtermanReingoldLayout = new FruchtermanReingoldLayout(graph, null);
        final NodeArray<Point2D> coordinates = new NodeArray<>(graph);
        fruchtermanReingoldLayout.apply(1000, coordinates);
        boolean first = true;
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            NodeData nodeData = (NodeData) v.getData();
            nodeData.setLocation(coordinates.get(v));
            if (first) {
                boundingBox.setRect(coordinates.get(v).getX(), coordinates.get(v).getY(), 1, 1);
                first = false;
            } else
                boundingBox.add(coordinates.get(v));
        }
        boundingBox.setRect(boundingBox.getX() - maxRadius, boundingBox.getY() - maxRadius, boundingBox.getWidth() + 2 * maxRadius,
                boundingBox.getHeight() + 2 * maxRadius);
    }


    public int getMaxRadius() {
        return maxRadius;
    }

    public void setMaxRadius(int maxRadius) {
        this.maxRadius = maxRadius;
    }

    /**
     * draw the x axis
     *
     * @param gc
     */
    protected void drawXAxis(Graphics2D gc) {
    }

    /**
     * draw the y-axis
     *
     * @param gc
     */
    protected void drawYAxis(Graphics2D gc, Dimension size) {
    }

    protected void drawYAxisGrid(Graphics2D gc) {
    }

    public boolean canShowLegend() {
        return false;
    }

    public double getMinThreshold() {
        return minThreshold;
    }

    public void setMinThreshold(float minThreshold) {
        this.minThreshold = minThreshold;
        ProgramProperties.put("COMinThreshold", minThreshold);
    }

    public int getMinProbability() {
        return minProbability;
    }

    public void setMinProbability(int minProbability) {
        this.minProbability = minProbability;
        ProgramProperties.put("COMinProbability", minProbability);
    }

    public int getMinPrevalence() {
        return minPrevalence;
    }

    public void setMinPrevalence(int minPrevalence) {
        this.minPrevalence = minPrevalence;
        ProgramProperties.put("COMinPrevalence", minPrevalence);
    }

    public int getMaxPrevalence() {
        return maxPrevalence;
    }

    public void setMaxPrevalence(int maxPrevalence) {
        this.maxPrevalence = maxPrevalence;
        ProgramProperties.put("COMaxPrevalence", maxPrevalence);
    }

    public void setShowAntiOccurring(boolean showAntiOccurring) {
        this.showAntiOccurring = showAntiOccurring;
        ProgramProperties.put("COShowAntiOccurring", showAntiOccurring);
    }

    public boolean isShowAntiOccurring() {
        return showAntiOccurring;
    }

    public boolean isShowCoOccurring() {
        return showCoOccurring;
    }

    public void setShowCoOccurring(boolean showCoOccurring) {
        this.showCoOccurring = showCoOccurring;
        ProgramProperties.put("COShowCoOccurring", showCoOccurring);
    }

    class NodeData {
        private String label;
        private Integer prevalence;
        private Point2D location;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Integer getPrevalence() {
            return prevalence;
        }

        public void setPrevalence(Integer prevalence) {
            this.prevalence = prevalence;
        }

        public Point2D getLocation() {
            return location;
        }

        public void setLocation(Point2D location) {
            this.location = location;
        }
    }

    /**
     * must x and y coordinates by zoomed together?
     *
     * @return
     */
    @Override
    public boolean isXYLocked() {
        return true;
    }

    /**
     * selects all nodes in the connected component hit by the mouse
     *
     * @param event
     * @return true if anything selected
     */
    public boolean selectComponent(MouseEvent event) {
        final SelectionGraphics<String[]> selectionGraphics = new SelectionGraphics<>(getGraphics());
        selectionGraphics.setMouseLocation(event.getPoint());
        if (transpose)
            drawChartTransposed(selectionGraphics);
        else
            drawChart(selectionGraphics);
        Set<String> seriesToSelect = new HashSet<>();
        Set<String> classesToSelect = new HashSet<>();

        int count = 0;
        int size = selectionGraphics.getSelectedItems().size();
        for (String[] pair : selectionGraphics.getSelectedItems()) {
            if (selectionGraphics.getUseWhich() == SelectionGraphics.Which.Last && count++ < size - 1)
                continue;
            if (pair[0] != null) {
                seriesToSelect.add(pair[0]);
            }
            if (pair[1] != null) {
                classesToSelect.add(pair[1]);
            }
            if (selectionGraphics.getUseWhich() == SelectionGraphics.Which.First)
                break;
        }

        if (transpose) {

        } else {
            Set<Node> toVisit = new HashSet<>();
            for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
                NodeData nodeData = (NodeData) v.getData();
                if (classesToSelect.contains(nodeData.getLabel())) {
                    toVisit.add(v);
                }
            }
            while (toVisit.size() > 0) {
                Node v = toVisit.iterator().next();
                toVisit.remove(v);
                selectRec(v, classesToSelect);
            }
        }
        if (seriesToSelect.size() > 0)
            getChartData().getChartSelection().setSelectedSeries(seriesToSelect, true);
        if (classesToSelect.size() > 0)
            getChartData().getChartSelection().setSelectedClass(classesToSelect, true);
        return seriesToSelect.size() > 0 || classesToSelect.size() > 0;

    }

    /**
     * recursively select all nodes in the same component
     *
     * @param v
     * @param selected
     */
    private void selectRec(Node v, Set<String> selected) {
        for (Edge e = v.getFirstAdjacentEdge(); e != null; e = v.getNextAdjacentEdge(e)) {
            Node w = e.getOpposite(v);
            String label = ((NodeData) w.getData()).getLabel();
            if (!selected.contains(label)) {
                selected.add(label);
                selectRec(w, selected);
            }
        }
    }

    /**
     * gets all the labels shown in the graph
     *
     * @return labels
     */
    public Set<String> getAllVisibleLabels() {
        Set<String> labels = new HashSet<>();
        for (Node v = graph.getFirstNode(); v != null; v = v.getNext()) {
            labels.add(((NodeData) v.getData()).getLabel());
        }
        return labels;
    }

    @Override
    public JToolBar getBottomToolBar() {
        return new CoOccurrenceToolBar(viewer, this);
    }

    @Override
    public ChartViewer.ScalingType getScalingTypePreference() {
        return ChartViewer.ScalingType.SQRT;
    }

    @Override
    public String getChartDrawerName() {
        return NAME;
    }
}
