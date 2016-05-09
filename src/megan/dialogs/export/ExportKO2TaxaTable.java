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
package megan.dialogs.export;

import jloda.graph.Node;
import jloda.graph.NodeSet;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.algorithms.KeggTopAssignment;
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
import java.util.BitSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * exports a KO to taxa table
 * Daniel Huson, 12.2011
 */
public class ExportKO2TaxaTable {

    /**
     * export a table of KO to taxa
     *
     * @param dir
     * @param file
     * @param separator
     * @param progressListener
     * @return
     * @throws IOException
     */
    public static int export(String format, Director dir, File file, char separator, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final Document doc = dir.getDocument();
            final MainViewer mainViewer = dir.getMainViewer();
            final IConnector connector = doc.getMeganFile().getDataConnector();

            ClassificationManager.ensureTreeIsLoaded("KEGG");

            int numberOfTaxa = mainViewer.getSelectedNodes().size();

            progressListener.setSubtask("KEGG to taxa");
            progressListener.setMaximum(numberOfTaxa);
            progressListener.setProgress(0);

            String[] taxonNames = new String[numberOfTaxa];
            SortedMap<Integer, int[]> ko2counts = new TreeMap<>();

            NodeSet selectedNodes = mainViewer.getSelectedNodes();

            int countTaxa = 0;
            for (Node v = selectedNodes.getFirstElement(); v != null; v = selectedNodes.getNextElement(v)) {
                Integer taxonId = (Integer) v.getInfo();
                taxonNames[countTaxa] = TaxonomyData.getName2IdMap().get(taxonId);
                try (IReadBlockIterator it = connector.getReadsIterator(ClassificationType.Taxonomy.toString(), taxonId, doc.getMinScore(), doc.getMaxExpected(), true, true)) {
                    while (it.hasNext()) {
                        IReadBlock readBlock = it.next();

                        final BitSet activeMatchesForTaxa = new BitSet();

                        int keggId;
                        if (readBlock.getComplexity() > 0 && readBlock.getComplexity() + 0.01 < doc.getMinComplexity())
                            keggId = IdMapper.LOW_COMPLEXITY_ID;
                        else
                            keggId = KeggTopAssignment.computeId("KEGG", doc.getMinScore(), doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock);
                        int[] counts = ko2counts.get(keggId);
                        if (counts == null) {
                            counts = new int[numberOfTaxa];
                            ko2counts.put(keggId, counts);
                        }
                        counts[countTaxa]++;
                        progressListener.checkForCancel();
                    }
                    countTaxa++;
                }
                progressListener.incrementProgress();
            }

            progressListener.setSubtask("Writing output");
            progressListener.setMaximum(ko2counts.size());
            progressListener.setProgress(0);

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                w.write("#KO-toTaxa" + separator + Basic.toString(taxonNames, separator + "") + "\n");
                for (Map.Entry<Integer, int[]> entry : ko2counts.entrySet()) {
                    int ko = entry.getKey();
                    w.write(String.format("%d", ko));
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
