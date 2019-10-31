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

import jloda.util.*;
import jloda.util.interval.Interval;
import jloda.util.interval.IntervalTree;
import megan.algorithms.IntervalTree4Matches;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.data.IConnector;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Uses frame-shift aware protein alignments to correct frame-shift problems in long reads
 * Daniel Huson, 8.2018
 */
public class FrameShiftCorrectedReadsExporter {
    private enum EditType {positiveFrameShift, negativeFrameShift}

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
            final String fName = fileName.replaceAll("%t", "all").replaceAll("%i", "all");

            try (BufferedWriter w = new BufferedWriter(new FileWriter(fName)); IReadBlockIterator it = connector.getAllReadsIterator(0, 10000, true, true)) {
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
                w = new BufferedWriter(fileName.endsWith(".gz") ? new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(fileName))) : new FileWriter(fileName));
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
                                    final String fName = fileName.replaceAll("%t", Basic.toCleanName(cName)).replaceAll("%i", "" + classId);
                                    final File file = new File(fName);
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

                                    w = new BufferedWriter(fName.endsWith(".gz") ? new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(fName))) : new FileWriter(fName));
                                }
                                first = false;
                            }
                            total++;
                            correctAndWrite(progress, it.next(), w);
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

        final IntervalTree<IMatchBlock> intervals = IntervalTree4Matches.computeIntervalTree(readBlock, null, null);

        // sort by decreasing bit score, then by decreasing length
        final TreeSet<Interval<IMatchBlock>> sortedIntervals = new TreeSet<>((interval1, interval2) -> {
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
        });
        sortedIntervals.addAll(intervals.getAllIntervals(false));

        final ArrayList<Edit> edits = new ArrayList<>();

        while (true) {
            final Interval<IMatchBlock> interval = sortedIntervals.pollFirst();
            if (interval == null)
                break;
            sortedIntervals.removeAll(intervals.getIntervals(interval));
            computeEdits(interval.getData(), edits);
            progress.checkForCancel();
        }

        int countPositiveFrameShifts = 0;
        int countNegativeFrameShift = 0;

        edits.sort((a, b) -> {
            if (a.pos < b.pos)
                return -1;
            else if (a.pos > b.pos)
                return 1;
            else return a.type.compareTo(b.type);
        });

        final StringBuilder buf = new StringBuilder();

        int prev = 0;
        for (Edit edit : edits) {
            final int pos = edit.getPos();

            if (pos > prev)
                buf.append(originalSequence, prev, pos);
            prev = pos;

            switch (edit.getType()) {
                case positiveFrameShift:
                    buf.append("N");
                    countPositiveFrameShifts++;
                    break;
                case negativeFrameShift:
                    buf.append("NN");
                    countNegativeFrameShift++;
                    break;
                default:
                    System.err.println("Illegal edit: " + edit);
            }
        }
        if (prev < originalSequence.length())
            buf.append(originalSequence, prev, originalSequence.length());

        final String originalHeader = readBlock.getReadHeader();
        final StringBuilder headerBuf = new StringBuilder();
        if (originalHeader == null)
            headerBuf.append(">unnamed");
        else
            headerBuf.append(originalHeader.startsWith(">") ? originalHeader.trim() : ">" + originalHeader.trim());
        headerBuf.append(String.format(" corrected+%d-%d", countPositiveFrameShifts, countNegativeFrameShift));
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
            final String queryString;
            final int start;

            if (queryStartEnd.getSecond() < queryStartEnd.getThird()) { // start < end
                start = queryStartEnd.getSecond();
                queryString = queryStartEnd.getFirst();
            } else { // end<start, so reverse alignment
                start = queryStartEnd.getThird();
                queryString = SequenceUtils.getReverse(queryStartEnd.getFirst()); // NOT reverse complement!
            }

            int pos = start - 1; // want this to be 0-based
            for (int i = 0; i < queryString.length(); i++) {
                final char ch = queryString.charAt(i);

                if (ch == '/') { // aligner postulates a positive frame shift, will have to insert one nucleotide in query fix frame
                    edits.add(new Edit(EditType.positiveFrameShift, pos - 1));
                    pos -= 1; // move back one nucleotide
                } else if (ch == '\\') { // aligner postulates a negative frame shift, will have to insert two nucleotides in query to fix frame
                    edits.add(new Edit(EditType.negativeFrameShift, pos));
                    pos += 1; // move forward one nucleotide
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
        private int pos; // 0-based position

        Edit(EditType type, int pos) {
            this.type = type;
            this.pos = pos;
        }

        EditType getType() {
            return type;
        }

        int getPos() {
            return pos;
        }

        public String toString() {
            return String.format("%s-%,d", type.toString(), pos);
        }

        public void incrementPos(int add) {
            pos += add;
        }
    }
}
