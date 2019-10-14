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
import jloda.util.ProgramProperties;

import java.awt.*;
import java.util.LinkedList;

/**
 * a drawable value
 * Daniel Huson, 2.2013
 */
public class DrawableValue {
    private final String label;
    private final int x;
    private final int y;
    private final boolean selected;

    /**
     * constructor
     *
     * @param label
     * @param x
     * @param y
     * @param selected
     */
    public DrawableValue(String label, int x, int y, boolean selected) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.selected = selected;
    }

    /**
     * get label
     *
     * @return label
     */
    public String getLabel() {
        return label;
    }

    /**
     * get x coordinate
     *
     * @return x coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * get y coordinate
     *
     * @return y coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * is selected
     *
     * @return selected
     */
    private boolean isSelected() {
        return selected;
    }

    /**
     * draw the label
     *
     * @param gc
     */
    private void draw(Graphics2D gc, boolean centerLabelWidth, boolean centerLabelHeight) {
        if (centerLabelWidth || centerLabelHeight) {
            Dimension labelSize = BasicSwing.getStringSize(gc, label, gc.getFont()).getSize();
            gc.drawString(label, (int) (x - (centerLabelWidth ? labelSize.getWidth() / 2 : 0)), (int) (y + (centerLabelHeight ? labelSize.getHeight() / 2 : 0)));
        } else
            gc.drawString(label, x, y);
    }

    /**
     * draw all values
     *
     * @param gc
     * @param valuesList
     * @param centerLabelWidth
     * @param centerLabelHeight
     */
    public static void drawValues(Graphics2D gc, LinkedList<DrawableValue> valuesList, boolean centerLabelWidth, boolean centerLabelHeight) {
        gc.setColor(Color.LIGHT_GRAY);
        for (DrawableValue value : valuesList) {
            if (!value.isSelected())
                value.draw(gc, centerLabelWidth, centerLabelHeight);
        }
        gc.setColor(ProgramProperties.SELECTION_COLOR_ADDITIONAL_TEXT);
        for (DrawableValue value : valuesList) {
            if (value.isSelected())
                value.draw(gc, centerLabelWidth, centerLabelHeight);
        }
    }
}
