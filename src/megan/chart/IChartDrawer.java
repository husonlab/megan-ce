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
package megan.chart;

import jloda.swing.util.ILabelGetter;
import jloda.swing.window.IPopupMenuModifier;
import megan.chart.cluster.ClusteringTree;
import megan.chart.data.IData;
import megan.chart.gui.ChartSelection;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.Label2LabelMapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ExecutorService;

/**
 * Interface for chart drawer
 * Daniel Huson, 4.2015
 */
public interface IChartDrawer {
    void setViewer(ChartViewer viewer);

    void setChartData(IData chartData);

    void setClass2HigherClassMapper(Label2LabelMapper class2HigherClassMapper);

    void setSeriesLabelGetter(ILabelGetter seriesLabelGetter);

    void setBackground(Color color);

    double getClassLabelAngle();

    void setClassLabelAngle(double classLabelAngle);

    boolean canTranspose();

    boolean isTranspose();

    void setTranspose(boolean transpose);

    ChartViewer.ScalingType getScalingType();

    void setScalingType(ChartViewer.ScalingType scalingType);

    boolean isSupportedScalingType(ChartViewer.ScalingType scalingType);

    boolean canShowLegend();

    boolean canShowValues();

    boolean isShowValues();

    void setShowValues(boolean showValues);

    boolean canColorByRank();

    String getChartTitle();

    void setChartTitle(String chartTitle);

    boolean canShowYAxis();

    boolean isShowYAxis();

    void setShowYAxis(boolean showYAxis);

    boolean canShowXAxis();

    boolean isShowXAxis();

    void setShowXAxis(boolean showXAxis);

    void updateView();

    ChartColorManager getChartColors();

    IData getChartData();

    void drawChart(Graphics2D gc);

    void drawChartTransposed(Graphics2D gc);

    void close();

    void setFont(String target, Font font, Color color);

    Font getFont(String target);

    Color getFontColor(String target, Color defaultColor);

    boolean isXYLocked();

    boolean isShowInternalLabels();

    void setShowInternalLabels(boolean showInternalLabels);

    boolean canShowInternalLabels();

    boolean selectOnMouseDown(MouseEvent mouseEvent, ChartSelection chartSelection);

    boolean selectOnRubberBand(Rectangle rectangle, MouseEvent mouseEvent, ChartSelection chartSelection);

    String[] getItemBelowMouse(MouseEvent mouseEvent, ChartSelection chartSelection);

    Label2LabelMapper getClass2HigherClassMapper();

    void forceUpdate();

    ILabelGetter getSeriesLabelGetter();

    JToolBar getBottomToolBar();

    ChartViewer.ScalingType getScalingTypePreference();

    boolean getShowXAxisPreference();

    boolean getShowYAxisPreference();

    void repaint();

    Rectangle2D getScrollBackReferenceRect();

    void setScrollBackWindowPoint(Point2D center);

    Point2D convertWindowToReference(Point2D center);

    void setScrollBackReferencePoint(Point2D scrollBackReferencePoint);

    void computeScrollBackReferenceRect();

    Point2D getScrollBackReferencePoint();

    Point2D getScrollBackWindowPoint();

    Point2D convertReferenceToWindow(Point2D scrollBackReferencePoint);

    JPanel getJPanel();

    ChartViewer getViewer();

    String getChartDrawerName();

    void setExecutorService(ExecutorService executorService);

    boolean usesHeatMapColors();

    IPopupMenuModifier getPopupMenuModifier();

    boolean isEnabled();

    void writeData(Writer w) throws IOException;

    boolean canCluster(ClusteringTree.TYPE type);

    boolean canAttributes();
}
