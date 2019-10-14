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
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.classification.util.TaggedValueIterator;
import megan.core.ClassificationType;
import megan.core.Document;
import megan.data.*;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * export accession related data
 * Daniel Huson, 8.2011, 3.206
 */
class CSVExportRefSeq {
    /**
     * export readname to refseq id naming
     *
     * @param viewer
     * @param file
     * @param separator
     * @param progressListener @return lines written
     */
    public static int exportReadName2Accession(MainViewer viewer, File file, char separator, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {

            final TaggedValueIterator iterator = new TaggedValueIterator(true, true, "gb|", "ref|");
            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                final Document doc = viewer.getDir().getDocument();
                final IConnector connector = doc.getConnector();
                final IClassificationBlock classificationBlock = connector.getClassificationBlock(ClassificationType.Taxonomy.toString());
                final java.util.Collection<Integer> taxonIds = viewer.getSelectedIds();

                progressListener.setSubtask("Read ids to Accession ids");
                progressListener.setMaximum(taxonIds.size());
                progressListener.setProgress(0);

                for (int taxonId : taxonIds) {
                    final Node v = viewer.getTaxId2Node(taxonId);
                    final Collection<Integer> allBelow;
                    if (v.getOutDegree() == 0)
                        allBelow = TaxonomyData.getTree().getAllDescendants(taxonId);
                    else
                        allBelow = Collections.singletonList(taxonId);

                    try (IReadBlockIterator it = connector.getReadsIteratorForListOfClassIds(viewer.getClassName(), allBelow, 0, 10000, true, false)) {
                        while (it.hasNext()) {
                            final IReadBlock readBlock = it.next();
                            w.write(readBlock.getReadName() + separator);
                            for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
                                final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                                if (matchBlock.getBitScore() >= doc.getMinScore() && matchBlock.getExpected() <= doc.getMaxExpected() &&
                                        (matchBlock.getPercentIdentity() == 0 || matchBlock.getPercentIdentity() >= doc.getMinPercentIdentity())) {
                                    iterator.restart(matchBlock.getTextFirstWord());
                                    if (iterator.hasNext()) {
                                        final String accessionId = iterator.next();
                                        if (accessionId != null && accessionId.length() > 0) {
                                            w.write(" " + accessionId);
                                        }
                                    }
                                }
                            }
                            w.write("\n");
                            totalLines++;
                            progressListener.checkForCancel();
                        }
                    }
                    progressListener.incrementProgress();
                }
            }
        } catch (CanceledException ex) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }
}
