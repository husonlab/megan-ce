/*
 *  Copyright (C) 2017 Daniel H. Huson
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

import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.FileInputIterator;
import jloda.util.ProgressListener;

import java.io.*;
import java.util.ArrayList;

/**
 * sorts a MAF file, if necessary
 * Created by huson on 4/10/17.
 */
public class MAFSorter {
    /**
     * apply the sorter
     *
     * @param readsFile
     * @param mafFile
     * @return name of temporary file containing all sorted alignments
     * @throws IOException
     */
    public static String apply(File readsFile, File mafFile, ProgressListener progress) throws IOException, CanceledException {
        if (isUnsorted(mafFile)) {
            final File outputFile = File.createTempFile(mafFile.getParent(), Basic.replaceFileSuffix(mafFile.getName(), "-tmp.maf.gz"));

            final ArrayList<File> tmpFiles = createTmpFiles(mafFile, progress);

            return createSortedFile(readsFile, mafFile, progress);
        } else // is already sorted, so return this file
            return mafFile.getPath();
    }


    /**
     * create tmp files
     *
     * @param mafFile
     * @param progress
     * @return
     * @throws IOException
     * @throws CanceledException
     */
    private static ArrayList<File> createTmpFiles(File mafFile, ProgressListener progress) throws IOException, CanceledException {

        ArrayList<File> tmpFiles = new ArrayList();

        BufferedWriter currentTmpWriter = null;
        try (FileInputIterator it = new FileInputIterator(mafFile)) {
            progress.setMaximum(it.getMaximumProgress());
            progress.setProgress(0);
            boolean initialHeader = true;
            while (it.hasNext()) {
                String line = it.next();
                if (line.startsWith("#")) {
                    if (line.startsWith("# batch ")) {
                        if (currentTmpWriter != null) {
                            currentTmpWriter.close();
                        }
                        final Integer number = Basic.parseInt(line.substring("# batch ".length()));
                        File file = File.createTempFile(mafFile.getParent(), Basic.replaceFileSuffix(mafFile.getName(), String.format("-tmp%03d,gz", number)));
                        currentTmpWriter = new BufferedWriter(new OutputStreamWriter(Basic.getOutputStreamPossiblyZIPorGZIP(file.getPath())));
                        tmpFiles.add(file);
                    }
                } else {
                    if (initialHeader)
                        initialHeader = false;
                    if (currentTmpWriter != null) {
                        currentTmpWriter.write(line);
                        currentTmpWriter.write("\n");
                    }
                }
            }
            progress.setProgress(it.getProgress());
        }

        if (currentTmpWriter != null) {
            currentTmpWriter.close();
        }
        return tmpFiles;
    }

    private static String createSortedFile(File readsFile, File mafFile, ProgressListener progress) throws IOException, CanceledException {
        final File outputFile = File.createTempFile(mafFile.getParent(), Basic.replaceFileSuffix(mafFile.getName(), "-tmp.maf.gz"));

        ArrayList<File> tmpFiles = new ArrayList();
        BufferedWriter currentTmpWriter = null;

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Basic.getOutputStreamPossiblyZIPorGZIP(outputFile.getPath())))) {
            try (FileInputIterator it = new FileInputIterator(mafFile)) {
                progress.setMaximum(it.getMaximumProgress());
                progress.setProgress(0);
                boolean initialHeader = true;
                while (it.hasNext()) {
                    String line = it.next();
                    if (line.startsWith("#")) {
                        if (line.startsWith("# batch ")) {
                            if (currentTmpWriter != null) {
                                currentTmpWriter.close();
                            }
                            final Integer number = Basic.parseInt(line.substring("# batch ".length()));
                            File file = File.createTempFile(mafFile.getParent(), Basic.replaceFileSuffix(mafFile.getName(), String.format("-tmp%03d,gz", number)));
                            currentTmpWriter = new BufferedWriter(new OutputStreamWriter(Basic.getOutputStreamPossiblyZIPorGZIP(file.getPath())));
                            tmpFiles.add(file);
                        } else if (initialHeader) {
                            writer.write(line);
                            writer.write("\n");
                        }
                    } else {
                        if (initialHeader)
                            initialHeader = false;
                        if (currentTmpWriter != null) {
                            currentTmpWriter.write(line);
                            currentTmpWriter.write("\n");
                        }
                    }
                }
                progress.setProgress(it.getProgress());
            }
        }
        if (currentTmpWriter != null) {
            currentTmpWriter.close();
        }
        return outputFile.getPath();
    }

    private static boolean isUnsorted(File mafFile) throws IOException {
        try (FileInputIterator it = new FileInputIterator(mafFile)) {
            while (it.hasNext()) {
                String line = it.next();
                if (line.startsWith("#")) {
                    if (line.startsWith("# batch"))
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
    public static ArrayList<Long> getBatchStartPositions(File mafFile) throws IOException {
        ArrayList<Long> batchStartPositions = new ArrayList<>();

        try (FileInputIterator it = new FileInputIterator(mafFile)) {
            while (it.hasNext()) {
                String aLine = it.next();
                if (aLine.startsWith("# batch"))
                    batchStartPositions.add(it.getPosition());
            }
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
    private static BufferedReader[] getReaders(File mafFile, ArrayList<Long> positions) throws IOException {
        BufferedReader[] readers = new BufferedReader[positions.size()];

        for (int i = 0; i < positions.size(); i++) {
            FileInputStream ins = new FileInputStream(mafFile);
            long skipped = 0;
            while (skipped < positions.get(i)) {
                skipped += ins.skip(positions.get(i) - skipped);
            }
            readers[i] = new BufferedReader(new InputStreamReader(ins));
        }
        return readers;
    }
}
