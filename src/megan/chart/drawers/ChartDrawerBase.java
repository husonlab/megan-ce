/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import gnu.jpdf.PDFGraphics;
import jloda.gui.ILabelGetter;
import jloda.gui.IPopupMenuModifier;
import jloda.util.Basic;
import jloda.util.Geometry;
import jloda.util.Pair;
import megan.chart.ChartColorManager;
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
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * base class for chart drawers
 * Daniel Huson, 5.2012
 */
public class ChartDrawerBase extends JPanel {

    protected double classLabelAngle = Math.PI / 4;

    protected final Map<String, Pair<Font, Color>> fonts = new HashMap<>();

    protected int leftMargin = 80;
    protected int rightMargin = 75;
    protected int bottomMargin = 200;
    protected int topMargin = 20;

    protected IData chartData;
    protected Label2LabelMapper class2HigherClassMapper;
    protected ILabelGetter seriesLabelGetter;

    protected boolean transpose = false;
    protected boolean showValues = false;

    protected boolean showXAxis = true;
    protected boolean showYAxis = true;

    protected String chartTitle = "Chart";

    protected ChartViewer.ScalingType scalingType = ChartViewer.ScalingType.LINEAR;
    protected final EnumSet<ChartViewer.ScalingType> supportedScalingTypes = EnumSet.of(ChartViewer.ScalingType.LINEAR, ChartViewer.ScalingType.LOG,
            ChartViewer.ScalingType.PERCENT, ChartViewer.ScalingType.SQRT);

    protected final static Stroke NORMAL_STROKE = new BasicStroke(1);
    protected final static Stroke HEAVY_STROKE = new BasicStroke(2);

    protected Rectangle2D scrollBackReferenceRect = null;  // reference rectangle
    protected Point2D scrollBackWindowPoint = null;
    protected Point2D scrollBackReferencePoint = null;

    // used when drawing values:
    protected final LinkedList<DrawableValue> valuesList = new LinkedList<>();
    protected ChartViewer viewer;
    protected ExecutorService executorService;

    boolean transposedHeightsAdditive = false;

    public ChartDrawerBase() {
    }

    public void setViewer(ChartViewer viewer) {
        this.viewer = viewer;
        this.scalingType = getScalingTypePreference();
        this.showXAxis = getShowXAxisPreference();
        this.showYAxis = getShowYAxisPreference();
    }

    public void setChartData(IData chartData) {
        this.chartData = chartData;
    }


    public void setClass2HigherClassMapper(Label2LabelMapper class2HigherClassMapper) {
        this.class2HigherClassMapper = class2HigherClassMapper;
    }

    public void setSeriesLabelGetter(ILabelGetter seriesLabelGetter) {
        this.seriesLabelGetter = seriesLabelGetter;
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
        Dimension labelSize = Basic.getStringSize(gc, label, gc.getFont()).getSize();

        if (gc instanceof PDFGraphics) {
            //double dx = 0.5 * Math.sin(labelAngle) * labelSize.height;
            //double dy = 0.5 * Math.cos(labelAngle) * labelSize.height;
            double dx = 0;  // can't use offsets unless drawRect is adjusted accordingly
            double dy = 0;
            if (labelAngle >= 0.5 * Math.PI && labelAngle <= 1.5 * Math.PI) {
                ((PDFGraphics) gc).drawString(label, (float) (x + dx), (float) (y - dy), (float) (labelAngle - Math.PI));
            } else {
                ((PDFGraphics) gc).drawString(label, (float) (apt.getX() - dx), (float) (apt.getY() + dy), (float) labelAngle);
            }
        } else {

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
            if (true) { // fixes bug in Java7:
                // see: http://stackoverflow.com/questions/14569475/java-rotated-text-has-reversed-characters-sequence
                FontRenderContext frc = new FontRenderContext(gc.getTransform(), true, true);
                gc.drawGlyphVector(gc.getFont().createGlyphVector(frc, label), (int) (apt.getX()), (int) (apt.getY() + dy));
            } else
                gc.drawString(label, (int) (apt.getX()), (int) (apt.getY() + dy));

            gc.setColor(Color.BLACK);
            gc.setTransform(saveTransform);
        }
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
        this.classLabelAngle = Geometry.moduloTwoPI(classLabelAngle);
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
        this.transpose = transpose;
    }

    public ChartViewer.ScalingType getScalingType() {
        return scalingType;
    }

    public void setScalingType(ChartViewer.ScalingType scalingType) {
        this.scalingType = scalingType;
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
        this.showValues = showValues;
    }

    public boolean canColorByRank() {
        return true;
    }

    public String getChartTitle() {
        return chartTitle;
    }


    public void setChartTitle(String chartTitle) {
        this.chartTitle = chartTitle;
    }


    public boolean canShowYAxis() {
        return true;
    }


    public boolean isShowYAxis() {
        return showYAxis;
    }


    public void setShowYAxis(boolean showYAxis) {
        this.showYAxis = showYAxis;
    }


    public boolean canShowXAxis() {
        return true;
    }


    public boolean isShowXAxis() {
        return showXAxis;
    }


    public void setShowXAxis(boolean showXAxis) {
        this.showXAxis = showXAxis;
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
        this.executorService = executorService;
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
        this.scrollBackReferenceRect = scrollBackReferenceRect;
    }

    public void setScrollBackWindowPoint(Point2D scrollBackWindowPoint) {
        this.scrollBackWindowPoint = scrollBackWindowPoint;
    }

    public void setScrollBackReferencePoint(Point2D scrollBackReferencePoint) {
        this.scrollBackReferencePoint = scrollBackReferencePoint;
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

        SelectionGraphics<String[]> selectionGraphics = new SelectionGraphics<>(getGraphics());
        selectionGraphics.setSelectionRectangle(rectangle);
        selectionGraphics.setShiftDown(mouseEvent.isShiftDown());
        selectionGraphics.setMouseClicks(mouseEvent.getClickCount());
        if (this instanceof BubbleChartDrawer) {
            ((BubbleChartDrawer) this).drawYAxis(selectionGraphics, null);
        }
        if (this instanceof HeatMapDrawer) {
            ((HeatMapDrawer) this).drawYAxis(selectionGraphics, null);
        }
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
        if (seriesToSelect.size() > 0) {
            chartSelection.toggleSelectedSeries(seriesToSelect);
        }
        if (classesToSelect.size() > 0)
            chartSelection.toggleSelectedClasses(classesToSelect);
        return seriesToSelect.size() > 0 || classesToSelect.size() > 0;
    }

    /**
     * get item below mouse
     *
     * @param mouseEvent
     * @param chartSelection
     * @return label for item below mouse
     */

    public Pair<String, String> getItemBelowMouse(MouseEvent mouseEvent, ChartSelection chartSelection) {
        SelectionGraphics<String[]> selectionGraphics = new SelectionGraphics<>(getGraphics());
        selectionGraphics.setMouseLocation(mouseEvent.getPoint());
        selectionGraphics.setShiftDown(mouseEvent.isShiftDown());
        selectionGraphics.setMouseClicks(mouseEvent.getClickCount());
        if (this instanceof BubbleChartDrawer) {
            ((BubbleChartDrawer) this).drawYAxis(selectionGraphics, null);
        }
        if (this instanceof HeatMapDrawer) {
            ((HeatMapDrawer) this).drawYAxis(selectionGraphics, null);
        }
        if (transpose)
            drawChartTransposed(selectionGraphics);
        else
            drawChart(selectionGraphics);

        int count = 0;
        int size = selectionGraphics.getSelectedItems().size();
        for (String[] pairs : selectionGraphics.getSelectedItems()) {
            if (selectionGraphics.getUseWhich() == SelectionGraphics.Which.Last && count++ < size - 1)
                continue;
            if (pairs[0] != null && chartSelection.isSelectedBasedOnSeries()) {
                return new Pair<>(pairs[0], pairs[1]);
            }
            if (pairs[1] != null && !chartSelection.isSelectedBasedOnSeries()) {
                return new Pair<>(pairs[0], pairs[1]);
            }
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

    public JPanel getJPanel() {
        return this;
    }

    public void setMargins(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
        this.leftMargin = leftMargin;
        this.topMargin = topMargin;
        this.rightMargin = rightMargin;
        this.bottomMargin = bottomMargin;
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

    public boolean isEnabled() {
        return true;
    }

}
