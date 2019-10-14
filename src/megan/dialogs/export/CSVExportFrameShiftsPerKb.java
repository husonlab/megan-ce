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
import jloda.swing.window.NotificationsInSwing;
import jloda.util.*;
import jloda.util.interval.IntervalTree;
import megan.algorithms.IntervalTree4Matches;
import megan.classification.Classification;
import megan.core.Document;
import megan.data.*;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * exports the average number of frame-shifts per kilo-base of aligned sequence
 * Daniel Huson, 13.2018
 */
class CSVExportFrameShiftsPerKb {
    /**
     * export mapping of read names to frame-shifts per kilo-base
     *
     * @param file
     * @param progress
     */
    public static int apply(final MainViewer viewer, final File file, final char separator, final boolean excludeDominated, final ProgressListener progress) throws IOException {
        final Document doc = viewer.getDocument();
        int totalLines = 0;

        System.err.println("Writing file: " + file);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            final IConnector connector = viewer.getDocument().getConnector();
            final Collection<Integer> ids = viewer.getSelectedIds();

            progress.setSubtask("Reads to FrameShifts per Kb");
            progress.setMaximum(1000 * ids.size());
            progress.setProgress(0);

            final IClassificationBlock classificationBlock = connector.getClassificationBlock(Classification.Taxonomy);

            if (classificationBlock != null) {
                final Set<Long> seen = new HashSet<>();

                int countsClasses = 0;
                ArrayList<Float> values = new ArrayList<>();

                try {
                    for (int classId : ids) {
                        final Collection<Integer> allBelow;
                        final Node v = viewer.getTaxId2Node(classId);

                        if (v.getOutDegree() == 0)
                            allBelow = TaxonomyData.getTree().getAllDescendants(classId);
                        else
                            allBelow = Collections.singletonList(classId);

                        if (viewer.getNodeData(v).getCountSummarized() > 0) {
                            try (IReadBlockIterator it = connector.getReadsIteratorForListOfClassIds(viewer.getClassName(), allBelow, doc.getMinScore(), doc.getMaxExpected(), true, true)) {
                                final ArrayList<Pair<String, Float>> pairs = new ArrayList<>();
                                while (it.hasNext()) {
                                    final IReadBlock readBlock = it.next();
                                    if (readBlock.getNumberOfAvailableMatchBlocks() > 0) {
                                        final long uid = readBlock.getUId();
                                        if (!seen.contains(uid)) {
                                            if (uid != 0)
                                                seen.add(uid);
                                            float frameShiftsPerKb = computeFrameShiftsPerKb(readBlock, excludeDominated);
                                            pairs.add(new Pair<>(readBlock.getReadName(), frameShiftsPerKb));
                                            totalLines++;
                                        }
                                    }
                                    progress.setProgress((long) (1000.0 * (countsClasses + (double) it.getProgress() / (double) it.getMaximumProgress())));
                                }

                                final Statistics statistics = new Statistics(Pair.secondValues(pairs));
                                w.write(String.format("# %s (count=%d, mean=%.1f, stddev=%.1f):\n", TaxonomyData.getName2IdMap().get(classId), statistics.getCount(), statistics.getMean(), statistics.getStdDev()));
                                for (Pair<String, Float> pair : pairs) {
                                    w.write(String.format("%s%c%.1f\n", pair.getFirst(), separator, pair.getSecond()));
                                }
                                for (Float value : Pair.secondValues(pairs))
                                    values.add(value);
                            }
                        }
                        countsClasses++;
                    }
                } finally {
                    if (values.size() > 0) {
                        final Statistics statistics = new Statistics(values);
                        final String summary = String.format("# Total frame-shifts per kb-aligned-sequence (reads=%,d): mean=%.1f, stddev=%.1f", statistics.getCount(), statistics.getMean(), statistics.getStdDev());
                        w.write(summary + "\n");
                        NotificationsInSwing.showInformation(summary, 5000);
                    }
                }
            }

        } catch (CanceledException ex) {
            System.err.println("USER CANCELED");
        }
        System.err.println("done");

        return totalLines;
    }

    /**
     * computes the number of frame shifts per KB for all matches associated with a read
     *
     * @param readBlock
     * @return frame shifts per KB
     */
    private static float computeFrameShiftsPerKb(IReadBlock readBlock, boolean excludeDominated) {
        int countFrameShifts = 0;
        int countAlignedBases = 0;

        if (excludeDominated) {
            try {
                final IntervalTree<IMatchBlock> intervals = IntervalTree4Matches.extractStronglyDominatingIntervals(IntervalTree4Matches.computeIntervalTree(readBlock, null, null), new String[]{Classification.Taxonomy}, "all");
                for (IMatchBlock matchBlock : intervals.values()) {
                    for (String line : Basic.getLinesFromString(matchBlock.getText())) {
                        if (line.startsWith("Query:")) {
                            countFrameShifts += Basic.countOccurrences(line, '\\');
                            countFrameShifts += Basic.countOccurrences(line, '/');
                        }
                    }
                    countAlignedBases += Math.abs(matchBlock.getAlignedQueryStart() - matchBlock.getAlignedQueryEnd()) + 1;
                }
            } catch (CanceledException ex) {
                // can't happen
            }
        } else {
            for (int m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
                final IMatchBlock matchBlock = readBlock.getMatchBlock(m);

                for (String line : Basic.getLinesFromString(matchBlock.getText())) {
                    if (line.startsWith("Query:")) {
                        countFrameShifts += Basic.countOccurrences(line, '\\');
                        countFrameShifts += Basic.countOccurrences(line, '/');
                    }
                }
                countAlignedBases += matchBlock.getLength();
            }
        }
        if (countAlignedBases > 0)
            return (1000f * countFrameShifts) / countAlignedBases;
        else
            return 0;
    }
}
