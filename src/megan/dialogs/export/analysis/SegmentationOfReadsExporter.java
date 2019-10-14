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

package megan.dialogs.export.analysis;

import jloda.graph.Node;
import jloda.util.*;
import megan.analysis.TaxonomicSegmentation;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.data.IConnector;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

/**
 *  export taxonomic segmentation
 * Daniel Huson, 8.2018
 */
public class SegmentationOfReadsExporter {
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
    public static int export(ProgressListener progress, String classificationName, Collection<Integer> classIds, int rank, IConnector connector, String fileName, TaxonomicSegmentation taxonomicSegmentation) throws IOException {
        int total = 0;
        try {
            progress.setTasks("Segmentation", "Initializing");

            final boolean useOneOutputFile = (!fileName.contains("%t") && !fileName.contains("%i"));

            final Classification classification;
            BufferedWriter w;
            if (useOneOutputFile) {
                w = new BufferedWriter(new FileWriter(fileName));
                w.write("#Segmentation parameters: " + taxonomicSegmentation.getParamaterString() + "\n");
                classification = null;
            } else {
                w = null;
                classification = ClassificationManager.get(classificationName, true);
            }

            int maxProgress = 100000 * classIds.size();

            progress.setMaximum(maxProgress);
            progress.setProgress(0);

            if (rank > 0) {
                taxonomicSegmentation.setRank(rank);
                System.err.println("Using rank: " + TaxonomicLevels.getName(taxonomicSegmentation.getRank()));
            }

            int countClassIds = 0;
            try {
                for (Integer classId : classIds) {
                    countClassIds++;

                    if (rank == 0) {
                        int classRank = TaxonomicLevels.getNextRank(getRank(classId));
                        taxonomicSegmentation.setRank(classRank > 0 ? classRank : TaxonomicLevels.getSpeciesId());
                        System.err.println("Taxon " + classId + " (" + TaxonomyData.getName2IdMap().get(classId) + "), using rank: " + TaxonomicLevels.getName(taxonomicSegmentation.getRank()));
                    }
                    taxonomicSegmentation.setClassId(classId);

                    boolean first = true;

                    try (IReadBlockIterator it = connector.getReadsIterator(classificationName, classId, 0, 10000, true, true)) {
                        while (it.hasNext()) {
                            // open file here so that we only create a file if there is actually something to iterate over...
                            if (first) {
                                if (!useOneOutputFile) {
                                    if (w != null)
                                        w.close();
                                    final String cName = classification.getName2IdMap().get(classId);
                                    final File file = new File(fileName.replaceAll("%t", Basic.toCleanName(cName)).replaceAll("%i", "" + classId));
                                    if (ProgramProperties.isUseGUI() && file.exists()) {
                                        final Single<Boolean> ok = new Single<>(true);
                                        try {
                                            SwingUtilities.invokeAndWait(() -> {
                                                switch (JOptionPane.showConfirmDialog(null, "File already exists, do you want to replace it?", "File exists", JOptionPane.YES_NO_CANCEL_OPTION)) {
                                                    case JOptionPane.NO_OPTION:
                                                    case JOptionPane.CANCEL_OPTION: // close and abort
                                                        ok.set(false);
                                                    default:
                                                }
                                            });
                                        } catch (InterruptedException | InvocationTargetException e) {
                                            Basic.caught(e);
                                        }
                                        if (!ok.get())
                                            throw new CanceledException();
                                    }
                                    w = new BufferedWriter(new FileWriter(file));
                                    w.write("#Segmentation parameters: " + taxonomicSegmentation.getParamaterString() + "\n");
                                }
                                first = false;
                            }
                            total++;

                            segmentAndWrite(progress, it.next(), taxonomicSegmentation, w);
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
     * get the rank of the class id (or next above)
     *
     * @param classId
     * @return rank id
     */
    private static int getRank(Integer classId) {
        Node v = TaxonomyData.getTree().getANode(classId);
        while (v != null) {
            final int rank = TaxonomyData.getTaxonomicRank(classId);
            if (rank > 0)
                return rank;
            if (v.getInDegree() > 0) {
                v = v.getFirstInEdge().getSource();
                classId = (Integer) v.getInfo();
            } else
                v = null;
        }
        return 0;
    }

    /**
     * segment and write a read
     *
     * @param readBlock
     * @param w
     * @return number of reads written
     * @throws IOException
     */
    private static void segmentAndWrite(ProgressListener progress, IReadBlock readBlock, TaxonomicSegmentation taxonomicSegmentation, Writer w) throws IOException, CanceledException {
        final ArrayList<TaxonomicSegmentation.Segment> segmentation = taxonomicSegmentation.computeTaxonomicSegmentation(progress, readBlock);

        String header = readBlock.getReadHeader();
        if (header == null)
            header = "untitled";
        w.write(Basic.swallowLeadingGreaterSign(header) + "\t" + Basic.toString(segmentation, "\t") + "\n");
    }

}

