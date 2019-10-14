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

import megan.data.LocationManager;
import megan.data.TextStoragePolicy;
import megan.io.IInputReader;
import megan.io.IOutputWriter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Objects;

/**
 * info section of RMA2 file
 * Daniel Huson, 10.2010
 */
public class InfoSection {
    static private final byte VERSION_RMA2_0 = (byte) 255; // original RMA2 format
    static private final byte VERSION_RMA2_1 = (byte) 254; // RMA2 format with READ and MATCH fixed-part formatting

    private byte version = VERSION_RMA2_0;

    private final byte AUXILIARY_DATA_AS_MAP_BYTE = (byte) 254;

    private long creationDate;
    private long modificationDate;
    private int numberOfReads;
    private int numberOfMatches;

    private TextStoragePolicy textStoragePolicy;
    private String[] textFileNames;
    private Long[] textFileSizes;

    private RMA2Formatter rma2Formatter;

    // text sections if textLocationType==ONBOARD
    private long dataDumpSectionStart;
    private long dataDumpSectionEnd; // first position after datasection

    private long dataIndexSectionStart;
    private long dataIndexSectionEnd;

    private String[] classificationNames = new String[0];
    private Integer[] classificationSizes = new Integer[0];
    private Long[] classificationDumpSectionStart = new Long[0];
    private Long[] classificationDumpSectionEnd = new Long[0];
    private Long[] classificationIndexSectionStart = new Long[0];
    private Long[] classificationIndexSectionEnd = new Long[0];

    private long auxiliaryDataStart;
    private long auxiliaryDataEnd;

    private boolean hasAuxiliaryMap;

    private long infoSectionStart;

    /**
     * constructor
     */
    public InfoSection() {
        creationDate = System.currentTimeMillis();
        modificationDate = creationDate;

        version = VERSION_RMA2_1;
        rma2Formatter = new RMA2Formatter(ReadBlockRMA2Formatter.DEFAULT_RMA2_5, MatchBlockRMA2Formatter.DEFAULT_RMA2_5);
    }

    /**
     * write to end of file
     *
     * @param w
     * @throws IOException
     */
    public void write(IOutputWriter w) throws IOException {
        infoSectionStart = w.getPosition();
        w.write(version);

        w.writeLong(creationDate);
        w.writeLong(modificationDate);

        w.writeInt(numberOfReads);
        w.writeInt(numberOfMatches);

        if (version != VERSION_RMA2_0) {
            w.writeString(rma2Formatter.getReadBlockRMA2Formatter().toString());
            w.writeString(rma2Formatter.getMatchBlockRMA2Formatter().toString());
        }

        w.write(TextStoragePolicy.getId(textStoragePolicy));

        switch (textStoragePolicy) {
            case Embed:
                w.writeLong(dataDumpSectionStart);
                w.writeLong(dataDumpSectionEnd);
                break;
            case InRMAZ:
            case Reference:
                w.writeInt(textFileNames.length);
                for (String textFileName : textFileNames) {
                    w.writeString(Objects.requireNonNullElse(textFileName, ""));
                }
                if (textFileSizes == null)
                    w.writeInt(0);
                else {
                    w.writeInt(textFileSizes.length);
                    for (Long textFileSize : textFileSizes) {
                        w.writeLong(Objects.requireNonNullElse(textFileSize, -1L));
                    }
                }
                break;
            default:
                throw new IOException("Unknown textStoragePolicy: " + textStoragePolicy);
        }

        w.writeLong(dataIndexSectionStart);
        w.writeLong(dataIndexSectionEnd);

        w.writeInt(classificationNames.length);
        for (String name : classificationNames)
            w.writeString(name);
        for (int size : classificationSizes)
            w.writeInt(size);
        for (long pos : classificationIndexSectionStart)
            w.writeLong(pos);
        for (long pos : classificationIndexSectionEnd)
            w.writeLong(pos);
        for (long pos : classificationDumpSectionStart)
            w.writeLong(pos);
        for (long pos : classificationDumpSectionEnd)
            w.writeLong(pos);
        w.writeLong(auxiliaryDataStart);
        w.writeLong(auxiliaryDataEnd);
        w.write(AUXILIARY_DATA_AS_MAP_BYTE);
        w.write(RMA2File.CHECK_BYTE);
        w.writeLong(infoSectionStart);
    }

    /**
     * read from end of file
     *
     * @param r
     * @throws IOException
     */
    public void read(IInputReader r) throws IOException {
        r.seek(r.length() - 9);
        RMA2File.check((byte) r.read(), RMA2File.CHECK_BYTE);
        infoSectionStart = r.readLong();
        r.seek(infoSectionStart);
        version = (byte) r.read();
        if (!(version == VERSION_RMA2_0 || version == VERSION_RMA2_1))
            throw new IOException("Unsupported subversion of RMA2 format (MEGAN version too old?)");

        setCreationDate(r.readLong());
        if (getCreationDate() < 1292313486909L) // 14.Dec 2010
            throw new IOException("RMA2 file generated by alpha version, too old, please regenerate");
        setModificationDate(r.readLong());

        setNumberOfReads(r.readInt());
        setNumberOfMatches(r.readInt());

        if (version == VERSION_RMA2_0) {
            rma2Formatter = new RMA2Formatter(ReadBlockRMA2Formatter.DEFAULT_RMA2_0, MatchBlockRMA2Formatter.DEFAULT_RMA2_0);
        } else // later versions have explicit format strings for reads and matches
        {
            rma2Formatter = new RMA2Formatter(r.readString(), r.readString());
        }

        setTextStoragePolicy(TextStoragePolicy.fromId((byte) r.read()));

        if (getTextStoragePolicy() == TextStoragePolicy.Embed) {
            setDataDumpSectionStart(r.readLong());
            setDataDumpSectionEnd(r.readLong());
            textFileNames = new String[0];
            textFileSizes = new Long[0];
        } else if (getTextStoragePolicy() == TextStoragePolicy.Reference || getTextStoragePolicy() == TextStoragePolicy.InRMAZ) {
            int length = r.readInt();
            textFileNames = new String[length];
            for (int i = 0; i < textFileNames.length; i++) {
                textFileNames[i] = r.readString();
            }
            length = r.readInt();
            textFileSizes = new Long[length];
            for (int i = 0; i < textFileSizes.length; i++) {
                textFileSizes[i] = r.readLong();
            }
            setDataDumpSectionStart(0);
            setDataDumpSectionEnd(0);
        } else
            throw new IOException("Unknown textStoragePolicy: " + textStoragePolicy);

        setDataIndexSectionStart(r.readLong());
        setDataIndexSectionEnd(r.readLong());

        setNumberOfClassifications(r.readInt());
        for (int i = 0; i < getNumberOfClassifications(); i++)
            setClassificationName(i, r.readString());
        for (int i = 0; i < getNumberOfClassifications(); i++)
            setClassificationSize(i, r.readInt());
        for (int i = 0; i < getNumberOfClassifications(); i++)
            setClassificationIndexSectionStart(i, r.readLong());
        for (int i = 0; i < getNumberOfClassifications(); i++)
            setClassificationIndexSectionEnd(i, r.readLong());
        for (int i = 0; i < getNumberOfClassifications(); i++)
            setClassificationDumpSectionStart(i, r.readLong());
        for (int i = 0; i < getNumberOfClassifications(); i++)
            setClassificationDumpSectionEnd(i, r.readLong());

        setAuxiliaryDataStart(r.readLong());
        setAuxiliaryDataEnd(r.readLong());

        byte aByte = (byte) r.read();

        if (aByte == AUXILIARY_DATA_AS_MAP_BYTE) {
            setHasAuxiliaryMap(true);
            RMA2File.check((byte) r.read(), RMA2File.CHECK_BYTE);
        } else {
            setHasAuxiliaryMap(false);
            RMA2File.check(aByte, RMA2File.CHECK_BYTE);
        }

        RMA2File.check(r.readLong(), infoSectionStart);
    }

    /**
     * write in human readable form
     *
     * @return string
     */
    public String toString() {
        StringWriter w = new StringWriter();
        if (version == VERSION_RMA2_0)
            w.write("RMA file version: 2.0\n");
        else if (version == VERSION_RMA2_1)
            w.write("RMA file version: 2.1\n");
        else
            w.write("RMA file version: 2.* (" + version + ")\n");

        w.write("creationDate: " + new Date(getCreationDate()) + "\n");
        w.write("modificationDate: " + new Date(getModificationDate()) + "\n");
        w.write("numberOfReads: " + getNumberOfReads() + "\n");
        w.write("numberOfMatches: " + getNumberOfMatches() + "\n");

        w.write("textStoragePolicy: " + getTextStoragePolicy() + "(" + TextStoragePolicy.getDescription(getTextStoragePolicy()) + ")\n");
        if (getTextFileNames() != null) {
            w.write("Source text files:\n");
            for (int i = 0; i < getTextFileNames().length; i++) {
                w.write("\t" + getTextFileNames()[i]);
                if (i < getTextFileSizes().length)
                    w.write("\t" + getTextFileSizes()[i]);
                w.write("\n");
            }
        }

        w.write("Reads format:   " + rma2Formatter.getReadBlockRMA2Formatter().toString() + "\n");
        w.write("Matches format: " + rma2Formatter.getMatchBlockRMA2Formatter().toString() + "\n");

        w.write("dataDumpSectionStart: " + getDataDumpSectionStart() + "\n");
        w.write("dataDumpSectionEnd: " + getDataDumpSectionEnd() + "\n");

        w.write("dataIndexSectionStart: " + getDataIndexSectionStart() + "\n");
        w.write("dataIndexSectionEnd: " + getDataIndexSectionEnd() + "\n");


        w.write("Number of classifications: " + getClassificationNames().length + "\n");
        w.write("classificationNames:");
        for (String name : getClassificationNames())
            w.write(" " + name);
        w.write("\n");
        w.write("classificationSizes:");
        for (int size : classificationSizes)
            w.write(" " + size);
        w.write("\n");
        w.write("classificationIndexSectionStart:");
        for (long pos : classificationIndexSectionStart)
            w.write(" " + pos);
        w.write("\n");
        w.write("classificationIndexSectionEnd:");
        for (long pos : classificationIndexSectionEnd)
            w.write(" " + pos);
        w.write("\n");
        w.write("classificationDumpSectionStart:");
        for (long pos : classificationDumpSectionStart)
            w.write(" " + pos);
        w.write("\n");
        w.write("classificationDumpSectionEnd:");
        for (long pos : classificationDumpSectionEnd)
            w.write(" " + pos);
        w.write("\n");
        w.write("hasAuxiliaryMap: " + hasAuxiliaryMap + "\n");
        w.write("userStateSectionStart: " + getAuxiliaryDataStart() + "\n");
        w.write("userStateSectionEnd:   " + getAuxiliaryDataEnd() + "\n");
        w.write("infoSectionStart: " + getInfoSectionStart() + "\n");
        return w.toString();
    }

    public long getCreationDate() {
        return creationDate;
    }

    private void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    private long getModificationDate() {
        return modificationDate;
    }

    private void setModificationDate(long modificationDate) {
        this.modificationDate = modificationDate;
    }

    public void updateModificationDate() {
        modificationDate = System.currentTimeMillis();
    }

    public int getNumberOfReads() {
        return numberOfReads;
    }

    public void setNumberOfReads(int numberOfReads) {
        this.numberOfReads = numberOfReads;
    }

    public int getNumberOfMatches() {
        return numberOfMatches;
    }

    public void setNumberOfMatches(int numberOfMatches) {
        this.numberOfMatches = numberOfMatches;
    }

    public int getClassificationSize(int i) {
        return classificationSizes[i];
    }

    private void setClassificationSize(int i, int size) {
        this.classificationSizes[i] = size;
    }

    private long getDataDumpSectionStart() {
        return dataDumpSectionStart;
    }

    public void setDataDumpSectionStart(long dataDumpSectionStart) {
        this.dataDumpSectionStart = dataDumpSectionStart;
    }

    private long getDataDumpSectionEnd() {
        return dataDumpSectionEnd;
    }

    public void setDataDumpSectionEnd(long dataDumpSectionEnd) {
        this.dataDumpSectionEnd = dataDumpSectionEnd;
    }

    public long getDataIndexSectionStart() {
        return dataIndexSectionStart;
    }

    public void setDataIndexSectionStart(long dataIndexSectionStart) {
        this.dataIndexSectionStart = dataIndexSectionStart;
    }

    public long getDataIndexSectionEnd() {
        return dataIndexSectionEnd;
    }

    public void setDataIndexSectionEnd(long dataIndexSectionEnd) {
        this.dataIndexSectionEnd = dataIndexSectionEnd;
    }

    public void setNumberOfClassifications(int numberOfClassifications) {
        classificationIndexSectionStart = new Long[numberOfClassifications];
        classificationIndexSectionEnd = new Long[numberOfClassifications];
        classificationDumpSectionStart = new Long[numberOfClassifications];
        classificationDumpSectionEnd = new Long[numberOfClassifications];
        classificationNames = new String[numberOfClassifications];
        classificationSizes = new Integer[numberOfClassifications];
    }

    public long getClassificationIndexSectionStart(int i) {
        return classificationIndexSectionStart[i];
    }

    private void setClassificationIndexSectionStart(int i, long classificationSectionStart) {
        this.classificationIndexSectionStart[i] = classificationSectionStart;
    }

    public long getClassificationIndexSectionEnd(int i) {
        return classificationIndexSectionEnd[i];
    }

    private void setClassificationIndexSectionEnd(int i, long classificationSectionEnd) {
        this.classificationIndexSectionEnd[i] = classificationSectionEnd;
    }

    public long getClassificationDumpSectionStart(int i) {
        return classificationDumpSectionStart[i];
    }

    private void setClassificationDumpSectionStart(int i, long classificationSectionStart) {
        this.classificationDumpSectionStart[i] = classificationSectionStart;
    }

    public long getClassificationDumpSectionEnd(int i) {
        return classificationDumpSectionEnd[i];
    }

    private void setClassificationDumpSectionEnd(int i, long classificationSectionEnd) {
        this.classificationDumpSectionEnd[i] = classificationSectionEnd;
    }

    private int getNumberOfClassifications() {
        return classificationNames.length;
    }

    public String[] getClassificationNames() {
        return classificationNames;
    }

    public String[] getClassificationName(int i) {
        return classificationNames;
    }

    private void setClassificationName(int i, String classificationName) {
        this.classificationNames[i] = classificationName;
    }

    public long getAuxiliaryDataStart() {
        return auxiliaryDataStart;
    }

    public void setAuxiliaryDataStart(long auxiliaryDataStart) {
        this.auxiliaryDataStart = auxiliaryDataStart;
    }

    public long getAuxiliaryDataEnd() {
        return auxiliaryDataEnd;
    }

    public void setAuxiliaryDataEnd(long auxiliaryDataEnd) {
        this.auxiliaryDataEnd = auxiliaryDataEnd;
    }

    public boolean isHasAuxiliaryMap() {
        return hasAuxiliaryMap;
    }

    private void setHasAuxiliaryMap(boolean hasAuxiliaryMap) {
        this.hasAuxiliaryMap = hasAuxiliaryMap;
    }

    public TextStoragePolicy getTextStoragePolicy() {
        return textStoragePolicy;
    }

    public void setTextStoragePolicy(TextStoragePolicy textStoragePolicy) {
        this.textStoragePolicy = textStoragePolicy;
    }

    private String[] getTextFileNames() {
        return textFileNames;
    }

    public void setTextFileNames(String[] textFileNames) {
        this.textFileNames = textFileNames;
    }

    private Long[] getTextFileSizes() {
        return textFileSizes;
    }

    public void setTextFileSizes(Long[] textFileSizes) {
        this.textFileSizes = textFileSizes;
    }

    public long getInfoSectionStart() {
        return infoSectionStart;
    }

    /**
     * gets the index of the given classification
     *
     * @param classificationName
     * @return index or -1
     */
    public int getClassificationNumber(String classificationName) {
        for (int i = 0; i < classificationNames.length; i++)
            if (classificationName.equals(classificationNames[i]))
                return i;
        return -1;
    }

    /**
     * add a new classification
     *
     * @param name
     * @param size
     * @param dumpStart
     * @param dumpEnd
     * @param indexStart
     * @param indexEnd
     */
    public void addClassification(String name, int size, long dumpStart, long dumpEnd, long indexStart, long indexEnd) {
        classificationNames = extend(classificationNames, name);
        classificationSizes = extend(classificationSizes, size);
        classificationDumpSectionStart = extend(classificationDumpSectionStart, dumpStart);
        classificationDumpSectionEnd = extend(classificationDumpSectionEnd, dumpEnd);
        classificationIndexSectionStart = extend(classificationIndexSectionStart, indexStart);
        classificationIndexSectionEnd = extend(classificationIndexSectionEnd, indexEnd);
    }

    private static String[] extend(String[] array, String add) {
        String[] newArray = new String[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[newArray.length - 1] = add;
        return newArray;
    }

    private static Long[] extend(Long[] array, Long add) {
        Long[] newArray = new Long[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[newArray.length - 1] = add;
        return newArray;
    }

    private static Integer[] extend(Integer[] array, Integer add) {
        Integer[] newArray = new Integer[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[newArray.length - 1] = add;
        return newArray;
    }

    /**
     * gets the location manager stored in this info section
     *
     * @return location manager
     */
    public LocationManager getLocationManager(File onboardFile) {
        LocationManager locationManager = new LocationManager(textStoragePolicy);
        if (getTextStoragePolicy() == TextStoragePolicy.Embed)
            locationManager.addFile(onboardFile);
        else {
            for (int i = 0; i < textFileNames.length; i++) {
                if (i < textFileSizes.length)
                    locationManager.addFile(new File(textFileNames[i]), textFileSizes[i]);
                else
                    locationManager.addFile(new File(textFileNames[i]));
            }
        }
        return locationManager;
    }

    /**
     * set the location manager
     *
     * @param locationManager
     */
    public void syncLocationManager2InfoSection(LocationManager locationManager) throws IOException {
        if (textStoragePolicy != locationManager.getTextStoragePolicy())
            throw new IOException("setLocationManager(): attempting to change textStoragePolicy from " +
                    textStoragePolicy + " to " + locationManager.getTextStoragePolicy());
        textStoragePolicy = locationManager.getTextStoragePolicy();
        textFileNames = locationManager.getFileNames();
        textFileSizes = locationManager.getFileSizes();
    }

    /**
     * gets the formatter associated with the file
     *
     * @return formatter
     */
    public RMA2Formatter getRMA2Formatter() {
        return rma2Formatter;

    }
}
