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
package megan.parsers;

import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.FileLineIterator;
import jloda.util.ProgramProperties;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.classification.IdParser;
import megan.core.ClassificationType;
import megan.core.DataTable;
import megan.core.Document;
import megan.core.SampleAttributeTable;
import megan.viewer.MainViewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * parses a CVS file containing a summary of one or multiple taxonomic dataset
 * Daniel Huson, 9.2010
 */
public class CSVSummaryParser {

    /**
     * apply the importer parser to the named file.
     * Format should be:   taxid,count[,count,...]
     * If the first line contains non-number tokens, then these are interpreted as the names of the datasets
     *
     * @param fileName
     * @param doc
     * @param multiplier
     */
    static public void apply(String fileName, Document doc, String[] cNames, boolean tabSeparator, long multiplier) throws IOException {
        String separator = (tabSeparator ? "\t" : ",");
        System.err.println("Importing summary of " + Basic.toString(cNames, ", ") + " assignments from CSV file");
        System.err.println("Specified line format: classname" + separator + "count{" + separator + "count" + separator + "count...}");

        DataTable table = doc.getDataTable();
        table.clear();
        table.setCreator(ProgramProperties.getProgramName());
        table.setCreationDate((new Date()).toString());
        table.setAlgorithm(ClassificationType.Taxonomy.toString(), "Summary");

        doc.getActiveViewers().clear();
        doc.getActiveViewers().addAll(Arrays.asList(cNames));

        final Set<Integer>[] knownIds = new HashSet[cNames.length];
        for (int i = 0; i < cNames.length; i++) {
            knownIds[i] = new HashSet<>();
            knownIds[i].addAll(ClassificationManager.get(cNames[i], true).getName2IdMap().getIds());
        }

        IdParser[] idParsers = new IdParser[cNames.length];
        int taxonomyIndex = -1;
        for (int i = 0; i < cNames.length; i++) {
            String cName = cNames[i];
            idParsers[i] = ClassificationManager.get(cName, true).getIdMapper().createIdParser();
            if (!cName.equals(Classification.Taxonomy)) {
                ClassificationManager.ensureTreeIsLoaded(cName);
                doc.getActiveViewers().add(cName);
            } else {
                taxonomyIndex = i;
            }
            idParsers[i].setUseTextParsing(true);
        }

        String[] names = null;

        final Map<Integer, float[]>[] class2counts = new HashMap[cNames.length];
        Arrays.fill(class2counts, new HashMap<>());

        float[][] total = new float[cNames.length][];

        int[] add = null;

        int numberOfColumns = -1;

        int numberOfErrors = 0;
        boolean first = true;

        int warningUnrecognizedName = 0;
        int[] warnings = new int[cNames.length];

        int numberOfLines = 0;
        try (FileLineIterator it = new FileLineIterator(fileName)) {
            while (it.hasNext()) {
                numberOfLines++;
                String aLine = it.next().trim();
                if (aLine.length() == 0 || (!first && aLine.startsWith("#")))
                    continue;
                try {
                    String[] tokens = aLine.split(separator);
                    if (numberOfColumns == -1) {
                        numberOfColumns = tokens.length;
                    } else if (tokens.length != numberOfColumns)
                        throw new IOException("Line " + it.getLineNumber() + ": incorrect number of columns, expected " + numberOfColumns + ", got: " + tokens.length + " (" + aLine + ")");

                    if (first) {
                        if (tokens.length < 2)
                            throw new IOException("Line " + it.getLineNumber() + ": incorrect number of columns, expected at least 2, got: " + tokens.length + " (" + aLine + ")");

                        boolean headerLinePresent = (Basic.getIndexIgnoreCase(tokens[0], "name", "names", "samples", "SampleId", SampleAttributeTable.SAMPLE_ID, "Dataset", "Datasets") != -1);
                        if (!headerLinePresent) { // check other tokens: unless all are numbers, assume the first line is header line
                            for (int i = 1; i < tokens.length; i++) {
                                if (!Basic.isFloat(tokens[i])) {
                                    headerLinePresent = true;
                                    break;
                                }
                            }
                        }

                        if (!headerLinePresent && tokens[0].startsWith("#")) {
                            System.err.println("Skipping comment line: " + Basic.abbreviateDotDotDot(aLine, 80));
                            continue;
                        }
                        first = false;

                        if (headerLinePresent) {
                            names = new String[tokens.length - 1];
                            System.arraycopy(tokens, 1, names, 0, names.length);
                            table.setSamples(names, null, null, null);
                        } else if (tokens.length == 2) {
                            names = new String[]{Basic.getFileBaseName((new File(fileName)).getName())};
                        } else {
                            names = new String[tokens.length - 1];
                            for (int i = 0; i < names.length; i++)
                                names[i] = "Sample" + (i + 1);
                        }

                        Arrays.fill(total, new float[names.length]);
                        if (headerLinePresent)
                            continue; // don't try to parse numbers from header line
                    }
                    if (add == null)
                        add = new int[names.length];
                    for (int i = 1; i < tokens.length; i++) {
                        String number = tokens[i].trim();
                        if (number.length() == 0)
                            add[i - 1] = 0;
                        else if (Basic.isInteger(number))
                            add[i - 1] = (int) (multiplier * Integer.parseInt(number));
                        else
                            add[i - 1] = (int) (multiplier * Double.parseDouble(number));
                    }

                    boolean found = false;
                    for (int i = 0; i < idParsers.length; i++) {
                        int id;
                        if (i == taxonomyIndex && Basic.isInteger(tokens[0]))
                            id = Basic.parseInt(tokens[0]);
                        else
                            id = idParsers[i].getIdFromHeaderLine(tokens[0]);
                        if (id != 0) {
                            found = true;
                            if (knownIds[i].contains(id)) {
                                float[] counts = getOrCreate(class2counts[i], id, names.length);
                                addToArray(counts, add);
                                addToArray(total[i], add);

                                /*
                                if(id>0) {
                                    System.err.println(tokens[0]+" -> "+id);
                                }
                                */
                            } else {
                                if (warnings[i] < 50) {
                                    System.err.println("Warning: " + cNames[i] + " Unclassified item: " + tokens[0]);
                                    warnings[i]++;
                                    if (warnings[i] == 50)
                                        System.err.println("No further warnings");
                                }
                            }
                        }
                    }
                    if (!found) {
                        if (warningUnrecognizedName < 50) {
                            System.err.println("Unrecognized name: " + tokens[0]);
                            warningUnrecognizedName++;
                            if (warningUnrecognizedName == 50)
                                System.err.println("No further warnings");
                        }
                        for (int i = 0; i < idParsers.length; i++) {
                            float[] counts = getOrCreate(class2counts[i], IdMapper.UNASSIGNED_ID, names.length);
                            addToArray(counts, add);
                            addToArray(total[i], add);
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Error: " + ex + ", skipping");
                    numberOfErrors++;
                }
            }
        }

        float[] sizes = new float[Objects.requireNonNull(names).length];
        if (taxonomyIndex == -1) {
            System.arraycopy(total[0], 0, sizes, 0, sizes.length);
            final Map<Integer, float[]> unassigned = new HashMap<>();
            unassigned.put(IdMapper.UNASSIGNED_ID, sizes);
            doc.getActiveViewers().add(Classification.Taxonomy);
            table.getClassification2Class2Counts().put(Classification.Taxonomy, unassigned);
        } else
            System.arraycopy(total[taxonomyIndex], 0, sizes, 0, sizes.length);

        table.setSamples(names, null, sizes, null);
        for (int i = 0; i < cNames.length; i++) {
            table.getClassification2Class2Counts().put(cNames[i], class2counts[i]);
        }
        doc.getSampleAttributeTable().setSampleOrder(Arrays.asList(names));

        long totalReads = 0;
        for (float size : sizes) {
            totalReads += size;
        }

        table.setTotalReads(totalReads);
        doc.setNumberReads(totalReads);
        System.err.println("Number of lines read: " + numberOfLines);
        if (numberOfErrors > 0)
            NotificationsInSwing.showWarning(MainViewer.getLastActiveFrame(), "Number of lines skipped during import: " + numberOfErrors);

        for (int i = 0; i < cNames.length; i++) {
            System.err.println("Different " + (cNames[i].length() <= 4 ? cNames[i] : cNames[i].substring(0, 3) + ".") + " classes identified: " + class2counts[i].size());
        }
        System.err.println("done (" + totalReads + " reads)");
    }

    /**
     * get the entry, if it exists, otherwise create it and initialize to zeros
     *
     * @param map
     * @param id
     * @param size
     * @return entry
     */
    private static float[] getOrCreate(Map<Integer, float[]> map, Integer id, int size) {
        float[] result = map.computeIfAbsent(id, k -> new float[size]);
        return result;
    }

    /**
     * add all values to sum
     *
     * @param sum
     * @param add
     */
    private static void addToArray(float[] sum, int[] add) {
        for (int i = 0; i < add.length; i++) {
            sum[i] += add[i];
        }
    }

    /**
     * get the number of tokens per line in the file
     *
     * @param file
     * @return tokens per line or 0
     */
    public static int getTokensPerLine(File file, String separator) {
        int result = 0;
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(file));
            String aLine = r.readLine();
            while (aLine != null && (aLine.trim().length() == 0 || aLine.trim().startsWith("#")))
                aLine = r.readLine().trim();
            if (aLine == null)
                return 0;
            else
                result = aLine.split(separator).length;
        } catch (Exception ignored) {
        } finally {
            if (r != null)
                try {
                    r.close();
                } catch (IOException ignored) {
                }
        }
        return result;
    }

    /**
     * guess whether the given file uses tab as the separator
     *
     * @param file
     * @return true, if first line contains tabs
     */
    public static boolean guessTabSeparator(File file) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(file));
            String aLine = r.readLine();
            while (aLine != null && (aLine.trim().length() == 0 || aLine.trim().startsWith("#")))
                aLine = r.readLine().trim();
            if (aLine != null)
                return aLine.contains("\t");
        } catch (Exception ignored) {
        } finally {
            if (r != null)
                try {
                    r.close();
                } catch (IOException ignored) {
                }
        }
        return false;
    }
}
