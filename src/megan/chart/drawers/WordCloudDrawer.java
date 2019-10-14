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
import jloda.swing.util.RTree;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.Triplet;
import megan.chart.IChartDrawer;
import megan.chart.IMultiChartDrawable;
import megan.chart.data.DefaultChartData;
import megan.chart.data.IChartData;
import megan.chart.gui.ChartViewer;
import megan.chart.gui.SelectionGraphics;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.concurrent.Future;

/**
 * draws a word cloud
 * Daniel Huson, 6.2012
 */
public class WordCloudDrawer extends BarChartDrawer implements IChartDrawer, IMultiChartDrawable {
    private static final String NAME = "WordCloud";

    private boolean useRectangleShape = false;
    private int maxFontSize = 128;
    private static final Map<Integer, Font> size2font = new HashMap<>();
    private final Set<String> previousClasses = new HashSet<>();
    private ChartViewer.ScalingType previousScalingType = null;
    private final RTree<Pair<String, Integer>> rTree;
    private boolean inUpdateCoordinates = false;
    private Future future; // used in recompute

    private Graphics graphics;
    private int width;
    private int height;

    /**
     * constructor
     */
    public WordCloudDrawer() {
        super();
        rTree = new RTree<>();
        maxFontSize = ProgramProperties.get("WordCloudMaxFontSize", 128);
    }

    /**
     * draw clouds in which words are datasets
     *
     * @param gc
     */
    public void drawChart(Graphics2D gc) {
        int x0 = 2;
        int x1 = getWidth() - 2;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;
        if (x0 >= x1)
            return;

        if (inUpdateCoordinates) {
            gc.setFont(getFont("Default"));
            gc.setColor(Color.LIGHT_GRAY);
            gc.drawString("Updating coordinates...", 20, 20);
            return;
        }
        if (rTree.size() == 0)
            return;

        Rectangle deviceBBox = new Rectangle(x0, y1, x1 - x0, y0 - y1);
        deviceBBox.x += deviceBBox.width / 2;
        deviceBBox.y += deviceBBox.height / 2;

        Rectangle worldBBox = new Rectangle();
        rTree.getBoundingBox(worldBBox);
        worldBBox.x += worldBBox.width / 2;
        worldBBox.y += worldBBox.height / 2;
        double xFactor = deviceBBox.width / (double) worldBBox.width;
        double yFactor = deviceBBox.height / (double) worldBBox.height;
        if (xFactor > 1)
            xFactor = 1;
        if (yFactor > 1)
            yFactor = 1;
        double factor = Math.min(xFactor, yFactor);

        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);

        for (Iterator<Pair<Rectangle2D, Pair<String, Integer>>> it = rTree.iterator(); it.hasNext(); ) {
            Pair<Rectangle2D, Pair<String, Integer>> pair = it.next();
            Rectangle2D rect = pair.get1();
            String label = pair.get2().get1();
            Integer fontSize = (int) (factor * pair.get2().get2());
            gc.setFont(getFontForSize(fontSize));
            if (fontSize >= 1) {
                double x = rect.getX();
                double y = rect.getY() + rect.getHeight();
                x = factor * (x - worldBBox.x) + deviceBBox.x;
                y = factor * (y - worldBBox.y) + deviceBBox.y;
                if (getChartData().getChartSelection().isSelected(null, label)) {
                    Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                    gc.setColor(getFontColor(ChartViewer.FontKeys.XAxisFont.toString(), Color.BLACK));

                    gc.setStroke(NORMAL_STROKE);
                    fillAndDrawRect(gc, x, y, labelSize.width, labelSize.height, 0, ProgramProperties.SELECTION_COLOR, ProgramProperties.SELECTION_COLOR_DARKER);
                }
                Color color = getFontColor(ChartViewer.FontKeys.DrawFont.toString(), null);
                if (color == null)
                    color = getChartColors().getClassColor(class2HigherClassMapper.get(label));
                gc.setColor(color);
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{null, label});
                gc.drawString(label, (int) Math.round(x), (int) Math.round(y));
                if (sgc != null)
                    sgc.clearCurrentItem();

                /*
                {
                    Dimension labelSize = Basic.getStringSize(gc, label, gc.getFont()).getSize();
                    gc.setColor(Color.LIGHT_GRAY);
                    gc.drawRect((int)x, (int)y-labelSize.height, labelSize.width, labelSize.height);
                    //drawRect(gc, x, y, labelSize.width, labelSize.height, 0);
                }
                */

            }
        }
    }

    private boolean mustUpdateCoordinates() {
        boolean mustUpdate = (previousScalingType != scalingType) || (((IChartData) chartData).getMaxTotalSeries() > 0 && rTree.size() == 0);
        if (!mustUpdate) {
            Set<String> currentClasses = new HashSet<>(getChartData().getSeriesNames());
            if (!previousClasses.equals(currentClasses))
                mustUpdate = true;
        }
        return mustUpdate;
    }

    /**
     * compute coordinates
     */
    private void updateCoordinates() {
        previousScalingType = scalingType;
        previousClasses.clear();
        previousClasses.addAll(getChartData().getSeriesNames());

        rTree.clear();
        Graphics gc = getGraphics();
        gc.setFont(getFont(ChartViewer.FontKeys.DrawFont.toString()));
        size2font.clear();

        double maxValue;
        if (scalingType == ChartViewer.ScalingType.PERCENT)
            maxValue = 100;
        else if (scalingType == ChartViewer.ScalingType.LOG) {
            maxValue = Math.log10(getChartData().getMaxTotalSeries());
        } else if (scalingType == ChartViewer.ScalingType.SQRT) {
            maxValue = Math.sqrt(getChartData().getMaxTotalSeries());
        } else
            maxValue = getChartData().getMaxTotalSeries();

        maxValue /= 10; // assume 10 is the average word length

        double fontFactor = (maxValue > 0 ? (maxFontSize) / maxValue : 12);
        final Triplet<Integer, Integer, Dimension> previous = new Triplet<>(-1, 1, new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        final Point center = new Point(0, 0);

        SortedSet<Pair<Double, String>> sorted = new TreeSet<>((pair1, pair2) -> {
            if (pair1.get1() > pair2.get1())
                return -1;
            if (pair1.get1() < pair2.get1())
                return 1;
            return pair1.get2().compareTo(pair2.get2());
        });
        for (String label : getChartData().getSeriesNames()) {
            Double value = getChartData().getTotalForSeries(label);
            sorted.add(new Pair<>(value, label));
        }

        for (Pair<Double, String> pair : sorted) {
            double total = pair.getFirst();
            String series = pair.getSecond();
            double value;
            if (scalingType == ChartViewer.ScalingType.PERCENT) {
                if (total == 0)
                    value = 0;
                else
                    value = 100 * total / getChartData().getMaxTotalClass();
            } else if (scalingType == ChartViewer.ScalingType.LOG) {
                value = total;
                if (value > 0)
                    value = Math.log10(value);
            } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                value = total;
                if (value > 0)
                    value = Math.sqrt(value);
            } else
                value = total;
            value /= (0.5 * series.length());
            int fontSize = (int) Math.min(maxFontSize, Math.round(fontFactor * value));

            computeCoordinates(gc, center, series, getFontForSize(fontSize), previous);
        }
    }

    /**
     * draw clouds in which words are classes
     *
     * @param gc
     */
    public void drawChartTransposed(Graphics2D gc) {
        int x0 = 2;
        int x1 = getWidth() - 2;
        int y0 = getHeight() - bottomMargin;
        int y1 = topMargin;
        if (x0 >= x1)
            return;
        if (inUpdateCoordinates) {
            gc.setFont(getFont("Default"));
            gc.setColor(Color.LIGHT_GRAY);
            gc.drawString("Updating coordinates...", 20, 20);
            return;
        }
        if (rTree.size() == 0)
            return;

        Rectangle deviceBBox = new Rectangle(x0, y1, x1 - x0, y0 - y1);
        deviceBBox.x += deviceBBox.width / 2;
        deviceBBox.y += deviceBBox.height / 2;
        Rectangle worldBBox = new Rectangle();
        rTree.getBoundingBox(worldBBox);
        worldBBox.x += worldBBox.width / 2;
        worldBBox.y += worldBBox.height / 2;
        double xFactor = deviceBBox.width / (double) worldBBox.width;
        double yFactor = deviceBBox.height / (double) worldBBox.height;
        if (xFactor > 1)
            xFactor = 1;
        if (yFactor > 1)
            yFactor = 1;
        double factor = Math.min(xFactor, yFactor);

        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);

        for (Iterator<Pair<Rectangle2D, Pair<String, Integer>>> it = rTree.iterator(); it.hasNext(); ) {
            Pair<Rectangle2D, Pair<String, Integer>> pair = it.next();
            Rectangle2D rect = pair.get1();
            String label = pair.get2().get1();
            Integer fontSize = (int) (factor * pair.get2().get2());
            if (fontSize >= 1) {
                gc.setFont(getFontForSize(fontSize));
                Color color = getFontColor(ChartViewer.FontKeys.DrawFont.toString(), null);
                if (color == null)
                    color = getChartColors().getSampleColor(label);
                gc.setColor(color);

                double x = rect.getX();
                double y = rect.getY() + rect.getHeight();
                x = factor * (x - worldBBox.x) + deviceBBox.x;
                y = factor * (y - worldBBox.y) + deviceBBox.y;
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{label, null});
                gc.drawString(label, (int) Math.round(x), (int) Math.round(y));
                if (sgc != null)
                    sgc.clearCurrentItem();

                if (getChartData().getChartSelection().isSelected(label, null)) {
                    Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                    gc.setStroke(HEAVY_STROKE);
                    gc.setColor(ProgramProperties.SELECTION_COLOR);
                    drawRect(gc, x, y, labelSize.width, labelSize.height, 0);
                    gc.setStroke(NORMAL_STROKE);
                }
            }
        }
    }

    private boolean mustUpdateCoordinatesTransposed() {
        boolean mustUpdate = (previousScalingType != scalingType) || ((IChartData) chartData).getMaxTotalClass() > 0 && rTree.size() == 0;
        if (!mustUpdate) {
            Set<String> currentClasses = new HashSet<>(getChartData().getClassNames());
            if (!previousClasses.equals(currentClasses))
                mustUpdate = true;
        }
        return mustUpdate;
    }

    /**
     * compute coordinates
     */
    private void updateCoordinatesTransposed() {
        previousScalingType = scalingType;
        previousClasses.clear();
        previousClasses.addAll(getChartData().getClassNames());

        rTree.clear();
        Graphics gc = getGraphics();
        size2font.clear();

        double maxValue;
        if (scalingType == ChartViewer.ScalingType.PERCENT)
            maxValue = 100;
        else if (scalingType == ChartViewer.ScalingType.LOG) {
            maxValue = Math.log10(getChartData().getMaxTotalClass());
        } else if (scalingType == ChartViewer.ScalingType.SQRT) {
            maxValue = Math.sqrt(getChartData().getMaxTotalClass());
        } else
            maxValue = getChartData().getMaxTotalClass();

        maxValue /= 10; // assume 10 is the average word length


        double fontFactor = (maxValue > 0 ? (maxFontSize) / maxValue : 12);
        final Triplet<Integer, Integer, Dimension> previous = new Triplet<>(-1, 1, new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        Point center = new Point(0, 0);

        SortedSet<Pair<Double, String>> sorted = new TreeSet<>((pair1, pair2) -> {
            if (pair1.get1() > pair2.get1())
                return -1;
            if (pair1.get1() < pair2.get1())
                return 1;
            return pair1.get2().compareTo(pair2.get2());
        });
        for (String label : getChartData().getClassNames()) {
            double value = getChartData().getTotalForClass(label);
            sorted.add(new Pair<>(value, label));
        }

        for (Pair<Double, String> pair : sorted) {
            double total = pair.getFirst();
            double value;
            String className = pair.getSecond();
            if (scalingType == ChartViewer.ScalingType.PERCENT) {
                if (total == 0)
                    value = 0;
                else
                    value = 100 * total / getChartData().getMaxTotalSeries();
            } else if (scalingType == ChartViewer.ScalingType.LOG) {
                value = total;
                if (value > 0)
                    value = Math.log10(value);
            } else if (scalingType == ChartViewer.ScalingType.SQRT) {
                value = total;
                if (value > 0)
                    value = Math.sqrt(value);
            } else
                value = total;

            value /= (0.5 * className.length());
            int fontSize = (int) Math.min(maxFontSize, Math.round(fontFactor * value));
            computeCoordinates(gc, center, className, getFontForSize(fontSize), previous);
        }
    }

    /**
     * gets a font of the given size
     *
     * @param fontSize
     * @return font
     */
    private Font getFontForSize(Integer fontSize) {
        Font font = size2font.get(fontSize);
        if (font == null) {
            Font theFont = getFont(ChartViewer.FontKeys.DrawFont.toString());
            font = new Font(theFont.getName(), theFont.getStyle(), fontSize);
            size2font.put(fontSize, font);
        }
        return font;
    }

    /**
     * compute coordinates for word cloud
     *
     * @param center
     * @param label
     * @param font
     * @return words
     */
    private void computeCoordinates(Graphics gc, Point center, String label, Font font, Triplet<Integer, Integer, Dimension> previous) {
        int x = center.x;
        int y = center.y;

        Rectangle bbox = new Rectangle();
        Dimension labelSize = BasicSwing.getStringSize(gc, label, font).getSize();
        if (labelSize.height < 1)
            return;
        bbox.setSize(labelSize);

        if (rTree.size() == 0) {
            bbox.setLocation(x - bbox.width / 2, y);
            if (!rTree.overlaps(bbox)) {
                Pair<String, Integer> pair = new Pair<>(label, font.getSize());
                rTree.add(bbox, pair);
                return;
            }
        }

        int direction = previous.getFirst();
        for (int k = 1; true; k++) { // number steps in a direction
            for (int i = 0; i < 2; i++) {  // two different directions
                if (direction == 3)
                    direction = 0;
                else
                    direction++;
                for (int j = previous.getSecond(); j <= k; j++) {  // the steps in the direction
                    switch (direction) {
                        case 0:
                            x += useRectangleShape ? 8 : 5;
                            break;
                        case 1:
                            y += 5;
                            break;
                        case 2:
                            x -= useRectangleShape ? 8 : 5;
                            break;
                        case 3:
                            y -= 5;
                            break;
                    }
                    bbox.setLocation(x - bbox.width / 2, y);
                    if (!rTree.overlaps(bbox)) {
                        Pair<String, Integer> pair = new Pair<>(label, font.getSize());
                        previous.setFirst(direction);
                        previous.setSecond(j);
                        previous.setThird(labelSize);

                        rTree.add(bbox, pair);
                        return;
                    }
                }
            }
        }
    }

    private boolean isUseRectangleShape() {
        return useRectangleShape;
    }

    public void setUseRectangleShape(boolean useRectangleShape) {
        this.useRectangleShape = useRectangleShape;
    }

    public boolean isShowXAxis() {
        return false;
    }

    public boolean isShowYAxis() {
        return false;
    }

    public boolean canShowLegend() {
        return false;
    }

    private int getMaxFontSize() {
        return maxFontSize;
    }

    private void setMaxFontSize(int maxFontSize) {
        this.maxFontSize = maxFontSize;
    }

    public void updateView() {
        if ((!isTranspose() && mustUpdateCoordinates()) || (isTranspose() && mustUpdateCoordinatesTransposed())) {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
            inUpdateCoordinates = true;
            future = executorService.submit(() -> {
                try {
                    if (isTranspose()) {
                        updateCoordinatesTransposed();
                    } else
                        updateCoordinates();
                    if (SwingUtilities.isEventDispatchThread()) {
                        inUpdateCoordinates = false;
                        viewer.repaint();
                        future = null;
                    } else {
                        SwingUtilities.invokeAndWait(() -> {
                            inUpdateCoordinates = false;
                            viewer.repaint();
                            future = null;
                        });
                    }
                } catch (Exception e) {
                    inUpdateCoordinates = false;
                }
            });
        }
    }

    public void updateViewImmediately() {
        if (isTranspose()) {
            updateCoordinatesTransposed();
        } else
            updateCoordinates();
    }

    public void close() {
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }

    public boolean canShowValues() {
        return false;
    }

    /**
     * force update
     */
    @Override
    public void forceUpdate() {
        previousScalingType = null;
    }

    /**
     * create a new instance of the given type of drawer, sharing internal data structures
     *
     * @return
     */
    public WordCloudDrawer createInstance() {
        final WordCloudDrawer drawer = new WordCloudDrawer();
        drawer.setViewer(viewer);
        drawer.setChartData(new DefaultChartData());
        drawer.setClass2HigherClassMapper(class2HigherClassMapper);
        drawer.setSeriesLabelGetter(seriesLabelGetter);
        drawer.setExecutorService(executorService);
        return drawer;

    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;

    }

    public boolean canTranspose() {
        return false;
    }

    @Override
    public void setGraphics(Graphics graphics) {
        this.graphics = graphics;
    }

    @Override
    public Graphics getGraphics() {
        return graphics;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public JToolBar getBottomToolBar() {
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

    /**
     * copy all user parameters from the given base drawer
     *
     * @param baseDrawer
     */
    @Override
    public void setValues(IMultiChartDrawable baseDrawer) {
        setMaxFontSize(((WordCloudDrawer) baseDrawer).getMaxFontSize());
        setUseRectangleShape(((WordCloudDrawer) baseDrawer).isUseRectangleShape());
    }

    @Override
    public String getChartDrawerName() {
        return NAME;
    }
}

