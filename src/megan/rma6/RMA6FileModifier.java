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
package megan.rma6;

import jloda.util.Basic;
import jloda.util.ListOfLongs;
import megan.io.InputOutputReaderWriter;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * class used to update the classifications in an Rma6 file
 * Daniel Huson, 4.2015
 */
public class RMA6FileModifier extends RMA6File implements Closeable {
    private InputOutputReaderWriter io;

    /**
     * construct an RMA6 modifier and read in RMA6 data
     *
     * @param fileName
     * @throws IOException
     */
    public RMA6FileModifier(String fileName) throws IOException {
        super(fileName, READ_WRITE);
        super.close(); // have read the file, now close the readerWriter
    }

    /**
     * update the classifications
     *
     * @param cNames
     * @param fName2ClassId2Location
     * @param fName2ClassId2Weight
     * @throws IOException
     */
    public void updateClassifications(String[] cNames, Map<Integer, ListOfLongs>[] fName2ClassId2Location, Map<Integer, Float>[] fName2ClassId2Weight) throws IOException {
        io = new InputOutputReaderWriter(new File(fileName), READ_WRITE);

        io.seek(footerSectionRMA6.getStartClassificationsSection());
        io.setLength(io.getPosition());

        footerSectionRMA6.getAvailableClassification2Position().clear();

        for (int c = 0; c < cNames.length; c++) {
            final String cName = cNames[c];
            final ClassificationBlockRMA6 classification = new ClassificationBlockRMA6(cName);
            final Map<Integer, ListOfLongs> id2locations = fName2ClassId2Location[c];
            for (int id : id2locations.keySet()) {
                final Float weight = fName2ClassId2Weight[c].get(id);
                classification.setWeightedSum(id, weight != null ? weight : 0f);
                final ListOfLongs list = fName2ClassId2Location[c].get(id);
                classification.setSum(id, list != null ? list.size() : 0);
            }
            footerSectionRMA6.getAvailableClassification2Position().put(cName, io.getPosition());
            classification.write(io, id2locations);
            System.err.println(String.format("Numb. %4s classes: %,10d", Basic.abbreviate(cName, 4), id2locations.size()));
        }

        footerSectionRMA6.setEndClassificationsSection(io.getPosition());
        footerSectionRMA6.setStartAuxDataSection(io.getPosition());
        io.writeInt(0);
        footerSectionRMA6.setEndAuxDataSection(io.getPosition());
        footerSectionRMA6.setStartFooterSection(io.getPosition());
        footerSectionRMA6.write(io);
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
     * save the aux data to the rma6 file
     *
     * @param label2data
     * @throws IOException
     */
    public void saveAuxData(Map<String, byte[]> label2data) throws IOException {
        final long location = footerSectionRMA6.getStartAuxDataSection();

        io = new InputOutputReaderWriter(new File(fileName), READ_WRITE);

        io.setLength(location);
        io.seek(location);

        io.writeInt(label2data.size());
        for (String name : label2data.keySet()) {
            io.writeString(name);
            byte[] bytes = label2data.get(name);
            io.writeInt(bytes.length);
            io.write(bytes);
        }

        footerSectionRMA6.setEndAuxDataSection(io.getPosition());
        footerSectionRMA6.setStartFooterSection(io.getPosition());
        footerSectionRMA6.write(io);
        close();
    }

    static class DataRecord {
        ListOfLongs locations;
        float weight;
        int sum;
    }
}
