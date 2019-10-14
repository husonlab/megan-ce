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
package megan.rma2;

import megan.io.ByteByteInt;
import megan.io.IInputReader;
import megan.io.IOutputWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * reads and writes the read block fixed part
 * Daniel Huson, 3.2011
 */
public class MatchBlockRMA2Formatter {
    static public final String DEFAULT_RMA2_0 = "BitScore:float;Identity:float;TaxonId:int;RefSeqID:BBI;";
    static public final String DEFAULT_RMA2_1 = "BitScore:float;Identity:float;TaxonId:int;RefSeqID:BBI;";
    static public final String DEFAULT_RMA2_5 = "BitScore:float;Identity:float;Expected:float;TaxonId:int;SeedId:int;CogId:int;KeggId:int;PfamId:int;";

    private final String format;

    private int numberOfBytes = 0;

    // access to commonly used ones:
    private Object[] bitScore;
    private Object[] expected;
    private Object[] percentIdentity;
    private Object[] taxonId;
    private Object[] seedId;
    private Object[] cogId;
    private Object[] keggId;
    private Object[] pfamId;
    private Object[] refSeqId;

    private Object[][] data;
    private final HashMap<String, Object[]> name2data = new HashMap<>();

    /**
     * constructs an instance and sets to the given format
     *
     * @param format
     */
    public MatchBlockRMA2Formatter(String format) {
        this.format = format;
        decode(format);
    }

    private void decode(String format) {
        LinkedList<Object[]> list = new LinkedList<>();
        StringTokenizer tokenizer = new StringTokenizer(format, ";");
        while (tokenizer.hasMoreElements()) {
            String[] split = tokenizer.nextToken().split(":");
            String name = split[0];
            String type = split[1];
            Object[] object = new Object[]{name, type.charAt(0), null};
            switch (type.charAt(0)) {
                case 'i':
                case 'f':
                    numberOfBytes += 4;
                    break;
                case 'l':
                    numberOfBytes += 8;
                    break;
                case 'b':
                    numberOfBytes += 1;
                    break;
                case 'B':
                    numberOfBytes += 6;
                    break;
                case 'c':
                    numberOfBytes += 2;
                    break;
            }
            name2data.put(name, object);
            list.add(object);
        }
        data = list.toArray(new Object[list.size()][]);

        // access to standard items set here:
        bitScore = name2data.get("BitScore");
        expected = name2data.get("Expected");
        percentIdentity = name2data.get("Identity");
        taxonId = name2data.get("TaxonId");
        seedId = name2data.get("SeedId");
        cogId = name2data.get("CogId");
        keggId = name2data.get("KeggId");
        pfamId = name2data.get("PfamId");
        refSeqId = name2data.get("RefSeqID");
    }

    /**
     * read the fixed part of the reads block
     *
     * @param dataIndexReader
     * @throws java.io.IOException
     */
    public void read(IInputReader dataIndexReader) throws IOException {
        for (Object[] dataRecord : data) {
            switch ((Character) dataRecord[1]) {
                case 'i':
                    dataRecord[2] = dataIndexReader.readInt();
                    break;
                case 'f':
                    dataRecord[2] = dataIndexReader.readFloat();
                    break;
                case 'l':
                    dataRecord[2] = dataIndexReader.readLong();
                    break;
                case 'b':
                    dataRecord[2] = (byte) dataIndexReader.read();
                    break;
                case 'B':
                    dataRecord[2] = dataIndexReader.readByteByteInt();
                    break;
                case 'c':
                    dataRecord[2] = dataIndexReader.readChar();
                    break;
            }
            // System.err.println("Read (match): "+ Basic.toString(dataRecord,","));
        }
    }

    /**
     * write the fixed part of the reads block
     *
     * @param indexWriter
     * @throws java.io.IOException
     */
    public void write(IOutputWriter indexWriter) throws IOException {
        for (Object[] dataRecord : data) {
            switch ((Character) dataRecord[1]) {
                case 'i':
                    indexWriter.writeInt((Integer) dataRecord[2]);
                    break;
                case 'f':
                    indexWriter.writeFloat((Float) dataRecord[2]);
                    break;
                case 'l':
                    indexWriter.writeLong((Long) dataRecord[2]);
                    break;
                case 'b':
                    indexWriter.write((Byte) dataRecord[2]);
                    break;
                case 'B':
                    indexWriter.writeByteByteInt((ByteByteInt) dataRecord[2]);
                    break;
                case 'c':
                    indexWriter.writeChar((Character) dataRecord[2]);
                    break;
            }
            // System.err.println("Wrote (match): "+ Basic.toString(dataRecord,","));
        }
    }

    /**
     * get the value for a name
     *
     * @param name
     * @return value
     */
    public Object get(String name) {
        return name2data.get(name)[2];
    }

    /**
     * set the value for a name. Does not check that value is of correct type!
     *
     * @param name
     * @param value
     */
    public void put(String name, Object value) {
        name2data.get(name)[2] = value;
    }

    public boolean hasBitScore() {
        return bitScore != null;
    }

    public Float getBitScore() {
        return (Float) bitScore[2];
    }

    public void setBitScore(Float value) {
        bitScore[2] = value;
    }

    public boolean hasExpected() {
        return expected != null;
    }

    public Float getExpected() {
        return (Float) expected[2];
    }

    public void setExpected(Float value) {
        expected[2] = value;
    }

    public boolean hasPercentIdentity() {
        return percentIdentity != null;
    }

    public Float getPercentIdentity() {
        return (Float) percentIdentity[2];
    }

    public void setPercentIdentity(Float value) {
        percentIdentity[2] = value;
    }

    public boolean hasTaxonId() {
        return taxonId != null;
    }

    public int getTaxonId() {
        return (Integer) taxonId[2];
    }

    public void setTaxonId(Integer value) {
        taxonId[2] = value;
    }

    public boolean hasSeedId() {
        return seedId != null;
    }

    public int getSeedId() {
        if (seedId[2] != null)
            return (Integer) seedId[2];
        else
            return 0;
    }

    public void setSeedId(Integer value) {
        seedId[2] = value;
    }

    public boolean hasCogId() {
        return cogId != null;
    }

    public int getCogId() {
        if (cogId[2] != null)
            return (Integer) cogId[2];
        else
            return 0;
    }

    public void setCogId(Integer value) {
        cogId[2] = value;
    }

    public boolean hasKeggId() {
        return keggId != null;
    }

    public int getKeggId() {
        if (keggId[2] != null)
            return (Integer) keggId[2];
        else
            return 0;
    }

    public void setKeggId(Integer value) {
        keggId[2] = value;
    }


    public boolean hasPfamId() {
        return pfamId != null;
    }

    public int getPfamId() {
        if (pfamId[2] != null)
            return (Integer) pfamId[2];
        else
            return 0;
    }

    public void setPfamId(Integer value) {
        pfamId[2] = value;
    }

    public boolean hasRefSeqId() {
        return refSeqId != null;
    }

    public ByteByteInt getRefSeqId() {
        if (refSeqId != null)
            return (ByteByteInt) refSeqId[2];
        else
            return ByteByteInt.ZERO;
    }

    public void setRefSeqId(ByteByteInt value) {
        if (refSeqId != null)
            refSeqId[2] = value;
    }

    public int getNumberOfBytes() {
        return numberOfBytes;
    }


    /**
     * gets the format string
     *
     * @return format string
     */
    public String toString() {
        return format;
    }
}
