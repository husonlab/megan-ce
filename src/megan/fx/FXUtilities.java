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

package megan.fx;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import jloda.util.Single;

import java.util.Set;

/**
 * some helpful JavaFX stuff
 * Created by huson on 2/18/17.
 */
public class FXUtilities {
    /**
     * bidirectionally bind scroll bars of two nodes
     *
     * @param node1
     * @param node2
     * @param orientation
     */
    public static void bidirectionallyBindScrollBars(final Node node1, final Node node2, Orientation orientation) {
        final ScrollBar scrollBar1 = findScrollBar(node1, orientation);
        final ScrollBar scrollBar2 = findScrollBar(node2, orientation);

        if (scrollBar1 != null && scrollBar2 != null) {
            final Single<Boolean> inChange = new Single<>(false);
            scrollBar1.valueProperty().addListener(observable -> {
                if (!inChange.get()) {
                    try {
                        inChange.set(true);
                        scrollBar2.setValue(scrollBar1.getValue() * (scrollBar2.getMax() - scrollBar2.getMin()) / (scrollBar1.getMax() - scrollBar1.getMin()));
                    } finally {
                        inChange.set(false);
                    }
                }
            });
            scrollBar2.valueProperty().addListener(observable -> {
                if (!inChange.get()) {
                    try {
                        inChange.set(true);
                        scrollBar1.setValue(scrollBar2.getValue() * (scrollBar1.getMax() - scrollBar1.getMin()) / (scrollBar2.getMax() - scrollBar2.getMin()));
                    } finally {
                        inChange.set(false);
                    }
                }
            });
        }
    }

    /**
     * Find the scrollbar of the given table.
     *
     * @param node
     * @return
     */
    public static ScrollBar findScrollBar(Node node, Orientation orientation) {
        Set<Node> below = node.lookupAll(".scroll-bar");
        for (final Node nodeBelow : below) {
            if (nodeBelow instanceof ScrollBar) {
                ScrollBar sb = (ScrollBar) nodeBelow;
                if (sb.getOrientation() == orientation) {
                    return sb;
                }
            }
        }
        return null;
    }

    /**
     * changes opacity, if paint is a color
     *
     * @param paint
     * @param opacity
     * @return paint
     */
    public static Paint changeOpacity(Paint paint, double opacity) {
        if (paint instanceof Color) {
            Color color = (Color) paint;
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
        } else
            return paint;
    }
}
