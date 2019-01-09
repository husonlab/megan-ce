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

import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.data.IConnector;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

/**
 * export all reads  to a file (or those associated with the set of selected taxa, if any selected)
 * Daniel Huson, 6.2010
 */
public class ReadsExporter {
    /**
     * export all matches in file
     *
     * @param connector
     * @param fileName
     * @param progressListener
     * @throws java.io.IOException
     */
    public static int exportAll(IConnector connector, String fileName, ProgressListener progressListener) throws IOException {
        int total = 0;
        try {
            progressListener.setTasks("Export", "Writing all reads");

            try (BufferedWriter w = new BufferedWriter(new FileWriter(fileName)); IReadBlockIterator it = connector.getAllReadsIterator(0, 10000, true, false)) {
                progressListener.setMaximum(it.getMaximumProgress());
                progressListener.setProgress(0);
                while (it.hasNext()) {
                    total++;
                    write(it.next(), w);
                    progressListener.setProgress(it.getProgress());
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
     * @param classification
     * @param classIds
     * @param connector
     * @param fileName
     * @param progressListener
     * @throws java.io.IOException
     * @throws jloda.util.CanceledException
     */
    public static int export(String classification, Collection<Integer> classIds, IConnector connector, String fileName, ProgressListener progressListener) throws IOException, CanceledException {
        int total = 0;
        try {
            progressListener.setTasks("Export", "Writing selected reads");

            try (BufferedWriter w = new BufferedWriter(new FileWriter(fileName))) {
                int maxProgress = 100000 * classIds.size();
                int currentProgress;
                progressListener.setMaximum(maxProgress);
                progressListener.setProgress(0);
                int countClassIds = 0;
                for (Integer classId : classIds) {
                    countClassIds++;
                    currentProgress = 100000 * countClassIds;
                    try (IReadBlockIterator it = connector.getReadsIterator(classification, classId, 0, 10000, true, false)) {
                        long progressIncrement = 100000 / (it.getMaximumProgress() + 1);

                        while (it.hasNext()) {
                            total++;
                            write(it.next(), w);
                            progressListener.setProgress(currentProgress);
                            currentProgress += progressIncrement;
                        }
                    }
                }
            }
        } catch (CanceledException ex) {
            System.err.println("USER CANCELED");
        }
        return total;
    }

    /**
     * write the read
     *
     * @param readBlock
     * @param w
     * @return number of reads written
     * @throws java.io.IOException
     */
    private static void write(IReadBlock readBlock, Writer w) throws IOException {
        String header = readBlock.getReadHeader();
        if (header != null) {
            if (!header.startsWith(">"))
                w.write(">");
            w.write(header);
            if (!header.endsWith("\n"))
                w.write("\n");
        } else
            w.write(">null\n");
        String sequence = readBlock.getReadSequence();
        if (sequence != null) {
            if (sequence.endsWith("\n\n")) {
                w.write(sequence.substring(0, sequence.length() - 1));
            } else {
                w.write(sequence);
                if (!sequence.endsWith("\n"))
                    w.write("\n");
            }
        } else
            w.write("null\n");
    }
}
