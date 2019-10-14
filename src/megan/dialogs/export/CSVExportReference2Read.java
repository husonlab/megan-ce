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

import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import jloda.util.ProgressListener;
import megan.core.ClassificationType;
import megan.core.Document;
import megan.data.*;
import megan.viewer.MainViewer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * export reference sequence to reads mapping
 * Daniel Huson, 1.2013
 */
class CSVExportReference2Read {
    /**
     * export reference sequence to reads mapping
     *
     * @param viewer
     * @param file
     * @param separator
     * @param progressListener
     * @return
     * @throws IOException
     */
    public static int exportReference2ReadName(MainViewer viewer, File file, char separator, ProgressListener progressListener) throws IOException {
        int totalLines = 0;
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            Document doc = viewer.getDir().getDocument();
            IConnector connector = doc.getConnector();
            IClassificationBlock classificationBlock = connector.getClassificationBlock(ClassificationType.Taxonomy.toString());
            java.util.Collection<Integer> taxonIds = viewer.getSelectedIds();

            progressListener.setSubtask("Mapping reads to references");

            boolean eachReadOnceOnly = ProgramProperties.get("each-read-once-only", false);

            Map<String, List<String>> reference2reads = new HashMap<>();

            if (taxonIds.size() == 0) {
                try (IReadBlockIterator it = connector.getAllReadsIterator(doc.getMinScore(), doc.getMaxExpected(), true, false)) {
                    progressListener.setMaximum(it.getMaximumProgress());
                    progressListener.setProgress(0);

                    while (it.hasNext()) {
                        IReadBlock readBlock = it.next();
                        for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
                            IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                            if (matchBlock.getBitScore() >= doc.getMinScore() && matchBlock.getExpected() <= doc.getMaxExpected() &&
                                    (matchBlock.getPercentIdentity() == 0 || matchBlock.getPercentIdentity() >= doc.getMinPercentIdentity())) {
                                String reference = Basic.getFirstLine(matchBlock.getText());
                                List<String> list = reference2reads.computeIfAbsent(reference, k -> new LinkedList<>());
                                list.add(readBlock.getReadName());
                                if (eachReadOnceOnly)
                                    break;
                            }
                        }
                        progressListener.setProgress(it.getProgress());
                    }
                }
            } else {
                int maxProgress = taxonIds.size();
                progressListener.setMaximum(maxProgress);
                progressListener.setProgress(0);

                for (int id : taxonIds) {
                    if (classificationBlock.getSum(id) > 0) {
                        try (IReadBlockIterator it = connector.getReadsIterator(ClassificationType.Taxonomy.toString(), id, doc.getMinScore(), doc.getMaxExpected(), true, false)) {
                            progressListener.setMaximum(it.getMaximumProgress());
                            progressListener.setProgress(0);
                            while (it.hasNext()) {
                                final IReadBlock readBlock = it.next();
                                for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
                                    IMatchBlock matchBlock = readBlock.getMatchBlock(i);
                                    if (matchBlock.getBitScore() >= doc.getMinScore() && matchBlock.getExpected() <= doc.getMaxExpected() && matchBlock.getPercentIdentity() >= doc.getMinPercentIdentity()) {
                                        String reference = Basic.getFirstLine(matchBlock.getText());
                                        List<String> list = reference2reads.computeIfAbsent(reference, k -> new LinkedList<>());
                                        list.add(readBlock.getReadName());
                                        if (eachReadOnceOnly)
                                            break;
                                    }
                                }
                                progressListener.setProgress(it.getProgress());
                            }
                        }
                    }
                }
            }
            progressListener.setSubtask("writing");
            progressListener.setMaximum(reference2reads.size());
            progressListener.setProgress(0);
            for (String reference : reference2reads.keySet()) {
                w.write(reference + separator + Basic.toString(reference2reads.get(reference), "" + separator) + "\n");
                progressListener.incrementProgress();
                totalLines++;
            }

        } catch (CanceledException e) {
            System.err.println("USER CANCELED");
        }
        return totalLines;
    }
}
