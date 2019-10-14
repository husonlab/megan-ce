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
import java.awt.geom.Point2D;

/**
 * base of scalable panels used in alignmentviewer
 * Daniel Huson, 9.2011
 */
abstract public class BasePanel extends JPanel implements Scrollable {
    public static int MIN_CELL_HEIGHT = 6;

    double cellWidth;
    double cellHeight;
    Font sequenceFont = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    final static Color highlightColor = ProgramProperties.SELECTION_COLOR_ADDITIONAL_TEXT.darker();
    final static Color highlightColorSemiTransparent = new Color(ProgramProperties.SELECTION_COLOR_ADDITIONAL_TEXT.getRed(),
            ProgramProperties.SELECTION_COLOR_ADDITIONAL_TEXT.getGreen(), ProgramProperties.SELECTION_COLOR_ADDITIONAL_TEXT.getBlue(), 60);

    final SelectedBlock selectedBlock;

    public BasePanel(SelectedBlock selectedBlock) {
        this.selectedBlock = selectedBlock;
        selectedBlock.addSelectionListener((selected, minRow, minCol, maxRow, maxCol) -> repaint());
    }

    public double getX(int col) {
        return cellWidth * col;
    }

    public double getY(int row) {
        return cellHeight * (row + 1);
    }

    Font getSequenceFont() {
        return sequenceFont;
    }

    public void setSequenceFont(Font sequenceFont) {
        this.sequenceFont = sequenceFont;
        revalidateGrid();
    }

    /**
     * this should be overridden
     */
    abstract void revalidateGrid();


    int getRow(Point2D aPt) {
        return (int) (aPt.getY() / cellHeight);

    }

    int getCol(Point2D aPt) {
        return (int) (aPt.getX() / cellWidth);

    }

    SelectedBlock getSelectedBlock() {
        return selectedBlock;
    }

    public double getCellHeight() {
        return cellHeight;
    }

    public double getCellWidth() {
        return cellWidth;
    }

    /**
     * sets the scale
     *
     * @param hScale
     * @param vScale
     */
    public void setScale(double hScale, double vScale) {
        double fontSize;
        if (hScale > 0 && vScale == 0)
            fontSize = hScale;
        else if (hScale == 0 && vScale > 0)
            fontSize = vScale;
        else if (hScale > 0 && vScale > 0)
            fontSize = Math.min(hScale, vScale);
        else
            return;

        sequenceFont = makeFont((int) Math.round(fontSize));
        cellWidth = hScale > 0 ? hScale : vScale;

        cellHeight = vScale > 0 ? vScale : hScale;
        revalidateGrid();
    }

    /**
     * makes a font of the correct apparent size
     *
     * @param fontSize
     * @return
     */
    private Font makeFont(int fontSize) {
        FontMetrics seqMetrics = getFontMetrics(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
        int fontHeight = seqMetrics.getAscent();
        return new Font(Font.MONOSPACED, Font.PLAIN, (int) (((float) fontSize * fontSize) / fontHeight));
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle rectangle, int direction, int value) {
        if (direction == SwingConstants.HORIZONTAL)
            return (int) (Math.round(cellWidth));
        else
            return (int) (Math.round(cellHeight));
    }

    public int getScrollableBlockIncrement(Rectangle rectangle, int direction, int value) {
        if (direction == SwingConstants.HORIZONTAL)
            return rectangle.width;
        else
            return rectangle.height;
    }

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
