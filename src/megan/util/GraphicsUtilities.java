/*
 *  Copyright (C) 2017 Daniel H. Huson
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
package megan.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import jloda.graphview.NodeShape;
import jloda.graphview.NodeShapeIcon;
import megan.core.Document;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * some graphics utilities
 * Created by huson on 11/27/15.
 */
public class GraphicsUtilities {
    /**
     * creates an icon to use with this sample for JavaFX
     *
     * @param doc
     * @param sample
     * @return icon
     */
    public static Node makeSampleIconFX(Document doc, String sample, boolean setColor, boolean setShape, int size) {
        return new ImageView(SwingFXUtils.toFXImage(makeSampleIconSwing(doc, sample, setColor, setShape, size), null));
    }

    /**
     * creates an icon to use with this sample for Swing
     *
     * @param doc
     * @param sample
     * @param setColor
     * @param setShape
     * @param size
     * @return
     */
    public static java.awt.image.BufferedImage makeSampleIconSwing(Document doc, String sample, boolean setColor, boolean setShape, int size) {
        Color color = null;
        if (setColor) {
            color = doc.getSampleAttributeTable().getSampleColor(sample);
            if (color == null)
                color = doc.getChartColorManager().getSampleColor(sample);
        }

        NodeShape shape = NodeShape.valueOfIgnoreCase(doc.getSampleAttributeTable().getSampleShape(sample));
        if (!setShape || shape == null)
            shape = NodeShape.Rectangle;
        return (BufferedImage) new NodeShapeIcon(shape, size, color).getImage();
    }
}
