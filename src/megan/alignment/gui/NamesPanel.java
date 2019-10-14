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

import jloda.swing.util.Cursors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;

/**
 * Names panel
 * Daniel Huson, 9.2011
 */
public class NamesPanel extends BasePanel {
    private Alignment alignment;

    /**
     * constructor
     */
    public NamesPanel(final SelectedBlock selectedBlock) {
        super(selectedBlock);
        selectedBlock.addSelectionListener((selected, minRow, minCol, maxRow, maxCol) -> repaint());

        MyMouseListener listener = new MyMouseListener();
        addMouseListener(listener);
        addMouseMotionListener(listener);
        revalidate();
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
        revalidateGrid();
    }

    /**
     * paint
     *
     * @param g
     */
    public void paint(Graphics g) {
        try {
            super.paint(g);
            paintNames(g);
            paintSelection(g);
        } catch (Exception ignored) {
        }
    }

    /**
     * Paints the names of the alignment
     *
     * @param g0 the graphics context of the sequence panel
     */
    private void paintNames(Graphics g0) {
        final Graphics2D g = (Graphics2D) g0;
        final Rectangle visibleRect = getVisibleRect();
        final Rectangle2D drawRect = new Rectangle2D.Double();

        g.setColor(Color.WHITE);//new Color(0.93f, 0.97f, 0.96f));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setBackground(Color.WHITE);
        if (sequenceFont.getSize() > 14)
            sequenceFont = sequenceFont.deriveFont(14.0f);
        g.setFont(sequenceFont);
        boolean showText = (sequenceFont.getSize() > 6);
        if (showText)
            g.setColor(Color.BLACK);
        else
            g.setColor(Color.GRAY);

        if (alignment != null && !alignment.getRowCompressor().isEnabled()) {
            int minVisibleRow = (int) Math.max(0, (visibleRect.getY() / cellHeight));
            int maxVisibleRow = (int) Math.min(alignment.getNumberOfSequences() - 1, (visibleRect.getY() + visibleRect.getHeight()) / cellHeight);

            for (int row = minVisibleRow; row <= maxVisibleRow; row++) {
                String name = alignment.getName(row);
                int y = (int) Math.round(getY(row)) - 2;
                if (showText)
                    g.drawString(name, Math.round(getX(0)), y - (int) (cellHeight - sequenceFont.getSize()) / 2);
                else {
                    drawRect.setRect(0, getY(row) - cellHeight + 2, getX(name.length()), Math.max(1, cellHeight - 1));
                    g.fill(drawRect);
                }

            }
        }
    }

    /**
     * paint the selection rectangle
     *
     * @param g0
     */
    private void paintSelection(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        SelectedBlock selectedBlock = getSelectedBlock();
        if (selectedBlock.isSelected()) {
            Rectangle2D rect = new Rectangle2D.Double(0, getY(selectedBlock.getFirstRow() - 1), 0, 0);
            rect.add(getSize().getWidth(), Math.min(getSize().height, getY(selectedBlock.getLastRow())));
            g.setColor(highlightColorSemiTransparent);
            g.fill(rect);
            // g.setColor(highlightColor);
            // g.draw(rect);
        }
    }


    /**
     * Adapts the grid parameters to the current sequenceFont. It is invoked every time the sequenceFont changes.
     */
    void revalidateGrid() {
        JScrollPane scrollPane = (JScrollPane) NamesPanel.this.getParent().getParent();
        //scrollPane.revalidate();
        Dimension bounds = scrollPane.getPreferredSize();
        if (alignment != null && !alignment.getRowCompressor().isEnabled()) {
            int width = (int) Math.max(bounds.width, cellWidth * alignment.getMaxNameLength());
            setSize(width, (int) (cellHeight * alignment.getNumberOfSequences() + 0.5) + 3);
        }
        setPreferredSize(getSize());
        revalidate();
    }

    class MyMouseListener extends MouseAdapter implements MouseListener, MouseMotionListener {
        private final int inMove = 2;
        private final int inRubberband = 3;
        private final int inScrollByMouse = 4;

        private boolean stillDownWithoutMoving = false;

        Point mouseDown = null;
        private boolean paintedRubberband = false;
        private int current = 0;

        @Override
        public void mouseClicked(MouseEvent me) {
            super.mouseClicked(me);
            int inClick = 1;
            current = inClick;
            if (me.getClickCount() == 1) {
                if (me.isShiftDown()) {
                    if (selectedBlock.isSelected()) {
                        if (!selectedBlock.isSelectedRow(getRow(me.getPoint())))
                            selectedBlock.extendSelection(getRow(me.getPoint()), -1);
                        else
                            selectedBlock.reduceSelection(getRow(me.getPoint()), -1);
                    }
                } else if (!selectedBlock.contains(getRow(me.getPoint()), getCol(me.getPoint()))) {
                    selectedBlock.clear();
                }
            } else if (me.getClickCount() == 2) {
                if (!me.isAltDown())
                    selectedBlock.selectRow(getRow(me.getPoint()));
            }
        }

        @Override
        public void mousePressed(MouseEvent me) {
            super.mousePressed(me);
            mouseDown = me.getPoint();
            paintedRubberband = false;
            boolean shiftDown = me.isShiftDown();

            if (me.isAltDown() || me.isShiftDown()) {
                current = inRubberband;
                setCursor(Cursor.getDefaultCursor());
            } else {
                current = inScrollByMouse;
                setCursor(Cursors.getClosedHand());

                stillDownWithoutMoving = true;

                final Thread worker = new Thread(new Runnable() {
                    public void run() {
                        try {
                            synchronized (this) {
                                wait(500);
                            }
                        } catch (InterruptedException ignored) {
                        }
                        if (stillDownWithoutMoving) {
                            current = inRubberband;
                            setCursor(Cursor.getDefaultCursor());
                        }
                    }
                });
                worker.setPriority(Thread.currentThread().getPriority() - 1);
                worker.start();
            }
        }

        @Override
        public void mouseMoved(MouseEvent me) {
            super.mouseMoved(me);
            stillDownWithoutMoving = false;
            setToolTipText(alignment.getToolTip(getRow(me.getPoint())));
        }

        @Override
        public void mouseDragged(MouseEvent me) {
            super.mouseDragged(me);
            stillDownWithoutMoving = false;
            if (current == inRubberband) {
                NamesPanel viewer = NamesPanel.this;
                Graphics2D gc = (Graphics2D) getGraphics();
                if (gc != null) {
                    Color color = viewer.getBackground() != null ? viewer.getBackground() : Color.WHITE;
                    gc.setXORMode(color);
                    if (paintedRubberband)
                        paintSelection(gc);
                    else
                        paintedRubberband = true;

                    int firstRow = Math.min(getRow(mouseDown), getRow(me.getPoint()));
                    int firstCol = 0;
                    int lastRow = Math.max(getRow(mouseDown), getRow(me.getPoint()));
                    int lastCol = Integer.MAX_VALUE;
                    getSelectedBlock().select(firstRow, firstCol, lastRow, lastCol, alignment.isTranslate());
                    paintSelection(gc);
                }
            } else if (current == inScrollByMouse) {
                if (NamesPanel.this.getParent() != null && NamesPanel.this.getParent().getParent() != null
                        && NamesPanel.this.getParent().getParent() instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) NamesPanel.this.getParent().getParent();
                    int dX = me.getX() - mouseDown.x;
                    int dY = me.getY() - mouseDown.y;

                    if (dY != 0) {
                        JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
                        int amount = Math.round(dY * (scrollBar.getMaximum() - scrollBar.getMinimum()) / getHeight());
                        if (amount != 0) {
                            scrollBar.setValue(scrollBar.getValue() - amount);
                        }
                    }
                    if (dX != 0) {
                        JScrollBar scrollBar = scrollPane.getHorizontalScrollBar();
                        int amount = Math.round(dX * (scrollBar.getMaximum() - scrollBar.getMinimum()) / getWidth());
                        if (amount != 0) {
                            scrollBar.setValue(scrollBar.getValue() - amount);
                        }
                    }
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            super.mouseReleased(me);
            stillDownWithoutMoving = false;
            if (current == inRubberband) {
                if (!me.getPoint().equals(mouseDown)) {
                    int firstRow = Math.min(getRow(mouseDown), getRow(me.getPoint()));
                    int lastRow = Math.max(getRow(mouseDown), getRow(me.getPoint()));
                    getSelectedBlock().selectRows(firstRow, lastRow);
                }
            }
            //AlignmentPanel.this.repaint();
            setCursor(Cursor.getDefaultCursor());
            current = 0;
        }
    }
}

