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
import megan.rma2.RMA2File;

import java.io.IOException;

/**
 * The header section of an RMA3 File
 * Created by huson on 5/14/14.
 */
public class FileHeaderRMA3 extends BaseRMA3 {
    private String creator;
    private long creationDate;


    /**
     * constructor
     */
    public FileHeaderRMA3() {
        super("Creator:String CreationDate:Long");
    }

    /**
     * write the current header data
     *
     * @param writer
     * @return position of beginning of block
     * @throws IOException
     */
    public void write(IOutputWriter writer) throws IOException {
        writer.writeInt(RMA2File.MAGIC_NUMBER);
        writer.writeInt(3);
        writer.writeString(getFormatDef());

        FormatDefinition formatDefinition = FormatDefinition.fromString(getFormatDef());
        formatDefinition.startWrite();
        for (Pair<String, FormatDefinition.Type> pair : formatDefinition.getList()) {
            if (pair.getFirst().equals("Creator"))
                formatDefinition.write(writer, "Creator", getCreator());
            else if (pair.getFirst().equals("CreationDate"))
                formatDefinition.write(writer, "CreationDate", getCreationDate());
        }
        formatDefinition.finishWrite();
    }

    /**
     * read the header from a file
     *
     * @param reader
     * @throws IOException
     */
    public void read(IInputReader reader, long startPos) throws IOException {
        reader.seek(startPos);

        final int magicNumber = reader.readInt();

        if (magicNumber != RMA2File.MAGIC_NUMBER) {
            throw new IOException("Not an RMA file");
        }
        final int version = reader.readInt();
        if (version != 3) {
            throw new IOException("Not an RMA 3 file");
        }

        setFormatDef(reader.readString());

        FormatDefinition formatDefinition = FormatDefinition.fromString(getFormatDef());

        for (Pair<String, FormatDefinition.Type> pair : formatDefinition.getList()) {
            if (pair.getFirst().equals("Creator"))
                setCreator(reader.readString());
            else if (pair.getFirst().equals("CreationDate"))
                setCreationDate(reader.readLong());
        }
    }

    private String getCreator() {
        return creator;
    }

    private void setCreator(String creator) {
        this.creator = creator;
    }

    public long getCreationDate() {
        return creationDate;
    }

    private void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

}
