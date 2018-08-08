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

import jloda.util.*;
import megan.algorithms.IntervalTree4Matches;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.data.IConnector;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.util.interval.Interval;
import megan.util.interval.IntervalTree;

import java.io.*;
import java.util.*;

/**
 * Uses frame-shift aware protein alignments to "correct" problems in long reads
 * Daniel Huson, 8.2018
 */
public class FrameShiftCorrectedReadsExporter {
    private static enum EditType {frameShiftRight, frameShiftLeft}

    /**
     * export all matches in file
     *
     * @param connector
     * @param fileName
     * @param progress
     * @throws IOException
     */
    public static int exportAll(IConnector connector, String fileName, ProgressListener progress) throws IOException {
        int total = 0;
        try {
            progress.setTasks("Export", "Writing all corrected reads");

            try (BufferedWriter w = new BufferedWriter(new FileWriter(fileName))) {
                final IReadBlockIterator it = connector.getAllReadsIterator(0, 10000, true, true);
                progress.setMaximum(it.getMaximumProgress());
                progress.setProgress(0);
                while (it.hasNext()) {
                    total++;
                    correctAndWrite(progress, it.next(), w);
                    progress.setProgress(it.getProgress());
                }
            }
        } catch (CanceledException ex) {
            System.err.println("USER CANCELED");
        }
        return total;
    }

    /**
     * export all reads for given set of classids in the given classification
     *
     * @param classificationName
     * @param classIds
     * @param connector
     * @param fileName
     * @param progress
     * @throws IOException
     * @throws CanceledException
     */
    public static int export(String classificationName, Collection<Integer> classIds, IConnector connector, String fileName, ProgressListener progress) throws IOException {
        int total = 0;
        try {
            progress.setTasks("Export", "Writing selected corrected reads");

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
            for (Integer classId : classIds) {
                countClassIds++;

                boolean first = true;

                final IReadBlockIterator it = connector.getReadsIterator(classificationName, classId, 0, 10000, true, true);
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
                    correctAndWrite(progress, it.next(), w);
                    progress.setProgress((long) (100000.0 * (countClassIds + (double) it.getProgress() / it.getMaximumProgress())));
                }
            }
            if (w != null)
                w.close();
        } catch (CanceledException ex) {
            System.err.println("USER CANCELED");
        }
        return total;
    }

    /**
     * correct and write a read
     *
     * @param readBlock
     * @param w
     * @return number of reads written
     * @throws IOException
     */
    private static void correctAndWrite(ProgressListener progress, IReadBlock readBlock, Writer w) throws IOException, CanceledException {
        final Pair<String, String> headerAndSequence = correctFrameShiftsInSequence(progress, readBlock);
        if (headerAndSequence != null) {
            w.write(headerAndSequence.getFirst());
            w.write("\n");
            w.write(Basic.foldHard(headerAndSequence.getSecond(), 140));
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
    private static Pair<String, String> correctFrameShiftsInSequence(ProgressListener progress, IReadBlock readBlock) throws CanceledException {
        final String originalSequence = readBlock.getReadSequence();
        if (originalSequence == null)
            return null;

        final IntervalTree<IMatchBlock> intervals = IntervalTree4Matches.computeIntervalTree(readBlock, null);

        final SortedSet<Interval<IMatchBlock>> sortedIntervals = new TreeSet<>(new Comparator<Interval<IMatchBlock>>() { // sort by decreasing bit score, then by decreasing length
            @Override
            public int compare(Interval<IMatchBlock> interval1, Interval<IMatchBlock> interval2) {
                final IMatchBlock matchBlock1 = interval1.getData();
                final IMatchBlock matchBlock2 = interval2.getData();

                if (matchBlock1.getBitScore() > matchBlock2.getBitScore())
                    return -1;
                else if (matchBlock1.getBitScore() < matchBlock2.getBitScore())
                    return 1;
                else if (interval1.length() > interval2.length())
                    return -1;
                else if (interval1.length() < interval2.length())
                    return 1;
                else
                    return matchBlock1.getTextFirstWord().compareTo(matchBlock2.getTextFirstWord());
            }
        });
        sortedIntervals.addAll(intervals.getAllIntervals(false));

        final ArrayList<Edit> edits = new ArrayList<>();

        while (true) {
            final Interval<IMatchBlock> interval = ((TreeSet<Interval<IMatchBlock>>) sortedIntervals).pollFirst();
            if (interval == null)
                break;
            sortedIntervals.removeAll(intervals.getIntervals(interval));
            computeEdits(interval.getData(), edits);
            progress.checkForCancel();
        }

        edits.sort(new Comparator<Edit>() {
            @Override
            public int compare(Edit a, Edit b) {
                if (a.pos > b.pos)
                    return -1;
                else if (a.pos < b.pos)
                    return 1;
                else return a.type.compareTo(b.type);
            }
        });

        int countInsertions = 0;
        int countDeletions = 0;

        int prevPos = -1;

        // apply edits: // todo: need to implement this more efficiently!
        final StringBuilder buf = new StringBuilder(originalSequence);
        for (Edit edit : edits) { // we are going through edits from back of sequence to front, so that positions stay valid
            switch (edit.getType()) {
                case frameShiftRight: // insert an extra nucleotide
                    buf.insert(edit.getPos(), 'N');
                    countInsertions++;
                    break;
                case frameShiftLeft: // remove the current nucleotide
                    buf.deleteCharAt(edit.getPos());
                    countDeletions++;
                    break;
            }
            if (edit.getPos() == prevPos)
                System.err.println("Warning: FrameShiftCorrection: multiple edits at same position: " + edit.getPos());
            else
                prevPos = edit.getPos();
        }

        final String originalHeader = readBlock.getReadHeader();
        final StringBuilder headerBuf = new StringBuilder();
        if (originalHeader == null)
            headerBuf.append(">unnamed");
        else
            headerBuf.append(originalHeader.startsWith(">") ? originalHeader.trim() : ">" + originalHeader.trim());
        headerBuf.append(String.format("|corrected+%d-%d", countInsertions, countDeletions));
        return new Pair<>(headerBuf.toString(), buf.toString());
    }

    /**
     * computes the edits implied by a match
     *
     * @param matchBlock
     * @param edits
     */
    private static void computeEdits(IMatchBlock matchBlock, ArrayList<Edit> edits) {
        final Triplet<String, Integer, Integer> queryStartEnd = getQuery(matchBlock.getText());
        if (queryStartEnd != null) {
            final int start;
            final String queryString;
            if (queryStartEnd.getSecond() < queryStartEnd.getThird()) { // start < end
                start = queryStartEnd.getSecond();
                queryString = queryStartEnd.getFirst();
            } else { // end<start, so reverse alignment
                start = queryStartEnd.getThird();
                queryString = SequenceUtils.getReverse(queryStartEnd.getFirst()); // not reverse complement!
            }

            int pos = start - 1; // want this to be 0-based
            for (int i = 0; i < queryString.length(); i++) {
                char ch = queryString.charAt(i);
                if (ch == '/') { // something needs to inserted to query for the alignment to work, so frame-shift right
                    edits.add(new Edit(EditType.frameShiftRight, pos - 1)); // minus 1 because insertion happens after pos
                    pos -= 1; // move back one nucleotide
                } else if (ch == '\\') { // // something needs to deleted from query for the alignment to work, so frame-shift left
                    edits.add(new Edit(EditType.frameShiftLeft, pos));
                    pos += 1; // deletion: move forward nucleotide
                } else if (ch != '-') { // if not a gap or a frame-shift, move along 3 nucleotides in the original sequence
                    pos += 3;
                }
            }
        }
    }

    /**
     * parses the match text to extract the query alignment string and start and end positions
     *
     * @param matchText
     * @return query string, nucleotide start and end positions
     */
    private static Triplet<String, Integer, Integer> getQuery(String matchText) {
        final StringBuilder buf = new StringBuilder();
        String startString = null;
        String endString = null;

        try (final BufferedReader reader = new BufferedReader(new StringReader(matchText))) {
            String aLine;
            while ((aLine = reader.readLine()) != null) {
                aLine = aLine.trim();
                if (aLine.startsWith("Query:")) {
                    final String[] tokens = Basic.splitOnWhiteSpace(aLine);
                    if (startString == null)
                        startString = tokens[1];
                    buf.append(tokens[2]);
                    endString = tokens[3];
                }
            }
        } catch (IOException e) {
            Basic.caught(e); // can't happen
        }
        if (startString != null && endString != null)
            return new Triplet<>(buf.toString(), Integer.parseInt(startString), Integer.parseInt(endString));
        else
            return null;
    }

    /**
     * a sequence edit
     */
    private static class Edit {
        private final EditType type;
        private final int pos; // 0-based position

        public Edit(EditType type, int pos) {
            this.type = type;
            this.pos = pos;
        }

        public EditType getType() {
            return type;
        }

        public int getPos() {
            return pos;
        }
    }
}
