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
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.data.*;
import megan.util.interval.IntervalTree;
import megan.viewer.ClassificationViewer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * export selected reads to length, number of alignments and amount of sequence covered
 * Created by huson on March 2017
 */
public class ExportReads2LengthAndAlignmentCoverage {
    /**
     * export read names to lengths and  amount of sequence covered
     *
     * @param file
     * @param progressListener
     * @return lines written
     */
    public static int apply(ClassificationViewer cViewer, File file, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try {
            final Classification classification = ClassificationManager.get(cViewer.getClassName(), true);

            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                w.write(getHeader());
                IConnector connector = cViewer.getDocument().getConnector();
                java.util.Collection<Integer> ids = cViewer.getSelectedIds();
                progressListener.setSubtask("Reads to length and coverage");
                progressListener.setMaximum(ids.size());
                progressListener.setProgress(0);

                final IClassificationBlock classificationBlock = connector.getClassificationBlock(cViewer.getClassName());

                if (classificationBlock != null) {
                    for (int classId : ids) {
                        final Set<Long> seen = new HashSet<>();
                        final Set<Integer> allBelow;
                        Node v = classification.getFullTree().getANode(classId);
                        if (v.getOutDegree() > 0)
                            allBelow = classification.getFullTree().getAllDescendants(classId);
                        else {
                            allBelow = new HashSet<>();
                            allBelow.add(classId);
                        }

                        for (final int id : allBelow) {
                            if (classificationBlock.getSum(id) > 0) {
                                try (IReadBlockIterator it = connector.getReadsIterator(cViewer.getClassName(), id, 0, 10000, true, true)) {
                                    while (it.hasNext()) {
                                        final IReadBlock readBlock = it.next();
                                        final long uid = readBlock.getUId();
                                        if (!seen.contains(uid)) {
                                            if (uid != 0)
                                                seen.add(uid);
                                            w.write(createReportLine(readBlock));
                                            totalLines++;
                                        }
                                    }
                                }
                                progressListener.checkForCancel();
                            }
                        }
                        progressListener.incrementProgress();
                    }
                }
            }
        } catch (CanceledException canceled) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }

    /**
     * get header comment line
     *
     * @return header
     */
    public static String getHeader() {
        return "# seqname number-of-bases number-of-alignments number-of-bases-covered\n";
    }

    /**
     * create a line of the report
     *
     * @param readBlock
     * @return line
     */
    public static String createReportLine(IReadBlock readBlock) {
        final IntervalTree<IMatchBlock> intervalTree = new IntervalTree<>();
        for (int m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
            intervalTree.add(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), matchBlock);
        }
        return String.format("%s\t%d\t%d\t%d\n", readBlock.getReadName(), readBlock.getReadLength(), readBlock.getNumberOfAvailableMatchBlocks(),
                intervalTree.getCovered());
    }
}
