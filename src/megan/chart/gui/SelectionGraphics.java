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

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.LinkedList;
import java.util.Map;

/**
 * class for selecting objects instead of drawing them
 * Daniel Huson, 3.2013
 */
public class SelectionGraphics<T> extends Graphics2D {
    private final Graphics2D gc;

    private final LinkedList<T> selection = new LinkedList<>();
    private T currentItem = null;
    private T previouslySelectedItem = null;
    private Rectangle selectionRectangle;

    private final Rectangle rectangle = new Rectangle();
    private final Line2D line = new Line2D.Float();

    public enum Which {First, Last, All}

    private Which useWhich = Which.All;

    private boolean shiftDown = false;
    private int mouseClicks = 0;

    /**
     * constructor
     *
     * @param gc
     */
    public SelectionGraphics(Graphics gc) {
        this.gc = (Graphics2D) gc;
    }

    public void setMouseLocation(Point mouseLocation) {
        if (selectionRectangle == null)
            selectionRectangle = new Rectangle(mouseLocation.x - 2, mouseLocation.y - 2, 4, 4);
        else
            selectionRectangle.setRect(mouseLocation.x - 2, mouseLocation.y - 2, 4, 4);
    }

    public Rectangle getSelectionRectangle() {
        return selectionRectangle;
    }

    public void setSelectionRectangle(Rectangle selectionRectangle) {
        this.selectionRectangle = selectionRectangle;
    }

    /**
     * set the current label. If anything is hit while this is set, get selection will return it
     *
     * @param currentItem
     */
    public void setCurrentItem(T currentItem) {
        this.currentItem = currentItem;
    }

    /**
     * get the current label or null
     *
     * @return
     */
    public T getCurrentItem() {
        return currentItem;
    }

    /**
     * erase the current item
     */
    public void clearCurrentItem() {
        currentItem = null;
    }

    /**
     * get selected objects
     *
     * @return selection
     */
    public LinkedList<T> getSelectedItems() {
        return selection;
    }

    /**
     * erase selection
     */
    public void clearSelection() {
        selection.clear();
        previouslySelectedItem = null;
        shiftDown = false;
        mouseClicks = 0;
    }

    public boolean isShiftDown() {
        return shiftDown;
    }

    public void setShiftDown(boolean shiftDown) {
        this.shiftDown = shiftDown;
    }

    public int getMouseClicks() {
        return mouseClicks;
    }

    public void setMouseClicks(int mouseClicks) {
        this.mouseClicks = mouseClicks;
    }

    private void testForHit(Shape shape, boolean onStroke) {
        if (selectionRectangle != null && currentItem != null && currentItem != previouslySelectedItem) {
            //  if (gc != null && !gc.getTransform().isIdentity())
            //      shape = gc.getTransform().createTransformedShape(shape);
            if (shape instanceof Line2D) {
                if (shape.intersects(selectionRectangle)) {
                    selection.add(currentItem);
                    previouslySelectedItem = currentItem;
                }
            } else if (shape instanceof Rectangle2D) {
                if (selectionRectangle.intersects(shape.getBounds())) {
                    selection.add(currentItem);
                    previouslySelectedItem = currentItem;
                }
            } else {
                if ((new Area(shape)).intersects(selectionRectangle)) {
                    selection.add(currentItem);
                    previouslySelectedItem = currentItem;
                }
            }
        }
    }

    /**
     * which of the selected items should be used?
     *
     * @return use which
     */
    public Which getUseWhich() {
        return useWhich;
    }

    /**
     * set which to be used
     *
     * @param useWhich
     */
    public void setUseWhich(Which useWhich) {
        this.useWhich = useWhich;
    }

    // todo: all the draw operations

    public void draw(Shape shape) {
        testForHit(shape, false);
    }

    public void fill(Shape shape) {
        testForHit(shape, false);
    }

    public void drawString(String s, float x, float y) {
        drawString(s, Math.round(x), Math.round(y));
    }

    public void drawString(String s, int x, int y) {
        if (currentItem != null && gc != null) {
            Dimension labelSize = BasicSwing.getStringSize(gc, s, gc.getFont()).getSize();
            rectangle.setRect(x, y - labelSize.height, labelSize.width, labelSize.height);
            testForHit(rectangle, false);
        }
    }

    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        if (currentItem != null) {
            String string = new String(data, offset, length);
            drawString(string, x, y);
        }
    }

    public void drawChars(char[] data, int offset, int length, int x, int y) {
        if (currentItem != null) {
            String string = new String(data, offset, length);
            drawString(string, x, y);
        }
    }

    public void fillOval(int x, int y, int width, int height) {
        if (currentItem != null) {
            rectangle.setRect(x, y, width, height);
            testForHit(rectangle, false);
        }
    }

    public void drawOval(int x, int y, int width, int height) {
        if (currentItem != null) {
            rectangle.setRect(x, y, width, height);
            testForHit(rectangle, true);
        }
    }

    public void fillRect(int x, int y, int width, int height) {
        if (currentItem != null) {
            rectangle.setRect(x, y, width, height);
            testForHit(rectangle, false);
        }
    }

    public void fillPolygon(Polygon polygon) {
        if (currentItem != null) {
            testForHit(polygon, false);
        }
    }

    public void drawPolygon(int[] ints, int[] ints1, int i) {
        if (currentItem != null) {
            Polygon polygon = new Polygon(ints, ints1, i);
            testForHit(polygon, true);
        }
    }

    public void draw3DRect(int x, int y, int width, int height, boolean b) {
        if (currentItem != null) {
            rectangle.setRect(x, y, width, height);
            testForHit(rectangle, true);
        }
    }

    public void fill3DRect(int x, int y, int width, int height, boolean b) {
        if (currentItem != null) {
            rectangle.setRect(x, y, width, height);
            testForHit(rectangle, false);
        }
    }

    public void drawRect(int x, int y, int width, int height) {
        if (currentItem != null) {
            rectangle.setRect(x, y, width, height);
            testForHit(rectangle, true);
        }
    }

    public void fillPolygon(int[] ints, int[] ints1, int i) {
        if (currentItem != null) {
            Polygon polygon = new Polygon(ints, ints1, i);
            testForHit(polygon, false);
        }
    }

    public void drawLine(int i, int i1, int i2, int i3) {
        if (currentItem != null) {
            line.setLine(i, i1, i2, i3);
            testForHit(line, true);
        }
    }

    public void drawPolyline(int[] ints, int[] ints1, int i) {
        if (currentItem != null && gc != null) {
            gc.drawPolyline(ints, ints1, i);
        }
    }

    public void drawPolygon(Polygon polygon) {
        if (currentItem != null && gc != null) {
            gc.drawPolygon(polygon);
        }
    }

    public void fillRoundRect(int x, int y, int width, int height, int i4, int i5) {
        if (currentItem != null) {
            rectangle.setRect(x, y, width, height);
            testForHit(rectangle, false);
        }
    }

    public void drawRoundRect(int x, int y, int width, int height, int i4, int i5) {
        if (currentItem != null) {
            rectangle.setRect(x, y, width, height);
            testForHit(rectangle, true);
        }
    }

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (currentItem != null) {
            rectangle.setRect(x, y, width, height);
            Arc2D arc = new Arc2D.Float(rectangle, startAngle, arcAngle, Arc2D.PIE);
            testForHit(arc, true);
        }
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (currentItem != null) {
            rectangle.setRect(x, y, width, height);
            Arc2D arc = new Arc2D.Float(rectangle, startAngle, arcAngle, Arc2D.PIE);
            testForHit(arc, false);
        }
    }

    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        if (currentItem != null) {
            rectangle.setRect(x + dx, y + dy, width, height);
            testForHit(rectangle, false);
        }
    }

    public void drawImage(BufferedImage bufferedImage, BufferedImageOp bufferedImageOp, int x, int y) {
        if (currentItem != null) {
            rectangle.setRect(x, y, bufferedImage.getWidth(), bufferedImage.getHeight());
            testForHit(rectangle, false);
        }
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        if (currentItem != null) {
            rectangle.setRect(dx1, dy1, dx2 - dx1, dy2 - dy1);
            testForHit(rectangle, false);
        }
        return true;
    }

    public boolean drawImage(Image image, int x, int y, int width, int height, Color color, ImageObserver imageObserver) {
        if (currentItem != null) {
            rectangle.setRect(x, y, width, height);
            testForHit(rectangle, false);
        }
        return true;
    }

    public boolean drawImage(Image image, int x, int y, Color color, ImageObserver imageObserver) {
        if (currentItem != null) {
            rectangle.setRect(x, y, image.getWidth(imageObserver), image.getWidth(imageObserver));
            testForHit(rectangle, false);
        }
        return true;
    }

    public boolean drawImage(Image image, int x, int y, ImageObserver imageObserver) {
        if (currentItem != null) {
            rectangle.setRect(x, y, image.getWidth(imageObserver), image.getWidth(imageObserver));
            testForHit(rectangle, false);
        }
        return true;
    }

    public boolean drawImage(Image image, int x, int y, int width, int height, ImageObserver imageObserver) {
        if (currentItem != null) {
            rectangle.setRect(x, y, width, height);
            testForHit(rectangle, false);
        }
        return true;
    }


    public void drawRenderableImage(RenderableImage renderableImage, AffineTransform affineTransform) {
        if (currentItem != null) {
            // todo: implement when required
        }
    }

    public void drawRenderedImage(RenderedImage renderedImage, AffineTransform affineTransform) {
        if (currentItem != null) {
            // todo: implement when required
        }
    }

    public void drawGlyphVector(GlyphVector glyphVector, float x, float y) {
        if (currentItem != null) {
            testForHit(glyphVector.getOutline(x, y), false);
        }
    }

    public boolean drawImage(Image image, AffineTransform affineTransform, ImageObserver imageObserver) {
        if (currentItem != null) {
            // todo: implement when required
        }
        return true;
    }

    public void drawString(AttributedCharacterIterator attributedCharacterIterator, float v, float v1) {
        if (currentItem != null) {
            // todo: implement when required
        }
    }

    public void drawString(AttributedCharacterIterator attributedCharacterIterator, int i, int i1) {
        if (currentItem != null) {
            // todo: implement when required
        }
    }

    // todo: leave all below as is:

    public void rotate(double v, double v1, double v2) {
        if (gc != null)
            gc.rotate(v, v1, v2);
    }

    public void setComposite(Composite composite) {
        if (gc != null) gc.setComposite(composite);
    }

    public void setRenderingHints(Map<?, ?> map) {
        if (gc != null) gc.setRenderingHints(map);
    }

    public void shear(double v, double v1) {
        if (gc != null) gc.shear(v, v1);
    }

    public void translate(double v, double v1) {
        if (gc != null) gc.translate(v, v1);
    }

    public Stroke getStroke() {
        if (gc != null)
            return gc.getStroke();
        else
            return null;
    }

    public boolean hit(Rectangle rectangle, Shape shape, boolean b) {
        if (gc != null)
            return gc.hit(rectangle, shape, b);
        else
            return false;
    }

    public void setBackground(Color color) {
        if (gc != null) gc.setBackground(color);
    }

    public void transform(AffineTransform affineTransform) {
        if (gc != null) gc.transform(affineTransform);
    }

    public void setStroke(Stroke stroke) {
        if (gc != null) gc.setStroke(stroke);
    }

    public void setRenderingHint(RenderingHints.Key key, Object o) {
        if (gc != null) gc.setRenderingHint(key, o);
    }

    public void rotate(double v) {
        if (gc != null) gc.rotate(v);
    }

    public RenderingHints getRenderingHints() {
        if (gc != null) return gc.getRenderingHints();
        else
            return null;
    }

    public FontRenderContext getFontRenderContext() {
        if (gc != null) return gc.getFontRenderContext();
        else
            return null;
    }

    public void setPaint(Paint paint) {
        if (gc != null) gc.setPaint(paint);
    }

    public AffineTransform getTransform() {
        if (gc != null) return gc.getTransform();
        else return null;
    }

    public GraphicsConfiguration getDeviceConfiguration() {
        if (gc != null) return gc.getDeviceConfiguration();
        else return null;
    }

    public Object getRenderingHint(RenderingHints.Key key) {
        if (gc != null) return gc.getRenderingHint(key);
        else return null;
    }

    public void setTransform(AffineTransform affineTransform) {
        if (gc != null) gc.setTransform(affineTransform);
    }

    public Composite getComposite() {
        if (gc != null) return gc.getComposite();
        else return null;
    }

    public Color getBackground() {
        if (gc != null) return gc.getBackground();
        else return null;
    }

    public Paint getPaint() {
        if (gc != null) return gc.getPaint();
        else return null;
    }

    public Graphics create() {
        if (gc != null) return gc.create();
        else return null;
    }

    public void clipRect(int i, int i1, int i2, int i3) {
        if (gc != null) gc.clipRect(i, i1, i2, i3);
    }

    public void dispose() {
        if (gc != null)
            gc.dispose();
    }

    public FontMetrics getFontMetrics() {
        if (gc != null) return gc.getFontMetrics();
        else return null;
    }

    public Color getColor() {
        if (gc != null) return gc.getColor();
        else return null;
    }

    public Rectangle getClipBounds(Rectangle rectangle) {
        if (gc != null)
            return gc.getClipBounds(rectangle);
        else return null;
    }

    public FontMetrics getFontMetrics(Font font) {
        if (gc != null) return gc.getFontMetrics(font);
        else return null;
    }

    public void setClip(int i, int i1, int i2, int i3) {
        if (gc != null) gc.setClip(i, i1, i2, i3);
    }

    public Font getFont() {
        if (gc != null) return gc.getFont();
        else return null;
    }


    public Graphics create(int i, int i1, int i2, int i3) {
        if (gc != null) return gc.create(i, i1, i2, i3);
        else return null;
    }

    public Shape getClip() {
        if (gc != null) return gc.getClip();
        else return null;
    }

    public void setPaintMode() {
        if (gc != null) gc.setPaintMode();
    }

    public void translate(int i, int i1) {
        if (gc != null) gc.translate(i, i1);
    }

    public boolean hitClip(int i, int i1, int i2, int i3) {
        if (gc != null) return gc.hitClip(i, i1, i2, i3);
        else return false;
    }

    public void setColor(Color color) {
        if (gc != null)
            gc.setColor(color);
    }

    public void clearRect(int i, int i1, int i2, int i3) {
        if (gc != null) gc.clearRect(i, i1, i2, i3);
    }

    public void setClip(Shape shape) {
        if (gc != null) gc.setClip(shape);
    }


    public void setFont(Font font) {
        if (gc != null) gc.setFont(font);
    }

    public boolean drawImage(Image image, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7, ImageObserver imageObserver) {
        if (gc != null)
            return gc.drawImage(image, i, i1, i2, i3, i4, i5, i6, i7, imageObserver);
        else
            return false;
    }

    public Rectangle getClipRect() {
        if (gc != null) return gc.getClipRect();
        else return null;
    }

    public Rectangle getClipBounds() {
        if (gc != null) return gc.getClipBounds();
        else return null;
    }

    public void setXORMode(Color color) {
        if (gc != null) gc.setXORMode(color);
    }

    public void addRenderingHints(Map<?, ?> map) {
        if (gc != null)
            gc.addRenderingHints(map);
    }

    public void scale(double v, double v1) {
        if (gc != null) gc.scale(v, v1);
    }

    public void clip(Shape shape) {
        if (gc != null) gc.clip(shape);
    }
}
