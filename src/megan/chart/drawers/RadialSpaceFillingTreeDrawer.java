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
package megan.chart.drawers;

import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeData;
import jloda.phylo.PhyloTree;
import jloda.swing.util.BasicSwing;
import jloda.swing.util.Geometry;
import jloda.util.ProgramProperties;
import megan.chart.IChartDrawer;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.RedGradient;
import megan.chart.gui.SelectionGraphics;

import java.awt.*;
import java.awt.geom.*;
import java.util.LinkedList;

/**
 * draws a radial space filling chart
 * Daniel Huson, 3.2013
 */
public class RadialSpaceFillingTreeDrawer extends BarChartDrawer implements IChartDrawer {
    private static final String NAME = "RadialTreeChart";

    private enum DrawWhat {RegionsAndBars, Names, Selection, Values}

    private boolean colorByClasses = true;
    private boolean colorBySeries = false;
    private boolean showInternalLabels = true;

    private int angleOffset = 0;

    private final LinkedList<Area> areas = new LinkedList<>();

    /**
     * constructor
     */
    public RadialSpaceFillingTreeDrawer() {
    }

    /**
     * draw a Radial Chart
     *
     * @param gc
     */
    public void drawChart(Graphics2D gc) {
        colorByClasses = true;
        colorBySeries = false;
        doDraw(gc);
    }

    /**
     * draw a transposed Radial Chart
     *
     * @param gc
     */
    public void drawChartTransposed(Graphics2D gc) {
        colorByClasses = false;
        colorBySeries = true;
        doDraw(gc);
    }

    /**
     * draw both chart and transposed chart
     *
     * @param gc
     */
    private void doDraw(Graphics2D gc) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        if (sgc != null)
            sgc.setUseWhich(SelectionGraphics.Which.Last);

        int x0 = 2;
        int x1 = getWidth() - 2;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;
        if (x0 >= x1)
            return;

        Rectangle deviceBBox = new Rectangle(x0, y1, x1 - x0, y0 - y1);

        if (getChartData().getSeriesNames().size() == 0)
            return; // nothing to draw.

        PhyloTree tree = getChartData().getTree();
        if (tree == null || tree.getRoot() == null)
            return;

        double centerRadius = 0.5;  // proportional size of center disk
        double maxLevel = determineMaxLevel(getChartData().getTree()) + centerRadius;
        double radiusFactor = Math.min((deviceBBox.getHeight() - 100) / 2, (deviceBBox.getWidth() - 100) / 2) / (maxLevel + 2);
        radiusFactor *= ProgramProperties.get("RadialChartScalingFactor", 1f);
        Point2D center = new Point2D.Double(deviceBBox.getCenterX(), deviceBBox.getCenterY());

        double maxValue = getMaxValue();
        double topY;
        if (scalingType == ChartViewer.ScalingType.PERCENT)
            topY = 101;
        else if (scalingType == ChartViewer.ScalingType.LOG) {
            topY = computeMaxYAxisValueLogScale(maxValue);
            maxValue = Math.log(maxValue);
        } else if (scalingType == ChartViewer.ScalingType.SQRT) {
            topY = Math.sqrt(maxValue);
            maxValue = Math.sqrt(maxValue);

        } else
            topY = getMaxValue();

        double barFactor = radiusFactor / topY; // multiple value by this to get height of bar

        gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
        gc.setColor(getFontColor(ChartViewer.FontKeys.ValuesFont.toString(), Color.DARK_GRAY));

        // draw regions and bars, selected bars are highlighted in red
        areas.clear();
        drawRec(DrawWhat.RegionsAndBars, gc, tree, center, radiusFactor, tree.getRoot(), centerRadius, maxLevel, 0.0, 360.0, barFactor, maxValue);

        // draw center disk
        Ellipse2D disk = new Ellipse2D.Double(center.getX() - centerRadius * radiusFactor, center.getY() - centerRadius * radiusFactor,
                2 * centerRadius * radiusFactor, 2 * centerRadius * radiusFactor);

        gc.setColor(Color.WHITE);
        gc.fill(disk);
        gc.setColor(Color.LIGHT_GRAY);
        gc.draw(disk);

        if (isShowValues()) {
            gc.setFont(getFont(ChartViewer.FontKeys.ValuesFont.toString()));
            gc.setColor(getFontColor(ChartViewer.FontKeys.ValuesFont.toString(), Color.DARK_GRAY));
            areas.clear();
            drawRec(DrawWhat.Values, gc, tree, center, radiusFactor, tree.getRoot(), centerRadius, maxLevel, 0.0, 360.0, barFactor, maxValue);
        }
        {// show class labels
            gc.setFont(getFont(ChartViewer.FontKeys.LegendFont.toString()));
            gc.setColor(getFontColor(ChartViewer.FontKeys.LegendFont.toString(), Color.BLACK));
            areas.clear();
            drawRec(DrawWhat.Names, gc, tree, center, radiusFactor, tree.getRoot(), centerRadius, maxLevel, 0.0, 360.0, barFactor, maxValue);
        }
        if (getChartData().getChartSelection().getSelectedClasses().size() > 0) {
            gc.setColor(ProgramProperties.SELECTION_COLOR);
            areas.clear();
            drawRec(DrawWhat.Selection, gc, tree, center, radiusFactor, tree.getRoot(), centerRadius, maxLevel, 0.0, 360.0, barFactor, maxValue);
        }
    }

    /**
     * recursively draw the graph
     *
     * @param gc
     * @param center
     * @param radiusFactor
     * @param v
     * @param level
     * @param maxLevel
     * @param angleV
     * @param extentV
     * @param barFactor
     * @param maxValue
     */
    private void drawRec(DrawWhat what, Graphics2D gc, PhyloTree tree, Point2D center, double radiusFactor, Node v, double level, double maxLevel, double angleV, double extentV, double barFactor, double maxValue) {
        if (v.getOutDegree() > 0) {
            // recursively visit all nodes below:
            float countV = ((NodeData) v.getData()).getCountSummarized() - ((NodeData) v.getData()).getCountAssigned();
            if (countV > 0) {
                double factor = extentV / countV;
                double used = 0;
                for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
                    Node w = e.getTarget();
                    float countW = ((NodeData) w.getData()).getCountSummarized();
                    double angleW = angleV + used;
                    double extentW = factor * countW;
                    drawRec(what, gc, tree, center, radiusFactor, w, level + 1, maxLevel, angleW, extentW, barFactor, maxValue);
                    used += extentW;
                }
            }
        }

        if (v.getInDegree() > 0) // not root
        {
            SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);

            String className = tree.getLabel(v);
            if (getChartData().getClassNames().contains(className)) {
                boolean classIsSelected = (getChartData().getChartSelection().isSelected(null, className));
                // draw class regions:
                if (what == DrawWhat.RegionsAndBars) {
                    double radius = (v.getOutDegree() > 0 ? level : maxLevel) * radiusFactor;

                    Rectangle2D rect = new Rectangle2D.Double(center.getX() - radius, center.getY() - radius, 2 * radius, 2 * radius);
                    Arc2D arc = new Arc2D.Double(rect, modulo360(angleV + angleOffset), extentV, Arc2D.PIE);
                    if (isTranspose() && chartData.getNumberOfSeries() == 1) {  // if tranposed and only one dataset, draw colored by count
                        String series = chartData.getSeriesNames().iterator().next();
                        Color color;
                        if (scalingType == ChartViewer.ScalingType.PERCENT) {
                            double total = getChartData().getTotalForSeriesIncludingDisabledAttributes(series);
                            double value;
                            if (total == 0)
                                value = 0;
                            else
                                value = 100 * getChartData().getValueAsDouble(series, className) / total;
                            color = RedGradient.getColor((int) value, (int) maxValue);
                        } else if (scalingType == ChartViewer.ScalingType.LOG) {
                            double value = getChartData().getValueAsDouble(series, className);
                            double inverseMaxValueLog = 1 / Math.log(maxValue);
                            color = RedGradient.getColorLogScale((int) value, inverseMaxValueLog);
                        } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                            double value = Math.sqrt(getChartData().getValueAsDouble(series, className));
                            color = RedGradient.getColor((int) value, (int) maxValue);
                        } else {
                            double value = getChartData().getValueAsDouble(series, className);
                            color = RedGradient.getColor((int) value, (int) maxValue);
                        }
                        gc.setColor(color);
                    } else {
                        if (colorByClasses)
                            gc.setColor(getChartColors().getClassColor(class2HigherClassMapper.get(className), 255));
                        else
                            gc.setColor(Color.WHITE);
                    }
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{null, className});
                    gc.fill(arc);
                    gc.setColor(Color.LIGHT_GRAY);
                    gc.draw(arc);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                }
                // draw series bar charts, the selected ones get colored red here
                if (what == DrawWhat.RegionsAndBars) {
                    int numberOfBars = getChartData().getNumberOfSeries();
                    if (numberOfBars > 1) {
                        double part = extentV / numberOfBars;
                        double used = 0;
                        for (String series : chartData.getSeriesNames()) {
                            double value;
                            if (scalingType == ChartViewer.ScalingType.PERCENT) {
                                double total = getChartData().getTotalForSeriesIncludingDisabledAttributes(series);
                                if (total == 0)
                                    value = 0;
                                else
                                    value = 100 * getChartData().getValueAsDouble(series, className) / total;
                            } else if (scalingType == ChartViewer.ScalingType.LOG) {
                                value = getChartData().getValueAsDouble(series, className);
                                if (value > 0)
                                    value = Math.log10(value);
                            } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                                value = getChartData().getValueAsDouble(series, className);
                                if (value > 0)
                                    value = Math.sqrt(value);
                            } else
                                value = getChartData().getValueAsDouble(series, className);

                            double radius = (level - 1) * radiusFactor + value * barFactor;
                            final Rectangle2D rect = new Rectangle2D.Double(center.getX() - radius, center.getY() - radius, 2 * radius, 2 * radius);
                            final Arc2D arc = new Arc2D.Double(rect, modulo360(angleV + used + angleOffset), part, Arc2D.PIE);
                            used += part;
                            if (colorBySeries) {
                                gc.setColor(getChartColors().getSampleColor(series));
                            } else {
                                final Color color = getChartColors().getClassColor(class2HigherClassMapper.get(className));
                                gc.setColor(color.brighter());
                            }
                            if (sgc != null)
                                sgc.setCurrentItem(new String[]{series, className});
                            gc.fill(arc);
                            gc.setColor(Color.GRAY);
                            gc.draw(arc);
                            if (sgc != null)
                                sgc.clearCurrentItem();

                            if (getChartData().getChartSelection().isSelected(series, null)) {
                                gc.setStroke(HEAVY_STROKE);
                                gc.setColor(ProgramProperties.SELECTION_COLOR);
                                gc.draw(arc);
                                double textAngle = Geometry.deg2rad(360 - (arc.getAngleStart() + arc.getAngleExtent() / 2));
                                Point2D apt = Geometry.translateByAngle(center, textAngle, radius + 2);
                                String label = "" + getChartData().getValueAsDouble(series, className);
                                gc.setColor(ProgramProperties.SELECTION_COLOR_ADDITIONAL_TEXT);
                                drawString(gc, label, apt.getX(), apt.getY(), textAngle);
                                radius = (level - 1) * radiusFactor + 2;
                                rect.setRect(center.getX() - radius, center.getY() - radius, 2 * radius, 2 * radius);
                                arc.setFrame(rect);
                                gc.setColor(ProgramProperties.SELECTION_COLOR);
                                arc.setArcType(Arc2D.OPEN);
                                gc.draw(arc);
                                gc.setColor(Color.BLACK);
                                gc.setStroke(NORMAL_STROKE);

                            }
                        }
                    }
                }
                if (what == DrawWhat.RegionsAndBars && classIsSelected) {
                    // draw selected class regions:
                    double radius = (v.getOutDegree() > 0 ? level : maxLevel) * radiusFactor;
                    Rectangle2D rect = new Rectangle2D.Double(center.getX() - radius, center.getY() - radius, 2 * radius, 2 * radius);
                    Arc2D arc = new Arc2D.Double(rect, modulo360(angleV + angleOffset), extentV, Arc2D.PIE);
                    gc.setStroke(HEAVY_STROKE);
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    gc.draw(arc);
                    radius = (level - 1) * radiusFactor + 2;
                    rect.setRect(center.getX() - radius, center.getY() - radius, 2 * radius, 2 * radius);
                    arc.setFrame(rect);
                    arc.setArcType(Arc2D.OPEN);
                    gc.draw(arc);
                    gc.setColor(Color.BLACK);
                    gc.setStroke(NORMAL_STROKE);
                }

                if (what == DrawWhat.Names || what == DrawWhat.Selection) {

                    if (v.getOutDegree() > 0) {
                        if (showInternalLabels) {
                            Dimension labelSize = BasicSwing.getStringSize(gc, className, gc.getFont()).getSize();
                            double angleInDeg = modulo360(angleV + angleOffset + extentV / 2.0);
                            double angleInRad = Geometry.deg2rad(270.0 - (angleV + angleOffset + extentV / 2.0));
                            double top = 2;
                            for (double shift = 0.3; shift < top; shift += 0.1) {
                                Point2D apt = Geometry.translateByAngle(center, Geometry.deg2rad(angleInDeg), (level - (1 - shift)) * radiusFactor);
                                apt.setLocation(apt.getX(), 2 * center.getY() - apt.getY());  // middle or region
                                apt = Geometry.translateByAngle(apt, angleInRad, -labelSize.width / 2); // move label back so that it will be centered at center of region
                                angleInRad = Geometry.deg2rad(270.0 - (angleV + angleOffset + extentV / 2.0));
                                if (shift < top - 0.05) {
                                    Shape shape = getLabelShape(apt, labelSize, angleInRad);
                                    boolean collision = false;
                                    for (Area aArea : areas) {
                                        Area area = new Area(shape);
                                        if (area.getBounds().intersects(aArea.getBounds())) {
                                            area.intersect(aArea);
                                            if (!area.isEmpty()) {
                                                collision = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (collision)
                                        continue; // try next shift
                                } else {
                                    shift = 0.3; // couldn't find a good place
                                    apt = Geometry.translateByAngle(center, Geometry.deg2rad(angleInDeg), (level - shift) * radiusFactor);
                                    apt.setLocation(apt.getX(), 2 * center.getY() - apt.getY());  // middle or region
                                    apt = Geometry.translateByAngle(apt, angleInRad, -labelSize.width / 2); // move label back so that it will be centered at center of region
                                    angleInRad = Geometry.deg2rad(270.0 - (angleV + angleOffset + extentV / 2.0));
                                }
                                areas.add(new Area(getLabelShape(apt, labelSize, angleInRad)));

                                if ((what == DrawWhat.Names && extentV > 2) || classIsSelected) {
                                    if (sgc != null)
                                        sgc.setCurrentItem(new String[]{null, className});
                                    if (classIsSelected) {
                                        gc.setColor(ProgramProperties.SELECTION_COLOR);
                                        fillAndDrawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, angleInRad, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                                    }
                                    gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.DARK_GRAY));

                                    drawString(gc, className, apt.getX(), apt.getY(), angleInRad);
                                    if (classIsSelected)
                                        gc.setColor(Color.BLACK);
                                    if (sgc != null)
                                        sgc.clearCurrentItem();
                                }
                                break;
                            }
                        }
                    } else  // draw label of leaf node
                    {
                        if ((extentV > 2 && what == DrawWhat.Names) || classIsSelected) {
                            double angleInRad = Geometry.deg2rad(360.0 - (angleV + angleOffset + extentV / 2.0));
                            Point2D apt = Geometry.translateByAngle(center, angleInRad, (maxLevel - 0.5) * radiusFactor + 5);
                            Dimension labelSize = BasicSwing.getStringSize(gc, className, gc.getFont()).getSize();
                            //apt.setLocation(apt.getX(), 2 * center.getY() - apt.getY());
                            //gc.drawString(className, (int)apt.getX(),(int)apt.getY());
                            if (sgc != null)
                                sgc.setCurrentItem(new String[]{null, className});
                            if (classIsSelected) {
                                gc.setColor(ProgramProperties.SELECTION_COLOR);
                                fillAndDrawRect(gc, apt.getX(), apt.getY(), labelSize.width, labelSize.height, angleInRad, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                            }
                            gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.DARK_GRAY));
                            drawString(gc, className, apt.getX(), apt.getY(), angleInRad);
                            if (classIsSelected)
                                gc.setColor(Color.BLACK);
                            if (sgc != null)
                                sgc.clearCurrentItem();
                        }
                    }
                }
                if ((what == DrawWhat.Values && !classIsSelected)
                        || (what == DrawWhat.Selection && classIsSelected)) {
                    if (extentV > 2 || classIsSelected) {
                        Point2D apt = Geometry.translateByAngle(center, Geometry.deg2rad(angleV + angleOffset + extentV / 2), (level - 0.5) * radiusFactor);
                        apt.setLocation(apt.getX(), 2 * center.getY() - apt.getY());
                        String label = "" + (int) getChartData().getTotalForClass(className);
                        Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                        if (classIsSelected)
                            gc.setColor(ProgramProperties.SELECTION_COLOR_ADDITIONAL_TEXT);
                        gc.drawString(label, (int) (apt.getX() - labelSize.width / 2), (int) apt.getY() + labelSize.height + 3);
                        if (classIsSelected)
                            gc.setColor(Color.BLACK);
                    }
                }
            }
        }
    }

    private static double modulo360(double angle) {
        while (angle > 360)
            angle -= 360;
        while (angle < 0)
            angle += 360;
        return angle;
    }

    /**
     * gets the max value used in the plot.
     *
     * @return max value
     */
    protected double getMaxValue() {
        double maxValue = 0;
        PhyloTree tree = getChartData().getTree();
        for (Node v = tree.getFirstNode(); v != null; v = tree.getNextNode(v)) {
            if (v.getInDegree() > 0) {
                String className = tree.getLabel(v);
                for (String series : getChartData().getSeriesNames()) {
                    if (getChartData().getValue(series, className) != null)
                        maxValue = Math.max(maxValue, getChartData().getValue(series, className).doubleValue());
                }
            }
        }
        return maxValue;
    }

    /**
     * determine the max levels of the tree
     *
     * @param tree
     * @return max levels
     */
    private int determineMaxLevel(PhyloTree tree) {
        return determineMaxLevelRec(0, tree.getRoot());
    }

    /**
     * recursively does the work
     *
     * @param level
     * @param v
     * @return level
     */
    private int determineMaxLevelRec(int level, Node v) {
        int newLevel = level;
        for (Edge e = v.getFirstOutEdge(); e != null; e = v.getNextOutEdge(e)) {
            newLevel = Math.max(newLevel, determineMaxLevelRec(level, e.getTarget()) + 1);
        }
        return newLevel;
    }

    public boolean isShowXAxis() {
        return false;
    }

    public boolean isShowYAxis() {
        return false;
    }

    public boolean canShowValues() {
        return true;
    }

    public boolean isXYLocked() {
        return true;
    }

    public boolean isShowInternalLabels() {
        return showInternalLabels;
    }

    public void setShowInternalLabels(boolean showInternalLabels) {
        this.showInternalLabels = showInternalLabels;
    }

    public boolean canShowInternalLabels() {
        return true;
    }

    public boolean canShowXAxis() {
        return false;
    }

    public boolean canShowYAxis() {
        return false;
    }

    public int getAngleOffset() {
        return angleOffset;
    }

    public void setAngleOffset(int angleOffset) {
        this.angleOffset = (int) Math.round(modulo360(angleOffset));
    }

    private Shape getLabelShape(Point2D labelPosition, Dimension labelSize, double labelAngle) {
        if (labelSize != null) {
            Point2D apt = labelPosition;
            if (apt != null) {
                if (labelAngle == 0) {
                    return new Rectangle((int) apt.getX(), (int) apt.getY() - labelSize.height + 1, labelSize.width, labelSize.height);
                } else {
                    labelAngle = labelAngle + 0.0001; // to ensure that labels all get same orientation in

                    AffineTransform localTransform = new AffineTransform();
                    // rotate label to desired angle
                    if (labelAngle >= 0.5 * Math.PI && labelAngle <= 1.5 * Math.PI) {
                        double d = labelSize.getWidth();
                        apt = Geometry.translateByAngle(apt, labelAngle, d);
                        localTransform.rotate(Geometry.moduloTwoPI(labelAngle - Math.PI), apt.getX(), apt.getY());
                    } else
                        localTransform.rotate(labelAngle, apt.getX(), apt.getY());
                    double[] pts = new double[]{apt.getX(), apt.getY(),
                            apt.getX() + labelSize.width, apt.getY(),
                            apt.getX() + labelSize.width, apt.getY() - labelSize.height,
                            apt.getX(), apt.getY() - labelSize.height};
                    localTransform.transform(pts, 0, pts, 0, 4);
                    return new Polygon(new int[]{(int) pts[0], (int) pts[2], (int) pts[4], (int) pts[6]}, new int[]{(int) pts[1], (int) pts[3],
                            (int) pts[5], (int) pts[7]}, 4);
                }
            }
        }
        return null;
    }

    @Override
    public ChartViewer.ScalingType getScalingTypePreference() {
        return ChartViewer.ScalingType.SQRT;
    }

    @Override
    public boolean getShowXAxisPreference() {
        return false;
    }

    @Override
    public boolean getShowYAxisPreference() {
        return false;
    }

    @Override
    public String getChartDrawerName() {
        return NAME;
    }
}
