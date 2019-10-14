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
package megan.viewer.gui;

import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.swing.graphview.GraphView;
import jloda.swing.graphview.INodeDrawer;
import jloda.swing.graphview.NodeShape;
import jloda.swing.graphview.NodeView;
import jloda.util.ProgramProperties;
import jloda.util.Statistics;
import megan.core.Document;
import megan.main.MeganProperties;
import megan.viewer.MainViewer;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * node drawer for multi-sample nodes
 * Daniel Huson, 2.2012
 */
public class NodeDrawer implements INodeDrawer {
    static public Color pvalueColor = ProgramProperties.get(MeganProperties.PVALUE_COLOR, Color.CYAN);
    private static final Font selectionFont = ProgramProperties.get("selectionFont", Font.decode("Helvetica-PLAIN-11"));

    public enum ScalingType {
        LOG, SQRT, LINEAR
    }

    public enum Style {
        Square,
        Circle,
        PieChart,
        BarChart,
        HeatMap,
        CoxComb,
        Default
    }

    public enum ScaleBy {
        Summarized,
        Assigned,
        None
    }

    private Graphics2D gc;
    private final GraphView viewer;
    private final Document doc;

    private Style style = Style.Circle;
    private ScaleBy scaleBy = ScaleBy.Assigned;

    private boolean drawLeavesOnly = false;
    private int maxNodeHeight = 20;
    private double maxTotalCount = 0;
    private double maxSingleCount = 0;
    private double maxValue = 0;

    private ScalingType scalingType = ScalingType.SQRT;
    private double linearFactor = 1; // multiple count by this to get radius for linear scaling
    private double sqrtFactor = 1; // multiple count by this to get radius for sqrt scaling
    private double logFactor = 1;  // multiple count by this to get radius for log scaling
    private double inverseLogMaxCount = 1;
    private double inverseSqrtMaxCount = 1;

    /**
     * constructor
     *
     * @param viewer
     */
    public NodeDrawer(Document doc, GraphView viewer) {
        this.viewer = viewer;
        this.doc = doc;
    }

    /**
     * setup data
     *
     * @param graphView
     * @param gc
     */
    public void setup(GraphView graphView, Graphics2D gc) {
        this.gc = gc;
    }

    /**
     * get the Node style
     *
     * @return node style
     */
    public Style getStyle() {
        return style;
    }

    /**
     * set the node style
     *
     * @param style
     */
    public void setStyle(Style style) {
        this.style = style;

        switch (style) {
            default:
            case PieChart:
            case Circle:
                maxValue = maxTotalCount;
                break;
            case CoxComb:
            case BarChart:
            case HeatMap:
                maxValue = maxSingleCount;
        }
        this.linearFactor = (maxValue > 0 ? maxNodeHeight / maxValue : 0);
        this.sqrtFactor = (maxValue > 0 ? maxNodeHeight / Math.sqrt(maxValue) : 0);
        this.logFactor = (maxValue > 0 ? maxNodeHeight / Math.log(maxValue) : 0);
        inverseLogMaxCount = (maxValue > 0 ? 1.0 / Math.log(maxValue) : 0);
        inverseSqrtMaxCount = (maxValue > 0 ? 1.0 / Math.sqrt(maxValue) : 0);
    }

    public void setStyle(String styleName, Style defaultValue) {
        Style style = null;
        if (styleName != null) {
            for (Style aStyle : Style.values()) {
                if (styleName.equalsIgnoreCase(aStyle.toString())) {
                    style = aStyle;
                    break;
                }
            }
        }
        if (style == null)
            setStyle(defaultValue);
        else
            setStyle(style);
    }

    public ScalingType getScalingType() {
        return scalingType;
    }

    public void setScalingType(ScalingType scalingType) {
        this.scalingType = scalingType;
    }

    /**
     * set the max single account and max total node count
     *
     * @param maxSingleCountAndMaxTotalCount
     */
    public void setCounts(double[] maxSingleCountAndMaxTotalCount) {
        this.maxSingleCount = maxSingleCountAndMaxTotalCount[0];
        this.maxTotalCount = maxSingleCountAndMaxTotalCount[1];
        setStyle(getStyle()); // trigger rescan
    }

    public double getMaxSingleCount() {
        return maxSingleCount;
    }

    public double getMaxTotalCount() {
        return maxTotalCount;
    }

    public int getMaxNodeHeight() {
        return maxNodeHeight;
    }

    public void setMaxNodeHeight(int maxNodeHeight) {
        this.maxNodeHeight = maxNodeHeight;
        setStyle(getStyle()); // trigger rescan
    }

    /**
     * gets size at which the value should be drawn
     *
     * @param value
     * @return scaled size
     */
    public double getScaledSize(double value) {
        if (value > maxValue)
            value = maxValue;
        switch (scalingType) {
            default:
            case LINEAR:
                return value * linearFactor;
            case SQRT:
                return Math.sqrt(value) * sqrtFactor;
            case LOG:
                return Math.log(value) * logFactor;
        }
    }

    /**
     * draw the node
     *
     * @param selected
     */
    public void draw(Node v, boolean selected) {
        final NodeView nv = viewer.getNV(v);
        final NodeData data = (NodeData) v.getData();

        if (selected)
            hilite(v);

        if ((!drawLeavesOnly || v.getOutDegree() == 0) && scaleBy != ScaleBy.None && nv.getNodeShape() != NodeShape.None) {
            switch (style) {
                case HeatMap:
                    drawAsHeatMap(v, nv, data);
                    break;
                case BarChart:
                    drawAsBarChart(v, nv, data);
                    break;
                case PieChart:
                    drawAsCircle(v, nv, data);
                    drawAsPieChart(v, nv, data);
                    break;
                case CoxComb:
                    drawAsCoxComb(v, nv, data);
                    break;
                default:
                case Circle:
                    drawAsCircle(v, nv, data);
                    break;
            }
        } else {
            nv.setNodeShape(NodeShape.None);
        }
    }

    /**
     * hilite the node
     *
     * @param v
     */
    private void hilite(Node v) {
        NodeView nv = viewer.getNV(v);

        if (nv.getLocation() == null)
            return;
        {
            int scaledWidth;
            int scaledHeight;
            if (nv.getNodeShape() == NodeShape.None) {
                scaledWidth = scaledHeight = 2;
            } else {
                if (nv.getFixedSize()) {
                    scaledWidth = nv.getWidth();
                    scaledHeight = nv.getHeight();
                } else {
                    scaledWidth = NodeView.computeScaledWidth(viewer.trans, nv.getWidth());
                    scaledHeight = NodeView.computeScaledHeight(viewer.trans, nv.getHeight());
                }
            }

            Point apt = viewer.trans.w2d(nv.getLocation());
            apt.x -= (scaledWidth >> 1);
            apt.y -= (scaledHeight >> 1);

            gc.setColor(ProgramProperties.SELECTION_COLOR);
            Shape shape = new Rectangle(apt.x - 2, apt.y - 2, scaledWidth + 4, scaledHeight + 4);
            gc.fill(shape);
            gc.setColor(ProgramProperties.SELECTION_COLOR_DARKER);
            final Stroke oldStroke = gc.getStroke();
            gc.setStroke(NodeView.NORMAL_STROKE);
            gc.draw(shape);
            gc.setStroke(oldStroke);
        }
    }

    /**
     * hilite the node label
     *
     * @param v
     */
    private void hiliteLabel(Node v, NodeData data) {
        NodeView nv = viewer.getNV(v);

        if (nv.getLocation() == null)
            return;

        Point apt = nv.getLabelPosition(viewer.trans);

        if (apt == null)
            return;

        gc.setColor(ProgramProperties.SELECTION_COLOR_ADDITIONAL_TEXT);
        if (!nv.getLabelVisible() && nv.getLabel() != null) {
            gc.setFont(nv.getFont());
            gc.drawString(nv.getLabel(), apt.x, apt.y);
        }

        if ((data.getSummarized().length > 1 || (data.getSummarized().length == 1 && data.getSummarized()[0] > 0))) {
            gc.setFont(selectionFont);

            StringBuilder buf = new StringBuilder();
            if (data.getCountAssigned() > 0) {
                buf.append("Assigned=");
                if (data.getAssigned().length < 50) {
                    for (float value : data.getAssigned()) {
                        buf.append(String.format("%,d  ", Math.round(value)));
                    }
                } else {
                    final Statistics statistics = new Statistics(data.getAssigned());
                    final int mean = (int) Math.round(statistics.getMean());
                    if (mean <= 1)
                        buf.append(String.format(" %,d - %,d", Math.round(statistics.getMin()), Math.round(statistics.getMax())));
                    else
                        buf.append(String.format(" %,d - %,d (mean: %,d sd: %,d)", Math.round(statistics.getMin()), Math.round(statistics.getMax()), mean, Math.round(statistics.getStdDev())));
                }
                gc.drawString(buf.toString(), apt.x, apt.y += 14);
            }
            buf = new StringBuilder();
            buf.append("Summed=");
            if (data.getSummarized().length < 50) {
                for (float value : data.getSummarized()) {
                    buf.append(String.format("%,.0f  ", value));
                }
            } else {
                final Statistics statistics = new Statistics(data.getSummarized());
                final int mean = (int) Math.round(statistics.getMean());
                if (mean <= 1)
                    buf.append(String.format(" %,d - %,d", Math.round(statistics.getMin()), Math.round(statistics.getMax())));
                else
                    buf.append(String.format(" %,d - %,d (mean: %,d sd: %,d)", Math.round(statistics.getMin()), Math.round(statistics.getMax()), mean, Math.round(statistics.getStdDev())));
            }
            gc.drawString(buf.toString(), apt.x, apt.y += 12);
        }

        if (data.getUpPValue() != -1) {
            gc.drawString("UPv=" + (float) data.getUpPValue(), apt.x, apt.y += 12);
        }
        if (data.getDownPValue() != -1) {
            gc.drawString("DPv=" + (float) data.getDownPValue(), apt.x, apt.y += 12);
        }
    }


    /**
     * draw the label of the node
     *
     * @param selected
     */
    public void drawLabel(Node v, boolean selected) {
        viewer.getNV(v).drawLabel(gc, viewer.trans, viewer.getFont(), selected);

        if (selected)
            hiliteLabel(v, (NodeData) v.getData());
    }

    /**
     * draw the node and the label
     *
     * @param selected
     */
    public void drawNodeAndLabel(Node v, boolean selected) {
        draw(v, selected);
        drawLabel(v, selected);
    }

    /**
     * draw node as a scaled circle
     *
     * @param v
     * @param nv
     * @param data
     */
    private void drawAsCircle(Node v, NodeView nv, NodeData data) {
        Point2D location = nv.getLocation();

        if (location == null)
            return; // no location, don't draw

        nv.setNodeShape(NodeShape.Oval);
        float num;
        if (scaleBy == ScaleBy.Summarized || v.getOutDegree() == 0)
            num = data.getCountSummarized();
        else
            num = data.getCountAssigned();

        if (num > 0) {
            int radius = (int) Math.max(1.0, getScaledSize(num));
            nv.setHeight((2 * radius));
            nv.setWidth((2 * radius));
        } else {
            nv.setWidth(1);
            nv.setHeight(1);
        }

        if (data.getUpPValue() >= 0 || data.getDownPValue() >= 0) {
            int width = nv.getWidth();
            int height = nv.getHeight();
            Point apt = viewer.trans.w2d(location);
            apt.x -= (width >> 1);
            apt.y -= (height >> 1);

            if (data.getUpPValue() >= 0) {
                gc.setColor(pvalueColor);
                Stroke oldStroke = gc.getStroke();
                int leftWidth = getWidthForPValue(data.getUpPValue());
                gc.setStroke(new BasicStroke(leftWidth));
                gc.drawArc(apt.x - (leftWidth >> 1) - 1, apt.y - (leftWidth >> 1) - 1, width + leftWidth + 1, height + leftWidth + 1, 90, 180);
                gc.setStroke(oldStroke);
            }
            if (data.getDownPValue() >= 0) {
                gc.setColor(pvalueColor);
                Stroke oldStroke = gc.getStroke();
                int rightWidth = getWidthForPValue(data.getDownPValue());
                gc.setStroke(new BasicStroke(rightWidth));
                gc.drawArc(apt.x - (rightWidth >> 1) - 1, apt.y - (rightWidth >> 1) - 1, width + rightWidth + 1, height + rightWidth + 1, 270, 180);
                gc.setStroke(oldStroke);
            }
        }
        nv.draw(gc, viewer.trans);
    }

    /**
     * draw as Cox comb
     *
     * @param v
     * @param nv
     * @param data
     */
    private void drawAsCoxComb(Node v, NodeView nv, NodeData data) {
        Point2D location = nv.getLocation();

        if (location == null)
            return; // no location, don't draw
        Point apt = viewer.trans.w2d(location);
        nv.setNodeShape(NodeShape.Oval);

        final float[] values;
        if (scaleBy == ScaleBy.Summarized || v.getOutDegree() == 0) // must be collapsed node
        {
            values = data.getSummarized();
        } else {
            values = data.getAssigned();
        }

        double delta = 360.0 / values.length;
        int maxRadius = 0;
        for (int i = 0; i < values.length; i++) {
            double radius = Math.max(1.0, getScaledSize(values[i]));
            // double radius = Math.sqrt((double)assigned[i] / count) *viewer.getMaxNodeRadius(); // we assume here that the largest value is 50% of total reads
            maxRadius = Math.max(maxRadius, (int) radius);
            Arc2D arc = new Arc2D.Double(apt.x - radius, apt.y - radius, 2 * radius, 2 * radius, i * delta + 45, delta, Arc2D.PIE);
            gc.setColor(doc.getColorByIndex(i));
            gc.fill(arc);
            //gc.drawString(""+array[i],apt.x+30,apt.y+(i+1)*20);
            if (values.length < 120)
                gc.setColor(Color.GRAY);
            gc.draw(arc);
        }
        if (data.getUpPValue() >= 0) {
            gc.setColor(pvalueColor);
            Stroke oldStroke = gc.getStroke();
            int leftWidth = getWidthForPValue(data.getUpPValue());
            gc.setStroke(new BasicStroke(leftWidth));
            gc.drawArc(apt.x - maxRadius, apt.y - maxRadius, 2 * maxRadius + 2, 2 * maxRadius + 2, 90, 180);

            gc.setStroke(oldStroke);
        }
        if (data.getDownPValue() >= 0) {
            gc.setColor(pvalueColor);
            Stroke oldStroke = gc.getStroke();
            int rightWidth = getWidthForPValue(data.getDownPValue());
            gc.setStroke(new BasicStroke(rightWidth));
            gc.drawArc(apt.x - maxRadius, apt.y - maxRadius, 2 * maxRadius + 2, 2 * maxRadius + 2, 270, 180);
            gc.setStroke(oldStroke);
        }
        nv.setWidth(Math.max(1, 2 * maxRadius));
        nv.setHeight(Math.max(1, 2 * maxRadius));
    }

    /**
     * draw as a pie chart
     *
     * @param v
     * @param nv
     * @param data
     */
    private void drawAsPieChart(Node v, NodeView nv, NodeData data) {
        Point2D location = nv.getLocation();

        if (location == null)
            return; // no location, don't draw
        Point apt = viewer.trans.w2d(location);

        int width = nv.getWidth();
        int height = nv.getHeight();
        apt.x -= (width >> 1);
        apt.y -= (height >> 1);
        nv.setNodeShape(NodeShape.Oval);

        if (data.getUpPValue() >= 0) {
            gc.setColor(pvalueColor);
            Stroke oldStroke = gc.getStroke();
            int leftWidth = getWidthForPValue(data.getUpPValue());
            gc.setStroke(new BasicStroke(leftWidth));
            gc.drawArc(apt.x - (leftWidth >> 1) - 1, apt.y - (leftWidth >> 1) - 1, width + leftWidth + 1, height + leftWidth + 1, 90, 180);
            gc.setStroke(oldStroke);
        }
        if (data.getDownPValue() >= 0) {
            gc.setColor(pvalueColor);
            Stroke oldStroke = gc.getStroke();
            int rightWidth = getWidthForPValue(data.getDownPValue());
            gc.setStroke(new BasicStroke(rightWidth));
            gc.drawArc(apt.x - (rightWidth >> 1) - 1, apt.y - (rightWidth >> 1) - 1, width + rightWidth + 1, height + rightWidth + 1, 270, 180);
            gc.setStroke(oldStroke);
        }

        final double count;
        final float[] array;
        if (scaleBy == ScaleBy.Summarized || v.getOutDegree() == 0) // must be collapsed node
        {
            count = data.getCountSummarized();
            array = data.getSummarized();
        } else {
            count = data.getCountAssigned();
            array = data.getAssigned();
        }

        if (count > 0) {
            //gc.drawString(""+count,apt.x+30,apt.y);

            double delta = 360.0 / count;
            double oldAngle = 0;
            int total = 0;

            for (int i = 0; i < array.length; i++) {
                if (array[i] > 0) {
                    total += array[i];
                    double newAngle = total * delta;
                    Arc2D arc = new Arc2D.Double(apt.x, apt.y, width, height, oldAngle, newAngle - oldAngle, Arc2D.PIE);

                    gc.setColor(doc.getColorByIndex(i));
                    gc.fill(arc);
                    //gc.drawString(""+array[i],apt.x+30,apt.y+(i+1)*20);
                    if (array.length < 120)
                        gc.setColor(Color.GRAY);
                    gc.draw(arc);
                    oldAngle = newAngle;
                }
            }
            gc.setColor(Color.GRAY);
            gc.drawOval(apt.x, apt.y, width, height);
        } else
            gc.drawOval(apt.x, apt.y, width, height);
    }

    /**
     * draw as a heat map
     *
     * @param v
     * @param nv
     * @param data
     */
    private void drawAsHeatMap(Node v, NodeView nv, NodeData data) {
        final double count;
        final float[] array;
        if (scaleBy == ScaleBy.Summarized || v.getOutDegree() == 0 && data.getCountSummarized() > data.getCountAssigned()) // must be collapsed node
        {
            count = data.getCountSummarized();
            array = data.getSummarized();
        } else {
            count = data.getCountAssigned();
            array = data.getAssigned();
        }

        Point2D location = nv.getLocation();
        Rectangle box = new Rectangle();
        viewer.trans.w2d(new Rectangle(0, 0, MainViewer.XSTEP, MainViewer.YSTEP), box);
        int width;
        if (array.length <= 1)
            width = 30;
        else
            width = (int) (30.0 / array.length * (Math.sqrt(array.length)));
        box.setRect(box.x, box.y, width, Math.min(2 * maxNodeHeight, box.height));

        if (location == null)
            return; // no location, don't draw

        Point apt = viewer.trans.w2d(location);

        if (v.getOutDegree() == 0 || count > 0) {
            nv.setNodeShape(NodeShape.Rectangle);
            nv.setWidth((int) Math.round(data.getAssigned().length * box.getWidth()));
            nv.setHeight((int) Math.round(box.getHeight()));
        } else {
            nv.setNodeShape(NodeShape.Oval);
            nv.setWidth(1);
            nv.setHeight(1);
        }

        apt.x -= nv.getWidth() >> 1; // offset
        apt.y -= box.getHeight() / 2 - 1;

        if (data.getUpPValue() >= 0) {
            gc.setColor(pvalueColor);
            Stroke oldStroke = gc.getStroke();
            int leftWidth = getWidthForPValue(data.getUpPValue());
            gc.setStroke(new BasicStroke(leftWidth));
            gc.drawLine(apt.x - 1, apt.y,
                    apt.x - 1, apt.y + nv.getHeight());

            gc.setStroke(oldStroke);
        }
        if (data.getDownPValue() >= 0) {
            gc.setColor(pvalueColor);
            Stroke oldStroke = gc.getStroke();
            int rightWidth = getWidthForPValue(data.getDownPValue());
            gc.setStroke(new BasicStroke(rightWidth));
            gc.drawLine(apt.x + nv.getWidth() + 1, apt.y,
                    apt.x + nv.getWidth() + 1, apt.y + nv.getHeight());
            gc.setStroke(oldStroke);
        }

        //gc.drawString(""+count,apt.x+30,apt.y);
        if (v.getOutDegree() == 0 || count > 0) {
            for (int i = 0; i < array.length; i++) {
                Color color;
                switch (scalingType) {
                    default:
                    case LINEAR:
                        color = doc.getChartColorManager().getHeatMapTable().getColor((int) array[i], (int) maxValue);
                        break;
                    case SQRT:
                        color = doc.getChartColorManager().getHeatMapTable().getColorSqrtScale((int) array[i], inverseSqrtMaxCount);
                        break;
                    case LOG:
                        color = doc.getChartColorManager().getHeatMapTable().getColorLogScale((int) array[i], inverseLogMaxCount);
                        break;
                }
                gc.setColor(color);

                // gc.setColor(getLogScaleColor(COLORS[i % COLORS.length], array[i], inverseLogTotalReads));
                double aWidth = Math.max(0.2, box.getWidth());
                gc.fill(new Rectangle2D.Double(apt.x + i * box.getWidth(), apt.y, aWidth, box.getHeight()));

                if (box.getWidth() > 1) {
                    gc.setColor(Color.DARK_GRAY);
                    gc.draw(new Rectangle2D.Double(apt.x + i * box.getWidth(), apt.y, box.getWidth(), box.getHeight()));
                }
            }
            if (box.getWidth() <= 1) {
                gc.setColor(Color.GRAY);
                gc.drawRect(apt.x, apt.y, (int) Math.round(array.length * box.getWidth()), (int) Math.round(box.getHeight()));
            }
        } else
            nv.draw(gc, viewer.trans);
    }

    /**
     * draw as meters
     *
     * @param v
     * @param nv
     * @param data
     */
    private void drawAsBarChart(Node v, NodeView nv, NodeData data) {
        final double count;
        final float[] array;
        if (scaleBy == ScaleBy.Summarized || v.getOutDegree() == 0) // must be collapsed node
        {
            count = data.getCountSummarized();
            array = data.getSummarized();
        } else {
            count = data.getCountAssigned();
            array = data.getAssigned();
        }

        Point2D location = nv.getLocation();
        Rectangle box = new Rectangle();
        viewer.trans.w2d(new Rectangle(0, 0, MainViewer.XSTEP, MainViewer.YSTEP), box);
        int width;
        if (array.length <= 1)
            width = 30;
        else
            width = (int) (30.0 / array.length * (Math.sqrt(array.length)));
        box.setRect(box.x, box.y, width, Math.min(2 * maxNodeHeight, box.height));


        if (location == null)
            return; // no location, don't draw

        Point apt = viewer.trans.w2d(location);

        if (v.getOutDegree() == 0 || count > 0) {
            nv.setNodeShape(NodeShape.Rectangle);
            nv.setWidth((int) Math.round((array.length) * box.getWidth()));
            nv.setHeight((int) Math.round(box.getHeight()));
        } else {
            nv.setNodeShape(NodeShape.Oval);
            nv.setWidth(1);
            nv.setHeight(1);
        }

        apt.x -= nv.getWidth() >> 1; // offset
        apt.y -= box.getHeight() / 2;

        if (data.getUpPValue() >= 0) {
            gc.setColor(pvalueColor);
            Stroke oldStroke = gc.getStroke();
            int leftWidth = getWidthForPValue(data.getUpPValue());
            gc.setStroke(new BasicStroke(leftWidth));
            gc.drawLine(apt.x - 1, apt.y, apt.x - 1, apt.y + nv.getHeight());
            gc.setStroke(oldStroke);
        }
        if (data.getDownPValue() >= 0) {
            gc.setColor(pvalueColor);
            Stroke oldStroke = gc.getStroke();
            int rightWidth = getWidthForPValue(data.getDownPValue());
            gc.setStroke(new BasicStroke(rightWidth));
            gc.drawLine(apt.x + nv.getWidth() + 1, apt.y,
                    apt.x + nv.getWidth() + 1, apt.y + nv.getHeight());
            gc.setStroke(oldStroke);
        }

        //gc.drawString(""+count,apt.x+30,apt.y);
        if (v.getOutDegree() == 0 || count > 0) {
            for (int i = 0; i < array.length; i++) {
                gc.setColor(Color.WHITE);
                gc.fill(new Rectangle2D.Double(apt.x + i * box.getWidth(), apt.y, box.getWidth(), box.getHeight()));
                gc.setColor(doc.getColorByIndex(i));
                double height = box.getHeight() / (double) getMaxNodeHeight() * getScaledSize(array[i]);

                double aWidth = Math.max(0.2, box.getWidth());
                gc.fill(new Rectangle2D.Double(apt.x + i * box.getWidth(), apt.y + (box.getHeight() - height), aWidth, height));

                // Color color = new Color(Math.max(0, (int) (0.8 * doc.getColorByIndex(i).getRed())), Math.max(0, (int) (0.8 * doc.getColorByIndex(i).getGreen())), Math.max(0, (int) (0.8 * doc.getColorByIndex(i).getBlue())));
                // gc.setColor(color);
                // gc.drawLine(apt.x + i * box.width, apt.y + (int) (box.height - height), apt.x + (i + 1) * box.width, apt.y + (int) (box.height - height));
                if (box.getWidth() > 1) {
                    gc.setColor(Color.GRAY);
                    gc.draw(new Rectangle2D.Double(apt.x + i * box.getWidth(), apt.y, box.getWidth(), box.getHeight()));
                }
            }
            if (box.getWidth() <= 1) {
                gc.setColor(Color.GRAY);
                gc.drawRect(apt.x, apt.y, (int) Math.round(array.length * box.getWidth()), (int) Math.round(box.getHeight()));
            }
        } else
            nv.draw(gc, viewer.trans);
    }

    /**
     * convert a pvalue into a line width
     *
     * @param pvalue
     * @return line width
     */
    private static int getWidthForPValue(double pvalue) {
        if (pvalue < 0) return 0;
        if (pvalue < Math.pow(10, -37)) return 9;
        if (pvalue < Math.pow(10, -32)) return 8;
        if (pvalue < Math.pow(10, -27)) return 7;
        if (pvalue < Math.pow(10, -22)) return 6;
        if (pvalue < Math.pow(10, -17)) return 5;
        if (pvalue < Math.pow(10, -12)) return 4;
        if (pvalue < Math.pow(10, -7)) return 3;
        if (pvalue < Math.pow(10, -2)) return 2;
        return 1;
    }

    public ScaleBy getScaleBy() {
        return scaleBy;
    }

    public void setScaleBy(String scaleByName) {
        for (ScaleBy aScaleBy : ScaleBy.values()) {
            if (scaleByName.equalsIgnoreCase(aScaleBy.toString())) {
                setScaleBy(aScaleBy);
                return;
            }
        }
    }

    public void setScaleBy(ScaleBy scaleBy) {
        this.scaleBy = scaleBy;
    }

    public boolean isDrawLeavesOnly() {
        return drawLeavesOnly;
    }

    public void setDrawLeavesOnly(boolean drawLeavesOnly) {
        this.drawLeavesOnly = drawLeavesOnly;
    }
}
