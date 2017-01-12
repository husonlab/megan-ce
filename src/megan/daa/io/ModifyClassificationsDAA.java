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
     * @param fNames
     * @param fName2ClassId2Location
     * @param fName2ClassId2Weight
     * @throws IOException
     */
    public static void saveClassifications(DAAHeader header, String[] cNames, Map<Integer, ListOfLongs>[] fName2ClassId2Location, Map<Integer, Integer>[] fName2ClassId2Weight) throws IOException {

        DAAModifier.removeMEGANClassificationData(header);

        for (int f = 0; f < cNames.length; f++) {
            final String cName = cNames[f];

            final ByteOutputStream outKey = new ByteOutputStream(1000000);
            final OutputWriterLittleEndian wKey = new OutputWriterLittleEndian(outKey);

            final ByteOutputStream outDump = new ByteOutputStream(1000000);
            final OutputWriterLittleEndian wDump = new OutputWriterLittleEndian(outDump);

            final Map<Integer, ListOfLongs> id2locations = fName2ClassId2Location[f];

            wKey.writeNullTerminatedString(cName.getBytes());
            wKey.writeInt(id2locations.size());

            wDump.writeNullTerminatedString(cName.getBytes());

            for (int classId : id2locations.keySet()) {
                wKey.writeInt(classId);
                wKey.writeInt(fName2ClassId2Weight[f].get(classId));
                final ListOfLongs list = id2locations.get(classId);
                wKey.writeInt(list.size());
                wKey.writeLong(wDump.getPosition()); // offset
                for (int i = 0; i < list.size(); i++) {
                    wDump.writeLong(list.get(i));
                }
            }

            DAAModifier.appendBlocks(header,
                    new BlockType[]{BlockType.megan_classification_key_block, BlockType.megan_classification_dump_block},
                    new byte[][]{outKey.getBytes(), outDump.getBytes()},
                    new int[]{outKey.size(), outDump.size()});
        }
    }
}
