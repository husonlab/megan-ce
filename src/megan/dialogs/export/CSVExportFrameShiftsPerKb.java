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

import jloda.util.*;
import megan.algorithms.IntervalTree4Matches;
import megan.classification.Classification;
import megan.core.Document;
import megan.data.*;
import megan.util.interval.IntervalTree;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * exports the average number of frame-shifts per kilo-base of aligned sequence
 * Daniel Huson, 13.2018
 */
public class CSVExportFrameShiftsPerKb {
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

                for (int classId : ids) {
                    final int numberOfReads = classificationBlock.getSum(classId);

                    if (numberOfReads > 0) {

                        try (IReadBlockIterator it = connector.getReadsIterator(viewer.getClassName(), classId, doc.getMinScore(), doc.getMaxExpected(), true, true)) {
                            final ArrayList<Pair<String, Float>> pairs = new ArrayList<>(numberOfReads);
                            while (it.hasNext()) {
                                final IReadBlock readBlock = it.next();
                                if (readBlock.getNumberOfAvailableMatchBlocks() > 0) {
                                    final long uid = readBlock.getUId();
                                    if (!seen.contains(uid)) {
                                        if (uid != 0)
                                            seen.add(uid);
                                        float frameShiftsPerKb = computeFrameShiftsPerKb(readBlock, excludeDominated);
                                        pairs.add(new Pair<String, Float>(readBlock.getReadName(), frameShiftsPerKb));
                                        totalLines++;
                                    }
                                }
                                progress.setProgress((long) (1000.0 * (countsClasses + (double) pairs.size() / (double) numberOfReads)));
                            }

                            final Statistics statistics;
                            {
                                float[] values = new float[pairs.size()];
                                int i = 0;
                                for (Pair<String, Float> pair : pairs) {
                                    values[i++] = pair.getSecond();
                                }
                                statistics = new Statistics(values);
                            }
                            w.write(String.format("# %s (count=%d, mean=%.1f, stddev=%.1f):\n", TaxonomyData.getName2IdMap().get(classId), pairs.size(), statistics.getMean(), statistics.getStdDev()));
                            for (Pair<String, Float> pair : pairs) {
                                w.write(String.format("%s%c%.1f\n", pair.getFirst(), separator, pair.getSecond()));
                            }
                        }
                    }
                    countsClasses++;

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
    public static float computeFrameShiftsPerKb(IReadBlock readBlock, boolean excludeDominated) {
        int countFrameShifts = 0;
        int countAlignedBases = 0;

        if (excludeDominated) {
            final IntervalTree<IMatchBlock> intervals = IntervalTree4Matches.extractDominatingIntervals(IntervalTree4Matches.computeIntervalTree(readBlock, null), new String[]{Classification.Taxonomy}, "all");
            for (IMatchBlock matchBlock : intervals.values()) {
                for (String line : Basic.getLinesFromString(matchBlock.getText())) {
                    if (line.startsWith("Query:")) {
                        countFrameShifts += Basic.countOccurrences(line, '\\');
                        countFrameShifts += Basic.countOccurrences(line, '/');
                    }
                }
                countAlignedBases += Math.abs(matchBlock.getAlignedQueryStart() - matchBlock.getAlignedQueryEnd()) + 1;
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
