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
package megan.core;

import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.Table;
import megan.viewer.MainViewer;

import java.awt.*;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.List;
import java.util.*;

/**
 * holds a table of metadata
 * Daniel Huson, 12.2012
 */
public class SampleAttributeTable {


    public enum Type {String, Integer, Float, Date}

    public static final String SAMPLE_ATTRIBUTES = "SAMPLE_ATTRIBUTES";
    public static final String USER_STATE = "USER_STATE";
    public static final String SAMPLE_ID = "#SampleID";
    public static final String DescriptionAttribute = "Description";

    public enum HiddenAttribute {
        Shape, Color, Label, Source, GroupId;

        /**
         * get the prefix used to identify hidden attributes
         *
         * @return prefix
         */
        static String getPrefix() {
            return "@";
        }

        /**
         * hidden attributes start with @
         *
         * @return
         */
        public String toString() {
            return getPrefix() + super.toString();
        }

        /**
         * get the value for the given label
         *
         * @param label
         * @return attribute
         */
        public HiddenAttribute getEnum(String label) {
            if (label.startsWith(getPrefix()))
                return valueOf(label.substring(1));
            else
                return valueOf(label);
        }
    }

    private final Table<String, String, Object> table = new Table<>();
    private final ArrayList<String> sampleOrder = new ArrayList<>();
    private final ArrayList<String> attributeOrder = new ArrayList<>();
    private final Map<String, Type> attribute2type = new HashMap<>();

    private String description = null;

    /**
     * erase the table
     */
    public void clear() {
        description = null;
        table.clear();
        sampleOrder.clear();
        attributeOrder.clear();
        attribute2type.clear();
    }

    /**
     * add a metadata table
     *
     * @param sampleAttributeTable
     * @param allowReplaceSample
     * @param allowAddAttribute
     * @return true, if anything was added
     */
    public boolean addTable(SampleAttributeTable sampleAttributeTable, boolean allowReplaceSample, boolean allowAddAttribute) {
        boolean changed = false;
        for (String sample : sampleAttributeTable.getSampleSet()) {
            if (allowReplaceSample || !table.rowKeySet().contains(sample)) {
                if (addSample(sample, sampleAttributeTable.getAttributesToValues(sample), true, allowAddAttribute))
                    changed = true;
            }
        }
        return changed;
    }

    /**
     * extract a metadata table containing the named samples
     *
     * @param samples
     * @return the new table
     */
    public SampleAttributeTable extractTable(Collection<String> samples) {
        SampleAttributeTable sampleAttributeTable = new SampleAttributeTable();

        for (String sample : getSampleOrder()) {
            if (samples.contains(sample)) {
                sampleAttributeTable.addSample(sample, getAttributesToValues(sample), true, true);
            }
        }
        sampleAttributeTable.attributeOrder.clear();
        sampleAttributeTable.attributeOrder.addAll(attributeOrder);
        for (String attribute : attribute2type.keySet()) {
            sampleAttributeTable.attribute2type.put(attribute, attribute2type.get(attribute));
        }
        sampleAttributeTable.removeUndefinedAttributes();
        return sampleAttributeTable;
    }

    /**
     * merges a set of samples and produces a new sample
     *
     * @param samples
     * @return true, if merged
     */
    public SampleAttributeTable mergeSamples(Collection<String> samples, String newName) {
        SampleAttributeTable sampleAttributeTable = new SampleAttributeTable();

        Map<String, Object> attribute2value = new HashMap<>();

        for (String attribute : getAttributeSet()) {
            boolean valueMismatch = false;
            Object previousValue = null;
            for (String sample : samples) {
                Object value = table.get(sample, attribute);
                if (value != null) {
                    if (previousValue == null)
                        previousValue = value;
                    else if (!value.equals(previousValue)) {
                        valueMismatch = true;
                        break;
                    }
                }
            }
            if (!valueMismatch && previousValue != null) {
                attribute2value.put(attribute, previousValue);
            }
        }
        sampleAttributeTable.addSample(newName, attribute2value, true, true);
        return sampleAttributeTable;
    }


    /**
     * add a sample to the table
     *
     * @param sample
     * @param attribute2value
     * @param allowReplaceSample
     * @param allowAddAttribute
     * @return true, if added
     */
    public boolean addSample(String sample, Map<String, Object> attribute2value, boolean allowReplaceSample, boolean allowAddAttribute) {
        if (!table.rowKeySet().contains(sample)) {
            sampleOrder.add(sample);
        }
        if (allowReplaceSample || !table.rowKeySet().contains(sample)) {
            for (String attribute : attribute2value.keySet()) {
                if (allowAddAttribute || getAttributeSet().contains(attribute))
                    put(sample, attribute, attribute2value.get(attribute));
            }
            //put(sample, SampleAttributeTable.SAMPLE_ID, sample);
            return true;
        }
        return false;
    }

    /**
     * set the sample order
     *
     * @param sampleNames
     */
    public void setSampleOrder(List<String> sampleNames) {
        sampleOrder.clear();
        sampleOrder.addAll(sampleNames);
        for (String sampleName : sampleNames) {
            if (!table.rowKeySet().contains(sampleName))
                table.put(sampleName, HiddenAttribute.Color.toString(), null);
        }
    }

    /**
     * remove a sample from the table
     *
     * @param name
     */

    public void removeSample(String name) {
        if (table.rowKeySet().contains(name)) {
            table.rowKeySet().remove(name);
            sampleOrder.remove(name);
        }
    }

    /**
     * rename a sample
     *
     * @param sample
     * @param newName
     * @param allowReplaceSample
     * @return true, if renamed
     */
    public boolean renameSample(String sample, String newName, boolean allowReplaceSample) {
        if (allowReplaceSample || !table.rowKeySet().contains(newName) && sampleOrder.contains(sample)) {
            sampleOrder.set(sampleOrder.indexOf(sample), newName);

            final Map<String, Object> row = table.row(sample);
            if (row != null) {
                table.rowKeySet().remove(sample);
                for (String key : row.keySet()) {
                    table.put(newName, key, row.get(key));
                }
            }
            return true;
        }
        return false;
    }

    /**
     * duplicate an existing sample
     *
     * @param sample
     * @param newName
     * @param allowReplaceSample
     * @return true, if duplicated
     */
    public boolean duplicateSample(String sample, String newName, boolean allowReplaceSample) {
        if (allowReplaceSample || !table.rowKeySet().contains(newName)) {
            Map<String, Object> row = table.row(sample);
            return addSample(newName, row, true, false);
        }
        return false;
    }

    /**
     * add an attribute with same value to all samples
     *
     * @param attribute
     * @param value
     * @param allowReplaceAttribute
     * @return true, if added
     */
    public boolean addAttribute(String attribute, Object value, boolean allowReplaceAttribute) {
        boolean change = false;
        if (allowReplaceAttribute || !table.columnKeySet().contains(attribute)) {
            for (String sample : getSampleOrder()) {
                put(sample, attribute, value);
            }
            change = true;
        }
        return change;
    }

    /**
     * add an attribute
     *
     * @param attribute
     * @param sample2value
     * @param allowReplaceAttribute
     * @param allowAddSample
     * @return true, if added
     */
    public boolean addAttribute(String attribute, Map<String, Object> sample2value, boolean allowReplaceAttribute, boolean allowAddSample) {
        if (allowReplaceAttribute || !table.columnKeySet().contains(attribute)) {
            if (sample2value.size() > 0) {
                for (String sample : sample2value.keySet()) {
                    if (allowAddSample || getSampleSet().contains(sample))
                        put(sample, attribute, sample2value.get(sample));
                }
            } else {
                for (String sample : getSampleSet()) {
                    put(sample, attribute, null);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * remove an attribute
     *
     * @param attribute
     */
    public void removeAttribute(String attribute) {
            attributeOrder.remove(attribute);
            attribute2type.keySet().remove(attribute);
            table.removeColumn(attribute);
    }

    /**
     * remove a collection of attributes
     *
     * @param attributes
     */
    private void removeAttributes(Collection<String> attributes) {
        attributeOrder.removeAll(attributes);
        attribute2type.keySet().removeAll(attributes);
        for (String attribute : attributes) {
            table.removeColumn(attribute);
        }
    }

    /**
     * duplicate an existing attribute
     *
     * @param attribute
     * @param newName
     * @param allowReplaceSample
     * @return true, if duplicated
     */
    public boolean duplicateAttribute(String attribute, String newName, boolean allowReplaceSample) {
        if (allowReplaceSample || !table.columnKeySet().contains(newName)) {
            Map<String, Object> samples2values = table.column(attribute);
            boolean result = addAttribute(newName, samples2values, true, false);
            if (result)
                attribute2type.put(newName, attribute2type.get(attribute));
            return result;
        }
        return false;
    }

    /**
     * expands an existing attribute
     *
     * @param attribute
     * @return number of columns added
     */
    public int expandAttribute(String attribute, boolean allowReplaceAttribute) {
        final Set<Object> values = new TreeSet<>(getSamples2Values(attribute).values());

        final ArrayList<String> newOrder = new ArrayList<>(getAttributeOrder().size() + values.size());
        newOrder.addAll(getAttributeOrder());
        int pos = newOrder.indexOf(attribute);

        int count = 0;
        for (Object value : values) {
            final String attributeName = attribute + ":" + value;
            if (!getAttributeOrder().contains(attributeName)) {
                Map<String, Object> samples2values = new HashMap<>();
                for (String sample : getSampleOrder()) {
                    samples2values.put(sample, get(sample, attribute).equals(value) ? 1 : 0);
                }
                boolean result = addAttribute(attributeName, samples2values, allowReplaceAttribute, false);
                if (result) {
                    attribute2type.put(attributeName, Type.Integer);
                    count++;
                    newOrder.add(pos + count, attributeName);
                }
            }
        }
        setAttributeOrder(newOrder);
        return count;
    }

    /**
     * removes all attributes for which no sample has a defined value
     *
     * @return true, if at least one attribute was removed
     */
    private boolean removeUndefinedAttributes() {
        LinkedList<String> undefined = new LinkedList<>();
        for (String attribute : getAttributeSet()) {
            Map<String, Object> sample2values = getSamples2Values(attribute);
            boolean ok = false;
            for (String sample : sample2values.keySet()) {
                if (sample2values.get(sample) != null) {
                    ok = true;
                    break;
                }
            }
            if (!ok)
                undefined.add(attribute);
        }
        if (undefined.size() > 0)
            removeAttributes(undefined);
        return undefined.size() > 0;
    }

    /**
     * gets the set type of an attribute
     *
     * @param attribute
     * @return type
     */
    public Type getAttributeType(String attribute) {
        Type type = attribute2type.get(attribute);
        if (type == null) {
            setAttributeTypeFromValues(attribute);
            type = attribute2type.get(attribute);
        }
        return type;
    }

    private void setAttributeType(String attribute, Type type) {
        attribute2type.put(attribute, type);
    }

    public ArrayList<String> getSampleOrder() {
        return sampleOrder;
    }

    public List<String> getAttributeOrder() {
        return attributeOrder;
    }

    public void setAttributeOrder(Collection<String> attributeOrder) {
        this.attributeOrder.clear();
        this.attributeOrder.addAll(attributeOrder);
    }

    public Set<String> getSampleSet() {
        return table.rowKeySet();
    }

    private Set<String> getAttributeSet() {
        return table.columnKeySet();
    }

    /**
     * gets attribute to values mapping for named sample
     *
     * @param sample
     * @return map
     */
    public Map<String, Object> getAttributesToValues(String sample) {
        return table.row(sample);
    }

    /**
     * gets sample to values mapping for named attribute
     *
     * @param attribute
     * @return map
     */
    public Map<String, Object> getSamples2Values(String attribute) {
        return table.column(attribute);
    }

    public int getNumberOfSamples() {
        return table.rowKeySet().size();
    }

    public int getNumberOfAttributes() {
        return table.columnKeySet().size();
    }


    public int getNumberOfUnhiddenAttributes() {
        int count = 0;
        for (String attribute : getAttributeOrder())
            if (!isHiddenAttribute(attribute) && !isSecretAttribute(attribute))
                count++;
        return count;
    }

    public ArrayList<String> getUnhiddenAttributes() {
        ArrayList<String> list = new ArrayList<>(getNumberOfAttributes());
        for (String attribute : getAttributeOrder())
            if (!isHiddenAttribute(attribute) && !isSecretAttribute(attribute))
                list.add(attribute);
        return list;
    }

    /**
     * get the value for a given sample and attribute
     *
     * @param sample
     * @param attribute
     * @return value or null
     */
    public Object get(String sample, String attribute) {
        if (attribute.equals(SAMPLE_ID))
            return sample;
        else
            return table.get(sample, attribute);
    }

    /**
     * get the value for a given sample and attribute
     *
     * @param sample
     * @param attribute
     * @return value or null
     */
    private Object get(String sample, HiddenAttribute attribute) {
        return table.get(sample, attribute.toString());
    }

    /**
     * put a value in the table
     *
     * @param sample
     * @param attribute
     * @param value
     */
    public void put(String sample, String attribute, Object value) {
        if (!sampleOrder.contains(sample))
            sampleOrder.add(sample);
        if (!attributeOrder.contains(attribute))
            attributeOrder.add(attribute);
        table.put(sample, attribute, value);
    }

    /**
     * put a value in the table
     *
     * @param sample
     * @param attribute
     * @param value
     */
    public void put(String sample, HiddenAttribute attribute, Object value) {
        put(sample, attribute.toString(), value);
    }


    /**
     * gets the color of a sample
     *
     * @param sampleName
     * @return sample color
     */
    public Color getSampleColor(String sampleName) {
        Object colorValue = get(sampleName, SampleAttributeTable.HiddenAttribute.Color);
        if (colorValue instanceof Integer) {
            return new Color((Integer) colorValue);
        } else
            return null;
    }

    /**
     * set the color associated with a sample
     *
     * @param sampleName
     * @param color      or null
     */
    public void putSampleColor(String sampleName, Color color) {
        put(sampleName, SampleAttributeTable.HiddenAttribute.Color, color != null ? color.getRGB() : null);
    }

    /**
     * is some sample colored
     *
     * @return true, if some sample colored
     */
    public boolean isSomeSampleColored() {
        for (String sample : getSampleOrder()) {
            if (get(sample, HiddenAttribute.Color) != null)
                return true;
        }
        return false;
    }

    /**
     * set the shape associated with a sample
     *
     * @param sampleName
     * @param shape
     */
    public void putSampleShape(String sampleName, String shape) {
        put(sampleName, HiddenAttribute.Shape, shape);
    }

    /**
     * get the shape associated with a sample
     *
     * @param sampleName
     * @return shape
     */
    public String getSampleShape(String sampleName) {
        Object shape = get(sampleName, HiddenAttribute.Shape);
        if (shape != null)
            return shape.toString();
        else
            return null;
    }

    /**
     * get the sample label
     *
     * @param sampleName
     * @return label
     */
    public String getSampleLabel(String sampleName) {
        Object label = get(sampleName, HiddenAttribute.Label);
        if (label != null)
            return label.toString();
        else
            return null;
    }

    /**
     * set the group id associated with a sample
     *
     * @param sampleName
     * @param id
     */
    public void putGroupId(String sampleName, String id) {
        put(sampleName, HiddenAttribute.GroupId, id);
    }

    /**
     * get the group id associated with a sample
     *
     * @param sampleName
     * @return join label
     */
    public String getGroupId(String sampleName) {
        Object obj = get(sampleName, HiddenAttribute.GroupId);
        if (obj == null)
            return null;
        else
            return obj.toString();
    }

    /**
     * are there any groups defined?
     *
     * @return true, if at least one sample has a group
     */
    public boolean hasGroups() {
        for (String sampleName : sampleOrder) {
            if (getGroupId(sampleName) != null)
                return true;
        }
        return false;
    }

    /**
     * put the label to be used for the sample
     *
     * @param sampleName
     * @param label
     */
    public void putSampleLabel(String sampleName, String label) {
        put(sampleName, SampleAttributeTable.HiddenAttribute.Label, label);
    }

    /**
     * look at all values for a given attribute and set its type
     *
     * @param attribute
     */
    private void setAttributeTypeFromValues(String attribute) {
        boolean isFloat = true;
        boolean isInteger = true;
        boolean isDate = true;

        for (String sample : getSampleSet()) {
            Object value = get(sample, attribute);
            if (value != null) {
                String label = value.toString();
                if (label.length() > 0) {
                    if (!Basic.isInteger(label))
                        isInteger = false;
                    if (!Basic.isFloat(label))
                        isFloat = false;
                    if (!Basic.isDate(label))
                        isDate = false;
                }
            }
        }
        Type type;
        if (isDate)
            type = Type.Date;
        else if (isInteger)
            type = Type.Integer;
        else if (isFloat)
            type = Type.Float;
        else
            type = Type.String;
        setAttributeType(attribute, type);
        for (String sample : getSampleSet()) {
            Object value = get(sample, attribute);
            if (value != null) {
                String label = value.toString();
                if (label != null) {
                    label = label.trim();
                    if (label.length() > 0) {
                        switch (type) {
                            case Date:
                                if (!(value instanceof Date))
                                    try {
                                        put(sample, attribute, DateFormat.getDateInstance().parse(label));
                                    } catch (ParseException e) {
                                        Basic.caught(e);
                                    }
                                break;
                            case Integer:
                                if (!(value instanceof Integer))
                                    put(sample, attribute, Integer.parseInt(label));
                                break;
                            case Float:
                                if (!(value instanceof Float))
                                    put(sample, attribute, Float.parseFloat(label));
                                break;
                            default:
                            case String:
                                if (!(value instanceof String))
                                    put(sample, attribute, label);
                                break;
                        }
                    }
                }
            }
        }
    }

    /**
     * set the attribute types from given values
     */
    private void setAllAttributeTypesFromValues() {
        for (String attribute : getAttributeSet()) {
            setAttributeTypeFromValues(attribute);
        }
    }

    /**
     * gets all source files embedded in this file
     *
     * @return embedded source files
     */
    public ArrayList<String> getSourceFiles() {
        final ArrayList<String> sourceFiles = new ArrayList<>();
        for (String sample : sampleOrder) {
            final Object obj = get(sample, SampleAttributeTable.HiddenAttribute.Source);
            if (obj != null) {
                sourceFiles.add(obj.toString());
            }
        }
        return sourceFiles;
    }

    /**
     * write the table to a file in QIIME-based format
     *
     * @param w
     * @throws IOException
     */
    public void write(Writer w, boolean ensureQIIMECompatible, boolean includeSecret) throws IOException {
        final Set<String> attributes = getAttributeSet();

        w.write(SAMPLE_ID);
        if (ensureQIIMECompatible) {
            if (attributes.contains("BarcodeSequence"))
                w.write("\tBarcodeSequence");
            w.write("\tLinkerPrimerSequence");
            for (String name : getAttributeOrder()) {
                if (!name.equals(SAMPLE_ID) && !name.equals("BarcodeSequence") && !name.equals("LinkerPrimerSequence") && !name.equals("Description") && (includeSecret || !isSecretAttribute(name))) {
                    w.write("\t" + name);
                }
            }
            w.write("\tDescription");
        } else {
            for (String name : getAttributeOrder()) {
                if (!name.equals(SAMPLE_ID) && !name.equals("BarcodeSequence") && !name.equals("LinkerPrimerSequence") && (includeSecret || !isSecretAttribute(name))) {
                    w.write("\t" + name);
                }
            }
        }
        w.write("\n");

        if (description != null && description.length() > 0)
            w.write((description.startsWith("#") ? description : description) + "\n");

        for (String sample : getSampleOrder()) {
            w.write(sample);
            final Map<String, Object> attributes2value = getAttributesToValues(sample);

            if (attributes2value != null) {
                if (ensureQIIMECompatible) {
                    if (attributes.contains("BarcodeSequence")) {
                        Object barcodeSequence = attributes2value.get("BarcodeSequence");
                        if (barcodeSequence != null)
                            w.write("\t" + Basic.quoteIfContainsTab(barcodeSequence));
                        else
                            w.write("\tAAA");
                    }
                    Object linkerPrimerSequence = attributes2value.get("LinkerPrimerSequence");
                    if (linkerPrimerSequence != null)
                        w.write("\t" + Basic.quoteIfContainsTab(linkerPrimerSequence));
                    else
                        w.write("\t");

                    for (String name : getAttributeOrder()) {
                        if (!name.equals(SAMPLE_ID) && !name.equals("BarcodeSequence") && !name.equals("LinkerPrimerSequence") && (includeSecret || !isSecretAttribute(name))) {
                            Object value = attributes2value.get(name);
                            if (value != null)
                                w.write("\t" + Basic.quoteIfContainsTab(value));
                            else
                                w.write("\tNA");
                        }
                    }
                    Object description = attributes2value.get("Description");
                    if (description != null)
                        w.write("\t" + Basic.quoteIfContainsTab(description));
                    else
                        w.write("\tNA");

                } else {
                    for (String name : getAttributeOrder()) {
                        if (!name.equals(SAMPLE_ID) && !name.equals("BarcodeSequence") && !name.equals("LinkerPrimerSequence") && (includeSecret || !isSecretAttribute(name))) {
                            Object value = attributes2value.get(name);
                            if (value != null)
                                w.write("\t" + Basic.quoteIfContainsTab(value));
                            else
                                w.write("\tNA");
                        }
                    }
                }
            }
            w.write("\n");
        }
        w.flush();
    }

    /**
     * gets a string of bytes representing this table
     *
     * @return bytes
     */
    public byte[] getBytes() {
        StringWriter w = new StringWriter();
        try {
            write(w, false, true);
        } catch (IOException ignored) {
        }
        return w.toString().getBytes();
    }

    /**
     * reads metadata from a readerWriter
     *
     * @param reader
     * @param knownSamples
     * @param clear        @throws IOException
     */
    public void read(Reader reader, Collection<String> knownSamples, boolean clear) throws IOException {
        int countNotFound = 0;
        if (clear)
            clear();
        try (BufferedReader r = new BufferedReader(reader)) {
            // read header line:
            String aLine = r.readLine();
            while (aLine != null && aLine.trim().length() == 0) {
                aLine = r.readLine();
            }

            if (aLine != null) {
                String[] tokens = Basic.splitWithQuotes(aLine, '\t');

                if (tokens.length < 1 || !tokens[0].startsWith(SAMPLE_ID)) {
                    throw new IOException(SAMPLE_ID + " tag not found, no sample-attributes data...");
                }
                final int tokensPerLine = tokens.length;
                final Set<String> newAttributes = new HashSet<>();
                final List<String> attributesOrder = new LinkedList<>();

                for (int i = 1; i < tokensPerLine; i++) // first token is SAMPLE_ID, not an attribute
                {
                    String attribute = tokens[i];

                    if (!isSecretAttribute(attribute) && !isHiddenAttribute(attribute) && (newAttributes.contains(attribute) || getAttributeOrder().contains(attribute))) {
                        int count = 1;
                        while (newAttributes.contains(attribute + "." + count) || getAttributeOrder().contains(attribute + "." + count)) {
                            count++;
                        }
                        System.err.println("Attribute " + attribute + " already exists, renaming to: " + attribute + "." + count);
                        attribute += "." + count;
                    }
                    newAttributes.add(attribute);
                    attributesOrder.add(attribute);
                }

                final String[] pos2attribute = attributesOrder.toArray(new String[0]);

                for (int i = 0; i < pos2attribute.length; i++) {
                    if (isHiddenAttribute(pos2attribute[i])) // don't import hidden attributes
                        pos2attribute[i] = null;
                    else
                        getAttributeOrder().add(pos2attribute[i]);
                }

                final Set<String> mentionedSamples = new HashSet<>();

                while ((aLine = r.readLine()) != null) {
                    aLine = aLine.trim();
                    if (aLine.startsWith("#")) {
                        if (description == null)
                            description = aLine;
                        else if (!description.equals("#EOF") && !description.equals("# EOF"))
                            description += " " + aLine;
                    } else {
                        tokens = Basic.splitWithQuotes(aLine, '\t');
                        if (tokens.length > 0) {
                            if (tokens.length != tokensPerLine)
                                throw new IOException("Expected " + tokensPerLine + " tokens, got " + tokens.length + " in line: " + aLine);
                            final String sample = tokens[0].trim();
                            if (sample.length() == 0)
                                continue; // empty?
                            if (mentionedSamples.contains(sample)) {
                                System.err.println("Sample occurs more than once: " + sample + ", ignoring repeated occurrences");
                                continue;
                            }
                            mentionedSamples.add(sample);
                            if (knownSamples == null || knownSamples.contains(sample)) {
                                Map<String, Object> attribute2value = new HashMap<>();
                                for (int i = 1; i < tokensPerLine; i++) {
                                    {
                                        String attribute = pos2attribute[i - 1];
                                        if (attribute != null)
                                            attribute2value.put(attribute, tokens[i].equals("NA") ? null : tokens[i]);
                                    }
                                }
                                addSample(sample, attribute2value, true, true);
                            } else {
                                System.err.println("Sample mentioned in metadata is not present in document, skipping: " + sample);
                                countNotFound++;
                            }
                        }
                    }
                }
                setAllAttributeTypesFromValues();
            }
        } finally {
            if (countNotFound > 0)
                NotificationsInSwing.showWarning(MainViewer.getLastActiveFrame(), "Loaded metadata, ignored " + countNotFound + " unknown input samples.");
            else {
                if (knownSamples != null) {
                    for (String sample : knownSamples)
                        putSampleLabel(sample, sample);
                }
            }
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    /**
     * determines which non-hidden attributes have a numerical interpretation and returns them
     *
     * @return numerical attributes
     */
    public Collection<String> getNumericalAttributes() {
        final Map<String, float[]> attributes = getNumericalAttributes(getUnhiddenAttributes());
        final SortedSet<String> result = new TreeSet<>(attributes.keySet());
        return result;
    }

    /**
     * determines which attributes have a numerical interpretation and returns their values
     *
     * @return numerical attributes
     */
    public HashMap<String, float[]> getNumericalAttributes(List<String> samples) {
        return getNumericalAttributes(samples, true);
    }

    /**
     * determines which attributes have a numerical interpretation and returns their values
     *
     * @return numerical attributes
     */
    public HashMap<String, float[]> getNumericalAttributes(List<String> samples, boolean normalize) {
        if (samples == null)
            samples = getSampleOrder();

        final HashMap<String, float[]> result = new HashMap<>();

        final Set<String> hiddenAttributes = new HashSet<>(Arrays.asList("BarcodeSequence", "LinkerPrimerSequence", "Description"));
        for (String name : getAttributeOrder()) {
            if (isSecretAttribute(name) || isHiddenAttribute(name) || name.equals("Size") || hiddenAttributes.contains(name))
                continue;
            switch (getAttributeType(name)) {
                case Float:
                case Integer: {
                    final float[] array = new float[samples.size()];
                    float maxAbs = Float.MIN_VALUE;

                    int i = 0;
                    for (String sample : samples) {
                        //System.err.println("sample: "+sample);
                        final Object object = get(sample, name);
                        if (object != null) {
                            String str = object.toString().trim();
                            float value = str.length() > 0 ? Float.parseFloat(str) : 0f;
                            array[i++] = value;
                            maxAbs = Math.max(Math.abs(value), maxAbs);
                        } else
                            array[i++] = 0;
                    }

                    if (normalize && maxAbs > 0) {
                        for (i = 0; i < array.length; i++)
                            array[i] /= maxAbs;
                    }

                    result.put(name, array);
                    break;
                }
                case String: {
                    String firstValue = null;
                    String secondValue = null;
                    for (String sample : samples) {
                        Object obj = get(sample, name);
                        if (obj != null) {
                            String str = obj.toString();
                            if (firstValue == null)
                                firstValue = str;
                            else if (!firstValue.equals(str)) {
                                if (secondValue == null) {
                                    secondValue = str;
                                } else if (!secondValue.equals(str)) {
                                    secondValue = null; // use this to indicate that not ok
                                    break;
                                }
                            }
                        }
                    }

                    if (secondValue != null) {
                        final float[] array = new float[samples.size()];
                        String firstState = null;
                        int i = 0;
                        for (String sample : samples) {
                            final Object object = get(sample, name);
                            if (object != null) {
                                String str = object.toString();

                                if (firstState == null || firstState.equals(str)) {
                                    firstState = str;
                                    array[i++] = 0;
                                } else
                                    array[i++] = 1;
                            } else
                                array[i++] = 0;
                        }
                        result.put(name, array);
                    }
                    break;
                }
            }
        }
        return result;
    }

    /**
     * sort the samples by the values of the given attribute
     *
     * @param attribute
     * @param ascending, if true, otherwise descending
     * @return true, if order is changed
     */
    public boolean sortSamplesByAttribute(String attribute, final boolean ascending) {
        final List<String> originalOrder = new ArrayList<>(getNumberOfSamples());
        originalOrder.addAll(getSampleOrder());

        try {
            final List<String> undefined = new ArrayList<>(getNumberOfSamples());

            final List<Pair<Object, String>> list = new ArrayList<>(getNumberOfSamples());
            for (String sample : getSampleOrder()) {
                Object value = get(sample, attribute);
                if (value != null && value.toString().length() > 0)
                    list.add(new Pair<>(value, sample));
                else
                    undefined.add(sample);
            }

            Pair<Object, String>[] array = list.toArray(new Pair[0]);

            switch (getAttributeType(attribute)) {
                case Integer:
                case Float:
                    Arrays.sort(array, (p1, p2) -> {
                        Float a1 = Float.parseFloat(p1.get1().toString());
                        Float a2 = Float.parseFloat(p2.get1().toString());
                        return ascending ? a1.compareTo(a2) : -a1.compareTo(a2);
                    });
                    break;
                default:
                case String:
                    Arrays.sort(array, (p1, p2) -> {
                        String a1 = p1.get1().toString();
                        String a2 = p2.get1().toString();
                        return ascending ? a1.compareTo(a2) : -a1.compareTo(a2);
                    });
                    break;
            }
            getSampleOrder().clear();
            for (Pair<Object, String> pair : array) {
                getSampleOrder().add(pair.get2());
            }
            getSampleOrder().addAll(undefined);
        } catch (Exception ex) {
            Basic.caught(ex);
            sampleOrder.clear();
            sampleOrder.addAll(originalOrder);
        }

        return !originalOrder.equals(getSampleOrder());
    }

    /**
     * is this a secret attribute (such as color etc)
     *
     * @param attribute
     * @return true, if secret
     */
    public boolean isSecretAttribute(String attribute) {
        return attribute.startsWith("@");
    }

    /**
     * is this a hidden attribute (such as color etc)
     *
     * @param attribute
     * @return true, if secret
     */
    public boolean isHiddenAttribute(String attribute) {
        return attribute.endsWith(" [hidden]");
    }

    /**
     * get a copy
     *
     * @return a copy
     */
    public SampleAttributeTable copy() {
        final SampleAttributeTable sampleAttributeTable = new SampleAttributeTable();
        try (StringWriter w = new StringWriter()) {
            write(w, false, true);
            sampleAttributeTable.read(new StringReader(w.toString()), getSampleOrder(), false);
        } catch (IOException e) {
            Basic.caught(e); // shouldn't happend
        }
        return sampleAttributeTable;
    }

    /**
     * move set of sample up or down one position
     *
     * @param up
     * @param samples
     */
    public void moveSamples(boolean up, Collection<String> samples) {
        samples = Basic.sortSubsetAsContainingSet(sampleOrder, samples);
        if (up) {
            for (String sample : samples) {
                final int index = sampleOrder.indexOf(sample);
                swapSamples(index, index - 1);
            }
        } else { // down
            for (String sample : Basic.reverseList(samples)) {
                final int index = sampleOrder.indexOf(sample);
                swapSamples(index, index + 1);
            }
        }
    }

    private void swapSamples(int index1, int index2) {
        final int min = Math.min(index1, index2);
        final int max = Math.max(index1, index2);
        if (min != max && min >= 0 && max < sampleOrder.size()) {
            final String minSample = sampleOrder.get(min);
            final String maxSample = sampleOrder.get(max);
            sampleOrder.set(min, maxSample);
            sampleOrder.set(max, minSample);
        }
    }
}
