/*
 * RMA2Creator.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.rma2;

import megan.data.IReadBlockWithLocation;
import megan.data.LocationManager;
import megan.data.TextStoragePolicy;
import megan.io.InputReader;
import megan.io.OutputWriter;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * used to create a new RMA 2file
 * Daniel Huson, 10.2010
 */
public class RMA2Creator {
    private final RMA2File rma2File;

    private final TextStoragePolicy textStoragePolicy;

    private final LocationManager locationManager;

    private final OutputWriter fileWriter;  // writer to file
    private final OutputWriter dumpWriter;   // writer to optimal dump file
    private final OutputWriter tmpWriter;    // writer to temporary file for ONBOARD

    private final InfoSection infoSection;
    private final RMA2Formatter rma2Formatter;

    private int numberOfReads = 0;
    private int numberOfMatches = 0;

    /**
     * open the file for creation
     *
     * @param file
     * @param textStoragePolicy
     * @throws IOException
     */
    public RMA2Creator(File file, TextStoragePolicy textStoragePolicy, LocationManager locationManager) throws IOException {
        rma2File = new RMA2File(file);
        this.textStoragePolicy = textStoragePolicy;
        this.locationManager = locationManager;

        infoSection = rma2File.getInfoSection();
        rma2Formatter = infoSection.getRMA2Formatter();
        infoSection.setTextStoragePolicy(textStoragePolicy);

        switch (textStoragePolicy) {
            case Embed:
                infoSection.setTextFileNames(new String[0]);
                infoSection.setTextFileSizes(new Long[0]);
                fileWriter = rma2File.getFileWriter();
                // no need to write check_byte here, required by dataindex, because is written in creation method:
                tmpWriter = rma2File.getTmpIndexFileWriter();
                dumpWriter = null;
                infoSection.setDataDumpSectionStart(fileWriter.getPosition());
                break;
            case InRMAZ:
                if (locationManager.getFiles().size() != 1)
                    throw new IOException("Wrong number of dump-file names: " + locationManager.getFileNames().length);
                infoSection.setTextFileNames(locationManager.getFileNames());
                infoSection.setTextFileSizes(new Long[0]);
                File dumpFile = locationManager.getFile(0);
                fileWriter = rma2File.getFileWriter();
                dumpWriter = rma2File.getDataDumpWriter(dumpFile);
                tmpWriter = null;
                infoSection.setDataIndexSectionStart(fileWriter.getPosition());
                fileWriter.write(RMA2File.CHECK_BYTE); // required by dataindex
                break;
            case Reference:
                infoSection.setTextFileNames(locationManager.getFileNames());
                infoSection.setTextFileSizes(locationManager.getFileSizes());
                fileWriter = rma2File.getFileWriter();
                dumpWriter = null;
                tmpWriter = null;
                infoSection.setDataIndexSectionStart(fileWriter.getPosition());
                fileWriter.write(RMA2File.CHECK_BYTE);  // required by dataindex
                break;
            default:
                throw new IOException("Unknown textStoragePolicy: " + textStoragePolicy);
        }
    }

    /**
     * write a read block to the file
     *
     * @param readBlock
     * @throws IOException
     */
    public void writeReadBlock(IReadBlockWithLocation readBlock) throws IOException {
        numberOfReads++;
        numberOfMatches += readBlock.getNumberOfMatches();
        switch (textStoragePolicy) {
            case Embed:
                ReadBlockRMA2.write(rma2Formatter, readBlock, fileWriter, tmpWriter);
                break;
            case InRMAZ:
                ReadBlockRMA2.write(rma2Formatter, readBlock, dumpWriter, fileWriter);
                break;
            case Reference:
                ReadBlockRMA2.write(rma2Formatter, readBlock, null, fileWriter);
                break;
            default:
                throw new IOException("Unknown textStoragePolicy: " + textStoragePolicy);
        }
    }

    /**
     * close the file.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        infoSection.syncLocationManager2InfoSection(locationManager);

        switch (textStoragePolicy) {
            case Embed:
                infoSection.setDataDumpSectionEnd(fileWriter.getPosition());
                infoSection.setDataIndexSectionStart(fileWriter.getPosition());

                // copy the temporary index file to the main file:
                tmpWriter.close();

                InputReader indexReader = rma2File.getTmpIndexFileReader();
                FileChannel indexChannel = indexReader.getChannel();

                fileWriter.write(RMA2File.CHECK_BYTE);
                final int bufferSize = 1000000;
                byte[] buffer = new byte[bufferSize];
                long length = indexReader.length();
                long total = 1;
                for (long i = 0; i < length; i += bufferSize) {
                    int count = indexReader.read(buffer, 0, bufferSize);
                    if (total + count > length)
                        count = (int) (length - total);
                    if (count > 0) {
                        fileWriter.write(buffer, 0, count);
                        total += count;
                    }
                }
                infoSection.setDataIndexSectionEnd(fileWriter.getPosition());

                // there are no classifications (yet), so we dont't write them

                // empty summary section:
                infoSection.setAuxiliaryDataStart(fileWriter.getPosition());
                infoSection.setAuxiliaryDataEnd(fileWriter.getPosition());

                indexReader.close();
                indexChannel.close();

                rma2File.getIndexTmpFile().delete();
                break;
            case InRMAZ:
                dumpWriter.close();

                infoSection.setDataIndexSectionEnd(fileWriter.getPosition());

                // there are no classifications (yet), so we dont't write them

                // empty summary section:
                infoSection.setAuxiliaryDataStart(fileWriter.getPosition());
                infoSection.setAuxiliaryDataEnd(fileWriter.getPosition());
                break;
            case Reference:
                infoSection.setDataIndexSectionEnd(fileWriter.getPosition());

                // there are no classifications (yet), so we dont't write them

                // empty summary section:
                infoSection.setAuxiliaryDataStart(fileWriter.getPosition());
                infoSection.setAuxiliaryDataEnd(fileWriter.getPosition());
                break;
            default:
                throw new IOException("Unknown textStoragePolicy: " + textStoragePolicy);
        }

        infoSection.setNumberOfReads(numberOfReads);
        infoSection.setNumberOfMatches(numberOfMatches);
        infoSection.write(fileWriter);

        // System.err.println("File size: " + fileWriter.length());
        fileWriter.close();

        //     System.err.println("****** InfoSection:\n" + rma2File.loadInfoSection().toString());
    }

    /**
     * adds all locations of the source files, if necessary
     *
     * @param locationManager
     */
    public void setLocations(LocationManager locationManager) {
        infoSection.setTextFileNames(locationManager.getFileNames());
    }

    /**
     * get current position of file writer (This is not necessarily the position at which the read gets written)
     *
     * @return file writer position
     * @throws IOException
     */
    public long getFileWriterPosition() throws IOException {
        return fileWriter.getPosition();
    }
}
