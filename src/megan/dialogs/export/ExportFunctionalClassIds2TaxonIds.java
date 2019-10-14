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
package megan.dialogs.export;

import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.algorithms.TopAssignment;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.classification.IdMapper;
import megan.core.ClassificationType;
import megan.core.Director;
import megan.core.Document;
import megan.data.IConnector;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * exports a functional classification to taxa table
 * Daniel Huson, 12.2011, 1.2019
 */
class ExportFunctionalClassIds2TaxonIds {

    /**
     * export a table of functional classification ids to taxa
     *
     *
     * @param classificationName
     * @param format
     * @param dir
     * @param file
     * @param separator
     * @param progressListener
     * @return
     * @throws IOException
     */
    public static int export(String classificationName, String format, Director dir, File file, char separator, ProgressListener progressListener) throws IOException {
        final boolean useClassId = format.contains("Id_to");
        final boolean useTaxonId = format.endsWith("Id");

        if (classificationName.equals(Classification.Taxonomy))
            throw new IOException("Not a functional classification: Taxonomy");
        int totalLines = 0;
        try {
            final Document doc = dir.getDocument();
            final MainViewer mainViewer = dir.getMainViewer();
            final IConnector connector = doc.getConnector();

            final Classification classification = ClassificationManager.get(classificationName, true);

            int numberOfTaxa = mainViewer.getSelectedNodes().size();

            progressListener.setSubtask(classificationName + " to taxa");
            progressListener.setMaximum(numberOfTaxa);
            progressListener.setProgress(0);

            final int[] taxonIds = new int[numberOfTaxa];
            final SortedMap<Integer, int[]> classId2Counts = new TreeMap<>();

            final NodeSet selectedNodes = mainViewer.getSelectedNodes();

            int countTaxa = 0;
            for (Node v = selectedNodes.getFirstElement(); v != null; v = selectedNodes.getNextElement(v)) {
                final Integer taxonId = (Integer) v.getInfo();
                taxonIds[countTaxa] = taxonId;
                try (IReadBlockIterator it = connector.getReadsIterator(ClassificationType.Taxonomy.toString(), taxonId, doc.getMinScore(), doc.getMaxExpected(), true, true)) {
                    while (it.hasNext()) {
                        final IReadBlock readBlock = it.next();

                        final int classId;
                        if (readBlock.getComplexity() > 0 && readBlock.getComplexity() + 0.01 < doc.getMinComplexity())
                            classId = IdMapper.LOW_COMPLEXITY_ID;
                        else
                            classId = TopAssignment.computeId(classificationName, doc.getMinScore(), doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock);
                        int[] counts = classId2Counts.computeIfAbsent(classId, k -> new int[numberOfTaxa]);
                        counts[countTaxa]++;
                        progressListener.checkForCancel();
                    }
                    countTaxa++;
                }
                progressListener.incrementProgress();
            }

            progressListener.setSubtask("Writing output");
            progressListener.setMaximum(classId2Counts.size());
            progressListener.setProgress(0);

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                if (useTaxonId)
                    w.write("#" + format + separator + Basic.toString(taxonIds, "" + separator) + "\n");
                else {
                    String[] taxonNames = new String[taxonIds.length];
                    for (int i = 0; i < taxonIds.length; i++) {
                        taxonNames[i] = TaxonomyData.getName2IdMap().get(taxonIds[i]);
                        if (taxonNames[i] == null)
                            taxonNames[i] = "" + taxonIds[i];
                    }
                    w.write("#" + format + separator + Basic.toString(taxonNames, "" + separator) + "\n");
                }
                for (Map.Entry<Integer, int[]> entry : classId2Counts.entrySet()) {
                    final int classId = entry.getKey();
                    if (useClassId) {
                        w.write("" + classId);
                    } else {
                        final String name = classification.getName2IdMap().get(classId);
                        w.write(Objects.requireNonNullElseGet(name, () -> "" + classId));
                    }

                    for (int x : entry.getValue()) {
                        w.write(String.format("%c%d", separator, x));
                    }
                    w.write("\n");
                    totalLines++;
                    progressListener.incrementProgress();
                }
            }
        } catch (CanceledException ex) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }
}
