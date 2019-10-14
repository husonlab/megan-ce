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
import java.util.SortedSet;

/**
 * Axis panel
 * Daniel Huson, 9.2011
 */
public class AxisPanel extends BasePanel {
    private Alignment alignment;

    /**
     * constructor
     */
    public AxisPanel(SelectedBlock selectedBlock) {
        super(selectedBlock);
        MyMouseListener listener = new MyMouseListener();
        addMouseListener(listener);
        addMouseMotionListener(listener);
        revalidate();
    }

    private Alignment getAlignment() {
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
        super.paint(g);
        paintAxis(g);
        paintSelection(g);
    }

    private final Font axisFont = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    private final FontMetrics axisMetrics = getFontMetrics(axisFont);

    /**
     * Paints the axis of the alignment
     *
     * @param g0 the graphics context of the sequence panel
     */
    private void paintAxis(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        Rectangle rec = getVisibleRect();

        g.setColor(Color.WHITE);//new Color(0.93f, 0.97f, 0.96f));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setBackground(Color.WHITE);
        g.setFont(axisFont);
//            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (alignment != null) {
            final GapColumnContractor gapColumnContractor = getAlignment().getGapColumnContractor();
            {
                g.setColor(Color.LIGHT_GRAY);
                SortedSet<Integer> jumpColumns = gapColumnContractor.getJumpPositionsRelativeToLayoutColumns();
                for (Integer col : jumpColumns) {
                    // g.setColor(Color.LIGHT_GRAY);
                    // g.fillRect((int) getX(col) - 2,0,4,getSize().height-2);
                    if (cellWidth > 1) {
                        g.setColor(Color.WHITE);
                        g.drawLine((int) getX(col), -1, (int) getX(col), getSize().height);
                        g.setColor(Color.GRAY);
                        // g.drawRect((int) getX(col) - 2,0,4,getSize().height-2);
                        g.drawLine((int) getX(col) - 1, -1, (int) getX(col) - 1, getSize().height);
                        g.drawLine((int) getX(col) + 1, -1, (int) getX(col) + 1, getSize().height);
                    } else {
                        g.drawLine((int) getX(col), -1, (int) getX(col), getSize().height);
                    }

                }
            }
            g.setColor(Color.BLACK);

            int minVisibleCol = (int) Math.max(0, (rec.getX() / cellWidth)) + gapColumnContractor.getFirstLayoutColumn();
            int maxVisibleCol = (int) Math.min(gapColumnContractor.getLastLayoutColumn(), (rec.getX() + rec.getWidth()) / cellWidth);

            double dashWidth = axisMetrics.getStringBounds("-", g).getWidth();
            double maxLabelWidth = axisMetrics.getStringBounds("|" + Math.min(alignment.getLength(), maxVisibleCol), g).getWidth();
            int step = 1;
            for (; step <= 1000000; step *= 10) {
                if (maxLabelWidth < getX(step) - getX(0))
                    break;
            }

            Integer[] jumpCols = gapColumnContractor.getJumpPositionsRelativeToLayoutColumns().toArray(new Integer[0]);
            int jc = 0;
            int jumped = 0;
            int offsetDueToInsertedPositions = 0;

            if (alignment.getInsertionsIntoReference().size() > 0) {
                // need to count all offsets due to insertions into reference:
                for (int layoutCol = gapColumnContractor.getFirstLayoutColumn(); layoutCol < minVisibleCol; layoutCol++) {
                    while (jc < jumpCols.length && jumpCols[jc] < layoutCol) {
                        jumped += gapColumnContractor.getJumpBeforeLayoutColumn(jumpCols[jc]);
                        jc++;
                    }
                    int trueCol = layoutCol + jumped;
                    if (alignment.getInsertionsIntoReference().contains(trueCol))
                        offsetDueToInsertedPositions++;
                }
            }

            double lastPos = Float.MIN_VALUE;
            for (int layoutCol = minVisibleCol; layoutCol <= maxVisibleCol; layoutCol++) {
                while (jc < jumpCols.length && jumpCols[jc] < layoutCol) {
                    jumped += gapColumnContractor.getJumpBeforeLayoutColumn(jumpCols[jc]);
                    jc++;
                }
                int trueCol = layoutCol + jumped;
                if (alignment.getInsertionsIntoReference().contains(trueCol - 1)) {
                    offsetDueToInsertedPositions++;
                    if (cellWidth > dashWidth)
                        g.drawString("-", (int) (getX(layoutCol - 1) + (cellWidth - dashWidth) / 2), 11);
                } else {
                    trueCol -= offsetDueToInsertedPositions; // we don't count positions that are insertions into the reference sequence
                    if (trueCol > 0 && ((trueCol) % step) == 0) {
                        double layoutX = getX(layoutCol - 1) - 3;
                        if (lastPos + maxLabelWidth < layoutX) {
                            g.drawString("|" + trueCol, Math.round(layoutX), 11);
                            lastPos = layoutX;
                        }
                    }
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
            Rectangle2D rect = new Rectangle2D.Double(Math.max(0, getX(selectedBlock.getFirstCol())), 0, 0, 0);
            rect.add(Math.min(getX(selectedBlock.getLastCol() + 1), getSize().getWidth()), getSize().height);
            g.setColor(highlightColorSemiTransparent);
            g.fill(rect);
        }
    }

    /**
     * Adapts the grid parameters to the current sequenceFont. It is invoked every time the sequenceFont changes.
     */
    void revalidateGrid() {
        JScrollPane scrollPane = (JScrollPane) getParent().getParent();
        //scrollPane.revalidate();
        Dimension bounds = scrollPane.getPreferredSize();
        if (alignment != null)
            setSize((int) (cellWidth * alignment.getGapColumnContractor().getLayoutLength() + 0.5) + 3, bounds.height - 4);
        setPreferredSize(getSize());
        revalidate();
    }

    class MyMouseListener extends MouseAdapter implements MouseListener, MouseMotionListener {
        private final int inMove = 2;
        private final int inRubberband = 3;
        private final int inScrollByMouse = 4;

        private boolean stillDownWithoutMoving = false;

        Point mouseDown = null;
        private int current = 0;

        @Override
        public void mouseClicked(MouseEvent me) {
            super.mouseClicked(me);
            int inClick = 1;
            current = inClick;
            if (me.getClickCount() == 1) {
                if (me.isShiftDown()) {
                    if (selectedBlock.isSelected()) {
                        if (!selectedBlock.isSelectedCol(getCol(me.getPoint())))
                            selectedBlock.extendSelection(-1, getCol(me.getPoint()));
                        else
                            selectedBlock.reduceSelection(-1, getCol(me.getPoint()));
                    }
                } else {
                    selectedBlock.clear();
                }
            } else if (me.getClickCount() == 2) {
                selectedBlock.selectCol(getCol(me.getPoint()), alignment.isTranslate());
            }
        }

        @Override
        public void mousePressed(MouseEvent me) {
            super.mousePressed(me);
            mouseDown = me.getPoint();
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
            //setToolTipText();
        }

        @Override
        public void mouseDragged(MouseEvent me) {
            super.mouseDragged(me);
            stillDownWithoutMoving = false;
            if (current == inRubberband) {
                AxisPanel viewer = AxisPanel.this;
                Graphics2D gc = (Graphics2D) getGraphics();
                if (gc != null) {
                    Color color = viewer.getBackground() != null ? viewer.getBackground() : Color.WHITE;
                    gc.setXORMode(color);

                    int firstRow = 0;
                    int firstCol = Math.min(getCol(mouseDown), getCol(me.getPoint()));
                    int lastRow = Integer.MAX_VALUE;
                    int lastCol = Math.max(getCol(mouseDown), getCol(me.getPoint()));
                    getSelectedBlock().select(firstRow, firstCol, lastRow, lastCol, alignment.isTranslate());
                    paintSelection(gc);
                }
            } else if (current == inScrollByMouse) {
                if (AxisPanel.this.getParent() != null && AxisPanel.this.getParent().getParent() != null
                        && AxisPanel.this.getParent().getParent() instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) AxisPanel.this.getParent().getParent();
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
                    int firstCol = Math.min(getCol(mouseDown), getCol(me.getPoint()));
                    int lastCol = Math.max(getCol(mouseDown), getCol(me.getPoint()));
                    getSelectedBlock().selectCols(firstCol, lastCol, alignment.isTranslate());
                }
            }
            //AlignmentPanel.this.repaint();
            setCursor(Cursor.getDefaultCursor());
            current = 0;
        }
    }
}

