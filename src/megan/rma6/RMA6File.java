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

import megan.io.IInputReader;
import megan.io.IInputReaderOutputWriter;
import megan.io.InputOutputReaderWriter;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * read-alignment archive file
 * Daniel Huson, 6.2015
 */
public class RMA6File implements Closeable {
    public final static int MAGIC_NUMBER = ('R' << 3) | ('M' << 2) | ('A' << 1) | ('R');
    public final static int VERSION = 6;
    public final static int MINOR_VERSION = 0;

    final public static String READ_ONLY = "r";
    final static String READ_WRITE = "rw";

    private final HeaderSectionRMA6 headerSectionRMA6;
    final FooterSectionRMA6 footerSectionRMA6;

    String fileName;
    IInputReaderOutputWriter readerWriter;

    /**
     * constructor
     */
    public RMA6File() {
        headerSectionRMA6 = new HeaderSectionRMA6();
        footerSectionRMA6 = new FooterSectionRMA6();
    }

    /**
     * constructor
     */
    public RMA6File(String fileName, String mode) throws IOException {
        headerSectionRMA6 = new HeaderSectionRMA6();
        footerSectionRMA6 = new FooterSectionRMA6();
        load(fileName, mode);
    }

    /**
     * load an existing file
     *
     * @param fileName
     * @param mode
     * @throws IOException
     */
    private void load(String fileName, String mode) throws IOException {
        this.fileName = fileName;

        this.readerWriter = new InputOutputReaderWriter(fileName, mode);
        headerSectionRMA6.read(readerWriter);
        readerWriter.seek(FooterSectionRMA6.readStartFooterSection(readerWriter));
        footerSectionRMA6.read(readerWriter);
    }

    /**
     * close the file
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (readerWriter != null) {
            readerWriter.close();
            readerWriter = null;
        }
    }

    public HeaderSectionRMA6 getHeaderSectionRMA6() {
        return headerSectionRMA6;
    }

    public FooterSectionRMA6 getFooterSectionRMA6() {
        return footerSectionRMA6;
    }

    public IInputReader getReader() {
        return readerWriter;
    }

    /**
     * reads the aux blocks
     *
     * @return aux blocks
     * @throws IOException
     */
    public Map<String, byte[]> readAuxBlocks() throws IOException {
        final Map<String, byte[]> label2data = new HashMap<>();
        readerWriter.seek(footerSectionRMA6.getStartAuxDataSection());
        final int count = readerWriter.readInt();
        for (int i = 0; i < count && readerWriter.getPosition() < footerSectionRMA6.getEndAuxDataSection(); i++) {
            String name = readerWriter.readString();
            int length = readerWriter.readInt();
            byte[] bytes = new byte[length];
            readerWriter.read(bytes, 0, length);
            label2data.put(name, bytes);
        }
        return label2data;
    }

    /**
     * write aux blocks
     *
     * @throws IOException
     */
    public void writeAuxBlocks(Map<String, byte[]> label2data) throws IOException {
        getFooterSectionRMA6().setStartAuxDataSection(readerWriter.getPosition());
        if (label2data == null)
            readerWriter.writeInt(0);
        else {
            readerWriter.writeInt(label2data.size()); // number of aux blocks
            for (String label : label2data.keySet()) {
                readerWriter.writeStringNoCompression(label);
                final byte[] data = label2data.get(label);
                if (data == null)
                    readerWriter.writeInt(0);
                else {
                    readerWriter.writeInt(data.length);
                    readerWriter.write(data);
                }
            }
        }
        getFooterSectionRMA6().setEndAuxDataSection(readerWriter.getPosition());
    }
}
