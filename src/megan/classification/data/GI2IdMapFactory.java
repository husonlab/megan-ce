/*
 *  Copyright (C) 2015 Daniel H. Huson
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

import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.data.IName2IdMap;
import megan.fx.NotificationsInSwing;

import java.io.File;
import java.io.IOException;

/**
 * factory for creating GI to ID mappings
 * Created by huson on 7/15/16.
 */
public class GI2IdMapFactory implements ILong2IntegerMapFactory {
    /**
     * create a long to integer map from the named file
     *
     * @param label2id option mapping of labels to ids
     * @param fileName file
     * @param progress progress listner
     * @return map or null
     */
    @Override
    public ILong2IntegerMap create(IName2IdMap label2id, String fileName, ProgressListener progress) throws IOException, CanceledException {
        final LoadableLong2IntegerMap giMap = new LoadableLong2IntegerMap();
        final String name = (new File(fileName)).getName();
        if (name.equals("gi_taxid-March2015X.bin") || name.equals("gi2kegg-Nov2015X.bin") || name.equals("gi2tax-Feb2016.bin") || name.equals("gi2tax-Feb2016X.bin"))
            NotificationsInSwing.showWarning("The mapping file '" + name + "' is known to contain errors, please use latest file from the MEGAN6 download page");
        giMap.loadFile(label2id, fileName, progress);

        return giMap;
    }
}
