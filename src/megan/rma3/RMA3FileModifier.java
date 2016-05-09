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
package megan.rma3;

import jloda.util.ListOfLongs;
import megan.core.ClassificationType;
import megan.io.InputOutputReaderWriter;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * class used to rescan the classifications in an RMA3 file
 */
public class RMA3FileModifier extends RMA3File {
    private InputOutputReaderWriter io;

    /**
     * construct an RMA3 modifier and read in RMA3 data
     *
     * @param fileName
     * @throws IOException
     */
    public RMA3FileModifier(String fileName) throws IOException {
        super(fileName, READ_WRITE);
        close(); // have read the file, now close the readerWriter

    }

    /**
     * start the modification process
     *
     * @throws IOException
     */
    public void startModification() throws IOException {
        io = new InputOutputReaderWriter(new File(getFileName()), READ_WRITE);
        getClassificationsFooter().clear();

        io.seek(getFileFooter().getClassificationsStart());
        io.setLength(io.getPosition());

    }

    /**
     * rescan a specific classification
     *
     * @param classificationType
     * @param classId2locations
     * @throws IOException
     */
    public void updateClassification(ClassificationType classificationType, Map<Integer, ListOfLongs> classId2locations) throws IOException {
        getClassificationsFooter().setStart(classificationType, io.getPosition());
        getClassificationsFooter().setDo(classificationType);

        final ClassificationBlockRMA3 classificationBlock = new ClassificationBlockRMA3(classificationType);

        for (Integer classId : classId2locations.keySet()) {
            classificationBlock.setSum(classId, classId2locations.get(classId).size());
        }
        classificationBlock.write(io, classId2locations);
        getClassificationsFooter().setEnd(classificationType, io.getPosition());
    }

    /**
     * finish the rescan process
     *
     * @throws IOException
     */
    public void finishModification() throws IOException {
        getFileFooter().setClassificationsFooter(io.getPosition());
        getClassificationsFooter().write(io);

        getFileFooter().setAuxStart(io.getPosition());
        getFileFooter().setAuxFooter(io.getPosition());
        getAuxBlocksFooter().write(io);

        getFileFooter().setFileFooter(io.getPosition());
        getFileFooter().write(io);
        close();
    }

    /**
     * close the readerWriter/writer, if it is open
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (io != null) {
            try {
                io.close();
            } finally {
                io = null;
            }
        }
    }

    /**
     * save the aux data to the rma3 file
     *
     * @param label2data
     * @throws IOException
     */
    public void saveAuxData(Map<String, byte[]> label2data) throws IOException {
        final FileFooterRMA3 fileFooter = getFileFooter();

        close();
        io = new InputOutputReaderWriter(new File(getFileName()), READ_WRITE);

        io.setLength(fileFooter.getAuxStart());
        io.seek(fileFooter.getAuxStart());

        fileFooter.setAuxStart(io.getPosition());
        getAuxBlocksFooter().writeAuxBlocks(io, label2data);

        fileFooter.setAuxFooter(io.getPosition());
        getAuxBlocksFooter().write(io);

        fileFooter.setFileFooter(io.getPosition());
        fileFooter.write(io);
        close();
    }
}
