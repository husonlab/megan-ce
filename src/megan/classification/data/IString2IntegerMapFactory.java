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

import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.data.IName2IdMap;

import java.io.IOException;

/**
 * factory for creating string-2-integer mappings
 * Created by huson on August 2017
 */
public interface IString2IntegerMapFactory {
    /**
     * create a string to integer map from the named file
     *
     * @param label2id option mapping of labels to ids
     * @param fileName file
     * @param progress progress listner
     * @return map or null
     */
    IString2IntegerMap create(IName2IdMap label2id, String fileName, ProgressListener progress) throws IOException, CanceledException;
}
