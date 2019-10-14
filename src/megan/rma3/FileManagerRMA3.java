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

import jloda.swing.util.ChooseFileDialog;
import jloda.swing.util.TextFileFilter;
import jloda.util.Basic;
import jloda.util.GZipUtils;
import jloda.util.ProgramProperties;
import megan.io.InputOutputReaderWriter;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * File manager for RMA3
 * Daniel Huson, 2015
 */
public class FileManagerRMA3 {
    private boolean dontAskAny = !ProgramProperties.isUseGUI();
    private final Set<String> tabooSet = new HashSet<>();
    private static FileManagerRMA3 instance;

    private FileManagerRMA3() {
    }

    /**
     * get the instance of the FileRMA3 manager
     *
     * @return instance
     */
    public static FileManagerRMA3 getInstance() {
        if (instance == null)
            instance = new FileManagerRMA3();
        return instance;
    }

    /**
     * get the sam file associated with a RMA3 file, asking to locate it, if missing
     *
     * @param rma3File
     * @return file or null
     */
    public File getSAMFile(String rma3File) throws IOException {
        final String fileName = (new RMA3File(rma3File, RMA3FileModifier.READ_ONLY)).getFileFooter().getAlignmentFile();
        if (fileName == null || fileName.length() == 0)
            return null;
        final String suffix = Basic.getFileSuffix(fileName);
        final String type = (new RMA3File(rma3File, RMA3FileModifier.READ_ONLY)).getFileFooter().getAlignmentFileFormat();

        return getFile(rma3File, fileName, suffix, type, true);
    }

    /**
     * get the fasta file associated with a RMA3 file, asking to locate it, if missing
     *
     * @param rma3File
     * @return file
     */
    public File getFASTAFile(String rma3File) throws IOException {
        final String fileName = (new RMA3File(rma3File, RMA3FileModifier.READ_ONLY)).getFileFooter().getReadsFile();
        if (fileName == null || fileName.length() == 0)
            return null;
        final String suffix = Basic.getFileSuffix(fileName);
        final String type = (new RMA3File(rma3File, RMA3FileModifier.READ_ONLY)).getFileFooter().getReadsFileFormat();

        return getFile(rma3File, fileName, suffix, type, false);
    }

    /**
     * gets a file, asking to decompress it, if it is gzipped and asking to locate it, if missing
     *
     * @param rma3File
     * @return file
     */
    private File getFile(String rma3File, String fileName, String suffix, String type, boolean alignmentFile) throws IOException {
        if (dontAskAny || tabooSet.contains(fileName)) {
            File file = new File(fileName);
            if (file.exists())
                return file;
            else
                throw new IOException("File not found: " + fileName);
        }

        if (fileName != null && fileName.length() > 0) {
            File file = new File(fileName);
            if (file.exists())
                return file;
            else {
                System.err.println("No such file: " + file);
                if ((new File(fileName + ".gz")).exists()) {
                    System.err.println("Run gunzip on: " + fileName + ".gz");
                    int response = JOptionPane.showConfirmDialog(null, "Required " + type + " file '" + file.getName() + "' is compressed, decompress?",
                            type + " file is compressed", JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon());
                    switch (response) {
                        case JOptionPane.YES_OPTION:
                            GZipUtils.inflate(fileName + ".gz", fileName);
                            return new File(fileName);
                        case JOptionPane.NO_OPTION:
                            break;
                        default:
                        case JOptionPane.CANCEL_OPTION:
                            throw new IOException("User canceled");
                    }
                }
                if ((new File(fileName + ".zip")).exists())
                    System.err.println("Run unzip on: " + fileName + ".zip");
            }
            if (ProgramProperties.isUseGUI()) {
                if (!file.getParentFile().exists() || !file.getParentFile().isDirectory()) {
                    file = new File((new File(rma3File)).getParent(), Basic.getFileNameWithoutPath(fileName));
                }

                String[] choices = new String[]{"Locate missing " + type + " file", "Don't ask again for this missing file", "Don't ask again for any missing files"};
                String choice = (String) JOptionPane.showInputDialog(null, "Need " + type + " file to access reads", "MEGAN requires " + type + " file",
                        JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), choices, choices[0]);
                if (choice == null)
                    throw new IOException("File not found: " + fileName);
                else if (choice.equals(choices[0])) {
                    File altFile = ChooseFileDialog.chooseFileToOpen(null, file, (new TextFileFilter(suffix)), (new TextFileFilter(suffix)), null, "Locate " + type + " file '" + file.getName() + "'");
                    if (altFile == null)
                        throw new IOException("User canceled");
                    if (!altFile.exists())
                        throw new IOException("No such file: " + altFile);


                    try (RMA3FileModifier modifier = new RMA3FileModifier(rma3File)) {
                        final FileFooterRMA3 footer = modifier.getFileFooter();

                        if (alignmentFile) {
                            if (altFile.exists() && altFile.length() != footer.getAlignmentFileSize()) {
                                throw new IOException("Specified file has wrong size " + file.length() + ", expected: " + footer.getAlignmentFileSize());
                            }
                            footer.setAlignmentFile(altFile.getPath());
                        } else {
                            if (altFile.exists() && altFile.length() != footer.getReadsFileSize()) {
                                throw new IOException("Specified " + type + " file has wrong size " + file.length() + ", expected: " + footer.getReadsFileSize());
                            }
                            footer.setReadsFile(altFile.getPath());
                        }

                        try (InputOutputReaderWriter io = new InputOutputReaderWriter(new File(rma3File), RMA3FileModifier.READ_WRITE)) {
                            io.setLength(footer.getFileFooter());
                            io.seek(footer.getFileFooter());
                            footer.write(io);
                        }
                    }
                    return altFile;

                } else if (choice.equals(choices[1])) {
                    tabooSet.add(fileName);
                    throw new IOException("File not found: " + fileName);
                } else if (choice.equals(choices[2])) {
                    dontAskAny = true;
                    throw new IOException("File not found: " + fileName);
                }
            }
        }
        throw new IOException("File not found: " + fileName);
    }
}
