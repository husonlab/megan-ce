/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.core;

import jloda.graphview.NodeView;
import jloda.util.parse.NexusStreamParser;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * manage shapes and labels to be set by sampleViewer and shown by clusterViewer
 * Daniel Huson, 4.2013
 * todo: need to intergrate this
 */
public class ShapeAndLabelManager {
    private final Map<String, String> sample2label = new HashMap<>();
    private final Map<String, Byte> sample2shape = new HashMap<>();

    /**
     * get the set label
     *
     * @param sample
     * @return label
     */
    public String getLabel(String sample) {
        String label = sample2label.get(sample);
        if (label == null)
            return sample;
        else
            return label;
    }

    /**
     * set label to use for sample
     *
     * @param sample
     * @param label
     */
    public void setLabel(String sample, String label) {
        sample2label.put(sample, label);
    }

    /**
     * erase labels
     */
    public void clearLabels() {
        sample2label.clear();
    }

    /**
     * are any labels defined?
     *
     * @return true, if defined
     */
    public boolean hasLabels() {
        return sample2label.size() > 0;
    }

    /**
     * get shape for sample
     *
     * @param sample
     * @return shape
     */
    public byte getShape(String sample) {
        Byte shape = sample2shape.get(sample);
        if (shape == null)
            return NodeView.OVAL_NODE;
        else
            return shape;
    }

    /**
     * set shape to use with sample
     *
     * @param sample
     * @param shape
     */
    public void setShape(String sample, byte shape) {
        sample2shape.put(sample, shape);
    }

    /**
     * erase shapes
     */
    public void clearShapes() {
        sample2shape.clear();
    }

    /**
     * are any shapes defined?
     *
     * @return true, if shapes defined
     */
    public boolean hasShapes() {
        return sample2shape.size() > 0;
    }

    /**
     * write label map as a line
     *
     * @return line
     */
    public String getLabelMapAsLine() {
        StringBuilder buf = new StringBuilder();
        for (String sample : sample2label.keySet()) {
            buf.append(" '").append(sample).append("': '").append(sample2label.get(sample)).append("'");
        }
        buf.append(";");
        return buf.toString();
    }

    /**
     * write shape map as a line
     *
     * @return line
     */
    public String getShapeMapAsLine() {
        StringBuilder buf = new StringBuilder();
        for (String sample : sample2shape.keySet()) {
            buf.append(" '").append(sample).append("': '").append((int) sample2shape.get(sample)).append("'");
        }
        buf.append(";");
        return buf.toString();
    }

    /**
     * parse label map from line
     *
     * @param labelMapAsLine
     */
    public void parseLabelMapFromLine(String labelMapAsLine) {
        NexusStreamParser np = new NexusStreamParser(new StringReader(labelMapAsLine));
        try {
            while (!np.peekMatchIgnoreCase(";")) {
                String sample = np.getWordRespectCase();
                np.matchIgnoreCase(":");
                String label = np.getWordRespectCase();
                sample2label.put(sample, label);
            }
        } catch (IOException e) {
        }
    }

    /**
     * parse label map from line
     *
     * @param shapeMapAsLine
     */
    public void parseShapeMapFromLine(String shapeMapAsLine) {
        NexusStreamParser np = new NexusStreamParser(new StringReader(shapeMapAsLine));
        try {
            while (!np.peekMatchIgnoreCase(";")) {
                String sample = np.getWordRespectCase();
                np.matchIgnoreCase(":");
                int shape = np.getInt();
                sample2shape.put(sample, (byte) shape);
            }
        } catch (IOException e) {
        }
    }

}
