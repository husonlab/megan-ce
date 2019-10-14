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
package megan.parsers.blast;

import jloda.swing.util.FastaFileFilter;
import jloda.util.Basic;
import jloda.util.FileLineIterator;
import jloda.util.ProgramProperties;
import megan.daa.io.DAAParser;
import megan.util.*;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * possible blast modes
 * Daniel Huson, 1.2014
 */
public class BlastModeUtils {
    /**
     * gets the value of a label ignoring case
     *
     * @param label
     * @return value or null
     */
    private static jloda.util.BlastMode valueOfIgnoreCase(String label) {
        for (jloda.util.BlastMode type : jloda.util.BlastMode.values())
            if (label.equalsIgnoreCase(type.toString()))
                return type;
        return null;
    }

    /**
     * gets the blast mode for a given file
     *
     * @param fileName
     * @return blast mode or null
     */
    public static jloda.util.BlastMode getBlastMode(String fileName) {
        if (SAMFileFilter.getInstance().accept(fileName))
            return determineBlastModeSAMFile(fileName);
        else if (DAAFileFilter.getInstance().accept(fileName))
            return DAAParser.getBlastMode(fileName);
        else if (BlastXTextFileFilter.getInstance().accept(fileName))
            return jloda.util.BlastMode.BlastX;
        else if (BlastNTextFileFilter.getInstance().accept(fileName))
            return jloda.util.BlastMode.BlastN;
        else if (BlastPTextFileFilter.getInstance().accept(fileName))
            return jloda.util.BlastMode.BlastP;
        else if (BlastTabFileFilter.getInstance().accept(fileName))
            return jloda.util.BlastMode.Unknown;
        if (LastMAFFileFilter.getInstance().accept(fileName))
            return jloda.util.BlastMode.Unknown;
        else if (BlastXMLFileFilter.getInstance().accept(fileName))
            return determineBlastModeXMLFile(fileName);
        else if (RAPSearch2AlnFileFilter.getInstance().accept(fileName))
            return jloda.util.BlastMode.BlastX;
        else if (RDPAssignmentDetailsFileFilter.getInstance().accept(fileName))
            return jloda.util.BlastMode.Classifier;
        else if (IlluminaReporterFileFilter.getInstance().accept(fileName))
            return jloda.util.BlastMode.Classifier;
        else if (RDPStandaloneFileFilter.getInstance().accept(fileName))
            return jloda.util.BlastMode.Classifier;
        else if (FastaFileFilter.getInstance().accept(fileName))
            return jloda.util.BlastMode.Classifier;
        else if (MothurFileFilter.getInstance().accept(fileName))
            return jloda.util.BlastMode.Classifier;
        else
            return jloda.util.BlastMode.Unknown;
    }

    /**
     * Determine the alignment mode
     *
     * @param owner
     * @param filename
     * @param ask
     * @return file format or null
     * @throws IOException
     */
    public static jloda.util.BlastMode detectMode(Component owner, String filename, boolean ask) throws IOException {
        jloda.util.BlastMode mode = getBlastMode(filename);

        if (mode == jloda.util.BlastMode.Unknown && ask) {
            if (!ProgramProperties.isUseGUI())
                throw new IOException("Couldn't detect BLAST mode, please specify");
            mode = (jloda.util.BlastMode) JOptionPane.showInputDialog(owner, "Cannot determine mode, please choose:", "Question: Which mode?",
                    JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), jloda.util.BlastMode.values(), jloda.util.BlastMode.BlastX);
        }
        return mode;
    }

    /**
     * determine blast mode of a SAM file
     *
     * @param samFile
     * @return blast mode
     * @throws java.io.IOException
     */
    public static jloda.util.BlastMode determineBlastModeSAMFile(String samFile) {
        try (final FileLineIterator it = new FileLineIterator(samFile)) {
            for (int i = 0; i < 50; i++) { // expect to figure out blast mode within the first 50 lines
                if (it.hasNext()) {
                    String aLine = it.next().trim();
                    if (aLine.startsWith("@PG")) {
                        String[] tokens = aLine.split("\t");
                        for (String token : tokens) {
                            if (token.startsWith("DS:") && token.length() > 3) {
                                jloda.util.BlastMode mode = valueOfIgnoreCase(token.substring(3));
                                if (mode != null)
                                    return mode;
                            }
                        }
                    }
                    // this is for backward compatibility:
                    if (aLine.startsWith("@mm")) {
                        String[] tokens = aLine.split("\t");
                        if (tokens.length >= 2) {
                            jloda.util.BlastMode mode = valueOfIgnoreCase(tokens[1]);
                            if (mode != null)
                                return mode;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return jloda.util.BlastMode.BlastN;
    }

    /**
     * determine the blast mode for an XML file
     *
     * @param xmlFile
     * @return blast mode
     */
    private static jloda.util.BlastMode determineBlastModeXMLFile(String xmlFile) {
        try (final FileLineIterator it = new FileLineIterator(xmlFile)) {
            for (int i = 0; i < 50; i++) { // expect to figure out blast mode within the first 50 lines
                if (it.hasNext()) {
                    String line = it.next().trim().toLowerCase();
                    if (line.startsWith("<blastoutput_program>") || line.startsWith("<blastoutput_version>")) {
                        if (line.contains("blastn"))
                            return jloda.util.BlastMode.BlastN;
                        else if (line.contains("blastx"))
                            return jloda.util.BlastMode.BlastX;
                        else if (line.contains("blastp"))
                            return jloda.util.BlastMode.BlastP;
                    }
                }
            }
        } catch (Exception ex) {
            Basic.caught(ex);
        }
        return jloda.util.BlastMode.Unknown;
    }

    /**
     * gets the value ignoring case
     *
     * @param formatName
     * @return value or null
     */
    public static jloda.util.BlastMode valueOfIgnoringCase(String formatName) {
        if (formatName != null) {
            for (jloda.util.BlastMode format : jloda.util.BlastMode.values()) {
                if (format.toString().equalsIgnoreCase(formatName))
                    return format;
            }
        }
        return null;
    }

    /**
     * set of all values except 'Unknown'
     *
     * @return
     */
    public static jloda.util.BlastMode[] valuesExceptUnknown() {
        jloda.util.BlastMode[] array = new jloda.util.BlastMode[jloda.util.BlastMode.values().length - 1];
        int i = -0;
        for (jloda.util.BlastMode value : jloda.util.BlastMode.values()) {
            if (value != jloda.util.BlastMode.Unknown)
                array[i++] = value;
        }
        return array;
    }
}
