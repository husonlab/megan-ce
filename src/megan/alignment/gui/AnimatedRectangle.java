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
package megan.alignment.gui;

import jloda.util.ProgramProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * An animated rectangle
 * Daniel Huson, 9.2011
 */
public class AnimatedRectangle {
    static private ScheduledExecutorService scheduler;
    static private final List<WeakReference<AnimatedRectangle>> animatedRectangles = new LinkedList<>();
    static private final BasicStroke backgroundStroke = new BasicStroke(2);
    static private final BasicStroke evenStroke = new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 1, new float[]{5, 5}, 0);
    static private final BasicStroke oddStroke = new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 1, new float[]{5, 5}, 5);
    static private final BasicStroke basicStroke = new BasicStroke(1);
    static private final Rectangle2D drawRectangle = new Rectangle2D.Float();
    private final static Color highlightColor = ProgramProperties.SELECTION_COLOR_ADDITIONAL_TEXT.darker();


    private JPanel panel;
    private boolean animate;
    private Rectangle2D rectangle;
    private boolean even;

    /**
     * @param panel
     */
    public AnimatedRectangle(JPanel panel) {
        this.panel = panel;
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        synchronized (animatedRectangles) {
                            for (WeakReference<AnimatedRectangle> weak : animatedRectangles) {
                                weak.get().draw();
                            }
                        }
                    });
                } catch (InterruptedException | InvocationTargetException ignored) {
                }
            }, 0, 500, MILLISECONDS);
        }
        animatedRectangles.add(new WeakReference<>(this));
    }

    /**
     * currently blinking?
     *
     * @return true, if blinking
     */
    public boolean isAnimate() {
        return animate;
    }

    /**
     * turn blinking on or off
     *
     * @param animate
     */
    public void setAnimate(boolean animate) {
        this.animate = animate;
    }

    /**
     * get the set rectangle
     *
     * @return rectangle
     */
    public Rectangle2D getRectangle() {
        return rectangle;
    }

    /**
     * set rectangle in panel coordinates
     *
     * @param rectangle
     */
    public void setRectangle(JPanel panel, Rectangle2D rectangle) {
        this.panel = panel;
        this.rectangle = (Rectangle2D) rectangle.clone();
    }

    /**
     * draws the blinking rectangle
     */
    private void draw() {
        if (animate && rectangle != null) {
            Rectangle2D visibleRect = panel.getVisibleRect();
            double xMin = Math.max(rectangle.getX(), visibleRect.getX());
            double xMax = Math.min(rectangle.getX() + rectangle.getWidth(), visibleRect.getX() + visibleRect.getWidth() - 2);
            double width = xMax - xMin;
            double yMin = Math.max(rectangle.getY(), visibleRect.getY());
            double yMax = Math.min(rectangle.getY() + rectangle.getHeight(), visibleRect.getY() + visibleRect.getHeight() - 2);
            double height = yMax - yMin;
            drawRectangle.setRect(xMin, yMin, width, height);
            Graphics2D gc = (Graphics2D) panel.getGraphics();
            if (gc != null) {
                gc.setStroke(backgroundStroke);
                gc.setColor(Color.WHITE);
                gc.draw(drawRectangle);
                gc.setColor(highlightColor);
                if (even) {
                    gc.setStroke(evenStroke);
                    even = false;
                } else {
                    gc.setStroke(oddStroke);
                    even = true;
                }
                gc.draw(drawRectangle);
                gc.setStroke(basicStroke);
            }
        }
    }
}
