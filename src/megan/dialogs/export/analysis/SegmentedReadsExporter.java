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

package megan.dialogs.export.analysis;

import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.analysis.TaxonomicSegmentation;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Document;
import megan.data.IConnector;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;

/**
 *  export taxonomic segmentationm
 * Daniel Huson, 8.2018
 */
public class SegmentedReadsExporter {
    /**
     * export all matches in file
     *
     * @param connector
     * @param fileName
     * @throws IOException
     */
    public static int exportAll(Document doc, IConnector connector, String fileName) throws IOException {
        int total = 0;
        try {
            final ProgressListener progress = doc.getProgressListener();
            progress.setTasks("Export", "Writing all segmented reads");

            final TaxonomicSegmentation taxonomicSegmentation = new TaxonomicSegmentation();

            try (BufferedWriter w = new BufferedWriter(new FileWriter(fileName)); IReadBlockIterator it = connector.getAllReadsIterator(0, 10000, true, true)) {
                progress.setMaximum(it.getMaximumProgress());
                progress.setProgress(0);
                while (it.hasNext()) {
                    total++;
                    segmentAndWrite(doc, progress, it.next(), taxonomicSegmentation, w);
                    progress.setProgress(it.getProgress());
                }
            }
        } catch (CanceledException ex) {
            System.err.println("USER CANCELED");
        }
        return total;
    }

    /**
     * Compute segmentation of long reads for given set of classids in the given classification
     *
     * @param classificationName
     * @param classIds
     * @param connector
     * @param fileName
     * @throws IOException
     * @throws CanceledException
     */
    public static int export(Document doc, String classificationName, Collection<Integer> classIds, IConnector connector, String fileName) throws IOException {
        int total = 0;
        try {
            final TaxonomicSegmentation taxonomicSegmentation = new TaxonomicSegmentation();

            final ProgressListener progress = doc.getProgressListener();

            progress.setTasks("Segmentation", "Writing segmentation of reads");

            final boolean useOneOutputFile = (!fileName.contains("%t") && !fileName.contains("%i"));

            final Classification classification;
            BufferedWriter w;
            if (useOneOutputFile) {
                w = new BufferedWriter(new FileWriter(fileName));
                classification = null;
            } else {
                w = null;
                classification = ClassificationManager.get(classificationName, true);
            }

            int maxProgress = 100000 * classIds.size();

            progress.setMaximum(maxProgress);
            progress.setProgress(0);

            int countClassIds = 0;
            try {
                for (Integer classId : classIds) {
                    countClassIds++;

                    boolean first = true;

                    try (IReadBlockIterator it = connector.getReadsIterator(classificationName, classId, 0, 10000, true, true)) {
                        while (it.hasNext()) {
                            // open file here so that we only create a file if there is actually something to iterate over...
                            if (first) {
                                if (!useOneOutputFile) {
                                    if (w != null)
                                        w.close();
                                    final String cName = classification.getName2IdMap().get(classId);
                                    w = new BufferedWriter(new FileWriter(fileName.replaceAll("%t", Basic.toCleanName(cName)).replaceAll("%i", "" + classId)));
                                }
                                first = false;
                            }
                            total++;
                            segmentAndWrite(doc, progress, it.next(), taxonomicSegmentation, w);
                            progress.setProgress((long) (100000.0 * (countClassIds + (double) it.getProgress() / it.getMaximumProgress())));
                        }
                    }
                }
            } finally {
                if (w != null)
                    w.close();
            }
        } catch (CanceledException ex) {
            System.err.println("USER CANCELED");
        }
        return total;
    }

    /**
     * segment and write a read
     *
     * @param readBlock
     * @param w
     * @return number of reads written
     * @throws IOException
     */
    private static void segmentAndWrite(Document doc, ProgressListener progress, IReadBlock readBlock, TaxonomicSegmentation taxonomicSegmentation, Writer w) throws IOException, CanceledException {
        final ArrayList<TaxonomicSegmentation.Segment> segmentation = taxonomicSegmentation.computeTaxonomicSegmentation(progress, readBlock);

        String header = readBlock.getReadHeader();
        if (header == null)
            header = ">untitled";
        w.write((header.startsWith(">") ? header : ">" + header) + "\n");
        w.write("Segmentation: " + Basic.toString(segmentation, " ") + "\n");
    }

}

