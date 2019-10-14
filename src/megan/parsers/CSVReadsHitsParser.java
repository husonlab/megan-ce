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
import jloda.util.*;
import megan.algorithms.MinSupportFilter;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.classification.IdParser;
import megan.core.ClassificationType;
import megan.core.DataTable;
import megan.core.Document;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * parses a CVS file containing a list of reads and hits
 * Daniel Huson, 9.2010
 */
public class CSVReadsHitsParser {

    /**
     * apply the importer parser to the named file.
     * Format should be:   readname,taxon,score
     *
     * @param fileName
     * @param doc
     */
    static public void apply(String fileName, Document doc, String[] cNames, boolean tabSeparator) throws IOException, CanceledException {
        final char separator = (tabSeparator ? '\t' : ',');

        System.err.println("Importing list of read to CLASS-id hits from CSV file");
        System.err.println("Line format: readname,CLASS-id,score     - for one of the following classificiatons: " + Basic.toString(cNames, " name,"));

        System.err.println("Using topPercent=" + doc.getTopPercent() + " minScore=" + doc.getMinScore() +
                (doc.getMinSupportPercent() > 0 ? " minSupportPercent=" + doc.getMinSupportPercent() : "") +
                " minSupport=" + doc.getMinSupport());

        final DataTable table = doc.getDataTable();
        table.clear();
        table.setCreator(ProgramProperties.getProgramName());
        table.setCreationDate((new Date()).toString());
        table.setAlgorithm(ClassificationType.Taxonomy.toString(), "Summary");

        doc.getActiveViewers().clear();
        doc.getActiveViewers().addAll(Arrays.asList(cNames));

        final IdParser[] parsers = new IdParser[cNames.length];
        int taxonomyIndex = -1;
        for (int i = 0; i < cNames.length; i++) {
            final String cName = cNames[i];
            parsers[i] = ClassificationManager.get(cName, true).getIdMapper().createIdParser();
            ClassificationManager.ensureTreeIsLoaded(cName);
            if (!cName.equals(Classification.Taxonomy)) {
                doc.getActiveViewers().add(cName);
            } else {
                taxonomyIndex = i;
            }
            parsers[i].setUseTextParsing(true);
        }

        final Map<String, List<Pair<Integer, Float>>>[] readName2IdAndScore = new HashMap[cNames.length];
        Arrays.fill(readName2IdAndScore, new HashMap<>());

        final int[] count = new int[parsers.length];
        int numberOfErrors = 0;
        int numberOfLines = 0;

        final ProgressListener progress = doc.getProgressListener();
        progress.setTasks("Importing CSV file", "Reading " + fileName);

        int countInputReadNames = 0;
        int countOutputReadNames = 0;
        int countClassNames = 0;
        int countUnrecognizedClassNames = 0;

        try (FileLineIterator it = new FileLineIterator(fileName)) {
            progress.setMaximum(it.getMaximumProgress());
            progress.setProgress(0);

            boolean warnedNoScoreGiven = false;

            String prevName = "";

            while (it.hasNext()) {
                numberOfLines++;
                final String aLine = it.next().trim();
                if (aLine.length() == 0 || aLine.startsWith("#"))
                    continue;
                try {
                    final String[] tokens = Basic.split(aLine, separator);

                    if (tokens.length < 2 || tokens.length > 3)
                        throw new IOException("Line " + numberOfLines + ": incorrect number of columns, expected 2 or 3, got: " + tokens.length);

                    final String readName = tokens[0].trim();
                    boolean found = false;
                    for (int i = 0; !found && i < parsers.length; i++) {
                        final int id = (parsers.length == 1 && Basic.isInteger(tokens[1]) ? Basic.parseInt(tokens[1]) : parsers[i].getIdFromHeaderLine(tokens[1]));

                        if (id != 0) {
                            float score;
                            if (tokens.length < 3) {
                                score = 50;
                                if (!warnedNoScoreGiven) {
                                    System.err.println("Setting score=50 for lines that only contained two tokens, such as line " + numberOfLines + ": '" + aLine + "'");
                                    warnedNoScoreGiven = true;
                                }
                            } else
                                score = Float.parseFloat(tokens[2].trim());
                            final List<Pair<Integer, Float>> taxonIdAndScore = readName2IdAndScore[i].computeIfAbsent(readName, k -> new LinkedList<>());
                            taxonIdAndScore.add(new Pair<>(id, score));
                            if (!readName.equals(prevName))
                                count[i]++;
                            found = true;
                        }
                    }

                    countClassNames++;
                    if (!found) {
                        System.err.println("Unrecognized name: " + tokens[1]);
                        countUnrecognizedClassNames++;
                    }

                    if (!readName.equals(prevName)) {
                        countInputReadNames++;
                        if (found)
                            countOutputReadNames++;
                    }

                    prevName = readName;
                } catch (Exception ex) {
                    System.err.println("Error: " + ex + ", skipping");
                    numberOfErrors++;
                }
                progress.setProgress(it.getProgress());
            }
        }
        if (progress instanceof ProgressPercentage)
            ((ProgressPercentage) progress).reportTaskCompleted();

        final int totalReads = Basic.max(count);

        if (taxonomyIndex >= 0) {
            progress.setSubtask("Running LCA");
            progress.setProgress(0);
            progress.setMaximum(readName2IdAndScore[taxonomyIndex].size());

            // run LCA algorithm to get assignment of reads
            Map<Integer, float[]> class2counts = new HashMap<>();
            Map<Integer, Float> class2count = new HashMap<>();

            for (String readName : readName2IdAndScore[taxonomyIndex].keySet()) {
                List<Pair<Integer, Float>> taxonIdAndScore = readName2IdAndScore[taxonomyIndex].get(readName);

                final int taxId = computeTaxonId(doc, taxonIdAndScore);

                if (taxId != 0) {
                    float[] counts = class2counts.computeIfAbsent(taxId, k -> new float[]{0});
                    counts[0]++;
                    if (class2count.get(taxId) == null)
                        class2count.put(taxId, 1f);
                    else
                        class2count.put(taxId, class2count.get(taxId) + 1);
                }
                progress.incrementProgress();
            }
            if (progress instanceof ProgressPercentage)
                ((ProgressPercentage) progress).reportTaskCompleted();

            // run the minsupport filter
            if (doc.getMinSupportPercent() > 0 || doc.getMinSupport() > 1) {
                if (doc.getMinSupportPercent() > 0) {
                    long assigned = 0;
                    for (int taxId : class2count.keySet()) {
                        if (taxId > 0)
                            assigned += class2count.get(taxId);
                    }
                    doc.setMinSupport((int) Math.max(1, (doc.getMinSupportPercent() / 100.0) * assigned));
                    System.err.println("MinSupport set to: " + doc.getMinSupport());
                }

                if (doc.getMinSupport() > 1) {
                    final MinSupportFilter minSupportFilter = new MinSupportFilter(Classification.Taxonomy, class2count, doc.getMinSupport(), progress);
                    try {
                        Map<Integer, Integer> changes = minSupportFilter.apply();
                        for (Integer oldTaxId : changes.keySet()) {
                            Integer newTaxId = changes.get(oldTaxId);
                            float oldCount = class2counts.get(oldTaxId)[0];

                            float[] newCounts = class2counts.get(newTaxId);
                            if (newCounts == null) {
                                newCounts = new float[]{oldCount};
                                class2counts.put(newTaxId, newCounts);
                            } else {
                                newCounts[0] += oldCount;
                            }
                            class2counts.remove(oldTaxId);
                        }
                    } catch (CanceledException ignored) {
                    }
                }
            }

            System.err.println(String.format("Reads in:%,13d", countInputReadNames));
            System.err.println(String.format("Reads out:%,12d", countOutputReadNames));

            System.err.println(String.format("Class names:%,10d", countClassNames));
            if (countUnrecognizedClassNames > 0)
                System.err.println(String.format("Unrecognized:%,9d", countUnrecognizedClassNames));

            if (countOutputReadNames < countInputReadNames) {
                float[] unassignedCounts = class2counts.computeIfAbsent(IdMapper.UNASSIGNED_ID, k -> new float[]{0});
                unassignedCounts[0] += (countInputReadNames - countOutputReadNames);
            }

            table.getClassification2Class2Counts().put(ClassificationType.Taxonomy.toString(), class2counts);
        } else {
            Map<Integer, float[]> class2counts = new HashMap<>();
            class2counts.put(IdMapper.UNASSIGNED_ID, new float[]{totalReads});
            table.getClassification2Class2Counts().put(ClassificationType.Taxonomy.toString(), class2counts);
        }

        for (int i = 0; i < cNames.length; i++) {
            if (i != taxonomyIndex) {
                progress.setSubtask("Classifying " + cNames[i]);
                progress.setProgress(0);
                progress.setMaximum(readName2IdAndScore[i].size());

                Map<Integer, float[]> class2counts = new HashMap<>();
                Map<Integer, Float> class2count = new HashMap<>();

                for (String readName : readName2IdAndScore[i].keySet()) {
                    final List<Pair<Integer, Float>> classIdAndScore = readName2IdAndScore[i].get(readName);
                    final int classId = getBestId(classIdAndScore);

                    if (classId != 0) {
                        float[] counts = class2counts.computeIfAbsent(classId, k -> new float[]{0});
                        counts[0]++;
                        if (class2count.get(classId) == null)
                            class2count.put(classId, 1f);
                        else
                            class2count.put(classId, class2count.get(classId) + 1);
                    }
                    progress.incrementProgress();
                }
                table.getClassification2Class2Counts().put(cNames[i], class2counts);
                if (progress instanceof ProgressPercentage)
                    ((ProgressPercentage) progress).reportTaskCompleted();
            }
        }

        table.setSamples(new String[]{Basic.getFileBaseName(new File(fileName).getName())}, null, new float[]{totalReads}, new BlastMode[]{BlastMode.Unknown});
        table.setTotalReads(totalReads);
        doc.setNumberReads(totalReads);
        for (int i = 0; i < cNames.length; i++) {
            if (i != taxonomyIndex)
                doc.getActiveViewers().remove(cNames[i]);
        }
        if (numberOfErrors > 0)
            NotificationsInSwing.showWarning(MainViewer.getLastActiveFrame(), "Lines skipped during import: " + numberOfErrors + " (of " + numberOfLines + ")");
        System.err.println("done (" + totalReads + " reads)");
    }

    private static int getBestId(List<Pair<Integer, Float>> idAndScore) {
        int bestId = 0;
        float bestScore = 0;

        for (Pair<Integer, Float> pair : idAndScore) {
            if (pair.getSecond() > bestScore) {
                bestScore = pair.getSecond();
                bestId = pair.getFirst();
            }
        }
        return bestId;
    }

    /**
     * compute the taxon id for a read using the LCA algorithm
     *
     * @param doc
     * @param taxonIdAndScore
     * @return taxonId
     */
    private static int computeTaxonId(Document doc, List<Pair<Integer, Float>> taxonIdAndScore) {
        final Pair<Integer, Float>[] pairs = taxonIdAndScore.toArray((Pair<Integer, Float>[]) new Pair[taxonIdAndScore.size()]);

        // sort by decreasing bit-score:
        Arrays.sort(pairs, (pair1, pair2) -> pair2.getSecond().compareTo(pair1.getSecond()));

        Set<Integer> taxonIds = new HashSet<>();

        Float bestScore = null;

        double threshold = doc.getMinScore();

        for (Pair<Integer, Float> pair : pairs) {
            Integer taxonId = pair.getFirst();
            Float score = pair.getSecond();
            if (score >= threshold) {
                if (bestScore == null) {
                    bestScore = score;
                    taxonIds.add(taxonId);
                    if (doc.getTopPercent() != 0) {
                        threshold = Math.max((1.0 - doc.getTopPercent() / 100.0) * bestScore, threshold);
                    }
                }
                taxonIds.add(taxonId);
            }
        }

        return TaxonomyData.getLCA(taxonIds, true);
    }
}
