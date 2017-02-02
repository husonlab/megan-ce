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

import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import megan.core.Document;
import megan.fx.Utilities;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * some graphics utilities
 * Created by huson on 11/27/15.
 */
public class GraphicsUtilities {
    /**
     * creates an icon to use with this sample
     *
     * @param doc
     * @param sample
     * @return icon
     */
    public static Node makeSampleIconFX(Document doc, String sample, boolean setColor, boolean setShape, int size) {
        final javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(size, size);

        Color color = null;
        if (setColor) {
            color = doc.getSampleAttributeTable().getSampleColor(sample);
            if (color == null)
                color = doc.getChartColorManager().getSampleColor(sample);
        }

        final GraphicsContext gc = canvas.getGraphicsContext2D();
        if (color != null)
            gc.setFill(Utilities.getColorFX(color));
        gc.setStroke(javafx.scene.paint.Color.BLACK);

        String shapeName = doc.getSampleAttributeTable().getSampleShape(sample);
        if (shapeName == null || !setShape)
            shapeName = "square";

        switch (shapeName.toLowerCase()) {
            case "triangle":
                if (color != null)
                    gc.fillPolygon(new double[]{1, size - 1, size / 2}, new double[]{size - 1, size - 1, 1}, 3);
                gc.strokePolygon(new double[]{1, size - 1, size / 2}, new double[]{size - 1, size - 1, 1}, 3);
                break;
            case "diamond":
                if (color != null)
                    gc.fillPolygon(new double[]{1, size / 2, size - 1, size / 2}, new double[]{size / 2, size - 1, size / 2, 1}, 4);
                gc.strokePolygon(new double[]{1, size / 2, size - 1, size / 2}, new double[]{size / 2, size - 1, size / 2, 1}, 4);
                break;
            case "circle":
                if (color != null)
                    gc.fillOval(1, 1, size - 2, size - 2);
                gc.strokeOval(1, 1, size - 2, size - 2);
                break;
            default:
            case "square":
                if (color != null)
                    gc.fillRect(1, 1, size - 2, size - 2);
                gc.strokeRect(1, 1, size - 2, size - 2);
        }
        return canvas;
    }

    /**
     * creates an icon to use with this sample
     *
     * @param doc
     * @param sample
     * @param setColor
     * @param setShape
     * @param size
     * @return
     */
    public static Image makeSampleIconSwing(Document doc, String sample, boolean setColor, boolean setShape, int size) {
        final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D gc = image.createGraphics();

        if (setColor && doc.getChartColorManager().getSampleColor(sample) == null)
            setColor = false;

        if (setColor)
            gc.setColor(doc.getChartColorManager().getSampleColor(sample));

        String shapeName = doc.getSampleAttributeTable().getSampleShape(sample);
        if (shapeName == null || !setShape)
            shapeName = "square";

        switch (shapeName.toLowerCase()) {
            case "triangle":
                if (setColor)
                    gc.fillPolygon(new int[]{1, size - 1, size / 2}, new int[]{size - 1, size - 1, 1}, 3);
                gc.setColor(Color.BLACK);
                gc.drawPolygon(new int[]{1, size - 1, size / 2}, new int[]{size - 1, size - 1, 1}, 3);
                break;
            case "diamond":
                if (setColor)
                    gc.fillPolygon(new int[]{1, size / 2, size - 1, size / 2}, new int[]{size / 2, size - 1, size / 2, 1}, 4);
                gc.setColor(Color.BLACK);
                gc.drawPolygon(new int[]{1, size / 2, size - 1, size / 2}, new int[]{size / 2, size - 1, size / 2, 1}, 4);
                break;
            case "circle":
                if (setColor)
                    gc.fillOval(1, 1, size - 2, size - 2);
                gc.setColor(Color.BLACK);
                gc.drawOval(1, 1, size - 2, size - 2);
                break;
            default:
            case "square":
                if (setColor)
                    gc.fillRect(1, 1, size - 2, size - 2);
                gc.setColor(Color.BLACK);
                gc.drawRect(1, 1, size - 2, size - 2);
        }
        return image;
    }
}
