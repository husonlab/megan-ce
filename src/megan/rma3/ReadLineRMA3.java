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

import megan.data.TextStoragePolicy;
import megan.io.IInputReader;
import megan.io.IOutputWriter;

import java.io.IOException;

/**
 * A read
 * Created by huson on May 2014
 */
public class ReadLineRMA3 extends BaseRMA3 {
    // configuration:
    private boolean embedText;

    // data stored in a match line:
    private long readUid; // unique identifier and location in RMA3 file
    private int length;
    private int numberOfMatches;
    private int readWeight;
    private final boolean hasMagnitude;

    // alternatives, depending on textStoragePolicy:
    private long fileOffset; // location of text in reads file
    private String text; // read text (header and sequence)

    /**
     * constructor
     */
    public ReadLineRMA3(String format) {
        super("");
        setFormatDef(format); // call this to ensure that variables are set
        hasMagnitude = format.contains("Weight:Integer");
    }

    /**
     * constructor
     */
    public ReadLineRMA3(TextStoragePolicy textStoragePolicy, boolean hasReadWeight) {
        super("ReadUid:Long ReadLength:Integer " + (hasReadWeight ? "Weight:Integer " : "") + "NumMatches:Integer");
        this.hasMagnitude = hasReadWeight;
        this.embedText = (textStoragePolicy == TextStoragePolicy.Embed || textStoragePolicy == TextStoragePolicy.InRMAZ);
        setFormatDef(getFormatDef() + (embedText ? " BlastText:String" : " FileOffset:Long"));
    }

    /**
     * read
     *
     * @param reader
     * @throws java.io.IOException
     */
    public void read(IInputReader reader, long position) throws IOException {
        reader.seek(position);
        read(reader);
    }

    /**
     * read
     *
     * @param reader
     * @throws java.io.IOException
     */
    public void read(IInputReader reader) throws IOException {
        // todo: for efficiency, we assume that the format of match lines is always as follows:
        readUid = reader.readLong();
        length = reader.readInt();
        if (hasMagnitude)
            readWeight = reader.readInt();
        else
            readWeight = 1;
        numberOfMatches = reader.readInt();

        if (embedText) {
            text = reader.readString();
        } else {
            fileOffset = reader.readLong();
        }
    }

    /**
     * write
     *
     * @param writer
     * @throws java.io.IOException
     */
    public void write(IOutputWriter writer) throws IOException {
        // todo: for efficiency, we assume that the format of match lines is always as follows:
        writer.writeLong(readUid);
        writer.writeInt(length);
        if (hasMagnitude)
            writer.writeInt(readWeight);
        writer.writeInt(numberOfMatches);
        if (embedText)
            writer.writeString(text);
        else
            writer.writeLong(fileOffset);
    }

    public long getReadUid() {
        return readUid;
    }

    public void setReadUid(long readUid) {
        this.readUid = readUid;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getReadWeight() {
        return readWeight;
    }

    public void setReadWeight(int readWeight) {
        this.readWeight = readWeight;
    }

    public int getNumberOfMatches() {
        return numberOfMatches;
    }

    public void setNumberOfMatches(int numberOfMatches) {
        this.numberOfMatches = numberOfMatches;
    }

    public long getFileOffset() {
        return fileOffset;
    }

    public void setFileOffset(long fileOffset) {
        this.fileOffset = fileOffset;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isEmbedText() {
        return embedText;
    }

    public void setFormatDef(String formatDef) {
        embedText = formatDef.contains("BlastText:String");
        super.setFormatDef(formatDef);
    }

}
