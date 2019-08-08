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

import jloda.swing.window.NotificationsInSwing;
import megan.util.BlastPTextFileFilter;

import java.io.IOException;


/**
 * parses a blastp files into SAM format
 * Daniel Huson, 4.2015
 */
public class BlastP2SAMIterator extends BlastX2SAMIterator implements ISAMIterator {
    /**
     * constructor
     *
     * @param fileName
     * @param maxNumberOfMatchesPerRead
     * @throws IOException
     */
    public BlastP2SAMIterator(String fileName, int maxNumberOfMatchesPerRead) throws IOException {
        super(fileName, maxNumberOfMatchesPerRead, true);
        if (!BlastPTextFileFilter.getInstance().accept(fileName)) {
            NotificationsInSwing.showWarning("Might not be a BLASTP file in TEXT format: " + fileName);
        }
    }
}
