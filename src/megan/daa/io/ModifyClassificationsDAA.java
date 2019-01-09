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

package megan.daa.io;

import jloda.util.ListOfLongs;

import java.io.IOException;
import java.util.Map;

/**
 * modifies a DAA file
 * Daniel Huson, 8.2015
 */
public class ModifyClassificationsDAA {

    /**
     * update the classifications
     *
     * @param cNames
     * @param fName2ClassId2Location
     * @param fName2ClassId2Weight
     * @throws IOException
     */
    public static void saveClassifications(DAAHeader header, String[] cNames, Map<Integer, ListOfLongs>[] fName2ClassId2Location, Map<Integer, Float>[] fName2ClassId2Weight) throws IOException {
        DAAModifier.removeMEGANClassificationData(header);

        for (int c = 0; c < cNames.length; c++) {
            final String cName = cNames[c];

            final ByteOutputStream outputStreamClassKeys = new ByteOutputStream(1000000);
            final OutputWriterLittleEndian writerClassKeys = new OutputWriterLittleEndian(outputStreamClassKeys);

            final ByteOutputStream outputStreamClassReadLocationsDump = new ByteOutputStream(1000000);
            final OutputWriterLittleEndian writerClassReadLocationsDump = new OutputWriterLittleEndian(outputStreamClassReadLocationsDump);

            final Map<Integer, ListOfLongs> id2locations = fName2ClassId2Location[c];

            writerClassKeys.writeNullTerminatedString(cName.getBytes());
            writerClassKeys.writeInt(id2locations.size());

            writerClassReadLocationsDump.writeNullTerminatedString(cName.getBytes());

            for (int classId : id2locations.keySet()) {
                writerClassKeys.writeInt(classId);
                float weight = fName2ClassId2Weight[c].get(classId);
                writerClassKeys.writeInt((int) weight);
                final ListOfLongs list = id2locations.get(classId);
                writerClassKeys.writeInt(list.size());
                writerClassKeys.writeLong(writerClassReadLocationsDump.getPosition()); // offset
                for (int i = 0; i < list.size(); i++) {
                    writerClassReadLocationsDump.writeLong(list.get(i));
                }
            }

            DAAModifier.appendBlocks(header,
                    new BlockType[]{BlockType.megan_classification_key_block, BlockType.megan_classification_dump_block},
                    new byte[][]{outputStreamClassKeys.getBytes(), outputStreamClassReadLocationsDump.getBytes()},
                    new int[]{outputStreamClassKeys.size(), outputStreamClassReadLocationsDump.size()});
        }
    }
}
