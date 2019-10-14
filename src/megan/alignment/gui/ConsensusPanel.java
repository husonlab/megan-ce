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
import megan.alignment.gui.colors.ColorSchemeText;
import megan.alignment.gui.colors.IColorScheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.SortedSet;

/**
 * consensus panel
 * Daniel Huson, 4.2012
 */
public class ConsensusPanel extends BasePanel {
    private Alignment alignment;
    private IColorScheme colorScheme = new ColorSchemeText();
    private boolean showColors = true;

    /**
     * constructor
     */
    public ConsensusPanel(SelectedBlock selectedBlock) {
        super(selectedBlock);
        MyMouseListener listener = new MyMouseListener();
        addMouseListener(listener);
        addMouseMotionListener(listener);
        setToolTipText("Consensus sequence");
        revalidate();
    }

    private Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
        revalidateGrid();
    }

    private IColorScheme getColorScheme() {
        return colorScheme;
    }

    public void setColorScheme(IColorScheme colorScheme) {
        this.colorScheme = colorScheme;
    }

    private boolean isShowColors() {
        return showColors;
    }

    public void setShowColors(boolean showColors) {
        this.showColors = showColors;
    }

    /**
     * paint
     *
     * @param g
     */
    public void paint(Graphics g) {
        super.paint(g);
        paintConsensus(g);
        paintSelection(g);
    }

    /**
     * Paints the axis of the alignment
     *
     * @param g0 the graphics context of the sequence panel
     */
    private void paintConsensus(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        Rectangle rec = getVisibleRect();

        g.setColor(Color.WHITE);//new Color(0.93f, 0.97f, 0.96f));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.BLACK);
        g.setBackground(Color.WHITE);
        g.setFont(sequenceFont);
//            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final Lane consensusSequence = alignment.getConsensus();

        if (alignment.getConsensus() != null) {
            final GapColumnContractor gapColumnContractor = getAlignment().getGapColumnContractor();

            int minVisibleCol = (int) Math.max(0, (rec.getX() / cellWidth)) + gapColumnContractor.getFirstOriginalColumn();
            int maxVisibleCol = (int) Math.min(gapColumnContractor.getLastOriginalColumn() - 1, (rec.getX() + rec.getWidth()) / cellWidth);

            if (minVisibleCol - 3 > 0)
                minVisibleCol -= 3; // just to cover previous codon

            if ((!alignment.isTranslate() && cellWidth < 1) || cellWidth < 0.5) {
                final Lane lane = alignment.getConsensus();

                int firstLayoutCol = lane.getFirstNonGapPosition();

                Integer[] jumpCols = gapColumnContractor.getJumpPositionsRelativeToOriginalColumns().toArray(new Integer[0]);
                if (jumpCols.length > 0) {
                    int jc = 0;
                    int jumped = 0;

                    while (jc < jumpCols.length && jumpCols[jc] <= firstLayoutCol) {
                        jumped += gapColumnContractor.getJumpBeforeOriginalColumn(jumpCols[jc]);
                        jc++;
                    }
                    firstLayoutCol -= jumped;
                }

                int lastLayoutCol = lane.getLastNonGapPosition();
                if (jumpCols.length > 0) {
                    int jc = 0;
                    int jumped = 0;

                    while (jc < jumpCols.length && jumpCols[jc] < lastLayoutCol) {
                        jumped += gapColumnContractor.getJumpBeforeOriginalColumn(jumpCols[jc]);
                        jc++;
                    }
                    lastLayoutCol -= jumped;
                }

                double firstX = getX(firstLayoutCol);
                double lastX = getX(lastLayoutCol);
                if (firstX <= maxVisibleCol && lastX >= minVisibleCol) {
                    g.setColor(Color.GRAY);
                    g.fill(new Rectangle2D.Double(firstX, 0, lastX - firstX, getHeight()));
                }
            } else {
                final Rectangle2D drawRect = new Rectangle2D.Float();

                if (isShowColors() && getColorScheme() != null) {          // color scheme selected?
                    g.setColor(Color.WHITE);

                    Integer[] jumpCols = gapColumnContractor.getJumpPositionsRelativeToLayoutColumns().toArray(new Integer[0]);
                    int jc = 0;
                    int jumped = 0;
                    int colorStreak = 0;
                    for (int layoutCol = minVisibleCol; layoutCol <= maxVisibleCol; layoutCol++) {

                        while (jc < jumpCols.length && jumpCols[jc] <= layoutCol) {
                            jumped += gapColumnContractor.getJumpBeforeLayoutColumn(jumpCols[jc]);
                            jc++;
                        }
                        int trueCol = layoutCol + jumped;

                        char ch = consensusSequence.charAt(trueCol);


                        // draw colors
                        if (ch == 0 || ch == '-') {
                            g.setColor(Color.WHITE);
                        } else if (ch != ' ') {
                            g.setColor(getColorScheme().getBackground(ch));
                            colorStreak = 0;
                        } else { // only repeat the same color 3 times (for a codon...)
                            colorStreak++;
                            if (colorStreak == 3) {
                                g.setColor(Color.WHITE);
                                colorStreak = 0;
                            }
                        }

                        if (!g.getColor().equals(Color.WHITE)) {
                            drawRect.setRect(getX(layoutCol) - 1, 0, cellWidth, getSize().height);
                            g.fill(drawRect);

                        }
                    }
                    g.setColor(Color.BLACK);
                }
            }

            if (cellWidth > 4) {
                Integer[] jumpCols = gapColumnContractor.getJumpPositionsRelativeToLayoutColumns().toArray(new Integer[0]);
                int jc = 0;
                int jumped = 0;

                for (int layoutCol = minVisibleCol; layoutCol <= maxVisibleCol; layoutCol++) {
                    while (jc < jumpCols.length && jumpCols[jc] <= layoutCol) {
                        jumped += gapColumnContractor.getJumpBeforeLayoutColumn(jumpCols[jc]);
                        jc++;
                    }
                    int trueCol = layoutCol + jumped;
                    if (trueCol < gapColumnContractor.getLastOriginalColumn()) {
                        char ch = consensusSequence.charAt(trueCol);
                        if (ch == 0)
                            ch = '-';

                        // draw colors
                        if (isShowColors() && getColorScheme() != null) {          // color scheme selected?
                            g.setColor(Color.BLACK);
                        }
                        g.drawString("" + ch, Math.round(getX(layoutCol)),
                                (int) Math.round(getSize().height - 0.5 * (getSize().height - cellHeight)) - 2);
                    }
                }
            }

            if (cellWidth > 1) {
                SortedSet<Integer> jumpColumns = gapColumnContractor.getJumpPositionsRelativeToLayoutColumns();
                for (Integer col : jumpColumns) {
                    g.setColor(Color.WHITE);
                    g.drawLine((int) getX(col), -1, (int) getX(col), getSize().height);
                    g.setColor(Color.GRAY);
                    g.drawLine((int) getX(col) - 1, -1, (int) getX(col) - 1, getSize().height);
                    g.drawLine((int) getX(col) + 1, -1, (int) getX(col) + 1, getSize().height);
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
            g.setColor(highlightColor);
            g.draw(rect);
        }
    }

    /**
     * Adapts the grid parameters to the current sequenceFont. It is invoked every time the sequenceFont changes.
     */
    void revalidateGrid() {
        if (alignment != null) {
            setSize((int) (cellWidth * (alignment.getGapColumnContractor().getLayoutLength()) + 0.5) + 3, (int) Math.max(20, cellHeight));
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
                ConsensusPanel viewer = ConsensusPanel.this;
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
                if (ConsensusPanel.this.getParent() != null && ConsensusPanel.this.getParent().getParent() != null
                        && ConsensusPanel.this.getParent().getParent() instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) ConsensusPanel.this.getParent().getParent();
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

