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
package megan.core;

import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.parsers.blast.BlastMode;

import java.io.*;
import java.util.*;

/**
 * megan data table. Keeps a summary of all data and also holds metadata
 * Daniel Huson, 6.2010
 */
public class DataTable {
    public static final String MEGAN6_SUMMARY_TAG_NOT_USED_ANYMORE = "@MEGAN6"; // some early versions of MEGAN6 use this, but later versions do not
    public static final String MEGAN4_SUMMARY_TAG = "@MEGAN4";
    // new type of file:
    private static final String MEGAN6SummaryFormat_NotUsedAnyMore = "Summary6";
    private static final String MEGAN4SummaryFormat = "Summary4";
    private static final String MEGAN3SummaryFormat = "Summary";

    // tags used in file:
    final public static String CONTENT_TYPE = "@ContentType";
    final public static String CREATION_DATE = "@CreationDate";
    final public static String CREATOR = "@Creator";
    final public static String NAMES = "@Names";
    final public static String BLAST_MODE = "@BlastMode";
    final public static String DISABLED = "@Disabled";
    final public static String NODE_FORMATS = "@NodeFormats";
    final public static String EDGE_FORMATS = "@EdgeFormats";
    public static final String ALGORITHM = "@Algorithm";
    public static final String NODE_STYLE = "@NodeStyle";
    public static final String COLOR_TABLE = "@ColorTable";
    public static final String COLOR_EDITS = "@ColorEdits";

    public static final String PARAMETERS = "@Parameters";
    public static final String TOTAL_READS = "@TotalReads";
    public static final String ADDITIONAL_READS = "@AdditionalReads";

    public static final String COLLAPSE = "@Collapse";
    public static final String SIZES = "@Sizes";
    public static final String UIDS = "@Uids";

    // variables:
    private String contentType = MEGAN4SummaryFormat;
    private String creator = ProgramProperties.getProgramName();
    private String creationDate = null;

    private long totalReads = -1;
    private long additionalReads = -1;

    private final Vector<String> sampleNames = new Vector<>();
    private final Vector<BlastMode> blastModes = new Vector<>();
    private final Vector<Integer> sampleSizes = new Vector<>();
    private final Vector<Long> sampleUIds = new Vector<>();

    private final Set<String> disabledSamples = new HashSet<>();
    private DataTable originalData = null;

    private final Map<String, Set<Integer>> classification2collapsedIds = new HashMap<>();

    private final Map<String, String> classification2algorithm = new HashMap<>();
    private final Map<String, String> classification2NodeStyle = new HashMap<>();
    private final Map<String, String> classification2NodeFormats = new HashMap<>();
    private final Map<String, String> classification2EdgeFormats = new HashMap<>();

    private String colorTable = null;
    private String colorTableHeatMap = null;
    private String colorEdits = null;
    private boolean colorByPosition = false;


    private String parameters;

    private final Map<String, Map<Integer, Integer[]>> classification2class2counts = new HashMap<>();

    /**
     * constructor
     */
    public DataTable() {
    }

    /**
     * erase the summary block
     */
    public void clear() {
        creationDate = null;
        sampleNames.clear();
        blastModes.clear();
        sampleSizes.clear();
        sampleUIds.clear();
        disabledSamples.clear();
        totalReads = 0;
        additionalReads = 0;
        classification2collapsedIds.clear();
        classification2NodeStyle.clear();
        classification2algorithm.clear();
        classification2NodeFormats.clear();
        classification2EdgeFormats.clear();
        parameters = null;
        classification2class2counts.clear();
    }

    /**
     * read a complete file
     *
     * @param r
     * @return number of lines read
     * @throws IOException
     */
    public int read(BufferedReader r, boolean headerOnly) throws IOException {
        try {
            Set<String> disabledSamples = new HashSet<>();

            clear();
            int lineNumber = 0;
            String aLine;
            while ((aLine = r.readLine()) != null) {
                lineNumber++;
                aLine = aLine.trim();
                if (aLine.length() == 0 || aLine.startsWith("#"))
                    continue;
                final String[] tokens = aLine.split("\t");

                if (lineNumber == 1 && (aLine.equals(MEGAN6_SUMMARY_TAG_NOT_USED_ANYMORE) || aLine.equals(MEGAN4_SUMMARY_TAG) || aLine.equals("!MEGAN4")))
                    continue;

                if (aLine.equals("BEGIN_METADATA_TABLE") || aLine.equals("END_OF_DATA_TABLE")) // everything below this token is sample attribute
                    break; // BEGIN_METADATA_TABLE is for legacy purposes only and is no longer used or supported

                if (aLine.startsWith("@")) {
                    switch (tokens[0]) {
                        case CONTENT_TYPE: {
                            StringBuilder buf = new StringBuilder();
                            for (int i = 1; i < tokens.length; i++)
                                buf.append(" ").append(tokens[i]);
                            contentType = buf.toString().trim();
                            if (!contentType.startsWith(MEGAN6SummaryFormat_NotUsedAnyMore) && !contentType.startsWith(MEGAN4SummaryFormat))
                                throw new IOException("Wrong content type: " + contentType + ", expected: " + MEGAN4SummaryFormat);
                            break;
                        }
                        case CREATOR: {
                            StringBuilder buf = new StringBuilder();
                            for (int i = 1; i < tokens.length; i++)
                                buf.append(" ").append(tokens[i]);
                            creator = buf.toString().trim();
                            break;
                        }
                        case CREATION_DATE: {
                            StringBuilder buf = new StringBuilder();
                            for (int i = 1; i < tokens.length; i++)
                                buf.append(" ").append(tokens[i]);
                            creationDate = buf.toString().trim();
                            break;
                        }
                        case BLAST_MODE:
                            for (int i = 1; i < tokens.length; i++) {
                                BlastMode blastMode = BlastMode.valueOfIgnoreCase(tokens[i]);
                                if (blastMode == null)
                                    blastMode = BlastMode.Unknown;
                                blastModes.add(blastMode);
                            }
                            break;
                        case NAMES:
                            sampleNames.addAll(Arrays.asList(tokens).subList(1, tokens.length));
                            break;
                        case DISABLED:
                            disabledSamples.addAll(Arrays.asList(tokens).subList(1, tokens.length));
                            break;
                        case UIDS:
                            for (int i = 1; i < tokens.length; i++)
                                if (tokens[i] != null && !tokens[i].equals("null"))
                                    sampleUIds.add(Long.parseLong(tokens[i]));
                            break;
                        case SIZES:
                            for (int i = 1; i < tokens.length; i++)
                                sampleSizes.add(Basic.parseInt(tokens[i]));
                            break;
                        case TOTAL_READS:
                            totalReads = (Integer.parseInt(tokens[1]));
                            break;
                        case ADDITIONAL_READS:
                            additionalReads = (Basic.parseInt(tokens[1]));
                            break;
                        case COLLAPSE:
                            if (tokens.length > 1) {
                                String data = tokens[1];
                                Set<Integer> collapsedIds = new HashSet<>();
                                classification2collapsedIds.put(data, collapsedIds);
                                for (int i = 2; i < tokens.length; i++)
                                    collapsedIds.add(Integer.parseInt(tokens[i]));
                            }
                            break;
                        case ALGORITHM:
                            if (tokens.length > 1) {
                                String data = tokens[1];
                                StringBuilder buf = new StringBuilder();
                                for (int i = 2; i < tokens.length; i++)
                                    buf.append(" ").append(tokens[i]);
                                classification2algorithm.put(data, buf.toString().trim());
                            }
                            break;
                        case NODE_STYLE:
                            if (tokens.length > 1) {
                                String data = tokens[1];
                                StringBuilder buf = new StringBuilder();
                                for (int i = 2; i < tokens.length; i++)
                                    buf.append(" ").append(tokens[i]);
                                classification2NodeStyle.put(data, buf.toString().trim());
                            }
                            break;
                        case COLOR_TABLE:
                            colorTable = tokens[1];
                            colorByPosition = false;
                            for (int k = 2; k < tokens.length; k++) {
                                if (tokens[k].equals("byPosition"))
                                    colorByPosition = true;
                                else
                                    colorTableHeatMap = tokens[k];
                            }
                            break;
                        case COLOR_EDITS:
                            if (tokens.length > 1) {
                                colorEdits = Basic.toString(tokens, 1, tokens.length - 1, "\t");
                            } else
                                colorEdits = null;
                            break;
                        case NODE_FORMATS:
                            if (tokens.length > 1) {
                                String data = tokens[1];
                                StringBuilder buf = new StringBuilder();
                                for (int i = 2; i < tokens.length; i++)
                                    buf.append(" ").append(tokens[i]);
                                classification2NodeFormats.put(data, buf.toString().trim());
                            }
                            break;
                        case EDGE_FORMATS:
                            if (tokens.length > 1) {
                                String data = tokens[1];
                                StringBuilder buf = new StringBuilder();
                                for (int i = 2; i < tokens.length; i++)
                                    buf.append(" ").append(tokens[i]);
                                classification2EdgeFormats.put(data, buf.toString().trim());
                            }
                            break;
                        case PARAMETERS:
                            if (tokens.length > 1) {
                                StringBuilder buf = new StringBuilder();
                                for (int i = 1; i < tokens.length; i++)
                                    buf.append(" ").append(tokens[i]);
                                parameters = buf.toString().trim();
                            }
                            break;
                        default:
                            System.err.println("Line: " + lineNumber + ": Skipping unknown token: " + tokens[0]);
                            break;
                    }
                } else {
                    if (headerOnly)
                        break;
                    if (tokens.length > 2) {
                        String classification = ClassificationType.getFullName(tokens[0]);
                        Integer classId = Integer.parseInt(tokens[1]);

                        Map<Integer, Integer[]> class2counts = classification2class2counts.get(classification);
                        if (class2counts == null) {
                            class2counts = new HashMap<>();
                            classification2class2counts.put(classification, class2counts);
                        }
                        Integer[] counts = class2counts.get(classId);
                        if (counts == null) {
                            counts = new Integer[Math.min(getNumberOfSamples(), tokens.length - 2)];
                            class2counts.put(classId, counts);
                        }
                        for (int i = 2; i < Math.min(tokens.length, counts.length + 2); i++) {
                            counts[i - 2] = Integer.parseInt(tokens[i]);
                        }
                    } else
                        System.err.println("Line " + lineNumber + ": Too few tokens in classification: " + aLine);
                }
            }
            if (disabledSamples.size() > 0) {
                disableSamples(disabledSamples);
            }
            if (blastModes.size() < getNumberOfSamples()) {
                for (int i = blastModes.size(); i < getNumberOfSamples(); i++) {
                    blastModes.add(BlastMode.Unknown);
                }
            }
            return lineNumber;
        } catch (IOException ex) {
            Basic.caught(ex);
            throw ex;
        } catch (Exception ex) {
            Basic.caught(ex);
            throw new IOException(ex);
        }
    }

    /**
     * returns the user state as bytes (for saving to auxiliary data in RMA file)
     *
     * @return bytes of auxiliary data
     * @throws IOException
     */
    public byte[] getUserStateAsBytes() throws IOException {
        StringWriter w = new StringWriter();
        w.write(MEGAN4_SUMMARY_TAG + "\n");
        writeHeader(w);
        return w.toString().getBytes("UTF-8");
    }

    /**
     * write the datatable
     *
     * @param w
     * @throws IOException
     */
    public void write(Writer w) throws IOException {
        boolean useOriginal = (originalData != null && disabledSamples.size() > 0);
        write(w, useOriginal);

    }

    /**
     * write data to writer
     *
     * @param w
     * @param useOriginal use original data, if set
     * @throws IOException
     */
    private void write(Writer w, boolean useOriginal) throws IOException {
        if (useOriginal) {
            originalData.disabledSamples.addAll(disabledSamples);
            originalData.write(w, false);
            originalData.disabledSamples.clear();
        } else {
            // write the header:
            writeHeader(w);

            // write the data:
            for (String classification : classification2class2counts.keySet()) {
                Map<Integer, Integer[]> class2counts = classification2class2counts.get(classification);
                classification = ClassificationType.getShortName(classification);
                for (Integer classId : class2counts.keySet()) {
                    Integer[] counts = class2counts.get(classId);
                    if (counts != null) {
                        w.write(classification + "\t" + classId);
                        int last = Math.min(counts.length, getNumberOfSamples()) - 1;
                        while (last > 0 && (counts[last] == null || counts[last] == 0))
                            last--;

                        for (int i = 0; i <= last; i++) {
                            if (i < counts.length)
                                w.write("\t" + (counts[i] != null ? counts[i] : 0));
                        }
                        w.write("\n");
                    }
                }
            }
            w.write("END_OF_DATA_TABLE\n");
        }
    }

    /**
     * write the header to a writer
     *
     * @param w
     * @throws IOException
     */
    public void writeHeader(Writer w) throws IOException {
        w.write(CREATOR + "\t" + (creator != null ? creator : ProgramProperties.getProgramName()) + "\n");
        w.write(CREATION_DATE + "\t" + (creationDate != null ? creationDate : ((new Date()).toString())) + "\n");
        w.write(CONTENT_TYPE + "\t" + getContentType() + "\n");
        w.write(NAMES);
        for (String dataName : sampleNames) w.write("\t" + dataName);
        w.write("\n");
        w.write(BLAST_MODE);
        for (BlastMode blastMode : blastModes) w.write("\t" + blastMode.toString());
        w.write("\n");
        if (disabledSamples.size() > 0) {
            w.write(DISABLED);
            for (String dataName : disabledSamples) w.write("\t" + dataName);
            w.write("\n");
        }
        if (sampleUIds.size() > 0) {
            w.write(UIDS);
            for (Long dataUid : sampleUIds) w.write("\t" + dataUid);
            w.write("\n");
        }
        if (sampleSizes.size() > 0) {
            w.write(SIZES);
            for (Integer dataSize : sampleSizes) w.write("\t" + dataSize);
            w.write("\n");
        }

        if (totalReads != -1)
            w.write(TOTAL_READS + "\t" + totalReads + "\n");

        if (additionalReads != -1)
            w.write(ADDITIONAL_READS + "\t" + additionalReads + "\n");

        for (String classification : classification2collapsedIds.keySet()) {
            Set<Integer> collapsed = classification2collapsedIds.get(classification);
            if (collapsed != null && collapsed.size() > 0) {
                w.write(COLLAPSE + "\t" + classification);
                for (Integer id : collapsed)
                    w.write("\t" + id);
            }
            w.write("\n");
        }

        for (String classification : classification2algorithm.keySet()) {
            String algorithm = classification2algorithm.get(classification);
            if (algorithm != null) {
                w.write(ALGORITHM + "\t" + classification + "\t" + algorithm + "\n");
            }
        }

        if (parameters != null)
            w.write(PARAMETERS + "\t" + parameters + "\n");

        for (String classification : classification2NodeStyle.keySet()) {
            String nodeType = classification2NodeStyle.get(classification);
            if (nodeType != null) {
                w.write(NODE_STYLE + "\t" + classification + "\t" + nodeType + "\n");
            }
        }
        if (colorTable != null)
            w.write(COLOR_TABLE + "\t" + colorTable + (colorByPosition ? "\tbyPosition\t" : "\t") + getColorTableHeatMap() + "\n");
        if (colorEdits != null)
            w.write(COLOR_EDITS + "\t" + colorEdits + "\n");

        for (String classification : classification2NodeFormats.keySet()) {
            String formatting = classification2NodeFormats.get(classification);
            if (formatting != null) {
                w.write(NODE_FORMATS + "\t" + classification + "\t" + formatting + "\n");
            }
        }

        for (String classification : classification2EdgeFormats.keySet()) {
            String formatting = classification2EdgeFormats.get(classification);
            if (formatting != null)
                w.write(EDGE_FORMATS + "\t" + classification + "\t" + formatting + "\n");
        }
    }

    /**
     * get the header in pretty print (HTML)
     *
     * @return header
     * @throws IOException
     */
    public String getHeaderPrettyPrint() throws IOException {
        Writer w = new StringWriter();

        w.write("<b>" + CREATOR.substring(1) + ":</b> " + (creator != null ? creator : ProgramProperties.getProgramName()) + "<br>");
        w.write("<b>" + CREATION_DATE.substring(1) + ":</b>" + (creationDate != null ? creationDate : "unknown") + "<br>");
        w.write("<b>" + CONTENT_TYPE.substring(1) + ":</b>" + getContentType() + "<br>");
        w.write("<b>" + NAMES.substring(1) + ":</b>");
        for (String dataName : sampleNames) w.write(" " + dataName);
        w.write("<br>");
        w.write("<b>" + BLAST_MODE.substring(1) + ":</b>");
        for (BlastMode blastMode : blastModes) w.write(" " + blastMode.toString());
        w.write("<br>");

        if (sampleUIds.size() > 0) {
            w.write("<b>" + UIDS.substring(1) + ":</b>");
            for (Long dataUid : sampleUIds) w.write(" " + dataUid);
            w.write("<br>");
        }
        if (sampleSizes.size() > 0) {
            w.write("<b>" + SIZES.substring(1) + ":</b>");
            for (Integer dataSize : sampleSizes) w.write(" " + dataSize);
            w.write("<br>");
        }

        if (totalReads != -1)
            w.write("<b>" + TOTAL_READS.substring(1) + ":</b> " + totalReads + "<br>");
        if (additionalReads != -1)
            w.write("<b>" + ADDITIONAL_READS.substring(1) + ":</b> " + additionalReads + "<br>");

        w.write("<b>Classifications:</b> ");
        for (String classification : classification2class2counts.keySet()) {
            Map<Integer, Integer[]> class2counts = classification2class2counts.get(classification);
            int size = class2counts != null ? class2counts.size() : 0;
            w.write(classification + " (" + size + " classes)");
        }
        w.write("<br>");

        for (String classification : classification2algorithm.keySet()) {
            String algorithm = classification2algorithm.get(classification);
            if (algorithm != null) {
                w.write("<b>" + ALGORITHM.substring(1) + ":</b> " + classification + ": " + algorithm + "<br>");
            }
        }

        if (parameters != null)
            w.write("<b>" + PARAMETERS.substring(1) + ":</b> " + parameters + "<br>");

        if (colorTable != null)
            w.write("<b>ColorTable:</b> " + colorTable + (colorByPosition ? " byPosition" : "") + " <b>HeatMapColorTable:</b>" + colorTableHeatMap + "<br>");

        if (colorEdits != null)
            w.write("<b>ColorEdits:</b> " + Basic.abbreviateDotDotDot(colorEdits, 50) + " <br>");

        return w.toString();
    }

    /**
     * get summary
     *
     * @return header
     * @throws IOException
     */
    public String getSummary() throws IOException {
        Writer w = new StringWriter();

        w.write(CREATOR.substring(1) + ": " + (creator != null ? creator : ProgramProperties.getProgramName()) + "\n");
        w.write(CREATION_DATE.substring(1) + ":" + (creationDate != null ? creationDate : "unknown") + "\n");
        w.write(CONTENT_TYPE.substring(1) + ":" + getContentType() + "\n");
        w.write(NAMES.substring(1) + ":");
        for (String dataName : sampleNames) w.write(" " + dataName);
        w.write("\n");
        w.write(BLAST_MODE.substring(1) + ":");
        for (BlastMode blastMode : blastModes) w.write(" " + blastMode.toString());
        w.write("\n");

        if (sampleUIds.size() > 0) {
            w.write(UIDS.substring(1) + ": ");
            for (Long dataUid : sampleUIds) w.write(" " + dataUid);
            w.write("\n");
        }
        if (sampleSizes.size() > 0) {
            w.write(SIZES.substring(1) + ": ");
            for (Integer dataSize : sampleSizes) w.write(" " + dataSize);
            w.write("\n");
        }

        if (totalReads != -1)
            w.write(TOTAL_READS.substring(1) + ": " + totalReads + "\n");
        if (additionalReads != -1)
            w.write(ADDITIONAL_READS.substring(1) + ": " + additionalReads + "\n");

        w.write("Classifications:\n");
        for (String classification : classification2class2counts.keySet()) {
            Map<Integer, Integer[]> class2counts = classification2class2counts.get(classification);
            int size = class2counts != null ? class2counts.size() : 0;
            w.write(" " + classification + " (" + size + " classes)");
        }
        w.write("\n");

        for (String classification : classification2algorithm.keySet()) {
            String algorithm = classification2algorithm.get(classification);
            if (algorithm != null) {
                w.write(ALGORITHM.substring(1) + ": " + classification + ": " + algorithm + "\n");
            }
        }

        if (parameters != null)
            w.write(PARAMETERS.substring(1) + ": " + parameters + "\n");
        return w.toString();
    }

    /**
     * imports a MEGAN3-summary file
     *
     * @param r
     * @param headerOnly
     * @return number of lines read
     * @throws IOException
     */
    public int importMEGAN3SummaryFile(String fileName, BufferedReader r, boolean headerOnly) throws IOException {
        int lineNumber = 0;
        try {
            String aLine;
            sampleNames.clear();
            sampleNames.add(Basic.getFileBaseName(fileName));
            blastModes.clear();
            while ((aLine = r.readLine()) != null) {
                lineNumber++;
                aLine = aLine.trim();
                if (aLine.length() == 0 || aLine.startsWith("#"))
                    continue;

                if (lineNumber == 1 && aLine.equals("!MEGAN"))
                    continue;
                if (aLine.startsWith("@")) // preamble
                {
                    int separatorPos = aLine.indexOf("=");
                    if (separatorPos == -1)
                        throw new IOException("Line " + lineNumber + ": Can't parse: " + aLine);
                    String first = aLine.substring(0, separatorPos);
                    String second = aLine.substring(separatorPos + 1);
                    switch (first) {
                        case CONTENT_TYPE:
                            if (!second.equals(MEGAN3SummaryFormat))
                                throw new IOException("Wrong format: " + second);
                            setContentType(MEGAN4SummaryFormat + "\t(imported from MEGAN3 " + second + " format)");
                            break;
                        case CREATOR:
                            setCreator(second);
                            break;
                        case CREATION_DATE:
                            setCreationDate(second);
                            break;
                        case PARAMETERS:
                            setParameters(second);
                            break;
                        case ALGORITHM:
                            setAlgorithm(ClassificationType.Taxonomy.toString(), second);
                            break;
                        case COLLAPSE:
                            if (second.length() > 0) {
                                String[] tokens = second.split(";");
                                Set<Integer> collapse = new HashSet<>();
                                for (String token : tokens) {
                                    collapse.add(Integer.parseInt(token));
                                }
                                setCollapsed(ClassificationType.Taxonomy.toString(), collapse);
                            }
                            break;
                        case NODE_FORMATS:
                            setNodeFormats(ClassificationType.Taxonomy.toString(), second);
                            break;
                        case EDGE_FORMATS:
                            setEdgeFormats(ClassificationType.Taxonomy.toString(), second);
                            break;
                        case "@Format":
                            // ignore
                            break;
                        case TOTAL_READS:
                            totalReads = Integer.parseInt(second);
                            break;
                    }
                } else if (!headerOnly)// data line
                {
                    String[] tokens = aLine.split(" ");
                    if (tokens.length == 3) {
                        int classId = Integer.parseInt(tokens[0]);
                        int value = Integer.parseInt(tokens[2]);
                        setClassification2Class2Count(ClassificationType.Taxonomy.toString(), classId, 0, value);
                    }
                }
            }
            if (!headerOnly)
                determineSizesFromTaxonomyClassification();
        } finally {
            r.close();
        }
        return lineNumber;
    }

    /**
     * determine the size of datasets from the taxonomy classification
     */
    private void determineSizesFromTaxonomyClassification() {
        // determine sizes:
        Map<Integer, Integer[]> class2count = classification2class2counts.get(ClassificationType.Taxonomy.toString());
        if (class2count != null) {
            int[] sizes = new int[getNumberOfSamples()];
            for (Integer classId : class2count.keySet()) {
                Integer[] counts = class2count.get(classId);
                if (counts != null) {
                    for (int i = 0; i < getNumberOfSamples(); i++) {
                        if (counts[i] != null)
                            sizes[i] += counts[i];
                    }
                }
            }
            sampleSizes.clear();
            for (Integer size : sizes)
                sampleSizes.add(size);
        }
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * gets the classification2class2counts mapping
     *
     * @return mapping
     */
    public Map<String, Map<Integer, Integer[]>> getClassification2Class2Counts() {
        return classification2class2counts;
    }

    /**
     * set the classification2class2count value for a given classification, classId, datasetid and count
     *
     * @param classification
     * @param classId
     * @param sampleId
     * @param count
     */
    public void setClassification2Class2Count(String classification, int classId, int sampleId, int count) {
        Map<Integer, Integer[]> class2count = classification2class2counts.get(classification);
        if (class2count == null)
            class2count = new HashMap<>();
        classification2class2counts.put(classification, class2count);
        Integer[] counts = class2count.get(classId);
        if (counts == null) {
            counts = new Integer[getNumberOfSamples()];
            class2count.put(classId, counts);
        }
        counts[sampleId] = count;
    }

    /**
     * gets all non-disabled sample names
     *
     * @return sample names
     */
    public String[] getSampleNames() {
        return sampleNames.toArray(new String[sampleNames.size()]);
    }

    /**
     * gets all blast modes
     *
     * @return blast modes
     */
    public BlastMode[] getBlastModes() {
        BlastMode[] result = new BlastMode[getNumberOfSamples()];
        for (int i = 0; i < result.length; i++) {
            if (i < blastModes.size()) {
                result[i] = blastModes.get(i);
            } else
                result[i] = BlastMode.Unknown;
        }
        return result;
    }

    /**
     * gets the first blast mode
     *
     * @return blast mode
     */
    public BlastMode getBlastMode() {
        if (blastModes.size() > 0)
            return blastModes.get(0);
        else
            return BlastMode.Unknown;
    }

    public void setBlastMode(int index, BlastMode blastMode) {
        blastModes.ensureCapacity(index + 1);
        blastModes.insertElementAt(blastMode, index);
    }

    /**
     * set basic stuff about samples
     *
     * @param names
     * @param uids
     * @param sizes
     * @param modes
     */
    public void setSamples(String[] names, Long[] uids, Integer[] sizes, BlastMode[] modes) {
        sampleNames.clear();
        if (names != null)
            sampleNames.addAll(Arrays.asList(names));
        sampleUIds.clear();
        if (uids != null)
            sampleUIds.addAll(Arrays.asList(uids));
        sampleSizes.clear();
        if (sizes != null)
            sampleSizes.addAll(Arrays.asList(sizes));
        blastModes.clear();
        if (modes != null)
            blastModes.addAll(Arrays.asList(modes));
        else {
            for (int i = 0; i < getNumberOfSamples(); i++)
                blastModes.add(BlastMode.Unknown);
        }
    }

    public Integer[] getSampleSizes() {
        return sampleSizes.toArray(new Integer[sampleSizes.size()]);
    }

    public Long[] getSampleUIds() {
        return sampleUIds.toArray(new Long[sampleUIds.size()]);
    }

    /**
     * get the algorithm string for a classification
     *
     * @param classification
     * @return algorithm string
     */
    public String getAlgorithm(String classification) {
        return classification2algorithm.get(classification);
    }

    /**
     * set the algorithm string for a classification
     *
     * @param classification
     * @param algorithm
     */
    public void setAlgorithm(String classification, String algorithm) {
        classification2algorithm.put(classification, algorithm);
    }

    /**
     * get the parameters string for the dataset
     *
     * @return parameters string
     */
    public String getParameters() {
        return parameters;
    }

    /**
     * set the parameters string
     *
     * @param parameters
     */
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    /**
     * set the formatting string for a classification
     *
     * @param classification
     * @param format
     */
    public void setNodeFormats(String classification, String format) {
        if (format == null || format.length() == 0)
            classification2NodeFormats.put(classification, null);
        else
            classification2NodeFormats.put(classification, format);
    }

    /**
     * get the node type string for a classification
     *
     * @param classification
     * @return formatting string
     */
    public String getNodeStyle(String classification) {
        return classification2NodeStyle.get(classification);
    }

    /**
     * get the node type string for a classification
     *
     * @param classification
     * @return formatting string
     */
    public String getNodeStyle(String classification, String defaultValue) {
        String style = classification2NodeStyle.get(classification);
        if (style != null)
            return style;
        else
            return defaultValue;
    }

    /**
     * set the node type string for a classification
     *
     * @param classification
     * @param nodeStyle      Possible values: circle, heatmap, heatmap2, barchart, coxcombs, piechart
     */
    public void setNodeStyle(String classification, String nodeStyle) {
        if (nodeStyle == null || nodeStyle.length() == 0)
            classification2NodeStyle.put(classification, null);
        else
            classification2NodeStyle.put(classification, nodeStyle);
    }

    /**
     * get the formatting string for a classification
     *
     * @param classification
     * @return formatting string
     */
    public String getNodeFormats(String classification) {
        return classification2NodeFormats.get(classification);
    }

    /**
     * set the formatting string for a classification
     *
     * @param classification
     * @param format
     */
    public void setEdgeFormats(String classification, String format) {
        if (format == null || format.length() == 0)
            classification2EdgeFormats.put(classification, null);
        else
            classification2EdgeFormats.put(classification, format);
    }

    /**
     * get the formatting string for a classification
     *
     * @param classification
     * @return formatting string
     */
    public String getEdgeFormats(String classification) {
        return classification2EdgeFormats.get(classification);
    }

    /**
     * get the collapsed ids for the named classification
     *
     * @param classification
     * @return set of collapsed ids
     */
    public Set<Integer> getCollapsed(String classification) {
        return classification2collapsedIds.get(classification);
    }

    /**
     * sets the set of collapsed ids for a classification
     *
     * @param classification
     * @param collapsedIds
     */
    public void setCollapsed(String classification, Set<Integer> collapsedIds) {
        classification2collapsedIds.put(classification, collapsedIds);
    }

    /**
     * gets the content type
     *
     * @return
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * sets the content type
     *
     * @param contentType
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Map<Integer, Integer[]> getClass2Counts(ClassificationType classification) {
        return classification2class2counts.get(classification.toString());
    }

    public Map<Integer, Integer[]> getClass2Counts(String classification) {
        return classification2class2counts.get(classification);
    }

    public void setClass2Counts(String classification, Map<Integer, Integer[]> classId2count) {
        classification2class2counts.put(classification, classId2count);
    }

    public void setTotalReads(long totalReads) {
        this.totalReads = totalReads;
    }

    public long getTotalReads() {
        if (totalReads < 0)
            return 0;
        else
            return totalReads;
    }

    public void setAdditionalReads(long additionalReads) {
        this.additionalReads = additionalReads;
    }

    public long getAdditionalReads() {
        if (additionalReads < 0)
            return 0;
        else
            return additionalReads;
    }

    /**
     * copy the given Megan4Summary
     *
     * @param megan4Table
     */
    public void copy(DataTable megan4Table) {
        clear();
        StringWriter sw = new StringWriter();
        try {
            megan4Table.write(sw, false);
            read(new BufferedReader(new StringReader(sw.toString())), false);
        } catch (IOException e) {
            Basic.caught(e);
        }
    }

    public String toString() {
        StringWriter w = new StringWriter();
        try {
            write(w, false);
        } catch (IOException e) {
            Basic.caught(e);
        }
        return w.toString();
    }

    /**
     * replace an existing sample name
     *
     * @param pid
     * @param newName
     */
    public void changeSampleName(Integer pid, String newName) {
        sampleNames.set(pid, newName);
    }

    public void duplicateSample(String name, String newName) {
        if (!sampleNames.contains(newName)) {
            int srcId = Basic.getIndex(name, sampleNames);
            sampleSizes.add(sampleSizes.get(srcId));
            sampleNames.add(newName);
            blastModes.add(blastModes.get(srcId));
            sampleUIds.add(System.currentTimeMillis());

            int tarId = Basic.getIndex(newName, sampleNames);
            for (Map<Integer, Integer[]> class2counts : classification2class2counts.values()) {
                for (Integer classId : class2counts.keySet()) {
                    Integer[] counts = class2counts.get(classId);
                    if (counts != null) {
                        int newLength = Math.max(counts.length + 1, tarId + 1);
                        Integer[] newCounts = new Integer[newLength];
                        System.arraycopy(counts, 0, newCounts, 0, counts.length);
                        newCounts[tarId] = counts[srcId];
                        class2counts.put(classId, newCounts);
                    }
                }
            }
            totalReads += sampleSizes.get(srcId);
        }
    }

    public void removeSample(String name) {
        removeSamples(Collections.singletonList(name));
    }

    public void removeSamples(Collection<String> toDelete) {
        Set<Integer> dead = new HashSet<>();
        for (String name : toDelete) {
            dead.add(Basic.getIndex(name, sampleNames));
        }

        // System.err.println("Existing sample name: "+Basic.toString(sampleNames,","));
        int top = sampleNames.size();
        for (int pid = top - 1; pid >= 0; pid--) {
            if (dead.contains(pid)) {
                sampleNames.remove(pid);
                sampleSizes.remove(pid);
                if (sampleUIds.size() > pid)
                    sampleUIds.remove(pid);
                if (blastModes.size() > pid)
                    blastModes.remove(pid);
            }
        }
        int alive = sampleNames.size();
        // System.err.println("Remaining sample name: "+Basic.toString(sampleNames,","));

        for (Map<Integer, Integer[]> class2counts : classification2class2counts.values()) {
            for (Integer classId : class2counts.keySet()) {
                Integer[] counts = class2counts.get(classId);
                if (counts != null) {
                    Integer[] newCounts = new Integer[alive];
                    int k = 0;
                    for (int i = 0; i < counts.length; i++) {
                        if (!dead.contains(i)) {
                            newCounts[k++] = counts[i];
                        }
                        class2counts.put(classId, newCounts);
                    }
                }
            }
        }
        totalReads = 0;
        for (Integer size : sampleSizes) {
            if (size != null)
                totalReads += size;
        }
    }

    public String getColorTable() {
        return colorTable;
    }

    public void setColorTableHeatMap(String colorTableHeatMap) {
        this.colorTableHeatMap = colorTableHeatMap;
    }

    public String getColorTableHeatMap() {
        return colorTableHeatMap;
    }

    public void setColorTable(String colorTable, boolean colorByPosition, String colorTableHeatMap) {
        this.colorTable = colorTable;
        this.colorByPosition = colorByPosition;
        this.colorTableHeatMap = colorTableHeatMap;
    }

    public boolean isColorByPosition() {
        return colorByPosition;
    }

    public String getColorEdits() {
        return colorEdits;
    }

    public void setColorEdits(String colorEdits) {
        this.colorEdits = colorEdits;
    }

    /**
     * add the named sample from the given source
     *
     * @param sample
     * @param source
     */
    public void addSample(String sample, DataTable source) {
        DataTable target = this;

        if (!Arrays.asList(target.getSampleNames()).contains(sample)) {
            int srcId = Basic.getIndex(sample, source.sampleNames);
            target.sampleSizes.add(source.sampleSizes.get(srcId));
            target.sampleNames.add(sample);
            target.sampleUIds.add(System.currentTimeMillis());
            if (srcId < blastModes.size())
                target.blastModes.add(blastModes.get(srcId));

            int tarId = Basic.getIndex(sample, target.sampleNames);

            for (String classification : source.classification2class2counts.keySet()) {
                Map<Integer, Integer[]> sourceClass2counts = source.classification2class2counts.get(classification);
                Map<Integer, Integer[]> targetClass2counts = target.classification2class2counts.get(classification);
                if (targetClass2counts == null) {
                    targetClass2counts = new HashMap<>();
                    target.classification2class2counts.put(classification, targetClass2counts);
                }
                for (Integer classId : sourceClass2counts.keySet()) {
                    Integer[] sourceCounts = sourceClass2counts.get(classId);
                    if (sourceCounts != null && srcId < sourceCounts.length && sourceCounts[srcId] != null) {
                        Integer[] targetCounts = targetClass2counts.get(classId);
                        Integer[] newCounts = new Integer[tarId + 1];
                        if (targetCounts != null) {
                            System.arraycopy(targetCounts, 0, newCounts, 0, targetCounts.length);
                        }
                        newCounts[tarId] = sourceCounts[srcId];
                        targetClass2counts.put(classId, newCounts);
                    }
                }
            }
            if (target.totalReads > 0)
                target.totalReads += source.sampleSizes.get(srcId);
            else
                target.totalReads = source.sampleSizes.get(srcId);
        }
    }

    /**
     * add the named sample
     *
     * @param sample
     * @param sourceClassification2class2counts
     */
    public void addSample(String sample, int sampleSize, BlastMode mode, int srcId, Map<String, Map<Integer, Integer[]>> sourceClassification2class2counts) {
        if (!Arrays.asList(this.getSampleNames()).contains(sample)) {
            this.sampleSizes.add(sampleSize);
            this.sampleNames.add(sample);
            this.sampleUIds.add(System.currentTimeMillis());
            this.blastModes.add(mode);

            int tarId = Basic.getIndex(sample, this.sampleNames);

            for (String classification : sourceClassification2class2counts.keySet()) {
                Map<Integer, Integer[]> sourceClass2counts = sourceClassification2class2counts.get(classification);
                Map<Integer, Integer[]> targetClass2counts = this.classification2class2counts.get(classification);
                if (targetClass2counts == null) {
                    targetClass2counts = new HashMap<>();
                    this.classification2class2counts.put(classification, targetClass2counts);
                }
                for (Integer classId : sourceClass2counts.keySet()) {
                    Integer[] sourceCounts = sourceClass2counts.get(classId);
                    if (sourceCounts != null && srcId < sourceCounts.length && sourceCounts[srcId] != null) {
                        Integer[] targetCounts = targetClass2counts.get(classId);
                        Integer[] newCounts = new Integer[tarId + 1];
                        if (targetCounts != null) {
                            System.arraycopy(targetCounts, 0, newCounts, 0, targetCounts.length);
                        }
                        newCounts[tarId] = sourceCounts[srcId];
                        targetClass2counts.put(classId, newCounts);
                    }
                }
            }
            if (this.totalReads >= 0)
                this.totalReads += sampleSize;
            else
                this.totalReads = sampleSize;
        }
    }

    /**
     * extract a set of samples to the given target
     *
     * @param samples
     * @param target
     * @return set of samples
     */
    public void extractSamplesTo(Collection<String> samples, DataTable target) {
        Set<String> toDelete = new HashSet<>();
        toDelete.addAll(sampleNames);
        toDelete.removeAll(samples);
        target.copy(this);
        target.removeSamples(toDelete);
    }


    /**
     * merge the named samples
     *
     * @param samples
     */
    public void mergeSamples(Collection<String> samples, String newName) {
        if (sampleNames.contains(newName))
            return;

        Set<Integer> pids = new HashSet<>();
        BlastMode mode=null;
        for (String name : samples) {
            int pid = Basic.getIndex(name, sampleNames);
            if (pid == -1) {
                System.err.println("No such sample: " + name);
                return;
            }
            pids.add(pid);
            if (mode == null) {
                mode = (pid < blastModes.size() ? blastModes.get(pid) : BlastMode.Unknown);
            } else if (mode != BlastMode.Unknown && blastModes.get(pid) != mode)
                mode = BlastMode.Unknown;
        }
        int reads = 0;
        for (int pid : pids) {
            reads += sampleSizes.get(pid);
        }

        sampleSizes.add(reads);
        sampleNames.add(newName);
        sampleUIds.add(System.currentTimeMillis());
        blastModes.add(mode);

        int tarId = Basic.getIndex(newName, sampleNames);
        for (Map<Integer, Integer[]> class2counts : classification2class2counts.values()) {
            for (Integer classId : class2counts.keySet()) {
                Integer[] counts = class2counts.get(classId);
                if (counts != null) {
                    int newLength = Math.max(counts.length + 1, tarId + 1);
                    Integer[] newCounts = new Integer[newLength];
                    System.arraycopy(counts, 0, newCounts, 0, counts.length);
                    int sum = 0;
                    for (int pid : pids) {
                        if (pid < counts.length && counts[pid] != null)
                            sum += counts[pid];
                    }
                    newCounts[tarId] = sum;
                    class2counts.put(classId, newCounts);
                }
            }
        }
        for (Integer size : sampleSizes) {
            if (size != null)
                totalReads += size;
        }
    }

    /**
     * reorder samples
     *
     * @param newOrder
     * @throws IOException
     */
    public void reorderSamples(Collection<String> newOrder) throws IOException {
        final Integer[] order = new Integer[newOrder.size()];

        int i = 0;
        for (String sample : newOrder) {
            int pid = Basic.getIndex(sample, getSampleNames());
            if (pid == -1)
                throw new IOException("Can't reorder: unknown sample: " + sample);
            order[i++] = pid;
        }

        final String[] datasetNames = modify(order, getSampleNames());
        final Long[] uids = modify(order, getSampleUIds());
        final Integer[] sizes = modify(order, getSampleSizes());
        final BlastMode[] modes = modify(order, getBlastModes());
        setSamples(datasetNames, uids, sizes,modes);

        final Map<String, Map<Integer, Integer[]>> classification2Class2Counts = getClassification2Class2Counts();
        for (String classification : classification2Class2Counts.keySet()) {
            final Map<Integer, Integer[]> class2Counts = classification2Class2Counts.get(classification);
            final Set<Integer> keys = new HashSet<>();
            keys.addAll(class2Counts.keySet());
            for (Integer classId : keys) {
                Integer[] values = class2Counts.get(classId);
                if (values != null) {
                    values = modify(order, values);
                    class2Counts.put(classId, values);
                }
            }
        }
    }

    /**
     * modify an array according to the given order
     *
     * @param order
     * @param array
     * @return modified array, possibly with changed length
     */
    private static String[] modify(Integer[] order, String[] array) {
        String[] tmp = new String[order.length];

        int pos = 0;
        for (Integer id : order) {
            if (id < array.length)
                tmp[pos++] = array[id];
        }
        return tmp;
    }

    /**
     * modify an array according to the given order
     *
     * @param order
     * @param array
     * @return modified array, possibly with changed length
     */
    private static Integer[] modify(Integer[] order, Integer[] array) {
        Integer[] tmp = new Integer[order.length];

        int pos = 0;
        for (Integer id : order) {
            if (id < array.length)
                tmp[pos++] = array[id];
        }
        return tmp;
    }

    /**
     * modify an array according to the given order
     *
     * @param order
     * @param array
     * @return modified array, possibly with changed length
     */
    private static Long[] modify(Integer[] order, Long[] array) {
        Long[] tmp = new Long[order.length];

        int pos = 0;
        for (Integer id : order) {
            if (id < array.length)
                tmp[pos++] = array[id];
        }
        return tmp;
    }

    /**
     * modify an array according to the given order
     *
     * @param order
     * @param array
     * @return modified array, possibly with changed length
     */
    private static BlastMode[] modify(Integer[] order, BlastMode[] array) {
        BlastMode[] tmp = new BlastMode[order.length];

        int pos = 0;
        for (Integer id : order) {
            if (id < array.length)
                tmp[pos++] = array[id];
        }
        return tmp;
    }

    /**
     * disable some of the samples
     *
     * @param sampleNames
     */
    public void disableSamples(Collection<String> sampleNames) {
        int size = disabledSamples.size();
        Set<String> newDisabled = new HashSet<>();
        newDisabled.addAll(disabledSamples);
        newDisabled.addAll(sampleNames);
        if (newDisabled.size() != size) {
            if (originalData == null) {
                originalData = new DataTable();
                originalData.copy(this);
            }
            copyEnabled(newDisabled, originalData);
            disabledSamples.clear();
            disabledSamples.addAll(newDisabled);
        }
    }

    /**
     * enable the given set of samples
     *
     * @param names
     * @return true, if changed
     */
    public boolean setEnabledSamples(Collection<String> names) {
        Set<String> oldDisabled = new HashSet<>();
        oldDisabled.addAll(disabledSamples);

        Set<String> newDisabled = new HashSet<>();

        if (originalData != null)
            newDisabled.addAll(originalData.sampleNames);
        else
            newDisabled.addAll(sampleNames);
        newDisabled.removeAll(names);
        if (originalData == null) {
            originalData = new DataTable();
            originalData.copy(this);
        }
        copyEnabled(newDisabled, originalData);
        return !disabledSamples.equals(oldDisabled);
    }

    /**
     * enable a set of samples
     *
     * @param sampleNames
     */
    public void enableSamples(Collection<String> sampleNames) {
        int size = disabledSamples.size();
        disabledSamples.removeAll(sampleNames);
        if (size != disabledSamples.size()) {
            if (originalData == null) {
                originalData = new DataTable();
                originalData.copy(this);
            }
            Set<String> newDisabled = new HashSet<>();
            newDisabled.addAll(disabledSamples);
            copyEnabled(newDisabled, originalData);
        }
    }

    /**
     * copy the enabled samples from the original data
     *
     * @param disabledSamples
     * @param originalData
     */
    public void copyEnabled(Set<String> disabledSamples, DataTable originalData) {
        clear();

        String[] origSampleNames = originalData.getSampleNames();
        BitSet activeIndices = new BitSet();
        for (int i = 0; i < origSampleNames.length; i++) {
            if (!disabledSamples.contains(origSampleNames[i])) {
                activeIndices.set(i);
            }
        }

        Long[] origSampleUIds = originalData.getSampleUIds();
        Integer[] origSampleSizes = originalData.getSampleSizes();
        BlastMode[] origBlastModes = originalData.getBlastModes();

        sampleNames.clear();
        int totalReads = 0;
        for (int origIndex = 0; origIndex < origSampleNames.length; origIndex++) {
            if (activeIndices.get(origIndex)) {
                sampleNames.addElement(origSampleNames[origIndex]);
                if (origSampleUIds != null && origSampleUIds.length > origIndex)
                    sampleUIds.addElement(origSampleUIds[origIndex]);
                if (origBlastModes != null && origBlastModes.length > origIndex)
                    blastModes.addElement(origBlastModes[origIndex]);
                sampleSizes.addElement(origSampleSizes[origIndex]);
                if (origSampleSizes[origIndex] != null)
                    totalReads += origSampleSizes[origIndex];
            }
        }
        setTotalReads(totalReads);

        // write the data:
        for (String classification : originalData.classification2class2counts.keySet()) {
            Map<Integer, Integer[]> origClass2counts = originalData.classification2class2counts.get(classification);
            Map<Integer, Integer[]> class2counts = classification2class2counts.get(classification);
            if (class2counts == null) {
                class2counts = new HashMap<>();
                classification2class2counts.put(classification, class2counts);
            }

            for (Integer classId : origClass2counts.keySet()) {
                Integer[] origCounts = origClass2counts.get(classId);
                Integer[] counts = new Integer[activeIndices.cardinality()];
                int index = 0;
                for (int origIndex = 0; origIndex < origCounts.length; origIndex++) {
                    if (activeIndices.get(origIndex)) {
                        counts[index++] = origCounts[origIndex];
                    }
                }
                class2counts.put(classId, counts);
            }
        }
        this.disabledSamples.addAll(disabledSamples);
    }

    public Set<String> getDisabledSamples() {
        Set<String> result = new HashSet<>();
        result.addAll(disabledSamples);
        return result;
    }

    public int getNumberOfSamples() {
        return sampleNames.size();
    }

    public String[] getOriginalSamples() {
        if (disabledSamples.size() > 0 && originalData != null)
            return originalData.getSampleNames();
        else
            return getSampleNames();
    }

    /**
     * gets the indices of the given samples
     *
     * @param samples
     * @return ids
     */
    public BitSet getSampleIds(Collection<String> samples) {
        BitSet sampleIds = new BitSet();
        String[] sampleNames = getSampleNames();
        for (int i = 0; i < sampleNames.length; i++) {
            if (samples.contains(sampleNames[i]))
                sampleIds.set(i);
        }
        return sampleIds;
    }
}
