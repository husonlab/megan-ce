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

import javax.swing.*;
import java.awt.*;


/**
 * keep scrollbar centered on same point when zooming
 * Daniel Huson, 10.2011
 */
public class ScrollPaneAdjuster {
    private final JScrollBar scrollBarX;
    private final JScrollBar scrollBarY;

    private final double xFactor;
    private final double yFactor;

    private final double xPortionOfVisible;
    private final double yPortionOfVisible;

    /**
     * construct object and "remember" how scrollpane is currently centered around middle of screen
     *
     * @param scrollPane
     */
    public ScrollPaneAdjuster(JScrollPane scrollPane) {
        this(scrollPane, null);
    }

    /**
     * construct object and "remember" how scrollpane is currently centered
     *
     * @param scrollPane
     * @param centerDC   center point in device coordinates
     */
    public ScrollPaneAdjuster(JScrollPane scrollPane, Point centerDC) {
        Rectangle viewRect = scrollPane.getViewport().getViewRect();
        scrollBarX = scrollPane.getHorizontalScrollBar();
        scrollBarY = scrollPane.getVerticalScrollBar();

        if (centerDC == null) {
            xPortionOfVisible = 0.5;
            yPortionOfVisible = 0.5;
        } else {
            xPortionOfVisible = (centerDC.x - viewRect.x) / (double) viewRect.width;
            yPortionOfVisible = (centerDC.y - viewRect.y) / (double) viewRect.height;
        }

        xFactor = (scrollBarX.getValue() + xPortionOfVisible * scrollBarX.getVisibleAmount()) / (scrollBarX.getMaximum() - scrollBarX.getMinimum());
        yFactor = (scrollBarY.getValue() + yPortionOfVisible * scrollBarY.getVisibleAmount()) / (scrollBarY.getMaximum() - scrollBarY.getMinimum());
    }

    /**
     * adjusts the scroll bars to recenter on world coordinates that were previously in
     * center of window
     *
     * @param horizontal adjust horizontally
     * @param vertical   adjust vertically
     */
    public void adjust(boolean horizontal, boolean vertical) {
        if (horizontal) {
            int newXValue = (int) Math.round(xFactor * (scrollBarX.getMaximum() - scrollBarX.getMinimum()) - xPortionOfVisible * scrollBarX.getVisibleAmount());
            scrollBarX.setValue(newXValue);
        }
        if (vertical) {
            int newYValue = (int) Math.round(yFactor * (scrollBarY.getMaximum() - scrollBarY.getMinimum()) - yPortionOfVisible * scrollBarY.getVisibleAmount());
            scrollBarY.setValue(newYValue);
        }
    }
}
