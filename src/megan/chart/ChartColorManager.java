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
package megan.chart;

import jloda.swing.util.ColorTable;
import jloda.swing.util.ColorTableManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;

import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * default chart colors, based on hash code of label
 * Daniel Huson, 5.2012
 */
public class ChartColorManager {
    public static final String SAMPLE_ID = "#SampleID";

    public static ChartColorManager programChartColorManager;

    private final Map<String, Color> class2color = new HashMap<>(); // cache changes
    private final Map<String, Color> attributeState2color = new HashMap<>(); // cache changes

    private ColorTable colorTable;
    private ColorTable colorTableHeatMap;
    private boolean colorByPosition;

    private ColorGetter seriesOverrideColorGetter = null;

    private final Map<String, Integer> class2position = new HashMap<>(); // maps classes to their position in the order of things
    private final Map<String, Integer> attributeState2position = new HashMap<>(); // maps attributes to their position in the order of things

    /**
     * constructor
     *
     * @param colorTable
     */
    public ChartColorManager(ColorTable colorTable) {
        this.colorTable = colorTable;
        setColorByPosition(ProgramProperties.get("ColorByPosition", false));
    }

    /**
     * initial the program-wide color table
     */
    public static void initialize() {
        if (programChartColorManager == null) {
            programChartColorManager = new ChartColorManager(ColorTableManager.getDefaultColorTable());
            programChartColorManager.setHeatMapTable(ColorTableManager.getDefaultColorTableHeatMap().getName());
            programChartColorManager.loadColorEdits(ProgramProperties.get("ColorMap", ""));
        }
    }

    /**
     * save document edits, if they exist
     */
    public static void store() {
        if (programChartColorManager != null) {
            ProgramProperties.put("ColorMap", programChartColorManager.getColorEdits());
        }
    }

    /**
     * get color for data set
     *
     * @param sample
     * @return color
     */
    public Color getSampleColor(String sample) {
        Color color = null;
        if (seriesOverrideColorGetter != null)
            color = seriesOverrideColorGetter.get(sample);

        if (color == null)
            color = getAttributeStateColor(SAMPLE_ID, sample);
        if (color == null) {
            if (sample == null || sample.equals("GRAY"))
                color = Color.GRAY;
            else {
                int key = sample.hashCode();
                color = colorTable.get(key);
            }
        }
        return color;
    }

    /**
     * get color for data set
     *
     * @param sample
     * @return color
     */
    public Color getSampleColorWithAlpha(String sample, int alpha) {
        Color color = getSampleColor(sample);
        if (color == null)
            return null;
        if (color.getAlpha() == alpha)
            return color;
        else
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }


    /**
     * set the color of a series
     *
     * @param sample
     * @param color
     */
    public void setSampleColor(String sample, Color color) {
        setAttributeStateColor(SAMPLE_ID, sample, color);
    }

    /**
     * get the color fo a specific chart and class
     *
     * @param className
     * @return color
     */
    public Color getClassColor(String className) {
        if (className == null)
            return null;
        if (className.equals("GRAY"))
            return Color.GRAY;

        if (class2color.containsKey(className))
            return class2color.get(className);

        if (isColorByPosition()) {
            Integer position = class2position.get(className);
            if (position == null) {
                position = class2position.size();
                class2position.put(className, position);
            }
            return colorTable.get(position);
        } else {
            return colorTable.get(className.hashCode());
        }
    }

    /**
     * get the color fo a specific chart and class
     *
     * @param className
     * @return color
     */
    public Color getClassColor(String className, int alpha) {
        Color color = getClassColor(className);
        if (color == null)
            return null;
        if (color.getAlpha() == alpha)
            return color;
        else
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /**
     * set the color for a class
     *
     * @param className
     * @param color
     */
    public void setClassColor(String className, Color color) {
        class2color.put(className, color);
    }


    public void setClassColorPositions(Collection<String> classNames) {
        class2position.clear();
        int pos = 0;
        for (String name : classNames) {
            if (!class2position.containsKey(name))
                class2position.put(name, pos++);
        }
    }

    public void setAttributeStateColorPositions(String attributeName, Collection<String> states) {
        attributeState2position.clear();
        int pos = 0;
        for (String state : states) {
            String attributeState = attributeName + "::" + state;
            if (!attributeState2position.containsKey(attributeState))
                attributeState2position.put(attributeState, pos++);
        }
    }

    /**
     * get the color fo a specific chart and attribute
     *
     * @param attributeName
     * @return color
     */
    public Color getAttributeStateColor(String attributeName, Object state) {
        if (state == null || state.toString().equals("NA"))
            return Color.GRAY;
        final String attributeState = attributeName + "::" + state;

        if (attributeState2color.containsKey(attributeState))
            return attributeState2color.get(attributeState);

        if (isColorByPosition()) {
            Integer position = attributeState2position.get(attributeState);
            if (position == null) {
                position = attributeState2position.size();
                attributeState2position.put(attributeState, position);
            }
            if (attributeState2position.size() >= colorTable.size()) {
                return colorTable.get(position);
            } else { // scale to color table
                int index = (position * colorTable.size()) / attributeState2position.size();
                //System.err.println("Position: "+position+" -> "+index);
                return colorTable.get(index);
            }
        } else {
            return colorTable.get(attributeState.hashCode());
        }
    }


    /**
     * set the color for an attribute
     *
     * @param attributeName
     * @param color
     */
    public void setAttributeStateColor(String attributeName, String state, Color color) {
        attributeState2color.put(attributeName + "::" + state, color);
    }

    /**
     * are the any changed colors?
     *
     * @return true, if has changed colors
     */
    public boolean hasChangedColors() {
        return attributeState2color.size() > 0 || class2color.size() > 0;
    }

    /**
     * clear all changed colors
     */
    public void clearChangedColors() {
        attributeState2color.clear();
        class2color.clear();
    }

    /**
     * load color edits
     *
     * @param colorEdits
     */
    public void loadColorEdits(String colorEdits) {
        if (colorEdits != null && colorEdits.length() > 0) {
            read(colorEdits.split(";"));
        }
    }

    public String getColorEdits() {
        try (StringWriter w = new StringWriter()) {
            write(w, ";");
            return w.toString();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * write color table
     *
     * @param w
     * @throws java.io.IOException
     */
    public void write(Writer w) throws IOException {
        write(w, "\n");
    }

    /**
     * write color table
     *
     * @param w
     * @throws java.io.IOException
     */
    private void write(Writer w, String separator) throws IOException {
        for (Map.Entry<String, Color> entry : class2color.entrySet()) {
            Color color = entry.getValue();
            if (color != null)
                w.write("C" + "\t" + entry.getKey() + "\t" + color.getRGB() + (color.getAlpha() < 255 ? "\t" + color.getAlpha() : "") + separator);
        }
        for (Map.Entry<String, Color> entry : attributeState2color.entrySet()) {
            Color color = entry.getValue();
            if (color != null)
                w.write("A" + "\t" + entry.getKey() + "\t" + color.getRGB() + (color.getAlpha() < 255 ? "\t" + color.getAlpha() : "") + separator);
        }
    }

    /**
     * read color table
     *
     * @param r0
     * @throws IOException
     */
    public void read(Reader r0) throws IOException {
        String[] tokens = Basic.getLines(r0);
        read(tokens);
    }

    /**
     * read color table
     *
     * @param lines
     * @throws IOException
     */
    private void read(String[] lines) {
        for (String aLine : lines) {
            aLine = aLine.trim();
            if (aLine.length() > 0 && !aLine.startsWith("#")) {
                String[] tokens = aLine.split("\t");
                if (tokens.length >= 3 && Basic.isInteger(tokens[2])) {
                    switch (tokens[0]) {
                        case "C": {
                            String className = tokens[1];
                            Color color = new Color(Integer.parseInt(tokens[2]));
                            if (tokens.length >= 4) {
                                int alpha = Integer.parseInt(tokens[3]);
                                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                            }
                            class2color.put(className, color);
                            break;
                        }
                        case "A": {
                            String attribute = tokens[1];
                            Color color = new Color(Integer.parseInt(tokens[2]));
                            if (tokens.length >= 4) {
                                int alpha = Integer.parseInt(tokens[3]);
                                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                            }
                            attributeState2color.put(attribute, color);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * gets a series color getter that overrides MEGAN-wide colors
     *
     * @return color getter
     */
    public ColorGetter getSeriesOverrideColorGetter() {
        return seriesOverrideColorGetter;
    }

    /**
     * sets a series color getter that overrides MEGAN-wide colors. Is used to implement document-specific colors
     *
     * @param seriesOverrideColorGetter
     */
    public void setSeriesOverrideColorGetter(ColorGetter seriesOverrideColorGetter) {
        this.seriesOverrideColorGetter = seriesOverrideColorGetter;
    }

    public ColorGetter getSeriesColorGetter() {
        return label -> getSampleColor(label);
    }

    public ColorGetter getClassColorGetter() {
        return label -> getClassColor(label);
    }

    public ColorGetter getAttributeColorGetter() {
        return label -> Color.WHITE;
    }

    public String getColorTableName() {
        return colorTable.getName();
    }

    public interface ColorGetter {
        Color get(String label);
    }

    public void setColorTable(String name) {
        this.colorTable = ColorTableManager.getColorTable(name);
    }

    public void setColorTable(String name, boolean colorByPosition) {
        this.colorTable = ColorTableManager.getColorTable(name);
        setColorByPosition(colorByPosition);
    }

    public ColorTable getColorTable() {
        return colorTable;
    }

    public boolean isColorByPosition() {
        return colorByPosition;
    }

    public void setColorByPosition(boolean colorByPosition) {
        this.colorByPosition = colorByPosition;
        ProgramProperties.put("ColorByPosition", colorByPosition);
    }

    public void setHeatMapTable(String name) {
        this.colorTableHeatMap = ColorTableManager.getColorTableHeatMap(name);
    }

    public ColorTable getHeatMapTable() {
        return colorTableHeatMap;
    }

    public boolean isUsingProgramColors() {
        return this == programChartColorManager;
    }
}
