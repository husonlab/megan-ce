/*
 *  Copyright (C) 2018 Daniel H. Huson
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

package megan.algorithms;

import java.util.ArrayList;

/**
 * Assignment algorithm
 * Daniel Huson, 1.2016
 */
public interface IMultiAssignmentAlgorithm extends IAssignmentAlgorithm {
    /**
     * get all additional assignments
     *
     * @param i                       the classification number to use in class ids entry
     * @param numberOfClassifications the total length of a class ids entry
     * @param classIds                all additional assignments returned here
     * @return the total number of gene segments detected
     */
    int getAdditionalClassIds(int i, int numberOfClassifications, ArrayList<int[]> classIds);
}
