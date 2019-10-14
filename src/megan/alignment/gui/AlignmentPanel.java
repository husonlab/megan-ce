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

import jloda.swing.director.ProjectManager;
import jloda.swing.util.Cursors;
import jloda.swing.util.ToolTipHelper;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.alignment.gui.colors.ColorSchemeText;
import megan.alignment.gui.colors.IColorScheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import java.util.SortedSet;

/**
 * a sequence alignment panel
 * Daniel Huson, 9.2011
 */
public class AlignmentPanel extends BasePanel {
    private Alignment alignment = new Alignment();
    private IColorScheme colorScheme = new ColorSchemeText();
    private boolean showColors = true;
    private boolean showUnalignedChars = false;
    private final AnimatedRectangle selectionRectangle;
    private final ToolTipHelper toolTipHelper;

    private boolean colorMatchesVsReference = false;
    private boolean colorMismatchesVsReference = true;
    private boolean colorMatchesVsConsensus = false;
    private boolean colorMismatchesVsConsensus = false;

    // used in drawing:
    private final Rectangle2D drawRect = new Rectangle2D.Double();
    private final Line2D drawLine = new Line2D.Double();

    /**
     * constructor
     */
    public AlignmentPanel(final SelectedBlock selectedBlock) {
        super(selectedBlock);
        selectedBlock.addSelectionListener((selected, minRow, minCol, maxRow, maxCol) -> repaint());
        toolTipHelper = new ToolTipHelper(this) {
            public String computeToolTip(Point mousePosition) {
                int read = alignment.getHitRead(getRow(mousePosition), getCol(mousePosition));
                if (read != -1)
                    return alignment.getToolTip(read);
                else
                    return "";
            }
        };

        MyMouseListener listener = new MyMouseListener();
        addMouseListener(listener);
        addMouseMotionListener(listener);
        selectionRectangle = new AnimatedRectangle(this);
        revalidate();
        setDoubleBuffered(true);
        colorMatchesVsReference = ProgramProperties.get("ColorMatchesVsReference", colorMatchesVsReference);
        colorMismatchesVsReference = ProgramProperties.get("ColorMismatchesVsReference", colorMismatchesVsReference);
        colorMatchesVsConsensus = ProgramProperties.get("ColorMatchesVsConsensus", colorMatchesVsConsensus);
        colorMismatchesVsConsensus = ProgramProperties.get("ColorMismatchesVsConsensus", colorMismatchesVsConsensus);
    }

    /**
     * call this when window is destroyed to release tooltip thread
     */
    public void close() {
        toolTipHelper.shutdownNow();
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
        JScrollPane scrollPane = getScrollPane();
        if (scrollPane != null) {
            scrollPane.getHorizontalScrollBar().setMinimum(0);
            scrollPane.getHorizontalScrollBar().setMaximum(alignment.getLength());
            scrollPane.getVerticalScrollBar().setMinimum(0);
            scrollPane.getVerticalScrollBar().setMaximum(alignment.getRowCompressor().getNumberRows());
        }
        revalidateGrid();
    }

    public void setAnimateSelection(boolean animate) {
        selectionRectangle.setAnimate(animate);
    }

    /**
     * paint
     *
     * @param g
     */
    public void paint(Graphics g) {
        try {
            super.paint(g);
            paintSequences(g);
            paintSelection(g);
        } catch (Exception ignored) {
        }
    }

    /**
     * Paints the sequences in the alignment
     *
     * @param g0 the graphics context of the sequence panel
     */
    private void paintSequences(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        Rectangle visibleRect = getVisibleRect();
        // add preceeding column so that we see their letters, even when only partially
        if (visibleRect.getX() - cellWidth >= 0)
            visibleRect.setRect(visibleRect.getX() - cellWidth, visibleRect.getY(), visibleRect.getWidth() + cellWidth, visibleRect.getHeight());

        g.setColor(Color.WHITE);
        //g.setColor(new Color(0.93f, 0.97f, 0.96f));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.BLACK);
        g.setBackground(Color.WHITE);
        g.setFont(sequenceFont);
        //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final Lane referenceSequence = alignment.getReference();
        final Lane consensusSequence = alignment.getConsensus();

        if (alignment != null) {
            final GapColumnContractor gapColumnContractor = getAlignment().getGapColumnContractor();
            final RowCompressor rowCompressor = getAlignment().getRowCompressor();

            boolean showGaps = (!rowCompressor.isEnabled());

            int minVisibleRow = (int) Math.max(0, (visibleRect.getY() / cellHeight) - 1);
            int maxVisibleRow = (int) Math.min(rowCompressor.getNumberRows() - 1, (visibleRect.getY() + visibleRect.getHeight()) / cellHeight);
            int minVisibleCol = (int) Math.max(0, (visibleRect.getX() / cellWidth)) + gapColumnContractor.getFirstOriginalColumn();
            int maxVisibleCol = (int) Math.min(gapColumnContractor.getLastOriginalColumn() - 1, (visibleRect.getX() + visibleRect.getWidth()) / cellWidth);

            if (minVisibleCol - 3 > 0)
                minVisibleCol -= 3; // just to cover previous codon

            if ((!alignment.isTranslate() && cellWidth < 1) || cellWidth < 0.5) {    // very small, draw gray bars
                minVisibleCol = 0;
                g.setColor(Color.GRAY);
                Integer[] jumpCols = gapColumnContractor.getJumpPositionsRelativeToOriginalColumns().toArray(new Integer[0]);
                for (int row = minVisibleRow; row <= maxVisibleRow; row++) {
                    for (int read : rowCompressor.getCompressedRow2Reads(row)) {
                        Lane lane = alignment.getLane(read);
                        if (lane != null) {
                            int firstLayoutCol = lane.getFirstNonGapPosition();

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
                            if (firstLayoutCol <= maxVisibleCol && lastLayoutCol >= minVisibleCol) {
                                drawRect.setRect(firstX - 1, getY(row) - cellHeight, Math.max(2, lastX - firstX), Math.max(1, cellHeight - 1));
                                g.fill(drawRect);
                            }
                        }
                    }
                }
                g.setColor(Color.BLACK);
            } else {  // not very small, draw colored blocks
                if (showColors && colorScheme != null) {          // color scheme selected?
                    boolean notTiny = (cellHeight > 6);
                    final int inset = notTiny ? 1 : 0;
                    Integer[] jumpCols = gapColumnContractor.getJumpPositionsRelativeToLayoutColumns().toArray(new Integer[0]);
                    g.setColor(Color.WHITE);
                    for (int row = minVisibleRow; row <= maxVisibleRow; row++) {
                        for (int read : rowCompressor.getCompressedRow2Reads(row)) {
                            int jc = 0;
                            int jumped = 0;
                            Lane lane = alignment.getLane(read);
                            for (int layoutCol = minVisibleCol; layoutCol <= maxVisibleCol; layoutCol++) {
                                while (jc < jumpCols.length && jumpCols[jc] <= layoutCol) {
                                    jumped += gapColumnContractor.getJumpBeforeLayoutColumn(jumpCols[jc]);
                                    jc++;
                                }
                                int trueCol = layoutCol + jumped;
                                if (trueCol < alignment.getLength()) {
                                    double x = getX(layoutCol) - 1;
                                    double y = getY(row);

                                    if (notTiny && lane != null && trueCol == lane.getFirstNonGapPosition()) {
                                        g.setColor(Color.LIGHT_GRAY);
                                        drawLine.setLine(x - 2, y - cellHeight + inset, x - 2, y - inset);
                                        g.draw(drawLine);
                                    }

                                    char ch = Objects.requireNonNull(lane).charAt(trueCol);

                                    if (ch != 0) {
                                        if (ch != ' ') {
                                            if ((colorMatchesVsReference && ch == referenceSequence.charAt(trueCol))
                                                    || (colorMismatchesVsReference && ch != referenceSequence.charAt(trueCol)) || (colorMatchesVsConsensus && ch == consensusSequence.charAt(trueCol))
                                                    || (colorMismatchesVsConsensus && ch != consensusSequence.charAt(trueCol)))
                                                g.setColor(getColorScheme().getBackground(ch));
                                            else
                                                g.setColor(Color.LIGHT_GRAY);
                                        }
                                        drawRect.setRect(x, y - cellHeight + inset, cellWidth, cellHeight - inset);
                                        g.fill(drawRect);
                                    }
                                    if (notTiny && trueCol == lane.getLastNonGapPosition() - 1) // todo: does this mean that we skip the last column?
                                    {
                                        g.setColor(Color.LIGHT_GRAY);
                                        drawLine.setLine(x + cellWidth + 1, y - cellHeight + inset, x + cellWidth + 1, y - inset);
                                        g.draw(drawLine);
                                    }

                                }
                            }
                        }
                    }
                }
            }

            if (sequenceFont.getSize() > 6) {    // font is big enough, draw text
                g.setColor(Color.BLACK);
                Integer[] jumpCols = gapColumnContractor.getJumpPositionsRelativeToLayoutColumns().toArray(new Integer[0]);
                for (int row = minVisibleRow; row <= maxVisibleRow; row++) {
                    for (int read : rowCompressor.getCompressedRow2Reads(row)) {
                        int jc = 0;
                        int jumped = 0;
                        Lane lane = alignment.getLane(read);
                        for (int layoutCol = minVisibleCol; layoutCol <= maxVisibleCol; layoutCol++) {
                            while (jc < jumpCols.length && jumpCols[jc] <= layoutCol) {
                                jumped += gapColumnContractor.getJumpBeforeLayoutColumn(jumpCols[jc]);
                                jc++;
                            }
                            int trueCol = layoutCol + jumped;
                            if (trueCol < alignment.getLength()) {
                                double xCoord = getX(layoutCol);
                                double yCoord = getY(row) - 0.5 * cellHeight + 0.3 * getSequenceFont().getSize();
                                if (visibleRect.contains(xCoord, yCoord)) {
                                    if (isShowUnalignedChars() && lane.hasUnalignedCharAt(trueCol)) {
                                        char ch = lane.getUnalignedCharAt(trueCol);
                                        if (ch != '-') {
                                            g.setColor(Color.GRAY);
                                            g.drawString("" + ch, Math.round(xCoord), Math.round(yCoord));
                                        }
                                    } else {
                                        char ch = lane.charAt(trueCol);

                                        if (ch == 0) {
                                            if (!showGaps) {
                                                continue;
                                            } else ch = '-';
                                        }
                                        if (ch == ' ') {
                                            continue;
                                        }

                                        // draw colors
                                        if (showColors && colorScheme != null) {          // color scheme selected?
                                            g.setColor(Color.BLACK);
                                        }
                                        g.drawString("" + ch, Math.round(xCoord), Math.round(yCoord));
                                    }
                                }
                            }
                        }
                    }
                }
                // System.err.println("Rows painted: "+(maxVisibleRow-minVisibleRow+1));
                // System.err.println("Cols painted: "+(maxVisibleCol-minVisibleCol+1));
            }

            SortedSet<Integer> jumpColumns = gapColumnContractor.getJumpPositionsRelativeToLayoutColumns();
            for (Integer col : jumpColumns) {
                if (cellWidth > 1) {
                    g.setColor(Color.WHITE);
                    g.drawLine((int) getX(col), -1, (int) getX(col), getSize().height);
                    g.setColor(Color.GRAY);
                    g.drawLine((int) getX(col) - 1, -1, (int) getX(col) - 1, getSize().height);
                    g.drawLine((int) getX(col) + 1, -1, (int) getX(col) + 1, getSize().height);
                } else {
                    g.setColor(Color.LIGHT_GRAY);
                    g.drawLine((int) getX(col), -1, (int) getX(col), getSize().height);

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
        if (selectedBlock.isSelected()) {
            Graphics2D g = (Graphics2D) g0;
            double xMin = Math.min(getX(selectedBlock.getFirstCol()), getSize().width);
            double xMax = Math.min((getX(selectedBlock.getLastCol() + 1)), getSize().width - 2);
            double yMin = Math.min(getY(selectedBlock.getFirstRow() - 1), getSize().height - 3);
            double yMax = Math.min(getY(selectedBlock.getLastRow()), getSize().height - 3);
            Rectangle2D rect = new Rectangle2D.Double(xMin, yMin, 0, 0);
            rect.add(xMax, yMax);
            g.setColor(highlightColor);
            g.draw(rect);
            g.setColor(highlightColorSemiTransparent);
            g.fill(rect);
            selectionRectangle.setRectangle(this, rect);
        }
        selectionRectangle.setAnimate(selectedBlock.isSelected());
    }

    /**
     * Adapts the grid parameters to the current sequenceFont. It is invoked every time the sequenceFont changes.
     */
    void revalidateGrid() {
        setSize((int) (cellWidth * alignment.getGapColumnContractor().getLayoutLength() + 0.5) + 3, (int) (cellHeight * alignment.getRowCompressor().getNumberRows() + 0.5) + 3);
        setPreferredSize(getSize());
        JScrollPane scrollPane = getScrollPane();
        revalidate();
        scrollPane.getHorizontalScrollBar().setMaximum((int) (Math.round(getPreferredSize().getWidth())));
        scrollPane.getVerticalScrollBar().setMaximum((int) (Math.round(getPreferredSize().getHeight())));
        selectedBlock.fireSelectionChanged();
    }

    public void setScale(double hScale, double vScale) {
        super.setScale(hScale, vScale);
        if (selectedBlock.isSelected()) {
            double xMin = Math.min(getX(selectedBlock.getFirstCol()), getSize().width);
            double xMax = Math.min((getX(selectedBlock.getLastCol())), getSize().width);
            double yMin = Math.min(getY(selectedBlock.getFirstRow() - 1), getSize().height - 3);
            double yMax = Math.min(getY(selectedBlock.getLastRow()), getSize().height - 3);
            Rectangle2D rect = new Rectangle2D.Double(xMin, yMin, 0, 0);
            rect.add(xMax, yMax);
            selectionRectangle.setRectangle(this, rect);
            selectionRectangle.setAnimate(selectedBlock.isSelected());
        }
    }

    private IColorScheme getColorScheme() {
        return colorScheme;
    }

    public void setColorScheme(IColorScheme colorScheme) {
        this.colorScheme = colorScheme;
    }

    public boolean isShowColors() {
        return showColors;
    }

    public void setShowColors(boolean showColors) {
        this.showColors = showColors;
    }

    public boolean isShowUnalignedChars() {
        return showUnalignedChars;
    }

    public void setShowUnalignedChars(boolean showUnalignedChars) {
        this.showUnalignedChars = showUnalignedChars;
    }

    /**
     * gets the zoom factor required for the alignment to fit horizontally into the window
     *
     * @return horizontal zoom to fit factor
     */
    public float getHZoomToFitFactor() {
        Rectangle rec = getScrollPane().getViewport().getViewRect();
        Rectangle bounds = getBounds();
        return (float) (rec.getWidth() / bounds.getWidth());
    }

    /**
     * gets the zoom factor required for the selected block to fit horizontally into the window
     *
     * @return horizontal zoom to fit factor
     */
    public float getHZoomToSelectionFactor() {
        if (selectedBlock.isSelected()) {
            Rectangle viewRect = getScrollPane().getViewport().getViewRect();
            return (float) (viewRect.getWidth() / (cellWidth * (selectedBlock.getLastCol() - selectedBlock.getFirstCol() + 1)));
        } else
            return 1;
    }

    /**
     * gets the zoom factor required for the alignment to fit vertically into the window
     *
     * @return vertical zoom to fit factor
     */
    public float getVZoomToFitFactor() {
        Rectangle rec = getScrollPane().getViewport().getViewRect();
        Rectangle bounds = getBounds();
        return (float) (rec.getHeight() / bounds.getHeight());
    }

    /**
     * gets the zoom factor required for the selected block to fit vertically into the window
     *
     * @return vertical zoom to fit factor
     */
    public float getVZoomToSelectionFactor() {
        if (selectedBlock.isSelected()) {
            Rectangle viewRect = getScrollPane().getViewport().getViewRect();
            return (float) (viewRect.getHeight() / (cellHeight * (selectedBlock.getLastRow() - selectedBlock.getFirstRow() + 1)));
        } else
            return 1;
    }

    public JScrollPane getScrollPane() {
        return (JScrollPane) AlignmentPanel.this.getParent().getParent();
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

            int row = getRow(me.getPoint());
            int col = getCol(me.getPoint());

            if (me.getClickCount() == 1) {
                if (me.isShiftDown()) {
                    if (selectedBlock.isSelected()) {
                        if (!selectedBlock.isSelected(row, col))
                            selectedBlock.extendSelection(row, col);
                        else
                            selectedBlock.reduceSelection(row, col);
                    }
                } else if (me.getButton() == 1 && !me.isControlDown() && !me.isAltDown()) {
                    int read = alignment.getHitRead(row, col);
                    if (read != -1) {
                        Lane lane = alignment.getLane(read);
                        int firstJump = alignment.getGapColumnContractor().getTotalJumpBeforeLayoutColumn(col);
                        int firstCol = lane.getFirstNonGapPosition() - firstJump;
                        int lastCol = lane.getLastNonGapPosition() - firstJump - 1;
                        selectedBlock.select(row, firstCol, row, lastCol, alignment.isTranslate());
                        ProjectManager.getPreviouslySelectedNodeLabels().clear();
                        ProjectManager.getPreviouslySelectedNodeLabels().add(Basic.getFirstWord(lane.getName()));
                    } else if ((me.isAltDown() && selectedBlock.isSelected()) || !selectedBlock.contains(row, col)) {
                        selectedBlock.clear();
                    }
                }
            } else if (me.getClickCount() == 2) {
                if (!me.isAltDown())
                    selectedBlock.selectRow(row);
                else {
                    selectedBlock.selectCol(col, alignment.isTranslate());
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent me) {
            super.mousePressed(me);
            mouseDown = me.getPoint();
            boolean shiftDown = me.isShiftDown();
            requestFocusInWindow();

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
            toolTipHelper.mouseMoved(me.getPoint());
        }

        @Override
        public void mouseDragged(MouseEvent me) {
            super.mouseDragged(me);
            toolTipHelper.mouseMoved(me.getPoint());
            stillDownWithoutMoving = false;
            if (current == inRubberband) {
                AlignmentPanel viewer = AlignmentPanel.this;
                Graphics2D gc = (Graphics2D) getGraphics();
                if (gc != null) {
                    Color color = viewer.getBackground() != null ? viewer.getBackground() : Color.WHITE;
                    gc.setXORMode(color);
                    int firstRow = Math.min(getRow(mouseDown), getRow(me.getPoint()));
                    int firstCol = Math.min(getCol(mouseDown), getCol(me.getPoint()));
                    int lastRow = Math.max(getRow(mouseDown), getRow(me.getPoint()));
                    int lastCol = Math.max(getCol(mouseDown), getCol(me.getPoint()));
                    getSelectedBlock().select(firstRow, firstCol, lastRow, lastCol, alignment.isTranslate());
                }
            } else if (current == inScrollByMouse) {
                if (AlignmentPanel.this.getParent() != null && AlignmentPanel.this.getParent().getParent() != null
                        && AlignmentPanel.this.getParent().getParent() instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) AlignmentPanel.this.getParent().getParent();
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
                    int firstCol = Math.min(getCol(mouseDown), getCol(me.getPoint()));
                    int lastRow = Math.max(getRow(mouseDown), getRow(me.getPoint()));
                    int lastCol = Math.max(getCol(mouseDown), getCol(me.getPoint()));
                    getSelectedBlock().select(firstRow, firstCol, lastRow, lastCol, alignment.isTranslate());
                }
            }
            setCursor(Cursor.getDefaultCursor());
            current = 0;
        }
    }

    public boolean isColorMatchesVsReference() {
        return colorMatchesVsReference;
    }

    public void setColorMatchesVsReference(boolean colorMatchesVsReference) {
        this.colorMatchesVsReference = colorMatchesVsReference;
        if (colorMatchesVsReference) {
            colorMatchesVsConsensus = false;
            colorMismatchesVsConsensus = false;
        }
    }

    public boolean isColorMismatchesVsReference() {
        return colorMismatchesVsReference;
    }

    public void setColorMismatchesVsReference(boolean colorMismatchesVsReference) {
        this.colorMismatchesVsReference = colorMismatchesVsReference;
        if (colorMismatchesVsReference) {
            colorMatchesVsConsensus = false;
            colorMismatchesVsConsensus = false;
        }
    }

    public boolean isColorMatchesVsConsensus() {
        return colorMatchesVsConsensus;
    }

    public void setColorMatchesVsConsensus(boolean colorMatchesVsConsensus) {
        this.colorMatchesVsConsensus = colorMatchesVsConsensus;
        if (colorMatchesVsConsensus) {
            colorMatchesVsReference = false;
            colorMismatchesVsReference = false;
        }
    }

    public boolean isColorMismatchesVsConsensus() {
        return colorMismatchesVsConsensus;
    }

    public void setColorMismatchesVsConsensus(boolean colorMismatchesVsConsensus) {
        this.colorMismatchesVsConsensus = colorMismatchesVsConsensus;
        if (colorMismatchesVsConsensus) {
            colorMatchesVsReference = false;
            colorMismatchesVsReference = false;
        }
    }
}
