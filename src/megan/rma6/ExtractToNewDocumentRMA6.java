/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.rma6;

import jloda.util.CanceledException;
import jloda.util.ListOfLongs;
import jloda.util.ProgressListener;
import jloda.util.Single;
import megan.io.IInputReader;
import megan.io.OutputWriter;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * extract a given classification and class-ids to a new document
 * Daniel Huson, 4.2015
 */
public class ExtractToNewDocumentRMA6 {
    /**
     * extract all named classes in the given classsification to a new RMA6 file
     * @param sourceRMA6FileName
     * @param sourceClassification
     * @param sourceClassIds
     * @param targetRMA6FileName
     * @param progressListener
     * @param totalReads return the total reads extracted here
     * @throws IOException
     * @throws CanceledException
     */
    public static void apply(String sourceRMA6FileName, String sourceClassification, Collection<Integer> sourceClassIds, String targetRMA6FileName,
                             ProgressListener progressListener, Single<Long> totalReads) throws IOException, CanceledException {

        final long startTime = System.currentTimeMillis();

        final RMA6File sourceRMA6File = new RMA6File(sourceRMA6FileName, "r");
        final boolean pairedReads = sourceRMA6File.getHeaderSectionRMA6().isPairedReads();
        final String[] cNames = sourceRMA6File.getHeaderSectionRMA6().getMatchClassNames();

        // determine the set of all positions to extract:
        final ClassificationBlockRMA6 block = new ClassificationBlockRMA6(sourceClassification);
        long start = sourceRMA6File.getFooterSectionRMA6().getStartClassification(sourceClassification);
        block.read(start, sourceRMA6File.getReader());
        final ListOfLongs list = new ListOfLongs();
        for (Integer classId : sourceClassIds) {
            if (block.getSum(classId) > 0) {
                block.readLocations(start, sourceRMA6File.getReader(), classId, list);
            }
        }

        long totalMatches = 0;

        //  open the target file for writing
        try (OutputWriter writer = new OutputWriter(new File(targetRMA6FileName))) {
            final FooterSectionRMA6 footerSection = new FooterSectionRMA6();

            try { // user might cancel inside this block....
                progressListener.setTasks("Extracting", "");
                progressListener.setProgress(0);
                progressListener.setMaximum(list.size());


                // copy the file header from the source file:
                footerSection.setStartHeaderSection(0);
                sourceRMA6File.getHeaderSectionRMA6().write(writer);
                footerSection.setEndHeaderSection(writer.getPosition());

                footerSection.setStartReadsSection(writer.getPosition());

                // copy all reads that belong to the given classes of the given classification:
                try (IInputReader reader = sourceRMA6File.getReader()) {
                    for (int i = 0; i < list.size(); i++) {
                        reader.seek(list.get(i));
                        totalReads.set(totalReads.get() + 1);

                        if (pairedReads) {
                            reader.skipBytes(8); // skip over mate UID, note that we can't use it
                            writer.writeLong(0);
                        }

                        // copy read text without decompressing:
                        {
                            int length = reader.readInt();
                            writer.writeInt(length);
                            length = Math.abs(length);
                            for (int b = 0; b < length; b++) {
                                writer.write(reader.read());
                            }
                        }
                        final int numberOfMatches = reader.readInt(); // number of matches
                        writer.writeInt(numberOfMatches);
                        totalMatches += numberOfMatches;

                        // copy classifications:
                        {
                            int length = numberOfMatches * cNames.length * 4;
                            for (int b = 0; b < length; b++) {
                                writer.write(reader.read());
                            }
                        }
                        // copy matches text without decompressing:
                        {
                            int length = reader.readInt();
                            writer.writeInt(length);
                            length = Math.abs(length);
                            for (int b = 0; b < length; b++) {
                                writer.write(reader.read());
                            }
                        }
                        progressListener.incrementProgress();
                    }
                }
            } finally { // if user cancels, finish writing file before leaving...
                long position = writer.getPosition();
                footerSection.setEndReadsSection(position);


                // write the footer section:

                footerSection.setStartClassificationsSection(position);
                footerSection.setEndClassificationsSection(position);
                footerSection.setStartAuxDataSection(position);
                footerSection.setEndAuxDataSection(position);
                footerSection.setStartFooterSection(position);

                footerSection.setNumberOfReads(totalReads.get());
                footerSection.setNumberOfMatches(totalMatches);

                footerSection.write(writer);
            }
        }
        System.err.println("Extraction required " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
    }
}
