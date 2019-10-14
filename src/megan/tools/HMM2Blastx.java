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
package megan.tools;

import jloda.swing.util.ArgsOptions;
import jloda.swing.util.ResourceManager;
import jloda.util.*;
import megan.main.Megan6;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * converts HMM output to BLASTX-like output
 */
public class HMM2Blastx {
    public enum EXPECTING {NextRefOrQuery, NextReference, NextQuery, Score, DomainAlignment}

    /**
     * converts the file
     *
     * @param args
     * @throws jloda.util.UsageException
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws Exception {
        try {
            ResourceManager.addResourceRoot(Megan6.class, "megan.resources");
            ProgramProperties.setProgramName("HMM2BlastX");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            long start = System.currentTimeMillis();
            (new HMM2Blastx()).run(args);
            System.err.println("Time: " + ((System.currentTimeMillis() - start) / 1000) + "s");
            System.exit(0);
        } catch (Exception ex) {
            Basic.caught(ex);
            System.exit(1);
        }
    }

    /**
     * run the program
     *
     * @param args
     */
    private void run(String[] args) throws Exception {
        final ArgsOptions options = new ArgsOptions(args, this, "Converts HMM output to BLASTX");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2019 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        final String[] inputFiles = options.getOptionMandatory("-i", "input", "HMM  files", new String[0]);
        final String[] readsFiles = options.getOption("-r", "reads", "Reads files (to determine order of output)", new String[0]);
        final String outputFileName = options.getOption("-o", "output", "Output file", "");
        final float minScore = options.getOption("-ms", "minScore", "Minimum bit score", 0);
        final int maxMatchesPerRead = options.getOption("-ma", "maxAlignmentsPerRead", "Maximum number of alignments per read", 25);
        final boolean reportNoHits = options.getOption("nh", "reportNoHits", "Report reads with no hits", false);
        options.done();

        final List<String> reads = new LinkedList<>();
        final Map<String, Integer> read2length = new HashMap<>();
        if (readsFiles != null) {
            for (String readsFile : readsFiles) {
                try (IFastAIterator it = FastAFileIterator.getFastAOrFastQAsFastAIterator(readsFile)) {
                    final ProgressPercentage progress = new ProgressPercentage("Parsing file: " + readsFile, it.getMaximumProgress());
                    while (it.hasNext()) {
                        Pair<String, String> pair = it.next();
                        String name = Basic.getFirstWord(Basic.swallowLeadingGreaterSign(pair.get1()));
                        reads.add(name);
                        read2length.put(Basic.getFirstWord(Basic.swallowLeadingGreaterSign(pair.get1())), pair.get2().length());
                        progress.setProgress(it.getProgress());
                    }
                    progress.close();
                }
            }
            System.err.println(String.format("Reads:   %,9d", reads.size()));
        }

        final Map<String, SortedSet<Pair<Float, String>>> query2alignments = new HashMap<>();

        int countReferences = 0;
        int countQueries = 0;
        int countAlignments = 0;

        for (String inputFile : inputFiles) {
            try (final FileLineIterator it = new FileLineIterator(inputFile)) {
                final ProgressPercentage progress = new ProgressPercentage("Parsing file: " + inputFile, it.getMaximumProgress());

                EXPECTING state = EXPECTING.NextRefOrQuery;

                String referenceName = null;
                String queryName = null;
                int frame = 0;
                float score = 0;
                float expected = 0;

                while (it.hasNext()) {
                    String aLine = it.next().trim();

                    if (state == EXPECTING.NextRefOrQuery) {
                        if (aLine.startsWith("Query:"))
                            state = EXPECTING.NextReference;
                        else if (aLine.startsWith(">>"))
                            state = EXPECTING.NextQuery;
                    }

                    switch (state) {
                        case NextRefOrQuery:
                            break;
                        case NextReference:
                            if (aLine.startsWith("Query:")) { // yes, queries are references...
                                referenceName = Basic.getWordAfter("Query:", aLine);
                                state = EXPECTING.NextQuery;
                                countReferences++;
                            }
                            break;
                        case NextQuery:
                            if (aLine.startsWith(">>")) {
                                queryName = Basic.getWordAfter(">>", aLine);
                                frame = getFrameFromSuffix(Objects.requireNonNull(queryName));
                                queryName = removeFrameSuffix(queryName);
                                state = EXPECTING.Score;
                                countQueries++;
                            }
                            break;
                        case Score:
                            if (aLine.contains(" score:")) {
                                score = Basic.parseFloat(Basic.getWordAfter(" score:", aLine));
                                if (aLine.contains(" E-value:"))
                                    expected = Basic.parseFloat(Basic.getWordAfter(" E-value:", aLine));
                                else
                                    throw new IOException("Couldn't find E-value in: " + aLine);
                                state = EXPECTING.DomainAlignment;
                                countAlignments++;
                            }
                            break;
                        case DomainAlignment:
                            if (aLine.endsWith("RF"))
                                aLine = it.next().trim();

                            /*
                              xxxxxxxxxxxxxxxxxx....... RF
           RNA_pol_Rpb2_1 134 GtFIInGtERVvvsQehrspgvffd 158
                              GtF+InGtERV+vsQ+hrspgvffd
  SRR172902.5536465_RF1.0   1 GTFVINGTERVIVSQLHRSPGVFFD 25
                              9***********************7 PP
                             */

                            int queryStart;
                            int queryEnd;
                            int refStart;
                            int refEnd;

                            String refAligned;
                            String midAligned;
                            String queryAligned;

                        {
                            final String[] refTokens = aLine.split("\\s+");
                            if (refTokens.length != 4)
                                throw new IOException("Expected 4 tokens, got: " + refTokens.length + ": " + aLine);
                            if (!refTokens[0].equals(referenceName))
                                throw new IOException("Ref expected, got: " + aLine);
                            refStart = Basic.parseInt(refTokens[1]);
                            refAligned = refTokens[2];
                            refEnd = Basic.parseInt(refTokens[3]);
                        }
                        {
                            midAligned = it.next().trim();
                        }
                        {
                            aLine = it.next().trim();
                            final String[] queryTokens = aLine.split("\\s+");
                            if (queryTokens.length != 4)
                                throw new IOException("Expected 4 tokens, got: " + queryTokens.length);
                            if (!removeFrameSuffix(queryTokens[0]).equals(queryName))
                                throw new IOException("Query expected, got: " + aLine);
                            queryStart = Basic.parseInt(queryTokens[1]);
                            queryAligned = queryTokens[2];
                            queryEnd = Basic.parseInt(queryTokens[3]);
                        }

                        if (score >= minScore) {
                            String blastString = makeBlastXAlignment(referenceName, score, expected, queryAligned, midAligned, refAligned, queryStart, queryEnd, refStart, refEnd, frame, read2length.get(queryName));

                            SortedSet<Pair<Float, String>> alignments = query2alignments.computeIfAbsent(queryName, k -> new TreeSet<>((o1, o2) -> {
                                if (o1.get1() > o2.get1())
                                    return -1;
                                else if (o1.get1() < o2.get1())
                                    return 1;
                                else return o1.get2().compareTo(o2.get2());
                            }));
                            if (alignments.size() == maxMatchesPerRead) {
                                if (score >= alignments.last().get1()) {
                                    alignments.add(new Pair<>(score, blastString));
                                    alignments.remove(alignments.last());
                                }
                            } else
                                alignments.add(new Pair<>(score, blastString));
                        }
                        state = EXPECTING.NextRefOrQuery;

                        break;
                        default:
                            throw new IOException("Invalid case: " + state);
                    }
                    progress.setProgress(it.getProgress());
                }
                progress.close();
            }
        }
        System.err.println(String.format("HMMs:    %,9d", countReferences));
        System.err.println(String.format("Reads:   %,9d", countQueries));
        System.err.println(String.format("Matches:%,10d", countAlignments));

        final Collection<String> queryNames;
        if (reads.size() > 0)
            queryNames = reads;
        else
            queryNames = query2alignments.keySet();

        int countAlignmentsWritten = 0;
        ProgressPercentage progress = new ProgressPercentage("Writing: " + outputFileName, queryNames.size());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            writer.write("BLASTX from HMM using " + Basic.getShortName(this.getClass()) + "\n\n");

            for (String queryName : queryNames) {
                Set<Pair<Float, String>> alignments = query2alignments.get(queryName);
                if (alignments == null || alignments.size() == 0) {
                    if (reportNoHits) {
                        writer.write("Query= " + queryName + "\n\n");
                        writer.write(" ***** No hits found ******\n\n");
                    }
                } else {
                    writer.write("Query= " + queryName + "\n\n");
                    for (Pair<Float, String> pair : alignments) {
                        writer.write(pair.get2());
                        countAlignmentsWritten++;
                    }
                }
            }
            progress.incrementProgress();
        }
        progress.close();
        System.err.println(String.format("Written:%,10d", countAlignmentsWritten));
    }

    /**
     * make a blast match text
     *
     * @param referenceName
     * @param score
     * @param expected
     * @param queryAligned
     * @param midAligned
     * @param refAligned
     * @param queryStart
     * @param queryEnd
     * @param refStart
     * @param refEnd
     * @param frame
     * @return blast match text
     */
    private String makeBlastXAlignment(String referenceName, float score, float expected, String queryAligned, String midAligned, String refAligned, int queryStart, int queryEnd, int refStart, int refEnd, int frame, Integer queryLength) throws IOException {
        queryAligned = queryAligned.toUpperCase();
        midAligned = midAligned.toUpperCase();
        refAligned = refAligned.toUpperCase();

        if (frame < 0)
            throw new IOException("Illegal: frame=" + frame);

        if (frame > 0) {
            frame = (frame <= 3 ? frame : 3 - frame);
            if (queryLength != null) {
                if (frame > 0) {
                    queryStart = 3 * (queryStart - 1) + 1 + (frame - 1);
                    queryEnd = 3 * (queryEnd) + (frame - 1);
                } else // frame <0
                {
                    queryStart = queryLength - 3 * (queryStart - 1) + (frame + 1);
                    queryEnd = queryLength - 3 * queryEnd + 1 + (frame + 1);
                }
            }
        }

        if (frame == -2 && queryEnd == 0) { // remove last letter from alignment:
            queryAligned = queryAligned.substring(0, queryAligned.length() - 1);
            midAligned = midAligned.substring(0, midAligned.length() - 1);
            refAligned = refAligned.substring(0, refAligned.length() - 1);
            queryEnd = 1;
        }
        if (frame == 2 && queryLength != null && queryEnd == queryLength + 1) {
            queryAligned = queryAligned.substring(0, queryAligned.length() - 1);
            midAligned = midAligned.substring(0, midAligned.length() - 1);
            refAligned = refAligned.substring(0, refAligned.length() - 1);
            queryEnd--;
        }

        StringBuilder buf = new StringBuilder();

        buf.append(">").append(referenceName).append("\n Length = -1\n\n");
        buf.append(String.format(" Score = %.1f (0), Expect = %g\n", score, expected));
        int[] identities = computeIdentities(midAligned);
        int[] positives = computePositives(midAligned);
        int[] gaps = computeGaps(queryAligned, refAligned, midAligned);
        buf.append(String.format(" Identities = %d/%d (%d%%), Positives = %d/%d (%d%%), Gaps = %d/%d (%d%%)\n",
                identities[0], identities[1], identities[2], positives[0], positives[1], positives[2], gaps[0], gaps[1], gaps[2]));
        buf.append(String.format(" Frame = %+d\n", frame));
        buf.append("\n");
        buf.append(String.format("Query: %8d %s %d\n", queryStart, queryAligned, queryEnd));
        buf.append(String.format("                %s\n", midAligned));
        buf.append(String.format("Sbjct: %8d %s %d\n", refStart, refAligned, refEnd));
        buf.append("\n");

        return buf.toString();
    }

    private int[] computeIdentities(String midLine) {
        int count = 0;
        for (int i = 0; i < midLine.length(); i++) {
            if (Character.isLetter(midLine.charAt(i)))
                count++;
        }
        return new int[]{count, midLine.length(), (int) Math.round(100.0 * count / midLine.length())};
    }

    private int[] computePositives(String midLine) {
        int count = 0;
        for (int i = 0; i < midLine.length(); i++) {
            if (midLine.charAt(i) != ' ')
                count++;
        }
        return new int[]{count, midLine.length(), (int) Math.round(100.0 * count / midLine.length())};
    }

    private int[] computeGaps(String queryAligned, String refAligned, String midLine) {
        int count = 0;
        for (int i = 0; i < queryAligned.length(); i++) {
            if (queryAligned.charAt(i) == '-')
                count++;
        }
        for (int i = 0; i < refAligned.length(); i++) {
            if (refAligned.charAt(i) == '-')
                count++;
        }
        return new int[]{count, midLine.length(), (int) Math.round(100.0 * count / midLine.length())};
    }

    /**
     * get the frame, or -1, if not defined
     *
     * @param query
     * @return frame 1-6 or -1
     */
    private int getFrameFromSuffix(String query) {
        int pos = query.indexOf("_RF");
        if (pos != -1)
            return Basic.parseInt(query.substring(pos + 3));
        else
            return -1;
    }

    /**
     * remove frame suffix
     *
     * @param query
     * @return query without frame suffix
     */
    private String removeFrameSuffix(String query) {
        int pos = query.indexOf("_RF");
        if (pos != -1)
            return query.substring(0, pos);
        else
            return query;
    }
}
