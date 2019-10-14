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

import jloda.util.Basic;
import megan.core.SampleAttributeTable;
import megan.data.LocationManager;
import megan.data.TextStoragePolicy;
import megan.io.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * rma2 file
 * Daniel Huson, 10.2010
 */
public class RMA2File {
    public final static int MAGIC_NUMBER = ('R' << 3) | ('M' << 2) | ('A' << 1) | ('R');

    public static final byte CHECK_BYTE = (byte) 255;

    private final File file;

    private final InfoSection infoSection;
    private String creator = "MEGAN";

    /**
     * constructor
     *
     * @param file
     * @throws IOException
     */
    public RMA2File(File file) throws IOException {
        this.file = file;
        infoSection = new InfoSection();
    }

    /**
     * gets the version of the RMA file
     *
     * @param file
     * @return RMA version number or 0, if not an RMA file
     */
    static public int getRMAVersion(File file) {
        try {
            try (InputReader r = new InputReader(file, null, null, true)) {
                int magicNumber = r.readInt();
                if (magicNumber != MAGIC_NUMBER)
                    throw new IOException("Not an RMA file");
                return r.readInt(); // version
            }

        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * get the info section. When opening a file, first call loadInfoSection()
     *
     * @return info section
     */
    public InfoSection getInfoSection() {
        return infoSection;
    }

    /**
     * load the info section
     *
     * @throws IOException
     */
    public InfoSection loadInfoSection() throws IOException {
        try (InputReader reader = new InputReader(getFile(), null, null, true)) {
            readHeader(reader);
            infoSection.read(reader);
        }
        // System.err.println("Loaded---------\n"+infoSection.toString());
        return infoSection;
    }

    /**
     * store the infosection
     *
     * @throws IOException
     */
    private void storeInfoSection() throws IOException {
        try (InputOutputReaderWriter io = new InputOutputReaderWriter(file, "rw")) {
            if (infoSection.getInfoSectionStart() <= 0)
                throw new IOException("getInfoSectionStart(), illegal value: " + infoSection.getInfoSectionStart());
            io.seek(infoSection.getInfoSectionStart());
            infoSection.write(io);
            io.setLength(io.getPosition());
        }
    }

    /**
     * opens a writer to the main file
     *
     * @return
     * @throws IOException
     */
    public OutputWriter getFileWriter() throws IOException {
        OutputWriter w = new OutputWriter(file);
        writeHeader(w);
        return w;
    }

    /**
     * opens a writer to the named dump file
     *
     * @param file
     * @return writer
     * @throws IOException
     */
    public OutputWriter getDataDumpWriter(File file) throws IOException {
        OutputWriter w = new OutputWriter(file);
        writeHeader(w);
        return w;
    }

    /**
     * gets a append for the
     *
     * @return
     * @throws IOException
     */
    public InputOutputReaderWriter getFileAppender() throws IOException {
        InputOutputReaderWriter io = new InputOutputReaderWriter(getFile(), "rw");
        io.seek(io.length());
        return io;
    }

    /**
     * get the data index readerWriter
     *
     * @return data index readerWriter
     * @throws IOException
     */
    public InputReader getDataIndexReader() throws IOException {
        loadInfoSection();
        boolean useRelativeFilePositions = (infoSection.getTextStoragePolicy() == TextStoragePolicy.Embed);
        InputReader r = new InputReader(getFile(), infoSection.getDataIndexSectionStart(), infoSection.getDataIndexSectionEnd(), !useRelativeFilePositions);
        if (useRelativeFilePositions)
            r.seek(0);
        else
            r.seek(infoSection.getDataIndexSectionStart());
        check((byte) r.read(), CHECK_BYTE);
        return r;
    }

    /**
     * get data index readerWriter-writer. Use only to modify existing index, not to create a new one
     *
     * @return readerWriter-writer
     * @throws IOException
     */
    public InputOutputReaderWriter getDataIndexModifier() throws IOException {
        loadInfoSection();
        InputOutputReaderWriter io = new InputOutputReaderWriter(getFile(), "rw");
        if (infoSection.getTextStoragePolicy() == TextStoragePolicy.Embed) {
            // this is dirty: when using text_onboard policy, the data index is first written to a separate file
            // and then copied to the main file. In this case, the file positions have be to offset
            io.setOffset(infoSection.getDataIndexSectionStart());  // need to do this because readUids are locations relative to dataindexstart
            io.seek(0);
        } else
            io.seek(infoSection.getDataIndexSectionStart());
        check((byte) io.read(), CHECK_BYTE);
        return io;
    }

    public InputReader getClassificationIndexReader(String classificationName) throws IOException {
        loadInfoSection();
        int id = infoSection.getClassificationNumber(classificationName);
        return new InputReader(getFile(), infoSection.getClassificationIndexSectionStart(id), infoSection.getClassificationIndexSectionEnd(id), true);
    }

    public InputReader getClassificationDumpReader(String classificationName) throws IOException {
        loadInfoSection();
        int id = infoSection.getClassificationNumber(classificationName);
        return new InputReader(getFile(), infoSection.getClassificationDumpSectionStart(id), infoSection.getClassificationDumpSectionEnd(id), true);
    }

    /**
     * gets the temporary readerWriter
     *
     * @return
     * @throws IOException
     */
    public OutputWriter getTmpIndexFileWriter() throws IOException {
        OutputWriter w = new OutputWriter(getIndexTmpFile());
        w.write(CHECK_BYTE);
        return w;
    }

    public InputReader getTmpIndexFileReader() throws IOException {
        InputReader r = new InputReader(getIndexTmpFile(), null, null, true);
        check((byte) r.read(), CHECK_BYTE);
        return r;
    }


    private void writeHeader(IOutputWriter w) throws IOException {
        w.writeInt(MAGIC_NUMBER);
        w.writeInt(2); // version 2
        w.writeString(creator);
    }

    private void readHeader(IInputReader r) throws IOException {
        int magicNumber = r.readInt();
        if (magicNumber != MAGIC_NUMBER)
            throw new IOException("Not a RMA2 file");
        int version = r.readInt();
        if (version != 2)
            throw new IOException("Not a RMA2 file, wrong version: " + version);

        creator = r.readString();
    }

    /**
     * check whether value read is the same as the value expected
     *
     * @param got
     * @param expected
     * @throws IOException
     */
    static public void check(long got, long expected) throws IOException {
        if (expected != got)
            throw new IOException("RMA2 file corrupt? Expected: " + expected + ", got: " + got);
    }

    private File getFile() {
        return file;
    }

    public File getIndexTmpFile() {
        File tmpFile = new File(file.getParent(), Basic.replaceFileSuffix(file.getName(), ".tmp0"));
        tmpFile.deleteOnExit();
        return tmpFile;
    }

    public File getClassificationIndexTmpFile() {
        File tmpFile = new File(file.getParent(), Basic.replaceFileSuffix(file.getName(), ".tmp1"));
        tmpFile.deleteOnExit();
        return tmpFile;
    }

    /**
     * gets the creation date of this dataset
     *
     * @return date
     * @throws IOException
     */
    public long getCreationDate() throws IOException {
        loadInfoSection();
        return infoSection.getCreationDate();
    }

    /**
     * gets the list of classification names
     *
     * @return names
     * @throws IOException
     */
    public String[] getClassificationNames() throws IOException {
        loadInfoSection();
        return infoSection.getClassificationNames();
    }

    public int getClassificationSize(String classificationName) throws IOException {
        loadInfoSection();
        int id = infoSection.getClassificationNumber(classificationName);
        if (id < 0)
            return 0;
        else
            return infoSection.getClassificationSize(id);
    }


    /**
     * gets the number of reads
     *
     * @return reads
     * @throws IOException
     */
    public int getNumberOfReads() throws IOException {
        loadInfoSection();
        return infoSection.getNumberOfReads();
    }


    /**
     * gets the number of matches
     *
     * @return matches
     * @throws IOException
     */
    public int getNumberOfMatches() throws IOException {
        loadInfoSection();
        return infoSection.getNumberOfMatches();
    }

    public void setNumberOfReads(int numberOfReads) throws IOException {
        loadInfoSection();
        infoSection.setNumberOfReads(numberOfReads);
        storeInfoSection();
    }

    public void setNumberOfMatches(int numberOfMatches) throws IOException {
        loadInfoSection();
        infoSection.setNumberOfMatches(numberOfMatches);
        storeInfoSection();
    }


    /**
     * replace the auxiliary data associated with the dataset
     *
     * @param label2data
     * @throws IOException
     */
    public void replaceAuxiliaryData(Map<String, byte[]> label2data) throws IOException {
        loadInfoSection();

        try (InputOutputReaderWriter io = new InputOutputReaderWriter(new FileRandomAccessReadWriteAdapter(file.getPath(), "rw"))) {
            long newPos = infoSection.getAuxiliaryDataStart();
            if (newPos == 0)
                newPos = infoSection.getInfoSectionStart();
            io.seek(newPos);
            infoSection.setAuxiliaryDataStart(newPos);
            StringBuilder buf = new StringBuilder();
            for (String label : label2data.keySet()) {
                byte[] bytes = label2data.get(label);
                if (bytes != null) {
                    buf.append("<<<").append(label).append(">>>").append(new String(bytes));
                }
            }
            io.write(buf.toString().getBytes());

            infoSection.setAuxiliaryDataEnd(io.getPosition());
            infoSection.write(io);
            io.setLength(io.getPosition());
        }
        // System.err.println("Saved summary:\n" + Basic.toString(auxiliaryData));
    }


    /**
     * gets the summary section of the file
     *
     * @return
     * @throws IOException
     */
    public Map<String, byte[]> getAuxiliaryData() throws IOException {
        loadInfoSection();

        Map<String, byte[]> result = new HashMap<>();

        if (infoSection.isHasAuxiliaryMap()) {
            String auxiliaryDataString;
            try (InputReader r = new InputReader(getFile(), infoSection.getAuxiliaryDataStart(), infoSection.getAuxiliaryDataEnd(), false)) {
                int size = (int) r.length();
                byte[] bytes = new byte[size];
                r.read(bytes, 0, size);
                auxiliaryDataString = new String(bytes);
            }
            String prevLabel = null;
            int prevDataStart = 0;

            for (int startPos = auxiliaryDataString.indexOf("<<<"); startPos != -1; startPos = auxiliaryDataString.indexOf("<<<", startPos + 1)) {
                if (prevLabel != null) {
                    String data = auxiliaryDataString.substring(prevDataStart, startPos);
                    result.put(prevLabel, data.getBytes());
                    prevLabel = null;
                }
                int endPos = auxiliaryDataString.indexOf(">>>", startPos + 1);
                if (startPos < endPos) {
                    prevLabel = auxiliaryDataString.substring(startPos + 3, endPos);
                    prevDataStart = endPos + 3;
                }
            }
            if (prevLabel != null) {
                String data = auxiliaryDataString.substring(prevDataStart);
                result.put(prevLabel, data.getBytes());
            }
        } else {
            String auxiliaryDataString;
            try (InputReader r = new InputReader(getFile(), infoSection.getAuxiliaryDataStart(), infoSection.getAuxiliaryDataEnd(), false)) {
                int size = (int) r.length();
                byte[] bytes = new byte[size];
                r.read(bytes, 0, size);
                auxiliaryDataString = new String(bytes);
            }
            int pos = auxiliaryDataString.indexOf("BEGIN_METADATA_TABLE");
            if (pos != -1) {
                byte[] bytes = auxiliaryDataString.substring(0, pos).getBytes();
                result.put(SampleAttributeTable.USER_STATE, bytes);
                pos += "BEGIN_METADATA_TABLE".length();
                bytes = auxiliaryDataString.substring(pos).getBytes();
                result.put(SampleAttributeTable.SAMPLE_ATTRIBUTES, bytes);
            } else
                result.put(SampleAttributeTable.USER_STATE, auxiliaryDataString.getBytes());
        }
        return result;
    }

    /**
     * gets a classification block
     *
     * @param classificationName
     * @return
     * @throws IOException
     */
    public ClassificationBlockRMA2 getClassificationBlock(String classificationName) throws IOException {
        ClassificationBlockRMA2 classificationBlock = new ClassificationBlockRMA2(classificationName);
        classificationBlock.load(getClassificationIndexReader(classificationName));
        return classificationBlock;
    }

    /**
     * get the location manager associated with this file
     *
     * @return locationManager
     * @throws IOException
     */
    public LocationManager getLocationManager() throws IOException {
        InfoSection infoSection = loadInfoSection();
        return infoSection.getLocationManager(getFile());
    }

    /**
     * replace the location manager by a new one
     *
     * @param locationManager
     * @throws IOException
     */
    public void replaceLocationManager(LocationManager locationManager) throws IOException {
        InfoSection infoSection = loadInfoSection();
        infoSection.syncLocationManager2InfoSection(locationManager);
        storeInfoSection();
    }
}
