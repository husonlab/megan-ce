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
import jloda.util.ProgramProperties;
import megan.util.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;


/**
 * alignment file formats
 * Daniel Huson. 4.2015
 */
public enum BlastFileFormat {
    Unknown, DAA, BlastText, BlastXML, BlastTab, LastMAF, RapSearch2Aln, IlluminaReporter, RDPAssignmentDetails, RDPStandalone, Mothur, SAM, References_as_FastA;

    /**
     * Determine the file format of an alignment file
     *
     * @param owner
     * @param fileName
     * @param ask
     * @return file format or null
     * @throws IOException
     */
    public static BlastFileFormat detectFormat(Component owner, String fileName, boolean ask) throws IOException {
        BlastFileFormat result = null;

        if (!(new File(fileName)).canRead() || (new File(fileName).isDirectory()))
            throw new IOException("Can't open file to read: " + fileName);

        if (SAMFileFilter.getInstance().accept(fileName))
            result = SAM;
        else if (DAAFileFilter.getInstance().accept(fileName))
            result = DAA;
        else if (BlastXTextFileFilter.getInstance().accept(fileName))
            result = BlastText;
        else if (BlastNTextFileFilter.getInstance().accept(fileName))
            result = BlastText;
        else if (BlastPTextFileFilter.getInstance().accept(fileName))
            result = BlastText;
        else if (BlastTabFileFilter.getInstance().accept(fileName))
            result = BlastTab;
        else if (BlastXMLFileFilter.getInstance().accept(fileName))
            result = BlastXML;
        else if (LastMAFFileFilter.getInstance().accept(fileName))
            result = LastMAF;
        else if (RAPSearch2AlnFileFilter.getInstance().accept(fileName))
            result = RapSearch2Aln;
        else if (RDPAssignmentDetailsFileFilter.getInstance().accept(fileName))
            result = RDPAssignmentDetails;
        else if (IlluminaReporterFileFilter.getInstance().accept(fileName))
            result = IlluminaReporter;
        else if (RDPStandaloneFileFilter.getInstance().accept(fileName))
            result = RDPStandalone;
        else if (FastaFileFilter.getInstance().accept(fileName))
            result = References_as_FastA;
        else if (MothurFileFilter.getInstance().accept(fileName))
            result = Mothur;

        if (result == null && ProgramProperties.isUseGUI() && ask) {
            result = (BlastFileFormat) JOptionPane.showInputDialog(owner, "Cannot determine format, please choose:", "Question: Which input format?",
                    JOptionPane.QUESTION_MESSAGE, ProgramProperties.getProgramIcon(), valuesExceptUnknown(), BlastText);
        }
        if (result == null) {
            throw new IOException("Failed to determine BLAST format");
        }
        return result;
    }

    /**
     * gets the value ignoring case
     *
     * @param formatName
     * @return value or null
     */
    public static BlastFileFormat valueOfIgnoreCase(String formatName) {
        if (formatName != null) {
            for (BlastFileFormat format : values()) {
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
    public static BlastFileFormat[] valuesExceptUnknown() {
        BlastFileFormat[] array = new BlastFileFormat[values().length - 1];
        int i = -0;
        for (BlastFileFormat value : values()) {
            if (value != Unknown)
                array[i++] = value;
        }
        return array;
    }
}
