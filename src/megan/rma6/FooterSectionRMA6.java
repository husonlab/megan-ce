/*
 * FooterSectionRMA6.java Copyright (C) 2020. Daniel H. Huson
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
 *
 */
package megan.rma6;

import megan.io.IInputReader;
import megan.io.IInputReaderOutputWriter;
import megan.io.IOutputWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * footer for read-alignment archive format
 * Daniel Huson, 6.2015
 */
public class FooterSectionRMA6 {
    private long numberOfReads;
    private long numberOfMatches;
    private final Map<String, Long> availableClassification2Position = new HashMap<>(); // classifications available

    private long startHeaderSection;
    private long endHeaderSection;
    private long startReadsSection;
    private long endReadsSection;
    private long startClassificationsSection;
    private long endClassificationsSection;
    private long startAuxDataSection;
    private long endAuxDataSection;
    private long startFooterSection;
    private long endFooterSection;

    public void read(IInputReader reader) throws IOException {
        numberOfReads = reader.readLong();
        numberOfMatches = reader.readLong();

        int numberOfAvailableClassifications = reader.readInt();
        for (int i = 0; i < numberOfAvailableClassifications; i++) {
            final String classificationName = reader.readString();
            final long pos = reader.readLong();
            if (pos > reader.length())
                throw new IOException("Bad file position: " + pos);
            availableClassification2Position.put(classificationName, pos);
        }

        startHeaderSection = reader.readLong();
        endHeaderSection = reader.readLong();
        startReadsSection = reader.readLong();
        endReadsSection = reader.readLong();
        startClassificationsSection = reader.readLong();
        endClassificationsSection = reader.readLong();
        startAuxDataSection = reader.readLong();
        endAuxDataSection = reader.readLong();
        startFooterSection = reader.readLong();
        endFooterSection = reader.readLong();
        if (endFooterSection != reader.length())
            throw new IOException("endFooterSection mismatch");
    }

    /**
     * reads the start of the footer section
     *
     * @param reader
     * @return start position of the footer section
     * @throws IOException
     */
    public static long readStartFooterSection(IInputReaderOutputWriter reader) throws IOException {
        reader.seek(reader.length() - 16);
        return reader.readLong();
    }

    /**
     * write the footer
     *
     * @param writer
     * @throws IOException
     */
    public void write(IOutputWriter writer) throws IOException {
        writer.writeLong(numberOfReads);
        writer.writeLong(numberOfMatches);

        writer.writeInt(availableClassification2Position.size());
        for (String classificationName : availableClassification2Position.keySet()) {
            writer.writeString(classificationName);
            writer.writeLong(availableClassification2Position.get(classificationName));
        }

        writer.writeLong(startHeaderSection);
        writer.writeLong(endHeaderSection);
        writer.writeLong(startReadsSection);
        writer.writeLong(endReadsSection);
        writer.writeLong(startClassificationsSection);
        writer.writeLong(endClassificationsSection);
        writer.writeLong(startAuxDataSection);
        writer.writeLong(endAuxDataSection);
        writer.writeLong(startFooterSection);
        endFooterSection = writer.length() + 8;
        writer.writeLong(endFooterSection);
    }

    public long getNumberOfReads() {
        return numberOfReads;
    }

    public void setNumberOfReads(long numberOfReads) {
        this.numberOfReads = numberOfReads;
    }

    public long getNumberOfMatches() {
        return numberOfMatches;
    }

    public void setNumberOfMatches(long numberOfMatches) {
        this.numberOfMatches = numberOfMatches;
    }

    public Map<String, Long> getAvailableClassification2Position() {
        return availableClassification2Position;
    }

    public long getStartHeaderSection() {
        return startHeaderSection;
    }

    public void setStartHeaderSection(long startHeaderSection) {
        this.startHeaderSection = startHeaderSection;
    }

    public long getEndHeaderSection() {
        return endHeaderSection;
    }

    public void setEndHeaderSection(long endHeaderSection) {
        this.endHeaderSection = endHeaderSection;
    }

    public long getStartReadsSection() {
        return startReadsSection;
    }

    public void setStartReadsSection(long startReadsSection) {
        this.startReadsSection = startReadsSection;
    }

    public long getEndReadsSection() {
        return endReadsSection;
    }

    public void setEndReadsSection(long endReadsSection) {
        this.endReadsSection = endReadsSection;
    }

    public long getStartClassificationsSection() {
        return startClassificationsSection;
    }

    public void setStartClassificationsSection(long startClassificationsSection) {
        this.startClassificationsSection = startClassificationsSection;
    }

    public long getEndClassificationsSection() {
        return endClassificationsSection;
    }

    public void setEndClassificationsSection(long endClassificationsSection) {
        this.endClassificationsSection = endClassificationsSection;
    }

    public long getStartAuxDataSection() {
        return startAuxDataSection;
    }

    public void setStartAuxDataSection(long startAuxDataSection) {
        this.startAuxDataSection = startAuxDataSection;
    }

    public long getEndAuxDataSection() {
        return endAuxDataSection;
    }

    public void setEndAuxDataSection(long endAuxDataSection) {
        this.endAuxDataSection = endAuxDataSection;
    }

    public long getStartFooterSection() {
        return startFooterSection;
    }

    public void setStartFooterSection(long startFooterSection) {
        this.startFooterSection = startFooterSection;
    }

    public long getEndFooterSection() {
        return endFooterSection;
    }

    public void setEndFooterSection(long endFooterSection) {
        this.endFooterSection = endFooterSection;
    }

    /**
     * gets the start position for a named classification
     *
     * @param classificationName
     * @return start or -1
     * @throws IOException
     */
    public Long getStartClassification(String classificationName) throws IOException {
        return getAvailableClassification2Position().get(classificationName);
    }

}
