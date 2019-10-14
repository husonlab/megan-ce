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

import jloda.util.Basic;
import jloda.util.BlastMode;
import jloda.util.ProgramProperties;

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
    private final static String CONTENT_TYPE = "@ContentType";
    private final static String CREATION_DATE = "@CreationDate";
    private final static String CREATOR = "@Creator";
    private final static String NAMES = "@Names";
    private final static String BLAST_MODE = "@BlastMode";
    private final static String DISABLED = "@Disabled";
    private final static String NODE_FORMATS = "@NodeFormats";
    private final static String EDGE_FORMATS = "@EdgeFormats";
    private static final String ALGORITHM = "@Algorithm";
    private static final String NODE_STYLE = "@NodeStyle";
    private static final String COLOR_TABLE = "@ColorTable";
    private static final String COLOR_EDITS = "@ColorEdits";

    private static final String PARAMETERS = "@Parameters";
    private static final String CONTAMINANTS = "@Contaminants";

    private static final String TOTAL_READS = "@TotalReads";
    private static final String ADDITIONAL_READS = "@AdditionalReads";

    private static final String COLLAPSE = "@Collapse";
    private static final String SIZES = "@Sizes";
    private static final String UIDS = "@Uids";

    // variables:
    private String contentType = MEGAN4SummaryFormat;
    private String creator = ProgramProperties.getProgramName();
    private String creationDate = null;

    private long totalReads = -1;
    private long additionalReads = -1;
    private double totalReadWeights = -1;

    private final Vector<String> sampleNames = new Vector<>();
    private final Vector<BlastMode> blastModes = new Vector<>();
    private final Vector<Float> sampleSizes = new Vector<>();
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
    private String contaminants;

    private final Map<String, Map<Integer, float[]>> classification2class2counts = new HashMap<>();

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
        totalReads = -1;
        additionalReads = -1;
        totalReadWeights = -1;
        classification2collapsedIds.clear();
        classification2NodeStyle.clear();
        classification2algorithm.clear();
        classification2NodeFormats.clear();
        classification2EdgeFormats.clear();
        parameters = null;
        classification2class2counts.clear();
        // don't clear contaminants
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
                                sampleSizes.add(Basic.parseFloat(tokens[i]));
                            break;
                        case TOTAL_READS:
                            totalReads = (Long.parseLong(tokens[1]));
                            break;
                        case ADDITIONAL_READS:
                            additionalReads = (Long.parseLong(tokens[1]));
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
                        case PARAMETERS: {
                            if (tokens.length > 1) {
                                StringBuilder buf = new StringBuilder();
                                for (int i = 1; i < tokens.length; i++)
                                    buf.append(" ").append(tokens[i]);
                                parameters = buf.toString().trim();
                            }
                            break;
                        }
                        case CONTAMINANTS: {
                            if (tokens.length > 1) {
                                StringBuilder buf = new StringBuilder();
                                for (int i = 1; i < tokens.length; i++)
                                    buf.append(" ").append(tokens[i]);
                                setContaminants(buf.toString().trim());
                            }
                            break;
                        }
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

                        Map<Integer, float[]> class2counts = classification2class2counts.computeIfAbsent(classification, k -> new HashMap<>());
                        float[] counts = class2counts.get(classId);
                        if (counts == null) {
                            counts = new float[Math.min(getNumberOfSamples(), tokens.length - 2)];
                            class2counts.put(classId, counts);
                        }
                        for (int i = 2; i < Math.min(tokens.length, counts.length + 2); i++) {
                            counts[i - 2] = Float.parseFloat(tokens[i]);
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
                Map<Integer, float[]> class2counts = classification2class2counts.get(classification);
                classification = ClassificationType.getShortName(classification);
                for (Integer classId : class2counts.keySet()) {
                    float[] counts = class2counts.get(classId);
                    if (counts != null) {
                        w.write(classification + "\t" + classId);
                        int last = Math.min(counts.length, getNumberOfSamples()) - 1;
                        while (last > 0 && counts[last] == 0)
                            last--;

                        for (int i = 0; i <= last; i++) {
                            if (i < counts.length)
                                w.write("\t" + counts[i]);
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
    private void writeHeader(Writer w) throws IOException {
        w.write(String.format("%s\t%s\n", CREATOR, (creator != null ? creator : ProgramProperties.getProgramName())));
        w.write(String.format("%s\t%s\n", CREATION_DATE, (creationDate != null ? creationDate : ((new Date()).toString()))));
        w.write(String.format("%s\t%s\n", CONTENT_TYPE, getContentType()));
        w.write(String.format("%s\t%s\n", NAMES, Basic.toString(sampleNames, "\t")));
        w.write(String.format("%s\t%s\n", BLAST_MODE, Basic.toString(blastModes, "\t")));
        if (disabledSamples.size() > 0) {
            w.write(String.format("%s\t%s\n", DISABLED, Basic.toString(disabledSamples, "\t")));
        }
        if (sampleUIds.size() > 0) {
            w.write(String.format("%s\t%s\n", UIDS, Basic.toString(sampleUIds, "\t")));
        }
        if (sampleSizes.size() > 0) {
            w.write(String.format("%s\t%s\n", SIZES, Basic.toString(sampleSizes, "\t")));
        }

        if (totalReads != -1)
            w.write(String.format("%s\t%d\n", TOTAL_READS, totalReads));

        if (additionalReads != -1)
            w.write(String.format("%s\t%d\n", ADDITIONAL_READS, additionalReads));

        for (String classification : classification2collapsedIds.keySet()) {
            Set<Integer> collapsed = classification2collapsedIds.get(classification);
            if (collapsed != null && collapsed.size() > 0) {
                w.write(String.format("%s\t%s\t%s\n", COLLAPSE, classification, Basic.toString(collapsed, "\t")));
            }
        }

        for (String classification : classification2algorithm.keySet()) {
            String algorithm = classification2algorithm.get(classification);
            if (algorithm != null) {
                w.write(String.format("%s\t%s\t%s\n", ALGORITHM, classification, algorithm));
            }
        }

        if (parameters != null)
            w.write(String.format("%s\t%s\n", PARAMETERS, parameters));
        if (hasContaminants())
            w.write(String.format("%s\t%s\n", CONTAMINANTS, getContaminants()));


        for (String classification : classification2NodeStyle.keySet()) {
            String nodeType = classification2NodeStyle.get(classification);
            if (nodeType != null) {
                w.write(String.format("%s\t%s\t%s\n", NODE_STYLE, classification, nodeType));
            }
        }
        if (colorTable != null)
            w.write(String.format("%s\t%s%s\t%s\n", COLOR_TABLE, colorTable, (colorByPosition ? "\tbyPosition" : ""), getColorTableHeatMap()));
        if (colorEdits != null)
            w.write(String.format("%s\t%s\n", COLOR_EDITS, colorEdits));

        for (String classification : classification2NodeFormats.keySet()) {
            String formatting = classification2NodeFormats.get(classification);
            if (formatting != null) {
                w.write(String.format("%s\t%s\t%s\n", NODE_FORMATS, classification, formatting));
            }
        }

        for (String classification : classification2EdgeFormats.keySet()) {
            String formatting = classification2EdgeFormats.get(classification);
            if (formatting != null)
                w.write(String.format("%s\t%s\t%s\n", EDGE_FORMATS, classification, formatting));
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
            for (float dataSize : sampleSizes) w.write(" " + dataSize);
            w.write("<br>");
        }

        if (totalReads != -1)
            w.write(String.format("<b>%s:</b> %d<br>\n", TOTAL_READS.substring(1), totalReads));
        if (additionalReads != -1)
            w.write(String.format("<b>%s:</b> %d<br>\n", ADDITIONAL_READS.substring(1), additionalReads));

        w.write("<b>Classifications:</b> ");
        for (String classification : classification2class2counts.keySet()) {
            Map<Integer, float[]> class2counts = classification2class2counts.get(classification);
            int size = class2counts != null ? class2counts.size() : 0;
            w.write(String.format("%s (%d classes)", classification, size));
        }
        w.write("<br>\n");

        for (String classification : classification2algorithm.keySet()) {
            String algorithm = classification2algorithm.get(classification);
            if (algorithm != null) {
                w.write(String.format("<b>%s:</b> %s: %s<br>\n", ALGORITHM.substring(1), classification, algorithm));
            }
        }

        if (parameters != null)
            w.write(String.format("<b>%s:</b> %s<br>\n", PARAMETERS.substring(1), parameters));
        if (hasContaminants())
            w.write(String.format("<b>%s:</b> %d taxa<br>\n", CONTAMINANTS.substring(1), Basic.countWords(getContaminants())));
        if (colorTable != null)
            w.write(String.format("<b>ColorTable:</b> %s%s <b>HeatMapColorTable:</b> %s<br>\n", colorTable, (colorByPosition ? " byPosition" : ""), colorTableHeatMap));

        if (colorEdits != null)
            w.write(String.format("<b>ColorEdits:</b> %s<br>\n", Basic.abbreviateDotDotDot(colorEdits, 50)));

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

        w.write(String.format("%s\t%s\n", CREATOR, (creator != null ? creator : ProgramProperties.getProgramName())));
        w.write(String.format("%s\t%s\n", CREATION_DATE, (creationDate != null ? creationDate : ((new Date()).toString()))));
        w.write(String.format("%s\t%s\n", CONTENT_TYPE, getContentType()));
        w.write(String.format("%s\t%s\n", NAMES, Basic.toString(sampleNames, "\t")));
        w.write(String.format("%s\t%s\n", BLAST_MODE, Basic.toString(blastModes, "\t")));

        if (sampleUIds.size() > 0) {
            w.write(String.format("%s\t%s\n", UIDS, Basic.toString(sampleUIds, "\t")));
        }
        if (sampleSizes.size() > 0) {
            w.write(String.format("%s\t%s\n", SIZES, Basic.toString(sampleSizes, "\t")));
        }

        if (totalReads != -1)
            w.write(String.format("%s\t%d\n", TOTAL_READS, totalReads));

        if (additionalReads != -1)
            w.write(String.format("%s\t%d\n", ADDITIONAL_READS, additionalReads));

        w.write("Classifications:\n");
        for (String classification : classification2class2counts.keySet()) {
            Map<Integer, float[]> class2counts = classification2class2counts.get(classification);
            int size = class2counts != null ? class2counts.size() : 0;
            w.write(" " + classification + " (" + size + " classes)");
        }
        w.write("\n");


        for (String classification : classification2algorithm.keySet()) {
            String algorithm = classification2algorithm.get(classification);
            if (algorithm != null) {
                w.write(String.format("%s\t%s\t%s\n", ALGORITHM, classification, algorithm));
            }
        }

        if (parameters != null)
            w.write(String.format("%s\t%s\n", PARAMETERS, parameters));

        if (hasContaminants())
            w.write(String.format("%s\t%s\n", CONTAMINANTS, getContaminants()));

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
        try (r) {
            String aLine;
            sampleNames.clear();
            sampleNames.add(Basic.getFileBaseName(fileName));
            blastModes.clear();
            contaminants = null;
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
                            totalReadWeights = totalReads;
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
        }
        return lineNumber;
    }

    /**
     * determine the size of datasets from the taxonomy classification
     */
    private void determineSizesFromTaxonomyClassification() {
        // determine sizes:
        Map<Integer, float[]> class2count = classification2class2counts.get(ClassificationType.Taxonomy.toString());
        if (class2count != null) {
            float[] sizes = new float[getNumberOfSamples()];
            for (Integer classId : class2count.keySet()) {
                float[] counts = class2count.get(classId);
                if (counts != null) {
                    for (int i = 0; i < getNumberOfSamples(); i++) {
                        if (counts[i] > 0)
                            sizes[i] += counts[i];
                    }
                }
            }
            sampleSizes.clear();
            for (float size : sizes)
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
    public Map<String, Map<Integer, float[]>> getClassification2Class2Counts() {
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
    public void setClassification2Class2Count(String classification, int classId, int sampleId, float count) {
        Map<Integer, float[]> class2count = classification2class2counts.get(classification);
        if (class2count == null)
            class2count = new HashMap<>();
        classification2class2counts.put(classification, class2count);
        float[] counts = class2count.get(classId);
        if (counts == null) {
            counts = new float[getNumberOfSamples()];
            class2count.put(classId, counts);
        }
        counts[sampleId] = count;
    }

    /**
     * gets all non-disabled sample names
     *
     * @return sample names
     */
    public String[] getSampleNamesArray() {
        return sampleNames.toArray(new String[0]);
    }

    public Collection<String> getSampleNames() {
        return new ArrayList<>(sampleNames);
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
    public void setSamples(String[] names, Long[] uids, float[] sizes, BlastMode[] modes) {
        sampleNames.clear();
        if (names != null)
            sampleNames.addAll(Arrays.asList(names));
        sampleUIds.clear();
        if (uids != null)
            sampleUIds.addAll(Arrays.asList(uids));
        sampleSizes.clear();
        if (sizes != null) {
            for (float size : sizes)
                sampleSizes.add(size);
        }
        blastModes.clear();
        if (modes != null) {
            if (modes.length == 1) { // if there is 1 or more samples and only one mode given, apply to all
                for (String name : sampleNames)
                    blastModes.add(modes[0]);
            } else
                blastModes.addAll(Arrays.asList(modes));
        } else {
            for (int i = 0; i < getNumberOfSamples(); i++)
                blastModes.add(BlastMode.Unknown);
        }
    }

    public float[] getSampleSizes() {
        final float[] sizes = new float[sampleSizes.size()];
        for (int i = 0; i < sizes.length; i++)
            sizes[i] = sampleSizes.get(i);
        return sizes;
    }

    public Long[] getSampleUIds() {
        return sampleUIds.toArray(new Long[0]);
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
     * get the contaminants string
     *
     * @return string or null
     */
    public String getContaminants() {
        return contaminants;
    }

    /**
     * set the contaminants string or null
     *
     * @param contaminants
     */
    public void setContaminants(String contaminants) {
        this.contaminants = contaminants;
    }

    public boolean hasContaminants() {
        return contaminants != null && contaminants.length() > 0;
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
    private String getContentType() {
        return contentType;
    }

    /**
     * sets the content type
     *
     * @param contentType
     */
    private void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Map<Integer, float[]> getClass2Counts(ClassificationType classification) {
        return classification2class2counts.get(classification.toString());
    }

    public Map<Integer, float[]> getClass2Counts(String classification) {
        return classification2class2counts.get(classification);
    }

    public void setClass2Counts(String classification, Map<Integer, float[]> classId2count) {
        classification2class2counts.put(classification, classId2count);
    }

    public void setTotalReads(long totalReads) {
        this.totalReads = totalReads;
    }

    public long getTotalReads() {
        if (totalReads > 0)
            return totalReads;
        else
            return 0;
    }

    public void setTotalReadWeights(double totalReadWeights) {
        this.totalReadWeights = totalReadWeights;
    }

    public double getTotalReadWeights() {
        if (totalReadWeights > 0)
            return totalReadWeights;
        else
            return totalReads;
    }

    public void setAdditionalReads(long additionalReads) {
        this.additionalReads = additionalReads;
    }

    public long getAdditionalReads() {
        if (additionalReads > 0)
            return additionalReads;
        else
            return 0;
    }

    /**
     * copy the given Megan4Summary
     *
     * @param megan4Table
     */
    private void copy(DataTable megan4Table) {
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

    /**
     * removes samples
     *
     * @param toDelete
     */
    private void removeSamples(Collection<String> toDelete) {
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

        for (Map<Integer, float[]> class2counts : classification2class2counts.values()) {
            for (Integer classId : class2counts.keySet()) {
                float[] counts = class2counts.get(classId);
                if (counts != null) {
                    float[] newCounts = new float[alive];
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
        for (float size : sampleSizes) {
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

        if (!Arrays.asList(target.getSampleNamesArray()).contains(sample)) {
            int srcId = Basic.getIndex(sample, source.sampleNames);
            target.sampleSizes.add(source.sampleSizes.get(srcId));
            target.sampleNames.add(sample);
            target.sampleUIds.add(System.currentTimeMillis());
            if (srcId < blastModes.size())
                target.blastModes.add(blastModes.get(srcId));

            int tarId = Basic.getIndex(sample, target.sampleNames);

            for (String classification : source.classification2class2counts.keySet()) {
                Map<Integer, float[]> sourceClass2counts = source.classification2class2counts.get(classification);
                Map<Integer, float[]> targetClass2counts = target.classification2class2counts.computeIfAbsent(classification, k -> new HashMap<>());
                for (Integer classId : sourceClass2counts.keySet()) {
                    float[] sourceCounts = sourceClass2counts.get(classId);
                    if (sourceCounts != null && srcId < sourceCounts.length && sourceCounts[srcId] != 0) {
                        float[] targetCounts = targetClass2counts.get(classId);
                        float[] newCounts = new float[tarId + 1];
                        if (targetCounts != null) {
                            System.arraycopy(targetCounts, 0, newCounts, 0, targetCounts.length);
                        }
                        newCounts[tarId] = sourceCounts[srcId];
                        targetClass2counts.put(classId, newCounts);
                    }
                }
            }
            target.totalReads += source.sampleSizes.get(srcId);
        }
    }

    /**
     * add the named sample
     *
     * @param sample
     * @param sourceClassification2class2counts
     */
    public void addSample(String sample, float sampleSize, BlastMode mode, int srcId, Map<String, Map<Integer, float[]>> sourceClassification2class2counts) {
        if (!Arrays.asList(this.getSampleNamesArray()).contains(sample)) {
            this.sampleSizes.add(sampleSize);
            this.sampleNames.add(sample);
            this.sampleUIds.add(System.currentTimeMillis());
            this.blastModes.add(mode);

            int tarId = Basic.getIndex(sample, this.sampleNames);

            for (String classification : sourceClassification2class2counts.keySet()) {
                Map<Integer, float[]> sourceClass2counts = sourceClassification2class2counts.get(classification);
                Map<Integer, float[]> targetClass2counts = this.classification2class2counts.computeIfAbsent(classification, k -> new HashMap<>());
                for (int classId : sourceClass2counts.keySet()) {
                    float[] sourceCounts = sourceClass2counts.get(classId);
                    if (sourceCounts != null && srcId < sourceCounts.length && sourceCounts[srcId] != 0) {
                        float[] targetCounts = targetClass2counts.get(classId);
                        float[] newCounts = new float[tarId + 1];
                        if (targetCounts != null) {
                            System.arraycopy(targetCounts, 0, newCounts, 0, targetCounts.length);
                        }
                        newCounts[tarId] = sourceCounts[srcId];
                        targetClass2counts.put(classId, newCounts);
                    }
                }
            }
            this.totalReads += sampleSize;
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
        Set<String> toDelete = new HashSet<>(sampleNames);
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
        BlastMode mode = null;
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
        float reads = 0;
        for (int pid : pids) {
            reads += sampleSizes.get(pid);
        }

        sampleSizes.add(reads);
        sampleNames.add(newName);
        sampleUIds.add(System.currentTimeMillis());
        blastModes.add(mode);

        int tarId = Basic.getIndex(newName, sampleNames);
        for (Map<Integer, float[]> class2counts : classification2class2counts.values()) {
            for (Integer classId : class2counts.keySet()) {
                float[] counts = class2counts.get(classId);
                if (counts != null) {
                    int newLength = Math.max(counts.length + 1, tarId + 1);
                    float[] newCounts = new float[newLength];
                    System.arraycopy(counts, 0, newCounts, 0, counts.length);
                    int sum = 0;
                    for (int pid : pids) {
                        if (pid < counts.length && counts[pid] != 0)
                            sum += counts[pid];
                    }
                    newCounts[tarId] = sum;
                    class2counts.put(classId, newCounts);
                }
            }
        }
        for (float size : sampleSizes) {
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
            int pid = Basic.getIndex(sample, getSampleNamesArray());
            if (pid == -1)
                throw new IOException("Can't reorder: unknown sample: " + sample);
            order[i++] = pid;
        }

        final String[] datasetNames = modify(order, getSampleNamesArray());
        final Long[] uids = modify(order, getSampleUIds());
        final float[] sizes = modify(order, getSampleSizes());
        final BlastMode[] modes = modify(order, getBlastModes());
        setSamples(datasetNames, uids, sizes, modes);

        final Map<String, Map<Integer, float[]>> classification2Class2Counts = getClassification2Class2Counts();
        for (String classification : classification2Class2Counts.keySet()) {
            final Map<Integer, float[]> class2Counts = classification2Class2Counts.get(classification);
            final Set<Integer> keys = new HashSet<>(class2Counts.keySet());
            for (Integer classId : keys) {
                float[] values = class2Counts.get(classId);
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
    private static float[] modify(Integer[] order, float[] array) {
        float[] tmp = new float[order.length];
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
    private void disableSamples(Collection<String> sampleNames) {
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
        Set<String> oldDisabled = new HashSet<>(disabledSamples);

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
            Set<String> newDisabled = new HashSet<>(disabledSamples);
            copyEnabled(newDisabled, originalData);
        }
    }

    /**
     * copy the enabled samples from the original data
     *
     * @param disabledSamples
     * @param originalData
     */
    private void copyEnabled(Set<String> disabledSamples, DataTable originalData) {
        clear();

        String[] origSampleNames = originalData.getSampleNamesArray();
        BitSet activeIndices = new BitSet();
        for (int i = 0; i < origSampleNames.length; i++) {
            if (!disabledSamples.contains(origSampleNames[i])) {
                activeIndices.set(i);
            }
        }

        Long[] origSampleUIds = originalData.getSampleUIds();
        float[] origSampleSizes = originalData.getSampleSizes();
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
                totalReads += origSampleSizes[origIndex];
            }
        }
        setTotalReads(totalReads);

        // write the data:
        for (String classification : originalData.classification2class2counts.keySet()) {
            Map<Integer, float[]> origClass2counts = originalData.classification2class2counts.get(classification);
            Map<Integer, float[]> class2counts = classification2class2counts.computeIfAbsent(classification, k -> new HashMap<>());

            for (Integer classId : origClass2counts.keySet()) {
                float[] origCounts = origClass2counts.get(classId);
                float[] counts = new float[activeIndices.cardinality()];
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
        Set<String> result = new HashSet<>(disabledSamples);
        return result;
    }

    public int getNumberOfSamples() {
        return sampleNames.size();
    }

    public String[] getOriginalSamples() {
        if (disabledSamples.size() > 0 && originalData != null)
            return originalData.getSampleNamesArray();
        else
            return getSampleNamesArray();
    }

    /**
     * gets the indices of the given samples
     *
     * @param samples
     * @return ids
     */
    public BitSet getSampleIds(Collection<String> samples) {
        BitSet sampleIds = new BitSet();
        String[] sampleNames = getSampleNamesArray();
        for (int i = 0; i < sampleNames.length; i++) {
            if (samples.contains(sampleNames[i]))
                sampleIds.set(i);
        }
        return sampleIds;
    }

}
