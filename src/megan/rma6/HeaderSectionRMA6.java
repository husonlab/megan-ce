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

import jloda.util.BlastMode;
import megan.io.IInputReader;
import megan.io.IOutputWriter;

import java.io.IOException;

/**
 * header for read-alignment archive format
 * Daniel Huson, 6.2015
 */
public class HeaderSectionRMA6 {
    private String creator;
    private long creationDate = 0;
    private BlastMode blastMode;
    private boolean pairedReads;
    private String[] matchClassNames;  // classifications for which matches have identifiers

    /**
     * read the header
     *
     * @throws java.io.IOException
     */
    public void read(IInputReader reader) throws IOException {
        final int magicNumber = reader.readInt();
        if (magicNumber != RMA6File.MAGIC_NUMBER) {
            throw new IOException("Not an RMA file");
        }
        final int version = reader.readInt();
        if (version != RMA6File.VERSION) {
            throw new IOException("Not an RMA " + RMA6File.VERSION + " file");
        }
        int minorVersion = reader.readInt();
        creator = reader.readString();
        creationDate = reader.readLong();
        blastMode = BlastMode.valueOf(reader.readString());
        pairedReads = (reader.read() == 1);
        matchClassNames = new String[reader.readInt()];
        for (int i = 0; i < matchClassNames.length; i++) {
            matchClassNames[i] = reader.readString();
        }
    }

    /**
     * writer the header
     *
     * @param writer
     * @throws IOException
     */
    public void write(IOutputWriter writer) throws IOException {
        writer.writeInt(RMA6File.MAGIC_NUMBER);
        writer.writeInt(RMA6File.VERSION);
        writer.writeInt(RMA6File.MINOR_VERSION);
        writer.writeString(creator);
        if (creationDate == 0)
            creationDate = System.currentTimeMillis();
        writer.writeLong(creationDate);
        writer.writeString(blastMode.toString());
        writer.write(pairedReads ? 1 : 0);
        writer.writeInt(matchClassNames.length);
        for (String name : matchClassNames)
            writer.writeString(name);
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public BlastMode getBlastMode() {
        return blastMode;
    }

    public void setBlastMode(BlastMode blastMode) {
        this.blastMode = blastMode;
    }

    public boolean isPairedReads() {
        return pairedReads;
    }

    public void setIsPairedReads(boolean isPairedReads) {
        this.pairedReads = isPairedReads;
    }

    public String[] getMatchClassNames() {
        return matchClassNames;
    }

    public void setMatchClassNames(String[] matchClassNames) {
        this.matchClassNames = matchClassNames;
    }
}
