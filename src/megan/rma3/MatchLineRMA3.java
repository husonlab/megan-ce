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
import java.util.Objects;

/**
 * A match
 * Created by huson on 5/16/14.
 */
public class MatchLineRMA3 extends BaseRMA3 {
    // configuration:
    private boolean embedText;
    private boolean doKegg;
    private boolean doSeed;
    private boolean doCog;
    private boolean doPfam;

    // data stored in a match line:
    private float expected;
    private int bitScore;
    private int percentId;
    private int taxId;
    private int keggId;
    private int seedId;
    private int cogId;
    private int pfamId;

    // alternatives, depending on textStoragePolicy:
    private long fileOffset; // location of match in SAM file
    private String text; // SAM line

    /**
     * constructor
     */
    public MatchLineRMA3(String format) {
        super("");
        setFormatDef(format);  // call this to ensure that variables are set
    }

    /**
     * constructor
     */
    public MatchLineRMA3(TextStoragePolicy textStoragePolicy, boolean doKegg, boolean doSeed, boolean doCog, boolean doPfam) {
        super("Expected:Float BitScore:Character PercentId:Byte Tax:Integer");
        this.doKegg = doKegg;
        this.doSeed = doSeed;
        this.doCog = doCog;
        this.doPfam = doPfam;
        this.embedText = (textStoragePolicy == TextStoragePolicy.Embed || textStoragePolicy == TextStoragePolicy.InRMAZ);
        setFormatDef(getFormatDef() + (doKegg ? " Kegg:Integer" : "")
                + (doSeed ? " Seed:Integer" : "") + (doCog ? " EGGNOG:Integer" : "")
                + (doPfam ? " Pfam:Integer" : "") + (embedText ? " BlastText:String" : " FileOffset:Long"));
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
        expected = reader.readFloat();
        bitScore = reader.readChar();
        percentId = reader.read();
        taxId = reader.readInt();
        if (doKegg)
            keggId = reader.readInt();
        if (doSeed)
            seedId = reader.readInt();
        if (doCog)
            cogId = reader.readInt();
        if (doPfam)
            pfamId = reader.readInt();

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
        writer.writeFloat(expected);
        writer.writeChar((char) bitScore);
        writer.write((byte) percentId);
        writer.writeInt(taxId);
        if (doKegg)
            writer.writeInt(keggId);
        if (doSeed)
            writer.writeInt(seedId);
        if (doCog)
            writer.writeInt(cogId);
        if (doPfam)
            writer.writeInt(pfamId);
        if (embedText)
            writer.writeString(text);
        else
            writer.writeLong(fileOffset);
    }

    public float getExpected() {
        return expected;
    }

    public void setExpected(float expected) {
        this.expected = expected;
    }

    public int getBitScore() {
        return bitScore;
    }

    public void setBitScore(int bitScore) {
        this.bitScore = bitScore;
    }

    public int getPercentId() {
        return percentId;
    }

    public void setPercentId(int percentId) {
        this.percentId = percentId;
    }

    public int getTaxId() {
        return taxId;
    }

    public void setTaxId(Integer id) {
        this.taxId = Objects.requireNonNullElse(id, 0);
    }

    public int getKeggId() {
        return keggId;
    }

    public void setKeggId(Integer id) {
        this.keggId = Objects.requireNonNullElse(id, 0);
    }

    public int getSeedId() {
        return seedId;
    }

    public void setSeedId(Integer id) {
        this.seedId = Objects.requireNonNullElse(id, 0);
    }

    public int getCogId() {
        return cogId;
    }

    public void setCogId(Integer id) {
        this.cogId = Objects.requireNonNullElse(id, 0);
    }

    public int getPfamId() {
        return pfamId;
    }

    public void setPfamId(Integer id) {
        this.pfamId = Objects.requireNonNullElse(id, 0);
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

    public boolean isDoKegg() {
        return doKegg;
    }

    public boolean isDoSeed() {
        return doSeed;
    }

    public boolean isDoCog() {
        return doCog;
    }

    public boolean isEmbedText() {
        return embedText;
    }

    public void setFormatDef(String formatDef) {
        doKegg = formatDef.contains("Kegg:Integer");
        doSeed = formatDef.contains("Seed:Integer");
        doCog = formatDef.contains("EGGNOG:Integer");
        embedText = formatDef.contains("BlastText:String");
        super.setFormatDef(formatDef);
    }
}
