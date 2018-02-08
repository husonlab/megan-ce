/*
 *  Copyright (C) 2018 Daniel H. Huson
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
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.algorithms.ActiveMatches;
import megan.classification.Classification;
import megan.core.ClassificationType;
import megan.core.Director;
import megan.core.Document;
import megan.data.IConnector;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * export mapped taxa to normalized counts
 * Daniel Huson, 12.2012
 */
public class ExportTaxa2NormalizedCounts {

    /**
     * export mapped taxa to normalized counts
     *
     * @param dir
     * @param file
     * @param separator
     * @param progressListener
     * @return
     * @throws IOException
     * @throws CanceledException
     */
    public static int export(String format, Director dir, File file, char separator, boolean reportSummarized, ProgressListener progressListener) throws IOException {
        int countTaxa = 0;
        try {
            final Document doc = dir.getDocument();
            final MainViewer viewer = dir.getMainViewer();
            final IConnector connector = doc.getConnector();

            int numberOfTaxa = viewer.getSelectedNodes().size();

            progressListener.setSubtask("Taxa to normalized counts");
            progressListener.setMaximum(numberOfTaxa);
            progressListener.setProgress(0);

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                final Set<String> references = new HashSet<>();
                final Collection<Integer> taxonIds = viewer.getSelectedIds();

                for (int taxonId : taxonIds) {
                    int countMatchedReads = 0;
                    final Set<Integer> allBelow;
                    final Node v = viewer.getTaxId2Node(taxonId);
                    if (v.getOutDegree() == 0 || reportSummarized)
                        allBelow = TaxonomyData.getTree().getAllDescendants(taxonId);
                    else {
                        allBelow = new HashSet<>();
                        allBelow.add(taxonId);
                    }
                    try (IReadBlockIterator it = connector.getReadsIteratorForListOfClassIds(ClassificationType.Taxonomy.toString(), allBelow, doc.getMinScore(), doc.getMaxExpected(), false, true)) {
                        while (it.hasNext()) {
                            final IReadBlock readBlock = it.next();
                            final BitSet activeMatchesForTaxa = new BitSet();

                            ActiveMatches.compute(doc.getMinScore(), doc.getTopPercent(), doc.getMaxExpected(), doc.getMinPercentIdentity(), readBlock, Classification.Taxonomy, activeMatchesForTaxa);

                            for (int i = activeMatchesForTaxa.nextSetBit(0); i >= 0; i = activeMatchesForTaxa.nextSetBit(i + 1)) {
                                final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                                String header = matchBlock.getText();
                                if (header != null) {
                                    int pos = matchBlock.getText().indexOf("\n");
                                    if (pos > 0)
                                        header = header.substring(0, pos);
                                }
                                if (header == null)
                                    header = matchBlock.getRefSeqId();
                                if (header != null) {
                                    references.add(header);
                                    countMatchedReads++;
                                }
                                progressListener.checkForCancel();
                            }
                        }
                    }
                    progressListener.incrementProgress();
                    float normalizedCount = (references.size() > 0 ? (float) countMatchedReads / (float) references.size() : 0f);
                    w.write(String.format("%s%c%d/%d%c%g\n", CSVExportTaxonomy.getTaxonLabelSource(format, taxonId), separator, countMatchedReads, references.size(), separator, normalizedCount));
                    references.clear();
                    countTaxa++;
                }
            }
        } catch (CanceledException ex) {
            System.err.println("USER CANCELED");
        }
        return countTaxa;
    }
}
