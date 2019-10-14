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

import jloda.swing.util.BasicSwing;
import jloda.swing.util.Geometry;
import jloda.swing.util.ILabelGetter;
import jloda.swing.window.IPopupMenuModifier;
import jloda.util.Pair;
import megan.chart.ChartColorManager;
import megan.chart.cluster.ClusteringTree;
import megan.chart.data.IData;
import megan.chart.gui.ChartSelection;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.Label2LabelMapper;
import megan.chart.gui.SelectionGraphics;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * base class for chart drawers
 * Daniel Huson, 5.2012
 */
public class ChartDrawerBase extends JPanel {
    double classLabelAngle = Math.PI / 4;

    final Map<String, Pair<Font, Color>> fonts = new HashMap<>();

    int leftMargin = 80;
    int rightMargin = 75;
    int bottomMargin = 200;
    int topMargin = 20;

    IData chartData;
    Label2LabelMapper class2HigherClassMapper;
    ILabelGetter seriesLabelGetter;

    boolean transpose = false;
    boolean showValues = false;

    boolean showXAxis = true;
    private boolean showYAxis = true;

    String chartTitle = "Chart";

    ChartViewer.ScalingType scalingType = ChartViewer.ScalingType.LINEAR;
    private final EnumSet<ChartViewer.ScalingType> supportedScalingTypes = EnumSet.of(ChartViewer.ScalingType.LINEAR, ChartViewer.ScalingType.LOG,
            ChartViewer.ScalingType.PERCENT, ChartViewer.ScalingType.SQRT);

    final static Stroke NORMAL_STROKE = new BasicStroke(1);
    final static Stroke HEAVY_STROKE = new BasicStroke(2);

    private Rectangle2D scrollBackReferenceRect = null;  // reference rectangle
    private Point2D scrollBackWindowPoint = null;
    private Point2D scrollBackReferencePoint = null;

    // used when drawing values:
    final LinkedList<DrawableValue> valuesList = new LinkedList<>();
    ChartViewer viewer;
    ExecutorService executorService;

    boolean transposedHeightsAdditive = false;

    public ChartDrawerBase() {
    }

    public void setViewer(ChartViewer viewer) {
        ChartDrawerBase.this.viewer = viewer;
        ChartDrawerBase.this.scalingType = getScalingTypePreference();
        ChartDrawerBase.this.showXAxis = getShowXAxisPreference();
        ChartDrawerBase.this.showYAxis = getShowYAxisPreference();
    }

    public void setChartData(IData chartData) {
        ChartDrawerBase.this.chartData = chartData;
    }


    public void setClass2HigherClassMapper(Label2LabelMapper class2HigherClassMapper) {
        ChartDrawerBase.this.class2HigherClassMapper = class2HigherClassMapper;
    }

    public void setSeriesLabelGetter(ILabelGetter seriesLabelGetter) {
        ChartDrawerBase.this.seriesLabelGetter = seriesLabelGetter;
    }

    /**
     * draw a string at the given anchor point at the given angle (in radiant)
     *
     * @param gc
     * @param label
     * @param x
     * @param y
     * @param labelAngle
     */
    public static void drawString(Graphics2D gc, String label, double x, double y, double labelAngle) {
        labelAngle = Geometry.moduloTwoPI(labelAngle);
        Point2D apt = new Point2D.Float((float) x, (float) y);
        Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();

        // save current transform:
        final AffineTransform saveTransform = gc.getTransform();
        // rotate label to desired angle
        if (labelAngle >= 0.5 * Math.PI && labelAngle <= 1.5 * Math.PI) {
            apt = Geometry.translateByAngle(apt, labelAngle, labelSize.getWidth());
            gc.rotate(Geometry.moduloTwoPI(labelAngle - Math.PI), apt.getX(), apt.getY());
        } else {
            gc.rotate(labelAngle, apt.getX(), apt.getY());
        }
        // double dy= 0.5 * labelSize.height;
        double dy = 0;
        // fixes bug in Java7:
        // see: http://stackoverflow.com/questions/14569475/java-rotated-text-has-reversed-characters-sequence
        FontRenderContext frc = new FontRenderContext(gc.getTransform(), true, true);
        gc.drawGlyphVector(gc.getFont().createGlyphVector(frc, label), (int) (apt.getX()), (int) (apt.getY() + dy));

        gc.setColor(Color.BLACK);
        gc.setTransform(saveTransform);
    }

    /**
     * draw string centered
     *
     * @param gc
     * @param label
     * @param x
     * @param y
     * @param addHeight if true, y is used as bottom coordinate, not top
     */
    public static void drawStringCentered(Graphics gc, String label, double x, double y, boolean addHeight) {
        Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
        gc.drawString(label, (int) Math.round(x - labelSize.getWidth() / 2), (int) Math.round(addHeight ? y + labelSize.getHeight() : y));
    }

    /**
     * draw a rectangle at the given anchor point at the given angle (in radiant)
     *
     * @param gc
     * @param x
     * @param y
     * @param width
     * @param height
     * @param labelAngle
     */
    public static void drawRect(Graphics2D gc, double x, double y, double width, double height, double labelAngle) {
        Dimension theSize = new Dimension((int) Math.round(width), (int) Math.round(height));
        Point2D apt = new Point2D.Float((float) x, (float) y);

        // save current transform:
        AffineTransform saveTransform = gc.getTransform();

        // rotate label to desired angle
        if (labelAngle >= 0.5 * Math.PI && labelAngle <= 1.5 * Math.PI) {
            apt = Geometry.translateByAngle(apt, labelAngle, theSize.getWidth());
            gc.rotate(Geometry.moduloTwoPI(labelAngle - Math.PI), apt.getX(), apt.getY());
        } else {
            gc.rotate(labelAngle, apt.getX(), apt.getY());
        }
        gc.drawRect((int) Math.round(apt.getX()), (int) Math.round(apt.getY()) - theSize.height, theSize.width, theSize.height);
        gc.setTransform(saveTransform);
    }

    /**
     * draw a rectangle at the given anchor point at the given angle (in radiant)
     *
     * @param gc
     * @param x
     * @param y
     * @param width
     * @param height
     * @param labelAngle
     */
    public static void fillAndDrawRect(Graphics2D gc, double x, double y, double width, double height, double labelAngle, Color fillColor, Color drawColor) {
        Dimension theSize = new Dimension((int) Math.round(width), (int) Math.round(height));
        Point2D apt = new Point2D.Float((float) x, (float) y);

        // save current transform:
        AffineTransform saveTransform = gc.getTransform();

        // rotate label to desired angle
        if (labelAngle >= 0.5 * Math.PI && labelAngle <= 1.5 * Math.PI) {
            apt = Geometry.translateByAngle(apt, labelAngle, theSize.getWidth());
            gc.rotate(Geometry.moduloTwoPI(labelAngle - Math.PI), apt.getX(), apt.getY());
        } else {
            gc.rotate(labelAngle, apt.getX(), apt.getY());
        }
        gc.setColor(fillColor);
        gc.fillRect((int) Math.round(apt.getX()), (int) Math.round(apt.getY()) - theSize.height, theSize.width, theSize.height);
        gc.setColor(drawColor);
        gc.drawRect((int) Math.round(apt.getX()), (int) Math.round(apt.getY()) - theSize.height, theSize.width, theSize.height);
        gc.setTransform(saveTransform);
    }


    public double getClassLabelAngle() {
        return classLabelAngle;
    }


    public void setClassLabelAngle(double classLabelAngle) {
        ChartDrawerBase.this.classLabelAngle = Geometry.moduloTwoPI(classLabelAngle);
    }

    /**
     * Draw an arrow head.
     *
     * @param gc Graphics
     * @param vp Point
     * @param wp Point
     */
    public void drawArrowHead(Graphics gc, Point vp, Point wp) {
        final int arrowLength = 5;
        final double arrowAngle = 2.2;
        double alpha = Geometry.computeAngle(new Point(wp.x - vp.x, wp.y - vp.y));
        Point a = new Point(arrowLength, 0);
        a = Geometry.rotate(a, alpha + arrowAngle);
        a.translate(wp.x, wp.y);
        Point b = new Point(arrowLength, 0);
        b = Geometry.rotate(b, alpha - arrowAngle);
        b.translate(wp.x, wp.y);
        gc.drawLine(a.x, a.y, wp.x, wp.y);
        gc.drawLine(wp.x, wp.y, b.x, b.y);
    }


    public boolean canTranspose() {
        return true;
    }

    public boolean isTranspose() {
        return transpose;
    }


    public void setTranspose(boolean transpose) {
        ChartDrawerBase.this.transpose = transpose;
    }

    public ChartViewer.ScalingType getScalingType() {
        return scalingType;
    }

    public void setScalingType(ChartViewer.ScalingType scalingType) {
        ChartDrawerBase.this.scalingType = scalingType;
    }

    public boolean isSupportedScalingType(ChartViewer.ScalingType scalingType) {
        return supportedScalingTypes.contains(scalingType);
    }

    public void setSupportedScalingTypes(ChartViewer.ScalingType... scalingType) {
        supportedScalingTypes.clear();
        supportedScalingTypes.addAll(Arrays.asList(scalingType));
    }

    public EnumSet<ChartViewer.ScalingType> getSupportedScalingTypes() {
        return supportedScalingTypes;
    }

    public boolean canShowLegend() {
        return true;
    }

    public boolean canShowValues() {
        return false;
    }


    public boolean isShowValues() {
        return showValues;
    }


    public void setShowValues(boolean showValues) {
        ChartDrawerBase.this.showValues = showValues;
    }

    public boolean canColorByRank() {
        return true;
    }

    public String getChartTitle() {
        return chartTitle;
    }


    public void setChartTitle(String chartTitle) {
        ChartDrawerBase.this.chartTitle = chartTitle;
    }


    public boolean canShowYAxis() {
        return true;
    }


    public boolean isShowYAxis() {
        return showYAxis;
    }


    public void setShowYAxis(boolean showYAxis) {
        ChartDrawerBase.this.showYAxis = showYAxis;
    }


    public boolean canShowXAxis() {
        return true;
    }


    public boolean isShowXAxis() {
        return showXAxis;
    }


    public void setShowXAxis(boolean showXAxis) {
        ChartDrawerBase.this.showXAxis = showXAxis;
    }

    /**
     * compute the maximum value on a log scale
     *
     * @param maxValue
     * @return max value on a log scale
     */
    protected double computeMaxYAxisValueLogScale(double maxValue) {
        double v = 0;
        int mantisse = 0;
        int exponent = 0;
        while (v < maxValue) {
            if (mantisse < 9)
                mantisse++;
            else {
                mantisse = 1;
                exponent++;
            }
            v = mantisse * Math.pow(10, exponent);
        }
        return Math.log10(v);
    }

    /**
     * override if necessary
     */

    public void updateView() {
    }

    public ChartColorManager getChartColors() {
        return viewer.getChartColorManager();
    }

    public IData getChartData() {
        return chartData;
    }

    public void setExecutorService(ExecutorService executorService) {
        ChartDrawerBase.this.executorService = executorService;
    }

    public void drawChart(Graphics2D gc) {
    }

    public void drawChartTransposed(Graphics2D gc) {
    }

    public void close() {
    }

    public void setFont(String target, Font font, Color color) {
        fonts.put(target, new Pair<>(font, color));
    }

    public Font getFont(String target) {
        Pair<Font, Color> pair = fonts.get(target);
        if (pair == null || pair.get1() == null)
            return getFont();
        else
            return pair.get1();
    }

    public Color getFontColor(String target, Color defaultColor) {
        Pair<Font, Color> pair = fonts.get(target);
        if (pair == null || pair.get2() == null)
            return defaultColor;
        else
            return pair.get2();
    }

    /**
     * converts a point from window coordinates to reference coordinates
     *
     * @param apt
     * @return reference coordinates
     */
    public Point2D convertWindowToReference(Point2D apt) {
        if (scrollBackReferenceRect == null)
            return null;
        else
            return new Point2D.Double((apt.getX() - scrollBackReferenceRect.getX()) / scrollBackReferenceRect.getWidth(),
                    (apt.getY() - scrollBackReferenceRect.getY()) / scrollBackReferenceRect.getHeight());
    }

    /**
     * converts a point from reference coordinates to window coordinates
     *
     * @param refPoint
     * @return window coordinates
     */
    public Point2D convertReferenceToWindow(Point2D refPoint) {
        if (scrollBackReferenceRect == null)
            return null;
        else
            return new Point2D.Double(Math.round(refPoint.getX() * scrollBackReferenceRect.getWidth() + scrollBackReferenceRect.getX()),
                    Math.round(refPoint.getY() * scrollBackReferenceRect.getHeight() + scrollBackReferenceRect.getY()));
    }

    public Rectangle2D getScrollBackReferenceRect() {
        return scrollBackReferenceRect;
    }

    public void setScrollBackReferenceRect(Rectangle2D scrollBackReferenceRect) {
        ChartDrawerBase.this.scrollBackReferenceRect = scrollBackReferenceRect;
    }

    public void setScrollBackWindowPoint(Point2D scrollBackWindowPoint) {
        ChartDrawerBase.this.scrollBackWindowPoint = scrollBackWindowPoint;
    }

    public void setScrollBackReferencePoint(Point2D scrollBackReferencePoint) {
        ChartDrawerBase.this.scrollBackReferencePoint = scrollBackReferencePoint;
    }

    public Point2D getScrollBackWindowPoint() {
        return scrollBackWindowPoint;
    }

    public Point2D getScrollBackReferencePoint() {
        return scrollBackReferencePoint;
    }

    public void computeScrollBackReferenceRect() {
        scrollBackReferenceRect = new Rectangle2D.Double(0, 0, getWidth(), getHeight());
    }

    public boolean isXYLocked() {
        return false;
    }

    public boolean isShowInternalLabels() {
        return false;
    }

    public void setShowInternalLabels(boolean showInternalLabels) {
    }

    public boolean canShowInternalLabels() {
        return false;
    }

    public boolean canCluster(ClusteringTree.TYPE type) {
        return false;
    }

    /**
     * select series and classes that contain given location
     *
     * @param mouseEvent
     * @param chartSelection
     * @return true if something selected
     */
    public boolean selectOnMouseDown(MouseEvent mouseEvent, ChartSelection chartSelection) {
        return selectOnRubberBand(new Rectangle(mouseEvent.getX() - 1, mouseEvent.getY() - 1, 2, 2), mouseEvent, chartSelection);

    }

    /**
     * select all classes and  series that intersection given rectangle
     *
     * @param rectangle
     * @param chartSelection
     * @return true if something selected
     */

    public boolean selectOnRubberBand(Rectangle rectangle, MouseEvent mouseEvent, ChartSelection chartSelection) {
        if (mouseEvent.isControlDown())
            return false;

        if (!mouseEvent.isShiftDown())
            chartSelection.clearSelectionAttributes(); // todo: don't know why only need to do this for attributes

        final SelectionGraphics<String[]> selectionGraphics = new SelectionGraphics<>(getGraphics());
        selectionGraphics.setSelectionRectangle(rectangle);
        selectionGraphics.setShiftDown(mouseEvent.isShiftDown());
        selectionGraphics.setMouseClicks(mouseEvent.getClickCount());

        if (this instanceof BubbleChartDrawer) {
            ((BubbleChartDrawer) ChartDrawerBase.this).drawYAxis(selectionGraphics, null);
        }
        if (this instanceof HeatMapDrawer) {
            ((HeatMapDrawer) ChartDrawerBase.this).drawYAxis(selectionGraphics, null);
        }
        if (transpose)
            drawChartTransposed(selectionGraphics);
        else
            drawChart(selectionGraphics);

        final Set<String> seriesToSelect = new HashSet<>();
        final Set<String> classesToSelect = new HashSet<>();
        final Set<String> attributesToSelect = new HashSet<>();

        int count = 0;
        int size = selectionGraphics.getSelectedItems().size();
        for (String[] seriesClassAttribute : selectionGraphics.getSelectedItems()) {
            if (selectionGraphics.getUseWhich() == SelectionGraphics.Which.Last && count++ < size - 1)
                continue;
            if (seriesClassAttribute[0] != null) {
                seriesToSelect.add(seriesClassAttribute[0]);
            }
            if (seriesClassAttribute[1] != null) {
                classesToSelect.add(seriesClassAttribute[1]);
            }
            if (seriesClassAttribute.length >= 3 && seriesClassAttribute[2] != null) {
                attributesToSelect.add(seriesClassAttribute[2]);
            }

            if (selectionGraphics.getUseWhich() == SelectionGraphics.Which.First)
                break;
        }

        if (seriesToSelect.size() > 0) {
            chartSelection.toggleSelectedSeries(seriesToSelect);
        }
        if (classesToSelect.size() > 0)
            chartSelection.toggleSelectedClasses(classesToSelect);
        if (attributesToSelect.size() > 0)
            chartSelection.toggleSelectedAttributes(attributesToSelect);
        return seriesToSelect.size() > 0 || classesToSelect.size() > 0 || attributesToSelect.size() > 0;
    }

    /**
     * get item below mouse
     *
     * @param mouseEvent
     * @param chartSelection
     * @return label for item below mouse
     */

    public String[] getItemBelowMouse(MouseEvent mouseEvent, ChartSelection chartSelection) {
        final SelectionGraphics<String[]> selectionGraphics = new SelectionGraphics<>(getGraphics());
        selectionGraphics.setMouseLocation(mouseEvent.getPoint());
        selectionGraphics.setShiftDown(mouseEvent.isShiftDown());
        selectionGraphics.setMouseClicks(mouseEvent.getClickCount());
        if (this instanceof BubbleChartDrawer) {
            ((BubbleChartDrawer) ChartDrawerBase.this).drawYAxis(selectionGraphics, null);
        }
        if (this instanceof HeatMapDrawer) {
            ((HeatMapDrawer) ChartDrawerBase.this).drawYAxis(selectionGraphics, null);
        }
        if (transpose)
            drawChartTransposed(selectionGraphics);
        else
            drawChart(selectionGraphics);

        int count = 0;
        int size = selectionGraphics.getSelectedItems().size();
        for (String[] seriesClassAttribute : selectionGraphics.getSelectedItems()) {
            if (selectionGraphics.getUseWhich() == SelectionGraphics.Which.Last && count++ < size - 1)
                continue;
            return seriesClassAttribute;
        }
        return null;
    }


    public Label2LabelMapper getClass2HigherClassMapper() {
        return class2HigherClassMapper;
    }

    /**
     * force update
     */

    public void forceUpdate() {
    }


    public ILabelGetter getSeriesLabelGetter() {
        return seriesLabelGetter;
    }


    public JToolBar getBottomToolBar() {
        return null;
    }


    public ChartViewer.ScalingType getScalingTypePreference() {
        return ChartViewer.ScalingType.LINEAR;
    }


    public boolean getShowXAxisPreference() {
        return true;
    }


    public boolean getShowYAxisPreference() {
        return true;
    }

    public void setMargins(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        ChartDrawerBase.this.leftMargin = leftMargin;
        ChartDrawerBase.this.topMargin = topMargin;
        ChartDrawerBase.this.rightMargin = rightMargin;
        ChartDrawerBase.this.bottomMargin = bottomMargin;
    }

    public ChartViewer getViewer() {
        return viewer;
    }

    public boolean usesHeatMapColors() {
        return false;
    }

    public IPopupMenuModifier getPopupMenuModifier() {
        return null;
    }

    public JPanel getJPanel() {
        return this;
    }

    public void writeData(Writer w) throws IOException {
        chartData.write(w);
    }

    public boolean canAttributes() {
        return false;
    }
}
