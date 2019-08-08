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

package megan.classification.data;

import jloda.swing.window.NotificationsInSwing;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.data.IName2IdMap;

import java.io.IOException;

/**
 * factory for creating Accession to ID mappings
 * Created by huson on 7/15/16.
 */
public class Accession2IdMapFactory implements IString2IntegerMapFactory {
    /**
     * create an accession to integer map from the named file
     *
     * @param label2id option mapping of labels to ids
     * @param fileName file
     * @param progress progress listener
     * @return map or null
     */
    @Override
    public IString2IntegerMap create(IName2IdMap label2id, String fileName, ProgressListener progress) throws IOException, CanceledException {
        if (String2IntegerFileBasedABinMap.isTableFile(fileName))
            return new String2IntegerFileBasedABinMap(fileName);
        else if (String2IntegerFileBasedABinMap.isIncompatibleTableFile(fileName)) {
            NotificationsInSwing.showError("Incompatible mapping file (UE?): " + fileName);
            throw new IOException("Incompatible mapping file (UE?): " + fileName);
        } else
            return new Accession2IdMap(label2id, fileName, progress);
    }
}
