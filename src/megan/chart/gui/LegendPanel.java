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
package megan.chart.gui;

import jloda.swing.util.BasicSwing;
import megan.chart.data.IChartData;
import megan.chart.data.IPlot2DData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * the panel that contains the legend
 * Daniel Huson, 3.2013
 */
public class LegendPanel extends JPanel {
    private final ChartViewer chartViewer;
    private JPopupMenu popupMenu = null;

    /**
     * constructor
     *
     * @param chartViewer
     */
    public LegendPanel(ChartViewer chartViewer) {
        this.chartViewer = chartViewer;
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger() && popupMenu != null)
                    popupMenu.show(LegendPanel.this, mouseEvent.getX(), mouseEvent.getY());
            }

            public void mouseReleased(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger() && popupMenu != null)
                    popupMenu.show(LegendPanel.this, mouseEvent.getX(), mouseEvent.getY());
            }
        });
    }

    public void setPopupMenu(JPopupMenu popupMenu) {
        this.popupMenu = popupMenu;
    }

    /**
     * draw the legend
     *
     * @param gc0
     */
    public void paint(Graphics gc0) {
        Graphics2D gc = (Graphics2D) gc0;
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        if (sgc == null) {
            super.paint(gc);
            gc.setColor(Color.WHITE);
            gc.fill(getVisibleRect());
        }
        draw(gc, null);
    }

    /**
     * update the view
     */
    public void updateView() {
        Graphics2D gc = (Graphics2D) getGraphics();
        Dimension size = new Dimension();
        draw(gc, size);
        setPreferredSize(size);
        revalidate();
    }

    /**
     * draw the legend
     *
     * @param gc
     * @param size
     */
    void draw(Graphics2D gc, Dimension size) {
        if (!chartViewer.isTranspose())
            drawLegend(gc, size);
        else
            drawLegendTransposed(gc, size);
    }

    /**
     * draw a legend for class colors
     *
     * @param gc
     */
    private void drawLegend(Graphics2D gc, Dimension size) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);
        boolean doDraw = (size == null);

        if (chartViewer.getChartData() instanceof IChartData) {
            boolean vertical = chartViewer.getShowLegend().equals("vertical");
            gc.setFont(chartViewer.getChartDrawer().getFont(ChartViewer.FontKeys.LegendFont.toString()));

            int yStart = getFont().getSize();
            int x = 3;
            int maxX = x;
            {
                String legend;
                if (chartViewer.getChartData().getClassesLabel() != null)
                    legend = "Legend (" + chartViewer.getChartData().getClassesLabel() + "):";
                else
                    legend = "Legend:";
                if (doDraw) {
                    gc.setColor(Color.BLACK);
                    gc.drawString(legend, x, yStart);
                }
                Dimension labelSize = BasicSwing.getStringSize(gc, legend, gc.getFont()).getSize();
                maxX = Math.max(maxX, labelSize.width);
            }
            int y = yStart + (int) (1.5 * gc.getFont().getSize());

            for (String className : ((IChartData) chartViewer.getChartData()).getClassNames()) {
                Dimension labelSize = BasicSwing.getStringSize(gc, className, gc.getFont()).getSize();
                if (x + 12 + labelSize.width + 2 > getWidth() || vertical) {
                    x = 2;
                    y += 1.5 * gc.getFont().getSize();
                }
                if (doDraw) {
                    Color color = chartViewer.getChartColorManager().getClassColor(chartViewer.getClass2HigherClassMapper().get(className));
                    gc.setColor(color);
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{null, className});
                    gc.fillRect(x, y - labelSize.height, labelSize.height, labelSize.height);
                    gc.setColor(color.darker());
                    gc.drawRect(x, y - labelSize.height, labelSize.height, labelSize.height);
                    gc.setColor(chartViewer.getChartDrawer().getFontColor(ChartViewer.FontKeys.LegendFont.toString(), Color.BLACK));
                    gc.drawString(className, x + labelSize.height + 2, y);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                }
                maxX = Math.max(maxX, x);
                x += labelSize.height + 2 + labelSize.width + 10;
                if (vertical)
                    maxX = Math.max(maxX, x);
            }
            if (!doDraw) {
                size.setSize(maxX, y + 5);
            }
        } else if (chartViewer.getChartData() instanceof IPlot2DData) {
            boolean vertical = chartViewer.getShowLegend().equals("vertical");
            int yStart = getFont().getSize();
            int x = 3;
            int maxX = x;
            if (doDraw) {
                String legend = "Legend (samples):";
                gc.setColor(Color.BLACK);
                gc.drawString(legend, x, yStart);
                Dimension labelSize = BasicSwing.getStringSize(gc, legend, gc.getFont()).getSize();
                maxX = Math.max(maxX, labelSize.width);
            }
            int y = yStart + (int) (1.5 * gc.getFont().getSize());

            for (String sampleName : chartViewer.getChartData().getSeriesNames()) {
                String label = chartViewer.getSeriesLabelGetter().getLabel(sampleName);
                if (!label.equals(sampleName))
                    label += " (" + sampleName + ")";

                final Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                if (x + 12 + labelSize.width + 2 > getWidth() || vertical) {
                    x = 3;
                    y += 1.5 * gc.getFont().getSize();
                }
                if (doDraw) {
                    Color color = chartViewer.getChartColorManager().getSampleColor(sampleName);
                    gc.setColor(color);
                    if (sgc != null)
                        sgc.setCurrentItem(new String[]{sampleName, null});
                    gc.fillRect(x, y - labelSize.height, labelSize.height, labelSize.height);
                    gc.setColor(color.darker());
                    gc.drawRect(x, y - labelSize.height, labelSize.height, labelSize.height);
                    gc.setColor(chartViewer.getChartDrawer().getFontColor(ChartViewer.FontKeys.LegendFont.toString(), Color.BLACK));
                    gc.drawString(label, x + labelSize.height + 2, y);
                    if (sgc != null)
                        sgc.clearCurrentItem();
                }
                maxX = Math.max(maxX, x);
                x += labelSize.height + 2 + labelSize.width + 10;
                if (vertical)
                    maxX = Math.max(maxX, x);
            }
            if (!doDraw) {
                size.setSize(maxX, y + 5);
            }
        }
    }


    /**
     * draw a legend for dataset colors
     *
     * @param gc
     */
    private void drawLegendTransposed(Graphics2D gc, Dimension size) {
        SelectionGraphics<String[]> sgc = (gc instanceof SelectionGraphics ? (SelectionGraphics<String[]>) gc : null);

        boolean vertical = chartViewer.getShowLegend().equals("vertical");
        try {
            gc.setFont(chartViewer.getFont(ChartViewer.FontKeys.LegendFont.toString()));
        } catch (Exception ignored) {
        }

        boolean doDraw = (size == null);

        int yStart = getFont().getSize();
        int x = 3;
        int maxX = x;
        if (doDraw) {
            String legend;
            if (chartViewer.getChartData().getSeriesLabel() != null)
                legend = "Legend (" + chartViewer.getChartData().getSeriesLabel() + "):";
            else
                legend = "Legend:";
            gc.setColor(Color.BLACK);
            gc.drawString(legend, x, yStart);
            Dimension labelSize = BasicSwing.getStringSize(gc, legend, gc.getFont()).getSize();
            maxX = Math.max(maxX, labelSize.width);
        }
        int y = yStart + (int) (1.5 * gc.getFont().getSize());

        for (String sampleName : chartViewer.getChartData().getSeriesNames()) {
            String label = chartViewer.getSeriesLabelGetter().getLabel(sampleName);
            if (!label.equals(sampleName))
                label += " (" + sampleName + ")";

            final Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
            if (x + 12 + labelSize.width + 2 > getWidth() || vertical) {
                x = 3;
                y += 1.5 * gc.getFont().getSize();
            }
            if (doDraw) {
                Color color = chartViewer.getChartColorManager().getSampleColor(sampleName);
                gc.setColor(color);
                if (sgc != null)
                    sgc.setCurrentItem(new String[]{sampleName, null});
                gc.fillRect(x, y - labelSize.height, labelSize.height, labelSize.height);
                gc.setColor(color.darker());
                gc.drawRect(x, y - labelSize.height, labelSize.height, labelSize.height);
                gc.setColor(chartViewer.getChartDrawer().getFontColor(ChartViewer.FontKeys.LegendFont.toString(), Color.BLACK));
                gc.drawString(label, x + labelSize.height + 2, y);
                if (sgc != null)
                    sgc.clearCurrentItem();
            }
            maxX = Math.max(maxX, x);
            x += labelSize.height + 2 + labelSize.width + 10;
            if (vertical)
                maxX = Math.max(maxX, x);
        }
        if (!doDraw) {
            size.setSize(maxX, y + 5);
        }
    }
}
