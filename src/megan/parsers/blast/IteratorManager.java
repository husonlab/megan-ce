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

import jloda.util.BlastMode;
import megan.daa.io.DAA2SAMIterator;

import java.io.IOException;

/**
 * manages the alignment file iterators
 * Daniel Huson, 4.2015
 */
public class IteratorManager {
    /**
     * gets the iterator for the given file, format and blastMode
     *
     * @param blastFile
     * @param format
     * @param blastMode
     * @param maxMatchesPerRead
     * @return iterator
     * @throws IOException
     */
    public static ISAMIterator getIterator(String blastFile, BlastFileFormat format, BlastMode blastMode, int maxMatchesPerRead, boolean longReads) throws IOException {
        final ISAMIterator iterator;
        if (format == BlastFileFormat.SAM)
            iterator = new SAM2SAMIterator(blastFile, maxMatchesPerRead, blastMode);
        else if (format == BlastFileFormat.DAA) {
            iterator = new DAA2SAMIterator(blastFile, maxMatchesPerRead, longReads);
        } else if (format == BlastFileFormat.BlastText && blastMode == BlastMode.BlastX)
            iterator = new BlastX2SAMIterator(blastFile, maxMatchesPerRead);
        else if (format == BlastFileFormat.BlastText && blastMode == BlastMode.BlastP)
            iterator = new BlastP2SAMIterator(blastFile, maxMatchesPerRead);
        else if (format == BlastFileFormat.BlastText && blastMode == BlastMode.BlastN)
            iterator = new BlastN2SAMIterator(blastFile, maxMatchesPerRead);
        else if (format == BlastFileFormat.BlastXML)
            iterator = new BlastXML2SAMIterator(blastFile, maxMatchesPerRead);
        else if (format == BlastFileFormat.BlastTab)
            iterator = new BlastTab2SAMIterator(blastFile, maxMatchesPerRead);
        else if (format == BlastFileFormat.LastMAF)
            iterator = new LastMAF2SAMIterator(blastFile, maxMatchesPerRead, blastMode);
        else if (format == BlastFileFormat.RapSearch2Aln && blastMode == BlastMode.BlastX)
            iterator = new RAPSearchAln2SAMIterator(blastFile, maxMatchesPerRead);
        else if (format == BlastFileFormat.RDPAssignmentDetails)
            iterator = new RDPAssignmentDetails2SAMIterator(blastFile, maxMatchesPerRead);
        else if (format == BlastFileFormat.IlluminaReporter)
            iterator = new IlluminaReporter2SAMIterator(blastFile, maxMatchesPerRead);
        else if (format == BlastFileFormat.RDPStandalone)
            iterator = new RDPStandalone2SAMIterator(blastFile, maxMatchesPerRead);
        else if (format == BlastFileFormat.References_as_FastA)
            iterator = new Fasta2SAMIterator(blastFile, maxMatchesPerRead);
        else if (format == BlastFileFormat.Mothur)
            iterator = new Mothur2SAMIterator(blastFile, maxMatchesPerRead);
        else
            throw new IOException("Unsupported combination of file format: " + format + " and alignment mode: " + blastMode);
        iterator.setParseLongReads(longReads);
        return iterator;
    }
}
