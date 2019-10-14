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
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * sort last MAF alignments
 */
public class SortLastMAFAlignmentsByQuery {
    /**
     * sort last MAF alignments
     *
     * @param args
     * @throws UsageException
     * @throws java.io.FileNotFoundException
     */
    public static void main(String[] args) throws Exception {
        try {
            ResourceManager.addResourceRoot(Megan6.class, "megan.resources");
            ProgramProperties.setProgramName("SortLastMAFAlignments");
            ProgramProperties.setProgramVersion(megan.main.Version.SHORT_DESCRIPTION);

            PeakMemoryUsageMonitor.start();
            (new SortLastMAFAlignmentsByQuery()).run(args);
            System.err.println("Total time:  " + PeakMemoryUsageMonitor.getSecondsSinceStartString());
            System.err.println("Peak memory: " + PeakMemoryUsageMonitor.getPeakUsageString());
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
        final ArgsOptions options = new ArgsOptions(args, this, "Sorts alignments in an MAF file by query");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2019 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        final String lastMAFFile = options.getOptionMandatory("-i", "input", "Input file in MAF format as produced by Last (.gz ok)", "");
        String readsFile = options.getOption("-r", "readsFile", "File containing all reads, if given, determines output order (.gz ok)", "");

        final String outputFile = options.getOption("-o", "output", "Output file (.gz ok, use 'stdout' for standard out)", "stdout");
        options.done();

        final HashMap<String, ArrayList<byte[][]>> readName2Alignments = new HashMap<>(1000000);

        final ArrayList<String> readNamesOrder = new ArrayList<>(1000000);
        final boolean orderSetFromReadsFile;

        if (readsFile.length() > 0) {
            try (IFastAIterator iterator = FastAFileIterator.getFastAOrFastQAsFastAIterator(readsFile); ProgressPercentage progress = new ProgressPercentage("Processing file: " + readsFile)) {
                progress.setMaximum(iterator.getMaximumProgress());
                while (iterator.hasNext()) {
                    readNamesOrder.add(Basic.getFirstWord(Basic.swallowLeadingGreaterSign(iterator.next().get1())));
                    progress.setProgress(iterator.getProgress());
                }
            }
            orderSetFromReadsFile = (readNamesOrder.size() > 0);
        } else
            orderSetFromReadsFile = false;

        boolean inInitialComments = true;

        long readsIn = 0;
        long readsOut = 0;
        long alignmentsIn = 0;
        long alignmentsOut = 0;

        try (FileLineIterator it = new FileLineIterator(lastMAFFile);
             BufferedWriter w = new BufferedWriter(outputFile.equals("stdout") ? new OutputStreamWriter(System.out) : new OutputStreamWriter(Basic.getOutputStreamPossiblyZIPorGZIP(outputFile)))) {

            try (ProgressPercentage progress = new ProgressPercentage("Processing file: " + lastMAFFile)) {
                progress.setMaximum(it.getMaximumProgress());
                while (it.hasNext()) {
                    String line = it.next();
                    if (line.startsWith("#")) {
                        if (inInitialComments && !line.startsWith("# batch")) {
                            w.write(line);
                            w.write('\n');
                        }
                    } else {
                        if (inInitialComments)
                            inInitialComments = false;
                        if (line.startsWith("a ")) {
                            final byte[][] alignment = new byte[3][];
                            alignment[0] = line.getBytes();
                            if (it.hasNext()) {
                                alignment[1] = it.next().getBytes();
                                if (it.hasNext()) {
                                    final String line2 = it.next();
                                    alignment[2] = line2.getBytes();
                                    alignmentsIn++;

                                    String readName = getSecondWord(line2);
                                    ArrayList<byte[][]> alignments = readName2Alignments.get(readName);
                                    if (alignments == null) {
                                        alignments = new ArrayList<>(100);
                                        readName2Alignments.put(readName, alignments);
                                        if (!orderSetFromReadsFile)
                                            readNamesOrder.add(readName);
                                        readsIn++;
                                    }
                                    alignments.add(alignment);
                                }
                            }
                        }

                    }
                    progress.setProgress(it.getProgress());
                }
            }

            try (ProgressPercentage progress = new ProgressPercentage("Writing file: " + outputFile)) {
                progress.setMaximum(readName2Alignments.keySet().size());

                Collection<String>[] order = new Collection[]{readNamesOrder, readName2Alignments.keySet()};

                // first output in order, then output any others that were not mentioned...
                for (int i = 0; i <= 1; i++) {
                    if (i == 1 && order[i].size() > 0 && orderSetFromReadsFile) {
                        System.err.println("Warning: alignments found for queries that are not mentioned in the provided reads file");
                    }
                    for (String readName : order[i]) {
                        ArrayList<byte[][]> alignments = readName2Alignments.get(readName);
                        if (alignments != null) {
                            alignments.sort((a, b) -> {
                                final int scoreA = parseScoreFromA(a[0]);
                                final int scoreB = parseScoreFromA(b[0]);
                                if (scoreA > scoreB)
                                    return -1;
                                else if (scoreA < scoreB)
                                    return 1;
                                else
                                    return 0;
                            });
                            for (byte[][] alignment : alignments) {
                                for (byte[] line : alignment) {
                                    w.write(Basic.toString(line));
                                    w.write('\n');
                                }
                                w.write('\n');
                                alignmentsOut++;
                            }
                            readsOut++;
                        }
                        readName2Alignments.remove(readName);
                        progress.incrementProgress();
                    }
                }
            }
        }

        if (alignmentsIn != alignmentsOut)
            System.err.println("Alignments: in=" + alignmentsIn + ", out=" + alignmentsOut);
        if (readsIn != readsOut)
            System.err.println("Reads: in=" + alignmentsIn + ", out=" + alignmentsOut);

        System.err.println(String.format("Alignments: %,10d", alignmentsIn));
        System.err.println(String.format("Reads      :%,10d", readsIn));
    }

    private int parseScoreFromA(byte[] s) {
        String string = Basic.toString(s);
        int a = string.indexOf('=') + 1;
        int b = a;
        while (b < s.length && !Character.isWhitespace(string.charAt(b)))
            b++;
        return Basic.parseInt(string.substring(a, b));
    }

    private String getSecondWord(String string) {
        int a = 0;
        while (a < string.length() && Character.isWhitespace(string.charAt(a))) // skip leading white space
            a++;
        while (a < string.length() && !Character.isWhitespace(string.charAt(a))) // skip first word
            a++;
        while (a < string.length() && Character.isWhitespace(string.charAt(a))) // skip separating white space
            a++;
        int b = a;
        while (b < string.length() && !Character.isWhitespace(string.charAt(b))) // find end of second word
            b++;
        if (b < string.length())
            return string.substring(a, b);
        else if (a < string.length())
            return string.substring(a);
        else
            return "";
    }
}
