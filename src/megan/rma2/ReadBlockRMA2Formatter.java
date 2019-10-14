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
 * formatting for readblock
 * Daniel Huson, 4.2011
 */
public class ReadBlockRMA2Formatter {
    static public final String DEFAULT_RMA2_0 = "MateUId:long;MateType:byte;ReadLength:int;NumberOfMatches:int;";
    static public final String DEFAULT_RMA2_1 = "MateUId:long;MateType:byte;ReadLength:int;Complexity:float;NumberOfMatches:int;";
    static public final String DEFAULT_RMA2_2 = "ReadWeight:int;MateUId:long;MateType:byte;ReadLength:int;Complexity:float;NumberOfMatches:int;";
    static public final String DEFAULT_RMA2_5 = "ReadWeight:int;MateUId:long;MateType:byte;ReadLength:int;Complexity:float;NumberOfMatches:int;"; // MEGAN5

    private final String format;

    private int numberOfBytes = 0;

    // access to commonly used ones:
    private Object[] readWeight;
    private Object[] mateUId;
    private Object[] mateType;
    private Object[] readLength;
    private Object[] complexity;
    private Object[] numberOfMatches;

    private Object[][] data;
    private final HashMap<String, Object[]> name2data = new HashMap<>();

    /**
     * constructs an instance and sets to the given format
     *
     * @param format
     */
    public ReadBlockRMA2Formatter(String format) {
        this.format = format;
        decode(format);
    }

    private void decode(String format) {
        LinkedList<Object[]> list = new LinkedList<>();
        StringTokenizer tokenizer = new StringTokenizer(format, ";");
        int index = 0;
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
        readWeight = name2data.get("ReadWeight");
        mateUId = name2data.get("MateUId");
        mateType = name2data.get("MateType");
        readLength = name2data.get("ReadLength");
        complexity = name2data.get("Complexity");
        numberOfMatches = name2data.get("NumberOfMatches");
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
            // System.err.println("Read (read): "+ Basic.toString(dataRecord, ","));
        }
    }

    /**
     * write the fixed part of the reads block
     *
     * @param indexWriter
     * @throws IOException
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
            //  System.err.println("Wrote (read): "+ Basic.toString(dataRecord,","));
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

    public boolean hasReadWeight() {
        return readWeight != null;
    }

    public Integer getReadWeight() {
        return (Integer) readWeight[2];
    }

    public void setReadWeight(Integer value) {
        if (readWeight != null)
            readWeight[2] = value;
    }

    public boolean hasMateUId() {
        return mateUId != null;
    }

    public Long getMateUId() {
        return (Long) mateUId[2];
    }

    public void setMateUId(Long value) {
        mateUId[2] = value;
    }

    public boolean hasMateType() {
        return mateType != null;
    }

    public byte getMateType() {
        return (Byte) mateType[2];
    }

    public void setMateType(Byte value) {
        mateType[2] = value;
    }

    public boolean hasReadLength() {
        return readLength != null;
    }

    public int getReadLength() {
        return (Integer) readLength[2];
    }

    public void setReadLength(Integer value) {
        readLength[2] = value;
    }

    public boolean hasComplexity() {
        return complexity != null;
    }

    public float getComplexity() {
        if (complexity != null)
            return (Float) complexity[2];
        else
            return 0;
    }

    public void setComplexity(Float value) {
        if (complexity != null)
            complexity[2] = value;
    }

    public boolean hasNumberOfMatches() {
        return numberOfMatches != null;
    }

    public int getNumberOfMatches() {
        return (Integer) numberOfMatches[2];
    }

    public void setNumberOfMatches(Integer value) {
        numberOfMatches[2] = value;
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
