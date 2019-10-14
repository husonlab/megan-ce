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
package megan.viewer;

import jloda.swing.util.BasicSwing;
import megan.core.Document;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * panel for drawing legend into
 * Daniel Huson, 2.2013
 */
public class LegendPanel extends JPanel {
    private final ViewerBase viewer;
    private final Document doc;
    private Font font = Font.decode("Helvetica-NORMAL-11");
    private Color fontColor = Color.BLACK;
    private NodeDrawer.Style style = NodeDrawer.Style.Circle;
    private JPopupMenu popupMenu = null;

    /**
     * constructor
     *
     * @param viewer
     */
    public LegendPanel(ViewerBase viewer) {
        this.viewer = viewer;
        doc = viewer.getDocument();
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
     * @param graphics
     */
    public void paint(Graphics graphics) {
        super.paint(graphics);
        Graphics2D gc = (Graphics2D) graphics;
        gc.setColor(Color.WHITE);
        gc.fill(getVisibleRect());
        draw(gc, null);
    }

    /**
     * rescan the view
     */
    public void updateView() {
        Graphics2D graphics = (Graphics2D) getGraphics();
        Dimension size = new Dimension();
        draw(graphics, size);
        //setPreferredSize(new Dimension(getPreferredSize().width,size.height));
        setPreferredSize(size);
        revalidate();
    }

    /**
     * draw a legend for dataset colors
     *
     * @param gc
     */
    void draw(Graphics2D gc, Dimension size) {
        if (gc != null && doc.getNumberOfSamples() > 1) {
            boolean vertical = viewer.getShowLegend().equals("vertical");

            if (getFont() != null)
                gc.setFont(getFont());
            boolean doDraw = (size == null);

            int yStart = 20;
            int x = 3;
            int maxX = x;
            if (doDraw) {
                String legend = "Legend:";
                gc.setColor(Color.BLACK);
                gc.drawString(legend, x, yStart);
                Dimension labelSize = BasicSwing.getStringSize(gc, legend, gc.getFont()).getSize();
                maxX = Math.max(maxX, labelSize.width);
            }
            int y = yStart + (int) (1.5 * gc.getFont().getSize());

            int count = 1;
            for (String sampleName : doc.getSampleNames()) {
                String label = doc.getSampleLabelGetter().getLabel(sampleName);
                if (!label.equals(sampleName))
                    label += " (" + sampleName + ")";

                if (style == NodeDrawer.Style.HeatMap) {
                    label = count + ": " + label;
                }
                final Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
                int boxSize = 0;
                if (style != NodeDrawer.Style.HeatMap)
                    boxSize = labelSize.height - 2;

                if (x + 12 + labelSize.width + 2 > getWidth() || vertical) {
                    x = 3;
                    y += 1.5 * gc.getFont().getSize();
                }
                if (doDraw) {
                    switch (style) {
                        case PieChart:
                        case BarChart:
                        case CoxComb: {
                            Color color = doc.getSampleAttributeTable().getSampleColor(sampleName);
                            if (color == null)
                                color = doc.getChartColorManager().getSampleColor(sampleName);

                            gc.setColor(color);
                            gc.fillRect(x, y - boxSize, boxSize, boxSize);
                            gc.setColor(color.darker());
                            gc.drawRect(x, y - boxSize, boxSize, boxSize);
                            break;
                        }
                        case Circle: {
                            gc.setColor(Color.DARK_GRAY);
                            gc.drawRect(x, y - boxSize, boxSize, boxSize);
                            break;
                        }
                        case HeatMap: {
                            break;
                        }
                    }
                    gc.setColor(getFontColor());
                    gc.drawString(label, x + boxSize + 2, y);
                }
                x += boxSize + 2 + labelSize.width + 10;
                maxX = Math.max(maxX, x);
                if (vertical)
                    maxX = Math.max(maxX, x);
                count++;
            }
            if (size != null)
                size.setSize(maxX, y);
        }
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public Font getFont() {
        return font;
    }

    public void setFontColor(Color fontColor) {
        this.fontColor = fontColor;
    }

    private Color getFontColor() {
        return fontColor;
    }

    public NodeDrawer.Style getStyle() {
        return style;
    }

    public void setStyle(NodeDrawer.Style style) {
        this.style = style;
    }
}
