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

import jloda.util.BlastMode;
import megan.io.IInputReader;
import megan.io.InputOutputReaderWriter;

import java.io.Closeable;
import java.io.IOException;

/**
 * access an RMA3 file
 * Created by huson on 5/13/14.
 */
public class RMA3File implements Closeable {
    final public static String READ_ONLY = "r";
    final public static String READ_WRITE = "rw";

    private final String fileName;
    private final InputOutputReaderWriter reader;

    private final FileHeaderRMA3 fileHeader;
    private final FileFooterRMA3 fileFooter;

    private final MatchFooterRMA3 matchFooter;
    private final ClassificationsFooterRMA3 classificationsFooter;

    private final AuxBlocksFooterRMA3 auxBlocksFooter;

    /**
     * construct an RMA3 object and open the named file in it
     *
     * @param fileName
     * @throws IOException
     */
    public RMA3File(String fileName, String mode) throws IOException {
        this.fileName = fileName;

        this.reader = new InputOutputReaderWriter(fileName, mode);

        fileHeader = new FileHeaderRMA3();
        try {
            fileHeader.read(reader, 0L);
        } catch (IOException ex) {
            System.err.println("File name: " + fileName);
            throw ex;
        }

        fileFooter = new FileFooterRMA3();
        reader.seek(reader.length() - 8L); //  // last long in file is position of fileFooter

        long footerPosition = reader.readLong();


        fileFooter.read(reader, footerPosition);

        matchFooter = new MatchFooterRMA3();
        matchFooter.read(reader, fileFooter.getMatchesFooter());

        classificationsFooter = new ClassificationsFooterRMA3();
        classificationsFooter.read(reader, fileFooter.getClassificationsFooter());

        auxBlocksFooter = new AuxBlocksFooterRMA3();
        auxBlocksFooter.read(reader, fileFooter.getAuxFooter());
    }

    /**
     * close this file
     *
     * @throws IOException
     */
    public void close() throws IOException {
        reader.close();
    }

    public String getFileName() {
        return fileName;
    }

    public IInputReader getReader() {
        return reader;
    }

    public FileHeaderRMA3 getFileHeader() {
        return fileHeader;
    }

    public FileFooterRMA3 getFileFooter() {
        return fileFooter;
    }

    public MatchFooterRMA3 getMatchFooter() {
        return matchFooter;
    }

    public ClassificationsFooterRMA3 getClassificationsFooter() {
        return classificationsFooter;
    }

    public AuxBlocksFooterRMA3 getAuxBlocksFooter() {
        return auxBlocksFooter;
    }

    public long getStartMatches() {
        return fileFooter.getMatchesStart();
    }

    public long getEndMatches() {
        return fileFooter.getEndMatches();
    }

    public BlastMode getBlastMode() {
        return BlastMode.valueOf(fileFooter.getBlastMode());
    }

    public String getSamFile() {
        String samFile = fileFooter.getAlignmentFile();
        if (samFile != null && samFile.length() > 0 && fileFooter.getAlignmentFileFormat().equals("SAM"))
            return samFile;
        else
            return null;
    }
}
