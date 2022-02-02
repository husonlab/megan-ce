/*
 * RMA2Modifier.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.rma2;

import megan.io.InputOutputReaderWriter;
import megan.io.InputReader;
import megan.io.OutputWriter;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * classed used to rescan classifications in RMA2 file
 * Daniel Huson, 10.2010
 */
public class RMA2Modifier {
    private final RMA2File rma2File;
    private final InfoSection infoSection;

    private final InputOutputReaderWriter io;

    private String currentName;
    private OutputWriter classificationIndexTmpFileWriter;
    private int numberOfClasses;

    private long dumpStart;

    /**
     * construct a new RMA2Modifier that can be used to rewrite all classifications. The summary section ist lost
     *
	 */
    public RMA2Modifier(File file) throws IOException {
        rma2File = new RMA2File(file);
        infoSection = rma2File.loadInfoSection();

        io = rma2File.getFileAppender();
        infoSection.read(io);
        // erase summary section:
        infoSection.setAuxiliaryDataStart(0);
        infoSection.setAuxiliaryDataEnd(0);
        infoSection.setNumberOfClassifications(0);

        // go to end of data section and remove all classifications, summary and info
        io.seek(infoSection.getDataIndexSectionEnd());
        io.setLength(infoSection.getDataIndexSectionEnd());
    }

    /**
     * start a new classification
     *
	 */
    public void startClassificationSection(String name) throws IOException {
        currentName = name;
        dumpStart = io.getPosition();
        classificationIndexTmpFileWriter = new OutputWriter(rma2File.getClassificationIndexTmpFile());
        numberOfClasses = 0;
    }

    /**
     * add an entry to the classification
     *
	 */
    public void addToClassification(Integer classId, float weight, List<Long> positions) throws IOException {
        numberOfClasses++;
        classificationIndexTmpFileWriter.writeInt(classId);
        if (weight == positions.size())
            classificationIndexTmpFileWriter.writeInt((int) weight);
        else {
            classificationIndexTmpFileWriter.writeInt(-(int) weight);
            classificationIndexTmpFileWriter.writeInt(positions.size());
        }

        // System.err.println("classId: "+classId+" size: "+size+" dumpPos: "+io.getPosition()+" readPos: "+ Basic.toString(positions,","));

        if (positions.size() > 0) {
            classificationIndexTmpFileWriter.writeLong(io.getPosition());
            for (Long pos : positions) {
                io.writeLong(pos);
            }
        } else // no elements, write -1
        {
            classificationIndexTmpFileWriter.writeLong(-1);
        }
    }

    /**
     * finish a classification. The temporary file is closed, appended to the main file and then deleted
     *
	 */
    public void finishClassificationSection() throws IOException {
        long dumpEnd = io.getPosition();

        // copy index:
        long indexStart = io.getPosition();
        if (classificationIndexTmpFileWriter != null && classificationIndexTmpFileWriter.length() > 0) {
            // System.err.println("Position at close: " + classificationIndexTmpFileWriter.getPosition());
            // System.err.println("Size at close: " + classificationIndexTmpFileWriter.length());
            classificationIndexTmpFileWriter.close();
            // System.err.println("File size: " + rma2File.getClassificationIndexTmpFile().length());

            try (InputReader r = new InputReader(rma2File.getClassificationIndexTmpFile(), null, null, true)) {
                // System.err.println("Channel: " + r.getChannel().size());

                final int bufferSize = 1000000;
                long length = r.length();
                int blocks = (int) (length / bufferSize);
                byte[] buffer = new byte[bufferSize];

                long total = 0;
                for (int i = 0; i < blocks; i++) {
                    if (r.read(buffer, 0, bufferSize) < bufferSize)
                        throw new IOException("Buffer underflow");
                    io.write(buffer, 0, bufferSize);
                    total += bufferSize;
                }
                int remainder = (int) (length - bufferSize * blocks);
                if (remainder > 0) {
                    if (r.read(buffer, 0, remainder) < remainder)
                        throw new IOException("Buffer underflow");
                    io.write(buffer, 0, remainder);
                }
                //System.err.println("Copied: " + total);

                io.seekToEnd();
            }
        }
        long indexEnd = io.getPosition();

        rma2File.getClassificationIndexTmpFile().delete();

        infoSection.addClassification(currentName, numberOfClasses, dumpStart, dumpEnd, indexStart, indexEnd);
    }

    /**
     * append the info section to the main file and then close it
     *
	 */
    public void close() throws IOException {
        infoSection.updateModificationDate();
        infoSection.write(io);
    }
}
