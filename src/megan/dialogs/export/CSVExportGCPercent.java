/*
 *  Copyright (C) 2015 Daniel H. Huson
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
import megan.classification.Classification;
import megan.core.Document;
import megan.data.IClassificationBlock;
import megan.data.IConnector;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * export mapping read names to GC percent
 * Daniel Huson, 2.2019
 */
class CSVExportGCPercent {
    /**
     * apply
     *
     * @param viewer
     * @param file
     * @param separator
     * @param progress
     * @return
     */
    public static int apply(MainViewer viewer, File file, char separator, ProgressListener progress) throws IOException {
        final Document doc = viewer.getDocument();
        int totalLines = 0;

        System.err.println("Writing file: " + file);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            final IConnector connector = viewer.getDocument().getConnector();
            final Collection<Integer> ids = viewer.getSelectedIds();

            final IClassificationBlock classificationBlock = connector.getClassificationBlock(Classification.Taxonomy);

            if (classificationBlock != null) {
                try {
                    progress.setSubtask("Reads to GC percentage");
                    progress.setMaximum(ids.size());
                    progress.setProgress(0);

                    for (int classId : ids) {
                        final Collection<Integer> allBelow;
                        w.write("# " + TaxonomyData.getName2IdMap().get(classId) + "\n");
                        final Node v = viewer.getTaxId2Node(classId);

                        if (v.getOutDegree() == 0)
                            allBelow = TaxonomyData.getTree().getAllDescendants(classId);
                        else
                            allBelow = Collections.singletonList(classId);

                        if (viewer.getNodeData(v).getCountSummarized() > 0) {
                            try (IReadBlockIterator it = connector.getReadsIteratorForListOfClassIds(viewer.getClassName(), allBelow, doc.getMinScore(), doc.getMaxExpected(), true, false)) {
                                while (it.hasNext()) {
                                    final IReadBlock readBlock = it.next();
                                    w.write(String.format("%s%c%.1f\n", readBlock.getReadName(), separator, computeCGPercent(readBlock.getReadSequence())));
                                    totalLines++;
                                }
                            }
                        }
                        progress.incrementProgress();
                    }
                } catch (CanceledException ex) {
                    System.err.println("USER CANCELED");
                }
                System.err.println("done");
            }
        }
        return totalLines;

    }

    /**
     * compute the CG percentage
     *
     * @param readSequence
     * @return cg Percerntage
     */
    private static double computeCGPercent(String readSequence) {
        double countCG = 0;
        double countAll = 0;
        for (int i = 0; i < readSequence.length(); i++) {
            final int ch = Character.toLowerCase(readSequence.charAt(i));
            if (ch == 'c' || ch == 'g') {
                countCG++;
                countAll++;
            } else if (ch == 'a' || ch == 't' || ch == 'u')
                countAll++;
        }
        return (countAll > 0 ? 100 * countCG / countAll : 0);
    }
}
