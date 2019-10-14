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

package megan.parsers.maf;

import jloda.util.*;

import java.io.*;
import java.util.ArrayList;

/**
 * sorts a MAF file, if necessary
 * Daniel Huson, October 2017
 * todo: unfortunately, this doesn't work because alignments for a given read do not appear consecutively even within batches...
 */
public class MAFSorter {
    private static final Pair<String, String[]> start = new Pair<>("", new String[0]);
    private static final Pair<String, String[]> done = new Pair<>("", new String[0]);

    private static final String SORTED = "# Sorted";
    private static final String BATCH = "# batch";

    /**
     * apply the sorter
     *
     * @param readsFile
     * @param mafFile
     * @return name of temporary file containing all sorted alignments
     * @throws IOException
     */
    private static String apply(File readsFile, File mafFile, ProgressListener progress) throws IOException, CanceledException {
        if (isUnsorted(mafFile)) {
            String header = getHeaderLines(mafFile);
            return createSortedFile(header, readsFile, mafFile, progress);
        } else // is already sorted, so return this file
            return mafFile.getPath();
    }

    /**
     * create a sorted file
     *
     * @param fileHeader
     * @param readsFile
     * @param mafFile
     * @param progress
     * @return original file or new sorted file
     * @throws IOException
     */
    private static String createSortedFile(String fileHeader, File readsFile, File mafFile, ProgressListener progress) throws IOException, CanceledException {

        final ArrayList<Long> batchPositions = getBatchStartPositions(mafFile, progress);
        final int numberOfBatches = batchPositions.size();

        if (numberOfBatches == 1)
            return mafFile.toString();
        else
            System.err.println("Batches in MAF file: " + numberOfBatches);

        final ArrayList<BufferedReader> readers = getReaders(mafFile, batchPositions);
        final ArrayList<Pair<String, String[]>> nextMaf = new ArrayList<>();
        for (int i = 0; i < numberOfBatches; i++)
            nextMaf.add(start);

        final File outputFile = new File(mafFile.getParent(), Basic.replaceFileSuffix(mafFile.getName(), "-sorted.maf.gz"));
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(Basic.getOutputStreamPossiblyZIPorGZIP(outputFile.getPath())))) {
            w.write(fileHeader);

            try {
                progress.setSubtask("Processing batches:");
                progress.setProgress(0);
                try (FileLineIterator it = new FileLineIterator(readsFile)) {
                    progress.setProgress(it.getMaximumProgress());
                    while (it.hasNext()) {
                        final String line = it.next();
                        if (line.startsWith(">")) {
                            final String name = Basic.getFirstWord(line.substring(1));
                            for (int i = 0; i < numberOfBatches; i++) {
                                if (nextMaf.get(i) == start) {
                                    nextMaf.set(i, readNextMafRecord(readers.get(i)));
                                }
                                while (nextMaf.get(i).getFirst().equals(name)) {
                                    w.write("\n");
                                    w.write(Basic.toString(nextMaf.get(i).getSecond(), "\n"));
                                    w.write("\n");
                                    nextMaf.set(i, readNextMafRecord(readers.get(i)));
                                }
                            }
                        }
                        progress.setProgress(it.getProgress());
                    }
                }
                if (progress instanceof ProgressPercentage) {
                    ((ProgressPercentage) progress).reportTaskCompleted();
                }
                for (int i = 0; i < nextMaf.size(); i++) {
                    final Pair<String, String[]> pair = nextMaf.get(i);
                    if (pair != done) {
                        System.err.println("Error: not all MAF records in batch " + i + " processed, e.g:\n" + Basic.toString(pair.getSecond(), "\n"));
                    }
                }
            } finally {
                for (BufferedReader r : readers) {
                    try {
                        r.close();
                    } catch (IOException ex) {
                        Basic.caught(ex);
                    }
                }
            }
        }
        return outputFile.getPath();
    }

    /**
     * gets the next MAF record
     * Format:
     * a score=159 EG2=1e-08 E=4.3e-17
     * s WP_005682092.1                       18 33 + 516 SAEANENERRWNDDKIDRKNQDSTNNYDKTRMK
     * s HISEQ:457:C5366ACXX:2:1101:2641:2226  1 99 + 100 TAEANENERHWNDDKIERKNQDPTNHYDKSRMR
     * *
     *
     * @param r
     * @return
     */
    private static Pair<String, String[]> readNextMafRecord(BufferedReader r) throws IOException {
        String aLine;
        while ((aLine = r.readLine()) != null) {
            if (aLine.startsWith("a")) {
                String s2 = r.readLine();
                String s3 = r.readLine();
                String name = Basic.getFirstWord(s3.substring((2)));
                return new Pair<>(name, new String[]{aLine, s2, s3});
            } else if (aLine.startsWith(BATCH))
                break;
        }
        return done;
    }

    /**
     * get the header lines from a maf file
     *
     * @param mafFile
     * @return header lines
     * @throws IOException
     */
    private static String getHeaderLines(File mafFile) throws IOException {
        final StringBuilder buf = new StringBuilder();
        try (FileLineIterator it = new FileLineIterator(mafFile)) {
            while (it.hasNext()) {
                final String aLine = it.next();
                if (aLine.startsWith("#")) {
                    if (!aLine.startsWith(BATCH))
                        buf.append(aLine).append("\n");
                } else
                    break;
            }
        }
        buf.append(SORTED).append("\n");
        return buf.toString();
    }

    /**
     * determines whether this is an unsorted file
     *
     * @param mafFile
     * @return true, if not sorted
     * @throws IOException
     */
    private static boolean isUnsorted(File mafFile) throws IOException {
        try (FileLineIterator it = new FileLineIterator(mafFile)) {
            while (it.hasNext()) {
                String line = it.next();
                if (line.startsWith("#")) {
                    if (line.equals(SORTED))
                        return false;
                    if (line.startsWith(BATCH))
                        return true; // is unsorted
                } else
                    return false;
            }
        }
        return true;
    }

    /**
     * locate all batch start positions
     *
     * @param mafFile
     * @return
     * @throws IOException
     */
    private static ArrayList<Long> getBatchStartPositions(File mafFile, ProgressListener progress) throws IOException, CanceledException {
        progress.setSubtask("Determining batches in MAF file");
        final ArrayList<Long> batchStartPositions = new ArrayList<>();

        try (FileLineIterator it = new FileLineIterator(mafFile)) {
            progress.setMaximum(it.getMaximumProgress());
            progress.setProgress(0);
            while (it.hasNext()) {
                String aLine = it.next();
                if (aLine.startsWith(BATCH))
                    batchStartPositions.add(it.getPosition() + BATCH.length());
                progress.setProgress(it.getProgress());
            }
        }
        if (progress instanceof ProgressPercentage) {
            ((ProgressPercentage) progress).reportTaskCompleted();
        }
        return batchStartPositions;
    }

    /**
     * get a reader for each given position
     *
     * @param mafFile
     * @param positions
     * @return readers
     * @throws IOException
     */
    private static ArrayList<BufferedReader> getReaders(File mafFile, ArrayList<Long> positions) throws IOException {
        final ArrayList<BufferedReader> readers = new ArrayList<>(positions.size());

        for (Long position : positions) {
            FileInputStream ins = new FileInputStream(mafFile);
            long skipped = 0;
            while (skipped < position) {
                skipped += ins.skip(position - skipped);
            }
            readers.add(new BufferedReader(new InputStreamReader(ins)));
        }
        return readers;
    }

    public static void main(String[] args) throws Exception {
        String mafFile = "/Users/huson/data/long-reads/nus-march2017/Anammox-R4-MinION.maf";
        String readsFile = "/Users/huson/data/long-reads/nus-march2017/Anammox-R4-MinION.fasta";

        String result = apply(new File(readsFile), new File(mafFile), new ProgressPercentage());

        System.err.println("Result: " + result);
    }
}
