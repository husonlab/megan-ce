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

import jloda.graph.Node;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.ProgressListener;
import megan.algorithms.IntervalTree4Matches;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.Document;
import megan.data.IConnector;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.util.interval.Interval;
import megan.util.interval.IntervalTree;
import megan.viewer.TaxonomicLevels;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * perform taxonomic segmentation of long reads
 * Daniel Huson, 8.2018
 */
public class SegmentedReadsExporter {
    private static boolean verbose = false;

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

            try (BufferedWriter w = new BufferedWriter(new FileWriter(fileName)); IReadBlockIterator it = connector.getAllReadsIterator(0, 10000, true, true)) {
                progress.setMaximum(it.getMaximumProgress());
                progress.setProgress(0);
                while (it.hasNext()) {
                    total++;
                    segmentAndWrite(doc, progress, it.next(), w);
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
        verbose = false;

        int total = 0;
        try {
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
                            segmentAndWrite(doc, progress, it.next(), w);
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
    private static void segmentAndWrite(Document doc, ProgressListener progress, IReadBlock readBlock, Writer w) throws IOException, CanceledException {
        final Pair<String, String> headerAndSegmentation = computeSegmentation(doc, progress, readBlock);
        if (headerAndSegmentation != null) {
            w.write(headerAndSegmentation.getFirst());
            w.write("\n");
            w.write(headerAndSegmentation.getSecond());
            w.write("\n");
        } else
            w.write("null\n");
    }

    /**
     * computes the corrected sequence, greedily using frame-shift alignments
     *
     * @param readBlock
     * @return corrected sequence or null
     */
    private static Pair<String, String> computeSegmentation(Document doc, ProgressListener progress, IReadBlock readBlock) throws CanceledException {
        final String originalSequence = readBlock.getReadSequence();
        if (originalSequence == null)
            return null;

        progress.setSubtask("Computing intervals");

        final IntervalTree<IMatchBlock> intervals = IntervalTree4Matches.computeIntervalTree(readBlock, null); // todo: use dominated only?

        final TreeSet<Integer> positions = new TreeSet<>();
        for (Interval<IMatchBlock> interval : intervals) {
            positions.add(interval.getStart() + 1);
            positions.add(interval.getEnd() - 1);
        }

        int rank = TaxonomicLevels.getId(TaxonomicLevels.Phylum);

        final ArrayList<DataPoint> dataPoints = computeDataPoints(intervals, positions, rank);

        final Set<Integer> allTaxa = new TreeSet<>();
        for (IMatchBlock matchBlock : intervals.values()) {
            if (matchBlock.getTaxonId() > 0) {
                int tax = getAncestorAtRank(rank, matchBlock.getTaxonId());
                if (tax > 0)
                    allTaxa.add(tax);
            }
        }

        final ArrayList<Segment> segments = computeSegments(progress, allTaxa, 1, readBlock.getReadLength(), dataPoints);

        final String originalHeader = readBlock.getReadHeader();
        final StringBuilder headerBuf = new StringBuilder();
        if (originalHeader == null)
            headerBuf.append(">unnamed");
        else
            headerBuf.append(originalHeader.startsWith(">") ? originalHeader.trim() : ">" + originalHeader.trim());

        return new Pair<>(headerBuf.toString(), Basic.toString(segments, " "));
    }

    /**
     * compute the datapoints
     *
     * @param intervals
     * @param positions
     * @param rank
     * @return DP data points
     */
    private static ArrayList<DataPoint> computeDataPoints(IntervalTree<IMatchBlock> intervals, TreeSet<Integer> positions, int rank) {
        final ArrayList<DataPoint> dataPoints = new ArrayList<>();

        DataPoint prevDataPoint = null;

        for (Integer pos : positions) {
            final DataPoint dataPoint = new DataPoint(pos);
            if (prevDataPoint == null) { // initialized first
                dataPoints.add(dataPoint);
            } else {
                for (Interval<IMatchBlock> interval : intervals.getIntervals(pos)) {
                    final IMatchBlock matchBlock = interval.getData();
                    final int segmentLength = pos - prevDataPoint.getPos() + 1;
                    if (segmentLength >= 5) {
                        final float score = matchBlock.getBitScore() * segmentLength / matchBlock.getLength();
                        final int tax = getAncestorAtRank(rank, matchBlock.getTaxonId());
                        if (tax > 0)
                            dataPoint.add(tax, score);
                    }
                }
                dataPoints.add(dataPoint);
            }
            prevDataPoint = dataPoint;
        }
        return dataPoints;
    }

    /**
     * create segments using dynamic programming
     *
     * @param readEnd
     * @param dataPoints
     * @return segments
     */
    private static ArrayList<Segment> computeSegments(ProgressListener progress, final Set<Integer> allTaxa, int readStart, int readEnd, ArrayList<DataPoint> dataPoints) throws CanceledException {
        if (allTaxa.size() == 0)
            return new ArrayList<>();

        progress.setSubtask("Running dynamic program");
        progress.setMaximum(dataPoints.size());
        progress.setProgress(0);


        final float changePenalty = 10000.0f;
        final float compatibleFactor = 1.0f;
        final float incompatibleFactor = -0.2f;

        final Map<Integer, Integer> tax2row = new HashMap<>();
        {
            int row = 0;
            for (Integer tax : allTaxa) {
                tax2row.put(tax, row++);
            }
        }

        final float[][] scoreMatrix = new float[dataPoints.size()][allTaxa.size()];
        final int[][] traceBackMatrix = new int[dataPoints.size()][allTaxa.size()];

        {
            for (int col = 1; col < dataPoints.size(); col++) { // skip the first point
                final DataPoint dataPoint = dataPoints.get(col);
                if (verbose)
                    System.err.println(String.format("DataPoint@ %,d", dataPoint.getPos()) + ":");
                for (Integer tax : allTaxa) {
                    final int row = tax2row.get(tax);

                    float maxScore = -1000000.0f;
                    int maxScoreTax = 0;

                    for (Integer otherTax : allTaxa) {
                        final int otherRow = tax2row.get(otherTax);
                        if (otherTax.equals(tax)) { // look at staying with tax
                            final float score;
                            if (dataPoint.getScore(tax) > 0) // there is an alignment using tax
                                score = scoreMatrix[col - 1][row] + compatibleFactor * dataPoint.getScore(tax);
                            else // no current alignment using tax, use the lowest scoring alternative
                                score = scoreMatrix[col - 1][row] + incompatibleFactor * dataPoint.getMinAlignmentScore();

                            if (score > maxScore) {
                                maxScore = score;
                                maxScoreTax = tax;
                            }
                        } else { // other != tax, look at switching from tax to other
                            final float score;
                            if (dataPoint.getScore(otherTax) > 0)
                                score = scoreMatrix[col - 1][otherRow] + compatibleFactor * dataPoint.getScore(otherTax) - changePenalty;
                            else
                                score = scoreMatrix[col - 1][otherRow] + incompatibleFactor * dataPoint.getMinAlignmentScore() - changePenalty;

                            if (score > maxScore) {
                                maxScore = score;
                                maxScoreTax = otherTax;
                            }
                        }
                    }
                    if (verbose)
                        System.err.println(String.format("Traceback %d (%s) %.1f from %d (%s) %.1f", tax, TaxonomyData.getName2IdMap().get(tax),
                                maxScore, maxScoreTax, TaxonomyData.getName2IdMap().get(maxScoreTax), scoreMatrix[col - 1][tax2row.get(maxScoreTax)]));

                    scoreMatrix[col][row] = maxScore;
                    traceBackMatrix[col][row] = maxScoreTax;
                }
                progress.incrementProgress();
            }
        }

        final List<Pair<Float, Integer>> bestScores;
        if (dataPoints.size() > 0)
            bestScores = computeBestScores(allTaxa, tax2row, scoreMatrix, 0.1);
        else
            bestScores = new ArrayList<>();
        if (verbose) {
            System.err.println("Best scores and taxa:");
            for (Pair<Float, Integer> pair : bestScores) {
                System.err.println(String.format("%d (%s): %.1f", pair.getSecond(), TaxonomyData.getName2IdMap().get(pair.getSecond()), pair.getFirst()));
            }
        }

        // trace back:
        ArrayList<Segment> segments = new ArrayList<>();

        if (bestScores.size() > 0) {
            int tax = bestScores.get(0).getSecond();
            int row = tax2row.get(tax);
            int col = scoreMatrix.length - 1;

            while (col > 0) {
                final DataPoint currentDataPoint = dataPoints.get(col);
                int prevCol = col - 1;
                while (prevCol > 0 && traceBackMatrix[prevCol][row] == tax)
                    prevCol--;
                final DataPoint prevDataPoint = dataPoints.get(prevCol);
                if (tax > 0)
                    segments.add(new Segment(prevDataPoint.getPos(), currentDataPoint.getPos(), tax));
                if (prevCol > 0) {
                    tax = traceBackMatrix[prevCol][row];
                    row = tax2row.get(tax);
                }
                col = prevCol;
            }
        }
        // reverse:
        segments = Basic.reverse(segments);

        System.err.println("Segments: " + Basic.toString(segments, " "));

        return segments;
    }

    /**
     * determine the best scores seen
     *
     * @param scoreMatrix
     * @param topProportion
     * @return best scores and taxa seen
     */
    private static List<Pair<Float, Integer>> computeBestScores(Set<Integer> taxa, Map<Integer, Integer> tax2row, float[][] scoreMatrix, double topProportion) {
        List<Pair<Float, Integer>> list = new ArrayList<>();

        final int col = scoreMatrix.length - 1;
        for (Integer tax : taxa) {
            list.add(new Pair<>(scoreMatrix[col][tax2row.get(tax)], tax));
        }
        if (list.size() > 1) {
            list.sort(new Comparator<Pair<Float, Integer>>() {
                @Override
                public int compare(Pair<Float, Integer> a, Pair<Float, Integer> b) {
                    if (a.getFirst() > b.getFirst())
                        return -1;
                    else if (a.getFirst() < b.getFirst())
                        return 1;
                    else
                        return a.getSecond().compareTo(b.getSecond());
                }
            });
            float bestScore = list.get(0).getFirst();
            for (int i = 1; i < list.size(); i++) {
                if (list.get(i).getFirst() < topProportion * bestScore) {
                    list = list.subList(0, i); // remove the remaining items
                    break;
                }
            }
        }
        return list;
    }


    /**
     * gets the ancestor tax id at given rank or 0
     *
     * @param targetLevel
     * @param taxonId
     * @return ancestor or 0
     */
    private static Integer getAncestorAtRank(int targetLevel, Integer taxonId) {
        // todo: cache values
        if (taxonId == 0)
            return 0;

        int ancestorId = taxonId;
        Node v = TaxonomyData.getTree().getANode(ancestorId);
        while (v != null) {
            int vLevel = TaxonomyData.getTaxonomicRank(ancestorId);
            if (vLevel == targetLevel) {
                return ancestorId;
            }
            if (v.getInDegree() > 0) {
                v = v.getFirstInEdge().getSource();
                ancestorId = (Integer) v.getInfo();
            } else
                break;
        }
        return 0;
    }

    /**
     * a segment with tax id
     */
    private static class Segment {
        final private int start;
        final private int end;
        final private int tax;

        public Segment(int start, int end, int tax) {
            this.start = start;
            this.end = end;
            this.tax = tax;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public int getTaxon() {
            return tax;
        }

        public String toString() {
            return String.format("%,d-%,d: %d (%s)", start, end, tax, TaxonomyData.getName2IdMap().get(tax));
        }
    }

    /**
     * summarizes all taxa currently alive at the end of a segment, together with their best scores
     */
    private static class DataPoint {
        // this is the data associated with the DP cell:
        private int pos;
        private final Map<Integer, Float> taxon2AlignmentScore; // todo: replace by array and rows
        private float minAlignmentScore = 0;

        public DataPoint(int pos) {
            this.pos = pos;
            taxon2AlignmentScore = new TreeMap<>();
        }

        public void add(int tax, float score) {
            if (score <= 0)
                throw new RuntimeException("Score must be positive, got: " + score); // should never happen

            final Float prev = taxon2AlignmentScore.get(tax);
            if (prev == null || prev < score)
                taxon2AlignmentScore.put(tax, score);
            if (minAlignmentScore == 0 || score < minAlignmentScore)
                minAlignmentScore = score;
        }

        public Set<Integer> getTaxa() {
            return taxon2AlignmentScore.keySet();
        }

        public Float getScore(int tax) {
            final Float value = taxon2AlignmentScore.get(tax);
            return value == null ? 0 : value;
        }

        public int getPos() {
            return pos;
        }

        public float getMinAlignmentScore() {
            return minAlignmentScore;
        }

        public String toString() {
            final StringBuilder buf = new StringBuilder(String.format("[%,d-%.1f-%d", pos, minAlignmentScore, taxon2AlignmentScore.size()));
            for (Integer tax : getTaxa()) {
                buf.append(String.format(" %d (%s)-%.1f", tax, TaxonomyData.getName2IdMap().get(tax), getScore(tax)));
            }
            buf.append("]");
            return buf.toString();
        }
    }
}

