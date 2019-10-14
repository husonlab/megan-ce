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
package megan.rma3;

import jloda.util.Pair;
import megan.io.IInputReader;
import megan.io.IOutputWriter;

import java.io.IOException;
import java.util.Map;

/**
 * Aux-blocks footer
 * Created by huson on 5/16/14.
 */
public class AuxBlocksFooterRMA3 extends BaseRMA3 {
    private String auxDataFormat = "Name:String Data:Bytes";
    private int count = 0;

    /**
     * constructor
     */
    public AuxBlocksFooterRMA3() {
        super("AuxDataFormat:String Count:Integer");
    }

    @Override
    public void read(IInputReader reader, long startPos) throws IOException {
        reader.seek(startPos);
        setFormatDef(reader.readString());
        FormatDefinition formatDefinition = FormatDefinition.fromString(getFormatDef());
        for (Pair<String, FormatDefinition.Type> pair : formatDefinition.getList()) {
            if (pair.getFirst().equals("AuxDataFormat"))
                setAuxDataFormat(reader.readString());
            else if (pair.getFirst().equals("Count"))
                setCount(reader.readInt());

        }
    }

    @Override
    public void write(IOutputWriter writer) throws IOException {
        writer.writeString(getFormatDef());
        FormatDefinition formatDefinition = FormatDefinition.fromString(getFormatDef());
        formatDefinition.startWrite();
        for (Pair<String, FormatDefinition.Type> pair : formatDefinition.getList()) {
            if (pair.getFirst().equals("AuxDataFormat"))
                formatDefinition.write(writer, "AuxDataFormat", getAuxDataFormat());
            else if (pair.getFirst().equals("Count"))
                formatDefinition.write(writer, "Count", getCount());
        }
        formatDefinition.finishWrite();
    }

    /**
     * auxiliary method that can be used to write auxblocks to RMA3 file
     *
     * @param writer
     * @param name2AuxData
     * @throws IOException
     */
    public void writeAuxBlocks(IOutputWriter writer, Map<String, byte[]> name2AuxData) throws IOException {
        setCount(name2AuxData.size());
        for (String name : name2AuxData.keySet()) {
            writer.writeString(name);
            byte[] bytes = name2AuxData.get(name);
            writer.writeInt(bytes.length);
            writer.write(bytes);
        }
    }

    /**
     * read the aux blocks from a file
     *
     * @param fileFooter
     * @param reader
     * @param name2AuxBlock
     * @throws IOException
     */
    public void readAuxBlocks(FileFooterRMA3 fileFooter, IInputReader reader, Map<String, byte[]> name2AuxBlock) throws IOException {
        reader.seek(fileFooter.getAuxStart());
        for (int i = 0; i < count && reader.getPosition() < fileFooter.getAuxFooter(); i++) {
            String name = reader.readString();
            int length = reader.readInt();
            byte[] bytes = new byte[length];
            reader.read(bytes, 0, length);
            name2AuxBlock.put(name, bytes);
        }
    }

    private String getAuxDataFormat() {
        return auxDataFormat;
    }

    private void setAuxDataFormat(String auxDataFormat) {
        this.auxDataFormat = auxDataFormat;
    }

    private int getCount() {
        return count;
    }

    private void setCount(int count) {
        this.count = count;
    }

}
